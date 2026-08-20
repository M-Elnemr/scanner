package com.umrah.scanner.lead.infrastructure;

import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Note the absence of a plain {@code findByCustomerIdAndTripId}: since cancelling frees the pair up
 * to be preserved again, a customer can accumulate several rows for the same trip and only one of
 * them is live. Every read that means "the lead this customer has on this trip" must therefore say
 * so with {@link #findNotCancelledByCustomerIdAndTripId}, or it risks more than one row.
 *
 * <p>Every multi-association fetch below spells out {@code left join fetch} rather than leaning on
 * {@code @EntityGraph}: Trip, CompanyProfile and CustomerProfile all carry
 * {@code @SQLRestriction("deleted_at is null")}, and Hibernate's entity-graph loader generates a
 * plain (inner) {@code join} for a to-one fetch graph regardless of the association's {@code
 * optional} flag — confirmed against the generated SQL, not assumed. An inner join against a
 * restricted table silently drops the whole Lead row the moment one of those three is soft-deleted,
 * which is exactly the bug this fixes: a trip an admin deletes must not make every lead that was
 * ever created against it vanish from every list and detail view. {@code left join fetch} is the
 * only reliable way to keep that guarantee in JPQL.
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
    @Query("select l from Lead l left join fetch l.trip left join fetch l.company left join fetch l.customer "
            + "where l.customer.id = :customerId "
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
    @Query("select l from Lead l left join fetch l.trip left join fetch l.company left join fetch l.customer "
            + "where l.customer.id = :customerId and l.tripId = :tripId "
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

    /** The guard for deleting a trip: refuse while it holds bookings still in flight. */
    @Query("select count(l) from Lead l where l.tripId = :tripId "
            + "and l.status not in (com.umrah.scanner.lead.domain.LeadStatus.CANCELLED, "
            + "com.umrah.scanner.lead.domain.LeadStatus.CASHBACK_PAID)")
    long countActiveByTripId(@Param("tripId") UUID tripId);

    Optional<Lead> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Lead> findByIdAndCompanyId(UUID id, UUID companyId);

    @Query(value = "select l from Lead l left join fetch l.trip left join fetch l.company left join fetch l.customer "
            + "where l.customer.id = :customerId",
            countQuery = "select count(l) from Lead l where l.customer.id = :customerId")
    Page<Lead> findAllByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    @Query(value = "select l from Lead l left join fetch l.trip left join fetch l.company left join fetch l.customer "
            + "where l.customer.id = :customerId and l.status = :status",
            countQuery = "select count(l) from Lead l where l.customer.id = :customerId and l.status = :status")
    Page<Lead> findAllByCustomerIdAndStatus(
            @Param("customerId") UUID customerId, @Param("status") LeadStatus status, Pageable pageable);

    @Query(value = "select l from Lead l left join fetch l.trip left join fetch l.company left join fetch l.customer "
            + "where l.company.id = :companyId",
            countQuery = "select count(l) from Lead l where l.company.id = :companyId")
    Page<Lead> findAllByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);

    @Query(value = "select l from Lead l left join fetch l.trip left join fetch l.company left join fetch l.customer "
            + "where l.company.id = :companyId and l.status = :status",
            countQuery = "select count(l) from Lead l where l.company.id = :companyId and l.status = :status")
    Page<Lead> findAllByCompanyIdAndStatus(
            @Param("companyId") UUID companyId, @Param("status") LeadStatus status, Pageable pageable);

    @Query(value = "select l from Lead l left join fetch l.trip left join fetch l.company left join fetch l.customer "
            + "where l.status = :status",
            countQuery = "select count(l) from Lead l where l.status = :status")
    Page<Lead> findAllByStatus(@Param("status") LeadStatus status, Pageable pageable);

    /** Every to-one association a lead's DTO needs, in one query — safe to combine with paging since none are collections. */
    @Query("select l from Lead l left join fetch l.trip left join fetch l.company left join fetch l.customer where l.id = :id")
    Optional<Lead> findWithDetailsById(@Param("id") UUID id);
}
