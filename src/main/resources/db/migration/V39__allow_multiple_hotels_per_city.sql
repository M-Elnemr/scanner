-- A trip may now offer more than one hotel option per city (informational — the customer sees
-- "this hotel or this hotel", pricing stays flat per trip regardless of which one). The old
-- one-row-per-city cap is replaced with one that just stops the same hotel being picked twice.

DROP INDEX uq_trip_hotels_trip_city;

CREATE UNIQUE INDEX uq_trip_hotels_trip_hotel ON trip_hotels(trip_id, hotel_id);
