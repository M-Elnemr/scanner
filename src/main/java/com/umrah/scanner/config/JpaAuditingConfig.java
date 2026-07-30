package com.umrah.scanner.config;

import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.common.security.CurrentUserAccessor;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorAware(CurrentUserAccessor currentUserAccessor) {
        return () -> currentUserAccessor.get().map(AuthenticatedUser::userId);
    }
}
