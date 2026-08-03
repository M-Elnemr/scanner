-- Turns three pieces of free text on a trip into proper reference data:
--   * currency  — was VARCHAR(3), now a row a company picks from
--   * airports  — were two loose codes, now four foreign keys covering the outbound AND return legs
-- and adds the fast-train inclusion flag.

-- ---------------------------------------------------------------------------
-- 1. Reference tables
-- ---------------------------------------------------------------------------

CREATE TABLE countries (
    id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name  VARCHAR(100) NOT NULL,
    iso2  VARCHAR(2)   NOT NULL
);

COMMENT ON TABLE countries IS
    'Fixed reference list. Exists so an airport can declare its country: routing rules are written against this relationship rather than against hardcoded country names, so adding an origin country later is an INSERT.';

CREATE UNIQUE INDEX uq_countries_name ON countries(name);
CREATE UNIQUE INDEX uq_countries_iso2 ON countries(iso2);

INSERT INTO countries (name, iso2) VALUES
    ('Egypt', 'EG'),
    ('Saudi Arabia', 'SA');

CREATE TABLE airports (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_id UUID         NOT NULL REFERENCES countries(id),
    iata_code  VARCHAR(3)   NOT NULL,
    name       VARCHAR(150) NOT NULL,
    city       VARCHAR(100) NOT NULL
);

COMMENT ON TABLE airports IS 'Fixed reference list of airports the platform flies between. Seeded here, never user-editable.';

CREATE UNIQUE INDEX uq_airports_iata_code ON airports(iata_code);
CREATE INDEX idx_airports_country_id      ON airports(country_id);

INSERT INTO airports (country_id, iata_code, name, city) VALUES
    ((SELECT id FROM countries WHERE iso2 = 'EG'), 'CAI', 'Cairo International Airport',                          'Cairo'),
    ((SELECT id FROM countries WHERE iso2 = 'EG'), 'HBE', 'Borg El Arab International Airport',                   'Borg El Arab'),
    ((SELECT id FROM countries WHERE iso2 = 'EG'), 'ATZ', 'Assiut International Airport',                         'Assiut'),
    ((SELECT id FROM countries WHERE iso2 = 'EG'), 'LXR', 'Luxor International Airport',                          'Luxor'),
    ((SELECT id FROM countries WHERE iso2 = 'SA'), 'JED', 'King Abdulaziz International Airport',                 'Jeddah'),
    ((SELECT id FROM countries WHERE iso2 = 'SA'), 'MED', 'Prince Mohammad Bin Abdulaziz International Airport',  'Madinah');

CREATE TABLE currencies (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code    VARCHAR(3)  NOT NULL,
    name    VARCHAR(60) NOT NULL,
    symbol  VARCHAR(8)  NOT NULL
);

COMMENT ON TABLE currencies IS
    'Currencies a trip can be priced in. Deliberately carries no exchange rate — a price is quoted and paid in the trip''s own currency, and nothing converts between them.';

CREATE UNIQUE INDEX uq_currencies_code ON currencies(code);

INSERT INTO currencies (code, name, symbol) VALUES
    ('EGP', 'Egyptian Pound', 'E£'),
    ('SAR', 'Saudi Riyal',    'SR'),
    ('USD', 'US Dollar',      '$');

-- ---------------------------------------------------------------------------
-- 2. Trips: four airports instead of two, currency by reference, fast train
-- ---------------------------------------------------------------------------
-- A round trip has two legs and therefore four airports. The old pair only described the
-- outbound; the return leg was implied. Naming them explicitly lets a company sell, say,
-- Cairo -> Jeddah out and Madinah -> Luxor back.

ALTER TABLE trips
    ADD COLUMN outbound_departure_airport_id UUID REFERENCES airports(id),
    ADD COLUMN outbound_arrival_airport_id   UUID REFERENCES airports(id),
    ADD COLUMN return_departure_airport_id   UUID REFERENCES airports(id),
    ADD COLUMN return_arrival_airport_id     UUID REFERENCES airports(id),
    ADD COLUMN currency_id                   UUID REFERENCES currencies(id),
    ADD COLUMN fast_train_included           BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN trips.outbound_departure_airport_id IS 'Leg 1 origin — the traveller''s home country.';
COMMENT ON COLUMN trips.outbound_arrival_airport_id   IS 'Leg 1 destination — Saudi Arabia.';
COMMENT ON COLUMN trips.return_departure_airport_id   IS 'Leg 2 origin — Saudi Arabia, not necessarily the same airport they landed at.';
COMMENT ON COLUMN trips.return_arrival_airport_id     IS 'Leg 2 destination — back to the home country.';
COMMENT ON COLUMN trips.fast_train_included IS 'Haramain high-speed rail between Makkah and Madinah included in the package price.';

-- Backfill from the old free-text codes. Anything unrecognised (the columns were unvalidated
-- text, so they may hold anything) falls back to the busiest airport on each side so the NOT NULL
-- constraints below can be applied without dropping rows.
UPDATE trips SET
    outbound_departure_airport_id = COALESCE(
        (SELECT a.id FROM airports a WHERE a.iata_code = upper(trips.departure_airport)),
        (SELECT a.id FROM airports a WHERE a.iata_code = 'CAI')),
    outbound_arrival_airport_id = COALESCE(
        (SELECT a.id FROM airports a WHERE a.iata_code = upper(trips.arrival_airport)),
        (SELECT a.id FROM airports a WHERE a.iata_code = 'JED')),
    currency_id = COALESCE(
        (SELECT c.id FROM currencies c WHERE c.code = upper(trips.currency)),
        (SELECT c.id FROM currencies c WHERE c.code = 'EGP'));

-- No return leg was ever recorded, so the only truthful reconstruction is the mirror of the outbound.
UPDATE trips SET
    return_departure_airport_id = outbound_arrival_airport_id,
    return_arrival_airport_id   = outbound_departure_airport_id;

ALTER TABLE trips
    ALTER COLUMN outbound_departure_airport_id SET NOT NULL,
    ALTER COLUMN outbound_arrival_airport_id   SET NOT NULL,
    ALTER COLUMN return_departure_airport_id   SET NOT NULL,
    ALTER COLUMN return_arrival_airport_id     SET NOT NULL,
    ALTER COLUMN currency_id                   SET NOT NULL;

ALTER TABLE trips
    DROP COLUMN departure_airport,
    DROP COLUMN arrival_airport,
    DROP COLUMN currency;

CREATE INDEX idx_trips_outbound_departure_airport ON trips(outbound_departure_airport_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_trips_outbound_arrival_airport   ON trips(outbound_arrival_airport_id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_trips_currency_id                ON trips(currency_id)                   WHERE deleted_at IS NULL;
