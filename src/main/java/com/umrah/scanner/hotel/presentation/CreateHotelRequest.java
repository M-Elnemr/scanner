package com.umrah.scanner.hotel.presentation;

import com.umrah.scanner.hotel.domain.HotelCity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateHotelRequest(
        @NotNull HotelCity city,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String nameAr,
        @Min(1) @Max(5) short stars,
        @PositiveOrZero Integer distanceToHaramM,
        boolean canWalk,
        String locationUrl,
        Double latitude,
        Double longitude,
        boolean active) {
}
