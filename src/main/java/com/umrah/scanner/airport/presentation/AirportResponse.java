package com.umrah.scanner.airport.presentation;

import com.umrah.scanner.airport.domain.Airport;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Shared shape: returned by the airport picker and embedded in every trip's itinerary. */
@Schema(name = "Airport", description = "A reference airport, with the country that scopes it.")
public record AirportResponse(
        UUID id,
        @Schema(example = "CAI") String iataCode,
        @Schema(example = "Cairo International Airport") String name,
        @Schema(example = "مطار القاهرة الدولي") String nameAr,
        @Schema(example = "Cairo") String city,
        @Schema(example = "القاهرة") String cityAr,
        UUID countryId,
        @Schema(example = "Egypt") String countryName,
        @Schema(example = "مصر") String countryNameAr,
        @Schema(example = "EG") String countryIso2) {

    public static AirportResponse from(Airport airport) {
        return new AirportResponse(
                airport.getId(), airport.getIataCode(), airport.getName(), airport.getNameAr(),
                airport.getCity(), airport.getCityAr(),
                airport.getCountry().getId(), airport.getCountry().getName(), airport.getCountry().getNameAr(),
                airport.getCountry().getIso2());
    }
}
