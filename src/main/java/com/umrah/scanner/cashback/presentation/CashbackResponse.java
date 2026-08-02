package com.umrah.scanner.cashback.presentation;

import com.umrah.scanner.cashback.domain.CashbackStatus;
import com.umrah.scanner.cashback.domain.CashbackTransaction;
import com.umrah.scanner.customer.domain.WalletType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "Cashback", description = "A cashback payout to the customer's mobile wallet.")
public record CashbackResponse(
        UUID id,
        UUID leadId,
        WalletType walletType,
        String walletNumber,
        @Schema(description = "EGP, fixed at lead creation") BigDecimal amount,
        CashbackStatus status,
        Instant sentAt) {

    public static CashbackResponse from(CashbackTransaction cashback) {
        return new CashbackResponse(
                cashback.getId(), cashback.getLead().getId(), cashback.getWalletType(), cashback.getWalletNumber(),
                cashback.getAmount(), cashback.getStatus(), cashback.getSentAt());
    }
}
