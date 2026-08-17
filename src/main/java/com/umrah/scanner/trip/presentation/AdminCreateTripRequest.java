package com.umrah.scanner.trip.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Everything {@link CreateTripRequest} carries, plus the company an admin is creating it for. */
public record AdminCreateTripRequest(
        @NotNull UUID companyId,
        @Valid @NotNull CreateTripRequest trip) {
}
