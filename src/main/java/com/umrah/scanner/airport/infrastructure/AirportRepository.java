package com.umrah.scanner.airport.infrastructure;

import com.umrah.scanner.airport.domain.Airport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AirportRepository extends JpaRepository<Airport, UUID> {

    /** country is always mapped into the response, so it is fetched up front rather than lazily per row. */
    @EntityGraph(attributePaths = "country")
    List<Airport> findAllByOrderByCityAsc();

    @EntityGraph(attributePaths = "country")
    List<Airport> findAllByCountryIdOrderByCityAsc(UUID countryId);

    @EntityGraph(attributePaths = "country")
    Optional<Airport> findWithCountryById(UUID id);

    Optional<Airport> findByIataCode(String iataCode);
}
