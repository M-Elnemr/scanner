CREATE TABLE device_tokens (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token      VARCHAR(500) NOT NULL,
    platform       VARCHAR(10) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_device_tokens_platform CHECK (platform IN ('ANDROID', 'IOS'))
);

COMMENT ON TABLE device_tokens IS 'FCM registration tokens per device, used to target push notifications.';

CREATE UNIQUE INDEX uq_device_tokens_fcm_token ON device_tokens(fcm_token);
CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);
