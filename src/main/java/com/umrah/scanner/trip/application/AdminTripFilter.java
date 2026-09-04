package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.domain.TripTier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Unlike {@link TripBrowseFilter}, this reaches trips of any status and any company. The
 * price/room-size/duration/city/airport fields mirror {@link TripBrowseFilter}'s so the admin
 * console can filter on everything a customer can, plus its own status/company/search — see
 * {@code TripQueryService#listForAdmin}.
 */
public record AdminTripFilter(
        UUID companyId, TripStatus status, TripTier tier,
        LocalDate departureFrom, LocalDate departureTo, String search,
        RoomType priceRoomType, BigDecimal minPrice, BigDecimal maxPrice,
        /** Nights ({@code durationDays}), already converted from the "days" query params by the controller. */
        Integer minNights, Integer maxNights,
        /** Trips whose organizing company has at least one branch address in this city. */
        UUID companyCityId,
        UUID departureAirportId) {

    public static final AdminTripFilter NONE = new AdminTripFilter(
            null, null, null, null, null, null, null, null, null, null, null, null, null);
}
