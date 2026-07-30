package com.umrah.scanner.company.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuspendCompanyUseCase {

    private final CompanyProfileRepository companyProfileRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final AuditLogService auditLogService;

    public SuspendCompanyUseCase(
            CompanyProfileRepository companyProfileRepository,
            NotificationDispatcher notificationDispatcher,
            AuditLogService auditLogService) {
        this.companyProfileRepository = companyProfileRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CompanyProfile execute(UUID adminUserId, UUID companyId, String reason) {
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyId));

        if (company.getStatus() != CompanyStatus.APPROVED) {
            throw new ConflictException("Only an approved company can be suspended");
        }

        company.setStatus(CompanyStatus.SUSPENDED);
        company.setRejectionReason(reason);

        notificationDispatcher.dispatch(
                company.getUser().getId(),
                "COMPANY_SUSPENDED",
                "Your company account has been suspended",
                reason,
                Map.of("companyId", company.getId().toString()));

        auditLogService.record(adminUserId, "COMPANY_SUSPENDED", "CompanyProfile", companyId, CompanyStatus.APPROVED, company.getStatus());
        return company;
    }
}
