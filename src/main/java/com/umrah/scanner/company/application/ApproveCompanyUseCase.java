package com.umrah.scanner.company.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveCompanyUseCase {

    private final CompanyProfileRepository companyProfileRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final AuditLogService auditLogService;

    public ApproveCompanyUseCase(
            CompanyProfileRepository companyProfileRepository,
            NotificationDispatcher notificationDispatcher,
            AuditLogService auditLogService) {
        this.companyProfileRepository = companyProfileRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CompanyProfile execute(UUID adminUserId, UUID companyId) {
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyId));

        if (company.getStatus() == CompanyStatus.APPROVED) {
            return company;
        }
        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            throw new ConflictException("A suspended company must be reinstated, not approved");
        }

        CompanyStatus previous = company.getStatus();
        company.setStatus(CompanyStatus.APPROVED);
        company.setApprovedBy(adminUserId);
        company.setApprovedAt(Instant.now());
        company.setRejectionReason(null);

        notificationDispatcher.dispatch(
                company.getUser().getId(),
                "COMPANY_APPROVED",
                "You're approved",
                "Your company profile has been approved. You can now publish Umrah trips.",
                Map.of("companyId", company.getId().toString()));

        auditLogService.record(adminUserId, "COMPANY_APPROVED", "CompanyProfile", companyId, previous, company.getStatus());
        return company;
    }
}
