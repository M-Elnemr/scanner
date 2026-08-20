package com.umrah.scanner.lead.infrastructure;

import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/** Dynamic filter predicates for the admin lead console, in the same style as {@code TripSpecifications}. */
public final class LeadSpecifications {

    /**
     * The admin console's "by state" order: lifecycle stage rather than the raw status name (which
     * would sort alphabetically and scatter INTERESTED, CONTACTED, DEPOSIT_PAID... in no useful
     * order). Deliberately distinct from {@link LeadStatus#stage()} only in where CANCELLED lands —
     * {@code stage()} keeps it off the ladder (negative) for {@code isAtLeast} comparisons, but here
     * it belongs at the end next to the other terminal status, not at the front.
     */
    private static final List<LeadStatus> STATUS_DISPLAY_ORDER = List.of(
            LeadStatus.INTERESTED, LeadStatus.CONTACTED, LeadStatus.PENDING_DEPOSIT_CONFIRMATION,
            LeadStatus.DEPOSIT_PAID, LeadStatus.PENDING_FULL_PAYMENT_CONFIRMATION, LeadStatus.FULLY_PAID,
            LeadStatus.PENDING_COMMISSION_CONFIRMATION, LeadStatus.COMMISSION_PAID, LeadStatus.CASHBACK_PAID,
            LeadStatus.CANCELLED);

    private LeadSpecifications() {
    }

    public static Specification<Lead> hasStatus(LeadStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Lead> hasCompanyId(UUID companyId) {
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<Lead> hasTripId(UUID tripId) {
        return (root, query, cb) -> cb.equal(root.get("tripId"), tripId);
    }

    public static Specification<Lead> hasCustomerId(UUID customerId) {
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Lead> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    /**
     * Sets the query's ORDER BY directly rather than returning a predicate that filters anything —
     * the {@code true}-returning {@code cb.conjunction()} is a no-op filter, present only because a
     * {@link Specification} must return a {@link Predicate}. Must be combined with a {@link Pageable}
     * that carries no sort of its own, or Spring Data overwrites this ordering right back out.
     */
    public static Specification<Lead> orderByStatusRank(boolean ascending) {
        return (root, query, cb) -> {
            CriteriaBuilder.Case<Integer> rank = cb.selectCase();
            for (int i = 0; i < STATUS_DISPLAY_ORDER.size(); i++) {
                rank = rank.when(cb.equal(root.get("status"), STATUS_DISPLAY_ORDER.get(i)), i);
            }
            Expression<Integer> rankExpr = rank.otherwise(STATUS_DISPLAY_ORDER.size());
            query.orderBy(ascending ? cb.asc(rankExpr) : cb.desc(rankExpr));
            return cb.conjunction();
        };
    }

    /**
     * Matches the customer's name or phone, or the trip's title or code — the admin console's
     * free-text box. The trip fields read {@link Lead#getTripTitle()}/{@link Lead#getTripCode()}
     * rather than joining through {@code trip} itself, so a search is never affected by whether the
     * trip a match belongs to has since been soft-deleted.
     */
    public static Specification<Lead> search(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byCustomerName = cb.like(cb.lower(root.get("customer").get("fullName")), pattern);
            Predicate byCustomerPhone = cb.like(cb.lower(root.get("customer").get("phone")), pattern);
            Predicate byTripTitle = cb.like(cb.lower(root.get("tripTitle")), pattern);
            Predicate byTripCode = cb.like(cb.lower(root.get("tripCode")), pattern);
            return cb.or(byCustomerName, byCustomerPhone, byTripTitle, byTripCode);
        };
    }

    /**
     * Left-join-fetches every to-one association {@link com.umrah.scanner.lead.presentation.LeadResponse}
     * needs, so the admin console's {@code findAll(Specification, Pageable)} query does not leave them
     * lazy (open-in-view is off — they must be initialized before the transaction closes) and does not
     * silently drop a lead whose trip or company has been soft-deleted, the way an inner join would.
     * {@code distinct} is unnecessary: every fetch here is to-one, so no row multiplication to collapse.
     */
    public static Specification<Lead> fetchDetails() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("trip", JoinType.LEFT);
                root.fetch("company", JoinType.LEFT);
                root.fetch("customer", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }
}
