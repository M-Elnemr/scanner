CREATE TABLE lead_status_history (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id      UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    from_status  VARCHAR(30),
    to_status    VARCHAR(30) NOT NULL,
    changed_by   UUID REFERENCES users(id),
    changed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    note         VARCHAR(500),

    CONSTRAINT chk_lead_status_history_from CHECK (from_status IS NULL OR from_status IN (
        'INTERESTED', 'COMPANY_CONTACTED', 'CUSTOMER_CONFIRMED', 'PAYMENT_PENDING',
        'COMPANY_MARKED_PAID', 'ADMIN_REVIEW', 'COMMISSION_RELEASED', 'CASHBACK_SENT'
    )),
    CONSTRAINT chk_lead_status_history_to CHECK (to_status IN (
        'INTERESTED', 'COMPANY_CONTACTED', 'CUSTOMER_CONFIRMED', 'PAYMENT_PENDING',
        'COMPANY_MARKED_PAID', 'ADMIN_REVIEW', 'COMMISSION_RELEASED', 'CASHBACK_SENT'
    ))
);

COMMENT ON TABLE lead_status_history IS 'Append-only transition log — the authoritative audit trail for a lead''s lifecycle.';

CREATE INDEX idx_lead_status_history_lead_id ON lead_status_history(lead_id, changed_at);
