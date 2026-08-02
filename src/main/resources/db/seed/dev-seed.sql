-- =============================================================================
-- Development seed data: 5 companies, 10 published trips each (50 trips).
--
-- NOT a Flyway migration, and deliberately so — it lives outside db/migration so
-- it can never run against staging or production. Apply it by hand:
--
--   psql -h localhost -U umrah_scanner -d umrah_scanner \
--        -f src/main/resources/db/seed/dev-seed.sql
--
-- Re-runnable: the teardown block below removes only rows this script created,
-- identified by the '@seed.test' email domain and the 'SEED-' trip-code prefix.
-- Any other data in the database is left untouched.
--
-- Logging in as a seeded company (dev profile only): the google_sub values match
-- what DevGoogleLoginUseCase derives from an email, so POST /api/v1/auth/dev-google-test
-- with {"email": "nour-al-haram@seed.test", "role": "COMPANY"} signs you in as
-- that existing company rather than creating a new account.
--
-- Companies are fictional. Cities are the real Egyptian governorates already in the
-- cities table; airlines, airports and Makkah/Madinah hotels are real, so distances
-- and journey details behave sensibly in the UI.
-- =============================================================================

BEGIN;

-- --- Teardown (safe to run on a database with no seed data) --------------------
-- ratings.trip_id and leads.trip_id have no ON DELETE CASCADE, so anything attached
-- to a seed trip goes first. Deleting the seed users then cascades through
-- company_profiles -> trips -> trip_hotels / room_prices / favourites.
DELETE FROM ratings WHERE trip_id IN (SELECT id FROM trips WHERE trip_code LIKE 'SEED-%');
DELETE FROM leads   WHERE trip_id IN (SELECT id FROM trips WHERE trip_code LIKE 'SEED-%');
DELETE FROM users   WHERE email LIKE '%@seed.test';


-- =============================================================================
-- 1. Nour Al-Haram Travel
-- =============================================================================

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('00399ae8-dc8c-5d0c-8905-079fb675aaae', 'nour-al-haram@seed.test', 'dev-test:nour-al-haram@seed.test', 'COMPANY', 'ACTIVE');

INSERT INTO company_profiles (
    id, user_id, company_name, license_number, logo_url, whatsapp, description,
    status, approved_at, rating_avg, rating_count, commission_per_traveler
) VALUES (
    'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd',
    '00399ae8-dc8c-5d0c-8905-079fb675aaae',
    'Nour Al-Haram Travel',
    'TRV-CAI-2019-4471',
    '/uploads/logos/seed-nour-al-haram.png',
    '+201001234501',
    'Cairo-based Umrah operator running since 2009. Five-star Haram-view programs with Egyptian-supervised group leaders, private air-conditioned coaches between Makkah and Madinah, and full-board meals throughout.',
    'APPROVED', now() - INTERVAL '135 days',
    4.60, 128, 2500.00
);

INSERT INTO company_addresses (company_id, city_id, address_text, mobile_number) VALUES
    ('d05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', (SELECT id FROM cities WHERE name = 'Cairo'), '24 Abbas El Akkad St, Nasr City, Cairo', '+201001234501'),
    ('d05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', (SELECT id FROM cities WHERE name = 'Giza'), '9 Tahrir St, Dokki, Giza', '+201001234502');

-- SEED-NOURA-01 | Late Summer Umrah - 10 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NOURA-01', 'Late Summer Umrah - 10 Nights',
    DATE '2026-08-22', DATE '2026-09-01',
    'CAI', 'JED', 'Air Cairo', 'SM797',
    0, NULL, NULL, 5, 5,
    true, true, true, true, true,
    '10 nights total: 5 in Makkah at Jabal Omar Marriott Hotel Makkah (450m from the Haram) and 5 in Madinah at Al Eiman Royal Hotel (200m from the Prophet''s Mosque). Direct flight from CAI to JED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 21, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'MAKKAH', 'Jabal Omar Marriott Hotel Makkah', 5, 450, 'https://maps.google.com/?q=Jabal%20Omar%20Marriott%20Hotel%20Makkah'),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'MADINAH', 'Al Eiman Royal Hotel', 4, 200, 'https://maps.google.com/?q=Al%20Eiman%20Royal%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'SINGLE', 293000.00),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'DOUBLE', 214000.00),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'TRIPLE', 184000.00),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'QUAD', 164500.00),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'CHILD', 118500.00),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'INFANT', 23000.00);

-- SEED-NOURA-02 | September Umrah - 12 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'd069050e-671b-530f-829d-cd5e888f455f', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NOURA-02', 'September Umrah - 12 Nights',
    DATE '2026-09-16', DATE '2026-09-28',
    'CAI', 'JED', 'flynas', 'XY580',
    0, NULL, NULL, 6, 6,
    true, true, true, true, true,
    '12 nights total: 6 in Makkah at Fairmont Makkah Clock Royal Tower (150m from the Haram) and 6 in Madinah at The Oberoi Madina (200m from the Prophet''s Mosque). Direct flight from CAI to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 40, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('d069050e-671b-530f-829d-cd5e888f455f', 'MAKKAH', 'Fairmont Makkah Clock Royal Tower', 5, 150, 'https://maps.google.com/?q=Fairmont%20Makkah%20Clock%20Royal%20Tower'),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'MADINAH', 'The Oberoi Madina', 5, 200, 'https://maps.google.com/?q=The%20Oberoi%20Madina');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('d069050e-671b-530f-829d-cd5e888f455f', 'SINGLE', 185500.00),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'DOUBLE', 135500.00),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'TRIPLE', 117000.00),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'QUAD', 104500.00),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'CHILD', 75000.00),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'INFANT', 14500.00);

-- SEED-NOURA-03 | Autumn Umrah - 14 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NOURA-03', 'Autumn Umrah - 14 Nights',
    DATE '2026-10-07', DATE '2026-10-21',
    'CAI', 'MED', 'Nile Air', 'NP408',
    0, NULL, NULL, 7, 7,
    true, true, true, true, true,
    '14 nights total: 7 in Makkah at Al Kiswah Towers Hotel (1800m from the Haram) and 7 in Madinah at Millennium Al Aqeeq Hotel (400m from the Prophet''s Mosque). Direct flight from CAI to MED on Nile Air. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 27, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'MAKKAH', 'Al Kiswah Towers Hotel', 3, 1800, 'https://maps.google.com/?q=Al%20Kiswah%20Towers%20Hotel'),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'MADINAH', 'Millennium Al Aqeeq Hotel', 4, 400, 'https://maps.google.com/?q=Millennium%20Al%20Aqeeq%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'SINGLE', 193000.00),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'DOUBLE', 141000.00),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'TRIPLE', 121500.00),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'QUAD', 108500.00),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'CHILD', 78000.00),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'INFANT', 15000.00);

-- SEED-NOURA-04 | Mid-Term Break Umrah - 15 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NOURA-04', 'Mid-Term Break Umrah - 15 Nights',
    DATE '2026-10-29', DATE '2026-11-13',
    'CAI', 'JED', 'EgyptAir', 'MS630',
    0, NULL, NULL, 8, 7,
    true, true, true, true, true,
    '15 nights total: 8 in Makkah at Elaf Kinda Hotel (300m from the Haram) and 7 in Madinah at Nozol Royal Inn (1100m from the Prophet''s Mosque). Direct flight from CAI to JED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 47, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'MAKKAH', 'Elaf Kinda Hotel', 4, 300, 'https://maps.google.com/?q=Elaf%20Kinda%20Hotel'),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'MADINAH', 'Nozol Royal Inn', 3, 1100, 'https://maps.google.com/?q=Nozol%20Royal%20Inn');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'SINGLE', 345000.00),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'DOUBLE', 252000.00),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'TRIPLE', 217000.00),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'QUAD', 194000.00),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'CHILD', 139500.00),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'INFANT', 27000.00);

-- SEED-NOURA-05 | November Umrah - 7 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NOURA-05', 'November Umrah - 7 Nights',
    DATE '2026-11-19', DATE '2026-11-26',
    'CAI', 'JED', 'Saudia', 'SV304',
    0, NULL, NULL, 4, 3,
    true, true, true, true, true,
    '7 nights total: 4 in Makkah at Rayyana Ajyad Hotel (700m from the Haram) and 3 in Madinah at Al Ansar Golden Hotel (900m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 36, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'MAKKAH', 'Rayyana Ajyad Hotel', 3, 700, 'https://maps.google.com/?q=Rayyana%20Ajyad%20Hotel'),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'MADINAH', 'Al Ansar Golden Hotel', 3, 900, 'https://maps.google.com/?q=Al%20Ansar%20Golden%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'SINGLE', 173500.00),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'DOUBLE', 127000.00),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'TRIPLE', 109000.00),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'QUAD', 97500.00),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'CHILD', 70000.00),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'INFANT', 13500.00);

