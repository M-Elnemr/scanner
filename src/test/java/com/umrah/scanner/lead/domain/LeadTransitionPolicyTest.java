package com.umrah.scanner.lead.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.umrah.scanner.user.domain.Role;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The workflow rules are worth pinning down precisely because everything else defers to them: if an
 * edge is missing here, the whole system rejects it.
 */
class LeadTransitionPolicyTest {

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        void customerReportParksAwaitingCompanyConfirmation() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.REPORT_DEPOSIT, LeadStatus.INTERESTED))
                    .contains(LeadStatus.PENDING_DEPOSIT_CONFIRMATION);
        }

        @Test
        void companyConfirmsAReportedDeposit() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.MARK_DEPOSIT_PAID, LeadStatus.PENDING_DEPOSIT_CONFIRMATION))
                    .contains(LeadStatus.DEPOSIT_PAID);
        }

        @Test
        void companyReportingFirstNeedsNoCustomerConfirmation() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.MARK_DEPOSIT_PAID, LeadStatus.INTERESTED))
                    .contains(LeadStatus.DEPOSIT_PAID);
        }
    }

    @Nested
    @DisplayName("full payment")
    class FullPayment {

        @Test
        void customerReportParksAwaitingCompanyConfirmation() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.REPORT_FULL_PAYMENT, LeadStatus.DEPOSIT_PAID))
                    .contains(LeadStatus.PENDING_FULL_PAYMENT_CONFIRMATION);
        }

        @Test
        void companyReportingFirstNeedsNoCustomerConfirmation() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.MARK_FULLY_PAID, LeadStatus.DEPOSIT_PAID))
                    .contains(LeadStatus.FULLY_PAID);
        }

        @Test
        void fullPaymentCannotBeReportedBeforeTheDepositIsSettled() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.REPORT_FULL_PAYMENT, LeadStatus.INTERESTED)).isEmpty();
        }
    }

    @Nested
    @DisplayName("commission and cashback")
    class CommissionAndCashback {

        @Test
        void companyReportsAndAdminConfirms() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.REPORT_COMMISSION_PAID, LeadStatus.FULLY_PAID))
                    .contains(LeadStatus.PENDING_COMMISSION_CONFIRMATION);
            assertThat(LeadTransitionPolicy.targetOf(
                    LeadAction.CONFIRM_COMMISSION_PAID, LeadStatus.PENDING_COMMISSION_CONFIRMATION))
                    .contains(LeadStatus.COMMISSION_PAID);
        }

        @Test
        void adminMayRecordTheCommissionDirectly() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.CONFIRM_COMMISSION_PAID, LeadStatus.FULLY_PAID))
                    .contains(LeadStatus.COMMISSION_PAID);
        }

        @Test
        void cashbackIsUnreachableUntilTheCommissionIsConfirmed() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.PAY_CASHBACK, LeadStatus.FULLY_PAID)).isEmpty();
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.PAY_CASHBACK, LeadStatus.DEPOSIT_PAID)).isEmpty();
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.PAY_CASHBACK, LeadStatus.PENDING_COMMISSION_CONFIRMATION))
                    .isEmpty();
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.PAY_CASHBACK, LeadStatus.COMMISSION_PAID))
                    .contains(LeadStatus.CASHBACK_PAID);
        }
    }

    @Nested
    @DisplayName("illegal moves")
    class IllegalMoves {

        @Test
        void noActionSkipsAheadFromInterested() {
            for (LeadAction action : LeadAction.values()) {
                Optional<LeadStatus> target = LeadTransitionPolicy.targetOf(action, LeadStatus.INTERESTED);
                assertThat(target).isNotEqualTo(Optional.of(LeadStatus.FULLY_PAID));
                assertThat(target).isNotEqualTo(Optional.of(LeadStatus.COMMISSION_PAID));
                assertThat(target).isNotEqualTo(Optional.of(LeadStatus.CASHBACK_PAID));
            }
        }

        @Test
        void noActionEverMovesALeadBackwards() {
            for (LeadAction action : LeadAction.values()) {
                for (LeadStatus from : LeadStatus.values()) {
                    LeadTransitionPolicy.targetOf(action, from).ifPresent(to ->
                            assertThat(to.stage())
                                    .as("%s from %s must move forward", action, from)
                                    .isGreaterThan(from.stage()));
                }
            }
        }

        @Test
        void theTerminalStateAcceptsNothing() {
            for (LeadAction action : LeadAction.values()) {
                assertThat(LeadTransitionPolicy.targetOf(action, LeadStatus.CASHBACK_PAID)).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("role enforcement")
    class RoleEnforcement {

        @Test
        void aCustomerCannotConfirmTheirOwnDeposit() {
            assertThat(LeadTransitionPolicy.isAllowed(LeadAction.MARK_DEPOSIT_PAID, LeadStatus.INTERESTED, Role.CUSTOMER))
                    .isFalse();
            assertThat(LeadTransitionPolicy.isAllowed(LeadAction.MARK_DEPOSIT_PAID, LeadStatus.INTERESTED, Role.COMPANY))
                    .isTrue();
        }

        @Test
        void aCompanyCannotConfirmItsOwnCommissionOrPayCashback() {
            assertThat(LeadTransitionPolicy.isAllowed(
                    LeadAction.CONFIRM_COMMISSION_PAID, LeadStatus.PENDING_COMMISSION_CONFIRMATION, Role.COMPANY))
                    .isFalse();
            assertThat(LeadTransitionPolicy.isAllowed(LeadAction.PAY_CASHBACK, LeadStatus.COMMISSION_PAID, Role.COMPANY))
                    .isFalse();
        }

        @Test
        void availableActionsAreScopedToTheAskingRole() {
            assertThat(LeadTransitionPolicy.availableActions(LeadStatus.INTERESTED, Role.CUSTOMER))
                    .containsExactly(LeadAction.REPORT_DEPOSIT);
            assertThat(LeadTransitionPolicy.availableActions(LeadStatus.INTERESTED, Role.COMPANY))
                    .containsExactly(LeadAction.MARK_DEPOSIT_PAID);
            assertThat(LeadTransitionPolicy.availableActions(LeadStatus.INTERESTED, Role.ADMIN)).isEmpty();
        }
    }
}
