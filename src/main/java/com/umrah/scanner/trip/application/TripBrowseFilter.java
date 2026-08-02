package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.TripTier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** All fields optional — an unset field applies no filtering on that dimension. */
public record TripBrowseFilter(
        List<TripTier> tiers,
        RoomType priceRoomType,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minDays,
        Integer maxDays,
        LocalDate departureFrom,
        LocalDate departureTo) {

    public static final TripBrowseFilter NONE = new TripBrowseFilter(null, null, null, null, null, null, null, null);
}
