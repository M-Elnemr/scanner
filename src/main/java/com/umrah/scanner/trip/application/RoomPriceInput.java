package com.umrah.scanner.trip.application;

import com.umrah.scanner.trip.domain.RoomType;
import java.math.BigDecimal;

public record RoomPriceInput(RoomType roomType, BigDecimal price) {
}
