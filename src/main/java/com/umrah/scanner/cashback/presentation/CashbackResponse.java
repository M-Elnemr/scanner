package com.umrah.scanner.cashback.presentation;

import com.umrah.scanner.cashback.domain.CashbackStatus;
import com.umrah.scanner.cashback.domain.CashbackTransaction;
import com.umrah.scanner.customer.domain.WalletType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashbackResponse(
        UUID id, UUID leadId, WalletType walletType, String walletNumber, BigDecimal amount, CashbackStatus status, Instant sentAt) {

    public static CashbackResponse from(CashbackTransaction cashback) {
        return new CashbackResponse(
                cashback.getId(), cashback.getLead().getId(), cashback.getWalletType(), cashback.getWalletNumber(),
                cashback.getAmount(), cashback.getStatus(), cashback.getSentAt());
    }
}
