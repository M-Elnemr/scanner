package com.umrah.scanner.trip.application;

import com.umrah.scanner.hotel.domain.HotelCity;
import java.util.List;

/**
 * A trip's hotel photos for the browse-list card. {@code makkahPhotoUrl}/{@code madinahPhotoUrl}
 * are the original one-per-city shape (either may be null); {@code photos} is every hotel that has
 * a photo, Makkah first then Madinah, for a client that wants to show more than one per city (e.g.
 * a swipeable gallery) instead of collapsing straight to the first hotel per city.
 */
public record TripHotelPhotos(String makkahPhotoUrl, String madinahPhotoUrl, List<HotelPhoto> photos) {

    public static final TripHotelPhotos EMPTY = new TripHotelPhotos(null, null, List.of());

    public record HotelPhoto(HotelCity city, String hotelName, String hotelNameAr, String photoUrl) {
    }
}
