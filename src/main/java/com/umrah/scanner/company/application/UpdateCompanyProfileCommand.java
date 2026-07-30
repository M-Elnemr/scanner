package com.umrah.scanner.company.application;

public record UpdateCompanyProfileCommand(
        String companyName,
        String city,
        String address,
        String logoUrl,
        String whatsapp,
        String description) {
}
