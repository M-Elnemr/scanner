-- Same reasoning as V35/V36's trip snapshots, for the company side: CompanyProfile carries the same
-- @SQLRestriction("deleted_at is null") as Trip, and a company can be deleted once every lead it
-- ever held has settled (DeleteCompanyUseCase only blocks deletion while *active* leads exist). A
-- lead's own record of which company it booked with must not depend on that company still existing.

ALTER TABLE leads ADD COLUMN company_name VARCHAR(255);

UPDATE leads l
SET company_name = c.company_name
FROM company_profiles c
WHERE c.id = l.company_id;

ALTER TABLE leads ALTER COLUMN company_name SET NOT NULL;

COMMENT ON COLUMN leads.company_name IS
    'Snapshot of the company name at lead creation time. Stays correct even if the company is later renamed or soft-deleted.';
