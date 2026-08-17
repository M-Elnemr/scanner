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
-- Deliberately more thorough than "delete the seed trips": these accounts are meant to be
-- logged into and tested against (see the dev-google-test note above), so by the time this
-- is re-run they may have real leads, ratings, extra trips and audit-trail rows attached —
-- not just the rows this script itself inserted. Every one of leads.customer_id/company_id/
-- trip_id, and every *_by / *_user_id audit column across the schema, is a plain foreign key
-- with no ON DELETE action, so any of them can block "DELETE FROM users" if left alone.
CREATE TEMP TABLE _seed_user_ids AS SELECT id FROM users WHERE email LIKE '%@seed.test';
CREATE TEMP TABLE _seed_customer_ids AS SELECT id FROM customer_profiles WHERE user_id IN (SELECT id FROM _seed_user_ids);
CREATE TEMP TABLE _seed_company_ids AS SELECT id FROM company_profiles WHERE user_id IN (SELECT id FROM _seed_user_ids);
CREATE TEMP TABLE _seed_trip_ids AS SELECT id FROM trips
    WHERE company_id IN (SELECT id FROM _seed_company_ids) OR trip_code LIKE 'SEED-%';

CREATE TEMP TABLE _seed_lead_ids AS SELECT id FROM leads WHERE
    customer_id IN (SELECT id FROM _seed_customer_ids)
    OR company_id  IN (SELECT id FROM _seed_company_ids)
    OR trip_id     IN (SELECT id FROM _seed_trip_ids);

-- audit_logs, analytics_events and notifications rows this script writes below are entirely
-- owned by it (every one of them is re-created on each run), so they are deleted outright —
-- not nulled — rather than relying on the generic seed-user cleanup further down, which would
-- otherwise leave an ever-growing pile of orphaned rows behind on every re-seed.
-- entity_id has no foreign key (audit_logs is a generic polymorphic log), so a Trip row survives
-- even after the trip itself is later soft-deleted — e.g. a developer creating and deleting a
-- trip via the API against a seed company leaves a TRIP_CREATED row nothing else ever cleans up.
DELETE FROM audit_logs WHERE
    (entity_type = 'Lead' AND entity_id IN (SELECT id FROM _seed_lead_ids))
    OR (entity_type = 'CompanyProfile' AND entity_id IN (SELECT id FROM _seed_company_ids))
    OR (entity_type = 'Trip' AND entity_id IN (SELECT id FROM _seed_trip_ids));
DELETE FROM analytics_events WHERE
    (entity_type = 'Trip' AND entity_id IN (SELECT id FROM _seed_trip_ids))
    OR user_id IN (SELECT id FROM _seed_user_ids);
-- Covers admin-recipient notifications too, which would not otherwise cascade (the admin
-- account is real, not seed data, so deleting seed users never touches its inbox directly).
DELETE FROM notifications WHERE (data->>'leadId')::uuid IN (SELECT id FROM _seed_lead_ids);

-- Cascades to ratings, commissions, cashback_transactions and lead_status_history for free.
DELETE FROM leads WHERE id IN (SELECT id FROM _seed_lead_ids);

-- Audit columns: nulled rather than deleted, since the rows themselves (a real trip, a real
-- company profile) are not seed data and must survive even when their author was.
UPDATE trips            SET created_by = NULL WHERE created_by IN (SELECT id FROM _seed_user_ids);
UPDATE trips            SET updated_by = NULL WHERE updated_by IN (SELECT id FROM _seed_user_ids);
UPDATE company_profiles SET approved_by = NULL WHERE approved_by IN (SELECT id FROM _seed_user_ids);
UPDATE company_profiles SET created_by  = NULL WHERE created_by  IN (SELECT id FROM _seed_user_ids);
UPDATE company_profiles SET updated_by  = NULL WHERE updated_by  IN (SELECT id FROM _seed_user_ids);
UPDATE customer_profiles SET created_by = NULL WHERE created_by IN (SELECT id FROM _seed_user_ids);
UPDATE customer_profiles SET updated_by = NULL WHERE updated_by IN (SELECT id FROM _seed_user_ids);
UPDATE company_documents SET reviewed_by = NULL WHERE reviewed_by IN (SELECT id FROM _seed_user_ids);
UPDATE audit_logs       SET actor_user_id = NULL WHERE actor_user_id IN (SELECT id FROM _seed_user_ids);
UPDATE analytics_events SET user_id       = NULL WHERE user_id       IN (SELECT id FROM _seed_user_ids);

-- Cascades to company_profiles/customer_profiles/device_tokens/notifications/refresh_tokens,
-- and from there to every trip (and its hotels/room_prices/favourites) the seed companies own.
DELETE FROM users WHERE id IN (SELECT id FROM _seed_user_ids);

DROP TABLE _seed_user_ids, _seed_customer_ids, _seed_company_ids, _seed_trip_ids, _seed_lead_ids;


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

-- SEED-NAH-01 | Late Summer Umrah - 10 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NAH-01', 'Late Summer Umrah - 10 Nights',
    DATE '2026-08-22', DATE '2026-09-01',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Air Cairo',
    0, NULL, NULL, 5, 5,
    true, true, true, true, true,
    true, '10 nights total: 5 in Makkah at Jabal Omar Marriott Hotel Makkah (450m from the Haram) and 5 in Madinah at Al Eiman Royal Hotel (200m from the Prophet''s Mosque). Direct flight from CAI to JED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 21, 'PUBLISHED', 'VIP'
);


-- =============================================================================
-- Hotels catalogue — the 30 distinct hotels the trips below pick from.
-- =============================================================================

INSERT INTO hotels (city, name, stars, distance_to_haram_m, can_walk, location_url) VALUES
    ('MAKKAH', 'Jabal Omar Marriott Hotel Makkah', 5, 450, true, 'https://maps.google.com/?q=Jabal%20Omar%20Marriott%20Hotel%20Makkah'),
    ('MADINAH', 'Al Eiman Royal Hotel', 4, 200, true, 'https://maps.google.com/?q=Al%20Eiman%20Royal%20Hotel'),
    ('MAKKAH', 'Fairmont Makkah Clock Royal Tower', 5, 150, true, 'https://maps.google.com/?q=Fairmont%20Makkah%20Clock%20Royal%20Tower'),
    ('MADINAH', 'The Oberoi Madina', 5, 200, true, 'https://maps.google.com/?q=The%20Oberoi%20Madina'),
    ('MAKKAH', 'Al Kiswah Towers Hotel', 3, 1800, false, 'https://maps.google.com/?q=Al%20Kiswah%20Towers%20Hotel'),
    ('MADINAH', 'Millennium Al Aqeeq Hotel', 4, 400, true, 'https://maps.google.com/?q=Millennium%20Al%20Aqeeq%20Hotel'),
    ('MAKKAH', 'Elaf Kinda Hotel', 4, 300, true, 'https://maps.google.com/?q=Elaf%20Kinda%20Hotel'),
    ('MADINAH', 'Nozol Royal Inn', 3, 1100, false, 'https://maps.google.com/?q=Nozol%20Royal%20Inn'),
    ('MAKKAH', 'Rayyana Ajyad Hotel', 3, 700, true, 'https://maps.google.com/?q=Rayyana%20Ajyad%20Hotel'),
    ('MADINAH', 'Al Ansar Golden Hotel', 3, 900, false, 'https://maps.google.com/?q=Al%20Ansar%20Golden%20Hotel'),
    ('MAKKAH', 'Hilton Makkah Convention Hotel', 5, 350, true, 'https://maps.google.com/?q=Hilton%20Makkah%20Convention%20Hotel'),
    ('MADINAH', 'Odst Al Madinah Hotel', 4, 450, true, 'https://maps.google.com/?q=Odst%20Al%20Madinah%20Hotel'),
    ('MAKKAH', 'Swissotel Al Maqam Makkah', 5, 200, true, 'https://maps.google.com/?q=Swissotel%20Al%20Maqam%20Makkah'),
    ('MADINAH', 'Saja Al Madinah Hotel', 4, 600, true, 'https://maps.google.com/?q=Saja%20Al%20Madinah%20Hotel'),
    ('MAKKAH', 'Al Shohada Hotel', 4, 350, true, 'https://maps.google.com/?q=Al%20Shohada%20Hotel'),
    ('MADINAH', 'Golden Tulip Al Mektan', 4, 500, true, 'https://maps.google.com/?q=Golden%20Tulip%20Al%20Mektan'),
    ('MAKKAH', 'Makkah Towers', 4, 250, true, 'https://maps.google.com/?q=Makkah%20Towers'),
    ('MADINAH', 'Shaza Al Madina', 5, 300, true, 'https://maps.google.com/?q=Shaza%20Al%20Madina'),
    ('MAKKAH', 'Emaar Grand Hotel', 4, 550, true, 'https://maps.google.com/?q=Emaar%20Grand%20Hotel'),
    ('MADINAH', 'Pullman Zamzam Madina', 5, 150, true, 'https://maps.google.com/?q=Pullman%20Zamzam%20Madina'),
    ('MADINAH', 'Frontel Al Harithia Hotel', 4, 350, true, 'https://maps.google.com/?q=Frontel%20Al%20Harithia%20Hotel'),
    ('MAKKAH', 'Pullman ZamZam Makkah', 5, 220, true, 'https://maps.google.com/?q=Pullman%20ZamZam%20Makkah'),
    ('MADINAH', 'Dar Al Taqwa Hotel', 5, 120, true, 'https://maps.google.com/?q=Dar%20Al%20Taqwa%20Hotel'),
    ('MAKKAH', 'Dar Al Tawhid InterContinental Makkah', 5, 100, true, 'https://maps.google.com/?q=Dar%20Al%20Tawhid%20InterContinental%20Makkah'),
    ('MADINAH', 'Anwar Al Madinah Movenpick', 5, 100, true, 'https://maps.google.com/?q=Anwar%20Al%20Madinah%20Movenpick'),
    ('MAKKAH', 'Anjum Hotel Makkah', 4, 900, false, 'https://maps.google.com/?q=Anjum%20Hotel%20Makkah'),
    ('MAKKAH', 'Conrad Makkah', 5, 400, true, 'https://maps.google.com/?q=Conrad%20Makkah'),
    ('MAKKAH', 'Le Meridien Makkah', 4, 800, false, 'https://maps.google.com/?q=Le%20Meridien%20Makkah'),
    ('MADINAH', 'Dar Al Iman InterContinental Madinah', 5, 180, true, 'https://maps.google.com/?q=Dar%20Al%20Iman%20InterContinental%20Madinah'),
    ('MADINAH', 'Elaf Taiba Hotel', 4, 250, true, 'https://maps.google.com/?q=Elaf%20Taiba%20Hotel')
-- Unlike cities/airports/currencies (seeded once, only by Flyway), these hotels are inserted by this
-- re-runnable script every time it runs, and they are not scoped to a seed user or trip the teardown
-- above would catch — ON CONFLICT is what keeps a second run from hitting uq_hotels_city_name.
ON CONFLICT (city, (lower(btrim(regexp_replace(name, '\s+', ' ', 'g'))))) WHERE deleted_at IS NULL DO NOTHING;

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '8ce534bd-a036-5a7a-b9d7-7ed5defedcc8'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Jabal Omar Marriott Hotel Makkah'
UNION ALL
SELECT '8ce534bd-a036-5a7a-b9d7-7ed5defedcc8'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Al Eiman Royal Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'SINGLE', 293000.00),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'DOUBLE', 214000.00),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'TRIPLE', 184000.00),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'QUAD', 164500.00),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'CHILD', 118500.00),
    ('8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'INFANT', 23000.00);

-- SEED-NAH-02 | September Umrah - 12 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'd069050e-671b-530f-829d-cd5e888f455f', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NAH-02', 'September Umrah - 12 Nights',
    DATE '2026-09-16', DATE '2026-09-28',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'flynas',
    0, NULL, NULL, 6, 6,
    true, true, true, true, true,
    true, '12 nights total: 6 in Makkah at Fairmont Makkah Clock Royal Tower (150m from the Haram) and 6 in Madinah at The Oberoi Madina (200m from the Prophet''s Mosque). Direct flight from CAI to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 58, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'd069050e-671b-530f-829d-cd5e888f455f'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Fairmont Makkah Clock Royal Tower'
UNION ALL
SELECT 'd069050e-671b-530f-829d-cd5e888f455f'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'The Oberoi Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('d069050e-671b-530f-829d-cd5e888f455f', 'SINGLE', 200000.00),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'DOUBLE', 146000.00),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'TRIPLE', 126000.00),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'QUAD', 112500.00),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'CHILD', 81000.00),
    ('d069050e-671b-530f-829d-cd5e888f455f', 'INFANT', 15500.00);

-- SEED-NAH-03 | Autumn Umrah - 14 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NAH-03', 'Autumn Umrah - 14 Nights',
    DATE '2026-10-07', DATE '2026-10-21',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Nile Air',
    0, NULL, NULL, 7, 7,
    true, true, true, true, true,
    true, '14 nights total: 7 in Makkah at Al Kiswah Towers Hotel (1800m from the Haram) and 7 in Madinah at Millennium Al Aqeeq Hotel (400m from the Prophet''s Mosque). Direct flight from CAI to MED on Nile Air. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 40, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '52892f2b-6f2a-5c44-81a8-0bf302cf046a'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Al Kiswah Towers Hotel'
UNION ALL
SELECT '52892f2b-6f2a-5c44-81a8-0bf302cf046a'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Millennium Al Aqeeq Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'SINGLE', 185500.00),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'DOUBLE', 135500.00),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'TRIPLE', 117000.00),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'QUAD', 104500.00),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'CHILD', 75000.00),
    ('52892f2b-6f2a-5c44-81a8-0bf302cf046a', 'INFANT', 14500.00);

-- SEED-NAH-04 | Mid-Term Break Umrah - 15 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NAH-04', 'Mid-Term Break Umrah - 15 Nights',
    DATE '2026-10-29', DATE '2026-11-13',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'EgyptAir',
    0, NULL, NULL, 8, 7,
    true, true, true, true, true,
    true, '15 nights total: 8 in Makkah at Elaf Kinda Hotel (300m from the Haram) and 7 in Madinah at Nozol Royal Inn (1100m from the Prophet''s Mosque). Direct flight from CAI to JED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 27, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '301ece4e-ac67-5aae-969c-6e8e2cac22e8'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Elaf Kinda Hotel'
UNION ALL
SELECT '301ece4e-ac67-5aae-969c-6e8e2cac22e8'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Nozol Royal Inn';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'SINGLE', 293500.00),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'DOUBLE', 214000.00),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'TRIPLE', 184500.00),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'QUAD', 165000.00),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'CHILD', 118500.00),
    ('301ece4e-ac67-5aae-969c-6e8e2cac22e8', 'INFANT', 23000.00);

-- SEED-NAH-05 | November Umrah - 7 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NAH-05', 'November Umrah - 7 Nights',
    DATE '2026-11-19', DATE '2026-11-26',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Saudia',
    0, NULL, NULL, 4, 3,
    true, true, true, true, true,
    true, '7 nights total: 4 in Makkah at Rayyana Ajyad Hotel (700m from the Haram) and 3 in Madinah at Al Ansar Golden Hotel (900m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 53, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '671b6f1d-d5a0-5bca-94a3-20adcbde2789'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Rayyana Ajyad Hotel'
UNION ALL
SELECT '671b6f1d-d5a0-5bca-94a3-20adcbde2789'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Al Ansar Golden Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'SINGLE', 163500.00),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'DOUBLE', 119500.00),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'TRIPLE', 103000.00),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'QUAD', 92000.00),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'CHILD', 66000.00),
    ('671b6f1d-d5a0-5bca-94a3-20adcbde2789', 'INFANT', 13000.00);

