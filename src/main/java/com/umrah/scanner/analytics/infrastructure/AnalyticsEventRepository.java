package com.umrah.scanner.analytics.infrastructure;

import com.umrah.scanner.analytics.application.EventTypeCount;
import com.umrah.scanner.analytics.application.MostViewedTrip;
import com.umrah.scanner.analytics.domain.AnalyticsEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    /** Uses the {@code (event_type, created_at)} index. */
    @Query("select new com.umrah.scanner.analytics.application.EventTypeCount(e.eventType, count(e)) "
            + "from AnalyticsEvent e where e.createdAt >= :from and e.createdAt < :to "
            + "group by e.eventType order by count(e) desc")
    List<EventTypeCount> countByEventTypeBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(e) from AnalyticsEvent e "
            + "where e.createdAt >= :from and e.createdAt < :to and e.userId is null")
    long countGuestEventsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(e) from AnalyticsEvent e "
            + "where e.createdAt >= :from and e.createdAt < :to and e.userId is not null")
    long countIdentifiedEventsBetween(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Uses the {@code (entity_type, entity_id, event_type)} index (migration V45) — without it,
     * this group-by would degrade to a full scan of the date range as the table grows.
     */
    @Query("select new com.umrah.scanner.analytics.application.MostViewedTrip(e.entityId, count(e)) "
            + "from AnalyticsEvent e where e.eventType = :eventType and e.entityType = :entityType "
            + "and e.createdAt >= :from and e.createdAt < :to "
            + "group by e.entityId order by count(e) desc")
    List<MostViewedTrip> mostViewed(
            @Param("eventType") String eventType,
            @Param("entityType") String entityType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
