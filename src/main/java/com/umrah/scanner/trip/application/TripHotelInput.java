package com.umrah.scanner.trip.application;

import java.util.UUID;

public record TripHotelInput(UUID hotelId, boolean freeBusIncluded) {
}