-- SEED-NOURA-06 | Rajab Umrah - 8 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '76e82682-157f-50f2-bb32-46cde2778b9c', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NOURA-06', 'Rajab Umrah - 8 Nights',
    DATE '2026-12-16', DATE '2026-12-24',
    'CAI', 'MED', 'Air Cairo', 'SM740',
    0, NULL, NULL, 4, 4,
    true, true, true, true, true,
    '8 nights total: 4 in Makkah at Hilton Makkah Convention Hotel (350m from the Haram) and 4 in Madinah at Odst Al Madinah Hotel (450m from the Prophet''s Mosque). Direct flight from CAI to MED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 53, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'MAKKAH', 'Hilton Makkah Convention Hotel', 5, 350, 'https://maps.google.com/?q=Hilton%20Makkah%20Convention%20Hotel'),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'MADINAH', 'Odst Al Madinah Hotel', 4, 450, 'https://maps.google.com/?q=Odst%20Al%20Madinah%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'SINGLE', 322000.00),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'DOUBLE', 235500.00),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'TRIPLE', 203000.00),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'QUAD', 181000.00),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'CHILD', 130500.00),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'INFANT', 25500.00);

-- SEED-NOURA-07 | Sha'ban Umrah - 10 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NOURA-07', 'Sha''ban Umrah - 10 Nights',
    DATE '2027-01-13', DATE '2027-01-23',
    'CAI', 'JED', 'flynas', 'XY578',
    0, NULL, NULL, 5, 5,
    true, true, true, true, true,
    '10 nights total: 5 in Makkah at Swissotel Al Maqam Makkah (200m from the Haram) and 5 in Madinah at Saja Al Madinah Hotel (600m from the Prophet''s Mosque). Direct flight from CAI to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 33, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'MAKKAH', 'Swissotel Al Maqam Makkah', 5, 200, 'https://maps.google.com/?q=Swissotel%20Al%20Maqam%20Makkah'),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'MADINAH', 'Saja Al Madinah Hotel', 4, 600, 'https://maps.google.com/?q=Saja%20Al%20Madinah%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'SINGLE', 184500.00),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'DOUBLE', 135000.00),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'TRIPLE', 116000.00),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'QUAD', 103500.00),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'CHILD', 74500.00),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'INFANT', 14500.00);

-- SEED-NOURA-08 | Ramadan Umrah - First Ten - 12 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NOURA-08', 'Ramadan Umrah - First Ten - 12 Nights',
    DATE '2027-02-11', DATE '2027-02-23',
    'CAI', 'JED', 'Nile Air', 'NP439',
    0, NULL, NULL, 9, 3,
    true, true, true, true, true,
    '12 nights total: 9 in Makkah at Al Shohada Hotel (350m from the Haram) and 3 in Madinah at Golden Tulip Al Mektan (500m from the Prophet''s Mosque). Direct flight from CAI to JED on Nile Air. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 23, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'MAKKAH', 'Al Shohada Hotel', 4, 350, 'https://maps.google.com/?q=Al%20Shohada%20Hotel'),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'MADINAH', 'Golden Tulip Al Mektan', 4, 500, 'https://maps.google.com/?q=Golden%20Tulip%20Al%20Mektan');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'SINGLE', 281000.00),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'DOUBLE', 205000.00),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'TRIPLE', 177000.00),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'QUAD', 158000.00),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'CHILD', 113500.00),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'INFANT', 22000.00);

-- SEED-NOURA-09 | Ramadan Umrah - Last Ten - 14 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NOURA-09', 'Ramadan Umrah - Last Ten - 14 Nights',
    DATE '2027-02-25', DATE '2027-03-11',
    'CAI', 'MED', 'EgyptAir', 'MS644',
    0, NULL, NULL, 11, 3,
    true, true, true, true, true,
    '14 nights total: 11 in Makkah at Makkah Towers (250m from the Haram) and 3 in Madinah at Shaza Al Madina (300m from the Prophet''s Mosque). Direct flight from CAI to MED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 27, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'MAKKAH', 'Makkah Towers', 4, 250, 'https://maps.google.com/?q=Makkah%20Towers'),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'MADINAH', 'Shaza Al Madina', 5, 300, 'https://maps.google.com/?q=Shaza%20Al%20Madina');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'SINGLE', 518500.00),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'DOUBLE', 379000.00),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'TRIPLE', 326500.00),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'QUAD', 291500.00),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'CHILD', 210000.00),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'INFANT', 41000.00);

-- SEED-NOURA-10 | Shawwal Umrah - 15 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NOURA-10', 'Shawwal Umrah - 15 Nights',
    DATE '2027-04-07', DATE '2027-04-22',
    'CAI', 'JED', 'Saudia', 'SV315',
    0, NULL, NULL, 8, 7,
    true, true, true, true, true,
    '15 nights total: 8 in Makkah at Emaar Grand Hotel (550m from the Haram) and 7 in Madinah at Pullman Zamzam Madina (150m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 58, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'MAKKAH', 'Emaar Grand Hotel', 4, 550, 'https://maps.google.com/?q=Emaar%20Grand%20Hotel'),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'MADINAH', 'Pullman Zamzam Madina', 5, 150, 'https://maps.google.com/?q=Pullman%20Zamzam%20Madina');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'SINGLE', 193000.00),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'DOUBLE', 141000.00),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'TRIPLE', 121500.00),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'QUAD', 108500.00),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'CHILD', 78000.00),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'INFANT', 15000.00);


-- =============================================================================
-- 2. Sakina Umrah Services
-- =============================================================================

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('60249520-68d9-5c25-9a6c-39819b72a607', 'sakina-umrah@seed.test', 'dev-test:sakina-umrah@seed.test', 'COMPANY', 'ACTIVE');

INSERT INTO company_profiles (
    id, user_id, company_name, license_number, logo_url, whatsapp, description,
    status, approved_at, rating_avg, rating_count, commission_per_traveler
) VALUES (
    'd12a303e-1fdf-533c-adf7-dd7537c8f4f3',
    '60249520-68d9-5c25-9a6c-39819b72a607',
    'Sakina Umrah Services',
    'TRV-ALX-2016-2288',
    '/uploads/logos/seed-sakina.png',
    '+201002234501',
    'Alexandria operator serving the Delta governorates. Departures from Borg El Arab with coach transfers from Damanhour and Kafr El Sheikh, walking-distance hotels, and a resident representative in Makkah for the whole season.',
    'APPROVED', now() - INTERVAL '150 days',
    4.30, 86, 1800.00
);

INSERT INTO company_addresses (company_id, city_id, address_text, mobile_number) VALUES
    ('d12a303e-1fdf-533c-adf7-dd7537c8f4f3', (SELECT id FROM cities WHERE name = 'Alexandria'), '31 Fawzy Moaz St, Smouha, Alexandria', '+201002234501'),
    ('d12a303e-1fdf-533c-adf7-dd7537c8f4f3', (SELECT id FROM cities WHERE name = 'Beheira'), '5 El Gomhoreya St, Damanhour', '+201002234502');

-- SEED-SAKINA-01 | Late Summer Umrah - 12 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '112a50d6-7f1d-5c37-9327-966884ef4b1b', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SAKINA-01', 'Late Summer Umrah - 12 Nights',
    DATE '2026-08-24', DATE '2026-09-05',
    'HBE', 'JED', 'flynas', 'XY521',
    0, NULL, NULL, 6, 6,
    true, true, true, true, true,
    '12 nights total: 6 in Makkah at Dar Al Tawhid InterContinental Makkah (100m from the Haram) and 6 in Madinah at Al Ansar Golden Hotel (900m from the Prophet''s Mosque). Direct flight from HBE to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 56, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'MAKKAH', 'Dar Al Tawhid InterContinental Makkah', 5, 100, 'https://maps.google.com/?q=Dar%20Al%20Tawhid%20InterContinental%20Makkah'),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'MADINAH', 'Al Ansar Golden Hotel', 3, 900, 'https://maps.google.com/?q=Al%20Ansar%20Golden%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'SINGLE', 193500.00),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'DOUBLE', 141500.00),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'TRIPLE', 122000.00),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'QUAD', 108500.00),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'CHILD', 78500.00),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'INFANT', 15000.00);

