-- Preserve immutable source content, classification decisions, matches, and confirmed tags.
-- Attachments are metadata only and are intentionally absent from classification locations.

CREATE TABLE announcement_source_content_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id uuid NOT NULL,
    raw_hash varchar(64) NOT NULL,
    title varchar(500) NOT NULL,
    body_text text,
    body_source_code varchar(30) NOT NULL,
    body_availability_code varchar(30) NOT NULL,
    source_url text,
    raw_payload_json jsonb,
    collected_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_content_versions_source
        FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT uq_announcement_source_content_versions_source_hash UNIQUE (source_id, raw_hash),
    CONSTRAINT uq_announcement_source_content_versions_id_source UNIQUE (id, source_id),
    CONSTRAINT ck_announcement_source_content_versions_hash CHECK (
        raw_hash ~ '^[0-9A-Fa-f]{64}$'
    ),
    CONSTRAINT ck_announcement_source_content_versions_body_source CHECK (
        body_source_code IN (
            'PROVIDER_FULL_TEXT', 'PROVIDER_SUMMARY', 'DETAIL_PAGE_TEXT', 'NONE'
        )
    ),
    CONSTRAINT ck_announcement_source_content_versions_body_availability CHECK (
        body_availability_code IN ('AVAILABLE', 'UNAVAILABLE', 'FETCH_FAILED', 'UNSUPPORTED')
    ),
    CONSTRAINT ck_announcement_source_content_versions_body_available CHECK (
        body_availability_code <> 'AVAILABLE'
        OR (
            body_source_code <> 'NONE'
            AND body_text IS NOT NULL
            AND length(btrim(body_text)) > 0
        )
    )
);

CREATE INDEX ix_announcement_source_content_versions_source
    ON announcement_source_content_versions (source_id, collected_at DESC, id);

CREATE TABLE announcement_source_classification_evaluations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id uuid NOT NULL,
    content_version_id uuid NOT NULL,
    run_id uuid,
    rule_release_id uuid NOT NULL,
    engine_version varchar(40) NOT NULL,
    body_source_code varchar(30) NOT NULL,
    body_availability_code varchar(30) NOT NULL,
    title_stage_code varchar(40) NOT NULL,
    body_stage_code varchar(40) NOT NULL,
    decision_status_code varchar(30) NOT NULL,
    reason_code varchar(80) NOT NULL,
    is_current boolean NOT NULL DEFAULT true,
    evaluated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_classification_evaluations_source
        FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_classification_evaluations_content_source
        FOREIGN KEY (content_version_id, source_id)
        REFERENCES announcement_source_content_versions (id, source_id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_classification_evaluations_run
        FOREIGN KEY (run_id) REFERENCES announcement_source_collection_runs (id) ON DELETE SET NULL,
    CONSTRAINT fk_announcement_source_classification_evaluations_release
        FOREIGN KEY (rule_release_id) REFERENCES announcement_source_classification_rule_releases (id),
    CONSTRAINT uq_announcement_source_classification_evaluations_id_source UNIQUE (id, source_id),
    CONSTRAINT ck_announcement_source_classification_evaluations_engine CHECK (
        length(btrim(engine_version)) > 0
    ),
    CONSTRAINT ck_announcement_source_classification_evaluations_body_source CHECK (
        body_source_code IN (
            'PROVIDER_FULL_TEXT', 'PROVIDER_SUMMARY', 'DETAIL_PAGE_TEXT', 'NONE'
        )
    ),
    CONSTRAINT ck_announcement_source_classification_evaluations_body_availability CHECK (
        body_availability_code IN ('AVAILABLE', 'UNAVAILABLE', 'FETCH_FAILED', 'UNSUPPORTED')
    ),
    CONSTRAINT ck_announcement_source_classification_evaluations_title_stage CHECK (
        title_stage_code IN (
            'GROUP_B_MATCHED', 'GROUP_A_MATCHED', 'COMBINATION_MATCHED',
            'COMBINATION_NOT_MATCHED', 'LEGACY_NOT_EVALUATED'
        )
    ),
    CONSTRAINT ck_announcement_source_classification_evaluations_body_stage CHECK (
        body_stage_code IN (
            'NOT_EVALUATED', 'GROUP_B_MATCHED', 'GROUP_A_MATCHED',
            'COMBINATION_CONFIRMED', 'COMBINATION_NOT_CONFIRMED',
            'UNAVAILABLE', 'FETCH_FAILED', 'LEGACY_NOT_EVALUATED'
        )
    ),
    CONSTRAINT ck_announcement_source_classification_evaluations_decision CHECK (
        decision_status_code IN ('ACCEPTED', 'REVIEW_REQUIRED', 'EXCLUDED')
    ),
    CONSTRAINT ck_announcement_source_classification_evaluations_reason CHECK (
        reason_code IN (
            'TITLE_GROUP_B_MATCHED', 'TITLE_GROUP_A_MATCHED',
            'TITLE_COMBINATION_MATCHED', 'TITLE_COMBINATION_NOT_MATCHED',
            'BODY_UNAVAILABLE', 'BODY_FETCH_FAILED', 'BODY_GROUP_B_MATCHED',
            'BODY_GROUP_A_MATCHED', 'TARGET_SUPPORT_CONFIRMED',
            'BODY_COMBINATION_NOT_CONFIRMED', 'LEGACY_V56'
        )
    )
);

