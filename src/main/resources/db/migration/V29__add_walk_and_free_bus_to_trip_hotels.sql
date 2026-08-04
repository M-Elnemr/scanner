-- Two more facts about a trip's hotel that travellers actually plan around: is it close enough to
-- walk to the Haram/Mosque, and if not, does the company provide a free shuttle bus.

ALTER TABLE trip_hotels
    ADD COLUMN can_walk           BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN free_bus_included  BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN trip_hotels.can_walk IS 'Whether the distance to the Haram (Makkah) or the Prophet''s Mosque (Madinah) is walkable.';
COMMENT ON COLUMN trip_hotels.free_bus_included IS 'Whether the company provides a free shuttle bus between this hotel and the Haram/Mosque.';
