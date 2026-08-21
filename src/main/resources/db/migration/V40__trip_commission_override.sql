ALTER TABLE trips ADD COLUMN commission_per_traveler NUMERIC(10,2);

COMMENT ON COLUMN trips.commission_per_traveler IS
    'Per-traveler commission override for this trip, in EGP. NULL means "use the company''s current rate" (company_profiles.commission_per_traveler). Leads still snapshot the resolved rate at creation time regardless of later edits to either this or the company''s rate.';
