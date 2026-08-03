package com.umrah.scanner.trip.infrastructure;

import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Every query whose result is mapped to a response carries {@link Trip#GRAPH_WITH_REFERENCES},
 * which fetch-joins the company, the currency and all four airports in one go. Without it those
 * to-one associations are lazy proxies by the time the controller maps them — open-in-view is off.
 */
public interface TripRepository extends JpaRepository<Trip, UUID>, JpaSpecificationExecutor<Trip> {

    Optional<Trip> findByTripCode(String tripCode);

    boolean existsByTripCode(String tripCode);

    Optional<Trip> findByIdAndCompanyId(UUID id, UUID companyId);

    @EntityGraph(Trip.GRAPH_WITH_REFERENCES)
    Page<Trip> findAllByCompanyId(UUID companyId, Pageable pageable);

    @EntityGraph(Trip.GRAPH_WITH_REFERENCES)
    Page<Trip> findAllByStatus(TripStatus status, Pageable pageable);

    @EntityGraph(Trip.GRAPH_WITH_REFERENCES)
    List<Trip> findAllByIdInAndStatus(List<UUID> ids, TripStatus status);

    /** Redeclared purely to attach the graph — the public browse endpoint runs through this one. */
    @Override
    @EntityGraph(Trip.GRAPH_WITH_REFERENCES)
    Page<Trip> findAll(Specification<Trip> spec, Pageable pageable);

    /**
     * Fetch-joins the to-one references only — hotels and roomPrices are both collections, and
     * Hibernate cannot fetch-join two bags in one query ({@code MultipleBagFetchException}). The
     * caller initializes those two separately (still inside the same transaction, still no lazy
     * exception later) via {@link com.umrah.scanner.trip.application.TripQueryService}.
     */
    @EntityGraph(Trip.GRAPH_WITH_REFERENCES)
    Optional<Trip> findWithDetailsById(UUID id);

    @EntityGraph(Trip.GRAPH_WITH_REFERENCES)
    Optional<Trip> findWithDetailsByIdAndCompanyId(UUID id, UUID companyId);
}
