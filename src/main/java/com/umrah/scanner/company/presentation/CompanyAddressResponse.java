package com.umrah.scanner.company.presentation;

import com.umrah.scanner.company.domain.CompanyAddress;
import java.util.UUID;

public record CompanyAddressResponse(UUID id, UUID cityId, String cityName, String addressText, String mobileNumber) {

    public static CompanyAddressResponse from(CompanyAddress address) {
        return new CompanyAddressResponse(
                address.getId(), address.getCity().getId(), address.getCity().getName(),
                address.getAddressText(), address.getMobileNumber());
    }
}
