CREATE TABLE favourites (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID NOT NULL REFERENCES customer_profiles(id) ON DELETE CASCADE,
    trip_id      UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE favourites IS 'Customer-saved trips.';

CREATE UNIQUE INDEX uq_favourites_customer_trip ON favourites(customer_id, trip_id);
CREATE INDEX idx_favourites_trip_id ON favourites(trip_id);
