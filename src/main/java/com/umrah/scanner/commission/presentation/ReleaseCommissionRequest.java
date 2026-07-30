package com.umrah.scanner.commission.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ReleaseCommissionRequest(@NotNull @PositiveOrZero BigDecimal amount) {
}
