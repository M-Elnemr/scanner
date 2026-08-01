package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.domain.TripTier;
import java.math.BigDecimal;
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
        TripStatus status,
        TripTier tier,
        BigDecimal priceStartsFrom) {

    /** @param priceStartsFrom the trip's QUAD (4-bed) room price, or null if none is set yet */
    public static TripSummaryResponse from(Trip trip, BigDecimal priceStartsFrom) {
        return new TripSummaryResponse(
                trip.getId(), trip.getTripCode(), trip.getTitle(), trip.getDepartureDate(), trip.getReturnDate(),
                trip.getAirline(), trip.getDaysInMakkah(), trip.getDaysInMadinah(), trip.getAvailableSeats(),
                trip.getCurrency(), trip.getStatus(), trip.getTier(), priceStartsFrom);
    }
}
