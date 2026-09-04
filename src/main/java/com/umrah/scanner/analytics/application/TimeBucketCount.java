package com.umrah.scanner.analytics.application;

import java.time.Instant;

/**
 * One point on the events-over-time trend — {@code bucket} is the truncated timestamp
 * (minute/hour/day, chosen server-side by {@link AnalyticsReportingService} from the requested
 * span) an interface projection maps a native query's {@code date_trunc(...)} column onto.
 */
public interface TimeBucketCount {
    Instant getBucket();

    long getCount();
}
