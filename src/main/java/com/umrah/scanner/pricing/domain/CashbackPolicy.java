package com.umrah.scanner.pricing.domain;

import java.math.BigDecimal;

/**
 * How much of the platform's commission is handed back to the customer.
 *
 * <p>Today there is exactly one implementation — a fixed share of the commission. The point of the
 * abstraction is that percentage changes, per-company promotions, coupon campaigns, seasonal offers
 * and flat-amount cashback all arrive as <em>new implementations</em> of this interface: register a
 * bean, give it a higher precedence than the default, and it wins for the requests it
 * {@link #supports(PricingRequest) supports}. No schema change and no caller change — the resulting
 * amount and the winning policy's {@link #code()} are already snapshotted on the lead.
 */
public interface CashbackPolicy {

    /** Stable identifier persisted on the lead, so a past payout can always be traced to its rule. */
    String code();

    /** Whether this policy applies to the request at all. The default policy answers true for everything. */
    boolean supports(PricingRequest request);

    BigDecimal cashbackFor(PricingRequest request, BigDecimal commissionAmount);
}
