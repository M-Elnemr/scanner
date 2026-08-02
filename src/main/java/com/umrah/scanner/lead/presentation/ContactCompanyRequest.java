package com.umrah.scanner.lead.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * The party size the customer is enquiring for. Bean validation gives the caller a clean 400 with
 * field errors; {@code TravelerParty} re-checks the same invariants in the domain so they hold no
 * matter which entry point sets them.
 */
@Schema(name = "ContactCompanyRequest", description = "Traveler counts for a new booking request.")
public record ContactCompanyRequest(

        @Schema(description = "Adults travelling; commission and cashback are calculated on this count", example = "2")
        @Min(value = 1, message = "At least one adult traveler is required")
        int adultCount,

        @Schema(description = "Children travelling", example = "1")
        @PositiveOrZero(message = "Child count cannot be negative")
        int childCount,

        @Schema(description = "Infants travelling", example = "0")
        @PositiveOrZero(message = "Infant count cannot be negative")
        int infantCount) {
}
