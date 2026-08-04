package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.TripCity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record TripHotelRequest(
        @NotNull TripCity city,
        @NotBlank String hotelName,
        @Min(1) @Max(5) short stars,
        @PositiveOrZero Integer distanceToHaramM,
        boolean canWalk,
        boolean freeBusIncluded,
        String locationUrl) {
}
