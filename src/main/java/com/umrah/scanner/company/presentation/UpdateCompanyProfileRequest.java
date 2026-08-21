package com.umrah.scanner.company.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateCompanyProfileRequest(
        @NotBlank @Size(max = 255) String companyName,
        String whatsapp,
        String description,
        @NotEmpty @Valid List<CompanyAddressRequest> addresses) {
}