-- SEED-NAH-06 | Rajab Umrah - 8 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '76e82682-157f-50f2-bb32-46cde2778b9c', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NAH-06', 'Rajab Umrah - 8 Nights',
    DATE '2026-12-16', DATE '2026-12-24',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Air Cairo',
    0, NULL, NULL, 4, 4,
    true, true, true, true, true,
    true, '8 nights total: 4 in Makkah at Hilton Makkah Convention Hotel (350m from the Haram) and 4 in Madinah at Odst Al Madinah Hotel (450m from the Prophet''s Mosque). Direct flight from CAI to MED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 20, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '76e82682-157f-50f2-bb32-46cde2778b9c'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Hilton Makkah Convention Hotel'
UNION ALL
SELECT '76e82682-157f-50f2-bb32-46cde2778b9c'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Odst Al Madinah Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'SINGLE', 364500.00),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'DOUBLE', 266000.00),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'TRIPLE', 229500.00),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'QUAD', 205000.00),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'CHILD', 147500.00),
    ('76e82682-157f-50f2-bb32-46cde2778b9c', 'INFANT', 28500.00);

-- SEED-NAH-07 | Sha'ban Umrah - 10 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NAH-07', 'Sha''ban Umrah - 10 Nights',
    DATE '2027-01-13', DATE '2027-01-23',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'flynas',
    0, NULL, NULL, 5, 5,
    true, true, true, true, true,
    true, '10 nights total: 5 in Makkah at Swissotel Al Maqam Makkah (200m from the Haram) and 5 in Madinah at Saja Al Madinah Hotel (600m from the Prophet''s Mosque). Direct flight from CAI to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 36, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '55cf667c-9299-58b8-9c3c-881aa14bc8f8'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Swissotel Al Maqam Makkah'
UNION ALL
SELECT '55cf667c-9299-58b8-9c3c-881aa14bc8f8'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Saja Al Madinah Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'SINGLE', 207500.00),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'DOUBLE', 151500.00),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'TRIPLE', 130500.00),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'QUAD', 116500.00),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'CHILD', 84000.00),
    ('55cf667c-9299-58b8-9c3c-881aa14bc8f8', 'INFANT', 16500.00);

-- SEED-NAH-08 | Ramadan Umrah - First Ten - 12 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NAH-08', 'Ramadan Umrah - First Ten - 12 Nights',
    DATE '2027-02-11', DATE '2027-02-23',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Nile Air',
    0, NULL, NULL, 9, 3,
    true, true, true, true, true,
    true, '12 nights total: 9 in Makkah at Al Shohada Hotel (350m from the Haram) and 3 in Madinah at Golden Tulip Al Mektan (500m from the Prophet''s Mosque). Direct flight from CAI to JED on Nile Air. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 27, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'b2c10b06-9b1d-5b48-8d21-67765ada8a4e'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Al Shohada Hotel'
UNION ALL
SELECT 'b2c10b06-9b1d-5b48-8d21-67765ada8a4e'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Golden Tulip Al Mektan';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'SINGLE', 255000.00),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'DOUBLE', 186000.00),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'TRIPLE', 160500.00),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'QUAD', 143000.00),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'CHILD', 103000.00),
    ('b2c10b06-9b1d-5b48-8d21-67765ada8a4e', 'INFANT', 20000.00);

-- SEED-NAH-09 | Ramadan Umrah - Last Ten - 14 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NAH-09', 'Ramadan Umrah - Last Ten - 14 Nights',
    DATE '2027-02-25', DATE '2027-03-11',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'EgyptAir',
    0, NULL, NULL, 11, 3,
    true, true, true, true, true,
    true, '14 nights total: 11 in Makkah at Makkah Towers (250m from the Haram) and 3 in Madinah at Shaza Al Madina (300m from the Prophet''s Mosque). Direct flight from CAI to MED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 57, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'c1374cc4-a322-5181-8a7a-b3b307fb4eb9'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Makkah Towers'
UNION ALL
SELECT 'c1374cc4-a322-5181-8a7a-b3b307fb4eb9'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Shaza Al Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'SINGLE', 508000.00),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'DOUBLE', 371000.00),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'TRIPLE', 319500.00),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'QUAD', 285500.00),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'CHILD', 205500.00),
    ('c1374cc4-a322-5181-8a7a-b3b307fb4eb9', 'INFANT', 40000.00);

-- SEED-NAH-10 | Shawwal Umrah - 15 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'SEED-NAH-10', 'Shawwal Umrah - 15 Nights',
    DATE '2027-04-07', DATE '2027-04-22',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Saudia',
    0, NULL, NULL, 8, 7,
    true, true, true, true, true,
    true, '15 nights total: 8 in Makkah at Emaar Grand Hotel (550m from the Haram) and 7 in Madinah at Pullman Zamzam Madina (150m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 33, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '831c9dd7-33a5-5bf9-aa68-ac2ceef1370c'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Emaar Grand Hotel'
UNION ALL
SELECT '831c9dd7-33a5-5bf9-aa68-ac2ceef1370c'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Pullman Zamzam Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'SINGLE', 176000.00),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'DOUBLE', 128500.00),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'TRIPLE', 111000.00),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'QUAD', 99000.00),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'CHILD', 71500.00),
    ('831c9dd7-33a5-5bf9-aa68-ac2ceef1370c', 'INFANT', 14000.00);


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

-- SEED-SUS-01 | Late Summer Umrah - 12 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '112a50d6-7f1d-5c37-9327-966884ef4b1b', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SUS-01', 'Late Summer Umrah - 12 Nights',
    DATE '2026-08-24', DATE '2026-09-05',
    (SELECT id FROM airports WHERE iata_code = 'HBE'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'HBE'),
    'flynas',
    0, NULL, NULL, 6, 6,
    true, true, true, true, true,
    true, '12 nights total: 6 in Makkah at Hilton Makkah Convention Hotel (350m from the Haram) and 6 in Madinah at Frontel Al Harithia Hotel (350m from the Prophet''s Mosque). Direct flight from HBE to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to HBE. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 50, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '112a50d6-7f1d-5c37-9327-966884ef4b1b'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Hilton Makkah Convention Hotel'
UNION ALL
SELECT '112a50d6-7f1d-5c37-9327-966884ef4b1b'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Frontel Al Harithia Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'SINGLE', 211500.00),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'DOUBLE', 154500.00),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'TRIPLE', 133000.00),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'QUAD', 119000.00),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'CHILD', 85500.00),
    ('112a50d6-7f1d-5c37-9327-966884ef4b1b', 'INFANT', 16500.00);

-- SEED-SUS-02 | September Umrah - 14 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SUS-02', 'September Umrah - 14 Nights',
    DATE '2026-09-18', DATE '2026-10-02',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Nile Air',
    0, NULL, NULL, 7, 7,
    true, true, false, false, true,
    false, '14 nights total: 7 in Makkah at Makkah Towers (250m from the Haram) and 7 in Madinah at The Oberoi Madina (200m from the Prophet''s Mosque). Direct flight from CAI to JED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 59, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '221d7dea-a52c-5eab-93e5-cb35e31b2cde'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Makkah Towers'
UNION ALL
SELECT '221d7dea-a52c-5eab-93e5-cb35e31b2cde'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'The Oberoi Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'SINGLE', 105500.00),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'DOUBLE', 77000.00),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'TRIPLE', 66500.00),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'QUAD', 59500.00),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'CHILD', 43000.00),
    ('221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'INFANT', 8500.00);

-- SEED-SUS-03 | Autumn Umrah - 15 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SUS-03', 'Autumn Umrah - 15 Nights',
    DATE '2026-10-09', DATE '2026-10-24',
    (SELECT id FROM airports WHERE iata_code = 'HBE'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'HBE'),
    'EgyptAir',
    0, NULL, NULL, 8, 7,
    true, true, true, true, true,
    true, '15 nights total: 8 in Makkah at Pullman ZamZam Makkah (220m from the Haram) and 7 in Madinah at Dar Al Taqwa Hotel (120m from the Prophet''s Mosque). Direct flight from HBE to MED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to HBE. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 22, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '23fbb84c-0e1e-5c5e-beb0-d491fecfc661'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Pullman ZamZam Makkah'
UNION ALL
SELECT '23fbb84c-0e1e-5c5e-beb0-d491fecfc661'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Dar Al Taqwa Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'SINGLE', 160000.00),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'DOUBLE', 117000.00),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'TRIPLE', 100500.00),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'QUAD', 90000.00),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'CHILD', 65000.00),
    ('23fbb84c-0e1e-5c5e-beb0-d491fecfc661', 'INFANT', 12500.00);

-- SEED-SUS-04 | Mid-Term Break Umrah - 7 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '2504afbe-b402-5e29-8f78-0f87c757b67f', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SUS-04', 'Mid-Term Break Umrah - 7 Nights',
    DATE '2026-10-31', DATE '2026-11-07',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Saudia',
    0, NULL, NULL, 4, 3,
    true, true, false, false, true,
    false, '7 nights total: 4 in Makkah at Dar Al Tawhid InterContinental Makkah (100m from the Haram) and 3 in Madinah at Anwar Al Madinah Movenpick (100m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 53, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '2504afbe-b402-5e29-8f78-0f87c757b67f'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Dar Al Tawhid InterContinental Makkah'
UNION ALL
SELECT '2504afbe-b402-5e29-8f78-0f87c757b67f'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Anwar Al Madinah Movenpick';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'SINGLE', 139000.00),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'DOUBLE', 101500.00),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'TRIPLE', 87500.00),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'QUAD', 78000.00),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'CHILD', 56500.00),
    ('2504afbe-b402-5e29-8f78-0f87c757b67f', 'INFANT', 11000.00);

-- SEED-SUS-05 | November Umrah - 8 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SUS-05', 'November Umrah - 8 Nights',
    DATE '2026-11-21', DATE '2026-11-29',
    (SELECT id FROM airports WHERE iata_code = 'HBE'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'HBE'),
    'Air Cairo',
    1, 'Riyadh', '2h 40m', 4, 4,
    true, true, true, true, true,
    true, '8 nights total: 4 in Makkah at Al Shohada Hotel (350m from the Haram) and 4 in Madinah at Saja Al Madinah Hotel (600m from the Prophet''s Mosque). One stop in Riyadh from HBE to JED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to HBE. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 40, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Al Shohada Hotel'
UNION ALL
SELECT 'a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Saja Al Madinah Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'SINGLE', 268000.00),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'DOUBLE', 195500.00),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'TRIPLE', 168500.00),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'QUAD', 150500.00),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'CHILD', 108500.00),
    ('a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe', 'INFANT', 21000.00);

-- SEED-SUS-06 | Rajab Umrah - 10 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SUS-06', 'Rajab Umrah - 10 Nights',
    DATE '2026-12-18', DATE '2026-12-28',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'flynas',
    0, NULL, NULL, 5, 5,
    true, true, true, true, true,
    true, '10 nights total: 5 in Makkah at Anjum Hotel Makkah (900m from the Haram) and 5 in Madinah at Pullman Zamzam Madina (150m from the Prophet''s Mosque). Direct flight from CAI to MED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 50, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '3bb8c748-cf8b-585d-b77c-5447162d5ebc'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Anjum Hotel Makkah'
UNION ALL
SELECT '3bb8c748-cf8b-585d-b77c-5447162d5ebc'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Pullman Zamzam Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'SINGLE', 199500.00),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'DOUBLE', 146000.00),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'TRIPLE', 125500.00),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'QUAD', 112000.00),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'CHILD', 81000.00),
    ('3bb8c748-cf8b-585d-b77c-5447162d5ebc', 'INFANT', 15500.00);

-- SEED-SUS-07 | Sha'ban Umrah - 12 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '128c5fc6-d142-5421-8144-381a8cac30e2', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SUS-07', 'Sha''ban Umrah - 12 Nights',
    DATE '2027-01-15', DATE '2027-01-27',
    (SELECT id FROM airports WHERE iata_code = 'HBE'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'HBE'),
    'Nile Air',
    0, NULL, NULL, 6, 6,
    true, true, false, false, true,
    false, '12 nights total: 6 in Makkah at Conrad Makkah (400m from the Haram) and 6 in Madinah at Shaza Al Madina (300m from the Prophet''s Mosque). Direct flight from HBE to JED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to HBE.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 42, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '128c5fc6-d142-5421-8144-381a8cac30e2'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Conrad Makkah'
UNION ALL
SELECT '128c5fc6-d142-5421-8144-381a8cac30e2'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Shaza Al Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'SINGLE', 146500.00),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'DOUBLE', 107000.00),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'TRIPLE', 92000.00),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'QUAD', 82000.00),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'CHILD', 59000.00),
    ('128c5fc6-d142-5421-8144-381a8cac30e2', 'INFANT', 11500.00);

-- SEED-SUS-08 | Ramadan Umrah - First Ten - 14 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SUS-08', 'Ramadan Umrah - First Ten - 14 Nights',
    DATE '2027-02-13', DATE '2027-02-27',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'EgyptAir',
    0, NULL, NULL, 11, 3,
    true, true, true, true, true,
    true, '14 nights total: 11 in Makkah at Le Meridien Makkah (800m from the Haram) and 3 in Madinah at Dar Al Iman InterContinental Madinah (180m from the Prophet''s Mosque). Direct flight from CAI to JED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 35, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Le Meridien Makkah'
UNION ALL
SELECT '30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Dar Al Iman InterContinental Madinah';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'SINGLE', 301500.00),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'DOUBLE', 220500.00),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'TRIPLE', 190000.00),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'QUAD', 169500.00),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'CHILD', 122000.00),
    ('30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87', 'INFANT', 23500.00);

-- SEED-SUS-09 | Ramadan Umrah - Last Ten - 15 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '8ecac468-3050-51c5-b627-43f3d78418dc', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SUS-09', 'Ramadan Umrah - Last Ten - 15 Nights',
    DATE '2027-02-27', DATE '2027-03-14',
    (SELECT id FROM airports WHERE iata_code = 'HBE'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'HBE'),
    'Saudia',
    0, NULL, NULL, 12, 3,
    true, true, false, false, true,
    true, '15 nights total: 12 in Makkah at Swissotel Al Maqam Makkah (200m from the Haram) and 3 in Madinah at Golden Tulip Al Mektan (500m from the Prophet''s Mosque). Direct flight from HBE to MED on Saudia. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to HBE. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 37, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '8ecac468-3050-51c5-b627-43f3d78418dc'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Swissotel Al Maqam Makkah'
UNION ALL
SELECT '8ecac468-3050-51c5-b627-43f3d78418dc'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Golden Tulip Al Mektan';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'SINGLE', 186500.00),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'DOUBLE', 136000.00),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'TRIPLE', 117500.00),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'QUAD', 104500.00),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'CHILD', 75500.00),
    ('8ecac468-3050-51c5-b627-43f3d78418dc', 'INFANT', 14500.00);

-- SEED-SUS-10 | Shawwal Umrah - 7 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '26d1a497-1b59-555b-bb11-aed272894cec', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'SEED-SUS-10', 'Shawwal Umrah - 7 Nights',
    DATE '2027-04-09', DATE '2027-04-16',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Air Cairo',
    0, NULL, NULL, 4, 3,
    true, true, true, true, true,
    true, '7 nights total: 4 in Makkah at Al Kiswah Towers Hotel (1800m from the Haram) and 3 in Madinah at Al Ansar Golden Hotel (900m from the Prophet''s Mosque). Direct flight from CAI to JED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 26, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '26d1a497-1b59-555b-bb11-aed272894cec'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Al Kiswah Towers Hotel'
UNION ALL
SELECT '26d1a497-1b59-555b-bb11-aed272894cec'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Al Ansar Golden Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'SINGLE', 317500.00),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'DOUBLE', 231500.00),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'TRIPLE', 199500.00),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'QUAD', 178000.00),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'CHILD', 128500.00),
    ('26d1a497-1b59-555b-bb11-aed272894cec', 'INFANT', 25000.00);


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

-- SEED-BAR-01 | Late Summer Umrah - 14 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'ad603b65-f5b6-502b-b29b-73a9361f79bc', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAR-01', 'Late Summer Umrah - 14 Nights',
    DATE '2026-08-26', DATE '2026-09-09',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Nile Air',
    0, NULL, NULL, 7, 7,
    true, true, false, false, true,
    false, '14 nights total: 7 in Makkah at Swissotel Al Maqam Makkah (200m from the Haram) and 7 in Madinah at The Oberoi Madina (200m from the Prophet''s Mosque). Direct flight from CAI to JED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 39, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'ad603b65-f5b6-502b-b29b-73a9361f79bc'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Swissotel Al Maqam Makkah'
