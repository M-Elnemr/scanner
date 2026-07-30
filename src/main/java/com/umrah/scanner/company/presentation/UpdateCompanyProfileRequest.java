package com.umrah.scanner.company.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyProfileRequest(
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 500) String address,
        String logoUrl,
        String whatsapp,
        String description) {
}