-- SEED-SAKINA-02 | September Umrah - 14 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SAKINA-02', 'September Umrah - 14 Nights',
    DATE '2026-09-18', DATE '2026-10-02',
    'CAI', 'JED', 'Nile Air', 'NP439',
    0, NULL, NULL, 7, 7,
    true, true, false, false, true,
    '14 nights total: 7 in Makkah at Elaf Kinda Hotel (300m from the Haram) and 7 in Madinah at Dar Al Iman InterContinental Madinah (180m from the Prophet''s Mosque). Direct flight from CAI to JED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 42, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'MAKKAH', 'Elaf Kinda Hotel', 4, 300, 'https://maps.google.com/?q=Elaf%20Kinda%20Hotel'),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'MADINAH', 'Dar Al Iman InterContinental Madinah', 5, 180, 'https://maps.google.com/?q=Dar%20Al%20Iman%20InterContinental%20Madinah');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'SINGLE', 126500.00),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'DOUBLE', 92500.00),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'TRIPLE', 79500.00),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'QUAD', 71000.00),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'CHILD', 51000.00),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'INFANT', 10000.00);

-- SEED-SAKINA-03 | Autumn Umrah - 15 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SAKINA-03', 'Autumn Umrah - 15 Nights',
    DATE '2026-10-09', DATE '2026-10-24',
    'HBE', 'MED', 'EgyptAir', 'MS696',
    0, NULL, NULL, 8, 7,
    true, true, true, true, true,
    '15 nights total: 8 in Makkah at Al Shohada Hotel (350m from the Haram) and 7 in Madinah at Millennium Al Aqeeq Hotel (400m from the Prophet''s Mosque). Direct flight from HBE to MED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 35, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'MAKKAH', 'Al Shohada Hotel', 4, 350, 'https://maps.google.com/?q=Al%20Shohada%20Hotel'),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'MADINAH', 'Millennium Al Aqeeq Hotel', 4, 400, 'https://maps.google.com/?q=Millennium%20Al%20Aqeeq%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'SINGLE', 200000.00),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'DOUBLE', 146000.00),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'TRIPLE', 126000.00),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'QUAD', 112500.00),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'CHILD', 81000.00),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'INFANT', 15500.00);

-- SEED-SAKINA-04 | Mid-Term Break Umrah - 7 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '2504afbe-b402-5e29-8f78-0f87c757b67f', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SAKINA-04', 'Mid-Term Break Umrah - 7 Nights',
    DATE '2026-10-31', DATE '2026-11-07',
    'CAI', 'JED', 'Saudia', 'SV337',
    0, NULL, NULL, 4, 3,
    true, true, false, false, true,
    '7 nights total: 4 in Makkah at Anjum Hotel Makkah (900m from the Haram) and 3 in Madinah at Shaza Al Madina (300m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 39, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'MAKKAH', 'Anjum Hotel Makkah', 4, 900, 'https://maps.google.com/?q=Anjum%20Hotel%20Makkah'),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'MADINAH', 'Shaza Al Madina', 5, 300, 'https://maps.google.com/?q=Shaza%20Al%20Madina');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'SINGLE', 127500.00),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'DOUBLE', 93000.00),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'TRIPLE', 80000.00),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'QUAD', 71500.00),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'CHILD', 51500.00),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'INFANT', 10000.00);

-- SEED-SAKINA-05 | November Umrah - 8 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SAKINA-05', 'November Umrah - 8 Nights',
    DATE '2026-11-21', DATE '2026-11-29',
    'HBE', 'JED', 'Air Cairo', 'SM717',
    1, 'Riyadh', '2h 40m', 4, 4,
    true, true, true, true, true,
    '8 nights total: 4 in Makkah at Rayyana Ajyad Hotel (700m from the Haram) and 4 in Madinah at Odst Al Madinah Hotel (450m from the Prophet''s Mosque). One stop in Riyadh from HBE to JED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 20, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'MAKKAH', 'Rayyana Ajyad Hotel', 3, 700, 'https://maps.google.com/?q=Rayyana%20Ajyad%20Hotel'),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'MADINAH', 'Odst Al Madinah Hotel', 4, 450, 'https://maps.google.com/?q=Odst%20Al%20Madinah%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'SINGLE', 306500.00),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'DOUBLE', 224000.00),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'TRIPLE', 193000.00),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'QUAD', 172500.00),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'CHILD', 124000.00),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'INFANT', 24000.00);

-- SEED-SAKINA-06 | Rajab Umrah - 10 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SAKINA-06', 'Rajab Umrah - 10 Nights',
    DATE '2026-12-18', DATE '2026-12-28',
    'CAI', 'MED', 'flynas', 'XY549',
    0, NULL, NULL, 5, 5,
    true, true, true, true, true,
    '10 nights total: 5 in Makkah at Al Kiswah Towers Hotel (1800m from the Haram) and 5 in Madinah at Al Eiman Royal Hotel (200m from the Prophet''s Mosque). Direct flight from CAI to MED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 19, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'MAKKAH', 'Al Kiswah Towers Hotel', 3, 1800, 'https://maps.google.com/?q=Al%20Kiswah%20Towers%20Hotel'),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'MADINAH', 'Al Eiman Royal Hotel', 4, 200, 'https://maps.google.com/?q=Al%20Eiman%20Royal%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'SINGLE', 206000.00),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'DOUBLE', 150500.00),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'TRIPLE', 129500.00),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'QUAD', 116000.00),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'CHILD', 83500.00),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'INFANT', 16000.00);

-- SEED-SAKINA-07 | Sha'ban Umrah - 12 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '128c5fc6-d142-5421-8144-381a8cac30e2', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SAKINA-07', 'Sha''ban Umrah - 12 Nights',
    DATE '2027-01-15', DATE '2027-01-27',
    'HBE', 'JED', 'Nile Air', 'NP404',
    0, NULL, NULL, 6, 6,
    true, true, false, false, true,
    '12 nights total: 6 in Makkah at Le Meridien Makkah (800m from the Haram) and 6 in Madinah at Nozol Royal Inn (1100m from the Prophet''s Mosque). Direct flight from HBE to JED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 20, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'MAKKAH', 'Le Meridien Makkah', 4, 800, 'https://maps.google.com/?q=Le%20Meridien%20Makkah'),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'MADINAH', 'Nozol Royal Inn', 3, 1100, 'https://maps.google.com/?q=Nozol%20Royal%20Inn');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'SINGLE', 126000.00),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'DOUBLE', 92000.00),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'TRIPLE', 79500.00),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'QUAD', 71000.00),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'CHILD', 51000.00),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'INFANT', 10000.00);

-- SEED-SAKINA-08 | Ramadan Umrah - First Ten - 14 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SAKINA-08', 'Ramadan Umrah - First Ten - 14 Nights',
    DATE '2027-02-13', DATE '2027-02-27',
    'CAI', 'JED', 'EgyptAir', 'MS618',
    0, NULL, NULL, 11, 3,
    true, true, true, true, true,
    '14 nights total: 11 in Makkah at Emaar Grand Hotel (550m from the Haram) and 3 in Madinah at Dar Al Taqwa Hotel (120m from the Prophet''s Mosque). Direct flight from CAI to JED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 52, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'MAKKAH', 'Emaar Grand Hotel', 4, 550, 'https://maps.google.com/?q=Emaar%20Grand%20Hotel'),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'MADINAH', 'Dar Al Taqwa Hotel', 5, 120, 'https://maps.google.com/?q=Dar%20Al%20Taqwa%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'SINGLE', 265000.00),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'DOUBLE', 193500.00),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'TRIPLE', 166500.00),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'QUAD', 149000.00),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'CHILD', 107000.00),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'INFANT', 21000.00);

