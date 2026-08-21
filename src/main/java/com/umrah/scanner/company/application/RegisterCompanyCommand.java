package com.umrah.scanner.company.application;

import java.util.List;

public record RegisterCompanyCommand(
        String companyName,
        String licenseNumber,
        String whatsapp,
        String description,
        List<CompanyAddressInput> addresses) {
}
