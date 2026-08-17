package com.umrah.scanner.trip.presentation;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReassignTripCompanyRequest(@NotNull UUID companyId) {
}
