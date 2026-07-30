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

    public UpdateCompanyProfileUseCase(CompanyProfileRepository companyProfileRepository) {
        this.companyProfileRepository = companyProfileRepository;
    }

    @Transactional
    public CompanyProfile execute(UUID userId, UpdateCompanyProfileCommand command) {
        CompanyProfile company = companyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", userId));

        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            throw new ValidationException("Suspended companies cannot update their profile");
        }

        company.setCompanyName(command.companyName());
        company.setCity(command.city());
        company.setAddress(command.address());
        company.setLogoUrl(command.logoUrl());
        company.setWhatsapp(command.whatsapp());
        company.setDescription(command.description());
        return company;
    }
}
