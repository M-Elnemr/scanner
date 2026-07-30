package com.umrah.scanner.analytics.application;

import com.umrah.scanner.analytics.domain.AnalyticsEvent;
import com.umrah.scanner.analytics.infrastructure.AnalyticsEventRepository;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Lightweight, best-effort behavioral event capture. Never allowed to fail the caller's use case. */
@Service
public class AnalyticsEventService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventService.class);

    private final AnalyticsEventRepository analyticsEventRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsEventService(AnalyticsEventRepository analyticsEventRepository, ObjectMapper objectMapper) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, String eventType, String entityType, UUID entityId, Map<String, Object> metadata) {
        try {
            AnalyticsEvent event = new AnalyticsEvent();
            event.setUserId(userId);
            event.setEventType(eventType);
            event.setEntityType(entityType);
            event.setEntityId(entityId);
            event.setMetadata(metadata == null || metadata.isEmpty() ? null : objectMapper.writeValueAsString(metadata));
            analyticsEventRepository.save(event);
        } catch (RuntimeException e) {
            log.warn("Discarding analytics event {} — failed to record", eventType, e);
        }
    }
}
