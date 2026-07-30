CREATE TABLE leads (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id          UUID NOT NULL REFERENCES customer_profiles(id),
    trip_id              UUID NOT NULL REFERENCES trips(id),
    company_id           UUID NOT NULL REFERENCES company_profiles(id),
    status               VARCHAR(30) NOT NULL DEFAULT 'INTERESTED',
    confirmed_room_type  VARCHAR(20),
    confirmed_price      NUMERIC(10,2),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_leads_status CHECK (status IN (
        'INTERESTED', 'COMPANY_CONTACTED', 'CUSTOMER_CONFIRMED', 'PAYMENT_PENDING',
        'COMPANY_MARKED_PAID', 'ADMIN_REVIEW', 'COMMISSION_RELEASED', 'CASHBACK_SENT'
    )),
    CONSTRAINT chk_leads_confirmed_room_type CHECK (
        confirmed_room_type IS NULL OR confirmed_room_type IN ('SINGLE', 'DOUBLE', 'TRIPLE', 'QUAD', 'CHILD', 'INFANT')
    ),
    CONSTRAINT chk_leads_confirmed_price CHECK (confirmed_price IS NULL OR confirmed_price >= 0)
);

COMMENT ON TABLE leads IS 'Customer-company interaction and its lifecycle. Permanent record — never soft-deleted. Per-transition audit lives in lead_status_history.';

-- One active engagement per customer/trip pair; re-contacting an existing trip resumes the same lead.
CREATE UNIQUE INDEX uq_leads_customer_trip   ON leads(customer_id, trip_id);
CREATE INDEX idx_leads_company_id_status     ON leads(company_id, status);
CREATE INDEX idx_leads_trip_id               ON leads(trip_id);
CREATE INDEX idx_leads_status                ON leads(status);