-- SEED-SAKINA-09 | Ramadan Umrah - Last Ten - 15 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '8ecac468-3050-51c5-b627-43f3d78418dc', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SAKINA-09', 'Ramadan Umrah - Last Ten - 15 Nights',
    DATE '2027-02-27', DATE '2027-03-14',
    'HBE', 'MED', 'Saudia', 'SV311',
    0, NULL, NULL, 12, 3,
    true, true, false, false, true,
    '15 nights total: 12 in Makkah at Makkah Towers (250m from the Haram) and 3 in Madinah at Pullman Zamzam Madina (150m from the Prophet''s Mosque). Direct flight from HBE to MED on Saudia. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 43, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'MAKKAH', 'Makkah Towers', 4, 250, 'https://maps.google.com/?q=Makkah%20Towers'),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'MADINAH', 'Pullman Zamzam Madina', 5, 150, 'https://maps.google.com/?q=Pullman%20Zamzam%20Madina');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'SINGLE', 173500.00),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'DOUBLE', 127000.00),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'TRIPLE', 109500.00),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'QUAD', 97500.00),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'CHILD', 70500.00),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'INFANT', 13500.00);

-- SEED-SAKINA-10 | Shawwal Umrah - 7 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '26d1a497-1b59-555b-bb11-aed272894cec', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SAKINA-10', 'Shawwal Umrah - 7 Nights',
    DATE '2027-04-09', DATE '2027-04-16',
    'CAI', 'JED', 'Air Cairo', 'SM727',
    0, NULL, NULL, 4, 3,
    true, true, true, true, true,
    '7 nights total: 4 in Makkah at Jabal Omar Marriott Hotel Makkah (450m from the Haram) and 3 in Madinah at Frontel Al Harithia Hotel (350m from the Prophet''s Mosque). Direct flight from CAI to JED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 45, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'MAKKAH', 'Jabal Omar Marriott Hotel Makkah', 5, 450, 'https://maps.google.com/?q=Jabal%20Omar%20Marriott%20Hotel%20Makkah'),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'MADINAH', 'Frontel Al Harithia Hotel', 4, 350, 'https://maps.google.com/?q=Frontel%20Al%20Harithia%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'SINGLE', 283500.00),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'DOUBLE', 207000.00),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'TRIPLE', 178500.00),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'QUAD', 159500.00),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'CHILD', 115000.00),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'INFANT', 22500.00);


-- =============================================================================
-- 3. Bayt Al-Rahma Tours
-- =============================================================================

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'bayt-al-rahma@seed.test', 'dev-test:bayt-al-rahma@seed.test', 'COMPANY', 'ACTIVE');

INSERT INTO company_profiles (
    id, user_id, company_name, license_number, logo_url, whatsapp, description,
    status, approved_at, rating_avg, rating_count, commission_per_traveler
) VALUES (
    '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc',
    '065dc9bf-f482-5680-83d2-91aaeef1799b',
    'Bayt Al-Rahma Tours',
    'TRV-GIZ-2021-7730',
    '/uploads/logos/seed-bayt-al-rahma.png',
    '+201003234501',
    'Value-focused Umrah programs for families from Giza and the Fayoum corridor. Quad and triple rooms as standard, group transport included, and instalment plans arranged before departure.',
    'APPROVED', now() - INTERVAL '165 days',
    4.10, 54, 1500.00
);

INSERT INTO company_addresses (company_id, city_id, address_text, mobile_number) VALUES
    ('8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', (SELECT id FROM cities WHERE name = 'Giza'), '142 Al Haram St, Giza', '+201003234501'),
    ('8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', (SELECT id FROM cities WHERE name = 'Fayoum'), '18 Gomhoureya St, Fayoum City', '+201003234502'),
    ('8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', (SELECT id FROM cities WHERE name = 'Beni Suef'), '7 Saad Zaghloul St, Beni Suef', '+201003234503');

-- SEED-BAYTA-01 | Late Summer Umrah - 14 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'ad603b65-f5b6-502b-b29b-73a9361f79bc', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAYTA-01', 'Late Summer Umrah - 14 Nights',
    DATE '2026-08-26', DATE '2026-09-09',
    'CAI', 'JED', 'Nile Air', 'NP417',
    0, NULL, NULL, 7, 7,
    true, true, false, false, true,
    '14 nights total: 7 in Makkah at Conrad Makkah (400m from the Haram) and 7 in Madinah at Anwar Al Madinah Movenpick (100m from the Prophet''s Mosque). Direct flight from CAI to JED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 24, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'MAKKAH', 'Conrad Makkah', 5, 400, 'https://maps.google.com/?q=Conrad%20Makkah'),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'MADINAH', 'Anwar Al Madinah Movenpick', 5, 100, 'https://maps.google.com/?q=Anwar%20Al%20Madinah%20Movenpick');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'SINGLE', 132000.00),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'DOUBLE', 96500.00),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'TRIPLE', 83000.00),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'QUAD', 74500.00),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'CHILD', 53500.00),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'INFANT', 10500.00);

-- SEED-BAYTA-02 | September Umrah - 15 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '01791c3d-3d4e-5509-907e-922f91963cca', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAYTA-02', 'September Umrah - 15 Nights',
    DATE '2026-09-20', DATE '2026-10-05',
    'CAI', 'JED', 'EgyptAir', 'MS694',
    0, NULL, NULL, 8, 7,
    true, true, false, false, true,
    '15 nights total: 8 in Makkah at Anjum Hotel Makkah (900m from the Haram) and 7 in Madinah at Elaf Taiba Hotel (250m from the Prophet''s Mosque). Direct flight from CAI to JED on EgyptAir. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 18, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'MAKKAH', 'Anjum Hotel Makkah', 4, 900, 'https://maps.google.com/?q=Anjum%20Hotel%20Makkah'),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'MADINAH', 'Elaf Taiba Hotel', 4, 250, 'https://maps.google.com/?q=Elaf%20Taiba%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'SINGLE', 123500.00),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'DOUBLE', 90500.00),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'TRIPLE', 78000.00),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'QUAD', 69500.00),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'CHILD', 50000.00),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'INFANT', 9500.00);

-- SEED-BAYTA-03 | Autumn Umrah - 7 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'bd83afca-f45f-5f0d-9257-0ec388c52576', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAYTA-03', 'Autumn Umrah - 7 Nights',
    DATE '2026-10-11', DATE '2026-10-18',
    'CAI', 'MED', 'Saudia', 'SV397',
    0, NULL, NULL, 4, 3,
    true, true, true, true, true,
    '7 nights total: 4 in Makkah at Fairmont Makkah Clock Royal Tower (150m from the Haram) and 3 in Madinah at Dar Al Iman InterContinental Madinah (180m from the Prophet''s Mosque). Direct flight from CAI to MED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 25, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'MAKKAH', 'Fairmont Makkah Clock Royal Tower', 5, 150, 'https://maps.google.com/?q=Fairmont%20Makkah%20Clock%20Royal%20Tower'),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'MADINAH', 'Dar Al Iman InterContinental Madinah', 5, 180, 'https://maps.google.com/?q=Dar%20Al%20Iman%20InterContinental%20Madinah');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'SINGLE', 164000.00),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'DOUBLE', 119500.00),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'TRIPLE', 103000.00),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'QUAD', 92000.00),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'CHILD', 66500.00),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'INFANT', 13000.00);

-- SEED-BAYTA-04 | Mid-Term Break Umrah - 8 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '3a5dabc5-85a0-58ce-8465-d6650a3d3d17', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAYTA-04', 'Mid-Term Break Umrah - 8 Nights',
    DATE '2026-11-02', DATE '2026-11-10',
    'CAI', 'JED', 'Air Cairo', 'SM741',
    0, NULL, NULL, 4, 4,
    true, true, false, false, true,
    '8 nights total: 4 in Makkah at Makkah Towers (250m from the Haram) and 4 in Madinah at Al Ansar Golden Hotel (900m from the Prophet''s Mosque). Direct flight from CAI to JED on Air Cairo. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 30, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'MAKKAH', 'Makkah Towers', 4, 250, 'https://maps.google.com/?q=Makkah%20Towers'),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'MADINAH', 'Al Ansar Golden Hotel', 3, 900, 'https://maps.google.com/?q=Al%20Ansar%20Golden%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'SINGLE', 123000.00),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'DOUBLE', 90000.00),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'TRIPLE', 77500.00),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'QUAD', 69000.00),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'CHILD', 50000.00),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'INFANT', 9500.00);

