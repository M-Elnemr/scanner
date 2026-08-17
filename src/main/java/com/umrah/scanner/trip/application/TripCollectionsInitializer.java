package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.Trip;
import org.hibernate.Hibernate;

/**
 * Every caller that maps a {@link Trip} to a response after its transaction closes (open-in-view is
 * off everywhere in this app) must force-initialize its lazy collections first, hotels' nested
 * {@code Hotel} included — otherwise {@code TripHotelResponse.from} dereferences an uninitialized
 * proxy and throws. Centralized here after the trip-hotel-catalogue change added a second lazy hop
 * to the hotels collection, which is easy to forget at any one of the several call sites.
 */
final class TripCollectionsInitializer {

    private TripCollectionsInitializer() {
    }

    static void initialize(Trip trip) {
        Hibernate.initialize(trip.getHotels());
        trip.getHotels().forEach(tripHotel -> Hibernate.initialize(tripHotel.getHotel()));
        Hibernate.initialize(trip.getRoomPrices());
    }
}
