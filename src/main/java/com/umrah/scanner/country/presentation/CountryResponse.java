package com.umrah.scanner.country.presentation;

import com.umrah.scanner.country.domain.Country;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "Country", description = "Reference country. Used to scope the airport picker.")
public record CountryResponse(UUID id, String name, @Schema(example = "EG") String iso2) {

    public static CountryResponse from(Country country) {
        return new CountryResponse(country.getId(), country.getName(), country.getIso2());
    }
}
