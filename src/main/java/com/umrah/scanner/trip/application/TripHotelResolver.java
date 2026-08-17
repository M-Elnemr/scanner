package com.umrah.scanner.trip.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.hotel.domain.Hotel;
import com.umrah.scanner.hotel.domain.HotelCity;
import com.umrah.scanner.hotel.infrastructure.HotelRepository;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Turns the hotel ids on a create/update command into real catalogue entries, and refuses the
 * request if they do not form a coherent set. Shared by both trip write use cases — same reason as
 * {@link TripRouteResolver}: the rules must not be able to drift between creating a trip and
 * editing one.
 */
@Service
public class TripHotelResolver {

    private final HotelRepository hotelRepository;

    public TripHotelResolver(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    /** @return resolved hotels in input order; the caller copies {@code city} off each hotel. */
    public List<ResolvedTripHotel> resolve(List<TripHotelInput> inputs) {
        List<ResolvedTripHotel> resolved = new ArrayList<>();
        Set<HotelCity> seenCities = EnumSet.noneOf(HotelCity.class);

        for (TripHotelInput input : inputs) {
            if (input.hotelId() == null) {
                throw new ValidationException("hotelId is required for every trip hotel");
            }
            Hotel hotel = hotelRepository.findById(input.hotelId())
                    .orElseThrow(() -> NotFoundException.of("Hotel", input.hotelId()));
            if (!hotel.isActive()) {
                throw new ValidationException("\"" + hotel.getName() + "\" is no longer offered");
            }
            if (!seenCities.add(hotel.getCity())) {
                throw new ValidationException("Only one hotel per city is allowed — " + hotel.getCity() + " is duplicated");
            }
            resolved.add(new ResolvedTripHotel(hotel, input.freeBusIncluded()));
        }
        return resolved;
    }

    public record ResolvedTripHotel(Hotel hotel, boolean freeBusIncluded) {
    }
}
