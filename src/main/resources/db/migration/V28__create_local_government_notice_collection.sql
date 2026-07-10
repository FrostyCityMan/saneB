-- Add local-government notice source registry, parser profiles, collection results, and approved schedules.

CREATE SEQUENCE local_government_notice_sources_public_code_seq;
CREATE SEQUENCE announcement_source_collection_schedules_public_code_seq;

CREATE TABLE local_government_notice_parser_profiles (
    id uuid PRIMARY KEY,
    profile_code varchar(50) NOT NULL,
    profile_name varchar(120) NOT NULL,
    parser_type_code varchar(30) NOT NULL,
    list_item_selector varchar(1000),
    title_selector varchar(1000),
    date_selector varchar(1000),
    link_selector varchar(1000),
    date_pattern varchar(100),
    is_enabled boolean NOT NULL DEFAULT true,
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_local_government_notice_parser_profiles_code UNIQUE (profile_code),
    CONSTRAINT fk_local_government_notice_parser_profiles_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_local_government_notice_parser_profiles_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_local_government_notice_parser_profiles_type CHECK (
        parser_type_code IN ('SAEOL_GOSI', 'SPRING_BBS', 'JSP_BBS', 'TC_GOSI', 'GENERIC_TABLE', 'GENERIC_LIST', 'MANUAL_ONLY')
    )
);

