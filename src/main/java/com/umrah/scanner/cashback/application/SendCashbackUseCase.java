package com.umrah.scanner.cashback.application;

import com.umrah.scanner.cashback.domain.CashbackStatus;
import com.umrah.scanner.cashback.domain.CashbackTransaction;
import com.umrah.scanner.cashback.infrastructure.CashbackTransactionRepository;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.customer.domain.CustomerProfile;
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
public class SendCashbackUseCase {

    private final CashbackTransactionRepository cashbackTransactionRepository;
    private final LeadRepository leadRepository;
    private final TransitionLeadStatusUseCase transitionLeadStatusUseCase;
    private final NotificationDispatcher notificationDispatcher;

    public SendCashbackUseCase(
            CashbackTransactionRepository cashbackTransactionRepository,
            LeadRepository leadRepository,
            TransitionLeadStatusUseCase transitionLeadStatusUseCase,
            NotificationDispatcher notificationDispatcher) {
        this.cashbackTransactionRepository = cashbackTransactionRepository;
        this.leadRepository = leadRepository;
        this.transitionLeadStatusUseCase = transitionLeadStatusUseCase;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional
    public CashbackTransaction execute(UUID adminUserId, UUID leadId, BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new ValidationException("Cashback amount must be zero or positive");
        }
        Lead lead = leadRepository.findById(leadId).orElseThrow(() -> NotFoundException.of("Lead", leadId));
        if (lead.getStatus() != LeadStatus.COMMISSION_RELEASED) {
            throw new ConflictException("Cashback can only be sent for a lead in COMMISSION_RELEASED");
        }
        if (cashbackTransactionRepository.existsByLeadId(leadId)) {
            throw new ConflictException("Cashback already sent for this lead");
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
        cashback.setAmount(amount);
        cashback.setStatus(CashbackStatus.SENT);
        cashback.setSentAt(Instant.now());
        cashback = cashbackTransactionRepository.save(cashback);

        transitionLeadStatusUseCase.applyAsAdmin(leadId, LeadStatus.CASHBACK_SENT, adminUserId);

        notificationDispatcher.dispatch(
                customer.getUser().getId(),
                "CASHBACK_SENT",
                "Cashback sent",
                "Your cashback has been sent to your " + customer.getWalletType() + " wallet.",
                Map.of("leadId", leadId.toString(), "amount", amount.toPlainString()));

        return cashback;
    }
}
