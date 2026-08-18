CREATE TABLE announcement_source_reclassification_runs (
    id uuid PRIMARY KEY,
    rule_release_id uuid NOT NULL,
    rule_snapshot_hash varchar(64) NOT NULL,
    run_status_code varchar(30) NOT NULL DEFAULT 'PREVIEW_PENDING',
    provider_code varchar(40),
    collected_from date,
    collected_to date,
    include_linked_announcements boolean NOT NULL DEFAULT false,
    maximum_count integer NOT NULL,
    batch_size integer NOT NULL,
    total_count integer NOT NULL DEFAULT 0,
    scope_snapshot_at timestamptz NOT NULL DEFAULT now(),
    request_reason_hash varchar(64) NOT NULL,
    application_reason_hash varchar(64),
    rollback_reason_hash varchar(64),
    requested_by uuid NOT NULL,
    row_version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    preview_completed_at timestamptz,
    application_started_at timestamptz,
    application_completed_at timestamptz,
    rollback_started_at timestamptz,
    rollback_completed_at timestamptz,
    CONSTRAINT fk_announcement_source_reclassification_runs_release
        FOREIGN KEY (rule_release_id) REFERENCES announcement_source_classification_rule_releases (id),
    CONSTRAINT fk_announcement_source_reclassification_runs_requested_by
        FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT ck_announcement_source_reclassification_runs_snapshot_hash
        CHECK (rule_snapshot_hash ~ '^[0-9A-Fa-f]{64}$'),
    CONSTRAINT ck_announcement_source_reclassification_runs_status
        CHECK (run_status_code IN (
            'PREVIEW_PENDING', 'PREVIEW_RUNNING', 'PREVIEW_COMPLETED', 'PREVIEW_PARTIAL_FAILED',
            'APPLY_PENDING', 'APPLY_RUNNING', 'APPLY_PAUSED', 'APPLY_COMPLETED', 'APPLY_PARTIAL_FAILED',
            'ROLLBACK_PENDING', 'ROLLBACK_RUNNING', 'ROLLBACK_COMPLETED', 'ROLLBACK_PARTIAL_FAILED'
        )),
    CONSTRAINT ck_announcement_source_reclassification_runs_provider
        CHECK (provider_code IS NULL OR provider_code IN ('BIZINFO', 'GOV24_PUBLIC_SERVICE', 'LOCAL_GOV_NOTICE')),
    CONSTRAINT ck_announcement_source_reclassification_runs_period
        CHECK (collected_from IS NULL OR collected_to IS NULL OR collected_from <= collected_to),
    CONSTRAINT ck_announcement_source_reclassification_runs_maximum_count
        CHECK (maximum_count BETWEEN 1 AND 100000),
    CONSTRAINT ck_announcement_source_reclassification_runs_batch_size
        CHECK (batch_size BETWEEN 1 AND 100),
    CONSTRAINT ck_announcement_source_reclassification_runs_total_count
        CHECK (total_count >= 0),
    CONSTRAINT ck_announcement_source_reclassification_runs_request_reason_hash
        CHECK (request_reason_hash ~ '^[0-9A-Fa-f]{64}$'),
    CONSTRAINT ck_announcement_source_reclassification_runs_application_reason_hash
        CHECK (application_reason_hash IS NULL OR application_reason_hash ~ '^[0-9A-Fa-f]{64}$'),
    CONSTRAINT ck_announcement_source_reclassification_runs_rollback_reason_hash
        CHECK (rollback_reason_hash IS NULL OR rollback_reason_hash ~ '^[0-9A-Fa-f]{64}$'),
    CONSTRAINT ck_announcement_source_reclassification_runs_row_version CHECK (row_version >= 0)
);

CREATE INDEX ix_announcement_source_reclassification_runs_status
    ON announcement_source_reclassification_runs (run_status_code, created_at, id);
