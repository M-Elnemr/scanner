package com.umrah.scanner.favourite.presentation;

import com.umrah.scanner.favourite.domain.Favourite;
import com.umrah.scanner.trip.presentation.TripSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record FavouriteResponse(UUID id, TripSummaryResponse trip, Instant createdAt) {

    public static FavouriteResponse from(Favourite favourite) {
        return new FavouriteResponse(favourite.getId(), TripSummaryResponse.from(favourite.getTrip()), favourite.getCreatedAt());
    }
}
