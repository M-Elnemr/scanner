package com.umrah.scanner.lead.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ForbiddenException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.TravelerParty;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets the customer correct the party size while the booking is still open. The cut-off lives on
 * {@link Lead#changeTravelers}, not here, so it holds no matter who calls it.
 *
 * <p>Deliberately does not reprice the lead: commission and cashback are fixed at creation.
 */
@Service
public class UpdateLeadTravelersUseCase {

    private final LeadRepository leadRepository;
    private final AuditLogService auditLogService;

    public UpdateLeadTravelersUseCase(LeadRepository leadRepository, AuditLogService auditLogService) {
        this.leadRepository = leadRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Lead execute(UUID leadId, UUID customerUserId, int adultCount, int childCount, int infantCount) {
        Lead lead = leadRepository.findWithDetailsById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));
        if (!lead.getCustomer().getUser().getId().equals(customerUserId)) {
            throw new ForbiddenException("This lead does not belong to the caller");
        }

        Map<String, Integer> before = snapshot(lead);
        lead.changeTravelers(TravelerParty.of(adultCount, childCount, infantCount));
        auditLogService.record(customerUserId, "LEAD_TRAVELERS_CHANGED", "Lead", leadId, before, snapshot(lead));
        return lead;
    }

    private Map<String, Integer> snapshot(Lead lead) {
        TravelerParty travelers = lead.getTravelers();
        return Map.of(
                "adultCount", travelers.getAdultCount(),
                "childCount", travelers.getChildCount(),
                "infantCount", travelers.getInfantCount());
    }
}
