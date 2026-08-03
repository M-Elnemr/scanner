package com.umrah.scanner.airport.presentation;

import com.umrah.scanner.airport.application.AirportQueryService;
import com.umrah.scanner.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reference data", description = "Fixed lookup lists: countries, airports, currencies, cities.")
@RestController
public class AirportController {

    private final AirportQueryService airportQueryService;

    public AirportController(AirportQueryService airportQueryService) {
        this.airportQueryService = airportQueryService;
    }

    @Operation(summary = "List airports",
            description = "Public. Pass countryId to scope the list — that is how a trip form offers only "
                    + "Egyptian airports where the itinerary needs one and only Saudi airports where it needs "
                    + "the other. Omit it for the full list.")
    @GetMapping("/api/v1/airports")
    public ApiResponse<List<AirportResponse>> list(@RequestParam(required = false) UUID countryId) {
        return ApiResponse.of(airportQueryService.list(countryId).stream().map(AirportResponse::from).toList());
    }
}
