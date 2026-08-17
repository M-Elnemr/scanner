-- Hotels stop being free text a company retypes on every trip and become reference data the
-- platform owns. A trip now PICKS a hotel per city and keeps only what is genuinely its own:
-- whether that company runs a free shuttle for this package.

-- ---------------------------------------------------------------------------
-- 1. The table
-- ---------------------------------------------------------------------------

CREATE TABLE hotels (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    city                 VARCHAR(20)  NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    name_ar              VARCHAR(255),
    stars                SMALLINT     NOT NULL,
    distance_to_haram_m  INTEGER,
    can_walk             BOOLEAN      NOT NULL DEFAULT false,
    location_url         VARCHAR(500),
    active               BOOLEAN      NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by           UUID REFERENCES users(id),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by           UUID REFERENCES users(id),
    deleted_at           TIMESTAMPTZ,

    CONSTRAINT chk_hotels_city     CHECK (city IN ('MAKKAH', 'MADINAH')),
    CONSTRAINT chk_hotels_stars    CHECK (stars BETWEEN 1 AND 5),
    CONSTRAINT chk_hotels_distance CHECK (distance_to_haram_m IS NULL OR distance_to_haram_m >= 0)
);

COMMENT ON TABLE hotels IS
    'Makkah/Madinah hotels the platform curates. Unlike cities/airports/currencies this list is
     admin-editable, hence the audit and soft-delete columns.';
COMMENT ON COLUMN hotels.can_walk IS
    'Admin judgement, not a computation over distance_to_haram_m: what counts as walkable differs
     for an elderly pilgrim, and the source data this column was backfilled from was never a formula
     either.';
COMMENT ON COLUMN hotels.active IS
    'false retires a hotel from the picker for NEW trips without touching the trips already using it.
     Retirement is what "delete" means for a hotel any trip_hotels row still references; deleted_at
     is reserved for rows nothing references (see DeleteHotelUseCase).';
COMMENT ON COLUMN hotels.name_ar IS
    'Nullable on purpose, unlike the reference tables in V30: there is no known Arabic value to
     backfill from free text, and writing the Latin name here would make "not translated yet"
     indistinguishable from "translated".';

-- Same normalization the backfill dedups on below, so a later "  swissotel  al maqam " is rejected
-- as a duplicate too. lower/btrim/regexp_replace are all IMMUTABLE, so they are legal in an index.
CREATE UNIQUE INDEX uq_hotels_city_name
    ON hotels (city, lower(btrim(regexp_replace(name, '\s+', ' ', 'g'))))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_hotels_city_active ON hotels(city) WHERE deleted_at IS NULL AND active;

-- Lets trip_hotels carry a composite FK (hotel_id, city) below, so a trip_hotels row's city can
-- never disagree with the hotel it points at — Postgres proves it instead of the application trusting it.
ALTER TABLE hotels ADD CONSTRAINT uq_hotels_id_city UNIQUE (id, city);

-- ---------------------------------------------------------------------------
-- 2. Backfill: free text -> catalogue
-- ---------------------------------------------------------------------------
-- Normalization is limited to trim + collapse-internal-whitespace + case-fold. All three are
-- lossless as a MATCHING key and none of them can merge two genuinely different hotels.
--
-- Deliberately NOT done: fuzzy matching. A similarity threshold is a judgement call that could
-- silently merge "Elaf Kinda Hotel" into "Elaf Taiba Hotel"; near-duplicates that survive this
-- migration are the admin's to merge afterwards, visibly, through the hotel-merge endpoint.
--
-- Also NOT done: merging the same name across cities. The same string in MAKKAH and MADINAH becomes
-- two hotel rows, because they are two different buildings and distance_to_haram_m is measured to a
-- different mosque in each.

