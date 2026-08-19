package com.umrah.scanner.trip.application;

/** A trip's Makkah/Madinah hotel photos, for the browse-list card — either may be null. */
public record TripHotelPhotos(String makkahPhotoUrl, String madinahPhotoUrl) {

    public static final TripHotelPhotos EMPTY = new TripHotelPhotos(null, null);
}
