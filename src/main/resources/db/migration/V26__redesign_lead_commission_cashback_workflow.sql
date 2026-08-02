-- Redesign of the Lead / Commission / Cashback / Traveler workflow.
--
-- Three things change together and therefore must migrate together:
--   1. Companies gain an admin-configured commission rate.
--   2. Leads carry traveler counts and a permanent commission/cashback snapshot.
--   3. The lead lifecycle is replaced with the report/confirm state machine.

-- ---------------------------------------------------------------------------
-- 1. Company commission
-- ---------------------------------------------------------------------------

ALTER TABLE company_profiles
    ADD COLUMN commission_per_traveler NUMERIC(10,2) NOT NULL DEFAULT 0;

ALTER TABLE company_profiles
    ADD CONSTRAINT chk_company_profiles_commission_per_traveler CHECK (commission_per_traveler >= 0);

COMMENT ON COLUMN company_profiles.commission_per_traveler IS
    'EGP the company pays the platform per billable traveler. Admin-writable only. Leads snapshot it at creation, so changes affect future leads only.';

-- ---------------------------------------------------------------------------
-- 2. Traveler counts and the per-lead pricing snapshot
-- ---------------------------------------------------------------------------

ALTER TABLE leads
    ADD COLUMN adult_count             INTEGER      NOT NULL DEFAULT 1,
    ADD COLUMN child_count             INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN infant_count            INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN commission_per_traveler NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN commission_amount       NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN cashback_amount         NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN commission_policy       VARCHAR(50)  NOT NULL DEFAULT 'PER_TRAVELER',
    ADD COLUMN cashback_policy         VARCHAR(50)  NOT NULL DEFAULT 'COMMISSION_SHARE';

ALTER TABLE leads
    ADD CONSTRAINT chk_leads_adult_count             CHECK (adult_count >= 1),
    ADD CONSTRAINT chk_leads_child_count             CHECK (child_count >= 0),
    ADD CONSTRAINT chk_leads_infant_count            CHECK (infant_count >= 0),
    ADD CONSTRAINT chk_leads_commission_per_traveler CHECK (commission_per_traveler >= 0),
    ADD CONSTRAINT chk_leads_commission_amount       CHECK (commission_amount >= 0),
    ADD CONSTRAINT chk_leads_cashback_amount         CHECK (cashback_amount >= 0);

COMMENT ON COLUMN leads.adult_count IS 'Billable travelers. Commission is charged per adult; children and infants ride along free.';
COMMENT ON COLUMN leads.commission_amount IS 'Fixed at lead creation (commission_per_traveler x adult_count) and never recalculated.';
COMMENT ON COLUMN leads.cashback_amount IS 'Fixed at lead creation by the winning cashback policy and never recalculated.';
COMMENT ON COLUMN leads.cashback_policy IS 'Which rule produced cashback_amount, so a past payout stays explainable after the rules change.';

-- Pre-existing leads were created before commission existed. They keep zeroed amounts on purpose:
-- back-filling them from today's rate would be exactly the retroactive repricing the rules forbid.

-- Per-step audit trail.
ALTER TABLE leads
    ADD COLUMN deposit_reported_by       UUID REFERENCES users(id),
    ADD COLUMN deposit_reported_at       TIMESTAMPTZ,
    ADD COLUMN deposit_confirmed_by      UUID REFERENCES users(id),
    ADD COLUMN deposit_confirmed_at      TIMESTAMPTZ,
    ADD COLUMN full_payment_reported_by  UUID REFERENCES users(id),
    ADD COLUMN full_payment_reported_at  TIMESTAMPTZ,
    ADD COLUMN full_payment_confirmed_by UUID REFERENCES users(id),
    ADD COLUMN full_payment_confirmed_at TIMESTAMPTZ,
    ADD COLUMN commission_reported_by    UUID REFERENCES users(id),
    ADD COLUMN commission_reported_at    TIMESTAMPTZ,
    ADD COLUMN commission_paid_by        UUID REFERENCES users(id),
    ADD COLUMN commission_paid_at        TIMESTAMPTZ,
    ADD COLUMN cashback_paid_by          UUID REFERENCES users(id),
    ADD COLUMN cashback_paid_at          TIMESTAMPTZ;

-- ---------------------------------------------------------------------------
-- 3. The new lifecycle
-- ---------------------------------------------------------------------------

ALTER TABLE leads DROP CONSTRAINT chk_leads_status;
ALTER TABLE leads ALTER COLUMN status TYPE VARCHAR(40);

ALTER TABLE lead_status_history DROP CONSTRAINT chk_lead_status_history_from;
ALTER TABLE lead_status_history DROP CONSTRAINT chk_lead_status_history_to;
ALTER TABLE lead_status_history ALTER COLUMN from_status TYPE VARCHAR(40);
ALTER TABLE lead_status_history ALTER COLUMN to_status   TYPE VARCHAR(40);

