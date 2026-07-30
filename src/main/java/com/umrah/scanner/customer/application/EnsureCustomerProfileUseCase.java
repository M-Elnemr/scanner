package com.umrah.scanner.customer.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.customer.infrastructure.CustomerProfileRepository;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Some actions (favouriting a trip) need a {@link CustomerProfile} row to attach to, but don't
 * require the profile to be complete. This creates the bare row on first use instead of forcing
 * every such action to special-case "profile doesn't exist yet".
 */
@Service
public class EnsureCustomerProfileUseCase {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;

    public EnsureCustomerProfileUseCase(CustomerProfileRepository customerProfileRepository, UserRepository userRepository) {
        this.customerProfileRepository = customerProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public UUID execute(UUID userId) {
        return customerProfileRepository.findByUserId(userId)
                .map(CustomerProfile::getId)
                .orElseGet(() -> {
                    CustomerProfile profile = new CustomerProfile();
                    profile.setUser(userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("User", userId)));
                    profile.setProfileCompleted(false);
                    return customerProfileRepository.save(profile).getId();
                });
    }
}
