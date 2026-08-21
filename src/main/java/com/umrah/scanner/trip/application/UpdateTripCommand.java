package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.TripTier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateTripCommand(
        String title,
        LocalDate departureDate,
        LocalDate returnDate,
        UUID outboundDepartureAirportId,
        UUID outboundArrivalAirportId,
        UUID returnDepartureAirportId,
        UUID returnArrivalAirportId,
        String airline,
        short daysInMakkah,
        short daysInMadinah,
        boolean visaIncluded,
        boolean transportationIncluded,
        boolean mealsIncluded,
        boolean guideIncluded,
        boolean zamzamIncluded,
        boolean fastTrainIncluded,
        String description,
        UUID currencyId,
        int availableSeats,
        List<TripHotelInput> hotels,
        List<RoomPriceInput> roomPrices,
        TripTier tier,
        BigDecimal commissionPerTraveler) {
}
