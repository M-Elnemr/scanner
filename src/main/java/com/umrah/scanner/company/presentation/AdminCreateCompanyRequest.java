package com.umrah.scanner.company.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record AdminCreateCompanyRequest(
        @NotBlank @Email String ownerEmail,
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Size(max = 100) String licenseNumber,
        String logoUrl,
        String whatsapp,
        String description,
        @NotEmpty @Valid List<CompanyAddressRequest> addresses,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal commissionPerTraveler,
        @NotNull Boolean autoApprove) {
}
