-- Whether a hotel runs a free shuttle to the Haram/Mosque is a fact about the HOTEL, not about
-- whichever company's trip happens to list it — the same shuttle serves every guest staying there
-- regardless of which package booked them in. Moves free_bus_included to sit next to can_walk on
-- hotels, same reasoning V32/V33 already applied to name/stars/distance/photo.

ALTER TABLE hotels ADD COLUMN free_bus_included BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN hotels.free_bus_included IS
    'Whether this hotel provides a free shuttle bus to the Haram (Makkah) or the Prophet''s Mosque
     (Madinah). A hotel-level fact, unlike trip_hotels.free_bus_included before it: the same shuttle
     serves every trip staying there, not just one company''s package.';

-- Best-effort backfill: a hotel inherits true if any trip already claimed a free bus to it.
UPDATE hotels h
SET free_bus_included = true
WHERE EXISTS (
    SELECT 1 FROM trip_hotels th
    WHERE th.hotel_id = h.id AND th.free_bus_included = true
);

ALTER TABLE trip_hotels DROP COLUMN free_bus_included;
