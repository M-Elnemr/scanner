package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import java.time.LocalDate;
import java.util.UUID;

/** Browse/list shape — company identity is deliberately never included here. */
public record TripSummaryResponse(
        UUID id,
        String tripCode,
        String title,
        LocalDate departureDate,
        LocalDate returnDate,
        String airline,
        short daysInMakkah,
        short daysInMadinah,
        int availableSeats,
        String currency,
        TripStatus status) {

    public static TripSummaryResponse from(Trip trip) {
        return new TripSummaryResponse(
                trip.getId(), trip.getTripCode(), trip.getTitle(), trip.getDepartureDate(), trip.getReturnDate(),
                trip.getAirline(), trip.getDaysInMakkah(), trip.getDaysInMadinah(), trip.getAvailableSeats(),
                trip.getCurrency(), trip.getStatus());
    }
}
