package com.umrah.scanner.trip.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.pricing.application.LeadPricingService;
import com.umrah.scanner.trip.domain.RoomPrice;
import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.infrastructure.RoomPriceRepository;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import com.umrah.scanner.trip.infrastructure.TripSpecifications;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final LeadPricingService leadPricingService;

    public TripQueryService(
            TripRepository tripRepository,
            LeadRepository leadRepository,
            RoomPriceRepository roomPriceRepository,
            LeadPricingService leadPricingService) {
        this.tripRepository = tripRepository;
        this.leadRepository = leadRepository;
        this.roomPriceRepository = roomPriceRepository;
        this.leadPricingService = leadPricingService;
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
        // Suspending a company does not touch its trips, so without this a suspended company's
        // trips stayed reachable through browse. Not applied to getPublicDetail below: a customer
        // who already holds a lead with a since-suspended company must keep reaching that trip by
        // its direct link, matching the same carve-out CompanyQueryService.getVisibleTo documents.
        specs.add(TripSpecifications.companyIsApproved());
        // Same carve-out as companyIsApproved() above: only applied to browse, not to
        // getPublicDetail, so a customer who already holds a lead can still open a now-past
        // trip via its direct link.
        specs.add(TripSpecifications.departureOnOrAfter(LocalDate.now()));
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

    /** The admin console: any company, any status — unlike {@link #browsePublished}, nothing is forced. */
    @Transactional(readOnly = true)
    public Page<Trip> listForAdmin(AdminTripFilter filter, Pageable pageable) {
        List<Specification<Trip>> specs = new ArrayList<>();
        if (filter.companyId() != null) {
            specs.add(TripSpecifications.hasCompanyId(filter.companyId()));
        }
        if (filter.status() != null) {
            specs.add(TripSpecifications.hasStatus(filter.status()));
        }
        if (filter.tier() != null) {
            specs.add(TripSpecifications.hasTierIn(List.of(filter.tier())));
        }
        if (filter.departureFrom() != null || filter.departureTo() != null) {
            specs.add(TripSpecifications.departureBetween(filter.departureFrom(), filter.departureTo()));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            specs.add(TripSpecifications.titleOrCodeContains(filter.search()));
        }
        return tripRepository.findAll(Specification.allOf(specs), pageable);
    }

    /**
     * Company identity is only revealed once the viewing customer has preserved this journey — and
     * it is taken away again if they cancel, which is why a cancelled lead does not count here.
     */
    @Transactional(readOnly = true)
    public TripDetailResult getPublicDetail(UUID tripId, Optional<UUID> viewingCustomerId) {
        Trip trip = tripRepository.findWithDetailsById(tripId).orElseThrow(() -> NotFoundException.of("Trip", tripId));
        initializeCollections(trip);
        boolean companyVisible = viewingCustomerId
                .map(customerId -> leadRepository.findNotCancelledByCustomerIdAndTripId(customerId, tripId).isPresent())
                .orElse(false);
        return new TripDetailResult(trip, companyVisible, cashbackPerTraveler(trip));
    }

    @Transactional(readOnly = true)
    public TripDetailResult getOwnedDetail(UUID companyId, UUID tripId) {
        Trip trip = tripRepository.findWithDetailsByIdAndCompanyId(tripId, companyId)
                .orElseThrow(() -> NotFoundException.of("Trip", tripId));
        initializeCollections(trip);
        return new TripDetailResult(trip, true, cashbackPerTraveler(trip));
    }

    /**
     * The detail view for a trip the caller just created or edited.
     *
     * <p>Re-reads by id rather than taking the returned entity: the write use cases resolve their
     * trip through {@code TripOwnershipGuard}, which does not fetch {@code company}, so that entity
     * comes back with an uninitialized proxy and a detached session. Reading the company off it in
     * the response mapper is what made these endpoints 500. Loading it here, with the company
     * fetch-joined and both collections initialized, keeps the mapping safe after the transaction
     * closes.
     */
    @Transactional(readOnly = true)
    public TripDetailResult ownedDetail(UUID tripId) {
        Trip trip = tripRepository.findWithDetailsById(tripId).orElseThrow(() -> NotFoundException.of("Trip", tripId));
        initializeCollections(trip);
        return new TripDetailResult(trip, true, cashbackPerTraveler(trip));
    }

    /**
     * Runs through the real cashback policies with a party of one, so the figure advertised on the
     * details page can never drift from what a lead created against this trip actually pays out.
     */
    private BigDecimal cashbackPerTraveler(Trip trip) {
        return leadPricingService.cashbackPerTraveler(
                trip.getCompany().getId(), trip.getId(), trip.getCompany().getCommissionPerTraveler());
    }

    /**
     * Hibernate can't fetch-join hotels and roomPrices together (two bags, one query) — each is
     * loaded here with its own simple query instead, while the session is still open, so the
     * controller can safely read both after this transaction commits.
     */
    private void initializeCollections(Trip trip) {
        TripCollectionsInitializer.initialize(trip);
    }
}
