-- Supports the admin most-viewed-trips aggregate (group by entity_id, filtered to
-- event_type/entity_type), which the existing (event_type, created_at) and (user_id) indexes
-- don't cover — without this it would degrade to a full scan of the date range as the table grows.
CREATE INDEX idx_analytics_events_entity ON analytics_events(entity_type, entity_id, event_type);
