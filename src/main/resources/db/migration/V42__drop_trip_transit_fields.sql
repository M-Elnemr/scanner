-- Flight transit details (stopover count/city/duration) are no longer tracked on a trip — every
-- company package sold through the platform is treated as a single flight leg for display purposes.

ALTER TABLE trips DROP CONSTRAINT chk_trips_transit_count;

ALTER TABLE trips
    DROP COLUMN transit_count,
    DROP COLUMN transit_city,
    DROP COLUMN transit_duration;
