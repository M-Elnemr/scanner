package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.hotel.domain.HotelCity;
import com.umrah.scanner.hotel.presentation.HotelResponse;
import com.umrah.scanner.trip.domain.TripHotel;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * BREAKING vs. the pre-catalogue shape: the hotel's own facts (name, stars, distance, walkability,
 * free shuttle, map link) are now nested entirely under {@code hotel}. {@code city} is the only
 * thing left here, kept for convenience even though it always matches {@code hotel.city}.
 */
@Schema(name = "TripHotel", description = "The catalogue hotel this trip uses in one city.")
public record TripHotelResponse(HotelCity city, HotelResponse hotel) {

    public static TripHotelResponse from(TripHotel tripHotel) {
        return new TripHotelResponse(tripHotel.getCity(), HotelResponse.from(tripHotel.getHotel()));
    }
}
