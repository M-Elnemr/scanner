package com.umrah.scanner.favourite.infrastructure;

import com.umrah.scanner.favourite.domain.Favourite;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavouriteRepository extends JpaRepository<Favourite, UUID> {

    boolean existsByCustomerIdAndTripId(UUID customerId, UUID tripId);

    Optional<Favourite> findByCustomerIdAndTripId(UUID customerId, UUID tripId);

    /**
     * Fetches everything {@code TripSummaryResponse} walks, not just the trip: the response is
     * mapped after the session closes, and it reads the trip's currency, its two outbound airports
     * and each airport's country. Naming only "trip" leaves those as proxies that blow up at
     * mapping time.
     */
    @EntityGraph(attributePaths = {
            "trip",
            "trip.currency",
            "trip.outboundDepartureAirport",
            "trip.outboundDepartureAirport.country",
            "trip.outboundArrivalAirport",
            "trip.outboundArrivalAirport.country"})
    Page<Favourite> findAllByCustomerId(UUID customerId, Pageable pageable);

    void deleteByCustomerIdAndTripId(UUID customerId, UUID tripId);
}
