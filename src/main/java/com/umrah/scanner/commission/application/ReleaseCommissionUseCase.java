package com.umrah.scanner.commission.application;

import com.umrah.scanner.commission.domain.Commission;
import com.umrah.scanner.commission.domain.CommissionStatus;
import com.umrah.scanner.commission.infrastructure.CommissionRepository;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.lead.application.TransitionLeadStatusUseCase;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.lead.infrastructure.LeadRepository;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseCommissionUseCase {

    private final CommissionRepository commissionRepository;
    private final LeadRepository leadRepository;
    private final TransitionLeadStatusUseCase transitionLeadStatusUseCase;
    private final NotificationDispatcher notificationDispatcher;

    public ReleaseCommissionUseCase(
            CommissionRepository commissionRepository,
            LeadRepository leadRepository,
            TransitionLeadStatusUseCase transitionLeadStatusUseCase,
            NotificationDispatcher notificationDispatcher) {
        this.commissionRepository = commissionRepository;
        this.leadRepository = leadRepository;
        this.transitionLeadStatusUseCase = transitionLeadStatusUseCase;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional
    public Commission execute(UUID adminUserId, UUID leadId, BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new ValidationException("Commission amount must be zero or positive");
        }
        Lead lead = leadRepository.findWithDetailsById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));
        if (lead.getStatus() != LeadStatus.ADMIN_REVIEW) {
            throw new ConflictException("Commission can only be released for a lead in ADMIN_REVIEW");
        }
        if (commissionRepository.existsByLeadId(leadId)) {
            throw new ConflictException("Commission already released for this lead");
        }

        Commission commission = new Commission();
        commission.setLead(lead);
        commission.setCompany(lead.getCompany());
        commission.setAmount(amount);
        commission.setStatus(CommissionStatus.RELEASED);
        commission.setReleasedBy(adminUserId);
        commission.setReleasedAt(Instant.now());
        commission = commissionRepository.save(commission);

        transitionLeadStatusUseCase.applyAsAdmin(leadId, LeadStatus.COMMISSION_RELEASED, adminUserId);

        notificationDispatcher.dispatch(
                lead.getCompany().getUser().getId(),
                "COMMISSION_RELEASED",
                "Commission released",
                "Your commission for lead " + leadId + " has been released.",
                Map.of("leadId", leadId.toString(), "amount", amount.toPlainString()));

        return commission;
    }
}
