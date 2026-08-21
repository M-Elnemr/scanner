package com.umrah.scanner.customer.presentation;

import jakarta.validation.constraints.NotBlank;

public record UpdateCustomerProfileRequest(
        @NotBlank String fullName,
        @NotBlank String phone) {
}