WITH normalized AS (
    SELECT th.id                                                            AS trip_hotel_id,
           th.city,
           btrim(regexp_replace(th.hotel_name, '\s+', ' ', 'g'))            AS display_name,
           lower(btrim(regexp_replace(th.hotel_name, '\s+', ' ', 'g')))     AS match_key,
           th.stars, th.distance_to_haram_m, th.can_walk, th.location_url, th.created_at
    FROM trip_hotels th
),
-- Distinct full spellings/attribute-sets, and how many trip_hotels rows used each.
variants AS (
    SELECT city, match_key, display_name, stars, distance_to_haram_m, can_walk, location_url,
           count(*)                    AS uses,
           max(created_at)             AS last_used_at,
           min(trip_hotel_id::text)    AS tiebreak
    FROM normalized
    GROUP BY city, match_key, display_name, stars, distance_to_haram_m, can_walk, location_url
),
-- One winning SOURCE ROW per hotel, taken whole rather than voted column-by-column — mixing one
-- company's star rating with another's distance would produce a record no company ever entered.
-- The tiebreak chain ends on a unique id, so this is fully deterministic and re-runnable.
winners AS (
    SELECT DISTINCT ON (city, match_key) *
    FROM variants
    ORDER BY city, match_key,
             uses DESC,                        -- what most companies agreed on
             (distance_to_haram_m IS NULL),     -- prefer a variant that actually recorded a distance
             length(display_name) DESC,        -- prefer the fuller spelling
             last_used_at DESC,
             tiebreak
)
INSERT INTO hotels (city, name, stars, distance_to_haram_m, can_walk, location_url)
SELECT city, display_name, stars, distance_to_haram_m, can_walk, location_url FROM winners;

-- ---------------------------------------------------------------------------
-- 3. Point trip_hotels at the catalogue
-- ---------------------------------------------------------------------------

ALTER TABLE trip_hotels ADD COLUMN hotel_id UUID;

UPDATE trip_hotels th
SET hotel_id = h.id
FROM hotels h
WHERE h.city = th.city
  AND lower(btrim(regexp_replace(h.name,        '\s+', ' ', 'g')))
    = lower(btrim(regexp_replace(th.hotel_name, '\s+', ' ', 'g')));

-- ---------------------------------------------------------------------------
-- 4. Nothing is lost: every losing spelling/value is preserved in the audit trail
-- ---------------------------------------------------------------------------
-- actor_user_id is nullable (V18) precisely for system-written entries like this one.

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_value, new_value)
SELECT NULL,
       'HOTEL_BACKFILLED_FROM_TRIP_HOTELS',
       'Hotel',
       h.id,
       to_jsonb(array_agg(jsonb_build_object(
           'tripHotelId',      th.id,
           'tripId',           th.trip_id,
           'hotelName',        th.hotel_name,
           'stars',            th.stars,
           'distanceToHaramM', th.distance_to_haram_m,
           'canWalk',          th.can_walk,
           'freeBusIncluded',  th.free_bus_included,
           'locationUrl',      th.location_url) ORDER BY th.id)),
       to_jsonb(h)
FROM hotels h
JOIN trip_hotels th ON th.hotel_id = h.id
GROUP BY h.id;

-- ---------------------------------------------------------------------------
-- 5. Constraints. The NOT NULL is the guard: if any row failed to match above, the migration
--    aborts here rather than silently dropping a trip's hotel.
-- ---------------------------------------------------------------------------

ALTER TABLE trip_hotels ALTER COLUMN hotel_id SET NOT NULL;

-- No ON DELETE clause: a hotel in use must be retired (active=false), never physically removed.
ALTER TABLE trip_hotels
    ADD CONSTRAINT fk_trip_hotels_hotel FOREIGN KEY (hotel_id, city) REFERENCES hotels (id, city);

CREATE INDEX idx_trip_hotels_hotel_id ON trip_hotels(hotel_id);

ALTER TABLE trip_hotels
    DROP COLUMN hotel_name,
    DROP COLUMN stars,
    DROP COLUMN distance_to_haram_m,
    DROP COLUMN can_walk,
    DROP COLUMN location_url;

COMMENT ON TABLE trip_hotels IS
    'Which catalogue hotel a trip uses in each city, plus the one genuinely trip-specific fact:
     whether this company runs a free shuttle on this package.';
