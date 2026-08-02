package com.umrah.scanner.pricing.domain;

import java.math.BigDecimal;

/**
 * The immutable result of pricing a lead. Every field is written to the lead once, at creation, and
 * is never recalculated — later changes to a company's rate or to the cashback rules affect only
 * leads created after the change.
 */
public record LeadPricing(
        BigDecimal commissionPerTraveler,
        int billableTravelerCount,
        BigDecimal commissionAmount,
        BigDecimal cashbackAmount,
        String commissionPolicyCode,
        String cashbackPolicyCode) {
}
