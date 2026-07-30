package com.umrah.scanner.config;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    /**
     * Placeholder until Spring Security is wired in — will resolve the authenticated
     * principal's user id from the security context instead of always returning empty.
     */
    @Bean
    public AuditorAware<UUID> auditorAware() {
        return Optional::empty;
    }
}
