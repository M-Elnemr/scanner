package com.umrah.scanner.pricing.infrastructure;

import com.umrah.scanner.pricing.domain.CashbackPolicy;
import com.umrah.scanner.pricing.domain.Money;
import com.umrah.scanner.pricing.domain.PricingRequest;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The platform-wide default: the customer gets a fixed share of the commission (a quarter, i.e.
 * commissionAmount / 4). Deliberately ordered last so any future targeted policy — a coupon, a
 * company promotion, a seasonal offer — takes precedence simply by being registered with a higher
 * precedence; this one is the fallback that always applies.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommissionShareCashbackPolicy implements CashbackPolicy {

    public static final String CODE = "COMMISSION_SHARE";

    private final BigDecimal share;

    public CommissionShareCashbackPolicy(@Value("${app.cashback.default-share:0.25}") BigDecimal share) {
        this.share = share;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean supports(PricingRequest request) {
        return true;
    }

    @Override
    public BigDecimal cashbackFor(PricingRequest request, BigDecimal commissionAmount) {
        BigDecimal base = commissionAmount == null ? Money.zero() : commissionAmount;
        return Money.normalize(base.multiply(share));
    }
}
