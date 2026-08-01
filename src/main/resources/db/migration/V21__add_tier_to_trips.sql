ALTER TABLE trips ADD COLUMN tier VARCHAR(20) NOT NULL DEFAULT 'ECONOMIC';

ALTER TABLE trips ADD CONSTRAINT chk_trips_tier CHECK (tier IN ('VIP', 'ECONOMIC'));

COMMENT ON COLUMN trips.tier IS 'Service tier the company assigns per trip; existing trips backfilled to ECONOMIC.';
