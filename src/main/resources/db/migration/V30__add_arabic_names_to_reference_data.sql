-- Arabic display names for the four fixed reference tables (cities, countries, airports,
-- currencies) so the app can switch language instantly, client-side, with no extra request:
-- every reference response now carries both the English and Arabic name in the same payload.
--
-- Columns are added nullable, backfilled by exact match against the English value already in the
-- table, then set NOT NULL. That ordering is deliberate: if a future seeded row is ever added to
-- these tables without its Arabic translation, the NOT NULL constraint rejects the migration
-- outright instead of silently shipping a blank name.

-- ---------------------------------------------------------------------------
-- Cities (Egypt governorates)
-- ---------------------------------------------------------------------------

ALTER TABLE cities ADD COLUMN name_ar VARCHAR(100);

UPDATE cities SET name_ar = v.name_ar FROM (VALUES
    ('Cairo',           'القاهرة'),
    ('Giza',            'الجيزة'),
    ('Alexandria',      'الإسكندرية'),
    ('Qalyubia',        'القليوبية'),
    ('Port Said',       'بورسعيد'),
    ('Suez',            'السويس'),
    ('Dakahlia',        'الدقهلية'),
    ('Sharqia',         'الشرقية'),
    ('Gharbia',         'الغربية'),
    ('Monufia',         'المنوفية'),
    ('Beheira',         'البحيرة'),
    ('Ismailia',        'الإسماعيلية'),
    ('Fayoum',          'الفيوم'),
    ('Beni Suef',       'بني سويف'),
    ('Minya',           'المنيا'),
    ('Asyut',           'أسيوط'),
    ('Sohag',           'سوهاج'),
    ('Qena',            'قنا'),
    ('Aswan',           'أسوان'),
    ('Luxor',           'الأقصر'),
    ('Red Sea',         'البحر الأحمر'),
    ('New Valley',      'الوادي الجديد'),
    ('Matrouh',         'مطروح'),
    ('North Sinai',     'شمال سيناء'),
    ('South Sinai',     'جنوب سيناء'),
    ('Kafr El Sheikh',  'كفر الشيخ'),
    ('Damietta',        'دمياط')
) AS v(name, name_ar)
WHERE cities.name = v.name;

ALTER TABLE cities ALTER COLUMN name_ar SET NOT NULL;

-- ---------------------------------------------------------------------------
-- Countries
-- ---------------------------------------------------------------------------

ALTER TABLE countries ADD COLUMN name_ar VARCHAR(100);

UPDATE countries SET name_ar = 'مصر'      WHERE iso2 = 'EG';
UPDATE countries SET name_ar = 'السعودية' WHERE iso2 = 'SA';

ALTER TABLE countries ALTER COLUMN name_ar SET NOT NULL;

-- ---------------------------------------------------------------------------
-- Airports — both the airport's own name and the city it serves are localized,
-- since the city field is what a traveller actually picks the airport by.
-- ---------------------------------------------------------------------------

ALTER TABLE airports
    ADD COLUMN name_ar VARCHAR(150),
    ADD COLUMN city_ar VARCHAR(100);

UPDATE airports SET name_ar = v.name_ar, city_ar = v.city_ar FROM (VALUES
    ('CAI', 'مطار القاهرة الدولي',                       'القاهرة'),
    ('HBE', 'مطار برج العرب الدولي',                     'برج العرب'),
    ('ATZ', 'مطار أسيوط الدولي',                         'أسيوط'),
    ('LXR', 'مطار الأقصر الدولي',                        'الأقصر'),
    ('JED', 'مطار الملك عبدالعزيز الدولي',                'جدة'),
    ('MED', 'مطار الأمير محمد بن عبدالعزيز الدولي',       'المدينة المنورة')
) AS v(iata_code, name_ar, city_ar)
WHERE airports.iata_code = v.iata_code;

ALTER TABLE airports
    ALTER COLUMN name_ar SET NOT NULL,
    ALTER COLUMN city_ar SET NOT NULL;

-- ---------------------------------------------------------------------------
-- Currencies — code (EGP/SAR/USD) and symbol stay as-is; they're already
-- language-neutral. Only the spelled-out name is localized.
-- ---------------------------------------------------------------------------

ALTER TABLE currencies ADD COLUMN name_ar VARCHAR(60);

UPDATE currencies SET name_ar = 'جنيه مصري'   WHERE code = 'EGP';
UPDATE currencies SET name_ar = 'ريال سعودي'  WHERE code = 'SAR';
UPDATE currencies SET name_ar = 'دولار أمريكي' WHERE code = 'USD';

ALTER TABLE currencies ALTER COLUMN name_ar SET NOT NULL;
