package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.airport.presentation.AirportResponse;
import com.umrah.scanner.currency.presentation.CurrencyResponse;
import com.umrah.scanner.trip.application.TripHotelPhotos;
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
        AirportResponse outboundDepartureAirport,
        AirportResponse outboundArrivalAirport,
        short daysInMakkah,
        short daysInMadinah,
        int availableSeats,
        CurrencyResponse currency,
        TripStatus status,
        TripTier tier,
        BigDecimal priceStartsFrom,
        String makkahHotelPhotoUrl,
        String madinahHotelPhotoUrl) {

    /**
     * @param priceStartsFrom the trip's QUAD (4-bed) room price, or null if none is set yet
     * @param hotelPhotos     the trip's Makkah/Madinah hotel photos, or {@link TripHotelPhotos#EMPTY}
     */
    public static TripSummaryResponse from(Trip trip, BigDecimal priceStartsFrom, TripHotelPhotos hotelPhotos) {
        return new TripSummaryResponse(
                trip.getId(), trip.getTripCode(), trip.getTitle(), trip.getDepartureDate(), trip.getReturnDate(),
                trip.getAirline(),
                AirportResponse.from(trip.getOutboundDepartureAirport()),
                AirportResponse.from(trip.getOutboundArrivalAirport()),
                trip.getDaysInMakkah(), trip.getDaysInMadinah(), trip.getAvailableSeats(),
                CurrencyResponse.from(trip.getCurrency()), trip.getStatus(), trip.getTier(), priceStartsFrom,
                hotelPhotos.makkahPhotoUrl(), hotelPhotos.madinahPhotoUrl());
    }
}
