package com.umrah.scanner.currency.application;

import com.umrah.scanner.currency.domain.Currency;
import com.umrah.scanner.currency.infrastructure.CurrencyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrencyQueryService {

    private final CurrencyRepository currencyRepository;

    public CurrencyQueryService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Transactional(readOnly = true)
    public List<Currency> listAll() {
        return currencyRepository.findAllByOrderByCodeAsc();
    }
}
