package com.umrah.scanner.hotel.application;

/**
 * No {@code city} field, deliberately: {@code trip_hotels} carries a composite foreign key on
 * {@code (hotel_id, city)} precisely so a trip's hotel can never disagree with its own city.
 * Changing a hotel's city out from under trips already using it would violate that FK the moment any
 * exist, and is nonsensical anyway — a hotel that moved city is a different hotel. If a hotel was
 * miscategorized before any trip used it, delete it and recreate it correctly.
 */
public record UpdateHotelCommand(
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
