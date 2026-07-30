package com.umrah.scanner.cashback.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record SendCashbackRequest(@NotNull @PositiveOrZero BigDecimal amount) {
}
