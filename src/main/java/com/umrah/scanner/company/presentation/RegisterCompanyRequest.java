package com.umrah.scanner.company.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RegisterCompanyRequest(
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Size(max = 100) String licenseNumber,
        String logoUrl,
        String whatsapp,
        String description,
        @NotEmpty @Valid List<CompanyAddressRequest> addresses) {
}
