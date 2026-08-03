package com.umrah.scanner.trip.application;

import com.umrah.scanner.airport.domain.Airport;

/** The four airports of a validated round trip, resolved from ids. */
public record TripRoute(
        Airport outboundDeparture,
        Airport outboundArrival,
        Airport returnDeparture,
        Airport returnArrival) {
}
