package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.Trip;

/** companyVisible is true only once the viewing customer has an active lead on this trip. */
public record TripDetailResult(Trip trip, boolean companyVisible) {
}