UNION ALL
SELECT 'ad603b65-f5b6-502b-b29b-73a9361f79bc'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'The Oberoi Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'SINGLE', 129500.00),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'DOUBLE', 94500.00),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'TRIPLE', 81500.00),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'QUAD', 72500.00),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'CHILD', 52500.00),
    ('ad603b65-f5b6-502b-b29b-73a9361f79bc', 'INFANT', 10000.00);

-- SEED-BAR-02 | September Umrah - 15 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '01791c3d-3d4e-5509-907e-922f91963cca', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAR-02', 'September Umrah - 15 Nights',
    DATE '2026-09-20', DATE '2026-10-05',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'EgyptAir',
    0, NULL, NULL, 8, 7,
    true, true, false, false, true,
    false, '15 nights total: 8 in Makkah at Elaf Kinda Hotel (300m from the Haram) and 7 in Madinah at Dar Al Taqwa Hotel (120m from the Prophet''s Mosque). Direct flight from CAI to JED on EgyptAir. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 59, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '01791c3d-3d4e-5509-907e-922f91963cca'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Elaf Kinda Hotel'
UNION ALL
SELECT '01791c3d-3d4e-5509-907e-922f91963cca'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Dar Al Taqwa Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'SINGLE', 121000.00),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'DOUBLE', 88500.00),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'TRIPLE', 76500.00),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'QUAD', 68000.00),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'CHILD', 49000.00),
    ('01791c3d-3d4e-5509-907e-922f91963cca', 'INFANT', 9500.00);

-- SEED-BAR-03 | Autumn Umrah - 7 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'bd83afca-f45f-5f0d-9257-0ec388c52576', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAR-03', 'Autumn Umrah - 7 Nights',
    DATE '2026-10-11', DATE '2026-10-18',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Saudia',
    0, NULL, NULL, 4, 3,
    true, true, true, true, true,
    true, '7 nights total: 4 in Makkah at Rayyana Ajyad Hotel (700m from the Haram) and 3 in Madinah at Odst Al Madinah Hotel (450m from the Prophet''s Mosque). Direct flight from CAI to MED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 23, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'bd83afca-f45f-5f0d-9257-0ec388c52576'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Rayyana Ajyad Hotel'
UNION ALL
SELECT 'bd83afca-f45f-5f0d-9257-0ec388c52576'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Odst Al Madinah Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'SINGLE', 193000.00),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'DOUBLE', 141000.00),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'TRIPLE', 121500.00),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'QUAD', 108500.00),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'CHILD', 78000.00),
    ('bd83afca-f45f-5f0d-9257-0ec388c52576', 'INFANT', 15000.00);

-- SEED-BAR-04 | Mid-Term Break Umrah - 8 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '3a5dabc5-85a0-58ce-8465-d6650a3d3d17', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAR-04', 'Mid-Term Break Umrah - 8 Nights',
    DATE '2026-11-02', DATE '2026-11-10',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Air Cairo',
    0, NULL, NULL, 4, 4,
    true, true, false, false, true,
    false, '8 nights total: 4 in Makkah at Al Shohada Hotel (350m from the Haram) and 4 in Madinah at Golden Tulip Al Mektan (500m from the Prophet''s Mosque). Direct flight from CAI to JED on Air Cairo. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 59, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '3a5dabc5-85a0-58ce-8465-d6650a3d3d17'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Al Shohada Hotel'
UNION ALL
SELECT '3a5dabc5-85a0-58ce-8465-d6650a3d3d17'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Golden Tulip Al Mektan';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'SINGLE', 113000.00),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'DOUBLE', 82500.00),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'TRIPLE', 71000.00),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'QUAD', 63500.00),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'CHILD', 45500.00),
    ('3a5dabc5-85a0-58ce-8465-d6650a3d3d17', 'INFANT', 9000.00);

-- SEED-BAR-05 | November Umrah - 10 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'bf13fbba-5c9c-5052-8507-3fd9625629d6', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAR-05', 'November Umrah - 10 Nights',
    DATE '2026-11-23', DATE '2026-12-03',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'flynas',
    0, NULL, NULL, 5, 5,
    true, true, true, true, true,
    true, '10 nights total: 5 in Makkah at Conrad Makkah (400m from the Haram) and 5 in Madinah at Al Ansar Golden Hotel (900m from the Prophet''s Mosque). Direct flight from CAI to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 56, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'bf13fbba-5c9c-5052-8507-3fd9625629d6'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Conrad Makkah'
UNION ALL
SELECT 'bf13fbba-5c9c-5052-8507-3fd9625629d6'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Al Ansar Golden Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'SINGLE', 190000.00),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'DOUBLE', 139000.00),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'TRIPLE', 119500.00),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'QUAD', 107000.00),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'CHILD', 77000.00),
    ('bf13fbba-5c9c-5052-8507-3fd9625629d6', 'INFANT', 15000.00);

-- SEED-BAR-06 | Rajab Umrah - 12 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '7eac1632-4d8a-512d-bd49-160795ecce86', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAR-06', 'Rajab Umrah - 12 Nights',
    DATE '2026-12-20', DATE '2027-01-01',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Nile Air',
    0, NULL, NULL, 6, 6,
    true, true, false, false, true,
    true, '12 nights total: 6 in Makkah at Al Kiswah Towers Hotel (1800m from the Haram) and 6 in Madinah at Al Eiman Royal Hotel (200m from the Prophet''s Mosque). Direct flight from CAI to MED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 36, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '7eac1632-4d8a-512d-bd49-160795ecce86'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Al Kiswah Towers Hotel'
UNION ALL
SELECT '7eac1632-4d8a-512d-bd49-160795ecce86'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Al Eiman Royal Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'SINGLE', 130000.00),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'DOUBLE', 95000.00),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'TRIPLE', 82000.00),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'QUAD', 73000.00),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'CHILD', 52500.00),
    ('7eac1632-4d8a-512d-bd49-160795ecce86', 'INFANT', 10000.00);

-- SEED-BAR-07 | Sha'ban Umrah - 14 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'e3a8dbc0-e720-5133-b79d-459285eb894c', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAR-07', 'Sha''ban Umrah - 14 Nights',
    DATE '2027-01-17', DATE '2027-01-31',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'EgyptAir',
    0, NULL, NULL, 7, 7,
    true, true, false, false, true,
    false, '14 nights total: 7 in Makkah at Le Meridien Makkah (800m from the Haram) and 7 in Madinah at Nozol Royal Inn (1100m from the Prophet''s Mosque). Direct flight from CAI to JED on EgyptAir. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 40, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'e3a8dbc0-e720-5133-b79d-459285eb894c'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Le Meridien Makkah'
UNION ALL
SELECT 'e3a8dbc0-e720-5133-b79d-459285eb894c'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Nozol Royal Inn';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'SINGLE', 148000.00),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'DOUBLE', 108000.00),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'TRIPLE', 93000.00),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'QUAD', 83000.00),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'CHILD', 60000.00),
    ('e3a8dbc0-e720-5133-b79d-459285eb894c', 'INFANT', 11500.00);

-- SEED-BAR-08 | Ramadan Umrah - First Ten - 15 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '3cba5827-bafe-543b-9a56-1bfa3a472797', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAR-08', 'Ramadan Umrah - First Ten - 15 Nights',
    DATE '2027-02-15', DATE '2027-03-02',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Saudia',
    0, NULL, NULL, 12, 3,
    true, true, true, true, true,
    true, '15 nights total: 12 in Makkah at Emaar Grand Hotel (550m from the Haram) and 3 in Madinah at Frontel Al Harithia Hotel (350m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 39, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '3cba5827-bafe-543b-9a56-1bfa3a472797'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Emaar Grand Hotel'
UNION ALL
SELECT '3cba5827-bafe-543b-9a56-1bfa3a472797'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Frontel Al Harithia Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'SINGLE', 247000.00),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'DOUBLE', 180500.00),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'TRIPLE', 155500.00),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'QUAD', 138500.00),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'CHILD', 100000.00),
    ('3cba5827-bafe-543b-9a56-1bfa3a472797', 'INFANT', 19500.00);

-- SEED-BAR-09 | Ramadan Umrah - Last Ten - 7 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '123a23ea-66f2-5207-8c5a-426627dd9462', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAR-09', 'Ramadan Umrah - Last Ten - 7 Nights',
    DATE '2027-03-01', DATE '2027-03-08',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Air Cairo',
    0, NULL, NULL, 4, 3,
    true, true, false, false, true,
    true, '7 nights total: 4 in Makkah at Pullman ZamZam Makkah (220m from the Haram) and 3 in Madinah at Shaza Al Madina (300m from the Prophet''s Mosque). Direct flight from CAI to MED on Air Cairo. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 35, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '123a23ea-66f2-5207-8c5a-426627dd9462'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Pullman ZamZam Makkah'
UNION ALL
SELECT '123a23ea-66f2-5207-8c5a-426627dd9462'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Shaza Al Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'SINGLE', 189500.00),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'DOUBLE', 138000.00),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'TRIPLE', 119000.00),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'QUAD', 106500.00),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'CHILD', 76500.00),
    ('123a23ea-66f2-5207-8c5a-426627dd9462', 'INFANT', 15000.00);

-- SEED-BAR-10 | Shawwal Umrah - 8 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'b2b3503d-81ae-5a9e-ab25-f3c069017c99', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'SEED-BAR-10', 'Shawwal Umrah - 8 Nights',
    DATE '2027-04-11', DATE '2027-04-19',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'flynas',
    0, NULL, NULL, 4, 4,
    true, true, true, true, true,
    true, '8 nights total: 4 in Makkah at Fairmont Makkah Clock Royal Tower (150m from the Haram) and 4 in Madinah at Pullman Zamzam Madina (150m from the Prophet''s Mosque). Direct flight from CAI to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'EGP'), 24, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'b2b3503d-81ae-5a9e-ab25-f3c069017c99'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Fairmont Makkah Clock Royal Tower'
UNION ALL
SELECT 'b2b3503d-81ae-5a9e-ab25-f3c069017c99'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Pullman Zamzam Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'SINGLE', 202500.00),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'DOUBLE', 148000.00),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'TRIPLE', 127500.00),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'QUAD', 113500.00),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'CHILD', 82000.00),
    ('b2b3503d-81ae-5a9e-ab25-f3c069017c99', 'INFANT', 16000.00);


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

-- SEED-DST-01 | Late Summer Umrah - 15 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'c4229bec-f3c9-56de-8804-2502e6a0c892', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DST-01', 'Late Summer Umrah - 15 Nights',
    DATE '2026-08-21', DATE '2026-09-05',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'EgyptAir',
    0, NULL, NULL, 8, 7,
    true, true, true, true, true,
    true, '15 nights total: 8 in Makkah at Makkah Towers (250m from the Haram) and 7 in Madinah at Millennium Al Aqeeq Hotel (400m from the Prophet''s Mosque). Direct flight from CAI to JED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'USD'), 39, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'c4229bec-f3c9-56de-8804-2502e6a0c892'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Makkah Towers'
UNION ALL
SELECT 'c4229bec-f3c9-56de-8804-2502e6a0c892'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Millennium Al Aqeeq Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'SINGLE', 172000.00),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'DOUBLE', 126000.00),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'TRIPLE', 108500.00),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'QUAD', 96500.00),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'CHILD', 69500.00),
    ('c4229bec-f3c9-56de-8804-2502e6a0c892', 'INFANT', 13500.00);

-- SEED-DST-02 | September Umrah - 7 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'b055074c-1edf-53a9-8ba0-3a08e24f7554', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DST-02', 'September Umrah - 7 Nights',
    DATE '2026-09-15', DATE '2026-09-22',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Saudia',
    0, NULL, NULL, 4, 3,
    true, true, true, true, true,
    true, '7 nights total: 4 in Makkah at Le Meridien Makkah (800m from the Haram) and 3 in Madinah at Frontel Al Harithia Hotel (350m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'USD'), 40, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'b055074c-1edf-53a9-8ba0-3a08e24f7554'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Le Meridien Makkah'
UNION ALL
SELECT 'b055074c-1edf-53a9-8ba0-3a08e24f7554'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Frontel Al Harithia Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'SINGLE', 278500.00),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'DOUBLE', 203500.00),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'TRIPLE', 175500.00),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'QUAD', 156500.00),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'CHILD', 112500.00),
    ('b055074c-1edf-53a9-8ba0-3a08e24f7554', 'INFANT', 22000.00);

-- SEED-DST-03 | Autumn Umrah - 8 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DST-03', 'Autumn Umrah - 8 Nights',
    DATE '2026-10-06', DATE '2026-10-14',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Air Cairo',
    0, NULL, NULL, 4, 4,
    true, true, true, true, true,
    true, '8 nights total: 4 in Makkah at Anjum Hotel Makkah (900m from the Haram) and 4 in Madinah at Dar Al Taqwa Hotel (120m from the Prophet''s Mosque). Direct flight from CAI to MED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'USD'), 58, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '27dd5ad4-3cc5-5a72-9688-de3e9b788c56'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Anjum Hotel Makkah'
UNION ALL
SELECT '27dd5ad4-3cc5-5a72-9688-de3e9b788c56'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Dar Al Taqwa Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'SINGLE', 157000.00),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'DOUBLE', 114500.00),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'TRIPLE', 99000.00),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'QUAD', 88000.00),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'CHILD', 63500.00),
    ('27dd5ad4-3cc5-5a72-9688-de3e9b788c56', 'INFANT', 12500.00);

-- SEED-DST-04 | Mid-Term Break Umrah - 10 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '2230c276-5330-5986-a5cb-70a7fe30a719', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DST-04', 'Mid-Term Break Umrah - 10 Nights',
    DATE '2026-10-28', DATE '2026-11-07',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'flynas',
    0, NULL, NULL, 5, 5,
    true, true, true, true, true,
    true, '10 nights total: 5 in Makkah at Jabal Omar Marriott Hotel Makkah (450m from the Haram) and 5 in Madinah at Shaza Al Madina (300m from the Prophet''s Mosque). Direct flight from CAI to JED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'USD'), 38, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '2230c276-5330-5986-a5cb-70a7fe30a719'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Jabal Omar Marriott Hotel Makkah'
UNION ALL
SELECT '2230c276-5330-5986-a5cb-70a7fe30a719'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Shaza Al Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'SINGLE', 202500.00),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'DOUBLE', 148000.00),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'TRIPLE', 127500.00),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'QUAD', 114000.00),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'CHILD', 82000.00),
    ('2230c276-5330-5986-a5cb-70a7fe30a719', 'INFANT', 16000.00);

-- SEED-DST-05 | November Umrah - 12 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '35472b50-9297-57bc-9d76-cb941e961af5', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DST-05', 'November Umrah - 12 Nights',
    DATE '2026-11-18', DATE '2026-11-30',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Nile Air',
    0, NULL, NULL, 6, 6,
    true, true, false, false, true,
    false, '12 nights total: 6 in Makkah at Pullman ZamZam Makkah (220m from the Haram) and 6 in Madinah at Golden Tulip Al Mektan (500m from the Prophet''s Mosque). Direct flight from CAI to JED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI.',
    (SELECT id FROM currencies WHERE code = 'USD'), 43, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '35472b50-9297-57bc-9d76-cb941e961af5'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Pullman ZamZam Makkah'
UNION ALL
SELECT '35472b50-9297-57bc-9d76-cb941e961af5'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Golden Tulip Al Mektan';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'SINGLE', 122500.00),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'DOUBLE', 89500.00),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'TRIPLE', 77000.00),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'QUAD', 69000.00),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'CHILD', 49500.00),
    ('35472b50-9297-57bc-9d76-cb941e961af5', 'INFANT', 9500.00);

