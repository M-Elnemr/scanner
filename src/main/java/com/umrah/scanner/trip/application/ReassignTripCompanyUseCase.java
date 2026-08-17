package com.umrah.scanner.trip.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves a trip's catalogue listing to a different company. Deliberately its own use case rather
 * than a field on {@link UpdateTripCommand}: this is the one trip mutation with a cross-aggregate
 * consequence, and burying it in a generic edit body means any admin edit could silently move a
 * trip's ownership.
 *
 * <p><strong>What this does not do:</strong> existing leads on this trip keep their original
 * {@code companyId}. A lead's commission is a snapshot of the company that existed when it was
 * created ({@code Lead.applyPricing} — see its javadoc on why a lead is never repriced); repointing
 * it would hand the new company a debt priced against the old company's rate, and would show a
 * customer a different operator than the one they engaged with. So after this call
 * {@code lead.company != lead.trip.company} for every pre-existing lead on this trip, by design.
 * Only leads created after the reassignment follow the new owner.
 */
@Service
public class ReassignTripCompanyUseCase {

    private final TripRepository tripRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final AuditLogService auditLogService;

    public ReassignTripCompanyUseCase(
            TripRepository tripRepository,
            CompanyProfileRepository companyProfileRepository,
            NotificationDispatcher notificationDispatcher,
            AuditLogService auditLogService) {
        this.tripRepository = tripRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Trip execute(UUID adminUserId, UUID tripId, UUID targetCompanyId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> NotFoundException.of("Trip", tripId));
        CompanyProfile targetCompany = companyProfileRepository.findById(targetCompanyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", targetCompanyId));

        if (trip.getStatus() == TripStatus.CLOSED) {
            throw new ValidationException("A closed trip can no longer be reassigned");
        }
        if (targetCompany.getStatus() != CompanyStatus.APPROVED) {
            throw new ValidationException("Trips can only be reassigned to an approved company");
        }
        CompanyProfile previousCompany = trip.getCompany();
        if (previousCompany.getId().equals(targetCompanyId)) {
            throw new ValidationException("This trip already belongs to that company");
        }

        trip.setCompany(targetCompany);
        trip.setLastUpdate(Instant.now());

        auditLogService.record(adminUserId, "TRIP_COMPANY_REASSIGNED", "Trip", tripId,
                previousCompany.getId(), targetCompanyId);

        notificationDispatcher.dispatch(previousCompany.getUser().getId(), "TRIP_REASSIGNED",
                "A trip left your catalogue",
                "\"" + trip.getTitle() + "\" has been reassigned to another company by the platform.",
                Map.of("tripId", tripId.toString()));
        notificationDispatcher.dispatch(targetCompany.getUser().getId(), "TRIP_REASSIGNED",
                "A trip joined your catalogue",
                "\"" + trip.getTitle() + "\" has been assigned to your company by the platform.",
                Map.of("tripId", tripId.toString()));

        // Only id and company are guaranteed initialized here — the caller re-reads the full detail
        // through TripQueryService, which fetch-joins every reference, rather than mapping this
        // entity directly (same pattern as every other trip write use case).
        return trip;
    }
}
