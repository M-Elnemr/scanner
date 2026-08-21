package com.umrah.scanner.company.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateCompanyProfileUseCase {

    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyProfileFactory companyProfileFactory;

    public UpdateCompanyProfileUseCase(CompanyProfileRepository companyProfileRepository, CompanyProfileFactory companyProfileFactory) {
        this.companyProfileRepository = companyProfileRepository;
        this.companyProfileFactory = companyProfileFactory;
    }

    @Transactional
    public CompanyProfile executeAsCompany(UUID userId, UpdateCompanyProfileCommand command) {
        CompanyProfile company = companyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", userId));

        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            throw new ValidationException("Suspended companies cannot update their profile");
        }

        apply(company, command);
        CompanyProfileInitializer.initializeAddresses(company);
        return company;
    }

    /**
     * The admin variant skips the suspended-company guard — an admin editing a suspended company's
     * details (a phone number, an address) is exactly when it is needed — and may also change
     * {@code licenseNumber}, which the self-service path treats as immutable after registration.
     */
    @Transactional
    public CompanyProfile executeAsAdmin(UUID companyId, AdminUpdateCompanyProfileCommand command) {
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyId));

        if (command.licenseNumber() == null || command.licenseNumber().isBlank()) {
            throw new ValidationException("licenseNumber is required");
        }
        company.setLicenseNumber(command.licenseNumber());
        apply(company, command.profile());
        CompanyProfileInitializer.initializeAddresses(company);
        return company;
    }

    // The client always sends its full current address list (add/remove happens in the UI
    // beforehand), so this is a full replace, not a partial/patch update.
    private void apply(CompanyProfile company, UpdateCompanyProfileCommand command) {
        company.setCompanyName(command.companyName());
        company.setWhatsapp(command.whatsapp());
        company.setDescription(command.description());
        companyProfileFactory.replaceAddresses(company, command.addresses());
    }
}
