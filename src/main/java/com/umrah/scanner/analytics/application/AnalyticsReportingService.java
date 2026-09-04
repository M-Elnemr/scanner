package com.umrah.scanner.analytics.application;

import com.umrah.scanner.analytics.infrastructure.AnalyticsEventRepository;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the {@code analytics_events} pipeline the app has been writing to (see
 * {@code AnalyticsEventService}) with nothing ever reading it back — this is that read side,
 * kept deliberately cheap: every query is bounded to a caller-supplied instant range (clamped to
 * {@link #MAX_RANGE_DAYS}) and runs against the table's existing indexes; trip titles for the
 * most-viewed/most-shared lists are resolved with one batched lookup after the group-by, not a
 * join inside the aggregation query itself.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsReportingService {

    private static final int MAX_RANGE_DAYS = 90;
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final String TRIP_VIEW_EVENT_TYPE = "trip_view";
    private static final String TRIP_SHARE_EVENT_TYPE = "trip_share";
    private static final String TRIP_ENTITY_TYPE = "TRIP";

    private final AnalyticsEventRepository analyticsEventRepository;
    private final TripRepository tripRepository;

    public AnalyticsReportingService(AnalyticsEventRepository analyticsEventRepository, TripRepository tripRepository) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.tripRepository = tripRepository;
    }

    public List<EventTypeCount> eventsByType(Instant from, Instant to) {
        Range range = clamp(from, to);
        return analyticsEventRepository.countByEventTypeBetween(range.from(), range.to());
    }

    public AudienceSplit audience(Instant from, Instant to) {
        Range range = clamp(from, to);
        long guests = analyticsEventRepository.countGuestEventsBetween(range.from(), range.to());
        long identified = analyticsEventRepository.countIdentifiedEventsBetween(range.from(), range.to());
        long uniqueUsers = analyticsEventRepository.countDistinctUsersBetween(range.from(), range.to());
        return new AudienceSplit(guests, identified, uniqueUsers);
    }

    /**
     * Buckets the range into minute/hour/day points depending on its span, so a 30-minute pick
     * renders as per-minute points and a 30-day pick renders as daily points rather than one lump
     * total either way. The bucket unit is always chosen here, never taken from the caller, before
     * it reaches the native {@code date_trunc} query.
     */
    public List<TimeBucketCount> eventsOverTime(Instant from, Instant to) {
        Range range = clamp(from, to);
        Duration span = Duration.between(range.from(), range.to());
        String unit;
        if (span.compareTo(Duration.ofHours(3)) <= 0) {
            unit = "minute";
        } else if (span.compareTo(Duration.ofDays(3)) <= 0) {
            unit = "hour";
        } else {
            unit = "day";
        }
        return analyticsEventRepository.countByTimeBucket(unit, range.from(), range.to());
    }

    public List<TripViewCount> mostViewedTrips(Instant from, Instant to, int limit) {
        return mostTrips(TRIP_VIEW_EVENT_TYPE, from, to, limit);
    }

    public List<TripViewCount> mostSharedTrips(Instant from, Instant to, int limit) {
        return mostTrips(TRIP_SHARE_EVENT_TYPE, from, to, limit);
    }

    private List<TripViewCount> mostTrips(String eventType, Instant from, Instant to, int limit) {
        Range range = clamp(from, to);
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), 50));
        List<MostViewedTrip> rows = analyticsEventRepository.mostViewed(
                eventType, TRIP_ENTITY_TYPE, range.from(), range.to(), pageable);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> titles = tripRepository.findAllById(rows.stream().map(MostViewedTrip::tripId).toList())
                .stream()
                .collect(Collectors.toMap(Trip::getId, Trip::getTitle));
        return rows.stream().map(r -> new TripViewCount(r.tripId(), titles.get(r.tripId()), r.count())).toList();
    }

    /**
     * Defaults to the last {@link #DEFAULT_RANGE_DAYS} days when the caller sends neither bound,
     * and clamps the span to {@link #MAX_RANGE_DAYS} regardless — every query this service runs
     * stays cheap no matter what a caller asks for.
     */
    private Range clamp(Instant from, Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(Duration.ofDays(DEFAULT_RANGE_DAYS));
        if (effectiveFrom.isAfter(effectiveTo)) {
            effectiveFrom = effectiveTo;
        }
        Instant earliestAllowed = effectiveTo.minus(Duration.ofDays(MAX_RANGE_DAYS));
        if (effectiveFrom.isBefore(earliestAllowed)) {
            effectiveFrom = earliestAllowed;
        }
        return new Range(effectiveFrom, effectiveTo);
    }

    private record Range(Instant from, Instant to) {
    }
}
