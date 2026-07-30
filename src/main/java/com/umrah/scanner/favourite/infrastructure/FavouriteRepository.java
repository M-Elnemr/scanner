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

    @EntityGraph(attributePaths = "trip")
    Page<Favourite> findAllByCustomerId(UUID customerId, Pageable pageable);

    void deleteByCustomerIdAndTripId(UUID customerId, UUID tripId);
}
