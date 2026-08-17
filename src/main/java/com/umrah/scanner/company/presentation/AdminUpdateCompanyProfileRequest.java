package com.umrah.scanner.company.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Everything {@link UpdateCompanyProfileRequest} carries, plus the licence number an admin may correct. */
public record AdminUpdateCompanyProfileRequest(
        @NotBlank @Size(max = 100) String licenseNumber,
        @NotBlank @Size(max = 255) String companyName,
        String logoUrl,
        String whatsapp,
        String description,
        @NotEmpty @Valid List<CompanyAddressRequest> addresses) {
}
