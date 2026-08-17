package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.hotel.domain.HotelCity;
import com.umrah.scanner.hotel.presentation.HotelResponse;
import com.umrah.scanner.trip.domain.TripHotel;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * BREAKING vs. the pre-catalogue shape: the hotel's own facts (name, stars, distance, walkability,
 * map link) are now nested under {@code hotel} instead of sitting flat on this record. Only
 * {@code city} and {@code freeBusIncluded} remain here, because they are the two things that are
 * still genuinely about this trip rather than about the hotel.
 */
@Schema(name = "TripHotel", description = "The catalogue hotel this trip uses in one city, plus what this company offers around it.")
public record TripHotelResponse(
        HotelCity city,
        HotelResponse hotel,
        @Schema(description = "Whether THIS company runs a free shuttle to the Haram/Mosque on THIS package")
        boolean freeBusIncluded) {

    public static TripHotelResponse from(TripHotel tripHotel) {
        return new TripHotelResponse(tripHotel.getCity(), HotelResponse.from(tripHotel.getHotel()), tripHotel.isFreeBusIncluded());
    }
}
