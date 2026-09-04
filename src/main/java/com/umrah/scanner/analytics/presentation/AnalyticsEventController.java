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

    // currentUser is null for a guest — this endpoint is permitAll (see SecurityConfig) so the
    // request reaches here with no principal at all rather than being rejected upstream.
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/api/v1/analytics/events")
    public void record(@AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody AnalyticsEventRequest request) {
        analyticsEventService.record(
                currentUser != null ? currentUser.userId() : null,
                request.eventType(), request.entityType(), request.entityId(), request.metadata());
    }
}
