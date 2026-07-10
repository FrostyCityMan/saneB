-- Create external announcement source collection, approval, review, and highlight tables.
-- Source snapshots are immutable reference data and do not participate in matching directly.

CREATE SEQUENCE announcement_source_collection_requests_public_code_seq;
CREATE SEQUENCE announcement_source_collection_runs_public_code_seq;
CREATE SEQUENCE announcement_source_snapshots_public_code_seq;

CREATE TABLE announcement_source_collection_requests (
    id uuid PRIMARY KEY,
    public_code varchar(32) NOT NULL DEFAULT ('ASR-' || lpad(nextval('announcement_source_collection_requests_public_code_seq')::text, 6, '0')),
    provider_code varchar(50) NOT NULL,
    request_type_code varchar(30) NOT NULL,
    request_status_code varchar(30) NOT NULL DEFAULT 'APPROVAL_PENDING',
    requested_by uuid,
    requested_at timestamptz NOT NULL DEFAULT now(),
    requested_from varchar(100),
    search_keyword varchar(300),
    search_region_code varchar(80),
    search_category_code varchar(80),
    start_date date,
    end_date date,
    max_count integer,
    request_note text,
    approved_by uuid,
    approved_at timestamptz,
    approval_note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_collection_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT fk_announcement_source_collection_requests_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_source_collection_requests_public_code UNIQUE (public_code),
    CONSTRAINT ck_announcement_source_collection_requests_provider CHECK (provider_code IN ('BIZINFO', 'GOV24_PUBLIC_SERVICE')),
    CONSTRAINT ck_announcement_source_collection_requests_type CHECK (request_type_code IN ('BATCH', 'MANUAL')),
    CONSTRAINT ck_announcement_source_collection_requests_status CHECK (request_status_code IN ('APPROVAL_PENDING', 'APPROVED', 'REJECTED', 'CANCELED', 'EXPIRED')),
    CONSTRAINT ck_announcement_source_collection_requests_max_count CHECK (max_count IS NULL OR max_count > 0)
);

CREATE INDEX ix_announcement_source_collection_requests_status
    ON announcement_source_collection_requests (request_status_code, requested_at DESC);
CREATE INDEX ix_announcement_source_collection_requests_provider
    ON announcement_source_collection_requests (provider_code, request_type_code, requested_at DESC);

CREATE TABLE announcement_source_snapshots (
    id uuid PRIMARY KEY,
    public_code varchar(32) NOT NULL DEFAULT ('SRC-' || lpad(nextval('announcement_source_snapshots_public_code_seq')::text, 6, '0')),
    provider_code varchar(50) NOT NULL,
    provider_notice_id varchar(200),
    title varchar(500) NOT NULL,
    agency_name varchar(300),
    application_start_date date,
    application_end_date date,
    posted_at timestamptz,
    modified_at timestamptz,
    source_url text,
    body_text text,
    inquiry_text text,
    application_method_text text,
    source_completeness_code varchar(30) NOT NULL DEFAULT 'PARTIAL',
    missing_fields_json jsonb,
    raw_payload_json jsonb,
    raw_hash varchar(64) NOT NULL,
    review_status_code varchar(30) NOT NULL DEFAULT 'REVIEW_PENDING',
    collected_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_announcement_source_snapshots_public_code UNIQUE (public_code),
    CONSTRAINT ck_announcement_source_snapshots_provider CHECK (provider_code IN ('BIZINFO', 'GOV24_PUBLIC_SERVICE')),
    CONSTRAINT ck_announcement_source_snapshots_completeness CHECK (source_completeness_code IN ('COMPLETE', 'PARTIAL', 'MINIMAL')),
    CONSTRAINT ck_announcement_source_snapshots_review CHECK (review_status_code IN ('COLLECTED', 'REVIEW_PENDING', 'CONDITION_INPUT_REQUIRED', 'REVIEW_COMPLETED', 'ACTIVATED', 'ARCHIVED', 'DUPLICATE', 'SKIPPED_ENDED'))
);

CREATE UNIQUE INDEX uq_announcement_source_snapshots_provider_notice
    ON announcement_source_snapshots (provider_code, provider_notice_id)
    WHERE provider_notice_id IS NOT NULL;
CREATE UNIQUE INDEX uq_announcement_source_snapshots_provider_url
    ON announcement_source_snapshots (provider_code, source_url)
    WHERE source_url IS NOT NULL;
CREATE UNIQUE INDEX uq_announcement_source_snapshots_provider_hash
    ON announcement_source_snapshots (provider_code, raw_hash);
CREATE INDEX ix_announcement_source_snapshots_review
    ON announcement_source_snapshots (review_status_code, collected_at DESC);
CREATE INDEX ix_announcement_source_snapshots_dates
    ON announcement_source_snapshots (application_start_date, application_end_date);

