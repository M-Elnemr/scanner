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

    // metadata is the one unbounded field on this request — now that the endpoint also accepts
    // unauthenticated (guest) traffic, a caller could otherwise attach an arbitrarily large blob.
    // Truncated rather than rejected: a best-effort analytics write never fails the caller.
    private static final int MAX_METADATA_JSON_LENGTH = 2048;

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
            event.setMetadata(serializeMetadata(metadata));
            analyticsEventRepository.save(event);
        } catch (RuntimeException e) {
            log.warn("Discarding analytics event {} — failed to record", eventType, e);
        }
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        String json = objectMapper.writeValueAsString(metadata);
        return json.length() > MAX_METADATA_JSON_LENGTH ? null : json;
    }
}
