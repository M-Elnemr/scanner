package com.umrah.scanner.lead.application;

import com.umrah.scanner.customer.domain.WalletType;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadAction;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import com.umrah.scanner.user.domain.Role;
import com.umrah.scanner.user.domain.User;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Turns a lead lifecycle event into the notification the other party needs to see. Kept apart from
 * the use case that moves the lead so the state machine stays about state, and so adding or
 * retargeting a message never touches transition logic.
 *
 * <p>Every user-facing title/body here is Arabic — the app's only audience language. {@code type}
 * and the {@code data} payload stay English identifiers; clients route/deep-link on those, they are
 * never rendered to a user.
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
        toCompany(lead, "NEW_LEAD", "عميل جديد مهتم",
                "يوجد عميل مهتم برحلة " + lead.getTripTitle() + " لعدد " + lead.getTravelerCount() + " من المسافرين.");
        toAdmins(lead, "NEW_LEAD", "حجز جديد",
                "قام عميل بحجز رحلة " + lead.getTripTitle() + " مع شركة " + lead.getCompanyName()
                        + " لعدد " + lead.getTravelerCount() + " من المسافرين.");
    }

    /**
     * @param statusBeforeAction where the lead sat before the action landed. Passed in rather than
     *                           read off the lead, which by this point already carries the new
     *                           status — cancellation in particular is only interesting relative to
     *                           how far the booking had got.
     */
    public void actionPerformed(Lead lead, LeadAction action, LeadStatus statusBeforeAction) {
        switch (action) {
            case MARK_DEPOSIT_PAID -> toCustomer(lead, "DEPOSIT_CONFIRMED", "تم تأكيد العربون",
                    "تم تأكيد عربونك الخاص برحلة " + lead.getTripTitle() + " من قبل الشركة.");

            case MARK_FULLY_PAID -> toCustomer(lead, "FULL_PAYMENT_CONFIRMED", "تم تأكيد السداد الكامل",
                    "تم تأكيد سدادك الكامل لرحلة " + lead.getTripTitle() + " من قبل الشركة.");

            case REPORT_COMMISSION_PAID -> toAdmins(lead, "COMMISSION_CONFIRMATION_REQUIRED", "تأكيد دفع عمولة",
                    "أبلغت إحدى الشركات عن دفع عمولتها. الرجاء التأكيد حتى يتم صرف الكاش باك.");

            case CONFIRM_COMMISSION_PAID -> toCompany(lead, "COMMISSION_PAID", "تم تأكيد العمولة",
                    "تم تأكيد دفع عمولتكم من قبل المنصة.");

            case PAY_CASHBACK -> toCustomer(lead, "CASHBACK_PAID", "تم إرسال الكاش باك",
                    "تم إرسال الكاش باك الخاص بك إلى محفظة " + walletLabel(lead.getCustomer().getWalletType()) + ".");

            case CANCEL -> leadCancelled(lead, statusBeforeAction);
        }
    }

    /**
     * The company always needs to know it has lost the booking. Admins are told as well once the
     * lead had reached DEPOSIT_PAID, because from that point a cancellation means real money
     * changed hands and someone has to sort out the refund.
     */
    private void leadCancelled(Lead lead, LeadStatus statusBeforeCancel) {
        toCompany(lead, "LEAD_CANCELLED", "قام عميل بالإلغاء",
                "قام أحد العملاء بإلغاء حجزه لرحلة " + lead.getTripTitle() + ".");

        if (statusBeforeCancel.isAtLeast(LeadStatus.DEPOSIT_PAID)) {
            toAdmins(lead, "PAID_LEAD_CANCELLED", "تم إلغاء حجز مدفوع",
                    "قام عميل بإلغاء رحلة " + lead.getTripTitle() + " بعد الدفع. "
                            + "تم إلغاء العمولة، ويجب متابعة عملية الاسترداد.");
        }
    }

    /**
     * An admin forced this lead's status directly, bypassing the normal report/confirm steps — see
     * {@link OverrideLeadStatusUseCase}. Both sides are told, since either could otherwise be
     * surprised by a status that neither of them produced.
     */
    public void statusOverridden(Lead lead, LeadStatus previous, LeadStatus target, String reason) {
        String body = "قام أحد المسؤولين بتغيير حالة هذا الحجز من \"" + statusLabel(previous) + "\" إلى \""
                + statusLabel(target) + "\" لرحلة " + lead.getTripTitle() + ". السبب: " + reason;
        toCustomer(lead, "LEAD_STATUS_OVERRIDDEN", "تم تحديث حالة حجزك من قبل المسؤول", body);
        toCompany(lead, "LEAD_STATUS_OVERRIDDEN", "تم تحديث حالة أحد الحجوزات من قبل المسؤول", body);
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
        return Map.of("leadId", lead.getId().toString(), "tripId", lead.getTripId().toString());
    }

    private static String statusLabel(LeadStatus status) {
        return switch (status) {
            case INTERESTED -> "مهتم";
            case CONTACTED -> "تم التواصل";
            case CONFIRMED -> "مؤكد";
            case PENDING_DEPOSIT_CONFIRMATION -> "بانتظار تأكيد العربون";
            case DEPOSIT_PAID -> "تم دفع العربون";
            case PENDING_FULL_PAYMENT_CONFIRMATION -> "بانتظار تأكيد السداد الكامل";
            case FULLY_PAID -> "تم السداد الكامل";
            case PENDING_COMMISSION_CONFIRMATION -> "بانتظار تأكيد العمولة";
            case COMMISSION_PAID -> "تم دفع العمولة";
            case CASHBACK_PAID -> "تم صرف الكاش باك";
            case CANCELLED -> "ملغي";
        };
    }

    private static String walletLabel(WalletType walletType) {
        return switch (walletType) {
            case VODAFONE_CASH -> "فودافون كاش";
            case ETISALAT_CASH -> "اتصالات كاش";
            case INSTA_PAY -> "إنستاباي";
        };
    }
}
