-- New lead state: an admin sent the customer the trip details over WhatsApp. Sits between
-- INTERESTED and PENDING_DEPOSIT_CONFIRMATION/DEPOSIT_PAID on the ladder.

ALTER TABLE leads DROP CONSTRAINT chk_leads_status;
ALTER TABLE leads ADD CONSTRAINT chk_leads_status CHECK (status IN (
    'INTERESTED', 'CONTACTED', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID',
    'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID',
    'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', 'CASHBACK_PAID',
    'CANCELLED'
));

ALTER TABLE lead_status_history DROP CONSTRAINT chk_lead_status_history_from;
ALTER TABLE lead_status_history ADD CONSTRAINT chk_lead_status_history_from CHECK (from_status IS NULL OR from_status IN (
    'INTERESTED', 'CONTACTED', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID',
    'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID',
    'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', 'CASHBACK_PAID',
    'CANCELLED'
));

ALTER TABLE lead_status_history DROP CONSTRAINT chk_lead_status_history_to;
ALTER TABLE lead_status_history ADD CONSTRAINT chk_lead_status_history_to CHECK (to_status IN (
    'INTERESTED', 'CONTACTED', 'PENDING_DEPOSIT_CONFIRMATION', 'DEPOSIT_PAID',
    'PENDING_FULL_PAYMENT_CONFIRMATION', 'FULLY_PAID',
    'PENDING_COMMISSION_CONFIRMATION', 'COMMISSION_PAID', 'CASHBACK_PAID',
    'CANCELLED'
));

ALTER TABLE leads
    ADD COLUMN contacted_by UUID REFERENCES users(id),
    ADD COLUMN contacted_at TIMESTAMPTZ;

COMMENT ON COLUMN leads.contacted_at IS
    'When an admin sent the customer the trip details over WhatsApp.';
