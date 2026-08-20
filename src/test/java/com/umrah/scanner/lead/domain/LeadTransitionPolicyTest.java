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
    @DisplayName("contact")
    class Contact {

        @Test
        void adminMarksAContactedLeadFromInterested() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.MARK_CONTACTED, LeadStatus.INTERESTED))
                    .contains(LeadStatus.CONTACTED);
        }

        @Test
        void cannotBeDoneTwice() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.MARK_CONTACTED, LeadStatus.CONTACTED)).isEmpty();
        }

        @Test
        void belongsToTheAdminAlone() {
            assertThat(LeadTransitionPolicy.actorOf(LeadAction.MARK_CONTACTED)).isEqualTo(Role.ADMIN);
            assertThat(LeadTransitionPolicy.isAllowed(LeadAction.MARK_CONTACTED, LeadStatus.INTERESTED, Role.CUSTOMER))
                    .isFalse();
            assertThat(LeadTransitionPolicy.isAllowed(LeadAction.MARK_CONTACTED, LeadStatus.INTERESTED, Role.COMPANY))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        void companyConfirmsFromInterestedOrContacted() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.MARK_DEPOSIT_PAID, LeadStatus.INTERESTED))
                    .contains(LeadStatus.DEPOSIT_PAID);
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.MARK_DEPOSIT_PAID, LeadStatus.CONTACTED))
                    .contains(LeadStatus.DEPOSIT_PAID);
        }

        @Test
        void companyConfirmsAReportedDeposit() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.MARK_DEPOSIT_PAID, LeadStatus.PENDING_DEPOSIT_CONFIRMATION))
                    .contains(LeadStatus.DEPOSIT_PAID);
        }
    }

    @Nested
    @DisplayName("full payment")
    class FullPayment {

        @Test
        void companyReportingFirstNeedsNoCustomerConfirmation() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.MARK_FULLY_PAID, LeadStatus.DEPOSIT_PAID))
                    .contains(LeadStatus.FULLY_PAID);
        }

        @Test
        void fullPaymentCannotBeMarkedBeforeTheDepositIsSettled() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.MARK_FULLY_PAID, LeadStatus.INTERESTED)).isEmpty();
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

        /** CANCEL is exempt: it is the one action that leaves the ladder rather than climbing it. */
        @Test
        void noActionEverMovesALeadBackwards() {
            for (LeadAction action : LeadAction.values()) {
                if (action == LeadAction.CANCEL) {
                    continue;
                }
                for (LeadStatus from : LeadStatus.values()) {
                    LeadTransitionPolicy.targetOf(action, from).ifPresent(to ->
                            assertThat(to.stage())
                                    .as("%s from %s must move forward", action, from)
                                    .isGreaterThan(from.stage()));
                }
            }
        }

        @Test
        void theTerminalStatesAcceptNothing() {
            for (LeadAction action : LeadAction.values()) {
                assertThat(LeadTransitionPolicy.targetOf(action, LeadStatus.CASHBACK_PAID)).isEmpty();
                assertThat(LeadTransitionPolicy.targetOf(action, LeadStatus.CANCELLED)).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("cancellation")
    class Cancellation {

        @Test
        void isAvailableFromEveryStatusExceptTheTwoTerminalOnes() {
            for (LeadStatus from : LeadStatus.values()) {
                var target = LeadTransitionPolicy.targetOf(LeadAction.CANCEL, from);
                if (from.isTerminal()) {
                    assertThat(target).as("cancelling a %s lead", from).isEmpty();
                } else {
                    assertThat(target).as("cancelling a %s lead", from).contains(LeadStatus.CANCELLED);
                }
            }
        }

        /** No source maps to CANCELLED from CANCELLED, so the second attempt is a conflict. */
        @Test
        void cannotBeDoneTwice() {
            assertThat(LeadTransitionPolicy.targetOf(LeadAction.CANCEL, LeadStatus.CANCELLED)).isEmpty();
        }

        @Test
        void belongsToTheCustomerAlone() {
            assertThat(LeadTransitionPolicy.actorOf(LeadAction.CANCEL)).isEqualTo(Role.CUSTOMER);
            for (LeadStatus from : LeadStatus.values()) {
                assertThat(LeadTransitionPolicy.isAllowed(LeadAction.CANCEL, from, Role.COMPANY)).isFalse();
                assertThat(LeadTransitionPolicy.isAllowed(LeadAction.CANCEL, from, Role.ADMIN)).isFalse();
            }
        }

        /** The client renders its cancel button off this list, so it has to appear in it. */
        @Test
        void showsUpInTheCustomersAvailableActions() {
            assertThat(LeadTransitionPolicy.availableActions(LeadStatus.DEPOSIT_PAID, Role.CUSTOMER))
                    .contains(LeadAction.CANCEL);
            assertThat(LeadTransitionPolicy.availableActions(LeadStatus.CASHBACK_PAID, Role.CUSTOMER))
                    .doesNotContain(LeadAction.CANCEL);
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
                    .containsExactly(LeadAction.CANCEL);
            assertThat(LeadTransitionPolicy.availableActions(LeadStatus.INTERESTED, Role.COMPANY))
                    .containsExactly(LeadAction.MARK_DEPOSIT_PAID);
            assertThat(LeadTransitionPolicy.availableActions(LeadStatus.INTERESTED, Role.ADMIN))
                    .containsExactly(LeadAction.MARK_CONTACTED);
        }
    }
}
