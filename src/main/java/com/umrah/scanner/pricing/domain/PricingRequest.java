package com.umrah.scanner.pricing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Everything a commission or cashback policy is allowed to see, expressed as plain identifiers and
 * numbers rather than JPA entities. Keeping the pricing module free of {@code Lead}/{@code Trip}/
 * {@code CompanyProfile} types is deliberate: a future coupon, seasonal or company-specific policy
 * loads whatever extra data it needs through its own repository using these ids, and no caller of
 * {@link CommissionPolicy}/{@link CashbackPolicy} has to change to accommodate it.
 *
 * @param commissionPerTraveler the company's configured rate at the moment of pricing — always the
 *                              snapshot, never re-read later
 * @param billableTravelerCount travellers the commission is charged for (adults only today)
 */
public record PricingRequest(
        UUID companyId,
        UUID tripId,
        UUID customerId,
        BigDecimal commissionPerTraveler,
        int adultCount,
        int childCount,
        int infantCount,
        int billableTravelerCount,
        Instant occurredAt) {

    /**
     * A "what would one traveller earn?" question — used by the trip details page, which shows
     * cashback per traveller before any lead exists.
     */
    public static PricingRequest preview(UUID companyId, UUID tripId, BigDecimal commissionPerTraveler) {
        return new PricingRequest(companyId, tripId, null, commissionPerTraveler, 1, 0, 0, 1, Instant.now());
    }
}
