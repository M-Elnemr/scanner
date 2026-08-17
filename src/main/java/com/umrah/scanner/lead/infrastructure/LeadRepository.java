package com.umrah.scanner.lead.infrastructure;

import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Note the absence of a plain {@code findByCustomerIdAndTripId}: since cancelling frees the pair up
 * to be preserved again, a customer can accumulate several rows for the same trip and only one of
 * them is live. Every read that means "the lead this customer has on this trip" must therefore say
 * so with {@link #findNotCancelledByCustomerIdAndTripId}, or it risks more than one row.
 */
public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    /**
     * The customer's single active lead, whichever trip it is on — the "preserved journey" slot.
     * Empty once they cancel it or the journey completes.
     *
     * <p>The statuses excluded here are exactly those of the {@code uq_leads_customer_active}
     * partial index, which is what actually guarantees at most one row comes back. Change one and
     * you must change the other.
     */
    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    @Query("select l from Lead l where l.customer.id = :customerId "
            + "and l.status not in (com.umrah.scanner.lead.domain.LeadStatus.CANCELLED, "
            + "com.umrah.scanner.lead.domain.LeadStatus.CASHBACK_PAID)")
    Optional<Lead> findActiveByCustomerId(@Param("customerId") UUID customerId);

    /**
     * The customer's live lead on one specific trip — at most one, per the
     * {@code uq_leads_customer_trip_live} partial index.
     *
     * <p>CASHBACK_PAID counts as live here while it does not in {@link #findActiveByCustomerId}:
     * a completed journey no longer occupies the customer's slot, but it is still an engagement
     * that earns them the company's contact details.
     */
    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    @Query("select l from Lead l where l.customer.id = :customerId and l.trip.id = :tripId "
            + "and l.status <> com.umrah.scanner.lead.domain.LeadStatus.CANCELLED")
    Optional<Lead> findNotCancelledByCustomerIdAndTripId(
            @Param("customerId") UUID customerId, @Param("tripId") UUID tripId);

    /**
     * "Has this customer an engagement with this company they have not withdrawn from?" — the gate
     * for revealing a company's full profile, mirroring the rule that reveals its identity on a trip.
     */
    @Query("select count(l) > 0 from Lead l where l.customer.id = :customerId and l.company.id = :companyId "
            + "and l.status <> com.umrah.scanner.lead.domain.LeadStatus.CANCELLED")
    boolean existsNotCancelledByCustomerIdAndCompanyId(
            @Param("customerId") UUID customerId, @Param("companyId") UUID companyId);

    /** The guard for deleting a company: refuse while it holds bookings still in flight. */
    @Query("select count(l) from Lead l where l.company.id = :companyId "
            + "and l.status not in (com.umrah.scanner.lead.domain.LeadStatus.CANCELLED, "
            + "com.umrah.scanner.lead.domain.LeadStatus.CASHBACK_PAID)")
    long countActiveByCompanyId(@Param("companyId") UUID companyId);

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

    /** Redeclared purely to attach the graph — the admin lead console runs through this one. */
    @Override
    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    Page<Lead> findAll(Specification<Lead> spec, Pageable pageable);

    /** Every to-one association a lead's DTO needs, in one query — safe to combine with paging since none are collections. */
    @EntityGraph(attributePaths = {"trip", "company", "customer"})
    Optional<Lead> findWithDetailsById(UUID id);
}
