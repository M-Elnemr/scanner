CREATE TABLE customer_profiles (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    full_name                VARCHAR(255),
    phone                    VARCHAR(30),
    cashback_wallet_number   VARCHAR(30),
    wallet_type              VARCHAR(20),
    profile_completed        BOOLEAN NOT NULL DEFAULT false,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by               UUID REFERENCES users(id),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by               UUID REFERENCES users(id),
    deleted_at               TIMESTAMPTZ,

    CONSTRAINT chk_customer_profiles_wallet_type CHECK (wallet_type IS NULL OR wallet_type IN ('VODAFONE_CASH', 'ETISALAT_CASH'))
);

COMMENT ON TABLE customer_profiles IS 'Optional customer profile, completed lazily before a lead can be created.';

CREATE UNIQUE INDEX uq_customer_profiles_user_id ON customer_profiles(user_id) WHERE deleted_at IS NULL;