-- SEED-DST-06 | Rajab Umrah - 14 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DST-06', 'Rajab Umrah - 14 Nights',
    DATE '2026-12-15', DATE '2026-12-29',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'EgyptAir',
    0, NULL, NULL, 7, 7,
    true, true, true, true, true,
    true, '14 nights total: 7 in Makkah at Hilton Makkah Convention Hotel (350m from the Haram) and 7 in Madinah at Anwar Al Madinah Movenpick (100m from the Prophet''s Mosque). Direct flight from CAI to MED on EgyptAir. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'USD'), 38, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '588c09c7-d1f0-5bea-b5eb-0be97e79e087'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Hilton Makkah Convention Hotel'
UNION ALL
SELECT '588c09c7-d1f0-5bea-b5eb-0be97e79e087'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Anwar Al Madinah Movenpick';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'SINGLE', 242000.00),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'DOUBLE', 177000.00),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'TRIPLE', 152500.00),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'QUAD', 136000.00),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'CHILD', 98000.00),
    ('588c09c7-d1f0-5bea-b5eb-0be97e79e087', 'INFANT', 19000.00);

-- SEED-DST-07 | Sha'ban Umrah - 15 Nights | VIP
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DST-07', 'Sha''ban Umrah - 15 Nights',
    DATE '2027-01-12', DATE '2027-01-27',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Saudia',
    0, NULL, NULL, 8, 7,
    true, true, true, true, true,
    true, '15 nights total: 8 in Makkah at Conrad Makkah (400m from the Haram) and 7 in Madinah at Al Eiman Royal Hotel (200m from the Prophet''s Mosque). Direct flight from CAI to JED on Saudia. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'USD'), 32, 'PUBLISHED', 'VIP'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'cf9fa25c-6980-5f71-a0e0-79fb9824cca2'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Conrad Makkah'
UNION ALL
SELECT 'cf9fa25c-6980-5f71-a0e0-79fb9824cca2'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Al Eiman Royal Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'SINGLE', 346500.00),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'DOUBLE', 253000.00),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'TRIPLE', 218000.00),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'QUAD', 195000.00),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'CHILD', 140000.00),
    ('cf9fa25c-6980-5f71-a0e0-79fb9824cca2', 'INFANT', 27500.00);

-- SEED-DST-08 | Ramadan Umrah - First Ten - 7 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '9f46f443-fa6f-579b-bc98-38a214365201', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DST-08', 'Ramadan Umrah - First Ten - 7 Nights',
    DATE '2027-02-10', DATE '2027-02-17',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Air Cairo',
    0, NULL, NULL, 4, 3,
    true, true, true, true, true,
    true, '7 nights total: 4 in Makkah at Elaf Kinda Hotel (300m from the Haram) and 3 in Madinah at Nozol Royal Inn (1100m from the Prophet''s Mosque). Direct flight from CAI to JED on Air Cairo. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'USD'), 50, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '9f46f443-fa6f-579b-bc98-38a214365201'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Elaf Kinda Hotel'
UNION ALL
SELECT '9f46f443-fa6f-579b-bc98-38a214365201'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Nozol Royal Inn';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'SINGLE', 241000.00),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'DOUBLE', 176000.00),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'TRIPLE', 152000.00),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'QUAD', 135500.00),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'CHILD', 97500.00),
    ('9f46f443-fa6f-579b-bc98-38a214365201', 'INFANT', 19000.00);

-- SEED-DST-09 | Ramadan Umrah - Last Ten - 8 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '54d161b5-b060-5d40-96e6-306db3eaa11e', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DST-09', 'Ramadan Umrah - Last Ten - 8 Nights',
    DATE '2027-02-24', DATE '2027-03-04',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'flynas',
    0, NULL, NULL, 5, 3,
    true, true, true, true, true,
    true, '8 nights total: 5 in Makkah at Fairmont Makkah Clock Royal Tower (150m from the Haram) and 3 in Madinah at Odst Al Madinah Hotel (450m from the Prophet''s Mosque). Direct flight from CAI to MED on flynas. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'USD'), 31, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '54d161b5-b060-5d40-96e6-306db3eaa11e'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Fairmont Makkah Clock Royal Tower'
UNION ALL
SELECT '54d161b5-b060-5d40-96e6-306db3eaa11e'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Odst Al Madinah Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'SINGLE', 292500.00),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'DOUBLE', 213500.00),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'TRIPLE', 184000.00),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'QUAD', 164000.00),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'CHILD', 118000.00),
    ('54d161b5-b060-5d40-96e6-306db3eaa11e', 'INFANT', 23000.00);

-- SEED-DST-10 | Shawwal Umrah - 10 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '6989c1ce-5fa7-5591-a2e6-726b733a2474', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'SEED-DST-10', 'Shawwal Umrah - 10 Nights',
    DATE '2027-04-06', DATE '2027-04-16',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Nile Air',
    0, NULL, NULL, 5, 5,
    true, true, false, false, true,
    false, '10 nights total: 5 in Makkah at Swissotel Al Maqam Makkah (200m from the Haram) and 5 in Madinah at Elaf Taiba Hotel (250m from the Prophet''s Mosque). Direct flight from CAI to JED on Nile Air. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI.',
    (SELECT id FROM currencies WHERE code = 'USD'), 29, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '6989c1ce-5fa7-5591-a2e6-726b733a2474'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Swissotel Al Maqam Makkah'
UNION ALL
SELECT '6989c1ce-5fa7-5591-a2e6-726b733a2474'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Elaf Taiba Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'SINGLE', 130000.00),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'DOUBLE', 95000.00),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'TRIPLE', 82000.00),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'QUAD', 73000.00),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'CHILD', 52500.00),
    ('6989c1ce-5fa7-5591-a2e6-726b733a2474', 'INFANT', 10000.00);


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

-- SEED-MAA-01 | Late Summer Umrah - 7 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '585246c2-f936-5abe-8232-9bd7131ce518', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MAA-01', 'Late Summer Umrah - 7 Nights',
    DATE '2026-08-23', DATE '2026-08-30',
    (SELECT id FROM airports WHERE iata_code = 'ATZ'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'ATZ'),
    'Saudia',
    0, NULL, NULL, 4, 3,
    true, true, false, false, true,
    false, '7 nights total: 4 in Makkah at Al Shohada Hotel (350m from the Haram) and 3 in Madinah at Dar Al Iman InterContinental Madinah (180m from the Prophet''s Mosque). Direct flight from ATZ to JED on Saudia. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to ATZ.',
    (SELECT id FROM currencies WHERE code = 'SAR'), 53, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '585246c2-f936-5abe-8232-9bd7131ce518'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Al Shohada Hotel'
UNION ALL
SELECT '585246c2-f936-5abe-8232-9bd7131ce518'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Dar Al Iman InterContinental Madinah';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'SINGLE', 134500.00),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'DOUBLE', 98000.00),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'TRIPLE', 84500.00),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'QUAD', 75500.00),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'CHILD', 54500.00),
    ('585246c2-f936-5abe-8232-9bd7131ce518', 'INFANT', 10500.00);

-- SEED-MAA-02 | September Umrah - 8 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '9e2ea781-005f-52a4-9ce3-af372e471fde', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MAA-02', 'September Umrah - 8 Nights',
    DATE '2026-09-17', DATE '2026-09-25',
    (SELECT id FROM airports WHERE iata_code = 'LXR'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'LXR'),
    'Air Cairo',
    0, NULL, NULL, 4, 4,
    true, true, false, false, true,
    false, '8 nights total: 4 in Makkah at Elaf Kinda Hotel (300m from the Haram) and 4 in Madinah at Al Ansar Golden Hotel (900m from the Prophet''s Mosque). Direct flight from LXR to JED on Air Cairo. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to LXR.',
    (SELECT id FROM currencies WHERE code = 'SAR'), 59, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '9e2ea781-005f-52a4-9ce3-af372e471fde'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Elaf Kinda Hotel'
UNION ALL
SELECT '9e2ea781-005f-52a4-9ce3-af372e471fde'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Al Ansar Golden Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'SINGLE', 121000.00),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'DOUBLE', 88500.00),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'TRIPLE', 76000.00),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'QUAD', 68000.00),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'CHILD', 49000.00),
    ('9e2ea781-005f-52a4-9ce3-af372e471fde', 'INFANT', 9500.00);

-- SEED-MAA-03 | Autumn Umrah - 10 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '02c6971c-bc4a-52f6-bfa5-e472e0429054', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MAA-03', 'Autumn Umrah - 10 Nights',
    DATE '2026-10-08', DATE '2026-10-18',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'flynas',
    0, NULL, NULL, 5, 5,
    true, true, false, false, true,
    true, '10 nights total: 5 in Makkah at Anjum Hotel Makkah (900m from the Haram) and 5 in Madinah at The Oberoi Madina (200m from the Prophet''s Mosque). Direct flight from CAI to MED on flynas. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'SAR'), 48, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '02c6971c-bc4a-52f6-bfa5-e472e0429054'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Anjum Hotel Makkah'
UNION ALL
SELECT '02c6971c-bc4a-52f6-bfa5-e472e0429054'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'The Oberoi Madina';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'SINGLE', 118500.00),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'DOUBLE', 86500.00),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'TRIPLE', 74500.00),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'QUAD', 66500.00),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'CHILD', 48000.00),
    ('02c6971c-bc4a-52f6-bfa5-e472e0429054', 'INFANT', 9500.00);

-- SEED-MAA-04 | Mid-Term Break Umrah - 12 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'c9d16fd9-bc66-57e6-934d-be23696d3595', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MAA-04', 'Mid-Term Break Umrah - 12 Nights',
    DATE '2026-10-30', DATE '2026-11-11',
    (SELECT id FROM airports WHERE iata_code = 'ATZ'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'ATZ'),
    'Nile Air',
    0, NULL, NULL, 6, 6,
    true, true, true, true, true,
    true, '12 nights total: 6 in Makkah at Dar Al Tawhid InterContinental Makkah (100m from the Haram) and 6 in Madinah at Dar Al Taqwa Hotel (120m from the Prophet''s Mosque). Direct flight from ATZ to JED on Nile Air. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to ATZ. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'SAR'), 52, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'c9d16fd9-bc66-57e6-934d-be23696d3595'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Dar Al Tawhid InterContinental Makkah'
UNION ALL
SELECT 'c9d16fd9-bc66-57e6-934d-be23696d3595'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Dar Al Taqwa Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'SINGLE', 186000.00),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'DOUBLE', 136000.00),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'TRIPLE', 117000.00),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'QUAD', 104500.00),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'CHILD', 75500.00),
    ('c9d16fd9-bc66-57e6-934d-be23696d3595', 'INFANT', 14500.00);

-- SEED-MAA-05 | November Umrah - 14 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '32be2121-93f2-5411-8f94-7aa6f785a2a0', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MAA-05', 'November Umrah - 14 Nights',
    DATE '2026-11-20', DATE '2026-12-04',
    (SELECT id FROM airports WHERE iata_code = 'LXR'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'LXR'),
    'EgyptAir',
    1, 'Riyadh', '2h 40m', 7, 7,
    true, true, false, false, true,
    false, '14 nights total: 7 in Makkah at Al Kiswah Towers Hotel (1800m from the Haram) and 7 in Madinah at Elaf Taiba Hotel (250m from the Prophet''s Mosque). One stop in Riyadh from LXR to JED on EgyptAir. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to LXR.',
    (SELECT id FROM currencies WHERE code = 'SAR'), 41, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '32be2121-93f2-5411-8f94-7aa6f785a2a0'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Al Kiswah Towers Hotel'
UNION ALL
SELECT '32be2121-93f2-5411-8f94-7aa6f785a2a0'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Elaf Taiba Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'SINGLE', 120500.00),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'DOUBLE', 88000.00),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'TRIPLE', 76000.00),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'QUAD', 67500.00),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'CHILD', 48500.00),
    ('32be2121-93f2-5411-8f94-7aa6f785a2a0', 'INFANT', 9500.00);

-- SEED-MAA-06 | Rajab Umrah - 15 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'bbde726c-6971-5ac4-837b-1504b7aa5733', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MAA-06', 'Rajab Umrah - 15 Nights',
    DATE '2026-12-17', DATE '2027-01-01',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Saudia',
    0, NULL, NULL, 8, 7,
    true, true, false, false, true,
    true, '15 nights total: 8 in Makkah at Emaar Grand Hotel (550m from the Haram) and 7 in Madinah at Saja Al Madinah Hotel (600m from the Prophet''s Mosque). Direct flight from CAI to MED on Saudia. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'SAR'), 53, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'bbde726c-6971-5ac4-837b-1504b7aa5733'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Emaar Grand Hotel'
UNION ALL
SELECT 'bbde726c-6971-5ac4-837b-1504b7aa5733'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Saja Al Madinah Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'SINGLE', 141500.00),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'DOUBLE', 103500.00),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'TRIPLE', 89000.00),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'QUAD', 79500.00),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'CHILD', 57500.00),
    ('bbde726c-6971-5ac4-837b-1504b7aa5733', 'INFANT', 11000.00);

-- SEED-MAA-07 | Sha'ban Umrah - 7 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MAA-07', 'Sha''ban Umrah - 7 Nights',
    DATE '2027-01-14', DATE '2027-01-21',
    (SELECT id FROM airports WHERE iata_code = 'ATZ'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'JED'), (SELECT id FROM airports WHERE iata_code = 'ATZ'),
    'Air Cairo',
    0, NULL, NULL, 4, 3,
    true, true, false, false, true,
    false, '7 nights total: 4 in Makkah at Makkah Towers (250m from the Haram) and 3 in Madinah at Frontel Al Harithia Hotel (350m from the Prophet''s Mosque). Direct flight from ATZ to JED on Air Cairo. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from JED to ATZ.',
    (SELECT id FROM currencies WHERE code = 'SAR'), 48, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'f9e1e65c-7f74-51d1-9b30-cb8166dbbedf'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Makkah Towers'
UNION ALL
SELECT 'f9e1e65c-7f74-51d1-9b30-cb8166dbbedf'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Frontel Al Harithia Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'SINGLE', 134000.00),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'DOUBLE', 98000.00),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'TRIPLE', 84500.00),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'QUAD', 75500.00),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'CHILD', 54500.00),
    ('f9e1e65c-7f74-51d1-9b30-cb8166dbbedf', 'INFANT', 10500.00);

-- SEED-MAA-08 | Ramadan Umrah - First Ten - 8 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MAA-08', 'Ramadan Umrah - First Ten - 8 Nights',
    DATE '2027-02-12', DATE '2027-02-20',
    (SELECT id FROM airports WHERE iata_code = 'LXR'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'LXR'),
    'flynas',
    0, NULL, NULL, 5, 3,
    true, true, false, false, true,
    false, '8 nights total: 5 in Makkah at Fairmont Makkah Clock Royal Tower (150m from the Haram) and 3 in Madinah at Anwar Al Madinah Movenpick (100m from the Prophet''s Mosque). Direct flight from LXR to JED on flynas. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to LXR.',
    (SELECT id FROM currencies WHERE code = 'SAR'), 41, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'fd3427da-dd5f-5a25-a4a8-9908c9d4dc57'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Fairmont Makkah Clock Royal Tower'
UNION ALL
SELECT 'fd3427da-dd5f-5a25-a4a8-9908c9d4dc57'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Anwar Al Madinah Movenpick';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'SINGLE', 162000.00),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'DOUBLE', 118500.00),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'TRIPLE', 102000.00),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'QUAD', 91000.00),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'CHILD', 65500.00),
    ('fd3427da-dd5f-5a25-a4a8-9908c9d4dc57', 'INFANT', 12500.00);

