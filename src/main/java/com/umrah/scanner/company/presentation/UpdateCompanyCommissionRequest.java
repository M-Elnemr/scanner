package com.umrah.scanner.company.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Schema(name = "UpdateCompanyCommissionRequest", description = "Admin-only. Applies to leads created after the change.")
public record UpdateCompanyCommissionRequest(

        @Schema(description = "EGP the company pays the platform per billable traveler", example = "2000.00")
        @NotNull
        @PositiveOrZero(message = "Commission per traveler must be zero or positive")
        @Digits(integer = 8, fraction = 2)
        BigDecimal commissionPerTraveler) {
}
