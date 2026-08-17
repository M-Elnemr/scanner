package com.umrah.scanner.lead.infrastructure;

import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Dynamic filter predicates for the admin lead console, in the same style as {@code TripSpecifications}. */
public final class LeadSpecifications {

    private LeadSpecifications() {
    }

    public static Specification<Lead> hasStatus(LeadStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Lead> hasCompanyId(UUID companyId) {
        return (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);
    }

    public static Specification<Lead> hasTripId(UUID tripId) {
        return (root, query, cb) -> cb.equal(root.get("trip").get("id"), tripId);
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

    /** Matches the customer's name or phone, or the trip's title or code — the admin console's free-text box. */
    public static Specification<Lead> search(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byCustomerName = cb.like(cb.lower(root.get("customer").get("fullName")), pattern);
            Predicate byCustomerPhone = cb.like(cb.lower(root.get("customer").get("phone")), pattern);
            Predicate byTripTitle = cb.like(cb.lower(root.get("trip").get("title")), pattern);
            Predicate byTripCode = cb.like(cb.lower(root.get("trip").get("tripCode")), pattern);
            return cb.or(byCustomerName, byCustomerPhone, byTripTitle, byTripCode);
        };
    }
}
