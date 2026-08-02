package com.umrah.scanner.commission.application;

import com.umrah.scanner.commission.domain.Commission;
import com.umrah.scanner.commission.domain.CommissionStatus;
import com.umrah.scanner.commission.infrastructure.CommissionRepository;
import com.umrah.scanner.lead.domain.Lead;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the commission ledger in step with a lead's lifecycle. The lead's status machine decides
 * <em>whether</em> a payment step is legal; this service only records the money side of the step
 * that was already authorised, so there is exactly one owner for each concern.
 *
 * <p>Both entry points are idempotent-by-upsert on the lead, which is what lets the admin confirm a
 * commission that was never reported by the company (the direct FULLY_PAID to COMMISSION_PAID path).
 */
@Service
public class CommissionLedgerService {

    private final CommissionRepository commissionRepository;

    public CommissionLedgerService(CommissionRepository commissionRepository) {
        this.commissionRepository = commissionRepository;
    }

    /** The company states it has settled; the row moves to REPORTED and awaits admin confirmation. */
    @Transactional
    public Commission recordReported(Lead lead, UUID companyUserId, Instant at) {
        Commission commission = openOrCreate(lead);
        commission.setStatus(CommissionStatus.REPORTED);
        commission.setReportedBy(companyUserId);
        commission.setReportedAt(at);
        return commissionRepository.save(commission);
    }

    /** Admin confirms receipt — the only state from which cashback becomes payable. */
    @Transactional
    public Commission recordConfirmed(Lead lead, UUID adminUserId, Instant at) {
        Commission commission = openOrCreate(lead);
        commission.setStatus(CommissionStatus.CONFIRMED);
        commission.setConfirmedBy(adminUserId);
        commission.setConfirmedAt(at);
        return commissionRepository.save(commission);
    }

    @Transactional(readOnly = true)
    public Optional<Commission> findByLead(UUID leadId) {
        return commissionRepository.findByLeadId(leadId);
    }

    private Commission openOrCreate(Lead lead) {
        return commissionRepository.findByLeadId(lead.getId()).orElseGet(() -> {
            Commission commission = new Commission();
            commission.setLead(lead);
            commission.setCompany(lead.getCompany());
            // Always the lead's snapshot — the company's current rate is irrelevant by now.
            commission.setAmount(lead.getCommissionAmount());
            commission.setStatus(CommissionStatus.PENDING);
            return commission;
        });
    }
}
