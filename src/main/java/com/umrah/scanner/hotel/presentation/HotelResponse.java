package com.umrah.scanner.hotel.presentation;

import com.umrah.scanner.hotel.domain.Hotel;
import com.umrah.scanner.hotel.domain.HotelCity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "Hotel", description = "A catalogue hotel. Embedded in a trip's detail and offered by the picker.")
public record HotelResponse(
        UUID id,
        HotelCity city,
        String name,
        @Schema(description = "Null when not yet translated") String nameAr,
        short stars,
        @Schema(description = "Distance to the Haram (Makkah) or the Prophet's Mosque (Madinah)") Integer distanceToHaramM,
        @Schema(description = "Whether that distance is walkable — an admin judgement, not a computed threshold") boolean canWalk,
        String locationUrl,
        @Schema(description = "false once retired from the picker; trips already using it are unaffected") boolean active) {

    public static HotelResponse from(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(), hotel.getCity(), hotel.getName(), hotel.getNameAr(), hotel.getStars(),
                hotel.getDistanceToHaramM(), hotel.isCanWalk(), hotel.getLocationUrl(), hotel.isActive());
    }
}
