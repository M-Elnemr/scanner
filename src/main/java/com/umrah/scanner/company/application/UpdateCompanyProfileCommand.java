package com.umrah.scanner.company.application;

import java.util.List;

public record UpdateCompanyProfileCommand(
        String companyName,
        String whatsapp,
        String description,
        List<CompanyAddressInput> addresses) {
}
