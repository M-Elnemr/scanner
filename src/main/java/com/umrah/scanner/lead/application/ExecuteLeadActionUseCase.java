package com.umrah.scanner.lead.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.cashback.application.CashbackPayoutService;
import com.umrah.scanner.commission.application.CommissionLedgerService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.ForbiddenException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadAction;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.lead.domain.LeadStatusHistory;
import com.umrah.scanner.lead.domain.LeadTransitionPolicy;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.lead.infrastructure.LeadStatusHistoryRepository;
import com.umrah.scanner.user.domain.Role;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place a lead's status is ever mutated. Every controller entry point — customer,
 * company or admin — funnels through here, so {@link LeadTransitionPolicy}'s allow-list cannot be
 * bypassed and the audit trail cannot be skipped.
 *
 * <p>Callers name an <em>action</em>, never a target status; the target is derived from where the
 * lead currently sits. That is what makes an illegal jump (INTERESTED to FULLY_PAID, FULLY_PAID to
 * CASHBACK_PAID, or anything backwards) unrepresentable rather than merely rejected.
 *
 * <p>The money side of an action is delegated: the commission ledger and the cashback payout each
 * own their own record. This service owns authorisation, state and audit — nothing else.
 */
@Service
public class ExecuteLeadActionUseCase {

    private final LeadRepository leadRepository;
    private final LeadStatusHistoryRepository leadStatusHistoryRepository;
    private final CommissionLedgerService commissionLedgerService;
    private final CashbackPayoutService cashbackPayoutService;
    private final LeadNotifier leadNotifier;
    private final AuditLogService auditLogService;

    public ExecuteLeadActionUseCase(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository leadStatusHistoryRepository,
            CommissionLedgerService commissionLedgerService,
            CashbackPayoutService cashbackPayoutService,
            LeadNotifier leadNotifier,
            AuditLogService auditLogService) {
        this.leadRepository = leadRepository;
        this.leadStatusHistoryRepository = leadStatusHistoryRepository;
        this.commissionLedgerService = commissionLedgerService;
        this.cashbackPayoutService = cashbackPayoutService;
        this.leadNotifier = leadNotifier;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Lead execute(UUID leadId, LeadAction action, Role actorRole, UUID actorUserId, String note) {
        Lead lead = leadRepository.findWithDetailsById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));

        if (LeadTransitionPolicy.actorOf(action) != actorRole) {
            throw new ForbiddenException(actorRole + " is not allowed to perform " + action);
        }
        requireOwnership(lead, actorRole, actorUserId);

        LeadStatus current = lead.getStatus();
        LeadStatus target = LeadTransitionPolicy.targetOf(action, current)
                .orElseThrow(() -> new ConflictException(action + " is not available while the lead is " + current));

        Instant now = Instant.now();
        applyMoneySideEffects(lead, action, actorUserId, now);

        lead.setStatus(target);
        lead.recordAction(action, actorUserId, now);
        recordHistory(lead, current, target, actorUserId, note);

        auditLogService.record(actorUserId, "LEAD_" + action.name(), "Lead", lead.getId(), current, target);
        leadNotifier.actionPerformed(lead, action, current);

        return lead;
    }

    /**
     * Runs before the status moves so a rejected payout (no wallet on file, an already-paid lead)
     * leaves the lead exactly where it was.
     */
    private void applyMoneySideEffects(Lead lead, LeadAction action, UUID actorUserId, Instant at) {
        switch (action) {
            case REPORT_COMMISSION_PAID -> commissionLedgerService.recordReported(lead, actorUserId, at);
            case CONFIRM_COMMISSION_PAID -> commissionLedgerService.recordConfirmed(lead, actorUserId, at);
            case PAY_CASHBACK -> cashbackPayoutService.pay(lead, actorUserId, at);

            // Voiding the debt, not refunding the customer: money that already changed hands
            // between them and the company is outside this system's reach.
            case CANCEL -> commissionLedgerService.recordCancelled(lead);

            default -> {
                // Deposit and full-payment steps are agreements between customer and company;
                // no platform ledger entry is created for them.
            }
        }
    }

    private void recordHistory(Lead lead, LeadStatus from, LeadStatus to, UUID actorUserId, String note) {
        LeadStatusHistory history = new LeadStatusHistory();
        history.setLead(lead);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actorUserId);
        history.setNote(note);
        leadStatusHistoryRepository.save(history);
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
}
