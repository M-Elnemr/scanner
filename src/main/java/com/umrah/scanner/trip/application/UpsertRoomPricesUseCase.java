package com.umrah.scanner.trip.application;

import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.trip.domain.RoomPrice;
import com.umrah.scanner.trip.domain.Trip;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpsertRoomPricesUseCase {

    private final TripOwnershipGuard tripOwnershipGuard;

    public UpsertRoomPricesUseCase(TripOwnershipGuard tripOwnershipGuard) {
        this.tripOwnershipGuard = tripOwnershipGuard;
    }

    @Transactional
    public Trip execute(UUID companyUserId, UUID tripId, List<RoomPriceInput> prices) {
        if (prices == null || prices.isEmpty()) {
            throw new ValidationException("At least one room price is required");
        }

        Trip trip = tripOwnershipGuard.findOwnedTrip(companyUserId, tripId);
        trip.getRoomPrices().clear();
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
