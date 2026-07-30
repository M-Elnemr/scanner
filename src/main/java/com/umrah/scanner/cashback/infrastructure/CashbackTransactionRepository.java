package com.umrah.scanner.cashback.infrastructure;

import com.umrah.scanner.cashback.domain.CashbackStatus;
import com.umrah.scanner.cashback.domain.CashbackTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashbackTransactionRepository extends JpaRepository<CashbackTransaction, UUID> {

    Optional<CashbackTransaction> findByLeadId(UUID leadId);

    boolean existsByLeadId(UUID leadId);

    Page<CashbackTransaction> findAllByCustomerIdAndStatus(UUID customerId, CashbackStatus status, Pageable pageable);
}
