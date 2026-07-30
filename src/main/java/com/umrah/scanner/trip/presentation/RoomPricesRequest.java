package com.umrah.scanner.trip.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RoomPricesRequest(@NotEmpty @Valid List<RoomPriceRequest> prices) {
}