-- SEED-BAYTA-05 | November Umrah - 10 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'bf13fbba-5c9c-5052-8507-3fd9625629d6', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAYTA-05', 'November Umrah - 10 Nights',
    DATE '2026-11-23', DATE '2026-12-03',
    'CAI', 'JED', 'flynas', 'XY563',
    0, NULL, NULL, 5, 5,
    true, true, true, true, true,
    '10 nights total: 5 in Makkah at Pullman ZamZam Makkah (220m from the Haram) and 5 in Madinah at Frontel Al Harithia Hotel (350m from the Prophet''s Mosque). Direct flight from CAI to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 40, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'MAKKAH', 'Pullman ZamZam Makkah', 5, 220, 'https://maps.google.com/?q=Pullman%20ZamZam%20Makkah'),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'MADINAH', 'Frontel Al Harithia Hotel', 4, 350, 'https://maps.google.com/?q=Frontel%20Al%20Harithia%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'SINGLE', 194000.00),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'DOUBLE', 141500.00),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'TRIPLE', 122000.00),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'QUAD', 109000.00),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'CHILD', 78500.00),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'INFANT', 15500.00);

-- SEED-BAYTA-06 | Rajab Umrah - 12 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '7eac1632-4d8a-512d-bd49-160795ecce86', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAYTA-06', 'Rajab Umrah - 12 Nights',
    DATE '2026-12-20', DATE '2027-01-01',
    'CAI', 'MED', 'Nile Air', 'NP428',
    0, NULL, NULL, 6, 6,
    true, true, false, false, true,
    '12 nights total: 6 in Makkah at Elaf Kinda Hotel (300m from the Haram) and 6 in Madinah at Shaza Al Madina (300m from the Prophet''s Mosque). Direct flight from CAI to MED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 38, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'MAKKAH', 'Elaf Kinda Hotel', 4, 300, 'https://maps.google.com/?q=Elaf%20Kinda%20Hotel'),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'MADINAH', 'Shaza Al Madina', 5, 300, 'https://maps.google.com/?q=Shaza%20Al%20Madina');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'SINGLE', 135000.00),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'DOUBLE', 99000.00),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'TRIPLE', 85000.00),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'QUAD', 76000.00),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'CHILD', 54500.00),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'INFANT', 10500.00);

-- SEED-BAYTA-07 | Sha'ban Umrah - 14 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'e3a8dbc0-e720-5133-b79d-459285eb894c', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAYTA-07', 'Sha''ban Umrah - 14 Nights',
    DATE '2027-01-17', DATE '2027-01-31',
    'CAI', 'JED', 'EgyptAir', 'MS631',
    0, NULL, NULL, 7, 7,
    true, true, false, false, true,
    '14 nights total: 7 in Makkah at Emaar Grand Hotel (550m from the Haram) and 7 in Madinah at The Oberoi Madina (200m from the Prophet''s Mosque). Direct flight from CAI to JED on EgyptAir. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 27, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'MAKKAH', 'Emaar Grand Hotel', 4, 550, 'https://maps.google.com/?q=Emaar%20Grand%20Hotel'),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'MADINAH', 'The Oberoi Madina', 5, 200, 'https://maps.google.com/?q=The%20Oberoi%20Madina');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'SINGLE', 125500.00),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'DOUBLE', 91500.00),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'TRIPLE', 79000.00),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'QUAD', 70500.00),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'CHILD', 50500.00),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'INFANT', 10000.00);

-- SEED-BAYTA-08 | Ramadan Umrah - First Ten - 15 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '3cba5827-bafe-543b-9a56-1bfa3a472797', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAYTA-08', 'Ramadan Umrah - First Ten - 15 Nights',
    DATE '2027-02-15', DATE '2027-03-02',
    'CAI', 'JED', 'Saudia', 'SV326',
    0, NULL, NULL, 12, 3,
    true, true, true, true, true,
    '15 nights total: 12 in Makkah at Rayyana Ajyad Hotel (700m from the Haram) and 3 in Madinah at Al Eiman Royal Hotel (200m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 57, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'MAKKAH', 'Rayyana Ajyad Hotel', 3, 700, 'https://maps.google.com/?q=Rayyana%20Ajyad%20Hotel'),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'MADINAH', 'Al Eiman Royal Hotel', 4, 200, 'https://maps.google.com/?q=Al%20Eiman%20Royal%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'SINGLE', 263000.00),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'DOUBLE', 192000.00),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'TRIPLE', 165500.00),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'QUAD', 148000.00),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'CHILD', 106500.00),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'INFANT', 20500.00);

-- SEED-BAYTA-09 | Ramadan Umrah - Last Ten - 7 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '123a23ea-66f2-5207-8c5a-426627dd9462', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAYTA-09', 'Ramadan Umrah - Last Ten - 7 Nights',
    DATE '2027-03-01', DATE '2027-03-08',
    'CAI', 'MED', 'Air Cairo', 'SM726',
    0, NULL, NULL, 4, 3,
    true, true, false, false, true,
    '7 nights total: 4 in Makkah at Dar Al Tawhid InterContinental Makkah (100m from the Haram) and 3 in Madinah at Pullman Zamzam Madina (150m from the Prophet''s Mosque). Direct flight from CAI to MED on Air Cairo. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 50, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'MAKKAH', 'Dar Al Tawhid InterContinental Makkah', 5, 100, 'https://maps.google.com/?q=Dar%20Al%20Tawhid%20InterContinental%20Makkah'),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'MADINAH', 'Pullman Zamzam Madina', 5, 150, 'https://maps.google.com/?q=Pullman%20Zamzam%20Madina');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'SINGLE', 190000.00),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'DOUBLE', 138500.00),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'TRIPLE', 119500.00),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'QUAD', 106500.00),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'CHILD', 77000.00),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'INFANT', 15000.00);

-- SEED-BAYTA-10 | Shawwal Umrah - 8 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'b2b3503d-81ae-5a9e-ab25-f3c069017c99', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAYTA-10', 'Shawwal Umrah - 8 Nights',
    DATE '2027-04-11', DATE '2027-04-19',
    'CAI', 'JED', 'flynas', 'XY509',
    0, NULL, NULL, 4, 4,
    true, true, true, true, true,
    '8 nights total: 4 in Makkah at Al Kiswah Towers Hotel (1800m from the Haram) and 4 in Madinah at Saja Al Madinah Hotel (600m from the Prophet''s Mosque). Direct flight from CAI to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 19, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'MAKKAH', 'Al Kiswah Towers Hotel', 3, 1800, 'https://maps.google.com/?q=Al%20Kiswah%20Towers%20Hotel'),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'MADINAH', 'Saja Al Madinah Hotel', 4, 600, 'https://maps.google.com/?q=Saja%20Al%20Madinah%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'SINGLE', 189000.00),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'DOUBLE', 138000.00),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'TRIPLE', 119000.00),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'QUAD', 106000.00),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'CHILD', 76500.00),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'INFANT', 15000.00);


-- =============================================================================
-- 4. Darb Al-Safa Travel
-- =============================================================================

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('b16dc04d-f9c8-5072-b9bc-ab1470a8cbc6', 'darb-al-safa@seed.test', 'dev-test:darb-al-safa@seed.test', 'COMPANY', 'ACTIVE');

INSERT INTO company_profiles (
    id, user_id, company_name, license_number, logo_url, whatsapp, description,
    status, approved_at, rating_avg, rating_count, commission_per_traveler
) VALUES (
    'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2',
    'b16dc04d-f9c8-5072-b9bc-ab1470a8cbc6',
    'Darb Al-Safa Travel',
    'TRV-DAK-2014-1163',
    '/uploads/logos/seed-darb-al-safa.png',
    '+201004234501',
    'Serving Mansoura, Zagazig and the eastern Delta since 2014. Direct flights from Cairo, hotels inside the central Haram area, and a dedicated women''s group supervisor on every departure.',
    'APPROVED', now() - INTERVAL '180 days',
    4.50, 97, 2000.00
);

INSERT INTO company_addresses (company_id, city_id, address_text, mobile_number) VALUES
    ('df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', (SELECT id FROM cities WHERE name = 'Dakahlia'), '63 Gehan St, Mansoura', '+201004234501'),
    ('df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', (SELECT id FROM cities WHERE name = 'Sharqia'), '22 El Kholafaa El Rashdeen St, Zagazig', '+201004234502');

