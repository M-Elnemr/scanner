package com.umrah.scanner.city.presentation;

import com.umrah.scanner.city.domain.City;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "City", description = "An Egyptian governorate. Used to scope a company address.")
public record CityResponse(
        UUID id,
        @Schema(example = "Cairo") String name,
        @Schema(example = "القاهرة") String nameAr) {

    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getNameAr());
    }
}
