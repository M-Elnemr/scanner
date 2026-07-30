package com.umrah.scanner.customer.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.customer.domain.CustomerProfile;
import com.umrah.scanner.customer.infrastructure.CustomerProfileRepository;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customers have no separate "register" step — the profile is created lazily on first edit. */
@Service
public class UpdateCustomerProfileUseCase {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;

    public UpdateCustomerProfileUseCase(CustomerProfileRepository customerProfileRepository, UserRepository userRepository) {
        this.customerProfileRepository = customerProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CustomerProfile execute(UUID userId, UpdateCustomerProfileCommand command) {
        CustomerProfile profile = customerProfileRepository.findByUserId(userId).orElseGet(() -> {
            CustomerProfile created = new CustomerProfile();
            created.setUser(userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("User", userId)));
            return created;
        });

        profile.setFullName(command.fullName());
        profile.setPhone(command.phone());
        profile.setCashbackWalletNumber(command.cashbackWalletNumber());
        profile.setWalletType(command.walletType());
        profile.setProfileCompleted(isComplete(profile));

        return customerProfileRepository.save(profile);
    }

    private boolean isComplete(CustomerProfile profile) {
        return profile.getFullName() != null && !profile.getFullName().isBlank()
                && profile.getPhone() != null && !profile.getPhone().isBlank()
                && profile.getCashbackWalletNumber() != null && !profile.getCashbackWalletNumber().isBlank()
                && profile.getWalletType() != null;
    }
}
