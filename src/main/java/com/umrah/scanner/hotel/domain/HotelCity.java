package com.umrah.scanner.hotel.domain;

/**
 * Was {@code trip.domain.TripCity} until hotels became reference data: a trip depends on a hotel,
 * so the city enum they both need has to live on the hotel side of that relationship or the two
 * packages would depend on each other. Wire values are unchanged, so this is a compile-time-only
 * move — the database CHECK constraints and the JSON on the wire both still say MAKKAH/MADINAH.
 */
public enum HotelCity {
    MAKKAH,
    MADINAH
}
