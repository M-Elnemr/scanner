package com.umrah.scanner.company.application;

import com.umrah.scanner.city.domain.City;
import com.umrah.scanner.city.infrastructure.CityRepository;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyAddress;
import com.umrah.scanner.company.domain.CompanyProfile;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The address-building half of {@link RegisterCompanyUseCase}, extracted so
 * {@link com.umrah.scanner.company.application.AdminCreateCompanyUseCase} and
 * {@link UpdateCompanyProfileUseCase} can share it instead of each re-implementing the same
 * city lookup and {@code CompanyAddress} construction.
 */
@Component
public class CompanyProfileFactory {

    private final CityRepository cityRepository;

    public CompanyProfileFactory(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    /** Replaces the company's whole address list — every caller sends the full current set. */
    public void replaceAddresses(CompanyProfile company, List<CompanyAddressInput> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            throw new ValidationException("At least one address is required");
        }
        company.getAddresses().clear();
        for (CompanyAddressInput input : addresses) {
            City city = cityRepository.findById(input.cityId())
                    .orElseThrow(() -> NotFoundException.of("City", input.cityId()));
            CompanyAddress address = new CompanyAddress();
            address.setCity(city);
            address.setAddressText(input.addressText());
            address.setMobileNumber(input.mobileNumber());
            company.addAddress(address);
        }
    }
}
