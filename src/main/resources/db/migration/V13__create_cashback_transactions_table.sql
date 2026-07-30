CREATE TABLE cashback_transactions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id        UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    customer_id    UUID NOT NULL REFERENCES customer_profiles(id),
    wallet_type    VARCHAR(20) NOT NULL,
    wallet_number  VARCHAR(30) NOT NULL,
    amount         NUMERIC(10,2) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_cashback_wallet_type CHECK (wallet_type IN ('VODAFONE_CASH', 'ETISALAT_CASH')),
    CONSTRAINT chk_cashback_status      CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT chk_cashback_amount      CHECK (amount >= 0)
);

COMMENT ON TABLE cashback_transactions IS 'Cashback payout to the customer''s mobile wallet. One row per lead, created when commission is released.';

CREATE UNIQUE INDEX uq_cashback_transactions_lead_id ON cashback_transactions(lead_id);
CREATE INDEX idx_cashback_transactions_customer_id_status ON cashback_transactions(customer_id, status);
