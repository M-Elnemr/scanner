CREATE TABLE trip_hotels (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id               UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    city                  VARCHAR(20) NOT NULL,
    hotel_name            VARCHAR(255) NOT NULL,
    stars                 SMALLINT NOT NULL,
    distance_to_haram_m   INTEGER,
    location_url          VARCHAR(500),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_trip_hotels_city             CHECK (city IN ('MAKKAH', 'MADINAH')),
    CONSTRAINT chk_trip_hotels_stars            CHECK (stars BETWEEN 1 AND 5),
    CONSTRAINT chk_trip_hotels_distance_positive CHECK (distance_to_haram_m IS NULL OR distance_to_haram_m >= 0)
);

COMMENT ON TABLE trip_hotels IS 'Per-city hotel detail for a trip (typically one Makkah row and one Madinah row).';

CREATE UNIQUE INDEX uq_trip_hotels_trip_city ON trip_hotels(trip_id, city);
