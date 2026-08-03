package com.umrah.scanner.airport.application;

import com.umrah.scanner.airport.domain.Airport;
import com.umrah.scanner.airport.infrastructure.AirportRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AirportQueryService {

    private final AirportRepository airportRepository;

    public AirportQueryService(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    /**
     * @param countryId optional; when supplied, the list is scoped to that country. This is what lets
     *                  a client show only Saudi airports where the itinerary requires one and only
     *                  Egyptian airports where it requires the other.
     */
    @Transactional(readOnly = true)
    public List<Airport> list(UUID countryId) {
        return countryId == null
                ? airportRepository.findAllByOrderByCityAsc()
                : airportRepository.findAllByCountryIdOrderByCityAsc(countryId);
    }
}