-- SEED-MAA-09 | Ramadan Umrah - Last Ten - 10 Nights | PREMIUM
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    '8b4eef5d-e56f-5767-aae5-e0833d91b227', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MAA-09', 'Ramadan Umrah - Last Ten - 10 Nights',
    DATE '2027-02-26', DATE '2027-03-08',
    (SELECT id FROM airports WHERE iata_code = 'CAI'), (SELECT id FROM airports WHERE iata_code = 'MED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'CAI'),
    'Nile Air',
    0, NULL, NULL, 7, 3,
    true, true, true, true, true,
    true, '10 nights total: 7 in Makkah at Swissotel Al Maqam Makkah (200m from the Haram) and 3 in Madinah at Nozol Royal Inn (1100m from the Prophet''s Mosque). Direct flight from CAI to MED on Nile Air. Full board with buffet meals and a dedicated group supervisor. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to CAI. Haramain high-speed train between Makkah and Madinah included.',
    (SELECT id FROM currencies WHERE code = 'SAR'), 29, 'PUBLISHED', 'PREMIUM'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT '8b4eef5d-e56f-5767-aae5-e0833d91b227'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Swissotel Al Maqam Makkah'
UNION ALL
SELECT '8b4eef5d-e56f-5767-aae5-e0833d91b227'::uuid, h.id, h.city, true FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Nozol Royal Inn';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'SINGLE', 327000.00),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'DOUBLE', 239000.00),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'TRIPLE', 206000.00),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'QUAD', 183500.00),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'CHILD', 132500.00),
    ('8b4eef5d-e56f-5767-aae5-e0833d91b227', 'INFANT', 25500.00);

-- SEED-MAA-10 | Shawwal Umrah - 12 Nights | ECONOMIC
INSERT INTO trips (
    id, company_id, trip_code, title, departure_date, return_date,
    outbound_departure_airport_id, outbound_arrival_airport_id,
    return_departure_airport_id, return_arrival_airport_id,
    airline,
    transit_count, transit_city, transit_duration, days_in_makkah, days_in_madinah,
    visa_included, transportation_included, meals_included, guide_included, zamzam_included,
    fast_train_included, description, currency_id, available_seats, status, tier
) VALUES (
    'fdc42d64-37cd-5fae-867b-a74a81453bb5', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'SEED-MAA-10', 'Shawwal Umrah - 12 Nights',
    DATE '2027-04-08', DATE '2027-04-20',
    (SELECT id FROM airports WHERE iata_code = 'ATZ'), (SELECT id FROM airports WHERE iata_code = 'JED'),
    (SELECT id FROM airports WHERE iata_code = 'MED'), (SELECT id FROM airports WHERE iata_code = 'ATZ'),
    'EgyptAir',
    1, 'Riyadh', '2h 40m', 6, 6,
    true, true, false, false, true,
    false, '12 nights total: 6 in Makkah at Jabal Omar Marriott Hotel Makkah (450m from the Haram) and 6 in Madinah at Al Eiman Royal Hotel (200m from the Prophet''s Mosque). One stop in Riyadh from ATZ to JED on EgyptAir. Breakfast included; half board available on request. Includes Umrah visa, airport transfers, and inter-city coach transport. Returns from MED to ATZ.',
    (SELECT id FROM currencies WHERE code = 'SAR'), 56, 'PUBLISHED', 'ECONOMIC'
);

INSERT INTO trip_hotels (trip_id, hotel_id, city, free_bus_included)
SELECT 'fdc42d64-37cd-5fae-867b-a74a81453bb5'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MAKKAH' AND h.name = 'Jabal Omar Marriott Hotel Makkah'
UNION ALL
SELECT 'fdc42d64-37cd-5fae-867b-a74a81453bb5'::uuid, h.id, h.city, false FROM hotels h WHERE h.city = 'MADINAH' AND h.name = 'Al Eiman Royal Hotel';

INSERT INTO room_prices (trip_id, room_type, price) VALUES
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'SINGLE', 124500.00),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'DOUBLE', 91000.00),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'TRIPLE', 78000.00),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'QUAD', 70000.00),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'CHILD', 50500.00),
    ('fdc42d64-37cd-5fae-867b-a74a81453bb5', 'INFANT', 10000.00);


