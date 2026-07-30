package com.umrah.scanner.user.infrastructure;

import com.umrah.scanner.user.domain.Role;
import com.umrah.scanner.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    boolean existsByGoogleSub(String googleSub);

    List<User> findAllByRole(Role role);
}
