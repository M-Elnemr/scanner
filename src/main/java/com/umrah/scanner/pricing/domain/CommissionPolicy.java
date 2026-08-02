package com.umrah.scanner.pricing.domain;

import java.math.BigDecimal;

/**
 * How much the company owes the platform for a booking. Resolved once, at lead creation, and then
 * snapshotted on the lead — changing the implementation (or the company's rate) never re-prices a
 * lead that already exists.
 */
public interface CommissionPolicy {

    /** Stable identifier stored alongside the priced amount so past leads stay explainable. */
    String code();

    BigDecimal commissionFor(PricingRequest request);
}
