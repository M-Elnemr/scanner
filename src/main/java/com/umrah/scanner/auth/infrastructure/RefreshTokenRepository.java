package com.umrah.scanner.auth.infrastructure;

import com.umrah.scanner.auth.domain.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query("""
        update RefreshToken t
           set t.revoked = true, t.revokedAt = :at
         where t.user.id = :userId
           and t.revoked = false
        """)
    int revokeAllActiveForUser(@Param("userId") UUID userId, @Param("at") Instant at);
}
