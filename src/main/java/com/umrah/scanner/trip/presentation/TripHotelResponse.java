package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.TripCity;
import com.umrah.scanner.trip.domain.TripHotel;

public record TripHotelResponse(TripCity city, String hotelName, short stars, Integer distanceToHaramM, String locationUrl) {

    public static TripHotelResponse from(TripHotel hotel) {
        return new TripHotelResponse(hotel.getCity(), hotel.getHotelName(), hotel.getStars(), hotel.getDistanceToHaramM(), hotel.getLocationUrl());
    }
}
