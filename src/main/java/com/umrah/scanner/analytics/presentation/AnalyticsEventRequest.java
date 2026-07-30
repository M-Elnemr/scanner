package com.umrah.scanner.analytics.presentation;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;

public record AnalyticsEventRequest(
        @NotBlank String eventType,
        String entityType,
        UUID entityId,
        Map<String, Object> metadata) {
}