-- =============================================================================
-- Customers
-- =============================================================================

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('6e1e51ec-b1e8-54d2-9791-428092682942', 'ahmed.fathy@seed.test', 'dev-test:ahmed.fathy@seed.test', 'CUSTOMER', 'ACTIVE');
INSERT INTO customer_profiles (id, user_id, full_name, phone, cashback_wallet_number, wallet_type, profile_completed) VALUES
    ('47358fb8-40f9-52f6-ae6e-b9c13eec4f17', '6e1e51ec-b1e8-54d2-9791-428092682942', 'Ahmed Fathy', '+201012345601', '+201012345601', 'VODAFONE_CASH', true);

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('eab8c72c-4e45-5c80-a39d-805d80339de0', 'mona.said@seed.test', 'dev-test:mona.said@seed.test', 'CUSTOMER', 'ACTIVE');
INSERT INTO customer_profiles (id, user_id, full_name, phone, cashback_wallet_number, wallet_type, profile_completed) VALUES
    ('33540d94-d926-5230-8bfd-86aa0efe51c5', 'eab8c72c-4e45-5c80-a39d-805d80339de0', 'Mona Said', '+201123456702', '+201123456702', 'ETISALAT_CASH', true);

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'youssef.ibrahim@seed.test', 'dev-test:youssef.ibrahim@seed.test', 'CUSTOMER', 'ACTIVE');
INSERT INTO customer_profiles (id, user_id, full_name, phone, cashback_wallet_number, wallet_type, profile_completed) VALUES
    ('7f458b6a-69bb-5967-b8b2-594769aba39e', '17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'Youssef Ibrahim', '+201234567803', '+201234567803', 'INSTA_PAY', true);

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', 'salma.adel@seed.test', 'dev-test:salma.adel@seed.test', 'CUSTOMER', 'ACTIVE');
INSERT INTO customer_profiles (id, user_id, full_name, phone, cashback_wallet_number, wallet_type, profile_completed) VALUES
    ('de7f6de7-d84f-556b-9121-e0d990034537', 'e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', 'Salma Adel', '+201598765404', '+201598765404', 'VODAFONE_CASH', true);

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('777e69a0-1778-5ece-8cea-080522881f82', 'karim.reda@seed.test', 'dev-test:karim.reda@seed.test', 'CUSTOMER', 'ACTIVE');
INSERT INTO customer_profiles (id, user_id, full_name, phone, cashback_wallet_number, wallet_type, profile_completed) VALUES
    ('36a30181-98ce-5388-8788-7774660c8412', '777e69a0-1778-5ece-8cea-080522881f82', 'Karim Reda', '+201187654305', '+201187654305', 'ETISALAT_CASH', true);

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('2fe2fa07-0cf2-551a-b504-aad6edb4d637', 'nourhan.tarek@seed.test', 'dev-test:nourhan.tarek@seed.test', 'CUSTOMER', 'ACTIVE');
INSERT INTO customer_profiles (id, user_id, full_name, phone, cashback_wallet_number, wallet_type, profile_completed) VALUES
    ('09a57c84-05c9-5e89-a03b-c05c7694ff3c', '2fe2fa07-0cf2-551a-b504-aad6edb4d637', 'Nourhan Tarek', NULL, NULL, NULL, false);

-- These last two exist because a customer may hold only one preserved journey (uq_leads_customer_active),
-- and the eight leads below cover eight lifecycle stages. Without them two customers would each need
-- two live leads, which the database now refuses.
INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('4c9f2f56-3a1b-5d47-9e2c-1f7a6b8d0e31', 'hala.mostafa@seed.test', 'dev-test:hala.mostafa@seed.test', 'CUSTOMER', 'ACTIVE');
INSERT INTO customer_profiles (id, user_id, full_name, phone, cashback_wallet_number, wallet_type, profile_completed) VALUES
    ('c8b21a90-77d4-5f1e-93a6-2b5e0c7f4a68', '4c9f2f56-3a1b-5d47-9e2c-1f7a6b8d0e31', 'Hala Mostafa', '+201012345606', '+201012345606', 'VODAFONE_CASH', true);

INSERT INTO users (id, email, google_sub, role, status) VALUES
    ('9d3e7b12-5c48-5a6f-8e91-4d2c7a0f6b53', 'tarek.samir@seed.test', 'dev-test:tarek.samir@seed.test', 'CUSTOMER', 'ACTIVE');
INSERT INTO customer_profiles (id, user_id, full_name, phone, cashback_wallet_number, wallet_type, profile_completed) VALUES
    ('e5a4c318-6b92-5d70-af23-8c1e9f4b2d06', '9d3e7b12-5c48-5a6f-8e91-4d2c7a0f6b53', 'Tarek Samir', '+201123456707', '+201123456707', 'INSTA_PAY', true);

-- =============================================================================
-- Favourites
-- =============================================================================

INSERT INTO favourites (customer_id, trip_id) VALUES
    ('47358fb8-40f9-52f6-ae6e-b9c13eec4f17', '52892f2b-6f2a-5c44-81a8-0bf302cf046a'),
    ('47358fb8-40f9-52f6-ae6e-b9c13eec4f17', 'b055074c-1edf-53a9-8ba0-3a08e24f7554'),
    ('33540d94-d926-5230-8bfd-86aa0efe51c5', 'a37d2dc4-dae2-5a9e-96e4-390eef2a4bfe'),
    ('33540d94-d926-5230-8bfd-86aa0efe51c5', '585246c2-f936-5abe-8232-9bd7131ce518'),
    ('7f458b6a-69bb-5967-b8b2-594769aba39e', '3a5dabc5-85a0-58ce-8465-d6650a3d3d17'),
    ('36a30181-98ce-5388-8788-7774660c8412', '30b45ec6-e8dc-5b66-ac50-a1d8fe5aed87'),
    ('36a30181-98ce-5388-8788-7774660c8412', 'd069050e-671b-530f-829d-cd5e888f455f');

-- =============================================================================
-- Leads — one per lifecycle stage, with the history/ledger/notification trail a real
-- booking moving through that many steps would actually leave behind.
-- =============================================================================

-- Lead 1/8: hala-mostafa -> INTERESTED (NAH, 2 adult/1 child/0 infant)
INSERT INTO leads (
    id, customer_id, trip_id, company_id, status, adult_count, child_count, infant_count,
    commission_per_traveler, commission_amount, cashback_amount, commission_policy, cashback_policy,
    deposit_reported_by, deposit_reported_at, deposit_confirmed_by, deposit_confirmed_at,
    full_payment_reported_by, full_payment_reported_at, full_payment_confirmed_by, full_payment_confirmed_at,
    commission_reported_by, commission_reported_at, commission_paid_by, commission_paid_at,
    cashback_paid_by, cashback_paid_at, created_at, updated_at
) VALUES (
    'a4c81670-926d-5ecd-be2d-0f0d15fe44e9', 'c8b21a90-77d4-5f1e-93a6-2b5e0c7f4a68', '8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'INTERESTED', 2, 1, 0,
    2500.00, 5000.00, 1250.00, 'PER_TRAVELER', 'COMMISSION_SHARE',
    NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL,
    NULL, NULL, now() - INTERVAL '31 days', now()
);

INSERT INTO lead_status_history (lead_id, from_status, to_status, changed_by, changed_at, note) VALUES
    ('a4c81670-926d-5ecd-be2d-0f0d15fe44e9', NULL, 'INTERESTED', '4c9f2f56-3a1b-5d47-9e2c-1f7a6b8d0e31', now() - INTERVAL '31 days', NULL);

INSERT INTO notifications (recipient_user_id, type, title, body, data, created_at) VALUES
    ('00399ae8-dc8c-5d0c-8905-079fb675aaae', 'NEW_LEAD', 'New interested customer', 'A customer is interested in this trip for 2 traveler(s).', '{"leadId": "a4c81670-926d-5ecd-be2d-0f0d15fe44e9", "tripId": "8ce534bd-a036-5a7a-b9d7-7ed5defedcc8"}'::jsonb, now() - INTERVAL '31 days');

INSERT INTO analytics_events (user_id, event_type, entity_type, entity_id, created_at) VALUES
    ('4c9f2f56-3a1b-5d47-9e2c-1f7a6b8d0e31', 'CONTACT_COMPANY', 'Trip', '8ce534bd-a036-5a7a-b9d7-7ed5defedcc8', now() - INTERVAL '31 days');

-- Lead 2/8: tarek-samir -> PENDING_DEPOSIT_CONFIRMATION (SUS, 3 adult/0 child/1 infant)
INSERT INTO leads (
    id, customer_id, trip_id, company_id, status, adult_count, child_count, infant_count,
    commission_per_traveler, commission_amount, cashback_amount, commission_policy, cashback_policy,
    deposit_reported_by, deposit_reported_at, deposit_confirmed_by, deposit_confirmed_at,
    full_payment_reported_by, full_payment_reported_at, full_payment_confirmed_by, full_payment_confirmed_at,
    commission_reported_by, commission_reported_at, commission_paid_by, commission_paid_at,
    cashback_paid_by, cashback_paid_at, created_at, updated_at
) VALUES (
    '3fa3b225-d1f7-5e6c-b5ce-447e223aa12b', 'e5a4c318-6b92-5d70-af23-8c1e9f4b2d06', '221d7dea-a52c-5eab-93e5-cb35e31b2cde', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'PENDING_DEPOSIT_CONFIRMATION', 3, 0, 1,
    1800.00, 5400.00, 1350.00, 'PER_TRAVELER', 'COMMISSION_SHARE',
    '9d3e7b12-5c48-5a6f-8e91-4d2c7a0f6b53', now() - INTERVAL '25 days', NULL, NULL,
    NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL,
    NULL, NULL, now() - INTERVAL '27 days', now()
);

INSERT INTO lead_status_history (lead_id, from_status, to_status, changed_by, changed_at, note) VALUES
    ('3fa3b225-d1f7-5e6c-b5ce-447e223aa12b', NULL, 'INTERESTED', '9d3e7b12-5c48-5a6f-8e91-4d2c7a0f6b53', now() - INTERVAL '27 days', NULL),
    ('3fa3b225-d1f7-5e6c-b5ce-447e223aa12b', 'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', '9d3e7b12-5c48-5a6f-8e91-4d2c7a0f6b53', now() - INTERVAL '25 days', 'Transferred via InstaPay.');

INSERT INTO notifications (recipient_user_id, type, title, body, data, created_at) VALUES
    ('60249520-68d9-5c25-9a6c-39819b72a607', 'NEW_LEAD', 'New interested customer', 'A customer is interested in this trip for 3 traveler(s).', '{"leadId": "3fa3b225-d1f7-5e6c-b5ce-447e223aa12b", "tripId": "221d7dea-a52c-5eab-93e5-cb35e31b2cde"}'::jsonb, now() - INTERVAL '27 days'),
    ('60249520-68d9-5c25-9a6c-39819b72a607', 'DEPOSIT_CONFIRMATION_REQUIRED', 'Confirm a deposit', 'A customer reported paying the deposit for September Umrah - 12 Nights. Please confirm.', '{"leadId": "3fa3b225-d1f7-5e6c-b5ce-447e223aa12b", "tripId": "221d7dea-a52c-5eab-93e5-cb35e31b2cde"}'::jsonb, now() - INTERVAL '25 days');

INSERT INTO analytics_events (user_id, event_type, entity_type, entity_id, created_at) VALUES
    ('9d3e7b12-5c48-5a6f-8e91-4d2c7a0f6b53', 'CONTACT_COMPANY', 'Trip', '221d7dea-a52c-5eab-93e5-cb35e31b2cde', now() - INTERVAL '27 days');

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES
    ('9d3e7b12-5c48-5a6f-8e91-4d2c7a0f6b53', 'LEAD_REPORT_DEPOSIT', 'Lead', '3fa3b225-d1f7-5e6c-b5ce-447e223aa12b', '"INTERESTED"', '"PENDING_DEPOSIT_CONFIRMATION"', now() - INTERVAL '25 days');

-- Lead 3/8: youssef-ibrahim -> DEPOSIT_PAID (BAR, 2 adult/2 child/0 infant)
INSERT INTO leads (
    id, customer_id, trip_id, company_id, status, adult_count, child_count, infant_count,
    commission_per_traveler, commission_amount, cashback_amount, commission_policy, cashback_policy,
    deposit_reported_by, deposit_reported_at, deposit_confirmed_by, deposit_confirmed_at,
    full_payment_reported_by, full_payment_reported_at, full_payment_confirmed_by, full_payment_confirmed_at,
    commission_reported_by, commission_reported_at, commission_paid_by, commission_paid_at,
    cashback_paid_by, cashback_paid_at, created_at, updated_at
) VALUES (
    'b4cc7b38-6cd9-5c84-8d9c-d0b38fc7ba7b', '7f458b6a-69bb-5967-b8b2-594769aba39e', 'bd83afca-f45f-5f0d-9257-0ec388c52576', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'DEPOSIT_PAID', 2, 2, 0,
    1500.00, 3000.00, 750.00, 'PER_TRAVELER', 'COMMISSION_SHARE',
    '17dc5702-d8f6-54d6-8027-5811cdcbd8e9', now() - INTERVAL '21 days', '065dc9bf-f482-5680-83d2-91aaeef1799b', now() - INTERVAL '19 days',
    NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL,
    NULL, NULL, now() - INTERVAL '23 days', now()
);

INSERT INTO lead_status_history (lead_id, from_status, to_status, changed_by, changed_at, note) VALUES
    ('b4cc7b38-6cd9-5c84-8d9c-d0b38fc7ba7b', NULL, 'INTERESTED', '17dc5702-d8f6-54d6-8027-5811cdcbd8e9', now() - INTERVAL '23 days', NULL),
    ('b4cc7b38-6cd9-5c84-8d9c-d0b38fc7ba7b', 'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', '17dc5702-d8f6-54d6-8027-5811cdcbd8e9', now() - INTERVAL '21 days', 'Transferred via InstaPay.'),
    ('b4cc7b38-6cd9-5c84-8d9c-d0b38fc7ba7b', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID', '065dc9bf-f482-5680-83d2-91aaeef1799b', now() - INTERVAL '19 days', NULL);

INSERT INTO ratings (lead_id, trip_id, company_id, customer_id, stars, comment, created_at) VALUES
    ('b4cc7b38-6cd9-5c84-8d9c-d0b38fc7ba7b', 'bd83afca-f45f-5f0d-9257-0ec388c52576', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', '7f458b6a-69bb-5967-b8b2-594769aba39e', 4, 'Good so far, deposit was confirmed quickly.', now() - INTERVAL '19 days');

INSERT INTO notifications (recipient_user_id, type, title, body, data, created_at) VALUES
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'NEW_LEAD', 'New interested customer', 'A customer is interested in this trip for 2 traveler(s).', '{"leadId": "b4cc7b38-6cd9-5c84-8d9c-d0b38fc7ba7b", "tripId": "bd83afca-f45f-5f0d-9257-0ec388c52576"}'::jsonb, now() - INTERVAL '23 days'),
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'DEPOSIT_CONFIRMATION_REQUIRED', 'Confirm a deposit', 'A customer reported paying the deposit for Autumn Umrah - 15 Nights. Please confirm.', '{"leadId": "b4cc7b38-6cd9-5c84-8d9c-d0b38fc7ba7b", "tripId": "bd83afca-f45f-5f0d-9257-0ec388c52576"}'::jsonb, now() - INTERVAL '21 days'),
    ('17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'DEPOSIT_CONFIRMED', 'Deposit confirmed', 'Your deposit for Autumn Umrah - 15 Nights has been confirmed by the company.', '{"leadId": "b4cc7b38-6cd9-5c84-8d9c-d0b38fc7ba7b", "tripId": "bd83afca-f45f-5f0d-9257-0ec388c52576"}'::jsonb, now() - INTERVAL '19 days');

INSERT INTO analytics_events (user_id, event_type, entity_type, entity_id, created_at) VALUES
    ('17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'CONTACT_COMPANY', 'Trip', 'bd83afca-f45f-5f0d-9257-0ec388c52576', now() - INTERVAL '23 days');

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES
    ('17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'LEAD_REPORT_DEPOSIT', 'Lead', 'b4cc7b38-6cd9-5c84-8d9c-d0b38fc7ba7b', '"INTERESTED"', '"PENDING_DEPOSIT_CONFIRMATION"', now() - INTERVAL '21 days'),
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'LEAD_MARK_DEPOSIT_PAID', 'Lead', 'b4cc7b38-6cd9-5c84-8d9c-d0b38fc7ba7b', '"PENDING_DEPOSIT_CONFIRMATION"', '"DEPOSIT_PAID"', now() - INTERVAL '19 days');

-- Lead 4/8: ahmed-fathy -> PENDING_FULL_PAYMENT_CONFIRMATION (DST, 4 adult/0 child/0 infant)
INSERT INTO leads (
    id, customer_id, trip_id, company_id, status, adult_count, child_count, infant_count,
    commission_per_traveler, commission_amount, cashback_amount, commission_policy, cashback_policy,
    deposit_reported_by, deposit_reported_at, deposit_confirmed_by, deposit_confirmed_at,
    full_payment_reported_by, full_payment_reported_at, full_payment_confirmed_by, full_payment_confirmed_at,
    commission_reported_by, commission_reported_at, commission_paid_by, commission_paid_at,
    cashback_paid_by, cashback_paid_at, created_at, updated_at
) VALUES (
    '3fb7fb3e-8b24-594e-bc74-213db703a9a4', '47358fb8-40f9-52f6-ae6e-b9c13eec4f17', '2230c276-5330-5986-a5cb-70a7fe30a719', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'PENDING_FULL_PAYMENT_CONFIRMATION', 4, 0, 0,
    2000.00, 8000.00, 2000.00, 'PER_TRAVELER', 'COMMISSION_SHARE',
    '6e1e51ec-b1e8-54d2-9791-428092682942', now() - INTERVAL '17 days', 'b16dc04d-f9c8-5072-b9bc-ab1470a8cbc6', now() - INTERVAL '15 days',
    '6e1e51ec-b1e8-54d2-9791-428092682942', now() - INTERVAL '13 days', NULL, NULL,
    NULL, NULL, NULL, NULL,
    NULL, NULL, now() - INTERVAL '19 days', now()
);

INSERT INTO lead_status_history (lead_id, from_status, to_status, changed_by, changed_at, note) VALUES
    ('3fb7fb3e-8b24-594e-bc74-213db703a9a4', NULL, 'INTERESTED', '6e1e51ec-b1e8-54d2-9791-428092682942', now() - INTERVAL '19 days', NULL),
    ('3fb7fb3e-8b24-594e-bc74-213db703a9a4', 'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', '6e1e51ec-b1e8-54d2-9791-428092682942', now() - INTERVAL '17 days', 'Transferred via InstaPay.'),
    ('3fb7fb3e-8b24-594e-bc74-213db703a9a4', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID', 'b16dc04d-f9c8-5072-b9bc-ab1470a8cbc6', now() - INTERVAL '15 days', NULL),
    ('3fb7fb3e-8b24-594e-bc74-213db703a9a4', 'DEPOSIT_PAID', 'PENDING_FULL_PAYMENT_CONFIRMATION', '6e1e51ec-b1e8-54d2-9791-428092682942', now() - INTERVAL '13 days', 'Bank transfer completed.');

INSERT INTO ratings (lead_id, trip_id, company_id, customer_id, stars, comment, created_at) VALUES
    ('3fb7fb3e-8b24-594e-bc74-213db703a9a4', '2230c276-5330-5986-a5cb-70a7fe30a719', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', '47358fb8-40f9-52f6-ae6e-b9c13eec4f17', 4, 'Good so far, deposit was confirmed quickly.', now() - INTERVAL '13 days');

INSERT INTO notifications (recipient_user_id, type, title, body, data, created_at) VALUES
    ('b16dc04d-f9c8-5072-b9bc-ab1470a8cbc6', 'NEW_LEAD', 'New interested customer', 'A customer is interested in this trip for 4 traveler(s).', '{"leadId": "3fb7fb3e-8b24-594e-bc74-213db703a9a4", "tripId": "2230c276-5330-5986-a5cb-70a7fe30a719"}'::jsonb, now() - INTERVAL '19 days'),
    ('b16dc04d-f9c8-5072-b9bc-ab1470a8cbc6', 'DEPOSIT_CONFIRMATION_REQUIRED', 'Confirm a deposit', 'A customer reported paying the deposit for Mid-Term Break Umrah - 8 Nights. Please confirm.', '{"leadId": "3fb7fb3e-8b24-594e-bc74-213db703a9a4", "tripId": "2230c276-5330-5986-a5cb-70a7fe30a719"}'::jsonb, now() - INTERVAL '17 days'),
    ('6e1e51ec-b1e8-54d2-9791-428092682942', 'DEPOSIT_CONFIRMED', 'Deposit confirmed', 'Your deposit for Mid-Term Break Umrah - 8 Nights has been confirmed by the company.', '{"leadId": "3fb7fb3e-8b24-594e-bc74-213db703a9a4", "tripId": "2230c276-5330-5986-a5cb-70a7fe30a719"}'::jsonb, now() - INTERVAL '15 days'),
    ('b16dc04d-f9c8-5072-b9bc-ab1470a8cbc6', 'FULL_PAYMENT_CONFIRMATION_REQUIRED', 'Confirm a full payment', 'A customer reported paying in full for Mid-Term Break Umrah - 8 Nights. Please confirm.', '{"leadId": "3fb7fb3e-8b24-594e-bc74-213db703a9a4", "tripId": "2230c276-5330-5986-a5cb-70a7fe30a719"}'::jsonb, now() - INTERVAL '13 days');

INSERT INTO analytics_events (user_id, event_type, entity_type, entity_id, created_at) VALUES
    ('6e1e51ec-b1e8-54d2-9791-428092682942', 'CONTACT_COMPANY', 'Trip', '2230c276-5330-5986-a5cb-70a7fe30a719', now() - INTERVAL '19 days');

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES
    ('6e1e51ec-b1e8-54d2-9791-428092682942', 'LEAD_REPORT_DEPOSIT', 'Lead', '3fb7fb3e-8b24-594e-bc74-213db703a9a4', '"INTERESTED"', '"PENDING_DEPOSIT_CONFIRMATION"', now() - INTERVAL '17 days'),
    ('b16dc04d-f9c8-5072-b9bc-ab1470a8cbc6', 'LEAD_MARK_DEPOSIT_PAID', 'Lead', '3fb7fb3e-8b24-594e-bc74-213db703a9a4', '"PENDING_DEPOSIT_CONFIRMATION"', '"DEPOSIT_PAID"', now() - INTERVAL '15 days'),
    ('6e1e51ec-b1e8-54d2-9791-428092682942', 'LEAD_REPORT_FULL_PAYMENT', 'Lead', '3fb7fb3e-8b24-594e-bc74-213db703a9a4', '"DEPOSIT_PAID"', '"PENDING_FULL_PAYMENT_CONFIRMATION"', now() - INTERVAL '13 days');

-- Lead 5/8: salma-adel -> FULLY_PAID (MAA, 2 adult/1 child/1 infant)
INSERT INTO leads (
    id, customer_id, trip_id, company_id, status, adult_count, child_count, infant_count,
    commission_per_traveler, commission_amount, cashback_amount, commission_policy, cashback_policy,
    deposit_reported_by, deposit_reported_at, deposit_confirmed_by, deposit_confirmed_at,
    full_payment_reported_by, full_payment_reported_at, full_payment_confirmed_by, full_payment_confirmed_at,
    commission_reported_by, commission_reported_at, commission_paid_by, commission_paid_at,
    cashback_paid_by, cashback_paid_at, created_at, updated_at
) VALUES (
    '6d2f6081-995f-5947-a640-f0f98c5ba75a', 'de7f6de7-d84f-556b-9121-e0d990034537', '32be2121-93f2-5411-8f94-7aa6f785a2a0', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'FULLY_PAID', 2, 1, 1,
    1200.00, 2400.00, 600.00, 'PER_TRAVELER', 'COMMISSION_SHARE',
    'e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', now() - INTERVAL '13 days', '2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7', now() - INTERVAL '11 days',
    'e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', now() - INTERVAL '9 days', '2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7', now() - INTERVAL '7 days',
    NULL, NULL, NULL, NULL,
    NULL, NULL, now() - INTERVAL '15 days', now()
);

INSERT INTO lead_status_history (lead_id, from_status, to_status, changed_by, changed_at, note) VALUES
    ('6d2f6081-995f-5947-a640-f0f98c5ba75a', NULL, 'INTERESTED', 'e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', now() - INTERVAL '15 days', NULL),
    ('6d2f6081-995f-5947-a640-f0f98c5ba75a', 'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', 'e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', now() - INTERVAL '13 days', 'Transferred via InstaPay.'),
    ('6d2f6081-995f-5947-a640-f0f98c5ba75a', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID', '2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7', now() - INTERVAL '11 days', NULL),
    ('6d2f6081-995f-5947-a640-f0f98c5ba75a', 'DEPOSIT_PAID', 'PENDING_FULL_PAYMENT_CONFIRMATION', 'e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', now() - INTERVAL '9 days', 'Bank transfer completed.'),
    ('6d2f6081-995f-5947-a640-f0f98c5ba75a', 'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID', '2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7', now() - INTERVAL '7 days', NULL);

INSERT INTO ratings (lead_id, trip_id, company_id, customer_id, stars, comment, created_at) VALUES
    ('6d2f6081-995f-5947-a640-f0f98c5ba75a', '32be2121-93f2-5411-8f94-7aa6f785a2a0', 'd73d9c94-0da0-5540-a193-46fe2658392a', 'de7f6de7-d84f-556b-9121-e0d990034537', 4, 'Good so far, deposit was confirmed quickly.', now() - INTERVAL '7 days');

INSERT INTO notifications (recipient_user_id, type, title, body, data, created_at) VALUES
    ('2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7', 'NEW_LEAD', 'New interested customer', 'A customer is interested in this trip for 2 traveler(s).', '{"leadId": "6d2f6081-995f-5947-a640-f0f98c5ba75a", "tripId": "32be2121-93f2-5411-8f94-7aa6f785a2a0"}'::jsonb, now() - INTERVAL '15 days'),
    ('2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7', 'DEPOSIT_CONFIRMATION_REQUIRED', 'Confirm a deposit', 'A customer reported paying the deposit for November Umrah - 12 Nights. Please confirm.', '{"leadId": "6d2f6081-995f-5947-a640-f0f98c5ba75a", "tripId": "32be2121-93f2-5411-8f94-7aa6f785a2a0"}'::jsonb, now() - INTERVAL '13 days'),
    ('e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', 'DEPOSIT_CONFIRMED', 'Deposit confirmed', 'Your deposit for November Umrah - 12 Nights has been confirmed by the company.', '{"leadId": "6d2f6081-995f-5947-a640-f0f98c5ba75a", "tripId": "32be2121-93f2-5411-8f94-7aa6f785a2a0"}'::jsonb, now() - INTERVAL '11 days'),
    ('2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7', 'FULL_PAYMENT_CONFIRMATION_REQUIRED', 'Confirm a full payment', 'A customer reported paying in full for November Umrah - 12 Nights. Please confirm.', '{"leadId": "6d2f6081-995f-5947-a640-f0f98c5ba75a", "tripId": "32be2121-93f2-5411-8f94-7aa6f785a2a0"}'::jsonb, now() - INTERVAL '9 days'),
    ('e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', 'FULL_PAYMENT_CONFIRMED', 'Full payment confirmed', 'Your full payment for November Umrah - 12 Nights has been confirmed by the company.', '{"leadId": "6d2f6081-995f-5947-a640-f0f98c5ba75a", "tripId": "32be2121-93f2-5411-8f94-7aa6f785a2a0"}'::jsonb, now() - INTERVAL '7 days');

INSERT INTO analytics_events (user_id, event_type, entity_type, entity_id, created_at) VALUES
    ('e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', 'CONTACT_COMPANY', 'Trip', '32be2121-93f2-5411-8f94-7aa6f785a2a0', now() - INTERVAL '15 days');

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES
    ('e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', 'LEAD_REPORT_DEPOSIT', 'Lead', '6d2f6081-995f-5947-a640-f0f98c5ba75a', '"INTERESTED"', '"PENDING_DEPOSIT_CONFIRMATION"', now() - INTERVAL '13 days'),
    ('2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7', 'LEAD_MARK_DEPOSIT_PAID', 'Lead', '6d2f6081-995f-5947-a640-f0f98c5ba75a', '"PENDING_DEPOSIT_CONFIRMATION"', '"DEPOSIT_PAID"', now() - INTERVAL '11 days'),
    ('e8c85ca1-c4ea-5cdf-b700-9a4c340e3e9f', 'LEAD_REPORT_FULL_PAYMENT', 'Lead', '6d2f6081-995f-5947-a640-f0f98c5ba75a', '"DEPOSIT_PAID"', '"PENDING_FULL_PAYMENT_CONFIRMATION"', now() - INTERVAL '9 days'),
    ('2e4fd770-1d5a-5f8a-8e55-ad0b208f9fc7', 'LEAD_MARK_FULLY_PAID', 'Lead', '6d2f6081-995f-5947-a640-f0f98c5ba75a', '"PENDING_FULL_PAYMENT_CONFIRMATION"', '"FULLY_PAID"', now() - INTERVAL '7 days');

-- Lead 6/8: mona-said -> PENDING_COMMISSION_CONFIRMATION (NAH, 3 adult/0 child/0 infant)
INSERT INTO leads (
    id, customer_id, trip_id, company_id, status, adult_count, child_count, infant_count,
    commission_per_traveler, commission_amount, cashback_amount, commission_policy, cashback_policy,
    deposit_reported_by, deposit_reported_at, deposit_confirmed_by, deposit_confirmed_at,
    full_payment_reported_by, full_payment_reported_at, full_payment_confirmed_by, full_payment_confirmed_at,
    commission_reported_by, commission_reported_at, commission_paid_by, commission_paid_at,
    cashback_paid_by, cashback_paid_at, created_at, updated_at
) VALUES (
    'fc4ef609-1956-5f86-af71-f57e99181448', '33540d94-d926-5230-8bfd-86aa0efe51c5', '76e82682-157f-50f2-bb32-46cde2778b9c', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'PENDING_COMMISSION_CONFIRMATION', 3, 0, 0,
    2500.00, 7500.00, 1875.00, 'PER_TRAVELER', 'COMMISSION_SHARE',
    'eab8c72c-4e45-5c80-a39d-805d80339de0', now() - INTERVAL '9 days', '00399ae8-dc8c-5d0c-8905-079fb675aaae', now() - INTERVAL '7 days',
    'eab8c72c-4e45-5c80-a39d-805d80339de0', now() - INTERVAL '5 days', '00399ae8-dc8c-5d0c-8905-079fb675aaae', now() - INTERVAL '3 days',
    '00399ae8-dc8c-5d0c-8905-079fb675aaae', now() - INTERVAL '1 days', NULL, NULL,
    NULL, NULL, now() - INTERVAL '11 days', now()
);

INSERT INTO lead_status_history (lead_id, from_status, to_status, changed_by, changed_at, note) VALUES
    ('fc4ef609-1956-5f86-af71-f57e99181448', NULL, 'INTERESTED', 'eab8c72c-4e45-5c80-a39d-805d80339de0', now() - INTERVAL '11 days', NULL),
    ('fc4ef609-1956-5f86-af71-f57e99181448', 'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', 'eab8c72c-4e45-5c80-a39d-805d80339de0', now() - INTERVAL '9 days', 'Transferred via InstaPay.'),
    ('fc4ef609-1956-5f86-af71-f57e99181448', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID', '00399ae8-dc8c-5d0c-8905-079fb675aaae', now() - INTERVAL '7 days', NULL),
    ('fc4ef609-1956-5f86-af71-f57e99181448', 'DEPOSIT_PAID', 'PENDING_FULL_PAYMENT_CONFIRMATION', 'eab8c72c-4e45-5c80-a39d-805d80339de0', now() - INTERVAL '5 days', 'Bank transfer completed.'),
    ('fc4ef609-1956-5f86-af71-f57e99181448', 'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID', '00399ae8-dc8c-5d0c-8905-079fb675aaae', now() - INTERVAL '3 days', NULL),
    ('fc4ef609-1956-5f86-af71-f57e99181448', 'FULLY_PAID', 'PENDING_COMMISSION_CONFIRMATION', '00399ae8-dc8c-5d0c-8905-079fb675aaae', now() - INTERVAL '1 days', NULL);

INSERT INTO commissions (lead_id, company_id, amount, status, reported_by, reported_at, confirmed_by, confirmed_at) VALUES
    ('fc4ef609-1956-5f86-af71-f57e99181448', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 7500.00, 'REPORTED', '00399ae8-dc8c-5d0c-8905-079fb675aaae', now() - INTERVAL '1 days', NULL, NULL);

INSERT INTO ratings (lead_id, trip_id, company_id, customer_id, stars, comment, created_at) VALUES
    ('fc4ef609-1956-5f86-af71-f57e99181448', '76e82682-157f-50f2-bb32-46cde2778b9c', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', '33540d94-d926-5230-8bfd-86aa0efe51c5', 4, 'Good so far, deposit was confirmed quickly.', now() - INTERVAL '1 days');

INSERT INTO notifications (recipient_user_id, type, title, body, data, created_at) VALUES
    ('00399ae8-dc8c-5d0c-8905-079fb675aaae', 'NEW_LEAD', 'New interested customer', 'A customer is interested in this trip for 3 traveler(s).', '{"leadId": "fc4ef609-1956-5f86-af71-f57e99181448", "tripId": "76e82682-157f-50f2-bb32-46cde2778b9c"}'::jsonb, now() - INTERVAL '11 days'),
    ('00399ae8-dc8c-5d0c-8905-079fb675aaae', 'DEPOSIT_CONFIRMATION_REQUIRED', 'Confirm a deposit', 'A customer reported paying the deposit for Rajab Umrah - 7 Nights. Please confirm.', '{"leadId": "fc4ef609-1956-5f86-af71-f57e99181448", "tripId": "76e82682-157f-50f2-bb32-46cde2778b9c"}'::jsonb, now() - INTERVAL '9 days'),
    ('eab8c72c-4e45-5c80-a39d-805d80339de0', 'DEPOSIT_CONFIRMED', 'Deposit confirmed', 'Your deposit for Rajab Umrah - 7 Nights has been confirmed by the company.', '{"leadId": "fc4ef609-1956-5f86-af71-f57e99181448", "tripId": "76e82682-157f-50f2-bb32-46cde2778b9c"}'::jsonb, now() - INTERVAL '7 days'),
    ('00399ae8-dc8c-5d0c-8905-079fb675aaae', 'FULL_PAYMENT_CONFIRMATION_REQUIRED', 'Confirm a full payment', 'A customer reported paying in full for Rajab Umrah - 7 Nights. Please confirm.', '{"leadId": "fc4ef609-1956-5f86-af71-f57e99181448", "tripId": "76e82682-157f-50f2-bb32-46cde2778b9c"}'::jsonb, now() - INTERVAL '5 days'),
    ('eab8c72c-4e45-5c80-a39d-805d80339de0', 'FULL_PAYMENT_CONFIRMED', 'Full payment confirmed', 'Your full payment for Rajab Umrah - 7 Nights has been confirmed by the company.', '{"leadId": "fc4ef609-1956-5f86-af71-f57e99181448", "tripId": "76e82682-157f-50f2-bb32-46cde2778b9c"}'::jsonb, now() - INTERVAL '3 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'COMMISSION_CONFIRMATION_REQUIRED', 'Confirm a commission payment', 'A company reported paying its commission. Please confirm so cashback can be released.', '{"leadId": "fc4ef609-1956-5f86-af71-f57e99181448", "tripId": "76e82682-157f-50f2-bb32-46cde2778b9c"}'::jsonb, now() - INTERVAL '1 days');

INSERT INTO analytics_events (user_id, event_type, entity_type, entity_id, created_at) VALUES
    ('eab8c72c-4e45-5c80-a39d-805d80339de0', 'CONTACT_COMPANY', 'Trip', '76e82682-157f-50f2-bb32-46cde2778b9c', now() - INTERVAL '11 days');

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES
    ('eab8c72c-4e45-5c80-a39d-805d80339de0', 'LEAD_REPORT_DEPOSIT', 'Lead', 'fc4ef609-1956-5f86-af71-f57e99181448', '"INTERESTED"', '"PENDING_DEPOSIT_CONFIRMATION"', now() - INTERVAL '9 days'),
    ('00399ae8-dc8c-5d0c-8905-079fb675aaae', 'LEAD_MARK_DEPOSIT_PAID', 'Lead', 'fc4ef609-1956-5f86-af71-f57e99181448', '"PENDING_DEPOSIT_CONFIRMATION"', '"DEPOSIT_PAID"', now() - INTERVAL '7 days'),
    ('eab8c72c-4e45-5c80-a39d-805d80339de0', 'LEAD_REPORT_FULL_PAYMENT', 'Lead', 'fc4ef609-1956-5f86-af71-f57e99181448', '"DEPOSIT_PAID"', '"PENDING_FULL_PAYMENT_CONFIRMATION"', now() - INTERVAL '5 days'),
    ('00399ae8-dc8c-5d0c-8905-079fb675aaae', 'LEAD_MARK_FULLY_PAID', 'Lead', 'fc4ef609-1956-5f86-af71-f57e99181448', '"PENDING_FULL_PAYMENT_CONFIRMATION"', '"FULLY_PAID"', now() - INTERVAL '3 days'),
    ('00399ae8-dc8c-5d0c-8905-079fb675aaae', 'LEAD_REPORT_COMMISSION_PAID', 'Lead', 'fc4ef609-1956-5f86-af71-f57e99181448', '"FULLY_PAID"', '"PENDING_COMMISSION_CONFIRMATION"', now() - INTERVAL '1 days');

-- Lead 7/8: karim-reda -> COMMISSION_PAID (SUS, 2 adult/0 child/0 infant)
INSERT INTO leads (
    id, customer_id, trip_id, company_id, status, adult_count, child_count, infant_count,
    commission_per_traveler, commission_amount, cashback_amount, commission_policy, cashback_policy,
    deposit_reported_by, deposit_reported_at, deposit_confirmed_by, deposit_confirmed_at,
    full_payment_reported_by, full_payment_reported_at, full_payment_confirmed_by, full_payment_confirmed_at,
    commission_reported_by, commission_reported_at, commission_paid_by, commission_paid_at,
    cashback_paid_by, cashback_paid_at, created_at, updated_at
) VALUES (
    '42c39eff-d715-525a-a79a-b42d802a02e7', '36a30181-98ce-5388-8788-7774660c8412', '128c5fc6-d142-5421-8144-381a8cac30e2', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'COMMISSION_PAID', 2, 0, 0,
    1800.00, 3600.00, 900.00, 'PER_TRAVELER', 'COMMISSION_SHARE',
    '777e69a0-1778-5ece-8cea-080522881f82', now() - INTERVAL '5 days', '60249520-68d9-5c25-9a6c-39819b72a607', now() - INTERVAL '3 days',
    '777e69a0-1778-5ece-8cea-080522881f82', now() - INTERVAL '1 days', '60249520-68d9-5c25-9a6c-39819b72a607', now() - INTERVAL '0 days',
    '60249520-68d9-5c25-9a6c-39819b72a607', now() - INTERVAL '0 days', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '0 days',
    NULL, NULL, now() - INTERVAL '7 days', now()
);

INSERT INTO lead_status_history (lead_id, from_status, to_status, changed_by, changed_at, note) VALUES
    ('42c39eff-d715-525a-a79a-b42d802a02e7', NULL, 'INTERESTED', '777e69a0-1778-5ece-8cea-080522881f82', now() - INTERVAL '7 days', NULL),
    ('42c39eff-d715-525a-a79a-b42d802a02e7', 'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', '777e69a0-1778-5ece-8cea-080522881f82', now() - INTERVAL '5 days', 'Transferred via InstaPay.'),
    ('42c39eff-d715-525a-a79a-b42d802a02e7', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID', '60249520-68d9-5c25-9a6c-39819b72a607', now() - INTERVAL '3 days', NULL),
    ('42c39eff-d715-525a-a79a-b42d802a02e7', 'DEPOSIT_PAID', 'PENDING_FULL_PAYMENT_CONFIRMATION', '777e69a0-1778-5ece-8cea-080522881f82', now() - INTERVAL '1 days', 'Bank transfer completed.'),
    ('42c39eff-d715-525a-a79a-b42d802a02e7', 'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID', '60249520-68d9-5c25-9a6c-39819b72a607', now() - INTERVAL '0 days', NULL),
    ('42c39eff-d715-525a-a79a-b42d802a02e7', 'FULLY_PAID', 'PENDING_COMMISSION_CONFIRMATION', '60249520-68d9-5c25-9a6c-39819b72a607', now() - INTERVAL '0 days', NULL),
    ('42c39eff-d715-525a-a79a-b42d802a02e7', 'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '0 days', NULL);

INSERT INTO commissions (lead_id, company_id, amount, status, reported_by, reported_at, confirmed_by, confirmed_at) VALUES
    ('42c39eff-d715-525a-a79a-b42d802a02e7', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', 3600.00, 'CONFIRMED', '60249520-68d9-5c25-9a6c-39819b72a607', now() - INTERVAL '0 days', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '0 days');

INSERT INTO ratings (lead_id, trip_id, company_id, customer_id, stars, comment, created_at) VALUES
    ('42c39eff-d715-525a-a79a-b42d802a02e7', '128c5fc6-d142-5421-8144-381a8cac30e2', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', '36a30181-98ce-5388-8788-7774660c8412', 5, 'Excellent service, the company handled everything smoothly.', now() - INTERVAL '0 days');

INSERT INTO notifications (recipient_user_id, type, title, body, data, created_at) VALUES
    ('60249520-68d9-5c25-9a6c-39819b72a607', 'NEW_LEAD', 'New interested customer', 'A customer is interested in this trip for 2 traveler(s).', '{"leadId": "42c39eff-d715-525a-a79a-b42d802a02e7", "tripId": "128c5fc6-d142-5421-8144-381a8cac30e2"}'::jsonb, now() - INTERVAL '7 days'),
    ('60249520-68d9-5c25-9a6c-39819b72a607', 'DEPOSIT_CONFIRMATION_REQUIRED', 'Confirm a deposit', 'A customer reported paying the deposit for Sha''ban Umrah - 10 Nights. Please confirm.', '{"leadId": "42c39eff-d715-525a-a79a-b42d802a02e7", "tripId": "128c5fc6-d142-5421-8144-381a8cac30e2"}'::jsonb, now() - INTERVAL '5 days'),
    ('777e69a0-1778-5ece-8cea-080522881f82', 'DEPOSIT_CONFIRMED', 'Deposit confirmed', 'Your deposit for Sha''ban Umrah - 10 Nights has been confirmed by the company.', '{"leadId": "42c39eff-d715-525a-a79a-b42d802a02e7", "tripId": "128c5fc6-d142-5421-8144-381a8cac30e2"}'::jsonb, now() - INTERVAL '3 days'),
    ('60249520-68d9-5c25-9a6c-39819b72a607', 'FULL_PAYMENT_CONFIRMATION_REQUIRED', 'Confirm a full payment', 'A customer reported paying in full for Sha''ban Umrah - 10 Nights. Please confirm.', '{"leadId": "42c39eff-d715-525a-a79a-b42d802a02e7", "tripId": "128c5fc6-d142-5421-8144-381a8cac30e2"}'::jsonb, now() - INTERVAL '1 days'),
    ('777e69a0-1778-5ece-8cea-080522881f82', 'FULL_PAYMENT_CONFIRMED', 'Full payment confirmed', 'Your full payment for Sha''ban Umrah - 10 Nights has been confirmed by the company.', '{"leadId": "42c39eff-d715-525a-a79a-b42d802a02e7", "tripId": "128c5fc6-d142-5421-8144-381a8cac30e2"}'::jsonb, now() - INTERVAL '0 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'COMMISSION_CONFIRMATION_REQUIRED', 'Confirm a commission payment', 'A company reported paying its commission. Please confirm so cashback can be released.', '{"leadId": "42c39eff-d715-525a-a79a-b42d802a02e7", "tripId": "128c5fc6-d142-5421-8144-381a8cac30e2"}'::jsonb, now() - INTERVAL '0 days'),
    ('60249520-68d9-5c25-9a6c-39819b72a607', 'COMMISSION_PAID', 'Commission confirmed', 'Your commission payment has been confirmed by the platform.', '{"leadId": "42c39eff-d715-525a-a79a-b42d802a02e7", "tripId": "128c5fc6-d142-5421-8144-381a8cac30e2"}'::jsonb, now() - INTERVAL '0 days');

INSERT INTO analytics_events (user_id, event_type, entity_type, entity_id, created_at) VALUES
    ('777e69a0-1778-5ece-8cea-080522881f82', 'CONTACT_COMPANY', 'Trip', '128c5fc6-d142-5421-8144-381a8cac30e2', now() - INTERVAL '7 days');

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES
    ('777e69a0-1778-5ece-8cea-080522881f82', 'LEAD_REPORT_DEPOSIT', 'Lead', '42c39eff-d715-525a-a79a-b42d802a02e7', '"INTERESTED"', '"PENDING_DEPOSIT_CONFIRMATION"', now() - INTERVAL '5 days'),
    ('60249520-68d9-5c25-9a6c-39819b72a607', 'LEAD_MARK_DEPOSIT_PAID', 'Lead', '42c39eff-d715-525a-a79a-b42d802a02e7', '"PENDING_DEPOSIT_CONFIRMATION"', '"DEPOSIT_PAID"', now() - INTERVAL '3 days'),
    ('777e69a0-1778-5ece-8cea-080522881f82', 'LEAD_REPORT_FULL_PAYMENT', 'Lead', '42c39eff-d715-525a-a79a-b42d802a02e7', '"DEPOSIT_PAID"', '"PENDING_FULL_PAYMENT_CONFIRMATION"', now() - INTERVAL '1 days'),
    ('60249520-68d9-5c25-9a6c-39819b72a607', 'LEAD_MARK_FULLY_PAID', 'Lead', '42c39eff-d715-525a-a79a-b42d802a02e7', '"PENDING_FULL_PAYMENT_CONFIRMATION"', '"FULLY_PAID"', now() - INTERVAL '0 days'),
    ('60249520-68d9-5c25-9a6c-39819b72a607', 'LEAD_REPORT_COMMISSION_PAID', 'Lead', '42c39eff-d715-525a-a79a-b42d802a02e7', '"FULLY_PAID"', '"PENDING_COMMISSION_CONFIRMATION"', now() - INTERVAL '0 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'LEAD_CONFIRM_COMMISSION_PAID', 'Lead', '42c39eff-d715-525a-a79a-b42d802a02e7', '"PENDING_COMMISSION_CONFIRMATION"', '"COMMISSION_PAID"', now() - INTERVAL '0 days');

-- Lead 8/8: youssef-ibrahim -> CASHBACK_PAID (BAR, 2 adult/1 child/0 infant)
INSERT INTO leads (
    id, customer_id, trip_id, company_id, status, adult_count, child_count, infant_count,
    commission_per_traveler, commission_amount, cashback_amount, commission_policy, cashback_policy,
    deposit_reported_by, deposit_reported_at, deposit_confirmed_by, deposit_confirmed_at,
    full_payment_reported_by, full_payment_reported_at, full_payment_confirmed_by, full_payment_confirmed_at,
    commission_reported_by, commission_reported_at, commission_paid_by, commission_paid_at,
    cashback_paid_by, cashback_paid_at, created_at, updated_at
) VALUES (
    'd8b0c28c-960b-5f2e-9294-b571cad62073', '7f458b6a-69bb-5967-b8b2-594769aba39e', '3cba5827-bafe-543b-9a56-1bfa3a472797', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'CASHBACK_PAID', 2, 1, 0,
    1500.00, 3000.00, 750.00, 'PER_TRAVELER', 'COMMISSION_SHARE',
    '17dc5702-d8f6-54d6-8027-5811cdcbd8e9', now() - INTERVAL '1 days', '065dc9bf-f482-5680-83d2-91aaeef1799b', now() - INTERVAL '0 days',
    '17dc5702-d8f6-54d6-8027-5811cdcbd8e9', now() - INTERVAL '0 days', '065dc9bf-f482-5680-83d2-91aaeef1799b', now() - INTERVAL '0 days',
    '065dc9bf-f482-5680-83d2-91aaeef1799b', now() - INTERVAL '0 days', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '0 days',
    (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '0 days', now() - INTERVAL '3 days', now()
);

INSERT INTO lead_status_history (lead_id, from_status, to_status, changed_by, changed_at, note) VALUES
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', NULL, 'INTERESTED', '17dc5702-d8f6-54d6-8027-5811cdcbd8e9', now() - INTERVAL '3 days', NULL),
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', 'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', '17dc5702-d8f6-54d6-8027-5811cdcbd8e9', now() - INTERVAL '1 days', 'Transferred via InstaPay.'),
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID', '065dc9bf-f482-5680-83d2-91aaeef1799b', now() - INTERVAL '0 days', NULL),
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', 'DEPOSIT_PAID', 'PENDING_FULL_PAYMENT_CONFIRMATION', '17dc5702-d8f6-54d6-8027-5811cdcbd8e9', now() - INTERVAL '0 days', 'Bank transfer completed.'),
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', 'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID', '065dc9bf-f482-5680-83d2-91aaeef1799b', now() - INTERVAL '0 days', NULL),
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', 'FULLY_PAID', 'PENDING_COMMISSION_CONFIRMATION', '065dc9bf-f482-5680-83d2-91aaeef1799b', now() - INTERVAL '0 days', NULL),
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', 'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '0 days', NULL),
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', 'COMMISSION_PAID', 'CASHBACK_PAID', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '0 days', NULL);

INSERT INTO commissions (lead_id, company_id, amount, status, reported_by, reported_at, confirmed_by, confirmed_at) VALUES
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 3000.00, 'CONFIRMED', '065dc9bf-f482-5680-83d2-91aaeef1799b', now() - INTERVAL '0 days', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '0 days');

INSERT INTO cashback_transactions (lead_id, customer_id, wallet_type, wallet_number, amount, status, sent_at, paid_by) VALUES
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', '7f458b6a-69bb-5967-b8b2-594769aba39e', 'INSTA_PAY', '+201234567803', 750.00, 'SENT', now() - INTERVAL '0 days', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'));

INSERT INTO ratings (lead_id, trip_id, company_id, customer_id, stars, comment, created_at) VALUES
    ('d8b0c28c-960b-5f2e-9294-b571cad62073', '3cba5827-bafe-543b-9a56-1bfa3a472797', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', '7f458b6a-69bb-5967-b8b2-594769aba39e', 5, 'Excellent service, the company handled everything smoothly.', now() - INTERVAL '0 days');

INSERT INTO notifications (recipient_user_id, type, title, body, data, created_at) VALUES
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'NEW_LEAD', 'New interested customer', 'A customer is interested in this trip for 2 traveler(s).', '{"leadId": "d8b0c28c-960b-5f2e-9294-b571cad62073", "tripId": "3cba5827-bafe-543b-9a56-1bfa3a472797"}'::jsonb, now() - INTERVAL '3 days'),
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'DEPOSIT_CONFIRMATION_REQUIRED', 'Confirm a deposit', 'A customer reported paying the deposit for Ramadan Umrah - First Ten - 14 Nights. Please confirm.', '{"leadId": "d8b0c28c-960b-5f2e-9294-b571cad62073", "tripId": "3cba5827-bafe-543b-9a56-1bfa3a472797"}'::jsonb, now() - INTERVAL '1 days'),
    ('17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'DEPOSIT_CONFIRMED', 'Deposit confirmed', 'Your deposit for Ramadan Umrah - First Ten - 14 Nights has been confirmed by the company.', '{"leadId": "d8b0c28c-960b-5f2e-9294-b571cad62073", "tripId": "3cba5827-bafe-543b-9a56-1bfa3a472797"}'::jsonb, now() - INTERVAL '0 days'),
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'FULL_PAYMENT_CONFIRMATION_REQUIRED', 'Confirm a full payment', 'A customer reported paying in full for Ramadan Umrah - First Ten - 14 Nights. Please confirm.', '{"leadId": "d8b0c28c-960b-5f2e-9294-b571cad62073", "tripId": "3cba5827-bafe-543b-9a56-1bfa3a472797"}'::jsonb, now() - INTERVAL '0 days'),
    ('17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'FULL_PAYMENT_CONFIRMED', 'Full payment confirmed', 'Your full payment for Ramadan Umrah - First Ten - 14 Nights has been confirmed by the company.', '{"leadId": "d8b0c28c-960b-5f2e-9294-b571cad62073", "tripId": "3cba5827-bafe-543b-9a56-1bfa3a472797"}'::jsonb, now() - INTERVAL '0 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'COMMISSION_CONFIRMATION_REQUIRED', 'Confirm a commission payment', 'A company reported paying its commission. Please confirm so cashback can be released.', '{"leadId": "d8b0c28c-960b-5f2e-9294-b571cad62073", "tripId": "3cba5827-bafe-543b-9a56-1bfa3a472797"}'::jsonb, now() - INTERVAL '0 days'),
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'COMMISSION_PAID', 'Commission confirmed', 'Your commission payment has been confirmed by the platform.', '{"leadId": "d8b0c28c-960b-5f2e-9294-b571cad62073", "tripId": "3cba5827-bafe-543b-9a56-1bfa3a472797"}'::jsonb, now() - INTERVAL '0 days'),
    ('17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'CASHBACK_PAID', 'Cashback sent', 'Your cashback has been sent to your INSTA_PAY wallet.', '{"leadId": "d8b0c28c-960b-5f2e-9294-b571cad62073", "tripId": "3cba5827-bafe-543b-9a56-1bfa3a472797"}'::jsonb, now() - INTERVAL '0 days');

INSERT INTO analytics_events (user_id, event_type, entity_type, entity_id, created_at) VALUES
    ('17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'CONTACT_COMPANY', 'Trip', '3cba5827-bafe-543b-9a56-1bfa3a472797', now() - INTERVAL '3 days');

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES
    ('17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'LEAD_REPORT_DEPOSIT', 'Lead', 'd8b0c28c-960b-5f2e-9294-b571cad62073', '"INTERESTED"', '"PENDING_DEPOSIT_CONFIRMATION"', now() - INTERVAL '1 days'),
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'LEAD_MARK_DEPOSIT_PAID', 'Lead', 'd8b0c28c-960b-5f2e-9294-b571cad62073', '"PENDING_DEPOSIT_CONFIRMATION"', '"DEPOSIT_PAID"', now() - INTERVAL '0 days'),
    ('17dc5702-d8f6-54d6-8027-5811cdcbd8e9', 'LEAD_REPORT_FULL_PAYMENT', 'Lead', 'd8b0c28c-960b-5f2e-9294-b571cad62073', '"DEPOSIT_PAID"', '"PENDING_FULL_PAYMENT_CONFIRMATION"', now() - INTERVAL '0 days'),
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'LEAD_MARK_FULLY_PAID', 'Lead', 'd8b0c28c-960b-5f2e-9294-b571cad62073', '"PENDING_FULL_PAYMENT_CONFIRMATION"', '"FULLY_PAID"', now() - INTERVAL '0 days'),
    ('065dc9bf-f482-5680-83d2-91aaeef1799b', 'LEAD_REPORT_COMMISSION_PAID', 'Lead', 'd8b0c28c-960b-5f2e-9294-b571cad62073', '"FULLY_PAID"', '"PENDING_COMMISSION_CONFIRMATION"', now() - INTERVAL '0 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'LEAD_CONFIRM_COMMISSION_PAID', 'Lead', 'd8b0c28c-960b-5f2e-9294-b571cad62073', '"PENDING_COMMISSION_CONFIRMATION"', '"COMMISSION_PAID"', now() - INTERVAL '0 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'LEAD_PAY_CASHBACK', 'Lead', 'd8b0c28c-960b-5f2e-9294-b571cad62073', '"COMMISSION_PAID"', '"CASHBACK_PAID"', now() - INTERVAL '0 days');

-- =============================================================================
-- Company documents
-- =============================================================================

INSERT INTO company_documents (company_id, doc_type, file_url, status, reviewed_by, reviewed_at) VALUES
    ('d05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', 'TOURISM_LICENSE', '/uploads/documents/seed-nour-al-haram-license.pdf', 'APPROVED', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '150 days');
INSERT INTO company_documents (company_id, doc_type, file_url, status, reviewed_by, reviewed_at) VALUES
    ('d12a303e-1fdf-533c-adf7-dd7537c8f4f3', 'TOURISM_LICENSE', '/uploads/documents/seed-sakina-license.pdf', 'APPROVED', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '150 days');
INSERT INTO company_documents (company_id, doc_type, file_url, status, reviewed_by, reviewed_at) VALUES
    ('8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', 'TOURISM_LICENSE', '/uploads/documents/seed-bayt-al-rahma-license.pdf', 'APPROVED', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '150 days');
INSERT INTO company_documents (company_id, doc_type, file_url, status, reviewed_by, reviewed_at) VALUES
    ('df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', 'TOURISM_LICENSE', '/uploads/documents/seed-darb-al-safa-license.pdf', 'APPROVED', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '150 days');
INSERT INTO company_documents (company_id, doc_type, file_url, status, reviewed_by, reviewed_at) VALUES
    ('d73d9c94-0da0-5540-a193-46fe2658392a', 'TOURISM_LICENSE', '/uploads/documents/seed-manasik-al-anwar-license.pdf', 'APPROVED', (SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), now() - INTERVAL '150 days');
INSERT INTO company_documents (company_id, doc_type, file_url, status) VALUES
    ('d73d9c94-0da0-5540-a193-46fe2658392a', 'COMMERCIAL_REGISTER', '/uploads/documents/seed-manasik-al-anwar-register.pdf', 'PENDING');

-- =============================================================================
-- Audit logs — company approvals and a commission-rate change
-- =============================================================================

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'COMPANY_APPROVED', 'CompanyProfile', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', '"PENDING"', '"APPROVED"', now() - INTERVAL '160 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'COMPANY_APPROVED', 'CompanyProfile', 'd12a303e-1fdf-533c-adf7-dd7537c8f4f3', '"PENDING"', '"APPROVED"', now() - INTERVAL '161 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'COMPANY_APPROVED', 'CompanyProfile', '8d4fa096-7eda-58ba-ad4a-ef690d8ab6dc', '"PENDING"', '"APPROVED"', now() - INTERVAL '162 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'COMPANY_APPROVED', 'CompanyProfile', 'df9a8a62-e2bb-5a27-a7ab-629cf5006ba2', '"PENDING"', '"APPROVED"', now() - INTERVAL '163 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'COMPANY_APPROVED', 'CompanyProfile', 'd73d9c94-0da0-5540-a193-46fe2658392a', '"PENDING"', '"APPROVED"', now() - INTERVAL '164 days'),
    ((SELECT id FROM users WHERE email = 'admin@umrahscanner.dev'), 'COMPANY_COMMISSION_UPDATED', 'CompanyProfile', 'd05db3d1-cf0f-53a0-a1b5-3e7c57ac38bd', '2000.00', '2500.00', now() - INTERVAL '90 days');


COMMIT;

-- Verify:
--   SELECT c.company_name, c.commission_per_traveler, count(t.id) AS trips
--   FROM company_profiles c JOIN trips t ON t.company_id = c.id
--   WHERE c.license_number LIKE 'TRV-%' GROUP BY 1, 2 ORDER BY 1;
--   SELECT status, count(*) FROM leads WHERE customer_id IN (SELECT id FROM customer_profiles
--   WHERE full_name IN ('Ahmed Fathy','Mona Said','Youssef Ibrahim','Salma Adel','Karim Reda')) GROUP BY 1;
--   SELECT type, count(*) FROM notifications GROUP BY 1 ORDER BY 1;
