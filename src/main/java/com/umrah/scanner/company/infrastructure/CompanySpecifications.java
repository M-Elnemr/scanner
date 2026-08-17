package com.umrah.scanner.company.infrastructure;

import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/** Dynamic filter predicates for the admin company console. */
public final class CompanySpecifications {

    private CompanySpecifications() {
    }

    public static Specification<CompanyProfile> hasStatus(CompanyStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<CompanyProfile> nameOrLicenseContains(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byName = cb.like(cb.lower(root.get("companyName")), pattern);
            Predicate byLicense = cb.like(cb.lower(root.get("licenseNumber")), pattern);
            return cb.or(byName, byLicense);
        };
    }
}
