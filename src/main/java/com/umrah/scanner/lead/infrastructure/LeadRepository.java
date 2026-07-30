package com.umrah.scanner.lead.infrastructure;

import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    Optional<Lead> findByCustomerIdAndTripId(UUID customerId, UUID tripId);

    Optional<Lead> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Lead> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Lead> findAllByCustomerId(UUID customerId, Pageable pageable);

    Page<Lead> findAllByCustomerIdAndStatus(UUID customerId, LeadStatus status, Pageable pageable);

    Page<Lead> findAllByCompanyId(UUID companyId, Pageable pageable);

    Page<Lead> findAllByCompanyIdAndStatus(UUID companyId, LeadStatus status, Pageable pageable);

    Page<Lead> findAllByStatus(LeadStatus status, Pageable pageable);
}
