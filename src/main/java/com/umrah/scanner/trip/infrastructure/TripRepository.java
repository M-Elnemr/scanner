package com.umrah.scanner.trip.infrastructure;

import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TripRepository extends JpaRepository<Trip, UUID>, JpaSpecificationExecutor<Trip> {

    Optional<Trip> findByTripCode(String tripCode);

    boolean existsByTripCode(String tripCode);

    Optional<Trip> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Trip> findAllByCompanyId(UUID companyId, Pageable pageable);

    Page<Trip> findAllByStatus(TripStatus status, Pageable pageable);

    List<Trip> findAllByIdInAndStatus(List<UUID> ids, TripStatus status);
}
