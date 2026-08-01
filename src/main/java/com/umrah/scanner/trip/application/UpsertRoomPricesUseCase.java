package com.umrah.scanner.trip.application;

import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.trip.domain.RoomPrice;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpsertRoomPricesUseCase {

    private final TripOwnershipGuard tripOwnershipGuard;
    private final TripRepository tripRepository;

    public UpsertRoomPricesUseCase(TripOwnershipGuard tripOwnershipGuard, TripRepository tripRepository) {
        this.tripOwnershipGuard = tripOwnershipGuard;
        this.tripRepository = tripRepository;
    }

    @Transactional
    public Trip execute(UUID companyUserId, UUID tripId, List<RoomPriceInput> prices) {
        if (prices == null || prices.isEmpty()) {
            throw new ValidationException("At least one room price is required");
        }

        Trip trip = tripOwnershipGuard.findOwnedTrip(companyUserId, tripId);
        trip.getRoomPrices().clear();
        // Force the orphan-removal deletes to run now, before the inserts below. Otherwise
        // Hibernate flushes inserts first and a replacement row for the same (trip_id, room_type)
        // hits uq_room_prices_trip_room_type before the old row is gone.
        tripRepository.flush();
        for (RoomPriceInput input : prices) {
            RoomPrice roomPrice = new RoomPrice();
            roomPrice.setRoomType(input.roomType());
            roomPrice.setPrice(input.price());
            trip.addRoomPrice(roomPrice);
        }
        trip.setLastUpdate(Instant.now());
        // The controller maps the response after this transaction/session closes (open-in-view is
        // off), so any lazy collection it touches must be force-initialized here first.
        trip.getHotels().size();
        return trip;
    }
}
