package com.umrah.scanner.analytics.application;

import java.util.UUID;

/**
 * One row of the most-viewed-trips aggregate, before the trip's title is resolved — see
 * {@link AnalyticsReportingService#mostViewedTrips}, which turns this into a {@link TripViewCount}.
 */
public record MostViewedTrip(UUID tripId, long count) {
}