-- Old -> new mapping. The three pre-payment states all collapse to INTERESTED because none of them
-- represented a reported payment; COMPANY_MARKED_PAID and ADMIN_REVIEW both meant "the money is in,
-- the platform has not been settled with yet", which is exactly FULLY_PAID.
UPDATE leads SET status = CASE status
    WHEN 'INTERESTED'          THEN 'INTERESTED'
    WHEN 'COMPANY_CONTACTED'   THEN 'INTERESTED'
    WHEN 'CUSTOMER_CONFIRMED'  THEN 'INTERESTED'
    WHEN 'PAYMENT_PENDING'     THEN 'PENDING_DEPOSIT_CONFIRMATION'
    WHEN 'COMPANY_MARKED_PAID' THEN 'FULLY_PAID'
    WHEN 'ADMIN_REVIEW'        THEN 'FULLY_PAID'
    WHEN 'COMMISSION_RELEASED' THEN 'COMMISSION_PAID'
    WHEN 'CASHBACK_SENT'       THEN 'CASHBACK_PAID'
    ELSE status
END;

UPDATE lead_status_history SET from_status = CASE from_status
    WHEN 'COMPANY_CONTACTED'   THEN 'INTERESTED'
    WHEN 'CUSTOMER_CONFIRMED'  THEN 'INTERESTED'
    WHEN 'PAYMENT_PENDING'     THEN 'PENDING_DEPOSIT_CONFIRMATION'
    WHEN 'COMPANY_MARKED_PAID' THEN 'FULLY_PAID'
    WHEN 'ADMIN_REVIEW'        THEN 'FULLY_PAID'
    WHEN 'COMMISSION_RELEASED' THEN 'COMMISSION_PAID'
    WHEN 'CASHBACK_SENT'       THEN 'CASHBACK_PAID'
    ELSE from_status
END;

UPDATE lead_status_history SET to_status = CASE to_status
    WHEN 'COMPANY_CONTACTED'   THEN 'INTERESTED'
    WHEN 'CUSTOMER_CONFIRMED'  THEN 'INTERESTED'
    WHEN 'PAYMENT_PENDING'     THEN 'PENDING_DEPOSIT_CONFIRMATION'
    WHEN 'COMPANY_MARKED_PAID' THEN 'FULLY_PAID'
    WHEN 'ADMIN_REVIEW'        THEN 'FULLY_PAID'
    WHEN 'COMMISSION_RELEASED' THEN 'COMMISSION_PAID'
    WHEN 'CASHBACK_SENT'       THEN 'CASHBACK_PAID'
    ELSE to_status
END;

ALTER TABLE leads ADD CONSTRAINT chk_leads_status CHECK (status IN (
    'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID',
    'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID',
    'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', 'CASHBACK_PAID'
));

ALTER TABLE lead_status_history ADD CONSTRAINT chk_lead_status_history_from CHECK (from_status IS NULL OR from_status IN (
    'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID',
    'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID',
    'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', 'CASHBACK_PAID'
));

ALTER TABLE lead_status_history ADD CONSTRAINT chk_lead_status_history_to CHECK (to_status IN (
    'INTERESTED', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID',
    'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID',
    'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', 'CASHBACK_PAID'
));

-- ---------------------------------------------------------------------------
-- 4. Commissions: direction reversed
-- ---------------------------------------------------------------------------
-- Previously the platform "released" a commission to the company. Now the company owes the platform
-- and reports payment, which an admin confirms.

ALTER TABLE commissions RENAME COLUMN released_by TO confirmed_by;
ALTER TABLE commissions RENAME COLUMN released_at TO confirmed_at;

ALTER TABLE commissions
    ADD COLUMN reported_by UUID REFERENCES users(id),
    ADD COLUMN reported_at TIMESTAMPTZ;

ALTER TABLE commissions DROP CONSTRAINT chk_commissions_status;
UPDATE commissions SET status = 'CONFIRMED' WHERE status = 'RELEASED';
ALTER TABLE commissions ADD CONSTRAINT chk_commissions_status CHECK (status IN ('PENDING', 'REPORTED', 'CONFIRMED'));

COMMENT ON TABLE commissions IS
    'What a company owes the platform for one lead. Amount is copied from the lead''s snapshot. Company reports payment, admin confirms.';

-- ---------------------------------------------------------------------------
-- 5. Cashback
-- ---------------------------------------------------------------------------

ALTER TABLE cashback_transactions ADD COLUMN paid_by UUID REFERENCES users(id);

COMMENT ON TABLE cashback_transactions IS
    'Cashback payout to the customer''s wallet. One row per lead, written once the company''s commission has been confirmed.';

COMMENT ON TABLE ratings IS 'One review per lead, allowed from DEPOSIT_PAID onwards, immutable once posted.';
