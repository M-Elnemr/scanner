package com.umrah.scanner.analytics.presentation;

import com.umrah.scanner.analytics.application.AnalyticsReportingService;
import com.umrah.scanner.analytics.application.AudienceSplit;
import com.umrah.scanner.analytics.application.EventTypeCount;
import com.umrah.scanner.analytics.application.TripViewCount;
import com.umrah.scanner.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read side of the analytics event pipeline (see {@code AnalyticsEventController}) — admin-only,
 * deliberately small: bounded date-range aggregates, not a general query API. Every handler
 * defaults {@code from}/{@code to} to the last 30 days and clamps to 90 — see
 * {@link AnalyticsReportingService}.
 */
@Tag(name = "Admin: Analytics", description = "Usage statistics — guest and signed-in, across the app and website.")
@RestController
public class AdminAnalyticsController {

    private final AnalyticsReportingService analyticsReportingService;

    public AdminAnalyticsController(AnalyticsReportingService analyticsReportingService) {
        this.analyticsReportingService = analyticsReportingService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/analytics/events-summary")
    public ApiResponse<List<EventTypeCount>> eventsSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.of(analyticsReportingService.eventsByType(from, to));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/analytics/audience")
    public ApiResponse<AudienceSplit> audience(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.of(analyticsReportingService.audience(from, to));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/analytics/most-viewed-trips")
    public ApiResponse<List<TripViewCount>> mostViewedTrips(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ApiResponse.of(analyticsReportingService.mostViewedTrips(from, to, limit));
    }
}
