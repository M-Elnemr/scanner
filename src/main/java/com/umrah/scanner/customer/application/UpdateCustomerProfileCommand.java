package com.umrah.scanner.customer.application;

import com.umrah.scanner.customer.domain.WalletType;

public record UpdateCustomerProfileCommand(
        String fullName,
        String phone,
        String cashbackWalletNumber,
        WalletType walletType) {
}
