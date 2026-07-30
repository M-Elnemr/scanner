package com.umrah.scanner.customer.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.customer.infrastructure.CustomerProfileRepository;
import java.util.Optional;
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

    /** The profile is created lazily, so "not found yet" is a normal, expected state — not an error. */
    @Transactional(readOnly = true)
    public Optional<CustomerProfile> findByUserId(UUID userId) {
        return customerProfileRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isProfileCompleted(UUID userId) {
        return customerProfileRepository.findByUserId(userId)
                .map(CustomerProfile::isProfileCompleted)
                .orElse(false);
    }
}