-- SEED-DARBA-01 | Late Summer Umrah - 15 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'c4229bec-f3c9-56de-8804-2502e6a0c892', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DARBA-01', 'Late Summer Umrah - 15 Nights',
    DATE '2026-08-21', DATE '2026-09-05',
    'CAI', 'JED', 'EgyptAir', 'MS651',
    0, NULL, NULL, 8, 7,
    true, true, true, true, true,
    '15 nights total: 8 in Makkah at Rayyana Ajyad Hotel (700m from the Haram) and 7 in Madinah at Nozol Royal Inn (1100m from the Prophet''s Mosque). Direct flight from CAI to JED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 57, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'MAKKAH', 'Rayyana Ajyad Hotel', 3, 700, 'https://maps.google.com/?q=Rayyana%20Ajyad%20Hotel'),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'MADINAH', 'Nozol Royal Inn', 3, 1100, 'https://maps.google.com/?q=Nozol%20Royal%20Inn');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'SINGLE', 214000.00),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'DOUBLE', 156000.00),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'TRIPLE', 134500.00),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'QUAD', 120000.00),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'CHILD', 86500.00),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'INFANT', 17000.00);

-- SEED-DARBA-02 | September Umrah - 7 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'b055074c-1edf-53a9-8ba0-3a08e24f7554', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DARBA-02', 'September Umrah - 7 Nights',
    DATE '2026-09-15', DATE '2026-09-22',
    'CAI', 'JED', 'Saudia', 'SV319',
    0, NULL, NULL, 4, 3,
    true, true, true, true, true,
    '7 nights total: 4 in Makkah at Fairmont Makkah Clock Royal Tower (150m from the Haram) and 3 in Madinah at Anwar Al Madinah Movenpick (100m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 55, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'MAKKAH', 'Fairmont Makkah Clock Royal Tower', 5, 150, 'https://maps.google.com/?q=Fairmont%20Makkah%20Clock%20Royal%20Tower'),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'MADINAH', 'Anwar Al Madinah Movenpick', 5, 100, 'https://maps.google.com/?q=Anwar%20Al%20Madinah%20Movenpick');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'SINGLE', 258500.00),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'DOUBLE', 189000.00),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'TRIPLE', 163000.00),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'QUAD', 145500.00),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'CHILD', 104500.00),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'INFANT', 20500.00);

-- SEED-DARBA-03 | Autumn Umrah - 8 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DARBA-03', 'Autumn Umrah - 8 Nights',
    DATE '2026-10-06', DATE '2026-10-14',
    'CAI', 'MED', 'Air Cairo', 'SM766',
    0, NULL, NULL, 4, 4,
    true, true, true, true, true,
    '8 nights total: 4 in Makkah at Emaar Grand Hotel (550m from the Haram) and 4 in Madinah at Millennium Al Aqeeq Hotel (400m from the Prophet''s Mosque). Direct flight from CAI to MED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 43, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'MAKKAH', 'Emaar Grand Hotel', 4, 550, 'https://maps.google.com/?q=Emaar%20Grand%20Hotel'),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'MADINAH', 'Millennium Al Aqeeq Hotel', 4, 400, 'https://maps.google.com/?q=Millennium%20Al%20Aqeeq%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'SINGLE', 202500.00),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'DOUBLE', 148000.00),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'TRIPLE', 127500.00),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'QUAD', 114000.00),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'CHILD', 82000.00),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'INFANT', 16000.00);

-- SEED-DARBA-04 | Mid-Term Break Umrah - 10 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '2230c276-5330-5986-a5cb-70a7fe30a719', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DARBA-04', 'Mid-Term Break Umrah - 10 Nights',
    DATE '2026-10-28', DATE '2026-11-07',
    'CAI', 'JED', 'flynas', 'XY595',
    0, NULL, NULL, 5, 5,
    true, true, true, true, true,
    '10 nights total: 5 in Makkah at Hilton Makkah Convention Hotel (350m from the Haram) and 5 in Madinah at Dar Al Taqwa Hotel (120m from the Prophet''s Mosque). Direct flight from CAI to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 23, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'MAKKAH', 'Hilton Makkah Convention Hotel', 5, 350, 'https://maps.google.com/?q=Hilton%20Makkah%20Convention%20Hotel'),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'MADINAH', 'Dar Al Taqwa Hotel', 5, 120, 'https://maps.google.com/?q=Dar%20Al%20Taqwa%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'SINGLE', 177000.00),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'DOUBLE', 129500.00),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'TRIPLE', 111500.00),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'QUAD', 99500.00),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'CHILD', 71500.00),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'INFANT', 14000.00);

-- SEED-DARBA-05 | November Umrah - 12 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '35472b50-9297-57bc-9d76-cb941e961af5', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DARBA-05', 'November Umrah - 12 Nights',
    DATE '2026-11-18', DATE '2026-11-30',
    'CAI', 'JED', 'Nile Air', 'NP429',
    0, NULL, NULL, 6, 6,
    true, true, false, false, true,
    '12 nights total: 6 in Makkah at Al Kiswah Towers Hotel (1800m from the Haram) and 6 in Madinah at Odst Al Madinah Hotel (450m from the Prophet''s Mosque). Direct flight from CAI to JED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 35, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'MAKKAH', 'Al Kiswah Towers Hotel', 3, 1800, 'https://maps.google.com/?q=Al%20Kiswah%20Towers%20Hotel'),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'MADINAH', 'Odst Al Madinah Hotel', 4, 450, 'https://maps.google.com/?q=Odst%20Al%20Madinah%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'SINGLE', 103500.00),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'DOUBLE', 75500.00),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'TRIPLE', 65000.00),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'QUAD', 58000.00),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'CHILD', 42000.00),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'INFANT', 8000.00);

-- SEED-DARBA-06 | Rajab Umrah - 14 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DARBA-06', 'Rajab Umrah - 14 Nights',
    DATE '2026-12-15', DATE '2026-12-29',
    'CAI', 'MED', 'EgyptAir', 'MS662',
    0, NULL, NULL, 7, 7,
    true, true, true, true, true,
    '14 nights total: 7 in Makkah at Dar Al Tawhid InterContinental Makkah (100m from the Haram) and 7 in Madinah at Al Eiman Royal Hotel (200m from the Prophet''s Mosque). Direct flight from CAI to MED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 24, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'MAKKAH', 'Dar Al Tawhid InterContinental Makkah', 5, 100, 'https://maps.google.com/?q=Dar%20Al%20Tawhid%20InterContinental%20Makkah'),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'MADINAH', 'Al Eiman Royal Hotel', 4, 200, 'https://maps.google.com/?q=Al%20Eiman%20Royal%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'SINGLE', 190500.00),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'DOUBLE', 139500.00),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'TRIPLE', 120000.00),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'QUAD', 107000.00),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'CHILD', 77000.00),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'INFANT', 15000.00);

-- SEED-DARBA-07 | Sha'ban Umrah - 15 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DARBA-07', 'Sha''ban Umrah - 15 Nights',
    DATE '2027-01-12', DATE '2027-01-27',
    'CAI', 'JED', 'Saudia', 'SV367',
    0, NULL, NULL, 8, 7,
    true, true, true, true, true,
    '15 nights total: 8 in Makkah at Anjum Hotel Makkah (900m from the Haram) and 7 in Madinah at Al Ansar Golden Hotel (900m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 33, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'MAKKAH', 'Anjum Hotel Makkah', 4, 900, 'https://maps.google.com/?q=Anjum%20Hotel%20Makkah'),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'MADINAH', 'Al Ansar Golden Hotel', 3, 900, 'https://maps.google.com/?q=Al%20Ansar%20Golden%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'SINGLE', 300000.00),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'DOUBLE', 219000.00),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'TRIPLE', 189000.00),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'QUAD', 168500.00),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'CHILD', 121500.00),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'INFANT', 23500.00);

-- SEED-DARBA-08 | Ramadan Umrah - First Ten - 7 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '9f46f443-fa6f-579b-bc98-38a214365201', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DARBA-08', 'Ramadan Umrah - First Ten - 7 Nights',
    DATE '2027-02-10', DATE '2027-02-17',
    'CAI', 'JED', 'Air Cairo', 'SM724',
    0, NULL, NULL, 4, 3,
    true, true, true, true, true,
    '7 nights total: 4 in Makkah at Makkah Towers (250m from the Haram) and 3 in Madinah at Golden Tulip Al Mektan (500m from the Prophet''s Mosque). Direct flight from CAI to JED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 25, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'MAKKAH', 'Makkah Towers', 4, 250, 'https://maps.google.com/?q=Makkah%20Towers'),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'MADINAH', 'Golden Tulip Al Mektan', 4, 500, 'https://maps.google.com/?q=Golden%20Tulip%20Al%20Mektan');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'SINGLE', 289000.00),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'DOUBLE', 211000.00),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'TRIPLE', 181500.00),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'QUAD', 162500.00),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'CHILD', 117000.00),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'INFANT', 22500.00);

