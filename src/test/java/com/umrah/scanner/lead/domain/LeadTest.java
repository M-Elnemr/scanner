package com.umrah.scanner.lead.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.pricing.domain.LeadPricing;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LeadTest {

    private static Lead leadAt(LeadStatus status) {
        Lead lead = new Lead();
        lead.setStatus(status);
        lead.changeTravelers(TravelerParty.of(2, 1, 0));
        return lead;
    }

    @Nested
    @DisplayName("traveler party")
    class Travelers {

        @Test
        void billableCountIsAdultsOnly() {
            TravelerParty party = TravelerParty.of(2, 3, 1);
            assertThat(party.getTravelerCount()).isEqualTo(2);
            assertThat(party.getTotalHeadCount()).isEqualTo(6);
        }

        @Test
        void atLeastOneAdultIsRequired() {
            assertThatThrownBy(() -> TravelerParty.of(0, 2, 0))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("At least one adult traveler is required");
        }

        @Test
        void negativeCountsAreRejected() {
            assertThatThrownBy(() -> TravelerParty.of(1, -1, 0)).isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> TravelerParty.of(1, 0, -1)).isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("traveler edit cut-off")
    class EditCutOff {

        @ParameterizedTest
        @EnumSource(value = LeadStatus.class, names = {
                "INTERESTED", "CONTACTED", "PENDING_DEPOSIT_CONFIRMATION", "DEPOSIT_PAID",
                "PENDING_FULL_PAYMENT_CONFIRMATION"})
        void editableWhileMoneyIsStillMoving(LeadStatus status) {
            Lead lead = leadAt(status);
            assertThat(lead.areTravelersEditable()).isTrue();

            lead.changeTravelers(TravelerParty.of(4, 0, 0));
            assertThat(lead.getTravelerCount()).isEqualTo(4);
        }

        @ParameterizedTest
        @EnumSource(value = LeadStatus.class, names = {
                "FULLY_PAID", "PENDING_COMMISSION_CONFIRMATION", "COMMISSION_PAID", "CASHBACK_PAID"})
        void frozenOnceTheBookingIsPaidInFull(LeadStatus status) {
            Lead lead = leadAt(LeadStatus.INTERESTED);
            lead.setStatus(status);

            assertThat(lead.areTravelersEditable()).isFalse();
            assertThatThrownBy(() -> lead.changeTravelers(TravelerParty.of(4, 0, 0)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("paid in full");
        }

        /**
         * CANCELLED sits off the status ladder with a negative stage, so a bare "is this lead at
         * least FULLY_PAID?" comparison would call it editable. This is the regression guard for
         * that: withdrawing from a journey has to close the traveler dialog, not reopen it.
         */
        @Test
        void frozenOnceCancelled() {
            Lead lead = leadAt(LeadStatus.INTERESTED);
            lead.setStatus(LeadStatus.CANCELLED);

            assertThat(lead.areTravelersEditable()).isFalse();
            assertThatThrownBy(() -> lead.changeTravelers(TravelerParty.of(4, 0, 0)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("cancelled journey");
        }
    }

    @Nested
    @DisplayName("pricing snapshot")
    class Pricing {

        private static LeadPricing pricing(String commission, String cashback) {
            return new LeadPricing(new BigDecimal("2000.00"), 2,
                    new BigDecimal(commission), new BigDecimal(cashback), "PER_TRAVELER", "COMMISSION_SHARE");
        }

        @Test
        void isWrittenOnce() {
            Lead lead = leadAt(LeadStatus.INTERESTED);
            lead.applyPricing(pricing("4000.00", "1000.00"));

            assertThat(lead.getCommissionAmount()).isEqualByComparingTo("4000.00");
            assertThat(lead.getCashbackAmount()).isEqualByComparingTo("1000.00");
            assertThat(lead.getCommissionPerTraveler()).isEqualByComparingTo("2000.00");
            assertThat(lead.getCashbackPolicy()).isEqualTo("COMMISSION_SHARE");
        }

        @Test
        void aLeadCanNeverBeRepriced() {
            Lead lead = leadAt(LeadStatus.INTERESTED);
            lead.applyPricing(pricing("4000.00", "1000.00"));

            assertThatThrownBy(() -> lead.applyPricing(pricing("9999.00", "2500.00")))
                    .isInstanceOf(ConflictException.class);
            assertThat(lead.getCommissionAmount()).isEqualByComparingTo("4000.00");
        }

        @Test
        void changingTravelersDoesNotReprice() {
            Lead lead = leadAt(LeadStatus.INTERESTED);
            lead.applyPricing(pricing("4000.00", "1000.00"));

            lead.changeTravelers(TravelerParty.of(10, 0, 0));

            assertThat(lead.getTravelerCount()).isEqualTo(10);
            assertThat(lead.getCommissionAmount()).isEqualByComparingTo("4000.00");
            assertThat(lead.getCashbackAmount()).isEqualByComparingTo("1000.00");
        }
    }

    @Nested
    @DisplayName("audit trail")
    class Audit {

        @Test
        void eachActionStampsItsOwnPair() {
            Lead lead = leadAt(LeadStatus.INTERESTED);
            var admin = java.util.UUID.randomUUID();
            var company = java.util.UUID.randomUUID();
            var at = java.time.Instant.parse("2026-07-20T08:00:00Z");

            lead.recordAction(LeadAction.MARK_CONTACTED, admin, at);
            lead.recordAction(LeadAction.MARK_DEPOSIT_PAID, company, at.plusSeconds(60));

            assertThat(lead.getContactedBy()).isEqualTo(admin);
            assertThat(lead.getContactedAt()).isEqualTo(at);
            assertThat(lead.getDepositConfirmedBy()).isEqualTo(company);
            assertThat(lead.getDepositConfirmedAt()).isEqualTo(at.plusSeconds(60));

            // Untouched steps stay null — the timeline shows only what actually happened.
            assertThat(lead.getFullPaymentReportedAt()).isNull();
            assertThat(lead.getCommissionPaidAt()).isNull();
            assertThat(lead.getCashbackPaidAt()).isNull();
            assertThat(lead.getCancelledAt()).isNull();
        }

        @Test
        void cancellingStampsWhoWithdrewAndWhen() {
            Lead lead = leadAt(LeadStatus.DEPOSIT_PAID);
            var customer = java.util.UUID.randomUUID();
            var at = java.time.Instant.parse("2026-07-20T08:00:00Z");

            lead.recordAction(LeadAction.CANCEL, customer, at);

            assertThat(lead.getCancelledBy()).isEqualTo(customer);
            assertThat(lead.getCancelledAt()).isEqualTo(at);
        }
    }
}
