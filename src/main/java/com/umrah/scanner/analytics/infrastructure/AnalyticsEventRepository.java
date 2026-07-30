package com.umrah.scanner.analytics.infrastructure;

import com.umrah.scanner.analytics.domain.AnalyticsEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {
}
