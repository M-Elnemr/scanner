package com.umrah.scanner.analytics.application;

import java.util.UUID;

/** A most-viewed-trips row with its title resolved — the admin-facing shape. */
public record TripViewCount(UUID tripId, String tripTitle, long count) {
}
