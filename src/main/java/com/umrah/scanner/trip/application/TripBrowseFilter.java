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
        /** Nights ({@code durationDays}), already converted from the public "days" query params by the controller. */
        Integer minNights,
        Integer maxNights,
        LocalDate departureFrom,
        LocalDate departureTo,
        /** Trips whose organizing company has at least one branch address in this city. */
        UUID companyCityId,
        /** The Egyptian airport the traveler wants to depart from and return to. */
        UUID departureAirportId,
        /** Orders results by the QUAD room price; {@code null} falls through to the combined default with {@link #durationSortDirection}. */
        Sort.Direction priceSortDirection,
        /** Orders results by trip length; {@code null} falls through to the combined default with {@link #priceSortDirection}. */
        Sort.Direction durationSortDirection) {

    public static final TripBrowseFilter NONE =
            new TripBrowseFilter(null, null, null, null, null, null, null, null, null, null, null, null);
}
