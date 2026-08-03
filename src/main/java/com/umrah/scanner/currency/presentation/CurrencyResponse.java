package com.umrah.scanner.currency.presentation;

import com.umrah.scanner.currency.domain.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Shared shape: returned by the currency picker and embedded in every trip. */
@Schema(name = "Currency", description = "A currency a trip can be priced in.")
public record CurrencyResponse(
        UUID id,
        @Schema(example = "EGP") String code,
        @Schema(example = "Egyptian Pound") String name,
        @Schema(example = "E£") String symbol) {

    public static CurrencyResponse from(Currency currency) {
        return new CurrencyResponse(currency.getId(), currency.getCode(), currency.getName(), currency.getSymbol());
    }
}
