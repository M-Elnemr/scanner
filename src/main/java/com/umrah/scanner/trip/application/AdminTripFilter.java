package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.domain.TripTier;
import java.time.LocalDate;
import java.util.UUID;

/** Unlike {@link TripBrowseFilter}, this reaches trips of any status and any company. */
public record AdminTripFilter(
        UUID companyId, TripStatus status, TripTier tier,
        LocalDate departureFrom, LocalDate departureTo, String search) {

    public static final AdminTripFilter NONE = new AdminTripFilter(null, null, null, null, null, null);
}
