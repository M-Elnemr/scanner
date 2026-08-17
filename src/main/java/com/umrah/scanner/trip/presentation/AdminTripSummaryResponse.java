package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.domain.TripTier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

/**
 * {@link TripSummaryResponse} deliberately never carries company identity — it backs public browse.
 * The admin console is the one place that identity belongs on a list row, so this is a separate
 * shape rather than a nullable field bolted onto the public one.
 */
@Schema(name = "AdminTripSummary", description = "A trip row in the admin console — includes the owning company.")
public record AdminTripSummaryResponse(
        UUID id,
        String tripCode,
        String title,
        UUID companyId,
        String companyName,
        LocalDate departureDate,
        LocalDate returnDate,
        TripStatus status,
        TripTier tier,
        int availableSeats) {

    public static AdminTripSummaryResponse from(Trip trip) {
        return new AdminTripSummaryResponse(
                trip.getId(), trip.getTripCode(), trip.getTitle(),
                trip.getCompany().getId(), trip.getCompany().getCompanyName(),
                trip.getDepartureDate(), trip.getReturnDate(), trip.getStatus(), trip.getTier(), trip.getAvailableSeats());
    }
}
