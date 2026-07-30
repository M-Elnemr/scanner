CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    google_sub      VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID REFERENCES users(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID REFERENCES users(id),
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT chk_users_role   CHECK (role IN ('ADMIN', 'COMPANY', 'CUSTOMER')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

COMMENT ON TABLE users IS 'Identity aggregate root. One row per Google-authenticated principal.';

-- Partial uniqueness so a soft-deleted account does not block re-registration of the same email/Google subject.
CREATE UNIQUE INDEX uq_users_email      ON users(email)      WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_google_sub ON users(google_sub) WHERE deleted_at IS NULL;

CREATE INDEX idx_users_role_status ON users(role, status) WHERE deleted_at IS NULL;
