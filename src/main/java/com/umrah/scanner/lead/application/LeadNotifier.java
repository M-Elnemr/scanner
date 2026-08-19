package com.umrah.scanner.lead.application;

import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadAction;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import com.umrah.scanner.user.domain.Role;
import com.umrah.scanner.user.domain.User;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Turns a lead lifecycle event into the notification the other party needs to see. Kept apart from
 * the use case that moves the lead so the state machine stays about state, and so adding or
 * retargeting a message never touches transition logic.
 */
@Service
public class LeadNotifier {

    private final NotificationDispatcher notificationDispatcher;
    private final UserRepository userRepository;

    public LeadNotifier(NotificationDispatcher notificationDispatcher, UserRepository userRepository) {
        this.notificationDispatcher = notificationDispatcher;
        this.userRepository = userRepository;
    }

    /** A customer has just contacted the company about a trip. */
    public void leadCreated(Lead lead) {
        toCompany(lead, "NEW_LEAD", "New interested customer",
                "A customer is interested in " + lead.getTrip().getTitle() + " for "
                        + lead.getTravelerCount() + " traveler(s).");
    }

    /**
     * @param statusBeforeAction where the lead sat before the action landed. Passed in rather than
     *                           read off the lead, which by this point already carries the new
     *                           status — cancellation in particular is only interesting relative to
     *                           how far the booking had got.
     */
    public void actionPerformed(Lead lead, LeadAction action, LeadStatus statusBeforeAction) {
        switch (action) {
            case MARK_DEPOSIT_PAID -> toCustomer(lead, "DEPOSIT_CONFIRMED", "Deposit confirmed",
                    "Your deposit for " + lead.getTrip().getTitle() + " has been confirmed by the company.");

            case MARK_FULLY_PAID -> toCustomer(lead, "FULL_PAYMENT_CONFIRMED", "Full payment confirmed",
                    "Your full payment for " + lead.getTrip().getTitle() + " has been confirmed by the company.");

            case REPORT_COMMISSION_PAID -> toAdmins(lead, "COMMISSION_CONFIRMATION_REQUIRED", "Confirm a commission payment",
                    "A company reported paying its commission. Please confirm so cashback can be released.");

            case CONFIRM_COMMISSION_PAID -> toCompany(lead, "COMMISSION_PAID", "Commission confirmed",
                    "Your commission payment has been confirmed by the platform.");

            case PAY_CASHBACK -> toCustomer(lead, "CASHBACK_PAID", "Cashback sent",
                    "Your cashback has been sent to your " + lead.getCustomer().getWalletType() + " wallet.");

            case CANCEL -> leadCancelled(lead, statusBeforeAction);
        }
    }

    /**
     * The company always needs to know it has lost the booking. Admins are told as well once the
     * lead had reached DEPOSIT_PAID, because from that point a cancellation means real money
     * changed hands and someone has to sort out the refund.
     */
    private void leadCancelled(Lead lead, LeadStatus statusBeforeCancel) {
        toCompany(lead, "LEAD_CANCELLED", "A customer cancelled",
                "A customer cancelled their booking for " + lead.getTrip().getTitle() + ".");

        if (statusBeforeCancel.isAtLeast(LeadStatus.DEPOSIT_PAID)) {
            toAdmins(lead, "PAID_LEAD_CANCELLED", "A paid booking was cancelled",
                    "A customer cancelled " + lead.getTrip().getTitle() + " after paying. "
                            + "The commission has been voided; the refund needs following up.");
        }
    }

    /**
     * An admin forced this lead's status directly, bypassing the normal report/confirm steps — see
     * {@link OverrideLeadStatusUseCase}. Both sides are told, since either could otherwise be
     * surprised by a status that neither of them produced.
     */
    public void statusOverridden(Lead lead, LeadStatus previous, LeadStatus target, String reason) {
        String body = "An administrator changed this booking's status from " + previous + " to " + target
                + " for " + lead.getTrip().getTitle() + ". Reason: " + reason;
        toCustomer(lead, "LEAD_STATUS_OVERRIDDEN", "Your booking status was updated by an administrator", body);
        toCompany(lead, "LEAD_STATUS_OVERRIDDEN", "A booking status was updated by an administrator", body);
    }

    private void toCompany(Lead lead, String type, String title, String body) {
        notificationDispatcher.dispatch(lead.getCompany().getUser().getId(), type, title, body, payload(lead));
    }

    private void toCustomer(Lead lead, String type, String title, String body) {
        notificationDispatcher.dispatch(lead.getCustomer().getUser().getId(), type, title, body, payload(lead));
    }

    private void toAdmins(Lead lead, String type, String title, String body) {
        for (User admin : userRepository.findAllByRole(Role.ADMIN)) {
            notificationDispatcher.dispatch(admin.getId(), type, title, body, payload(lead));
        }
    }

    private Map<String, Object> payload(Lead lead) {
        UUID tripId = lead.getTrip().getId();
        return Map.of("leadId", lead.getId().toString(), "tripId", tripId.toString());
    }
}
