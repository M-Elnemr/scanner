package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.RoomType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record RoomPriceRequest(@NotNull RoomType roomType, @NotNull @PositiveOrZero BigDecimal price) {
}
