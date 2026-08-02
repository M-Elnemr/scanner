package com.umrah.scanner.pricing.infrastructure;

import com.umrah.scanner.pricing.domain.CommissionPolicy;
import com.umrah.scanner.pricing.domain.Money;
import com.umrah.scanner.pricing.domain.PricingRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** commissionAmount = commissionPerTraveler x travelerCount. */
@Component
public class PerTravelerCommissionPolicy implements CommissionPolicy {

    public static final String CODE = "PER_TRAVELER";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public BigDecimal commissionFor(PricingRequest request) {
        BigDecimal rate = request.commissionPerTraveler() == null ? Money.zero() : request.commissionPerTraveler();
        return Money.normalize(rate.multiply(BigDecimal.valueOf(request.billableTravelerCount())));
    }
}
