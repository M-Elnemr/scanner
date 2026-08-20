package com.umrah.scanner.trip.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.trip.domain.Trip;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteTripUseCase {

    private final TripOwnershipGuard tripOwnershipGuard;
    private final AuditLogService auditLogService;
    private final LeadRepository leadRepository;

    public DeleteTripUseCase(
            TripOwnershipGuard tripOwnershipGuard, AuditLogService auditLogService, LeadRepository leadRepository) {
        this.tripOwnershipGuard = tripOwnershipGuard;
        this.auditLogService = auditLogService;
        this.leadRepository = leadRepository;
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
        long activeLeads = leadRepository.countActiveByTripId(trip.getId());
        if (activeLeads > 0) {
            throw new ConflictException(
                    "This trip has " + activeLeads + " live booking(s). Resolve or cancel them before deleting it.");
        }
        trip.markDeleted(Instant.now());
        auditLogService.record(actorUserId, "TRIP_DELETED", "Trip", trip.getId(), trip.getStatus(), null);
    }
}
