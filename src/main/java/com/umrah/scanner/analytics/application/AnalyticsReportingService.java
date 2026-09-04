package com.umrah.scanner.analytics.application;

import com.umrah.scanner.analytics.infrastructure.AnalyticsEventRepository;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
 * kept deliberately cheap: every query is bounded to a caller-supplied date range (clamped to
 * {@link #MAX_RANGE_DAYS}) and runs against the table's existing indexes; trip titles for the
 * most-viewed list are resolved with one batched lookup after the group-by, not a join inside the
 * aggregation query itself.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsReportingService {

    private static final int MAX_RANGE_DAYS = 90;
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final String TRIP_VIEW_EVENT_TYPE = "trip_view";
    private static final String TRIP_ENTITY_TYPE = "TRIP";

    private final AnalyticsEventRepository analyticsEventRepository;
    private final TripRepository tripRepository;

    public AnalyticsReportingService(AnalyticsEventRepository analyticsEventRepository, TripRepository tripRepository) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.tripRepository = tripRepository;
    }

    public List<EventTypeCount> eventsByType(LocalDate from, LocalDate to) {
        DateRange range = clamp(from, to);
        return analyticsEventRepository.countByEventTypeBetween(range.from(), range.to());
    }

    public AudienceSplit audience(LocalDate from, LocalDate to) {
        DateRange range = clamp(from, to);
        long guests = analyticsEventRepository.countGuestEventsBetween(range.from(), range.to());
        long identified = analyticsEventRepository.countIdentifiedEventsBetween(range.from(), range.to());
        return new AudienceSplit(guests, identified);
    }

    public List<TripViewCount> mostViewedTrips(LocalDate from, LocalDate to, int limit) {
        DateRange range = clamp(from, to);
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), 50));
        List<MostViewedTrip> rows = analyticsEventRepository.mostViewed(
                TRIP_VIEW_EVENT_TYPE, TRIP_ENTITY_TYPE, range.from(), range.to(), pageable);
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
     * stays cheap no matter what a caller asks for. {@code to} is exclusive-of-the-next-day
     * (converted to the start of the day after) so a same-day range still includes today's events.
     */
    private DateRange clamp(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_RANGE_DAYS);
        if (effectiveFrom.isAfter(effectiveTo)) {
            effectiveFrom = effectiveTo;
        }
        LocalDate earliestAllowed = effectiveTo.minusDays(MAX_RANGE_DAYS);
        if (effectiveFrom.isBefore(earliestAllowed)) {
            effectiveFrom = earliestAllowed;
        }
        return new DateRange(
                effectiveFrom.atStartOfDay(ZoneOffset.UTC).toInstant(),
                effectiveTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private record DateRange(Instant from, Instant to) {
    }
}
