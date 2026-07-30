CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type                VARCHAR(50) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    body                TEXT,
    data                JSONB,
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE notifications IS 'In-app notification inbox; mirrors each push sent via Firebase Cloud Messaging.';

CREATE INDEX idx_notifications_recipient_unread  ON notifications(recipient_user_id, read_at);
CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_user_id, created_at DESC);
