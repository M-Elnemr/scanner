package com.umrah.scanner.pricing.application;

import com.umrah.scanner.pricing.domain.CashbackPolicy;
import com.umrah.scanner.pricing.domain.PricingRequest;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Picks the winning cashback rule for a request: the first policy, in Spring's {@code @Order}
 * precedence, that says it supports the request. Adding a promotion or coupon campaign later means
 * adding one bean — nothing here, and nothing calling this, changes.
 */
@Component
public class CashbackPolicySelector {

    private final List<CashbackPolicy> policies;

    public CashbackPolicySelector(List<CashbackPolicy> policies) {
        this.policies = policies;
    }

    public CashbackPolicy select(PricingRequest request) {
        return policies.stream()
                .filter(policy -> policy.supports(request))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No cashback policy accepted the request; the default policy must always apply"));
    }
}
