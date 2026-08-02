package com.umrah.scanner.company.application;

import com.umrah.scanner.company.domain.CompanyProfile;
import org.hibernate.Hibernate;

/**
 * The controller maps CompanyResponse after each use case's transaction/session closes
 * (open-in-view is off), so lazy associations it reads — addresses, and each address's city —
 * must be force-initialized here first.
 */
final class CompanyProfileInitializer {

    private CompanyProfileInitializer() {
    }

    static void initializeAddresses(CompanyProfile company) {
        Hibernate.initialize(company.getAddresses());
        company.getAddresses().forEach(address -> Hibernate.initialize(address.getCity()));
    }
}
