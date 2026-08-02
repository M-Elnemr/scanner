package com.umrah.scanner.company.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RejectCompanyUseCase {

    private final CompanyProfileRepository companyProfileRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final AuditLogService auditLogService;

    public RejectCompanyUseCase(
            CompanyProfileRepository companyProfileRepository,
            NotificationDispatcher notificationDispatcher,
            AuditLogService auditLogService) {
        this.companyProfileRepository = companyProfileRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CompanyProfile execute(UUID adminUserId, UUID companyId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("A rejection reason is required");
        }
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyId));

        if (company.getStatus() == CompanyStatus.APPROVED) {
            throw new ConflictException("An approved company must be suspended, not rejected");
        }

        CompanyStatus previous = company.getStatus();
        company.setStatus(CompanyStatus.REJECTED);
        company.setRejectionReason(reason);

        notificationDispatcher.dispatch(
                company.getUser().getId(),
                "COMPANY_REJECTED",
                "Your company application was rejected",
                reason,
                Map.of("companyId", company.getId().toString()));

        auditLogService.record(adminUserId, "COMPANY_REJECTED", "CompanyProfile", companyId, previous, company.getStatus());
        CompanyProfileInitializer.initializeAddresses(company);
        return company;
    }
}
