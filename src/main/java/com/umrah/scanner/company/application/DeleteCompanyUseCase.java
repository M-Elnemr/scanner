package com.umrah.scanner.company.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.auth.infrastructure.RefreshTokenRepository;
import com.umrah.scanner.commission.domain.CommissionStatus;
import com.umrah.scanner.commission.infrastructure.CommissionRepository;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import com.umrah.scanner.user.domain.User;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A company that was created by mistake, not one that misbehaved — {@link SuspendCompanyUseCase}
 * plus {@link ReinstateCompanyUseCase} cover the operational "stop this company, maybe restart it
 * later" need without ever touching the record. This is reserved for the case those two don't:
 * unwinding something that should never have existed.
 *
 * <p>Blocked while any money is still moving, because {@code CompanyProfile} carries
 * {@code @SQLRestriction("deleted_at is null")} and both {@code Lead.company} and
 * {@code Trip.company} are non-optional associations — soft-deleting a company underneath a live
 * booking would make that booking unloadable for the customer holding it.
 */
@Service
public class DeleteCompanyUseCase {

    private static final List<CommissionStatus> OWED_STATUSES = List.of(CommissionStatus.PENDING, CommissionStatus.REPORTED);

    private final CompanyProfileRepository companyProfileRepository;
    private final LeadRepository leadRepository;
    private final CommissionRepository commissionRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;

    public DeleteCompanyUseCase(
            CompanyProfileRepository companyProfileRepository,
            LeadRepository leadRepository,
            CommissionRepository commissionRepository,
            TripRepository tripRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuditLogService auditLogService) {
        this.companyProfileRepository = companyProfileRepository;
        this.leadRepository = leadRepository;
        this.commissionRepository = commissionRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void execute(UUID adminUserId, UUID companyId) {
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyId));

        long activeLeads = leadRepository.countActiveByCompanyId(companyId);
        if (activeLeads > 0) {
            throw new ConflictException(
                    "This company has " + activeLeads + " live booking(s). Resolve or cancel them before deleting it.");
        }
        if (commissionRepository.existsByCompanyIdAndStatusIn(companyId, OWED_STATUSES)) {
            throw new ConflictException("This company has an unsettled commission owed to the platform.");
        }

        Instant now = Instant.now();
        company.markDeleted(now);
        tripRepository.softDeleteAllByCompanyId(companyId, now);

        User owner = company.getUser();
        owner.markDeleted(now);
        refreshTokenRepository.revokeAllActiveForUser(owner.getId(), now);
        userRepository.save(owner);

        // Nobody is notified: the owner's account no longer exists to receive it.
        auditLogService.record(adminUserId, "COMPANY_DELETED", "CompanyProfile", companyId, company.getCompanyName(), null);
    }
}
