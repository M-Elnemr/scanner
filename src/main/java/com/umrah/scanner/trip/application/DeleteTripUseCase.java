package com.umrah.scanner.trip.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.trip.domain.Trip;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteTripUseCase {

    private final TripOwnershipGuard tripOwnershipGuard;
    private final AuditLogService auditLogService;

    public DeleteTripUseCase(TripOwnershipGuard tripOwnershipGuard, AuditLogService auditLogService) {
        this.tripOwnershipGuard = tripOwnershipGuard;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void executeAsCompany(UUID companyUserId, UUID tripId) {
        delete(tripOwnershipGuard.findOwnedTrip(companyUserId, tripId), companyUserId);
    }

    @Transactional
    public void executeAsAdmin(UUID adminUserId, UUID tripId) {
        delete(tripOwnershipGuard.findAnyTrip(tripId), adminUserId);
    }

    private void delete(Trip trip, UUID actorUserId) {
        trip.markDeleted(Instant.now());
        auditLogService.record(actorUserId, "TRIP_DELETED", "Trip", trip.getId(), trip.getStatus(), null);
    }
}
