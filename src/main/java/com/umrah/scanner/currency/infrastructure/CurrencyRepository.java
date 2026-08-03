package com.umrah.scanner.currency.infrastructure;

import com.umrah.scanner.currency.domain.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, UUID> {

    List<Currency> findAllByOrderByCodeAsc();

    Optional<Currency> findByCode(String code);
}
