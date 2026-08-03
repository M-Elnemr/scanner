package com.umrah.scanner.trip.application;

import com.umrah.scanner.airport.domain.Airport;
import com.umrah.scanner.airport.infrastructure.AirportRepository;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.currency.domain.Currency;
import com.umrah.scanner.currency.infrastructure.CurrencyRepository;
import com.umrah.scanner.trip.domain.TripRoutePolicy;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Turns the ids on a create/update command into real reference entities, and refuses the request if
 * they do not form a coherent round trip. Shared by both write use cases so the itinerary rules
 * cannot drift between creating a trip and editing one.
 */
@Service
public class TripRouteResolver {

    private final AirportRepository airportRepository;
    private final CurrencyRepository currencyRepository;

    public TripRouteResolver(AirportRepository airportRepository, CurrencyRepository currencyRepository) {
        this.airportRepository = airportRepository;
        this.currencyRepository = currencyRepository;
    }

    public TripRoute resolveRoute(
            UUID outboundDepartureId, UUID outboundArrivalId, UUID returnDepartureId, UUID returnArrivalId) {

        Airport outboundDeparture = airport(outboundDepartureId, "outboundDepartureAirportId");
        Airport outboundArrival = airport(outboundArrivalId, "outboundArrivalAirportId");
        Airport returnDeparture = airport(returnDepartureId, "returnDepartureAirportId");
        Airport returnArrival = airport(returnArrivalId, "returnArrivalAirportId");

        TripRoutePolicy.validate(outboundDeparture, outboundArrival, returnDeparture, returnArrival);
        return new TripRoute(outboundDeparture, outboundArrival, returnDeparture, returnArrival);
    }

    public Currency resolveCurrency(UUID currencyId) {
        if (currencyId == null) {
            throw new ValidationException("currencyId is required");
        }
        return currencyRepository.findById(currencyId)
                .orElseThrow(() -> NotFoundException.of("Currency", currencyId));
    }

    /** Fetches the country alongside the airport — {@link TripRoutePolicy} reads it, and so does the response. */
    private Airport airport(UUID id, String field) {
        if (id == null) {
            throw new ValidationException(field + " is required");
        }
        return airportRepository.findWithCountryById(id).orElseThrow(() -> NotFoundException.of("Airport", id));
    }
}
