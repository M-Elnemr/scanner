package com.umrah.scanner.company.application;

import java.util.List;

public record RegisterCompanyCommand(
        String companyName,
        String licenseNumber,
        String city,
        String address,
        String logoUrl,
        String whatsapp,
        String description,
        List<String> phoneNumbers) {
}
