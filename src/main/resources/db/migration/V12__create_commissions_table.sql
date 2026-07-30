CREATE TABLE commissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id       UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    company_id    UUID NOT NULL REFERENCES company_profiles(id),
    amount        NUMERIC(10,2) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    released_by   UUID REFERENCES users(id),
    released_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_commissions_status CHECK (status IN ('PENDING', 'RELEASED')),
    CONSTRAINT chk_commissions_amount CHECK (amount >= 0)
);

COMMENT ON TABLE commissions IS 'Amount owed to a company for a closed lead. One row per lead, created at ADMIN_REVIEW, released by Admin.';

CREATE UNIQUE INDEX uq_commissions_lead_id ON commissions(lead_id);
CREATE INDEX idx_commissions_company_id_status ON commissions(company_id, status);
