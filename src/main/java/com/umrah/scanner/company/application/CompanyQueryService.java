package com.umrah.scanner.company.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyQueryService {

    private final CompanyProfileRepository companyProfileRepository;

    public CompanyQueryService(CompanyProfileRepository companyProfileRepository) {
        this.companyProfileRepository = companyProfileRepository;
    }

    @Transactional(readOnly = true)
    public CompanyProfile getByUserId(UUID userId) {
        CompanyProfile company = companyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", userId));
        CompanyProfileInitializer.initializeAddresses(company);
        return company;
    }

    @Transactional(readOnly = true)
    public CompanyProfile getById(UUID companyId) {
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyId));
        CompanyProfileInitializer.initializeAddresses(company);
        return company;
    }

    @Transactional(readOnly = true)
    public Page<CompanyProfile> listByStatus(CompanyStatus status, Pageable pageable) {
        Page<CompanyProfile> companies = companyProfileRepository.findAllByStatus(status, pageable);
        companies.forEach(CompanyProfileInitializer::initializeAddresses);
        return companies;
    }
}
