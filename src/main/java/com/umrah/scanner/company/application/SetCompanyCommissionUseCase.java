package com.umrah.scanner.company.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import com.umrah.scanner.pricing.domain.Money;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only writer of {@code commissionPerTraveler}. Admin-only by construction — no company-facing
 * use case can reach this field, so the "companies may view but never edit their rate" rule holds
 * regardless of what a controller does.
 *
 * <p>A change here is forward-looking: leads already created keep the amounts they were priced with.
 */
@Service
public class SetCompanyCommissionUseCase {

    private final CompanyProfileRepository companyProfileRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final AuditLogService auditLogService;

    public SetCompanyCommissionUseCase(
            CompanyProfileRepository companyProfileRepository,
            NotificationDispatcher notificationDispatcher,
            AuditLogService auditLogService) {
        this.companyProfileRepository = companyProfileRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CompanyProfile execute(UUID adminUserId, UUID companyId, BigDecimal commissionPerTraveler) {
        if (commissionPerTraveler == null || commissionPerTraveler.signum() < 0) {
            throw new ValidationException("Commission per traveler must be zero or positive");
        }

        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyId));

        BigDecimal previous = company.getCommissionPerTraveler();
        BigDecimal updated = Money.normalize(commissionPerTraveler);
        company.setCommissionPerTraveler(updated);

        notificationDispatcher.dispatch(
                company.getUser().getId(),
                "COMMISSION_RATE_UPDATED",
                "Commission updated",
                "Your commission is now " + updated.toPlainString() + " EGP per traveler. Existing leads are unchanged.",
                Map.of("companyId", companyId.toString(), "commissionPerTraveler", updated.toPlainString()));

        auditLogService.record(adminUserId, "COMPANY_COMMISSION_UPDATED", "CompanyProfile", companyId, previous, updated);

        CompanyProfileInitializer.initializeAddresses(company);
        return company;
    }
}
