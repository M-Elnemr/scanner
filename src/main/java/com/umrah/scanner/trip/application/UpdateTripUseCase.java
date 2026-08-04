package com.umrah.scanner.trip.application;

import com.umrah.scanner.common.exception.ValidationException;
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
public class UpdateTripUseCase {

    private final TripOwnershipGuard tripOwnershipGuard;
    private final TripRepository tripRepository;
    private final TripRouteResolver tripRouteResolver;

    public UpdateTripUseCase(
            TripOwnershipGuard tripOwnershipGuard,
            TripRepository tripRepository,
            TripRouteResolver tripRouteResolver) {
        this.tripOwnershipGuard = tripOwnershipGuard;
        this.tripRepository = tripRepository;
        this.tripRouteResolver = tripRouteResolver;
    }

    @Transactional
    public Trip execute(UUID companyUserId, UUID tripId, UpdateTripCommand command) {
        Trip trip = tripOwnershipGuard.findOwnedTrip(companyUserId, tripId);

        if (trip.getStatus() == TripStatus.CLOSED) {
            throw new ValidationException("A closed trip can no longer be edited");
        }
        if (!command.returnDate().isAfter(command.departureDate())) {
            throw new ValidationException("Return date must be after the departure date");
        }

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
        trip.setTier(command.tier());
        trip.setLastUpdate(Instant.now());

        if (command.hotels() != null) {
            trip.getHotels().clear();
            // Force the orphan-removal deletes to run now, before the inserts below. Otherwise
            // Hibernate flushes inserts first and a replacement row for the same (trip_id, city)
            // hits uq_trip_hotels_trip_city before the old row is gone.
            tripRepository.flush();
            for (TripHotelInput input : command.hotels()) {
                TripHotel hotel = new TripHotel();
                hotel.setCity(input.city());
                hotel.setHotelName(input.hotelName());
                hotel.setStars(input.stars());
                hotel.setDistanceToHaramM(input.distanceToHaramM());
                hotel.setCanWalk(input.canWalk());
                hotel.setFreeBusIncluded(input.freeBusIncluded());
                hotel.setLocationUrl(input.locationUrl());
                trip.addHotel(hotel);
            }
        }
        if (command.roomPrices() != null) {
            trip.getRoomPrices().clear();
            // Same ordering hazard as hotels above, against uq_room_prices_trip_room_type.
            tripRepository.flush();
            for (RoomPriceInput input : command.roomPrices()) {
                RoomPrice roomPrice = new RoomPrice();
                roomPrice.setRoomType(input.roomType());
                roomPrice.setPrice(input.price());
                trip.addRoomPrice(roomPrice);
            }
        }

        // The controller maps the response after this transaction/session closes (open-in-view is
        // off), so any lazy collection it touches must be force-initialized here first.
        trip.getHotels().size();
        trip.getRoomPrices().size();
        return trip;
    }
}
