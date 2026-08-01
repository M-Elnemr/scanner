package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.TripTier;
import java.time.LocalDate;
import java.util.List;

public record CreateTripCommand(
        String tripCode,
        String title,
        LocalDate departureDate,
        LocalDate returnDate,
        String departureAirport,
        String arrivalAirport,
        String airline,
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
        String currency,
        int availableSeats,
        List<TripHotelInput> hotels,
        List<RoomPriceInput> roomPrices,
        TripTier tier) {
}
