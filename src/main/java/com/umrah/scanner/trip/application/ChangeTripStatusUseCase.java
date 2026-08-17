package com.umrah.scanner.trip.application;

import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.hotel.domain.HotelCity;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeTripStatusUseCase {

    private final TripOwnershipGuard tripOwnershipGuard;

    public ChangeTripStatusUseCase(TripOwnershipGuard tripOwnershipGuard) {
        this.tripOwnershipGuard = tripOwnershipGuard;
    }

    @Transactional
    public Trip executeAsCompany(UUID companyUserId, UUID tripId, TripStatus target) {
        return change(tripOwnershipGuard.findOwnedTrip(companyUserId, tripId), target);
    }

    @Transactional
    public Trip executeAsAdmin(UUID adminUserId, UUID tripId, TripStatus target) {
        return change(tripOwnershipGuard.findAnyTrip(tripId), target);
    }

    private Trip change(Trip trip, TripStatus target) {
        if (trip.getStatus() == TripStatus.DRAFT && target == TripStatus.PUBLISHED) {
            requireReadyToPublish(trip);
        } else if (!(trip.getStatus() == TripStatus.PUBLISHED && target == TripStatus.CLOSED)) {
            throw new ValidationException("Cannot move a trip from " + trip.getStatus() + " to " + target);
        }

        trip.setStatus(target);
        trip.setLastUpdate(Instant.now());
        // The controller maps the response after this transaction/session closes (open-in-view is
        // off), so any lazy collection it touches must be force-initialized here first.
        TripCollectionsInitializer.initialize(trip);
        return trip;
    }

    private void requireReadyToPublish(Trip trip) {
        if (trip.getRoomPrices().isEmpty()) {
            throw new ValidationException("Publish requires at least one room price");
        }
        boolean hasMakkah = trip.getHotels().stream().anyMatch(h -> h.getCity() == HotelCity.MAKKAH);
        boolean hasMadinah = trip.getHotels().stream().anyMatch(h -> h.getCity() == HotelCity.MADINAH);
        if (!hasMakkah || !hasMadinah) {
            throw new ValidationException("Publish requires hotel details for both Makkah and Madinah");
        }
    }
}
