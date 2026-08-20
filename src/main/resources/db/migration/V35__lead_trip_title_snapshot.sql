-- A lead's trip can be renamed or soft-deleted after the lead is created. Deletion in particular
-- used to make the lead itself disappear from every list and detail view: Trip carries
-- @SQLRestriction("deleted_at is null"), and Lead.trip was a non-optional association, so Hibernate
-- fetch-joined it with an INNER JOIN that silently dropped the whole lead row once its trip was
-- soft-deleted. Lead.trip is now optional (LEFT JOIN), and this column gives every lead a permanent,
-- independent record of the trip title it was created against — the same reasoning as the
-- commission/cashback snapshot columns below it.

ALTER TABLE leads ADD COLUMN trip_title VARCHAR(255);

UPDATE leads l
SET trip_title = t.title
FROM trips t
WHERE t.id = l.trip_id;

ALTER TABLE leads ALTER COLUMN trip_title SET NOT NULL;

COMMENT ON COLUMN leads.trip_title IS
    'Snapshot of the trip title at lead creation time. Stays correct even if the trip is later renamed or soft-deleted.';
