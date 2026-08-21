package com.umrah.scanner.trip.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.trip.domain.RoomPrice;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripHotel;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTripUseCase {

    private final TripRepository tripRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final TripRouteResolver tripRouteResolver;
    private final TripHotelResolver tripHotelResolver;
    private final AuditLogService auditLogService;

    public CreateTripUseCase(
            TripRepository tripRepository,
            CompanyProfileRepository companyProfileRepository,
            TripRouteResolver tripRouteResolver,
            TripHotelResolver tripHotelResolver,
            AuditLogService auditLogService) {
        this.tripRepository = tripRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.tripRouteResolver = tripRouteResolver;
        this.tripHotelResolver = tripHotelResolver;
        this.auditLogService = auditLogService;
    }

    /** The company's own trips — resolves the owning company from the authenticated user. */
    @Transactional
    public Trip executeAsCompany(UUID companyUserId, CreateTripCommand command) {
        CompanyProfile company = companyProfileRepository.findByUserId(companyUserId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyUserId));
        return create(company, companyUserId, command);
    }

    /** An admin creating a trip on behalf of any approved company. */
    @Transactional
    public Trip executeAsAdmin(UUID adminUserId, UUID companyId, CreateTripCommand command) {
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyId));
        if (company.getStatus() != CompanyStatus.APPROVED) {
            throw new ValidationException("Trips can only be created for an approved company");
        }
        return create(company, adminUserId, command);
    }

    /**
     * Unchanged body of what used to be the single {@code execute} method, minus resolving the
     * company — {@code actorUserId} is who the audit log attributes the trip to, which is what
     * makes an admin-created trip correctly show the admin, not the company, as its author.
     */
    private Trip create(CompanyProfile company, UUID actorUserId, CreateTripCommand command) {
        if (tripRepository.existsByTripCode(command.tripCode())) {
            throw new ConflictException("Trip code already in use: " + command.tripCode());
        }
        if (!command.returnDate().isAfter(command.departureDate())) {
            throw new ValidationException("Return date must be after the departure date");
        }

        Trip trip = new Trip();
        trip.setCompany(company);
        trip.setTripCode(command.tripCode());
        trip.setTitle(command.title());
        trip.setDepartureDate(command.departureDate());
        trip.setReturnDate(command.returnDate());
        TripRoute route = tripRouteResolver.resolveRoute(
                command.outboundDepartureAirportId(), command.outboundArrivalAirportId(),
                command.returnDepartureAirportId(), command.returnArrivalAirportId());

        trip.setOutboundDepartureAirport(route.outboundDeparture());
        trip.setOutboundArrivalAirport(route.outboundArrival());
        trip.setReturnDepartureAirport(route.returnDeparture());
        trip.setReturnArrivalAirport(route.returnArrival());
        trip.setCurrency(tripRouteResolver.resolveCurrency(command.currencyId()));
        trip.setAirline(command.airline());
        trip.setTransitCount(command.transitCount());
        trip.setTransitCity(command.transitCity());
        trip.setTransitDuration(command.transitDuration());
        trip.setDaysInMakkah(command.daysInMakkah());
        trip.setDaysInMadinah(command.daysInMadinah());
        trip.setVisaIncluded(command.visaIncluded());
        trip.setTransportationIncluded(command.transportationIncluded());
        trip.setMealsIncluded(command.mealsIncluded());
        trip.setGuideIncluded(command.guideIncluded());
        trip.setZamzamIncluded(command.zamzamIncluded());
        trip.setFastTrainIncluded(command.fastTrainIncluded());
        trip.setDescription(command.description());
        trip.setAvailableSeats(command.availableSeats());
        trip.setStatus(TripStatus.PUBLISHED);
        trip.setTier(command.tier());
        trip.setCommissionPerTraveler(command.commissionPerTraveler());
        trip.setLastUpdate(Instant.now());

        if (command.hotels() != null) {
            for (TripHotelResolver.ResolvedTripHotel resolved : tripHotelResolver.resolve(command.hotels())) {
                TripHotel tripHotel = new TripHotel();
                tripHotel.setHotel(resolved.hotel());
                tripHotel.setCity(resolved.hotel().getCity());
                tripHotel.setFreeBusIncluded(resolved.freeBusIncluded());
                trip.addHotel(tripHotel);
            }
        }
        if (command.roomPrices() != null) {
            for (RoomPriceInput input : command.roomPrices()) {
                RoomPrice roomPrice = new RoomPrice();
                roomPrice.setRoomType(input.roomType());
                roomPrice.setPrice(input.price());
                trip.addRoomPrice(roomPrice);
            }
        }

        trip = tripRepository.save(trip);
        auditLogService.record(actorUserId, "TRIP_CREATED", "Trip", trip.getId(), null, trip.getStatus());
        return trip;
    }
}
