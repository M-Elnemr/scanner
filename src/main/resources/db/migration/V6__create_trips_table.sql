CREATE TABLE trips (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id                UUID NOT NULL REFERENCES company_profiles(id) ON DELETE CASCADE,
    trip_code                 VARCHAR(50) NOT NULL,
    title                     VARCHAR(255) NOT NULL,
    departure_date            DATE NOT NULL,
    return_date               DATE NOT NULL,
    departure_airport         VARCHAR(10) NOT NULL,
    arrival_airport           VARCHAR(10) NOT NULL,
    airline                   VARCHAR(100) NOT NULL,
    flight_number             VARCHAR(20),
    transit_count             SMALLINT NOT NULL DEFAULT 0,
    transit_city              VARCHAR(100),
    transit_duration          VARCHAR(50),
    days_in_makkah            SMALLINT NOT NULL DEFAULT 0,
    days_in_madinah           SMALLINT NOT NULL DEFAULT 0,
    visa_included             BOOLEAN NOT NULL DEFAULT false,
    transportation_included   BOOLEAN NOT NULL DEFAULT false,
    meals_included            BOOLEAN NOT NULL DEFAULT false,
    guide_included            BOOLEAN NOT NULL DEFAULT false,
    zamzam_included           BOOLEAN NOT NULL DEFAULT false,
    description               TEXT,
    currency                  VARCHAR(3) NOT NULL,
    available_seats           INTEGER NOT NULL DEFAULT 0,
    status                    VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    last_update               TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                   BIGINT NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                UUID REFERENCES users(id),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by                UUID REFERENCES users(id),
    deleted_at                TIMESTAMPTZ,

    CONSTRAINT chk_trips_status         CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'EXPIRED')),
    CONSTRAINT chk_trips_dates          CHECK (return_date > departure_date),
    CONSTRAINT chk_trips_seats          CHECK (available_seats >= 0),
    CONSTRAINT chk_trips_transit_count  CHECK (transit_count >= 0),
    CONSTRAINT chk_trips_days_makkah    CHECK (days_in_makkah >= 0),
    CONSTRAINT chk_trips_days_madinah   CHECK (days_in_madinah >= 0)
);

COMMENT ON TABLE trips IS 'Umrah trip package published by a company. version backs optimistic locking on available_seats.';
COMMENT ON COLUMN trips.version IS 'JPA @Version column — prevents oversell under concurrent lead creation.';

CREATE UNIQUE INDEX uq_trips_trip_code ON trips(trip_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_trips_company_id      ON trips(company_id)          WHERE deleted_at IS NULL;
CREATE INDEX idx_trips_status_departure ON trips(status, departure_date) WHERE deleted_at IS NULL;

-- Covers the public GET /trips browse endpoint: only published, non-deleted trips, sorted by departure.
CREATE INDEX idx_trips_public_browse ON trips(departure_date) WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
