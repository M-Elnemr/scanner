package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.TripCity;
import com.umrah.scanner.trip.domain.TripHotel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TripHotel", description = "A hotel in Makkah or Madinah used on this trip.")
public record TripHotelResponse(
        TripCity city,
        String hotelName,
        short stars,
        @Schema(description = "Distance to the Haram (Makkah) or the Prophet's Mosque (Madinah)") Integer distanceToHaramM,
        @Schema(description = "Whether that distance is walkable") boolean canWalk,
        @Schema(description = "Whether the company provides a free shuttle bus to the Haram/Mosque") boolean freeBusIncluded,
        String locationUrl) {

    public static TripHotelResponse from(TripHotel hotel) {
        return new TripHotelResponse(
                hotel.getCity(), hotel.getHotelName(), hotel.getStars(), hotel.getDistanceToHaramM(),
                hotel.isCanWalk(), hotel.isFreeBusIncluded(), hotel.getLocationUrl());
    }
}
