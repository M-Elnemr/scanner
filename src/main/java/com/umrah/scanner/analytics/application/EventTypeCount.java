package com.umrah.scanner.analytics.application;

/** One row of the events-by-type aggregate — see {@code AnalyticsEventRepository#countByEventTypeBetween}. */
public record EventTypeCount(String eventType, long count) {
}
