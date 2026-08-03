package com.umrah.scanner.trip.domain;

import com.umrah.scanner.airport.domain.Airport;
import com.umrah.scanner.common.exception.ValidationException;

/**
 * What makes four airports a coherent round trip.
 *
 * <p>Expressed against the countries the airports belong to rather than against "Egypt" and "Saudi
 * Arabia" by name. That is the whole reason {@code countries} exists as a table: opening the
 * platform to pilgrims departing from another country needs new airport rows and nothing else — no
 * change here, and no hardcoded name to hunt down.
 *
 * <p>Note what is deliberately <em>not</em> required: that the return departs from the same airport
 * the outbound landed at. Flying in to Jeddah and home from Madinah is a normal Umrah itinerary.
 */
public final class TripRoutePolicy {

    private TripRoutePolicy() {
    }

    public static void validate(
            Airport outboundDeparture, Airport outboundArrival, Airport returnDeparture, Airport returnArrival) {

        var origin = outboundDeparture.getCountry().getId();
        var destination = outboundArrival.getCountry().getId();

        if (origin.equals(destination)) {
            throw new ValidationException("The outbound flight must leave the traveler's country, not land back in it");
        }
        if (!destination.equals(returnDeparture.getCountry().getId())) {
            throw new ValidationException(
                    "The return flight must depart from " + outboundArrival.getCountry().getName()
                            + ", the country the outbound flight arrives in");
        }
        if (!origin.equals(returnArrival.getCountry().getId())) {
            throw new ValidationException(
                    "The return flight must arrive back in " + outboundDeparture.getCountry().getName()
                            + ", the country the trip departs from");
        }
    }
}