CREATE INDEX ix_announcement_source_reclassification_runs_release
    ON announcement_source_reclassification_runs (rule_release_id, created_at DESC);

CREATE TABLE announcement_source_reclassification_run_items (
    id uuid PRIMARY KEY,
    run_id uuid NOT NULL,
    source_id uuid NOT NULL,
    content_version_id uuid NOT NULL,
    content_hash varchar(64) NOT NULL,
    expected_classification_version integer NOT NULL,
    previous_evaluation_id uuid,
    previous_semantic_status_code varchar(30) NOT NULL,
    previous_semantic_reason_code varchar(80) NOT NULL,
    previous_semantic_matched_keywords varchar(1000),
    previous_review_status_code varchar(40) NOT NULL,
    predicted_semantic_status_code varchar(30),
    predicted_reason_code varchar(80),
    prediction_hash varchar(64),
    item_status_code varchar(30) NOT NULL DEFAULT 'PENDING',
    applied_evaluation_id uuid,
    applied_classification_version integer,
    error_code varchar(80),
    error_message varchar(500),
    previewed_at timestamptz,
    applied_at timestamptz,
    rolled_back_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_reclassification_run_items_run
        FOREIGN KEY (run_id) REFERENCES announcement_source_reclassification_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_reclassification_run_items_source
        FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_reclassification_run_items_content
        FOREIGN KEY (content_version_id, source_id)
        REFERENCES announcement_source_content_versions (id, source_id),
    CONSTRAINT fk_announcement_source_reclassification_run_items_previous_evaluation
        FOREIGN KEY (previous_evaluation_id, source_id)
        REFERENCES announcement_source_classification_evaluations (id, source_id),
    CONSTRAINT fk_announcement_source_reclassification_run_items_applied_evaluation
        FOREIGN KEY (applied_evaluation_id, source_id)
        REFERENCES announcement_source_classification_evaluations (id, source_id),
    CONSTRAINT uq_announcement_source_reclassification_run_items_source UNIQUE (run_id, source_id),
    CONSTRAINT ck_announcement_source_reclassification_run_items_content_hash
        CHECK (content_hash ~ '^[0-9A-Fa-f]{64}$'),
    CONSTRAINT ck_announcement_source_reclassification_run_items_version
        CHECK (expected_classification_version >= 0),
    CONSTRAINT ck_announcement_source_reclassification_run_items_previous_semantic
        CHECK (previous_semantic_status_code IN ('ACCEPTED', 'REVIEW_REQUIRED', 'EXCLUDED')),
    CONSTRAINT ck_announcement_source_reclassification_run_items_predicted_semantic
        CHECK (predicted_semantic_status_code IS NULL OR predicted_semantic_status_code IN (
            'ACCEPTED', 'REVIEW_REQUIRED', 'EXCLUDED'
        )),
    CONSTRAINT ck_announcement_source_reclassification_run_items_status
        CHECK (item_status_code IN (
            'PENDING', 'PREVIEWED', 'PREVIEW_CONFLICT', 'PREVIEW_FAILED',
            'APPLIED', 'APPLY_CONFLICT', 'APPLY_FAILED',
            'ROLLED_BACK', 'ROLLBACK_CONFLICT', 'ROLLBACK_FAILED'
        )),
    CONSTRAINT ck_announcement_source_reclassification_run_items_prediction_hash
        CHECK (prediction_hash IS NULL OR prediction_hash ~ '^[0-9A-Fa-f]{64}$'),
    CONSTRAINT ck_announcement_source_reclassification_run_items_applied_version
        CHECK (applied_classification_version IS NULL OR applied_classification_version >= 0)
);

CREATE INDEX ix_announcement_source_reclassification_run_items_work
    ON announcement_source_reclassification_run_items (run_id, item_status_code, source_id);
CREATE INDEX ix_announcement_source_reclassification_run_items_source
    ON announcement_source_reclassification_run_items (source_id, created_at DESC);
