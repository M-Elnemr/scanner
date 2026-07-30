package com.umrah.scanner.customer.presentation;

import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.customer.domain.WalletType;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String fullName,
        String phone,
        String cashbackWalletNumber,
        WalletType walletType,
        boolean profileCompleted) {

    public static CustomerResponse from(CustomerProfile profile) {
        return new CustomerResponse(
                profile.getId(), profile.getFullName(), profile.getPhone(),
                profile.getCashbackWalletNumber(), profile.getWalletType(), profile.isProfileCompleted());
    }

    public static CustomerResponse empty() {
        return new CustomerResponse(null, null, null, null, null, false);
    }
}
