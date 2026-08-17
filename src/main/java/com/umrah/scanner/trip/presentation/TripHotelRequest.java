package com.umrah.scanner.trip.presentation;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * City is not sent — it is derived from the picked hotel, so a caller cannot claim a Makkah hotel is
 * in Madinah. {@link com.umrah.scanner.trip.application.TripHotelResolver} enforces one hotel per
 * city and that the hotel is active.
 */
public record TripHotelRequest(@NotNull UUID hotelId, boolean freeBusIncluded) {
}