CREATE TABLE announcement_source_attachments (
    id uuid PRIMARY KEY,
    source_id uuid NOT NULL,
    file_name varchar(500),
    file_url text NOT NULL,
    file_type_code varchar(80),
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_attachments_source FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT ck_announcement_source_attachments_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_announcement_source_attachments_source
    ON announcement_source_attachments (source_id, sort_order);

CREATE TABLE announcement_source_highlights (
    id uuid PRIMARY KEY,
    source_id uuid NOT NULL,
    highlight_type_code varchar(50) NOT NULL,
    matched_text text NOT NULL,
    start_offset integer,
    end_offset integer,
    line_no integer,
    match_rule_code varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_highlights_source FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT ck_announcement_source_highlights_type CHECK (highlight_type_code IN ('TARGET', 'SUPPORT_CONTENT', 'APPLICATION_PERIOD', 'APPLICATION_METHOD', 'EXCLUDED_TARGET', 'PREFERRED_CONDITION', 'BUSINESS_AGE_CONDITION', 'SALES_CONDITION', 'INDUSTRY_CONDITION', 'REGION_CONDITION', 'INCOME_CONDITION', 'ASSET_CONDITION', 'HEALTH_INSURANCE_CONDITION', 'REQUIRED_DOCUMENT', 'INQUIRY')),
    CONSTRAINT ck_announcement_source_highlights_rule CHECK (match_rule_code IN ('RULE_HEADING', 'RULE_KEYWORD', 'RULE_PATTERN')),
    CONSTRAINT ck_announcement_source_highlights_offsets CHECK (
        (start_offset IS NULL AND end_offset IS NULL)
        OR (start_offset IS NOT NULL AND end_offset IS NOT NULL AND start_offset >= 0 AND end_offset >= start_offset)
    )
);

CREATE INDEX ix_announcement_source_highlights_source
    ON announcement_source_highlights (source_id, highlight_type_code);

CREATE TABLE announcement_source_review_histories (
    id uuid PRIMARY KEY,
    source_id uuid NOT NULL,
    previous_status_code varchar(30),
    next_status_code varchar(30) NOT NULL,
    reason text,
    changed_by uuid NOT NULL,
    changed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_review_histories_source FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_review_histories_changed_by FOREIGN KEY (changed_by) REFERENCES users (id),
    CONSTRAINT ck_announcement_source_review_histories_previous CHECK (previous_status_code IS NULL OR previous_status_code IN ('COLLECTED', 'REVIEW_PENDING', 'CONDITION_INPUT_REQUIRED', 'REVIEW_COMPLETED', 'ACTIVATED', 'ARCHIVED', 'DUPLICATE', 'SKIPPED_ENDED')),
    CONSTRAINT ck_announcement_source_review_histories_next CHECK (next_status_code IN ('COLLECTED', 'REVIEW_PENDING', 'CONDITION_INPUT_REQUIRED', 'REVIEW_COMPLETED', 'ACTIVATED', 'ARCHIVED', 'DUPLICATE', 'SKIPPED_ENDED'))
);

CREATE INDEX ix_announcement_source_review_histories_source
    ON announcement_source_review_histories (source_id, changed_at DESC);

CREATE TABLE announcement_source_links (
    id uuid PRIMARY KEY,
    source_id uuid NOT NULL,
    announcement_id uuid NOT NULL,
    linked_by uuid NOT NULL,
    linked_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_links_source FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id),
    CONSTRAINT fk_announcement_source_links_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT fk_announcement_source_links_linked_by FOREIGN KEY (linked_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_source_links_source_announcement UNIQUE (source_id, announcement_id)
);

CREATE INDEX ix_announcement_source_links_announcement
    ON announcement_source_links (announcement_id, linked_at DESC);

CREATE TABLE announcement_source_collection_runs (
    id uuid PRIMARY KEY,
    public_code varchar(32) NOT NULL DEFAULT ('ASRUN-' || lpad(nextval('announcement_source_collection_runs_public_code_seq')::text, 6, '0')),
    request_id uuid NOT NULL,
    run_status_code varchar(30) NOT NULL DEFAULT 'QUEUED',
    started_at timestamptz,
    finished_at timestamptz,
    total_count integer NOT NULL DEFAULT 0,
    collected_count integer NOT NULL DEFAULT 0,
    skipped_ended_count integer NOT NULL DEFAULT 0,
    duplicate_count integer NOT NULL DEFAULT 0,
    failed_count integer NOT NULL DEFAULT 0,
    error_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_collection_runs_request FOREIGN KEY (request_id) REFERENCES announcement_source_collection_requests (id),
    CONSTRAINT uq_announcement_source_collection_runs_public_code UNIQUE (public_code),
    CONSTRAINT ck_announcement_source_collection_runs_status CHECK (run_status_code IN ('QUEUED', 'RUNNING', 'COMPLETED', 'PARTIAL_FAILED', 'FAILED')),
    CONSTRAINT ck_announcement_source_collection_runs_counts CHECK (
        total_count >= 0
        AND collected_count >= 0
        AND skipped_ended_count >= 0
        AND duplicate_count >= 0
        AND failed_count >= 0
    )
);

CREATE INDEX ix_announcement_source_collection_runs_request
    ON announcement_source_collection_runs (request_id, created_at DESC);
CREATE INDEX ix_announcement_source_collection_runs_status
    ON announcement_source_collection_runs (run_status_code, created_at DESC);

CREATE TABLE announcement_source_collection_run_items (
    id uuid PRIMARY KEY,
    run_id uuid NOT NULL,
    source_id uuid,
    provider_notice_id varchar(200),
    source_url text,
    item_status_code varchar(30) NOT NULL,
    error_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_collection_run_items_run FOREIGN KEY (run_id) REFERENCES announcement_source_collection_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_collection_run_items_source FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id),
    CONSTRAINT ck_announcement_source_collection_run_items_status CHECK (item_status_code IN ('COLLECTED', 'DUPLICATE', 'SKIPPED_ENDED', 'FAILED'))
);

CREATE INDEX ix_announcement_source_collection_run_items_run
    ON announcement_source_collection_run_items (run_id, item_status_code, created_at DESC);
