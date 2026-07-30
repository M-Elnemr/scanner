package com.umrah.scanner.analytics.presentation;

import com.umrah.scanner.analytics.application.AnalyticsEventService;
import com.umrah.scanner.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsEventController {

    private final AnalyticsEventService analyticsEventService;

    public AnalyticsEventController(AnalyticsEventService analyticsEventService) {
        this.analyticsEventService = analyticsEventService;
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/api/v1/analytics/events")
    public void record(@AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody AnalyticsEventRequest request) {
        analyticsEventService.record(
                currentUser.userId(), request.eventType(), request.entityType(), request.entityId(), request.metadata());
    }
}
