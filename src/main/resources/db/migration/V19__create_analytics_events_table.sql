CREATE TABLE analytics_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES users(id),
    event_type   VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(100),
    entity_id    UUID,
    metadata     JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE analytics_events IS 'Lightweight behavioral event capture (view, search, compare, contact). user_id nullable for anonymous browsing.';

CREATE INDEX idx_analytics_events_type_created ON analytics_events(event_type, created_at DESC);
CREATE INDEX idx_analytics_events_user_id      ON analytics_events(user_id);
