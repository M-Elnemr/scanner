CREATE TABLE company_addresses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID NOT NULL REFERENCES company_profiles(id) ON DELETE CASCADE,
    city_id       UUID NOT NULL REFERENCES cities(id),
    address_text  VARCHAR(500) NOT NULL,
    mobile_number VARCHAR(30) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE company_addresses IS 'One or more branch addresses per company; each carries its own city, free-text address, and a contact mobile number.';

CREATE INDEX idx_company_addresses_company_id ON company_addresses(company_id);
CREATE INDEX idx_company_addresses_city_id    ON company_addresses(city_id);
