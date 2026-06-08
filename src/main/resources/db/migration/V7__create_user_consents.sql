CREATE TABLE consent_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    consent_code varchar(80) NOT NULL,
    consent_name varchar(150) NOT NULL,
    version_no integer NOT NULL,
    is_required boolean NOT NULL DEFAULT true,
    effective_from timestamptz NOT NULL DEFAULT now(),
    effective_to timestamptz,
    content_hash varchar(128),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_consent_versions_code_version UNIQUE (consent_code, version_no),
    CONSTRAINT ck_consent_versions_code CHECK (
        consent_code IN (
            'TERMS_OF_SERVICE',
            'PRIVACY_POLICY',
            'E_CERT',
            'CREDIT_CHECK'
        )
    ),
    CONSTRAINT ck_consent_versions_version CHECK (version_no > 0),
    CONSTRAINT ck_consent_versions_period CHECK (effective_to IS NULL OR effective_to > effective_from)
);

CREATE UNIQUE INDEX uq_consent_versions_current
    ON consent_versions (consent_code)
    WHERE effective_to IS NULL;

CREATE INDEX ix_consent_versions_code_effective
    ON consent_versions (consent_code, effective_from DESC);

CREATE TABLE user_consents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    consent_version_id uuid NOT NULL,
    consent_code varchar(80) NOT NULL,
    is_consented boolean NOT NULL,
    consented_at timestamptz NOT NULL DEFAULT now(),
    ip_address inet,
    user_agent varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    CONSTRAINT fk_user_consents_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_consents_version FOREIGN KEY (consent_version_id) REFERENCES consent_versions (id),
    CONSTRAINT fk_user_consents_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_user_consents_code CHECK (
        consent_code IN (
            'TERMS_OF_SERVICE',
            'PRIVACY_POLICY',
            'E_CERT',
            'CREDIT_CHECK'
        )
    )
);

CREATE INDEX ix_user_consents_user_code_consented_at
    ON user_consents (user_id, consent_code, consented_at DESC);

CREATE INDEX ix_user_consents_version
    ON user_consents (consent_version_id);

INSERT INTO consent_versions (
    consent_code,
    consent_name,
    version_no,
    is_required,
    content_hash
) VALUES
    ('TERMS_OF_SERVICE', '이용약관', 1, true, 'v1-terms-of-service'),
    ('PRIVACY_POLICY', '개인정보 처리방침', 1, true, 'v1-privacy-policy'),
    ('E_CERT', '전자증명 이용 동의', 1, false, 'v1-e-cert'),
    ('CREDIT_CHECK', '신용조회 동의', 1, false, 'v1-credit-check');
