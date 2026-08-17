package com.umrah.scanner.lead.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.lead.domain.LeadStatusHistory;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.lead.infrastructure.LeadStatusHistoryRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The operator escape hatch: forces a lead directly to any status, bypassing
 * {@link com.umrah.scanner.lead.domain.LeadTransitionPolicy} entirely. Deliberately its own use
 * case rather than a new {@code LeadAction} — the transition table exists to make an illegal jump
 * unrepresentable, and folding an "anything goes" path into it would mean that guarantee no longer
 * holds for anyone. This is for correcting the record (a status that never should have stuck, data
 * entered by hand while migrating a legacy booking), not a second way to run the normal workflow —
 * it therefore never touches the commission or cashback ledgers the typed actions in
 * {@link ExecuteLeadActionUseCase} do. An admin who also wants those to move still uses the real
 * confirm-commission / pay-cashback endpoints.
 */
@Service
public class OverrideLeadStatusUseCase {

    private final LeadRepository leadRepository;
    private final LeadStatusHistoryRepository leadStatusHistoryRepository;
    private final LeadNotifier leadNotifier;
    private final AuditLogService auditLogService;

    public OverrideLeadStatusUseCase(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository leadStatusHistoryRepository,
            LeadNotifier leadNotifier,
            AuditLogService auditLogService) {
        this.leadRepository = leadRepository;
        this.leadStatusHistoryRepository = leadStatusHistoryRepository;
        this.leadNotifier = leadNotifier;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Lead execute(UUID adminUserId, UUID leadId, LeadStatus target, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("A reason is required to override a lead's status");
        }
        Lead lead = leadRepository.findWithDetailsById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));

        LeadStatus previous = lead.getStatus();
        if (previous == target) {
            throw new ConflictException("The lead is already " + target);
        }

        // Reviving a lead out of a terminal status re-occupies the customer's one-preserved-journey
        // slot (uq_leads_customer_active). Check first so this returns a friendly 409 naming the
        // trip in the way, instead of a raw constraint violation surfacing as a 500.
        if (previous.isTerminal() && !target.isTerminal()) {
            leadRepository.findActiveByCustomerId(lead.getCustomer().getId())
                    .filter(active -> !active.getId().equals(leadId))
                    .ifPresent(active -> {
                        throw new ActiveLeadExistsException(active);
                    });
        }

        lead.setStatus(target);

        LeadStatusHistory history = new LeadStatusHistory();
        history.setLead(lead);
        history.setFromStatus(previous);
        history.setToStatus(target);
        history.setChangedBy(adminUserId);
        history.setNote(reason);
        leadStatusHistoryRepository.save(history);

        auditLogService.record(adminUserId, "LEAD_STATUS_OVERRIDDEN", "Lead", leadId, previous, target);
        leadNotifier.statusOverridden(lead, previous, target, reason);

        return lead;
    }
}
