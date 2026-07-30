CREATE TABLE company_profiles (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_name      VARCHAR(255) NOT NULL,
    license_number    VARCHAR(100) NOT NULL,
    city              VARCHAR(100) NOT NULL,
    address           VARCHAR(500) NOT NULL,
    logo_url          VARCHAR(500),
    whatsapp          VARCHAR(30),
    description       TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason  VARCHAR(500),
    approved_by       UUID REFERENCES users(id),
    approved_at       TIMESTAMPTZ,
    rating_avg        NUMERIC(3,2) NOT NULL DEFAULT 0,
    rating_count      INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID REFERENCES users(id),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        UUID REFERENCES users(id),
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT chk_company_profiles_status      CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    CONSTRAINT chk_company_profiles_rating_avg  CHECK (rating_avg BETWEEN 0 AND 5),
    CONSTRAINT chk_company_profiles_rating_count CHECK (rating_count >= 0)
);

COMMENT ON TABLE company_profiles IS 'Company aggregate: verification profile, one per COMPANY user. Only APPROVED companies are visible to customers.';

CREATE UNIQUE INDEX uq_company_profiles_user_id ON company_profiles(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_company_profiles_status        ON company_profiles(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_company_profiles_city           ON company_profiles(city)   WHERE deleted_at IS NULL;
