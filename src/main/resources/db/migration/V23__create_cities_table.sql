CREATE TABLE cities (
    id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name  VARCHAR(100) NOT NULL
);

COMMENT ON TABLE cities IS 'Fixed reference list of Egypt governorates, used as the city picker for company addresses.';

CREATE UNIQUE INDEX uq_cities_name ON cities(name);

INSERT INTO cities (name) VALUES
    ('Cairo'),
    ('Giza'),
    ('Alexandria'),
    ('Qalyubia'),
    ('Port Said'),
    ('Suez'),
    ('Dakahlia'),
    ('Sharqia'),
    ('Gharbia'),
    ('Monufia'),
    ('Beheira'),
    ('Ismailia'),
    ('Fayoum'),
    ('Beni Suef'),
    ('Minya'),
    ('Asyut'),
    ('Sohag'),
    ('Qena'),
    ('Aswan'),
    ('Luxor'),
    ('Red Sea'),
    ('New Valley'),
    ('Matrouh'),
    ('North Sinai'),
    ('South Sinai'),
    ('Kafr El Sheikh'),
    ('Damietta');
