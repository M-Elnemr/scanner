package com.umrah.scanner.trip.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyProfile;
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
    private final AuditLogService auditLogService;

    public CreateTripUseCase(
            TripRepository tripRepository,
            CompanyProfileRepository companyProfileRepository,
            TripRouteResolver tripRouteResolver,
            AuditLogService auditLogService) {
        this.tripRepository = tripRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.tripRouteResolver = tripRouteResolver;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Trip execute(UUID companyUserId, CreateTripCommand command) {
        CompanyProfile company = companyProfileRepository.findByUserId(companyUserId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyUserId));

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
        trip.setLastUpdate(Instant.now());

        if (command.hotels() != null) {
            for (TripHotelInput input : command.hotels()) {
                TripHotel hotel = new TripHotel();
                hotel.setCity(input.city());
                hotel.setHotelName(input.hotelName());
                hotel.setStars(input.stars());
                hotel.setDistanceToHaramM(input.distanceToHaramM());
                hotel.setLocationUrl(input.locationUrl());
                trip.addHotel(hotel);
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
        auditLogService.record(companyUserId, "TRIP_CREATED", "Trip", trip.getId(), null, trip.getStatus());
        return trip;
    }
}
