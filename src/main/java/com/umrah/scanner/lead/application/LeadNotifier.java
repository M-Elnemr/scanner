package com.umrah.scanner.lead.application;

import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadAction;
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

    public void actionPerformed(Lead lead, LeadAction action) {
        switch (action) {
            case REPORT_DEPOSIT -> toCompany(lead, "DEPOSIT_CONFIRMATION_REQUIRED", "Confirm a deposit",
                    "A customer reported paying the deposit for " + lead.getTrip().getTitle() + ". Please confirm.");

            case MARK_DEPOSIT_PAID -> toCustomer(lead, "DEPOSIT_CONFIRMED", "Deposit confirmed",
                    "Your deposit for " + lead.getTrip().getTitle() + " has been confirmed by the company.");

            case REPORT_FULL_PAYMENT -> toCompany(lead, "FULL_PAYMENT_CONFIRMATION_REQUIRED", "Confirm a full payment",
                    "A customer reported paying in full for " + lead.getTrip().getTitle() + ". Please confirm.");

            case MARK_FULLY_PAID -> toCustomer(lead, "FULL_PAYMENT_CONFIRMED", "Full payment confirmed",
                    "Your full payment for " + lead.getTrip().getTitle() + " has been confirmed by the company.");

            case REPORT_COMMISSION_PAID -> toAdmins(lead, "COMMISSION_CONFIRMATION_REQUIRED", "Confirm a commission payment",
                    "A company reported paying its commission. Please confirm so cashback can be released.");

            case CONFIRM_COMMISSION_PAID -> toCompany(lead, "COMMISSION_PAID", "Commission confirmed",
                    "Your commission payment has been confirmed by the platform.");

            case PAY_CASHBACK -> toCustomer(lead, "CASHBACK_PAID", "Cashback sent",
                    "Your cashback has been sent to your " + lead.getCustomer().getWalletType() + " wallet.");
        }
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
