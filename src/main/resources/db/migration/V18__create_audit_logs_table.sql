CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id  UUID REFERENCES users(id),
    action         VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100) NOT NULL,
    entity_id      UUID,
    old_value      JSONB,
    new_value      JSONB,
    ip_address     VARCHAR(45),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE audit_logs IS 'Before/after trail for sensitive state changes (approvals, lead transitions, commission/cashback release). Immutable, actor may be null for system-triggered entries.';

CREATE INDEX idx_audit_logs_entity      ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor       ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_logs_created_at  ON audit_logs(created_at DESC);
