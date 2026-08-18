package com.umrah.scanner.company.application;

import com.umrah.scanner.common.exception.ForbiddenException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.company.infrastructure.CompanySpecifications;
import com.umrah.scanner.customer.infrastructure.CustomerProfileRepository;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.user.domain.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyQueryService {

    private final CompanyProfileRepository companyProfileRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final LeadRepository leadRepository;

    public CompanyQueryService(
            CompanyProfileRepository companyProfileRepository,
            CustomerProfileRepository customerProfileRepository,
            LeadRepository leadRepository) {
        this.companyProfileRepository = companyProfileRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.leadRepository = leadRepository;
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

    /**
     * The customer-facing read. A company's full profile — branches, contact numbers, licence — is
     * not public: it opens up only once the caller has actually engaged that company by creating a
     * lead. That is the same rule that already governs revealing company identity on a trip, applied
     * to the richer profile.
     *
     * <p>Status is deliberately not checked. A customer with a booking in flight keeps access to the
     * branch phone numbers even if the company is later suspended, which is precisely when they are
     * most likely to need them.
     *
     * <p>A cancelled lead does not count: withdrawing from a journey takes the company's details
     * away again, exactly as if the customer had never preserved it.
     */
    @Transactional(readOnly = true)
    public CompanyProfile getVisibleTo(UUID companyId, UUID callerUserId, Role callerRole) {
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyId));

        boolean visible = switch (callerRole) {
            case ADMIN -> true;
            case COMPANY -> company.getUser().getId().equals(callerUserId);
            case CUSTOMER -> customerProfileRepository.findByUserId(callerUserId)
                    .map(customer -> leadRepository.existsNotCancelledByCustomerIdAndCompanyId(customer.getId(), companyId))
                    .orElse(false);
        };
        if (!visible) {
            throw new ForbiddenException("Contact this company about a trip to see its full details");
        }

        CompanyProfileInitializer.initializeAddresses(company);
        return company;
    }

    /** status and search are both optional — the admin console opens on "everything", not just PENDING. */
    @Transactional(readOnly = true)
    public Page<CompanyProfile> listForAdmin(CompanyStatus status, String search, Pageable pageable) {
        List<Specification<CompanyProfile>> specs = new ArrayList<>();
        if (status != null) {
            specs.add(CompanySpecifications.hasStatus(status));
        }
        if (search != null && !search.isBlank()) {
            specs.add(CompanySpecifications.nameOrLicenseContains(search));
        }
        Page<CompanyProfile> companies = companyProfileRepository.findAll(Specification.allOf(specs), pageable);
        List<UUID> ids = companies.getContent().stream().map(CompanyProfile::getId).toList();
        if (!ids.isEmpty()) {
            // Same managed entities as `companies` (same persistence context) — this just warms
            // their addresses/city associations in one query instead of one per row.
            companyProfileRepository.findAllById(ids);
        }
        return companies;
    }
}
