package com.umrah.scanner.city.presentation;

import com.umrah.scanner.city.application.CityQueryService;
import com.umrah.scanner.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CityController {

    private final CityQueryService cityQueryService;

    public CityController(CityQueryService cityQueryService) {
        this.cityQueryService = cityQueryService;
    }

    @GetMapping("/api/v1/cities")
    public ApiResponse<List<CityResponse>> list() {
        return ApiResponse.of(cityQueryService.listAll().stream().map(CityResponse::from).toList());
    }
}
