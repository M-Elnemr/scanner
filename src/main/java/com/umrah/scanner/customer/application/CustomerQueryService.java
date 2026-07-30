package com.umrah.scanner.customer.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.customer.infrastructure.CustomerProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerQueryService {

    private final CustomerProfileRepository customerProfileRepository;

    public CustomerQueryService(CustomerProfileRepository customerProfileRepository) {
        this.customerProfileRepository = customerProfileRepository;
    }

    @Transactional(readOnly = true)
    public CustomerProfile getByUserId(UUID userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> NotFoundException.of("CustomerProfile", userId));
    }

    @Transactional(readOnly = true)
    public boolean isProfileCompleted(UUID userId) {
        return customerProfileRepository.findByUserId(userId)
                .map(CustomerProfile::isProfileCompleted)
                .orElse(false);
    }
}
