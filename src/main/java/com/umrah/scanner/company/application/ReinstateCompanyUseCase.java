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

/**
 * Closes a dead end that predates this change: {@link ApproveCompanyUseCase} already refuses a
 * suspended company with "must be reinstated, not approved", but nothing implemented reinstating —
 * so suspension was a one-way door. Restores straight to {@code APPROVED} rather than {@code PENDING}:
 * the company was already vetted once, and sending it back through review would treat the
 * reinstatement as if nothing were known about it.
 */
@Service
public class ReinstateCompanyUseCase {

    private final CompanyProfileRepository companyProfileRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final AuditLogService auditLogService;

    public ReinstateCompanyUseCase(
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

        if (company.getStatus() != CompanyStatus.SUSPENDED) {
            throw new ConflictException("Only a suspended company can be reinstated");
        }

        company.setStatus(CompanyStatus.APPROVED);
        company.setApprovedBy(adminUserId);
        company.setApprovedAt(Instant.now());
        company.setRejectionReason(null);

        notificationDispatcher.dispatch(
                company.getUser().getId(),
                "COMPANY_REINSTATED",
                "Your company account has been reinstated",
                "You can publish and manage trips again.",
                Map.of("companyId", company.getId().toString()));

        auditLogService.record(adminUserId, "COMPANY_REINSTATED", "CompanyProfile", companyId, CompanyStatus.SUSPENDED, CompanyStatus.APPROVED);
        CompanyProfileInitializer.initializeAddresses(company);
        return company;
    }
}
