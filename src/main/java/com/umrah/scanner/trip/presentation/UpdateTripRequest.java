package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.TripTier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateTripRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull LocalDate departureDate,
        @NotNull LocalDate returnDate,
        @NotNull(message = "Outbound departure airport is required") UUID outboundDepartureAirportId,
        @NotNull(message = "Outbound arrival airport is required") UUID outboundArrivalAirportId,
        @NotNull(message = "Return departure airport is required") UUID returnDepartureAirportId,
        @NotNull(message = "Return arrival airport is required") UUID returnArrivalAirportId,
        @NotBlank String airline,
        short daysInMakkah,
        short daysInMadinah,
        boolean visaIncluded,
        boolean transportationIncluded,
        boolean mealsIncluded,
        boolean guideIncluded,
        boolean zamzamIncluded,
        boolean fastTrainIncluded,
        String description,
        @NotNull(message = "Currency is required") UUID currencyId,
        @PositiveOrZero int availableSeats,
        @Valid List<TripHotelRequest> hotels,
        @Valid List<RoomPriceRequest> prices,
        @NotNull TripTier tier,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal commissionPerTraveler) {
}
