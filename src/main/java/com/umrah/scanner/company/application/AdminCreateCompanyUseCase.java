package com.umrah.scanner.company.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.pricing.domain.Money;
import com.umrah.scanner.user.domain.PlaceholderGoogleSub;
import com.umrah.scanner.user.domain.Role;
import com.umrah.scanner.user.domain.User;
import com.umrah.scanner.user.domain.UserStatus;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The admin's "add a company from scratch". Unlike {@link RegisterCompanyUseCase}, the caller is
 * not the account being registered — the owner may not have an account yet at all, which is the
 * whole reason this is a separate use case rather than a parameter on the self-service one.
 */
@Service
public class AdminCreateCompanyUseCase {

    private final UserRepository userRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyProfileFactory companyProfileFactory;
    private final AuditLogService auditLogService;

    public AdminCreateCompanyUseCase(
            UserRepository userRepository,
            CompanyProfileRepository companyProfileRepository,
            CompanyProfileFactory companyProfileFactory,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.companyProfileFactory = companyProfileFactory;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CompanyProfile execute(UUID adminUserId, AdminCreateCompanyCommand command) {
        if (command.ownerEmail() == null || command.ownerEmail().isBlank()) {
            throw new ValidationException("ownerEmail is required");
        }

        User owner = userRepository.findByEmail(command.ownerEmail())
                .map(existing -> requireClaimable(existing, command.ownerEmail()))
                .orElseGet(() -> provisionOwner(command.ownerEmail()));

        CompanyProfile company = new CompanyProfile();
        company.setUser(owner);
        company.setCompanyName(command.companyName());
        company.setLicenseNumber(command.licenseNumber());
        company.setLogoUrl(command.logoUrl());
        company.setWhatsapp(command.whatsapp());
        company.setDescription(command.description());
        company.setCommissionPerTraveler(Money.normalize(
                command.commissionPerTraveler() != null ? command.commissionPerTraveler() : BigDecimal.ZERO));
        companyProfileFactory.replaceAddresses(company, command.addresses());

        if (command.autoApprove()) {
            company.setStatus(CompanyStatus.APPROVED);
            company.setApprovedBy(adminUserId);
            company.setApprovedAt(Instant.now());
        } else {
            company.setStatus(CompanyStatus.PENDING);
        }

        company = companyProfileRepository.save(company);
        auditLogService.record(adminUserId, "COMPANY_CREATED_BY_ADMIN", "CompanyProfile", company.getId(), null, company.getStatus());
        return company;
    }

    /**
     * A COMPANY user who signed in once but never finished registration is the common case this
     * exists for — everyone else with that email is refused rather than repurposed. In particular a
     * CUSTOMER or ADMIN account is never flipped to COMPANY here: it may already have leads, a
     * wallet, or admin authority, and silently converting it would destroy data the admin has no way
     * to see was lost.
     */
    private User requireClaimable(User existing, String email) {
        if (existing.getRole() != Role.COMPANY) {
            throw new ConflictException(
                    "A " + existing.getRole() + " account already exists for " + email + " — it cannot become a company");
        }
        if (companyProfileRepository.existsByUserId(existing.getId())) {
            throw new ConflictException("A company profile already exists for " + email);
        }
        return existing;
    }

    private User provisionOwner(String email) {
        User user = new User();
        user.setEmail(email);
        user.setGoogleSub(PlaceholderGoogleSub.forEmail(email));
        user.setRole(Role.COMPANY);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}
