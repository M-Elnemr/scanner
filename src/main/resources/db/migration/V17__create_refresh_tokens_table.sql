CREATE TABLE refresh_tokens (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash             VARCHAR(255) NOT NULL,
    device_info            VARCHAR(255),
    issued_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at             TIMESTAMPTZ NOT NULL,
    revoked                BOOLEAN NOT NULL DEFAULT false,
    revoked_at             TIMESTAMPTZ,
    replaced_by_token_id   UUID REFERENCES refresh_tokens(id)
);

COMMENT ON TABLE refresh_tokens IS 'Hashed, rotatable refresh tokens. A token reused after rotation (replaced_by_token_id set) signals theft and revokes the family.';

CREATE UNIQUE INDEX uq_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id_active ON refresh_tokens(user_id) WHERE revoked = false;
