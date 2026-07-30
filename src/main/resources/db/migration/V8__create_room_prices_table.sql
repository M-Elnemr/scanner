CREATE TABLE room_prices (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id     UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    room_type   VARCHAR(20) NOT NULL,
    price       NUMERIC(10,2) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_room_prices_room_type CHECK (room_type IN ('SINGLE', 'DOUBLE', 'TRIPLE', 'QUAD', 'CHILD', 'INFANT')),
    CONSTRAINT chk_room_prices_price     CHECK (price >= 0)
);

COMMENT ON TABLE room_prices IS 'Per-room-type price for a trip. Currency is inherited from trips.currency — never hardcoded.';

CREATE UNIQUE INDEX uq_room_prices_trip_room_type ON room_prices(trip_id, room_type);
