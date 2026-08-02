package com.umrah.scanner.city.presentation;

import com.umrah.scanner.city.domain.City;
import java.util.UUID;

public record CityResponse(UUID id, String name) {

    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getName());
    }
}
