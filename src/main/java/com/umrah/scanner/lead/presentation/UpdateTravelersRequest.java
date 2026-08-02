package com.umrah.scanner.lead.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(name = "UpdateTravelersRequest", description = "Corrected traveler counts. Rejected once the lead reaches DEPOSIT_PAID.")
public record UpdateTravelersRequest(

        @Min(value = 1, message = "At least one adult traveler is required") int adultCount,
        @PositiveOrZero(message = "Child count cannot be negative") int childCount,
        @PositiveOrZero(message = "Infant count cannot be negative") int infantCount) {
}