-- SEED-DARBA-09 | Ramadan Umrah - Last Ten - 8 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '54d161b5-b060-5d40-96e6-306db3eaa11e', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DARBA-09', 'Ramadan Umrah - Last Ten - 8 Nights',
    DATE '2027-02-24', DATE '2027-03-04',
    'CAI', 'MED', 'flynas', 'XY568',
    0, NULL, NULL, 5, 3,
    true, true, true, true, true,
    '8 nights total: 5 in Makkah at Elaf Kinda Hotel (300m from the Haram) and 3 in Madinah at Elaf Taiba Hotel (250m from the Prophet''s Mosque). Direct flight from CAI to MED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 44, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'MAKKAH', 'Elaf Kinda Hotel', 4, 300, 'https://maps.google.com/?q=Elaf%20Kinda%20Hotel'),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'MADINAH', 'Elaf Taiba Hotel', 4, 250, 'https://maps.google.com/?q=Elaf%20Taiba%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'SINGLE', 304000.00),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'DOUBLE', 222000.00),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'TRIPLE', 191500.00),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'QUAD', 171000.00),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'CHILD', 123000.00),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'INFANT', 24000.00);

-- SEED-DARBA-10 | Shawwal Umrah - 10 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '6989c1ce-5fa7-5591-a2e6-726b733a2474', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DARBA-10', 'Shawwal Umrah - 10 Nights',
    DATE '2027-04-06', DATE '2027-04-16',
    'CAI', 'JED', 'Nile Air', 'NP441',
    0, NULL, NULL, 5, 5,
    true, true, false, false, true,
    '10 nights total: 5 in Makkah at Swissotel Al Maqam Makkah (200m from the Haram) and 5 in Madinah at Frontel Al Harithia Hotel (350m from the Prophet''s Mosque). Direct flight from CAI to JED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 48, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'MAKKAH', 'Swissotel Al Maqam Makkah', 5, 200, 'https://maps.google.com/?q=Swissotel%20Al%20Maqam%20Makkah'),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'MADINAH', 'Frontel Al Harithia Hotel', 4, 350, 'https://maps.google.com/?q=Frontel%20Al%20Harithia%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'SINGLE', 131000.00),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'DOUBLE', 95500.00),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'TRIPLE', 82500.00),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'QUAD', 73500.00),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'CHILD', 53000.00),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'INFANT', 10500.00);


-- =============================================================================
-- 5. Manasik Al-Anwar
-- =============================================================================

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7', 'manasik-al-anwar@seed.test', 'dev-test:manasik-al-anwar@seed.test', 'COMPANY', 'ACTIVE');

INSERT INTO company_profiles (
    id, user_id, company_name, license_number, logo_url, whatsapp, description,
    status, approved_at, rating_avg, rating_count, commission_per_traveler
) VALUES (
    'd73d9c94-0da0-5540-a193-46fe2658392a',
    '2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7',
    'Manasik Al-Anwar',
    'TRV-AST-2018-5502',
    '/uploads/logos/seed-manasik-al-anwar.png',
    '+201005234501',
    'Upper Egypt Umrah operator covering Asyut, Minya and Sohag. Departures from Asyut and Sohag airports where available, economical quad-share programs, and Zamzam allocation included on every package.',
    'APPROVED', now() - INTERVAL '195 days',
    3.90, 41, 1200.00
);

INSERT INTO company_addresses (company_id, city_id, address_text, mobile_number) VALUES
    ('d73d9c94-0da0-5540-a193-46fe2658392a', (SELECT id FROM cities WHERE name = 'Asyut'), '11 El Gomhoreya St, Asyut', '+201005234501'),
    ('d73d9c94-0da0-5540-a193-46fe2658392a', (SELECT id FROM cities WHERE name = 'Minya'), '40 Corniche El Nil, Minya', '+201005234502'),
    ('d73d9c94-0da0-5540-a193-46fe2658392a', (SELECT id FROM cities WHERE name = 'Sohag'), '6 Port Said St, Sohag', '+201005234503');

-- SEED-MANASI-01 | Late Summer Umrah - 7 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '585246c2-f936-5abe-8232-9bd7131ce518', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MANASI-01', 'Late Summer Umrah - 7 Nights',
    DATE '2026-08-23', DATE '2026-08-30',
    'ATZ', 'JED', 'Saudia', 'SV391',
    0, NULL, NULL, 4, 3,
    true, true, false, false, true,
    '7 nights total: 4 in Makkah at Makkah Towers (250m from the Haram) and 3 in Madinah at Saja Al Madinah Hotel (600m from the Prophet''s Mosque). Direct flight from ATZ to JED on Saudia. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 51, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'MAKKAH', 'Makkah Towers', 4, 250, 'https://maps.google.com/?q=Makkah%20Towers'),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'MADINAH', 'Saja Al Madinah Hotel', 4, 600, 'https://maps.google.com/?q=Saja%20Al%20Madinah%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'SINGLE', 120500.00),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'DOUBLE', 88000.00),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'TRIPLE', 76000.00),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'QUAD', 67500.00),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'CHILD', 48500.00),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'INFANT', 9500.00);

-- SEED-MANASI-02 | September Umrah - 8 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '9e2ea781-005f-52a4-9ce3-af372e471fde', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MANASI-02', 'September Umrah - 8 Nights',
    DATE '2026-09-17', DATE '2026-09-25',
    'SOH', 'JED', 'Air Cairo', 'SM778',
    0, NULL, NULL, 4, 4,
    true, true, false, false, true,
    '8 nights total: 4 in Makkah at Dar Al Tawhid InterContinental Makkah (100m from the Haram) and 4 in Madinah at Anwar Al Madinah Movenpick (100m from the Prophet''s Mosque). Direct flight from SOH to JED on Air Cairo. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 50, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'MAKKAH', 'Dar Al Tawhid InterContinental Makkah', 5, 100, 'https://maps.google.com/?q=Dar%20Al%20Tawhid%20InterContinental%20Makkah'),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'MADINAH', 'Anwar Al Madinah Movenpick', 5, 100, 'https://maps.google.com/?q=Anwar%20Al%20Madinah%20Movenpick');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'SINGLE', 103000.00),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'DOUBLE', 75000.00),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'TRIPLE', 64500.00),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'QUAD', 57500.00),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'CHILD', 41500.00),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'INFANT', 8000.00);

-- SEED-MANASI-03 | Autumn Umrah - 10 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '02c6971c-bc4a-52f6-bfa5-e472e0429054', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MANASI-03', 'Autumn Umrah - 10 Nights',
    DATE '2026-10-08', DATE '2026-10-18',
    'CAI', 'MED', 'flynas', 'XY537',
    0, NULL, NULL, 5, 5,
    true, true, false, false, true,
    '10 nights total: 5 in Makkah at Hilton Makkah Convention Hotel (350m from the Haram) and 5 in Madinah at Al Eiman Royal Hotel (200m from the Prophet''s Mosque). Direct flight from CAI to MED on flynas. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 43, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'MAKKAH', 'Hilton Makkah Convention Hotel', 5, 350, 'https://maps.google.com/?q=Hilton%20Makkah%20Convention%20Hotel'),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'MADINAH', 'Al Eiman Royal Hotel', 4, 200, 'https://maps.google.com/?q=Al%20Eiman%20Royal%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'SINGLE', 111500.00),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'DOUBLE', 81500.00),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'TRIPLE', 70000.00),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'QUAD', 62500.00),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'CHILD', 45000.00),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'INFANT', 9000.00);

