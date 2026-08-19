package com.umrah.scanner.hotel.application;

import com.umrah.scanner.hotel.domain.HotelCity;

/** Shared by create and update — a hotel has no fields that are only settable once. */
public record HotelCommand(
        HotelCity city,
        String name,
        String nameAr,
        short stars,
        Integer distanceToHaramM,
        boolean canWalk,
        String locationUrl,
        Double latitude,
        Double longitude,
        boolean active) {
}
