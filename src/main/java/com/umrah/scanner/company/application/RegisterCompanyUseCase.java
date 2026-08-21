package com.umrah.scanner.company.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.user.domain.Role;
import com.umrah.scanner.user.domain.User;
import com.umrah.scanner.user.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterCompanyUseCase {

    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyProfileFactory companyProfileFactory;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public RegisterCompanyUseCase(
            CompanyProfileRepository companyProfileRepository,
            CompanyProfileFactory companyProfileFactory,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.companyProfileRepository = companyProfileRepository;
        this.companyProfileFactory = companyProfileFactory;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CompanyProfile execute(java.util.UUID userId, RegisterCompanyCommand command) {
        User user = userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("User", userId));
        if (user.getRole() != Role.COMPANY) {
            throw new ValidationException("Only COMPANY accounts can register a company profile");
        }
        if (companyProfileRepository.existsByUserId(userId)) {
            throw new ConflictException("Company profile already exists for this user");
        }

        CompanyProfile company = new CompanyProfile();
        company.setUser(user);
        company.setCompanyName(command.companyName());
        company.setLicenseNumber(command.licenseNumber());
        company.setWhatsapp(command.whatsapp());
        company.setDescription(command.description());
        company.setStatus(CompanyStatus.PENDING);
        companyProfileFactory.replaceAddresses(company, command.addresses());

        company = companyProfileRepository.save(company);
        auditLogService.record(userId, "COMPANY_REGISTERED", "CompanyProfile", company.getId(), null, company.getStatus());
        return company;
    }
}
