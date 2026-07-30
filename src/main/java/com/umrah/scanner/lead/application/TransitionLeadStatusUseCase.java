package com.umrah.scanner.lead.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.ForbiddenException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.lead.domain.LeadStatusHistory;
import com.umrah.scanner.lead.domain.LeadTransitionPolicy;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.lead.infrastructure.LeadStatusHistoryRepository;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import com.umrah.scanner.user.domain.Role;
import com.umrah.scanner.user.domain.User;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place a lead's status is ever mutated. Every controller and every other
 * use case that needs to move a lead forward (company, customer, or admin action) goes
 * through here so the allow-list in {@link LeadTransitionPolicy} can never be bypassed.
 */
@Service
public class TransitionLeadStatusUseCase {

    private final LeadRepository leadRepository;
    private final LeadStatusHistoryRepository leadStatusHistoryRepository;
    private final UserRepository userRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final AuditLogService auditLogService;

    public TransitionLeadStatusUseCase(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository leadStatusHistoryRepository,
            UserRepository userRepository,
            NotificationDispatcher notificationDispatcher,
            AuditLogService auditLogService) {
        this.leadRepository = leadRepository;
        this.leadStatusHistoryRepository = leadStatusHistoryRepository;
        this.userRepository = userRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Lead execute(UUID leadId, LeadStatus target, Role actorRole, UUID actorUserId) {
        Lead lead = leadRepository.findWithDetailsById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));
        requireOwnership(lead, actorRole, actorUserId);

        LeadStatus current = lead.getStatus();
        if (!LeadTransitionPolicy.isAllowed(current, target, actorRole)) {
            throw new ConflictException("Cannot move lead from " + current + " to " + target + " as " + actorRole);
        }

        applyTransition(lead, target, actorUserId);

        LeadTransitionPolicy.systemCascadeAfter(target)
                .ifPresent(cascadeTarget -> applyTransition(lead, cascadeTarget, null));

        return lead;
    }

    /**
     * "Confirm payment" is a single fixed action from the customer's point of view — the server
     * decides whether that means moving to CUSTOMER_CONFIRMED or on to PAYMENT_PENDING, so the
     * client never has to know the exact state machine.
     */
    @Transactional
    public Lead confirmPayment(UUID leadId, UUID customerUserId) {
        Lead lead = leadRepository.findWithDetailsById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));
        requireOwnership(lead, Role.CUSTOMER, customerUserId);

        LeadStatus target = switch (lead.getStatus()) {
            case INTERESTED, COMPANY_CONTACTED -> LeadStatus.CUSTOMER_CONFIRMED;
            case CUSTOMER_CONFIRMED -> LeadStatus.PAYMENT_PENDING;
            default -> throw new ConflictException("Payment cannot be confirmed from " + lead.getStatus());
        };
        applyTransition(lead, target, customerUserId);
        return lead;
    }

    /** Used by admin-side use cases (commission release, cashback send) that already own the ADMIN check. */
    @Transactional
    public Lead applyAsAdmin(UUID leadId, LeadStatus target, UUID adminUserId) {
        Lead lead = leadRepository.findWithDetailsById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));
        LeadStatus current = lead.getStatus();
        if (!LeadTransitionPolicy.isAllowed(current, target, Role.ADMIN)) {
            throw new ConflictException("Cannot move lead from " + current + " to " + target + " as ADMIN");
        }
        applyTransition(lead, target, adminUserId);
        return lead;
    }

    private void applyTransition(Lead lead, LeadStatus target, UUID actorUserId) {
        LeadStatus previous = lead.getStatus();
        lead.setStatus(target);

        LeadStatusHistory history = new LeadStatusHistory();
        history.setLead(lead);
        history.setFromStatus(previous);
        history.setToStatus(target);
        history.setChangedBy(actorUserId);
        leadStatusHistoryRepository.save(history);

        auditLogService.record(actorUserId, "LEAD_STATUS_CHANGED", "Lead", lead.getId(), previous, target);

        if (target == LeadStatus.ADMIN_REVIEW) {
            notifyAdmins(lead);
        }
    }

    private void requireOwnership(Lead lead, Role actorRole, UUID actorUserId) {
        boolean owns = switch (actorRole) {
            case COMPANY -> lead.getCompany().getUser().getId().equals(actorUserId);
            case CUSTOMER -> lead.getCustomer().getUser().getId().equals(actorUserId);
            case ADMIN -> true;
        };
        if (!owns) {
            throw new ForbiddenException("This lead does not belong to the caller");
        }
    }

    private void notifyAdmins(Lead lead) {
        for (User admin : userRepository.findAllByRole(Role.ADMIN)) {
            notificationDispatcher.dispatch(
                    admin.getId(),
                    "LEAD_ADMIN_REVIEW",
                    "Lead awaiting review",
                    "A lead has been marked paid and is ready for commission review.",
                    Map.of("leadId", lead.getId().toString()));
        }
    }
}
