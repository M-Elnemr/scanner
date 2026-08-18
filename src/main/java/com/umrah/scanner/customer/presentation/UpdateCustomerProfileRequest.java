package com.umrah.scanner.customer.presentation;

import com.umrah.scanner.customer.domain.WalletType;
import jakarta.validation.constraints.NotBlank;

public record UpdateCustomerProfileRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        String cashbackWalletNumber,
        WalletType walletType) {
}
