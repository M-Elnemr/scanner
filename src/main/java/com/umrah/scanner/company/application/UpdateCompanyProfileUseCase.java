package com.umrah.scanner.company.application;

import com.umrah.scanner.city.domain.City;
import com.umrah.scanner.city.infrastructure.CityRepository;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyAddress;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateCompanyProfileUseCase {

    private final CompanyProfileRepository companyProfileRepository;
    private final CityRepository cityRepository;

    public UpdateCompanyProfileUseCase(CompanyProfileRepository companyProfileRepository, CityRepository cityRepository) {
        this.companyProfileRepository = companyProfileRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional
    public CompanyProfile execute(UUID userId, UpdateCompanyProfileCommand command) {
        CompanyProfile company = companyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", userId));

        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            throw new ValidationException("Suspended companies cannot update their profile");
        }
        if (command.addresses() == null || command.addresses().isEmpty()) {
            throw new ValidationException("At least one address is required");
        }

        company.setCompanyName(command.companyName());
        company.setLogoUrl(command.logoUrl());
        company.setWhatsapp(command.whatsapp());
        company.setDescription(command.description());

        // The client always sends its full current address list (add/remove happens in the UI
        // beforehand), so this is a full replace, not a partial/patch update.
        company.getAddresses().clear();
        for (CompanyAddressInput input : command.addresses()) {
            City city = cityRepository.findById(input.cityId())
                    .orElseThrow(() -> NotFoundException.of("City", input.cityId()));
            CompanyAddress address = new CompanyAddress();
            address.setCity(city);
            address.setAddressText(input.addressText());
            address.setMobileNumber(input.mobileNumber());
            company.addAddress(address);
        }

        CompanyProfileInitializer.initializeAddresses(company);
        return company;
    }
}
