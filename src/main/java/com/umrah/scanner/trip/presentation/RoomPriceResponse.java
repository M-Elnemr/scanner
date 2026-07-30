package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.trip.domain.RoomPrice;
import com.umrah.scanner.trip.domain.RoomType;
import java.math.BigDecimal;

public record RoomPriceResponse(RoomType roomType, BigDecimal price) {

    public static RoomPriceResponse from(RoomPrice roomPrice) {
        return new RoomPriceResponse(roomPrice.getRoomType(), roomPrice.getPrice());
    }
}
