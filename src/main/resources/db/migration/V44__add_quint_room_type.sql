ALTER TABLE room_prices DROP CONSTRAINT chk_room_prices_room_type;
ALTER TABLE room_prices ADD CONSTRAINT chk_room_prices_room_type
    CHECK (room_type IN ('SINGLE', 'DOUBLE', 'TRIPLE', 'QUAD', 'QUINT', 'CHILD', 'INFANT'));

ALTER TABLE leads DROP CONSTRAINT chk_leads_confirmed_room_type;
ALTER TABLE leads ADD CONSTRAINT chk_leads_confirmed_room_type CHECK (
    confirmed_room_type IS NULL OR confirmed_room_type IN ('SINGLE', 'DOUBLE', 'TRIPLE', 'QUAD', 'QUINT', 'CHILD', 'INFANT')
);
