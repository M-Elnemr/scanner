package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.TripStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTripStatusRequest(@NotNull TripStatus status) {
}
