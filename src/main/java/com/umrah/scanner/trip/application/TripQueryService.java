package com.umrah.scanner.trip.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripQueryService {

    private final TripRepository tripRepository;
    private final LeadRepository leadRepository;

    public TripQueryService(TripRepository tripRepository, LeadRepository leadRepository) {
        this.tripRepository = tripRepository;
        this.leadRepository = leadRepository;
    }

    @Transactional(readOnly = true)
    public Page<Trip> browsePublished(Pageable pageable) {
        return tripRepository.findAllByStatus(TripStatus.PUBLISHED, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Trip> listForCompany(UUID companyId, Pageable pageable) {
        return tripRepository.findAllByCompanyId(companyId, pageable);
    }

    /** Company identity is only revealed once the viewing customer already has a lead on this trip. */
    @Transactional(readOnly = true)
    public TripDetailResult getPublicDetail(UUID tripId, Optional<UUID> viewingCustomerId) {
        Trip trip = tripRepository.findWithDetailsById(tripId).orElseThrow(() -> NotFoundException.of("Trip", tripId));
        initializeCollections(trip);
        boolean companyVisible = viewingCustomerId
                .map(customerId -> leadRepository.findByCustomerIdAndTripId(customerId, tripId).isPresent())
                .orElse(false);
        return new TripDetailResult(trip, companyVisible);
    }

    @Transactional(readOnly = true)
    public Trip getOwnedDetail(UUID companyId, UUID tripId) {
        Trip trip = tripRepository.findWithDetailsByIdAndCompanyId(tripId, companyId)
                .orElseThrow(() -> NotFoundException.of("Trip", tripId));
        initializeCollections(trip);
        return trip;
    }

    /**
     * Hibernate can't fetch-join hotels and roomPrices together (two bags, one query) — each is
     * loaded here with its own simple query instead, while the session is still open, so the
     * controller can safely read both after this transaction commits.
     */
    private void initializeCollections(Trip trip) {
        Hibernate.initialize(trip.getHotels());
        Hibernate.initialize(trip.getRoomPrices());
    }
}
