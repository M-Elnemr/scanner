package com.umrah.scanner.lead.application;

import com.umrah.scanner.common.exception.ForbiddenException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.customer.infrastructure.CustomerProfileRepository;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.lead.domain.LeadStatusHistory;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.lead.infrastructure.LeadSpecifications;
import com.umrah.scanner.lead.infrastructure.LeadStatusHistoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadQueryService {

    private final LeadRepository leadRepository;
    private final LeadStatusHistoryRepository leadStatusHistoryRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;

    public LeadQueryService(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository leadStatusHistoryRepository,
            CustomerProfileRepository customerProfileRepository,
            CompanyProfileRepository companyProfileRepository) {
        this.leadRepository = leadRepository;
        this.leadStatusHistoryRepository = leadStatusHistoryRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.companyProfileRepository = companyProfileRepository;
    }

    @Transactional(readOnly = true)
    public Page<Lead> listForCustomer(UUID customerUserId, LeadStatus status, Pageable pageable) {
        UUID customerId = customerProfileRepository.findByUserId(customerUserId)
                .orElseThrow(() -> NotFoundException.of("CustomerProfile", customerUserId))
                .getId();
        return status == null
                ? leadRepository.findAllByCustomerId(customerId, pageable)
                : leadRepository.findAllByCustomerIdAndStatus(customerId, status, pageable);
    }

    /**
     * The journey this customer is currently holding, if any.
     *
     * <p>Exists so the trip-details screen can render its button from one call — no lead means
     * "preserve", a lead on this trip means "preserved", a lead on a different trip means tapping
     * has to warn them first — instead of discovering the third case only by getting a 409 back.
     */
    @Transactional(readOnly = true)
    public Optional<Lead> findActiveForCustomer(UUID customerUserId) {
        return customerProfileRepository.findByUserId(customerUserId)
                .flatMap(customer -> leadRepository.findActiveByCustomerId(customer.getId()));
    }

    @Transactional(readOnly = true)
    public Page<Lead> listForCompany(UUID companyUserId, LeadStatus status, Pageable pageable) {
        UUID companyId = companyProfileRepository.findByUserId(companyUserId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyUserId))
                .getId();
        return status == null
                ? leadRepository.findAllByCompanyId(companyId, pageable)
                : leadRepository.findAllByCompanyIdAndStatus(companyId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Lead> listForAdmin(AdminLeadFilter filter, Pageable pageable) {
        List<Specification<Lead>> specs = new ArrayList<>();
        if (filter.status() != null) {
            specs.add(LeadSpecifications.hasStatus(filter.status()));
        }
        if (filter.companyId() != null) {
            specs.add(LeadSpecifications.hasCompanyId(filter.companyId()));
        }
        if (filter.tripId() != null) {
            specs.add(LeadSpecifications.hasTripId(filter.tripId()));
        }
        if (filter.customerId() != null) {
            specs.add(LeadSpecifications.hasCustomerId(filter.customerId()));
        }
        if (filter.createdFrom() != null || filter.createdTo() != null) {
            specs.add(LeadSpecifications.createdBetween(filter.createdFrom(), filter.createdTo()));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            specs.add(LeadSpecifications.search(filter.search()));
        }
        return leadRepository.findAll(Specification.allOf(specs), pageable);
    }

    /** Admin-only read, no ownership check — role is already enforced at the controller edge. */
    @Transactional(readOnly = true)
    public Lead getForAdmin(UUID leadId) {
        return leadRepository.findWithDetailsById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));
    }

    /** A single lead, visible to its own customer, its own company, or any admin. */
    @Transactional(readOnly = true)
    public Lead getForCaller(UUID leadId, UUID callerUserId, boolean isAdmin) {
        Lead lead = leadRepository.findWithDetailsById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));
        requireVisibility(lead, callerUserId, isAdmin);
        return lead;
    }

    @Transactional(readOnly = true)
    public List<LeadStatusHistory> getHistory(UUID leadId, UUID callerUserId, boolean isAdmin) {
        Lead lead = leadRepository.findById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));
        requireVisibility(lead, callerUserId, isAdmin);
        return leadStatusHistoryRepository.findAllByLeadIdOrderByChangedAtAsc(leadId);
    }

    private void requireVisibility(Lead lead, UUID callerUserId, boolean isAdmin) {
        boolean owns = isAdmin
                || lead.getCustomer().getUser().getId().equals(callerUserId)
                || lead.getCompany().getUser().getId().equals(callerUserId);
        if (!owns) {
            throw new ForbiddenException("This lead does not belong to the caller");
        }
    }
}
