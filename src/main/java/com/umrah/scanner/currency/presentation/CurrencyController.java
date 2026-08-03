package com.umrah.scanner.currency.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.currency.application.CurrencyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reference data", description = "Fixed lookup lists: countries, airports, currencies, cities.")
@RestController
public class CurrencyController {

    private final CurrencyQueryService currencyQueryService;

    public CurrencyController(CurrencyQueryService currencyQueryService) {
        this.currencyQueryService = currencyQueryService;
    }

    @Operation(summary = "List currencies", description = "Public. A company picks one of these per trip.")
    @GetMapping("/api/v1/currencies")
    public ApiResponse<List<CurrencyResponse>> list() {
        return ApiResponse.of(currencyQueryService.listAll().stream().map(CurrencyResponse::from).toList());
    }
}
