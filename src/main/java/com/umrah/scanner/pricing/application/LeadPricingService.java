package com.umrah.scanner.pricing.application;

import com.umrah.scanner.pricing.domain.CashbackPolicy;
import com.umrah.scanner.pricing.domain.CommissionPolicy;
import com.umrah.scanner.pricing.domain.LeadPricing;
import com.umrah.scanner.pricing.domain.Money;
import com.umrah.scanner.pricing.domain.PricingRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The one place commission and cashback amounts are ever produced. Callers hand over a
 * {@link PricingRequest} and get back the full {@link LeadPricing} snapshot; they never do the
 * arithmetic themselves, so the rules can only ever live in one place.
 */
@Service
public class LeadPricingService {

    private final CommissionPolicy commissionPolicy;
    private final CashbackPolicySelector cashbackPolicySelector;

    public LeadPricingService(CommissionPolicy commissionPolicy, CashbackPolicySelector cashbackPolicySelector) {
        this.commissionPolicy = commissionPolicy;
        this.cashbackPolicySelector = cashbackPolicySelector;
    }

    public LeadPricing price(PricingRequest request) {
        BigDecimal commissionAmount = commissionPolicy.commissionFor(request);
        CashbackPolicy cashbackPolicy = cashbackPolicySelector.select(request);
        BigDecimal cashbackAmount = cashbackPolicy.cashbackFor(request, commissionAmount);

        return new LeadPricing(
                Money.normalize(request.commissionPerTraveler() == null ? Money.zero() : request.commissionPerTraveler()),
                request.billableTravelerCount(),
                commissionAmount,
                cashbackAmount,
                commissionPolicy.code(),
                cashbackPolicy.code());
    }

    /**
     * What a single traveller earns back — the only pricing figure a browsing customer is ever
     * shown. Runs through the same policies as a real lead so the number on the trip details page
     * can never drift from what is actually paid out.
     */
    public BigDecimal cashbackPerTraveler(UUID companyId, UUID tripId, BigDecimal commissionPerTraveler) {
        return price(PricingRequest.preview(companyId, tripId, commissionPerTraveler)).cashbackAmount();
    }
}
