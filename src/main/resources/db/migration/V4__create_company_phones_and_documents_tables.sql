CREATE TABLE company_phones (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID NOT NULL REFERENCES company_profiles(id) ON DELETE CASCADE,
    phone_number  VARCHAR(30) NOT NULL,
    is_primary    BOOLEAN NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE company_phones IS 'One or more contact numbers per company.';

CREATE UNIQUE INDEX uq_company_phones_company_number ON company_phones(company_id, phone_number);
-- At most one primary phone per company.
CREATE UNIQUE INDEX uq_company_phones_primary ON company_phones(company_id) WHERE is_primary = true;
CREATE INDEX idx_company_phones_company_id ON company_phones(company_id);


CREATE TABLE company_documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID NOT NULL REFERENCES company_profiles(id) ON DELETE CASCADE,
    doc_type      VARCHAR(30) NOT NULL,
    file_url      VARCHAR(500) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by   UUID REFERENCES users(id),
    reviewed_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_company_documents_doc_type CHECK (doc_type IN ('TOURISM_LICENSE', 'COMMERCIAL_REGISTER', 'TAX_CARD', 'OTHER')),
    CONSTRAINT chk_company_documents_status   CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

COMMENT ON TABLE company_documents IS 'Uploaded verification documents reviewed by Admin as part of company approval.';

CREATE INDEX idx_company_documents_company_id ON company_documents(company_id);
CREATE INDEX idx_company_documents_status     ON company_documents(status);
