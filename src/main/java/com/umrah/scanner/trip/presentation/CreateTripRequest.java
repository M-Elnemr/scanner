package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.TripTier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateTripRequest(
        @NotBlank @Size(max = 50) String tripCode,
        @NotBlank @Size(max = 255) String title,
        @NotNull LocalDate departureDate,
        @NotNull LocalDate returnDate,
        @NotBlank String departureAirport,
        @NotBlank String arrivalAirport,
        @NotBlank String airline,
        String flightNumber,
        short transitCount,
        String transitCity,
        String transitDuration,
        short daysInMakkah,
        short daysInMadinah,
        boolean visaIncluded,
        boolean transportationIncluded,
        boolean mealsIncluded,
        boolean guideIncluded,
        boolean zamzamIncluded,
        String description,
        @Size(min = 3, max = 3) String currency,
        @PositiveOrZero int availableSeats,
        @Valid List<TripHotelRequest> hotels,
        @Valid List<RoomPriceRequest> prices,
        @NotNull TripTier tier) {
}
