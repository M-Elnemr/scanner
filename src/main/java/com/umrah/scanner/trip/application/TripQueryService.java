package com.umrah.scanner.trip.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.trip.domain.RoomPrice;
import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.infrastructure.RoomPriceRepository;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import com.umrah.scanner.trip.infrastructure.TripSpecifications;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripQueryService {

    private final TripRepository tripRepository;
    private final LeadRepository leadRepository;
    private final RoomPriceRepository roomPriceRepository;

    public TripQueryService(TripRepository tripRepository, LeadRepository leadRepository, RoomPriceRepository roomPriceRepository) {
        this.tripRepository = tripRepository;
        this.leadRepository = leadRepository;
        this.roomPriceRepository = roomPriceRepository;
    }

    /** "Price starts from" — the QUAD (4-bed) room price is the cheapest per-person rate we sell. */
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> priceStartsFrom(List<UUID> tripIds) {
        if (tripIds.isEmpty()) {
            return Map.of();
        }
        return roomPriceRepository.findAllByTripIdInAndRoomType(tripIds, RoomType.QUAD).stream()
                .collect(Collectors.toMap(rp -> rp.getTrip().getId(), RoomPrice::getPrice));
    }

    @Transactional(readOnly = true)
    public Page<Trip> browsePublished(TripBrowseFilter filter, Pageable pageable) {
        List<Specification<Trip>> specs = new ArrayList<>();
        specs.add(TripSpecifications.hasStatus(TripStatus.PUBLISHED));
        if (filter.tiers() != null && !filter.tiers().isEmpty()) {
            specs.add(TripSpecifications.hasTierIn(filter.tiers()));
        }
        if (filter.departureFrom() != null || filter.departureTo() != null) {
            specs.add(TripSpecifications.departureBetween(filter.departureFrom(), filter.departureTo()));
        }
        if (filter.minDays() != null || filter.maxDays() != null) {
            specs.add(TripSpecifications.durationBetween(filter.minDays(), filter.maxDays()));
        }
        if (filter.minPrice() != null || filter.maxPrice() != null) {
            RoomType roomType = filter.priceRoomType() != null ? filter.priceRoomType() : RoomType.QUAD;
            specs.add(TripSpecifications.priceForRoomTypeBetween(roomType, filter.minPrice(), filter.maxPrice()));
        }
        return tripRepository.findAll(Specification.allOf(specs), pageable);
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
