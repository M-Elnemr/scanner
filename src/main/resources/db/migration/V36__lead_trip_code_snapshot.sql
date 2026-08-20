-- Companion to V35's trip_title snapshot: the admin console's free-text search matches trip code
-- as well as trip title, and that predicate must not need to join through the (possibly
-- soft-deleted, @SQLRestriction-filtered) trips table either.

ALTER TABLE leads ADD COLUMN trip_code VARCHAR(50);

UPDATE leads l
SET trip_code = t.trip_code
FROM trips t
WHERE t.id = l.trip_id;

ALTER TABLE leads ALTER COLUMN trip_code SET NOT NULL;

COMMENT ON COLUMN leads.trip_code IS
    'Snapshot of the trip code at lead creation time. Stays correct even if the trip is later renamed or soft-deleted.';
