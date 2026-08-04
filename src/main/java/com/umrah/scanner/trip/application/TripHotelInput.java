package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.TripCity;

public record TripHotelInput(
        TripCity city,
        String hotelName,
        short stars,
        Integer distanceToHaramM,
        boolean canWalk,
        boolean freeBusIncluded,
        String locationUrl) {
}
