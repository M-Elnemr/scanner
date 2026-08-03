package com.umrah.scanner.country.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.country.application.CountryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reference data", description = "Fixed lookup lists: countries, airports, currencies, cities.")
@RestController
public class CountryController {

    private final CountryQueryService countryQueryService;

    public CountryController(CountryQueryService countryQueryService) {
        this.countryQueryService = countryQueryService;
    }

    @Operation(summary = "List countries", description = "Public. Use the returned id to scope /api/v1/airports.")
    @GetMapping("/api/v1/countries")
    public ApiResponse<List<CountryResponse>> list() {
        return ApiResponse.of(countryQueryService.listAll().stream().map(CountryResponse::from).toList());
    }
}
