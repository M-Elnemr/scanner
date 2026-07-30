package com.umrah.scanner.lead.infrastructure;

import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    Optional<Lead> findByCustomerIdAndTripId(UUID customerId, UUID tripId);

    Optional<Lead> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Lead> findByIdAndCompanyId(UUID id, UUID companyId);

    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    Page<Lead> findAllByCustomerId(UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    Page<Lead> findAllByCustomerIdAndStatus(UUID customerId, LeadStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    Page<Lead> findAllByCompanyId(UUID companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    Page<Lead> findAllByCompanyIdAndStatus(UUID companyId, LeadStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    Page<Lead> findAllByStatus(LeadStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    Page<Lead> findAll(Pageable pageable);

    /** Every to-one association a lead's DTO needs, in one query — safe to combine with paging since none are collections. */
    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    Optional<Lead> findWithDetailsById(UUID id);
}
