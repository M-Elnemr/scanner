package com.umrah.scanner.trip.application;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteTripUseCase {

    private final TripOwnershipGuard tripOwnershipGuard;

    public DeleteTripUseCase(TripOwnershipGuard tripOwnershipGuard) {
        this.tripOwnershipGuard = tripOwnershipGuard;
    }

    @Transactional
    public void execute(UUID companyUserId, UUID tripId) {
        tripOwnershipGuard.findOwnedTrip(companyUserId, tripId).markDeleted(Instant.now());
    }
}
