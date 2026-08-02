package com.umrah.scanner.cashback.application;

import com.umrah.scanner.cashback.domain.CashbackStatus;
import com.umrah.scanner.cashback.domain.CashbackTransaction;
import com.umrah.scanner.cashback.infrastructure.CashbackTransactionRepository;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.lead.domain.Lead;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records the cashback payout for a lead. Whether the payout is <em>allowed</em> — i.e. that the
 * company's commission has actually been confirmed first — is settled by the lead status machine
 * before this is ever called; this service owns only the payout record itself.
 *
 * <p>The transfer is manual today, so the row is written as SENT at the moment the admin confirms
 * it. Wiring an automated wallet transfer later means writing PENDING here and flipping the row
 * from a payment-provider callback — no caller changes.
 */
@Service
public class CashbackPayoutService {

    private final CashbackTransactionRepository cashbackTransactionRepository;

    public CashbackPayoutService(CashbackTransactionRepository cashbackTransactionRepository) {
        this.cashbackTransactionRepository = cashbackTransactionRepository;
    }

    @Transactional
    public CashbackTransaction pay(Lead lead, UUID adminUserId, Instant at) {
        if (cashbackTransactionRepository.existsByLeadId(lead.getId())) {
            throw new ConflictException("Cashback has already been paid for this lead");
        }

        CustomerProfile customer = lead.getCustomer();
        if (customer.getWalletType() == null || customer.getCashbackWalletNumber() == null) {
            throw new ValidationException("Customer has no cashback wallet on file");
        }

        CashbackTransaction cashback = new CashbackTransaction();
        cashback.setLead(lead);
        cashback.setCustomer(customer);
        cashback.setWalletType(customer.getWalletType());
        cashback.setWalletNumber(customer.getCashbackWalletNumber());
        // The amount fixed at lead creation — never re-derived from today's commission rules.
        cashback.setAmount(lead.getCashbackAmount());
        cashback.setStatus(CashbackStatus.SENT);
        cashback.setPaidBy(adminUserId);
        cashback.setSentAt(at);
        return cashbackTransactionRepository.save(cashback);
    }

    @Transactional(readOnly = true)
    public Optional<CashbackTransaction> findByLead(UUID leadId) {
        return cashbackTransactionRepository.findByLeadId(leadId);
    }
}
