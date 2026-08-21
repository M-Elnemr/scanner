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
    private final TripHotelResolver tripHotelResolver;

    public UpdateTripUseCase(
            TripOwnershipGuard tripOwnershipGuard,
            TripRepository tripRepository,
            TripRouteResolver tripRouteResolver,
            TripHotelResolver tripHotelResolver) {
        this.tripOwnershipGuard = tripOwnershipGuard;
        this.tripRepository = tripRepository;
        this.tripRouteResolver = tripRouteResolver;
        this.tripHotelResolver = tripHotelResolver;
    }

    @Transactional
    public Trip executeAsCompany(UUID companyUserId, UUID tripId, UpdateTripCommand command) {
        return update(tripOwnershipGuard.findOwnedTrip(companyUserId, tripId), command, false);
    }

    /** Admin bypasses ownership only — every data-integrity rule below still applies. */
    @Transactional
    public Trip executeAsAdmin(UUID adminUserId, UUID tripId, UpdateTripCommand command) {
        return update(tripOwnershipGuard.findAnyTrip(tripId), command, true);
    }

    /**
     * {@code allowCommissionOverride} is false on the company's own path — same reasoning as
     * {@link CreateTripUseCase}: a company that could edit its own trip's commission override would
     * be able to undercut the platform's rate at will.
     */
    private Trip update(Trip trip, UpdateTripCommand command, boolean allowCommissionOverride) {
        if (trip.getStatus() == TripStatus.CLOSED) {
            throw new ValidationException("A closed trip can no longer be edited");
        }
        if (!command.returnDate().isAfter(command.departureDate())) {
            throw new ValidationException("Return date must be after the departure date");
        }
        if (!allowCommissionOverride && command.commissionPerTraveler() != null) {
            throw new ValidationException("Only an admin can set a trip's commission override");
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
        // A company's own update never carries this field (the guard above already rejected it if
        // it tried to) — leave whatever an admin last set completely alone, rather than nulling it
        // out just because the company's request understandably omitted it.
        if (allowCommissionOverride) {
            trip.setCommissionPerTraveler(command.commissionPerTraveler());
        }
        trip.setLastUpdate(Instant.now());

        if (command.hotels() != null) {
            trip.getHotels().clear();
            // Force the orphan-removal deletes to run now, before the inserts below. Otherwise
            // Hibernate flushes inserts first and a replacement row for the same (trip_id, city)
            // hits uq_trip_hotels_trip_city before the old row is gone.
            tripRepository.flush();
            for (TripHotelResolver.ResolvedTripHotel resolved : tripHotelResolver.resolve(command.hotels())) {
                TripHotel tripHotel = new TripHotel();
                tripHotel.setHotel(resolved.hotel());
                tripHotel.setCity(resolved.hotel().getCity());
                tripHotel.setFreeBusIncluded(resolved.freeBusIncluded());
                trip.addHotel(tripHotel);
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
        TripCollectionsInitializer.initialize(trip);
        return trip;
    }
}
