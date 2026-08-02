ALTER TABLE trips DROP CONSTRAINT chk_trips_tier;
ALTER TABLE trips ADD CONSTRAINT chk_trips_tier CHECK (tier IN ('VIP', 'PREMIUM', 'ECONOMIC'));

ALTER TABLE trips ADD COLUMN duration_days INTEGER
    GENERATED ALWAYS AS (return_date - departure_date) STORED NOT NULL;

COMMENT ON COLUMN trips.duration_days IS 'Trip length in days, derived from return_date - departure_date; backs the browse days-range filter.';

CREATE INDEX idx_trips_duration_days ON trips(duration_days) WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
