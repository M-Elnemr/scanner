package com.umrah.scanner.hotel.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** No {@code city} field — see {@code UpdateHotelCommand} for why it is immutable after creation. */
public record UpdateHotelRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String nameAr,
        @Min(1) @Max(5) short stars,
        @PositiveOrZero Integer distanceToHaramM,
        boolean canWalk,
        boolean freeBusIncluded,
        String locationUrl,
        Double latitude,
        Double longitude,
        boolean active) {
}