-- SEED-MANASI-04 | Mid-Term Break Umrah - 12 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'c9d16fd9-bc66-57e6-934d-be23696d3595', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MANASI-04', 'Mid-Term Break Umrah - 12 Nights',
    DATE '2026-10-30', DATE '2026-11-11',
    'ATZ', 'JED', 'Nile Air', 'NP440',
    0, NULL, NULL, 6, 6,
    true, true, true, true, true,
    '12 nights total: 6 in Makkah at Rayyana Ajyad Hotel (700m from the Haram) and 6 in Madinah at Golden Tulip Al Mektan (500m from the Prophet''s Mosque). Direct flight from ATZ to JED on Nile Air. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 41, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'MAKKAH', 'Rayyana Ajyad Hotel', 3, 700, 'https://maps.google.com/?q=Rayyana%20Ajyad%20Hotel'),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'MADINAH', 'Golden Tulip Al Mektan', 4, 500, 'https://maps.google.com/?q=Golden%20Tulip%20Al%20Mektan');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'SINGLE', 227000.00),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'DOUBLE', 165500.00),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'TRIPLE', 142500.00),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'QUAD', 127500.00),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'CHILD', 91500.00),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'INFANT', 18000.00);

-- SEED-MANASI-05 | November Umrah - 14 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '32be2121-93f2-5411-8f94-7aa6f785a2a0', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MANASI-05', 'November Umrah - 14 Nights',
    DATE '2026-11-20', DATE '2026-12-04',
    'SOH', 'JED', 'EgyptAir', 'MS632',
    1, 'Riyadh', '2h 40m', 7, 7,
    true, true, false, false, true,
    '14 nights total: 7 in Makkah at Swissotel Al Maqam Makkah (200m from the Haram) and 7 in Madinah at Shaza Al Madina (300m from the Prophet''s Mosque). One stop in Riyadh from SOH to JED on EgyptAir. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 44, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'MAKKAH', 'Swissotel Al Maqam Makkah', 5, 200, 'https://maps.google.com/?q=Swissotel%20Al%20Maqam%20Makkah'),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'MADINAH', 'Shaza Al Madina', 5, 300, 'https://maps.google.com/?q=Shaza%20Al%20Madina');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'SINGLE', 109000.00),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'DOUBLE', 79500.00),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'TRIPLE', 68500.00),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'QUAD', 61500.00),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'CHILD', 44000.00),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'INFANT', 8500.00);

-- SEED-MANASI-06 | Rajab Umrah - 15 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'bbde726c-6971-5ac4-837b-1504b7aa5733', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MANASI-06', 'Rajab Umrah - 15 Nights',
    DATE '2026-12-17', DATE '2027-01-01',
    'CAI', 'MED', 'Saudia', 'SV312',
    0, NULL, NULL, 8, 7,
    true, true, false, false, true,
    '15 nights total: 8 in Makkah at Fairmont Makkah Clock Royal Tower (150m from the Haram) and 7 in Madinah at Elaf Taiba Hotel (250m from the Prophet''s Mosque). Direct flight from CAI to MED on Saudia. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 56, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'MAKKAH', 'Fairmont Makkah Clock Royal Tower', 5, 150, 'https://maps.google.com/?q=Fairmont%20Makkah%20Clock%20Royal%20Tower'),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'MADINAH', 'Elaf Taiba Hotel', 4, 250, 'https://maps.google.com/?q=Elaf%20Taiba%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'SINGLE', 120000.00),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'DOUBLE', 87500.00),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'TRIPLE', 75500.00),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'QUAD', 67500.00),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'CHILD', 48500.00),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'INFANT', 9500.00);

-- SEED-MANASI-07 | Sha'ban Umrah - 7 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MANASI-07', 'Sha''ban Umrah - 7 Nights',
    DATE '2027-01-14', DATE '2027-01-21',
    'ATZ', 'JED', 'Air Cairo', 'SM730',
    0, NULL, NULL, 4, 3,
    true, true, false, false, true,
    '7 nights total: 4 in Makkah at Al Kiswah Towers Hotel (1800m from the Haram) and 3 in Madinah at Millennium Al Aqeeq Hotel (400m from the Prophet''s Mosque). Direct flight from ATZ to JED on Air Cairo. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 47, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'MAKKAH', 'Al Kiswah Towers Hotel', 3, 1800, 'https://maps.google.com/?q=Al%20Kiswah%20Towers%20Hotel'),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'MADINAH', 'Millennium Al Aqeeq Hotel', 4, 400, 'https://maps.google.com/?q=Millennium%20Al%20Aqeeq%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'SINGLE', 145000.00),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'DOUBLE', 106000.00),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'TRIPLE', 91000.00),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'QUAD', 81500.00),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'CHILD', 58500.00),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'INFANT', 11500.00);

-- SEED-MANASI-08 | Ramadan Umrah - First Ten - 8 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MANASI-08', 'Ramadan Umrah - First Ten - 8 Nights',
    DATE '2027-02-12', DATE '2027-02-20',
    'SOH', 'JED', 'flynas', 'XY532',
    0, NULL, NULL, 5, 3,
    true, true, false, false, true,
    '8 nights total: 5 in Makkah at Jabal Omar Marriott Hotel Makkah (450m from the Haram) and 3 in Madinah at The Oberoi Madina (200m from the Prophet''s Mosque). Direct flight from SOH to JED on flynas. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 54, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'MAKKAH', 'Jabal Omar Marriott Hotel Makkah', 5, 450, 'https://maps.google.com/?q=Jabal%20Omar%20Marriott%20Hotel%20Makkah'),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'MADINAH', 'The Oberoi Madina', 5, 200, 'https://maps.google.com/?q=The%20Oberoi%20Madina');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'SINGLE', 151000.00),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'DOUBLE', 110000.00),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'TRIPLE', 95000.00),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'QUAD', 84500.00),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'CHILD', 61000.00),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'INFANT', 12000.00);

-- SEED-MANASI-09 | Ramadan Umrah - Last Ten - 10 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    '8b4eef5d-e56f-5767-aae5-e0833d91b227', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MANASI-09', 'Ramadan Umrah - Last Ten - 10 Nights',
    DATE '2027-02-26', DATE '2027-03-08',
    'CAI', 'MED', 'Nile Air', 'NP441',
    0, NULL, NULL, 7, 3,
    true, true, true, true, true,
    '10 nights total: 7 in Makkah at Anjum Hotel Makkah (900m from the Haram) and 3 in Madinah at Frontel Al Harithia Hotel (350m from the Prophet''s Mosque). Direct flight from CAI to MED on Nile Air. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 32, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'MAKKAH', 'Anjum Hotel Makkah', 4, 900, 'https://maps.google.com/?q=Anjum%20Hotel%20Makkah'),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'MADINAH', 'Frontel Al Harithia Hotel', 4, 350, 'https://maps.google.com/?q=Frontel%20Al%20Harithia%20Hotel');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'SINGLE', 313000.00),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'DOUBLE', 228500.00),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'TRIPLE', 197000.00),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'QUAD', 176000.00),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'CHILD', 126500.00),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'INFANT', 24500.00);

-- SEED-MANASI-10 | Shawwal Umrah - 12 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    departure_airport, arrival_airport, airline, flight_number,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    description, currency, available_seats, status, tier
) VALUES (
    'fdc42d64-37cd-5fae-867b-a74a81453bb5', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MANASI-10', 'Shawwal Umrah - 12 Nights',
    DATE '2027-04-08', DATE '2027-04-20',
    'ATZ', 'JED', 'EgyptAir', 'MS623',
    1, 'Riyadh', '2h 40m', 6, 6,
    true, true, false, false, true,
    '12 nights total: 6 in Makkah at Emaar Grand Hotel (550m from the Haram) and 6 in Madinah at Dar Al Iman InterContinental Madinah (180m from the Prophet''s Mosque). One stop in Riyadh from ATZ to JED on EgyptAir. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport.',
    'EGP', 50, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, city, hotel_name, stars, distance_to_haram_m, location_url) VALUES
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'MAKKAH', 'Emaar Grand Hotel', 4, 550, 'https://maps.google.com/?q=Emaar%20Grand%20Hotel'),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'MADINAH', 'Dar Al Iman InterContinental Madinah', 5, 180, 'https://maps.google.com/?q=Dar%20Al%20Iman%20InterContinental%20Madinah');

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'SINGLE', 133000.00),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'DOUBLE', 97000.00),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'TRIPLE', 83500.00),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'QUAD', 74500.00),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'CHILD', 54000.00),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'INFANT', 10500.00);


COMMIT;

-- Verify:
--   SELECT c.company_name, c.commission_per_traveler, count(t.id) AS trips
--   FROM company_profiles c JOIN trips t ON t.company_id = c.id
--   WHERE c.license_number LIKE 'TRV-%' GROUP BY 1, 2 ORDER BY 1;
