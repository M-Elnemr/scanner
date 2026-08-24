package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.TripTier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;

/** All fields optional — an unset field applies no filtering on that dimension. */
public record TripBrowseFilter(
        List<TripTier> tiers,
        RoomType priceRoomType,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minDays,
        Integer maxDays,
        LocalDate departureFrom,
        LocalDate departureTo,
        /** Trips whose organizing company has at least one branch address in this city. */
        UUID companyCityId,
        /** The Egyptian airport the traveler wants to depart from and return to. */
        UUID departureAirportId,
        /** Orders results by the QUAD room price; {@code null} leaves the default (unordered) result order. */
        Sort.Direction priceSortDirection) {

    public static final TripBrowseFilter NONE =
            new TripBrowseFilter(null, null, null, null, null, null, null, null, null, null, null);
}
