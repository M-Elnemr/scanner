CREATE TABLE ratings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id      UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    trip_id      UUID NOT NULL REFERENCES trips(id),
    company_id   UUID NOT NULL REFERENCES company_profiles(id),
    customer_id  UUID NOT NULL REFERENCES customer_profiles(id),
    stars        SMALLINT NOT NULL,
    comment      TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_ratings_stars CHECK (stars BETWEEN 1 AND 5)
);

COMMENT ON TABLE ratings IS 'One rating per completed lead (status = CASHBACK_SENT), immutable once posted.';

CREATE UNIQUE INDEX uq_ratings_lead_id ON ratings(lead_id);
CREATE INDEX idx_ratings_company_id ON ratings(company_id);
CREATE INDEX idx_ratings_trip_id    ON ratings(trip_id);
