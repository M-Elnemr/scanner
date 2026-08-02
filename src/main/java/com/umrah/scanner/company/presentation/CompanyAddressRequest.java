package com.umrah.scanner.company.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CompanyAddressRequest(
        @NotNull UUID cityId,
        @NotBlank @Size(max = 500) String addressText,
        @NotBlank @Size(max = 30) String mobileNumber) {
}
