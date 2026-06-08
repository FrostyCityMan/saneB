CREATE TABLE stored_files (
    id uuid PRIMARY KEY,
    owner_user_id uuid NOT NULL,
    original_filename varchar(255) NOT NULL,
    stored_filename varchar(120) NOT NULL,
    storage_key varchar(500) NOT NULL,
    content_type varchar(120),
    file_size bigint NOT NULL,
    checksum_sha256 varchar(64) NOT NULL,
    status_code varchar(20) NOT NULL DEFAULT 'STORED',
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_stored_files_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_stored_files_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_stored_files_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_stored_files_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_stored_files_size CHECK (file_size > 0),
    CONSTRAINT ck_stored_files_status CHECK (status_code IN ('STORED', 'DELETED'))
);

CREATE INDEX ix_stored_files_owner_created_at ON stored_files (owner_user_id, created_at);
CREATE INDEX ix_stored_files_status_created_at ON stored_files (status_code, created_at);

CREATE TABLE document_submissions (
    id uuid PRIMARY KEY,
    file_id uuid NOT NULL,
    submitted_by uuid NOT NULL,
    resource_type_code varchar(40) NOT NULL,
    resource_id uuid NOT NULL,
    document_type_code varchar(80) NOT NULL,
    status_code varchar(30) NOT NULL DEFAULT 'SUBMITTED',
    review_note varchar(1000),
    reviewed_by uuid,
    reviewed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_document_submissions_file FOREIGN KEY (file_id) REFERENCES stored_files (id),
    CONSTRAINT fk_document_submissions_submitted_by FOREIGN KEY (submitted_by) REFERENCES users (id),
    CONSTRAINT fk_document_submissions_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT fk_document_submissions_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_document_submissions_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_document_submissions_resource_type CHECK (
        resource_type_code IN ('PARTNER_VERIFICATION', 'APPLICATION_PROGRESS')
    ),
    CONSTRAINT ck_document_submissions_status CHECK (status_code IN ('SUBMITTED', 'APPROVED', 'REJECTED'))
);

CREATE INDEX ix_document_submissions_resource ON document_submissions (
    resource_type_code,
    resource_id,
    created_at
);
CREATE INDEX ix_document_submissions_submitter_status ON document_submissions (submitted_by, status_code, created_at);
CREATE INDEX ix_document_submissions_file ON document_submissions (file_id);

CREATE TABLE document_submission_reviews (
    id uuid PRIMARY KEY,
    submission_id uuid NOT NULL,
    reviewer_user_id uuid NOT NULL,
    before_status_code varchar(30) NOT NULL,
    after_status_code varchar(30) NOT NULL,
    review_note varchar(1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_document_submission_reviews_submission FOREIGN KEY (submission_id) REFERENCES document_submissions (id),
    CONSTRAINT fk_document_submission_reviews_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES users (id),
    CONSTRAINT ck_document_submission_reviews_before_status CHECK (
        before_status_code IN ('SUBMITTED', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT ck_document_submission_reviews_after_status CHECK (
        after_status_code IN ('SUBMITTED', 'APPROVED', 'REJECTED')
    )
);

CREATE INDEX ix_document_submission_reviews_submission_created_at ON document_submission_reviews (
    submission_id,
    created_at
);
CREATE INDEX ix_document_submission_reviews_reviewer_created_at ON document_submission_reviews (
    reviewer_user_id,
    created_at
);