CREATE UNIQUE INDEX uq_announcement_source_classification_evaluations_current
    ON announcement_source_classification_evaluations (source_id)
    WHERE is_current = true;
CREATE INDEX ix_announcement_source_classification_evaluations_current_decision
    ON announcement_source_classification_evaluations (
        is_current, decision_status_code, evaluated_at DESC
    );
CREATE INDEX ix_announcement_source_classification_evaluations_release
    ON announcement_source_classification_evaluations (rule_release_id, evaluated_at DESC);

CREATE TABLE announcement_source_classification_matches (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_id uuid NOT NULL,
    keyword_rule_id uuid NOT NULL,
    keyword_term_id uuid NOT NULL,
    match_location_code varchar(20) NOT NULL,
    matched_text varchar(500) NOT NULL,
    start_offset integer NOT NULL,
    end_offset integer NOT NULL,
    applied_action_code varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_classification_matches_evaluation
        FOREIGN KEY (evaluation_id)
        REFERENCES announcement_source_classification_evaluations (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_classification_matches_rule
        FOREIGN KEY (keyword_rule_id)
        REFERENCES announcement_source_classification_keyword_rules (id),
    CONSTRAINT fk_announcement_source_classification_matches_term_rule
        FOREIGN KEY (keyword_term_id, keyword_rule_id)
        REFERENCES announcement_source_classification_keyword_terms (id, keyword_rule_id),
    CONSTRAINT uq_announcement_source_classification_matches_position
        UNIQUE (evaluation_id, keyword_term_id, match_location_code, start_offset, end_offset),
    CONSTRAINT ck_announcement_source_classification_matches_location CHECK (
        match_location_code IN ('TITLE', 'BODY')
    ),
    CONSTRAINT ck_announcement_source_classification_matches_offsets CHECK (
        start_offset >= 0 AND end_offset > start_offset
    ),
    CONSTRAINT ck_announcement_source_classification_matches_action CHECK (
        applied_action_code IN (
            'EXCLUDED', 'REVIEW_REQUIRED', 'TAG', 'CONTEXT_ONLY', 'MASK_ONLY'
        )
    )
);

CREATE INDEX ix_announcement_source_classification_matches_evaluation
    ON announcement_source_classification_matches (
        evaluation_id, match_location_code, start_offset, end_offset
    );
CREATE INDEX ix_announcement_source_classification_matches_rule
    ON announcement_source_classification_matches (keyword_rule_id, keyword_term_id);

CREATE TABLE announcement_source_classification_target_matches (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_id uuid NOT NULL,
    target_category_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_classification_target_matches_evaluation
        FOREIGN KEY (evaluation_id)
        REFERENCES announcement_source_classification_evaluations (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_classification_target_matches_category
        FOREIGN KEY (target_category_id) REFERENCES announcement_target_categories (id),
    CONSTRAINT uq_announcement_source_classification_target_matches
        UNIQUE (evaluation_id, target_category_id)
);

CREATE INDEX ix_announcement_source_classification_target_matches_category
    ON announcement_source_classification_target_matches (target_category_id, evaluation_id);

CREATE TABLE announcement_source_classification_support_matches (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_id uuid NOT NULL,
    support_type_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_classification_support_matches_evaluation
        FOREIGN KEY (evaluation_id)
        REFERENCES announcement_source_classification_evaluations (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_classification_support_matches_type
        FOREIGN KEY (support_type_id) REFERENCES announcement_support_types (id),
    CONSTRAINT uq_announcement_source_classification_support_matches
        UNIQUE (evaluation_id, support_type_id)
);

CREATE INDEX ix_announcement_source_classification_support_matches_type
    ON announcement_source_classification_support_matches (support_type_id, evaluation_id);

CREATE TABLE announcement_source_confirmed_target_categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id uuid NOT NULL,
    target_category_id uuid NOT NULL,
    based_on_evaluation_id uuid NOT NULL,
    confirmation_status_code varchar(20) NOT NULL DEFAULT 'CURRENT',
    confirmed_by uuid NOT NULL,
    confirmed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_confirmed_targets_source
        FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_confirmed_targets_category
        FOREIGN KEY (target_category_id) REFERENCES announcement_target_categories (id),
    CONSTRAINT fk_announcement_source_confirmed_targets_evaluation_source
        FOREIGN KEY (based_on_evaluation_id, source_id)
        REFERENCES announcement_source_classification_evaluations (id, source_id),
    CONSTRAINT fk_announcement_source_confirmed_targets_confirmed_by
        FOREIGN KEY (confirmed_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_source_confirmed_targets_category
        UNIQUE (source_id, target_category_id),
    CONSTRAINT ck_announcement_source_confirmed_targets_status CHECK (
        confirmation_status_code IN ('CURRENT', 'STALE')
    )
);

CREATE INDEX ix_announcement_source_confirmed_targets_source
    ON announcement_source_confirmed_target_categories (
        source_id, confirmation_status_code, target_category_id
    );

CREATE TABLE announcement_source_confirmed_support_types (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id uuid NOT NULL,
    support_type_id uuid NOT NULL,
    based_on_evaluation_id uuid NOT NULL,
    confirmation_status_code varchar(20) NOT NULL DEFAULT 'CURRENT',
    confirmed_by uuid NOT NULL,
    confirmed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_confirmed_supports_source
        FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_confirmed_supports_type
        FOREIGN KEY (support_type_id) REFERENCES announcement_support_types (id),
    CONSTRAINT fk_announcement_source_confirmed_supports_evaluation_source
        FOREIGN KEY (based_on_evaluation_id, source_id)
        REFERENCES announcement_source_classification_evaluations (id, source_id),
    CONSTRAINT fk_announcement_source_confirmed_supports_confirmed_by
        FOREIGN KEY (confirmed_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_source_confirmed_supports_type
        UNIQUE (source_id, support_type_id),
    CONSTRAINT ck_announcement_source_confirmed_supports_status CHECK (
        confirmation_status_code IN ('CURRENT', 'STALE')
    )
);

CREATE INDEX ix_announcement_source_confirmed_supports_source
    ON announcement_source_confirmed_support_types (
        source_id, confirmation_status_code, support_type_id
    );

ALTER TABLE announcement_source_collection_runs
    ADD COLUMN rule_release_id uuid,
    ADD COLUMN search_plan_hash varchar(64),
    ADD COLUMN search_plan_json jsonb;

ALTER TABLE announcement_source_collection_runs
    ADD CONSTRAINT fk_announcement_source_collection_runs_rule_release
        FOREIGN KEY (rule_release_id) REFERENCES announcement_source_classification_rule_releases (id),
    ADD CONSTRAINT ck_announcement_source_collection_runs_search_plan_hash CHECK (
        search_plan_hash IS NULL OR search_plan_hash ~ '^[0-9a-f]{64}$'
    );

CREATE INDEX ix_announcement_source_collection_runs_rule_release
    ON announcement_source_collection_runs (rule_release_id, created_at DESC)
    WHERE rule_release_id IS NOT NULL;

ALTER TABLE announcement_source_snapshots
    ADD COLUMN classification_row_version integer NOT NULL DEFAULT 0;

ALTER TABLE announcement_source_snapshots
    ADD CONSTRAINT ck_announcement_source_snapshots_classification_row_version CHECK (
        classification_row_version >= 0
    );

-- Preserve one immutable content version for each existing snapshot without inferring a V2 evaluation.
INSERT INTO announcement_source_content_versions (
    id,
    source_id,
    raw_hash,
    title,
    body_text,
    body_source_code,
    body_availability_code,
    source_url,
    raw_payload_json,
    collected_at
)
SELECT
    md5(
        'announcement-source-content-version-backfill-'
        || snapshot.id::text || '-' || snapshot.raw_hash
    )::uuid,
    snapshot.id,
    snapshot.raw_hash,
    snapshot.title,
    snapshot.body_text,
    CASE
        WHEN snapshot.body_text IS NULL OR length(btrim(snapshot.body_text)) = 0 THEN 'NONE'
        WHEN snapshot.provider_code = 'LOCAL_GOV_NOTICE' THEN 'DETAIL_PAGE_TEXT'
        ELSE 'PROVIDER_SUMMARY'
    END,
    CASE
        WHEN snapshot.body_text IS NULL OR length(btrim(snapshot.body_text)) = 0 THEN 'UNAVAILABLE'
        ELSE 'AVAILABLE'
    END,
    coalesce(snapshot.canonical_source_url, snapshot.source_url),
    snapshot.raw_payload_json,
    snapshot.collected_at
FROM announcement_source_snapshots AS snapshot;

-- Existing V56 status/keywords remain the compatibility projection only.
-- No V56 rule or comma-separated keyword is auto-converted into a V2 evaluation or match.