CREATE TABLE local_government_notice_sources (
    id uuid PRIMARY KEY,
    public_code varchar(32) NOT NULL DEFAULT ('LGS-' || lpad(nextval('local_government_notice_sources_public_code_seq')::text, 6, '0')),
    sido_code varchar(10),
    sido_name varchar(80) NOT NULL,
    sigungu_code varchar(10) NOT NULL,
    sigungu_name varchar(100) NOT NULL,
    institution_type_code varchar(40) NOT NULL,
    institution_name varchar(150) NOT NULL,
    homepage_url text,
    notice_url text NOT NULL,
    page_type_code varchar(60),
    parser_profile_code varchar(50),
    collection_hint text,
    confidence_code varchar(20) NOT NULL DEFAULT 'MEDIUM',
    validation_status_code varchar(30) NOT NULL DEFAULT 'CHECK_REQUIRED',
    is_enabled boolean NOT NULL DEFAULT false,
    collection_status_code varchar(30) NOT NULL DEFAULT 'CHECK_REQUIRED',
    last_collected_at timestamptz,
    last_success_at timestamptz,
    last_http_status integer,
    last_error_code varchar(80),
    last_error_message varchar(1000),
    etag varchar(500),
    last_modified_value varchar(500),
    last_content_fingerprint varchar(64),
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uq_local_government_notice_sources_public_code UNIQUE (public_code),
    CONSTRAINT fk_local_government_notice_sources_parser_profile FOREIGN KEY (parser_profile_code) REFERENCES local_government_notice_parser_profiles (profile_code),
    CONSTRAINT fk_local_government_notice_sources_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_local_government_notice_sources_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_local_government_notice_sources_institution_type CHECK (
        institution_type_code IN ('SIDO', 'BASIC_LOCAL_GOVERNMENT', 'ADMINISTRATIVE_CITY', 'CHECK_REQUIRED')
    ),
    CONSTRAINT ck_local_government_notice_sources_confidence CHECK (confidence_code IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT ck_local_government_notice_sources_validation CHECK (validation_status_code IN ('VERIFIED', 'CHECK_REQUIRED', 'FAILED')),
    CONSTRAINT ck_local_government_notice_sources_collection_status CHECK (
        collection_status_code IN ('READY', 'SUCCESS', 'NO_CHANGE', 'FAILED', 'URL_ERROR', 'ACCESS_BLOCKED', 'PARSER_UNSUPPORTED', 'CHECK_REQUIRED', 'DISABLED')
    ),
    CONSTRAINT ck_local_government_notice_sources_enabled CHECK (
        NOT is_enabled OR (validation_status_code = 'VERIFIED' AND parser_profile_code IS NOT NULL AND deleted_at IS NULL)
    ),
    CONSTRAINT ck_local_government_notice_sources_http_status CHECK (last_http_status IS NULL OR last_http_status BETWEEN 100 AND 599)
);

CREATE UNIQUE INDEX uq_local_government_notice_sources_active_url
    ON local_government_notice_sources (sigungu_code, notice_url)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_local_government_notice_sources_region
    ON local_government_notice_sources (sido_name, sigungu_name, institution_name);
CREATE INDEX ix_local_government_notice_sources_operation
    ON local_government_notice_sources (is_enabled, collection_status_code, last_collected_at);

CREATE TABLE announcement_source_collection_schedules (
    id uuid PRIMARY KEY,
    public_code varchar(32) NOT NULL DEFAULT ('ASSCH-' || lpad(nextval('announcement_source_collection_schedules_public_code_seq')::text, 6, '0')),
    provider_code varchar(50) NOT NULL,
    schedule_name varchar(150) NOT NULL,
    cron_expression varchar(100) NOT NULL,
    timezone varchar(60) NOT NULL DEFAULT 'Asia/Seoul',
    schedule_status_code varchar(30) NOT NULL DEFAULT 'APPROVAL_PENDING',
    max_count integer,
    requested_by uuid,
    approved_by uuid,
    approved_at timestamptz,
    approval_note text,
    last_run_at timestamptz,
    next_run_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_announcement_source_collection_schedules_public_code UNIQUE (public_code),
    CONSTRAINT fk_announcement_source_collection_schedules_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT fk_announcement_source_collection_schedules_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT ck_announcement_source_collection_schedules_provider CHECK (provider_code IN ('BIZINFO', 'GOV24_PUBLIC_SERVICE', 'LOCAL_GOV_NOTICE')),
    CONSTRAINT ck_announcement_source_collection_schedules_status CHECK (
        schedule_status_code IN ('APPROVAL_PENDING', 'APPROVED', 'PAUSED', 'REJECTED', 'EXPIRED')
    ),
    CONSTRAINT ck_announcement_source_collection_schedules_max_count CHECK (max_count IS NULL OR max_count > 0)
);

CREATE INDEX ix_announcement_source_collection_schedules_due
    ON announcement_source_collection_schedules (schedule_status_code, next_run_at);

ALTER TABLE announcement_source_collection_requests
    ADD COLUMN local_government_source_id uuid,
    ADD COLUMN schedule_id uuid;
ALTER TABLE announcement_source_collection_requests
    ADD CONSTRAINT fk_announcement_source_collection_requests_local_source
        FOREIGN KEY (local_government_source_id) REFERENCES local_government_notice_sources (id),
    ADD CONSTRAINT fk_announcement_source_collection_requests_schedule
        FOREIGN KEY (schedule_id) REFERENCES announcement_source_collection_schedules (id);
ALTER TABLE announcement_source_collection_requests DROP CONSTRAINT ck_announcement_source_collection_requests_provider;
ALTER TABLE announcement_source_collection_requests
    ADD CONSTRAINT ck_announcement_source_collection_requests_provider
        CHECK (provider_code IN ('BIZINFO', 'GOV24_PUBLIC_SERVICE', 'LOCAL_GOV_NOTICE'));

ALTER TABLE announcement_source_snapshots
    ADD COLUMN local_government_source_id uuid,
    ADD COLUMN canonical_source_url text,
    ADD COLUMN normalized_title varchar(500),
    ADD COLUMN normalized_agency_name varchar(300),
    ADD COLUMN posted_date date;
ALTER TABLE announcement_source_snapshots
    ADD CONSTRAINT fk_announcement_source_snapshots_local_source
        FOREIGN KEY (local_government_source_id) REFERENCES local_government_notice_sources (id);
ALTER TABLE announcement_source_snapshots DROP CONSTRAINT ck_announcement_source_snapshots_provider;
ALTER TABLE announcement_source_snapshots
    ADD CONSTRAINT ck_announcement_source_snapshots_provider
        CHECK (provider_code IN ('BIZINFO', 'GOV24_PUBLIC_SERVICE', 'LOCAL_GOV_NOTICE'));
UPDATE announcement_source_snapshots
SET canonical_source_url = source_url,
    normalized_title = lower(trim(title)),
    normalized_agency_name = lower(trim(coalesce(agency_name, ''))),
    posted_date = posted_at::date;
CREATE INDEX ix_announcement_source_snapshots_canonical_url
    ON announcement_source_snapshots (canonical_source_url)
    WHERE canonical_source_url IS NOT NULL;
CREATE INDEX ix_announcement_source_snapshots_normalized_identity
    ON announcement_source_snapshots (normalized_title, normalized_agency_name, posted_date);

ALTER TABLE announcement_source_collection_run_items
    ADD COLUMN local_government_source_id uuid,
    ADD COLUMN error_code varchar(80);
ALTER TABLE announcement_source_collection_run_items
    ADD CONSTRAINT fk_announcement_source_collection_run_items_local_source
        FOREIGN KEY (local_government_source_id) REFERENCES local_government_notice_sources (id);

CREATE TABLE announcement_source_collection_source_results (
    id uuid PRIMARY KEY,
    run_id uuid NOT NULL,
    local_government_source_id uuid NOT NULL,
    result_status_code varchar(30) NOT NULL,
    discovered_count integer NOT NULL DEFAULT 0,
    new_count integer NOT NULL DEFAULT 0,
    duplicate_count integer NOT NULL DEFAULT 0,
    failed_count integer NOT NULL DEFAULT 0,
    http_status integer,
    error_code varchar(80),
    error_message varchar(1000),
    started_at timestamptz NOT NULL DEFAULT now(),
    finished_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_collection_source_results_run FOREIGN KEY (run_id) REFERENCES announcement_source_collection_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_collection_source_results_source FOREIGN KEY (local_government_source_id) REFERENCES local_government_notice_sources (id),
    CONSTRAINT uq_announcement_source_collection_source_results_run_source UNIQUE (run_id, local_government_source_id),
    CONSTRAINT ck_announcement_source_collection_source_results_status CHECK (
        result_status_code IN ('SUCCESS', 'NO_CHANGE', 'PARTIAL_FAILED', 'FAILED', 'URL_ERROR', 'ACCESS_BLOCKED', 'PARSER_UNSUPPORTED', 'SKIPPED')
    ),
    CONSTRAINT ck_announcement_source_collection_source_results_counts CHECK (
        discovered_count >= 0 AND new_count >= 0 AND duplicate_count >= 0 AND failed_count >= 0
    ),
    CONSTRAINT ck_announcement_source_collection_source_results_http CHECK (http_status IS NULL OR http_status BETWEEN 100 AND 599)
);

CREATE INDEX ix_announcement_source_collection_source_results_source
    ON announcement_source_collection_source_results (local_government_source_id, started_at DESC);

CREATE TABLE announcement_source_snapshot_duplicates (
    id uuid PRIMARY KEY,
    source_id uuid NOT NULL,
    candidate_source_id uuid NOT NULL,
    match_type_code varchar(30) NOT NULL,
    title_matched boolean NOT NULL DEFAULT false,
    agency_matched boolean NOT NULL DEFAULT false,
    posted_date_matched boolean NOT NULL DEFAULT false,
    source_url_matched boolean NOT NULL DEFAULT false,
    match_reason varchar(1000),
    decision_status_code varchar(40) NOT NULL DEFAULT 'PENDING',
    decided_by uuid,
    decided_at timestamptz,
    decision_note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_snapshot_duplicates_source FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_snapshot_duplicates_candidate FOREIGN KEY (candidate_source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_snapshot_duplicates_decided_by FOREIGN KEY (decided_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_source_snapshot_duplicates_pair UNIQUE (source_id, candidate_source_id),
    CONSTRAINT ck_announcement_source_snapshot_duplicates_distinct CHECK (source_id <> candidate_source_id),
    CONSTRAINT ck_announcement_source_snapshot_duplicates_order CHECK (source_id < candidate_source_id),
    CONSTRAINT ck_announcement_source_snapshot_duplicates_match CHECK (match_type_code IN ('EXACT_DUPLICATE', 'SIMILAR')),
    CONSTRAINT ck_announcement_source_snapshot_duplicates_decision CHECK (
        decision_status_code IN ('AUTO_CONFIRMED', 'PENDING', 'CREATE_NEW_SELECTED', 'UPDATE_EXISTING_SELECTED', 'IGNORED')
    )
);

CREATE INDEX ix_announcement_source_snapshot_duplicates_source
    ON announcement_source_snapshot_duplicates (source_id, match_type_code, decision_status_code);

CREATE TABLE announcement_source_schedule_executions (
    id uuid PRIMARY KEY,
    schedule_id uuid NOT NULL,
    scheduled_for timestamptz NOT NULL,
    request_id uuid,
    run_id uuid,
    execution_status_code varchar(30) NOT NULL DEFAULT 'QUEUED',
    error_message varchar(1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_schedule_executions_schedule FOREIGN KEY (schedule_id) REFERENCES announcement_source_collection_schedules (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_schedule_executions_request FOREIGN KEY (request_id) REFERENCES announcement_source_collection_requests (id),
    CONSTRAINT fk_announcement_source_schedule_executions_run FOREIGN KEY (run_id) REFERENCES announcement_source_collection_runs (id),
    CONSTRAINT uq_announcement_source_schedule_executions_slot UNIQUE (schedule_id, scheduled_for),
    CONSTRAINT ck_announcement_source_schedule_executions_status CHECK (
        execution_status_code IN ('QUEUED', 'RUNNING', 'COMPLETED', 'PARTIAL_FAILED', 'FAILED', 'SKIPPED')
    )
);

CREATE INDEX ix_announcement_source_schedule_executions_schedule
    ON announcement_source_schedule_executions (schedule_id, scheduled_for DESC);
