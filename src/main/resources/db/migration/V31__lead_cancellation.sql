-- Cancellation, and the "one preserved journey at a time" rule that depends on it.
--
-- A customer may hold exactly one live lead. To take up a different trip they must first cancel the
-- one they are holding — so cancellation has to exist before the rule can be enforced, and both
-- arrive together here.

-- ---------------------------------------------------------------------------
-- 1. CANCELLED joins the lead lifecycle
-- ---------------------------------------------------------------------------

ALTER TABLE leads DROP CONSTRAINT chk_leads_status;
ALTER TABLE leads ADD CONSTRAINT chk_leads_status CHECK (status IN (
    'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID',
    'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID',
    'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', 'CASHBACK_PAID',
    'CANCELLED'
));

ALTER TABLE lead_status_history DROP CONSTRAINT chk_lead_status_history_from;
ALTER TABLE lead_status_history ADD CONSTRAINT chk_lead_status_history_from CHECK (from_status IS NULL OR from_status IN (
    'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID',
    'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID',
    'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', 'CASHBACK_PAID',
    'CANCELLED'
));

ALTER TABLE lead_status_history DROP CONSTRAINT chk_lead_status_history_to;
ALTER TABLE lead_status_history ADD CONSTRAINT chk_lead_status_history_to CHECK (to_status IN (
    'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID',
    'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID',
    'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', 'CASHBACK_PAID',
    'CANCELLED'
));

ALTER TABLE leads
    ADD COLUMN cancelled_by UUID REFERENCES users(id),
    ADD COLUMN cancelled_at TIMESTAMPTZ;

COMMENT ON COLUMN leads.cancelled_at IS
    'When the customer withdrew. The reason lives on the matching lead_status_history row.';

-- ---------------------------------------------------------------------------
-- 2. A cancelled lead owes the platform nothing
-- ---------------------------------------------------------------------------
-- The row is kept rather than deleted: a commission that really was reported or confirmed stays on
-- the record, it is simply no longer collectable.

ALTER TABLE commissions DROP CONSTRAINT chk_commissions_status;
ALTER TABLE commissions ADD CONSTRAINT chk_commissions_status
    CHECK (status IN ('PENDING', 'REPORTED', 'CONFIRMED', 'CANCELLED'));

-- ---------------------------------------------------------------------------
-- 3. Back-fill: collapse pre-existing customers down to one live lead
-- ---------------------------------------------------------------------------
-- Nothing stopped a customer holding several leads until now, so the unique index in step 4 would
-- fail on existing data. Their most recently created lead is kept as the preserved one and the rest
-- are cancelled, each with a history row saying why — this is a data change, not just a schema one.

WITH superseded AS (
    SELECT id, customer_id
    FROM (
        SELECT id,
               customer_id,
               ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY created_at DESC, id DESC) AS rn
        FROM leads
        WHERE status NOT IN ('CANCELLED', 'CASHBACK_PAID')
    ) ranked
    WHERE rn > 1
),
history AS (
    INSERT INTO lead_status_history (lead_id, from_status, to_status, changed_by, note)
    SELECT l.id, l.status, 'CANCELLED', NULL,
           'Cancelled automatically: a customer may hold only one preserved journey, and a more recent one exists.'
    FROM leads l
    JOIN superseded s ON s.id = l.id
    RETURNING lead_id
)
UPDATE leads
SET status = 'CANCELLED',
    cancelled_at = now(),
    updated_at = now()
WHERE id IN (SELECT lead_id FROM history);

-- ---------------------------------------------------------------------------
-- 4. The two uniqueness rules, both partial on "not cancelled"
-- ---------------------------------------------------------------------------
-- Partial rather than plain, because cancelling has to free the slot up again: a customer who
-- withdraws from a trip must be able to preserve it later, and that second attempt is a NEW lead
-- row priced at the rates of the day, leaving the cancelled one intact as a permanent record.
--
-- JPA cannot express a partial index, so these live here and nowhere else — see the comment on the
-- Lead entity, and LeadRepository.findActiveByCustomerId whose status list must match index 2.

DROP INDEX uq_leads_customer_trip;

CREATE UNIQUE INDEX uq_leads_customer_trip_live ON leads(customer_id, trip_id)
    WHERE status <> 'CANCELLED';

CREATE UNIQUE INDEX uq_leads_customer_active ON leads(customer_id)
    WHERE status NOT IN ('CANCELLED', 'CASHBACK_PAID');

COMMENT ON INDEX uq_leads_customer_active IS
    'One preserved journey per customer. A completed (CASHBACK_PAID) or cancelled lead frees the slot.';
