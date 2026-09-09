-- 첨부 근거는 기존 TITLE/BODY 판정과 분리한다. 정책과 worker는 자동 활성화하지 않는다.
ALTER TABLE announcement_source_classification_rule_groups
    ADD CONSTRAINT uq_att_group_release UNIQUE (id, release_id);
ALTER TABLE announcement_source_classification_evaluations
    ADD CONSTRAINT uq_att_base_binding UNIQUE (id, source_id, content_version_id, rule_release_id);

CREATE TABLE announcement_attachment_policies (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_code varchar(40) NOT NULL,
    version_no integer NOT NULL CHECK (version_no > 0),
    policy_status_code varchar(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (policy_status_code IN ('DRAFT','ACTIVE','RETIRED')),
    mode_code varchar(20) NOT NULL CHECK (mode_code IN ('OFF','COLLECT_ONLY','ENFORCE')),
    rule_release_id uuid NOT NULL REFERENCES announcement_source_classification_rule_releases(id),
    policy_hash varchar(64) CHECK (policy_hash ~ '^[0-9a-f]{64}$'),
    settings_json jsonb NOT NULL CHECK (jsonb_typeof(settings_json) = 'object'),
    profile_manifest_json jsonb NOT NULL CHECK (jsonb_typeof(profile_manifest_json) = 'array'),
    created_by uuid NOT NULL REFERENCES users(id),
    published_at timestamptz,
    row_version integer NOT NULL DEFAULT 0 CHECK (row_version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_att_policy_version UNIQUE (policy_code, version_no),
    CONSTRAINT uq_att_policy_release UNIQUE (id, rule_release_id),
    CHECK (policy_status_code = 'DRAFT' OR (policy_hash IS NOT NULL AND published_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_att_policy_active ON announcement_attachment_policies(rule_release_id)
    WHERE policy_status_code = 'ACTIVE';
CREATE INDEX ix_att_policy_actor ON announcement_attachment_policies(created_by);

CREATE TABLE announcement_attachment_batches (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_type_code varchar(20) NOT NULL CHECK (batch_type_code IN ('BACKFILL','RETRY')),
    policy_id uuid NOT NULL REFERENCES announcement_attachment_policies(id),
    batch_status_code varchar(30) NOT NULL DEFAULT 'SCOPE_READY' CHECK (batch_status_code IN (
        'SCOPE_READY','COLLECTION_PENDING','COLLECTING','COLLECTED','COLLECTION_PARTIAL_FAILED',
        'COLLECTION_PAUSED','PREVIEW_RUNNING','PREVIEW_READY','PREVIEW_PARTIAL_FAILED',
        'APPLYING','APPLIED','APPLY_PARTIAL_FAILED','APPLY_PAUSED',
        'ROLLING_BACK','ROLLED_BACK','ROLLBACK_PARTIAL_FAILED')),
    scope_json jsonb NOT NULL CHECK (jsonb_typeof(scope_json) = 'object'),
    scope_hash varchar(64) NOT NULL CHECK (scope_hash ~ '^[0-9a-f]{64}$'),
    maximum_count integer NOT NULL CHECK (maximum_count BETWEEN 1 AND 1000),
    requested_by uuid NOT NULL REFERENCES users(id),
    approved_by uuid REFERENCES users(id),
    scope_fixed_at timestamptz NOT NULL DEFAULT now(),
    preview_hash varchar(64) CHECK (preview_hash ~ '^[0-9a-f]{64}$'),
    applied_at timestamptz,
    reason_hash varchar(64) NOT NULL CHECK (reason_hash ~ '^[0-9a-f]{64}$'),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    deleted_item_count integer NOT NULL DEFAULT 0 CHECK (deleted_item_count >= 0),
    row_version integer NOT NULL DEFAULT 0 CHECK (row_version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_att_batch_policy ON announcement_attachment_batches(policy_id);
CREATE INDEX ix_att_batch_status ON announcement_attachment_batches(batch_status_code,created_at,id);
CREATE INDEX ix_att_batch_requester ON announcement_attachment_batches(requested_by);
CREATE INDEX ix_att_batch_approver ON announcement_attachment_batches(approved_by);

CREATE TABLE announcement_source_attachment_sets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id uuid NOT NULL REFERENCES announcement_source_snapshots(id) ON DELETE CASCADE,
    content_version_id uuid NOT NULL,
    policy_id uuid NOT NULL REFERENCES announcement_attachment_policies(id),
    data_purpose_code varchar(20) NOT NULL CHECK (data_purpose_code IN ('PRODUCTION','QA')),
    discovery_status_code varchar(30) NOT NULL DEFAULT 'PENDING' CHECK (discovery_status_code IN (
        'PENDING','FOUND','NO_FILES','FAILED','PROFILE_REQUIRED','LIMIT_EXCEEDED')),
    set_status_code varchar(20) NOT NULL DEFAULT 'OPEN' CHECK (set_status_code IN ('OPEN','SEALED','CANCELLED')),
    manifest_hash varchar(64) CHECK (manifest_hash ~ '^[0-9a-f]{64}$'),
    profile_hash varchar(64) NOT NULL CHECK (profile_hash ~ '^[0-9a-f]{64}$'),
    discovered_count integer NOT NULL DEFAULT 0 CHECK (discovered_count >= 0),
    processed_count integer NOT NULL DEFAULT 0 CHECK (processed_count >= 0 AND processed_count <= discovered_count),
    is_discovery_complete boolean NOT NULL DEFAULT false,
    discovered_at timestamptz,
    sealed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_set_content FOREIGN KEY (content_version_id,source_id)
        REFERENCES announcement_source_content_versions(id,source_id) ON DELETE CASCADE,
    CONSTRAINT uq_att_set_source UNIQUE (id,source_id),
    CONSTRAINT uq_att_set_binding UNIQUE (id,source_id,content_version_id,policy_id),
    CHECK (set_status_code <> 'SEALED' OR (manifest_hash IS NOT NULL AND sealed_at IS NOT NULL
        AND processed_count = discovered_count AND discovery_status_code <> 'PENDING')),
    CHECK (discovery_status_code <> 'NO_FILES' OR (is_discovery_complete AND discovered_count = 0)),
    CHECK (sealed_at IS NULL OR sealed_at >= created_at)
);
CREATE INDEX ix_att_set_source ON announcement_source_attachment_sets(source_id,created_at DESC,id);
CREATE INDEX ix_att_set_content ON announcement_source_attachment_sets(content_version_id,source_id);
CREATE INDEX ix_att_set_policy ON announcement_source_attachment_sets(policy_id);

CREATE TABLE announcement_source_attachment_files (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    set_id uuid NOT NULL,
    source_id uuid NOT NULL,
    stable_locator_hash varchar(64) NOT NULL CHECK (stable_locator_hash ~ '^[0-9a-f]{64}$'),
    safe_locator_json jsonb NOT NULL CHECK (jsonb_typeof(safe_locator_json) = 'object'),
    display_name varchar(500),
    detected_type_code varchar(20) CHECK (detected_type_code IN ('PDF','HWP','HWPX')),
    document_role_code varchar(20) NOT NULL DEFAULT 'UNKNOWN'
        CHECK (document_role_code IN ('NOTICE','GUIDE','FORM','REFERENCE','UNKNOWN')),
    role_origin_code varchar(20) NOT NULL DEFAULT 'UNKNOWN'
        CHECK (role_origin_code IN ('UNKNOWN','PROFILE','MANUAL')),
    download_status_code varchar(20) NOT NULL DEFAULT 'PENDING'
        CHECK (download_status_code IN ('PENDING','SUCCEEDED','FAILED','BLOCKED','CANCELLED')),
    downloaded_bytes bigint NOT NULL DEFAULT 0 CHECK (downloaded_bytes >= 0),
    binary_hash varchar(64) CHECK (binary_hash ~ '^[0-9a-f]{64}$'),
    sort_order integer NOT NULL CHECK (sort_order >= 0),
    error_code varchar(80),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_file_set FOREIGN KEY (set_id,source_id)
        REFERENCES announcement_source_attachment_sets(id,source_id) ON DELETE CASCADE,
    CONSTRAINT uq_att_file_locator UNIQUE (set_id,stable_locator_hash),
    CONSTRAINT uq_att_file_binding UNIQUE (id,set_id,source_id),
    CHECK (download_status_code <> 'SUCCEEDED' OR (binary_hash IS NOT NULL AND downloaded_bytes > 0))
);
CREATE INDEX ix_att_file_set ON announcement_source_attachment_files(set_id,source_id,download_status_code);

CREATE TABLE announcement_source_attachment_extractions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id uuid NOT NULL,
    set_id uuid NOT NULL,
    source_id uuid NOT NULL,
    attempt_no integer NOT NULL CHECK (attempt_no BETWEEN 1 AND 3),
    extractor_code varchar(40) NOT NULL,
    extractor_version varchar(40) NOT NULL,
    extractor_config_hash varchar(64) NOT NULL CHECK (extractor_config_hash ~ '^[0-9a-f]{64}$'),
    quality_code varchar(30) NOT NULL CHECK (quality_code IN (
        'COMPLETE_TEXT','PARTIAL_TEXT','OCR_REQUIRED','ENCRYPTED','CORRUPT','UNSUPPORTED',
        'LIMIT_EXCEEDED','TIMEOUT','FAILED','ISOLATION_UNAVAILABLE')),
    extracted_text text,
    text_hash varchar(64) CHECK (text_hash ~ '^[0-9a-f]{64}$'),
    blocks_json jsonb NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(blocks_json) = 'array'),
    character_count integer NOT NULL DEFAULT 0 CHECK (character_count BETWEEN 0 AND 1000000),
    page_count integer CHECK (page_count BETWEEN 0 AND 200),
    duration_ms integer NOT NULL CHECK (duration_ms >= 0),
    error_code varchar(80),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_extract_file FOREIGN KEY (file_id,set_id,source_id)
        REFERENCES announcement_source_attachment_files(id,set_id,source_id) ON DELETE CASCADE,
    CONSTRAINT uq_att_extract_attempt UNIQUE (file_id,attempt_no),
    CONSTRAINT uq_att_extract_binding UNIQUE (id,file_id,set_id,source_id),
    CHECK (quality_code <> 'COMPLETE_TEXT' OR (text_hash IS NOT NULL
        AND length(btrim(extracted_text)) > 0 AND character_count > 0 AND jsonb_array_length(blocks_json) > 0)),
    CHECK ((extracted_text IS NULL AND character_count = 0 AND text_hash IS NULL)
        OR (extracted_text IS NOT NULL AND text_hash IS NOT NULL AND character_count = char_length(extracted_text)))
);

CREATE TABLE announcement_source_attachment_evaluations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id uuid NOT NULL REFERENCES announcement_source_snapshots(id) ON DELETE CASCADE,
    content_version_id uuid NOT NULL,
    base_evaluation_id uuid NOT NULL,
    set_id uuid NOT NULL,
    policy_id uuid NOT NULL,
    rule_release_id uuid NOT NULL,
    engine_version varchar(40) NOT NULL,
    input_hash varchar(64) NOT NULL CHECK (input_hash ~ '^[0-9a-f]{64}$'),
    decision_hash varchar(64) NOT NULL CHECK (decision_hash ~ '^[0-9a-f]{64}$'),
    decision_status_code varchar(30) NOT NULL CHECK (decision_status_code IN ('ACCEPTED','REVIEW_REQUIRED')),
    reason_code varchar(80) NOT NULL,
    warning_codes_json jsonb NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(warning_codes_json) = 'array'),
    is_current boolean NOT NULL DEFAULT false,
    evaluated_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_eval_base FOREIGN KEY (base_evaluation_id,source_id,content_version_id,rule_release_id)
        REFERENCES announcement_source_classification_evaluations(id,source_id,content_version_id,rule_release_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_att_eval_set FOREIGN KEY (set_id,source_id,content_version_id,policy_id)
        REFERENCES announcement_source_attachment_sets(id,source_id,content_version_id,policy_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_eval_policy FOREIGN KEY (policy_id,rule_release_id)
        REFERENCES announcement_attachment_policies(id,rule_release_id),
    CONSTRAINT uq_att_eval_input UNIQUE (source_id,input_hash,engine_version),
    CONSTRAINT uq_att_eval_source UNIQUE (id,source_id),
    CONSTRAINT uq_att_eval_set UNIQUE (id,set_id,source_id),
    CONSTRAINT uq_att_eval_release UNIQUE (id,rule_release_id)
);
CREATE UNIQUE INDEX uq_att_eval_current ON announcement_source_attachment_evaluations(source_id) WHERE is_current;
CREATE INDEX ix_att_eval_base ON announcement_source_attachment_evaluations(base_evaluation_id,source_id,content_version_id,rule_release_id);
CREATE INDEX ix_att_eval_set ON announcement_source_attachment_evaluations(set_id,source_id,content_version_id,policy_id);
CREATE INDEX ix_att_eval_policy ON announcement_source_attachment_evaluations(policy_id,rule_release_id);

CREATE TABLE announcement_source_attachment_evaluation_inputs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_id uuid NOT NULL,
    source_id uuid NOT NULL,
    set_id uuid NOT NULL,
    file_id uuid NOT NULL,
    extraction_id uuid,
    document_role_code varchar(20) NOT NULL CHECK (document_role_code IN ('NOTICE','GUIDE','FORM','REFERENCE','UNKNOWN')),
    input_status_code varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_input_eval FOREIGN KEY (evaluation_id,set_id,source_id)
        REFERENCES announcement_source_attachment_evaluations(id,set_id,source_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_input_file FOREIGN KEY (file_id,set_id,source_id)
        REFERENCES announcement_source_attachment_files(id,set_id,source_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_input_extract FOREIGN KEY (extraction_id,file_id,set_id,source_id)
        REFERENCES announcement_source_attachment_extractions(id,file_id,set_id,source_id) ON DELETE CASCADE,
    CONSTRAINT uq_att_input_file UNIQUE (evaluation_id,file_id),
    CONSTRAINT uq_att_input_binding UNIQUE (evaluation_id,file_id,extraction_id,set_id,source_id)
);
CREATE INDEX ix_att_input_file ON announcement_source_attachment_evaluation_inputs(file_id,set_id,source_id);
CREATE INDEX ix_att_input_extract ON announcement_source_attachment_evaluation_inputs(extraction_id,file_id,set_id,source_id);

CREATE TABLE announcement_source_attachment_matches (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_id uuid NOT NULL,
    source_id uuid NOT NULL,
    set_id uuid NOT NULL,
    file_id uuid NOT NULL,
    extraction_id uuid NOT NULL,
    rule_release_id uuid NOT NULL,
    keyword_group_id uuid NOT NULL,
    keyword_rule_id uuid NOT NULL,
    keyword_term_id uuid NOT NULL,
    block_index integer NOT NULL CHECK (block_index >= 0),
    start_offset integer NOT NULL CHECK (start_offset >= 0),
    end_offset integer NOT NULL CHECK (end_offset > start_offset),
    applied_action_code varchar(30) NOT NULL CHECK (applied_action_code IN ('TAG','REVIEW_REQUIRED','CONTEXT_ONLY','MASK_ONLY')),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_match_input FOREIGN KEY (evaluation_id,file_id,extraction_id,set_id,source_id)
        REFERENCES announcement_source_attachment_evaluation_inputs(evaluation_id,file_id,extraction_id,set_id,source_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_att_match_release FOREIGN KEY (evaluation_id,rule_release_id)
        REFERENCES announcement_source_attachment_evaluations(id,rule_release_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_match_group FOREIGN KEY (keyword_group_id,rule_release_id)
        REFERENCES announcement_source_classification_rule_groups(id,release_id),
    CONSTRAINT fk_att_match_rule FOREIGN KEY (keyword_rule_id,keyword_group_id)
        REFERENCES announcement_source_classification_keyword_rules(id,group_id),
    CONSTRAINT fk_att_match_term FOREIGN KEY (keyword_term_id,keyword_rule_id)
        REFERENCES announcement_source_classification_keyword_terms(id,keyword_rule_id),
    CONSTRAINT uq_att_match_position UNIQUE (evaluation_id,file_id,keyword_term_id,start_offset,end_offset)
);
CREATE INDEX ix_att_match_input ON announcement_source_attachment_matches(evaluation_id,file_id,extraction_id,set_id,source_id);
CREATE INDEX ix_att_match_release ON announcement_source_attachment_matches(evaluation_id,rule_release_id);
CREATE INDEX ix_att_match_group ON announcement_source_attachment_matches(keyword_group_id,rule_release_id);
CREATE INDEX ix_att_match_rule ON announcement_source_attachment_matches(keyword_rule_id,keyword_group_id);
CREATE INDEX ix_att_match_term ON announcement_source_attachment_matches(keyword_term_id,keyword_rule_id);

CREATE TABLE announcement_source_attachment_confirmations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id uuid NOT NULL,
    evaluation_id uuid NOT NULL,
    set_hash varchar(64) NOT NULL CHECK (set_hash ~ '^[0-9a-f]{64}$'),
    confirmed_by uuid NOT NULL REFERENCES users(id),
    confirmed_at timestamptz NOT NULL DEFAULT now(),
    review_method_code varchar(30) NOT NULL CHECK (review_method_code IN ('EXTRACTED_TEXT','MANUAL_SOURCE_CHECK')),
    acknowledged_error_codes_json jsonb NOT NULL CHECK (jsonb_typeof(acknowledged_error_codes_json) = 'array'),
    review_note varchar(1000) NOT NULL CHECK (length(btrim(review_note)) > 0),
    is_current boolean NOT NULL DEFAULT true,
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_confirm_eval FOREIGN KEY (evaluation_id,source_id)
        REFERENCES announcement_source_attachment_evaluations(id,source_id) ON DELETE CASCADE,
    CONSTRAINT uq_att_confirm_eval UNIQUE (id,evaluation_id),
    CONSTRAINT uq_att_confirm_source UNIQUE (id,source_id)
);
CREATE UNIQUE INDEX uq_att_confirm_current ON announcement_source_attachment_confirmations(source_id) WHERE is_current;
CREATE INDEX ix_att_confirm_eval ON announcement_source_attachment_confirmations(evaluation_id,source_id);
CREATE INDEX ix_att_confirm_actor ON announcement_source_attachment_confirmations(confirmed_by);

CREATE TABLE announcement_source_attachment_tags (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_id uuid NOT NULL REFERENCES announcement_source_attachment_evaluations(id) ON DELETE CASCADE,
    confirmation_id uuid,
    target_category_id uuid REFERENCES announcement_target_categories(id),
    support_type_id uuid REFERENCES announcement_support_types(id),
    origin_code varchar(20) NOT NULL CHECK (origin_code IN ('AUTO','CONFIRMED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_tag_confirm FOREIGN KEY (confirmation_id,evaluation_id)
        REFERENCES announcement_source_attachment_confirmations(id,evaluation_id) ON DELETE CASCADE,
    CHECK ((target_category_id IS NULL) <> (support_type_id IS NULL)),
    CHECK ((origin_code = 'AUTO' AND confirmation_id IS NULL) OR (origin_code = 'CONFIRMED' AND confirmation_id IS NOT NULL))
);
CREATE UNIQUE INDEX uq_att_tag_auto_target ON announcement_source_attachment_tags(evaluation_id,target_category_id) WHERE origin_code='AUTO';
CREATE UNIQUE INDEX uq_att_tag_auto_support ON announcement_source_attachment_tags(evaluation_id,support_type_id) WHERE origin_code='AUTO';
CREATE UNIQUE INDEX uq_att_tag_confirm_target ON announcement_source_attachment_tags(confirmation_id,target_category_id) WHERE origin_code='CONFIRMED';
CREATE UNIQUE INDEX uq_att_tag_confirm_support ON announcement_source_attachment_tags(confirmation_id,support_type_id) WHERE origin_code='CONFIRMED';
CREATE INDEX ix_att_tag_confirm ON announcement_source_attachment_tags(confirmation_id,evaluation_id);
CREATE INDEX ix_att_tag_target ON announcement_source_attachment_tags(target_category_id);
CREATE INDEX ix_att_tag_support ON announcement_source_attachment_tags(support_type_id);

ALTER TABLE announcement_source_snapshots
    ADD COLUMN attachment_policy_id uuid REFERENCES announcement_attachment_policies(id),
    ADD COLUMN is_attachment_review_required boolean NOT NULL DEFAULT false,
    ADD COLUMN attachment_row_version integer NOT NULL DEFAULT 0 CHECK (attachment_row_version >= 0),
    ADD COLUMN current_attachment_evaluation_id uuid,
    ADD CONSTRAINT fk_att_source_current FOREIGN KEY (current_attachment_evaluation_id,id)
        REFERENCES announcement_source_attachment_evaluations(id,source_id) DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT ck_att_source_policy CHECK (NOT is_attachment_review_required OR attachment_policy_id IS NOT NULL);
CREATE INDEX ix_att_source_policy ON announcement_source_snapshots(attachment_policy_id);
CREATE INDEX ix_att_source_current ON announcement_source_snapshots(current_attachment_evaluation_id,id);

CREATE TABLE announcement_attachment_jobs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id uuid NOT NULL REFERENCES announcement_source_snapshots(id) ON DELETE CASCADE,
    content_version_id uuid NOT NULL,
    base_evaluation_id uuid NOT NULL,
    rule_release_id uuid NOT NULL,
    policy_id uuid NOT NULL,
    batch_id uuid REFERENCES announcement_attachment_batches(id),
    set_id uuid,
    generation integer NOT NULL CHECK (generation > 0),
    expected_source_version integer NOT NULL CHECK (expected_source_version >= 0),
    expected_attachment_version integer NOT NULL CHECK (expected_attachment_version >= 0),
    job_status_code varchar(30) NOT NULL DEFAULT 'SCOPE_READY' CHECK (job_status_code IN (
        'SCOPE_READY','PENDING','RUNNING','RETRY_WAIT','SUCCEEDED','PARTIAL_FAILED','FAILED','CONFLICT','CANCELLED','PAUSED')),
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count BETWEEN 0 AND 3),
    next_attempt_at timestamptz,
    lease_token uuid,
    lease_expires_at timestamptz,
    heartbeat_at timestamptz,
    error_code varchar(80),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    preview_evaluation_id uuid,
    preview_hash varchar(64) CHECK (preview_hash ~ '^[0-9a-f]{64}$'),
    previous_attachment_evaluation_id uuid,
    previous_confirmation_id uuid,
    previous_policy_id uuid REFERENCES announcement_attachment_policies(id),
    previous_is_review_required boolean,
    applied_evaluation_id uuid,
    applied_attachment_version integer CHECK (applied_attachment_version >= 0),
    application_status_code varchar(30) NOT NULL DEFAULT 'NOT_REQUESTED'
        CHECK (application_status_code IN ('NOT_REQUESTED','PENDING','APPLIED','CONFLICT','FAILED')),
    rollback_status_code varchar(30) NOT NULL DEFAULT 'NOT_REQUESTED'
        CHECK (rollback_status_code IN ('NOT_REQUESTED','ROLLED_BACK','CONFLICT','FAILED')),
    is_selected_for_application boolean NOT NULL DEFAULT false,
    row_version integer NOT NULL DEFAULT 0 CHECK (row_version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_job_base FOREIGN KEY (base_evaluation_id,source_id,content_version_id,rule_release_id)
        REFERENCES announcement_source_classification_evaluations(id,source_id,content_version_id,rule_release_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_job_policy FOREIGN KEY (policy_id,rule_release_id)
        REFERENCES announcement_attachment_policies(id,rule_release_id),
    CONSTRAINT fk_att_job_set FOREIGN KEY (set_id,source_id,content_version_id,policy_id)
        REFERENCES announcement_source_attachment_sets(id,source_id,content_version_id,policy_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_att_job_preview FOREIGN KEY (preview_evaluation_id,source_id)
        REFERENCES announcement_source_attachment_evaluations(id,source_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_att_job_previous FOREIGN KEY (previous_attachment_evaluation_id,source_id)
        REFERENCES announcement_source_attachment_evaluations(id,source_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_att_job_confirmation FOREIGN KEY (previous_confirmation_id,source_id)
        REFERENCES announcement_source_attachment_confirmations(id,source_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_att_job_applied FOREIGN KEY (applied_evaluation_id,source_id)
        REFERENCES announcement_source_attachment_evaluations(id,source_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT uq_att_job_generation UNIQUE (source_id,content_version_id,policy_id,generation),
    CHECK (job_status_code <> 'RUNNING' OR (lease_token IS NOT NULL AND lease_expires_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_att_job_active ON announcement_attachment_jobs(source_id)
    WHERE job_status_code IN ('SCOPE_READY','PENDING','RUNNING','RETRY_WAIT','PAUSED');
CREATE INDEX ix_att_job_queue ON announcement_attachment_jobs(job_status_code,next_attempt_at,id);
CREATE INDEX ix_att_job_batch ON announcement_attachment_jobs(batch_id,source_id);
CREATE INDEX ix_att_job_base ON announcement_attachment_jobs(base_evaluation_id,source_id,content_version_id,rule_release_id);
CREATE INDEX ix_att_job_policy ON announcement_attachment_jobs(policy_id,rule_release_id);
CREATE INDEX ix_att_job_set ON announcement_attachment_jobs(set_id,source_id,content_version_id,policy_id);
CREATE INDEX ix_att_job_preview ON announcement_attachment_jobs(preview_evaluation_id,source_id);
CREATE INDEX ix_att_job_previous ON announcement_attachment_jobs(previous_attachment_evaluation_id,source_id);
CREATE INDEX ix_att_job_confirmation ON announcement_attachment_jobs(previous_confirmation_id,source_id);
CREATE INDEX ix_att_job_applied ON announcement_attachment_jobs(applied_evaluation_id,source_id);
CREATE INDEX ix_att_job_previous_policy ON announcement_attachment_jobs(previous_policy_id);

-- pointer와 current 플래그는 같은 transaction의 최종 상태에서 일치해야 한다.
CREATE FUNCTION check_attachment_current_binding() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE source_key uuid;
BEGIN
    IF TG_TABLE_NAME = 'announcement_source_snapshots' THEN
        source_key := COALESCE(NEW.id,OLD.id);
    ELSE
        source_key := COALESCE(NEW.source_id,OLD.source_id);
    END IF;
    IF EXISTS (
        SELECT 1 FROM announcement_source_snapshots s
        LEFT JOIN announcement_source_attachment_evaluations e ON e.source_id=s.id AND e.is_current
        WHERE s.id=source_key AND s.current_attachment_evaluation_id IS DISTINCT FROM e.id
    ) THEN
        RAISE EXCEPTION 'attachment current binding mismatch' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_source_current AFTER INSERT OR UPDATE OR DELETE ON announcement_source_snapshots
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_current_binding();
CREATE CONSTRAINT TRIGGER ct_att_eval_current AFTER INSERT OR UPDATE OR DELETE ON announcement_source_attachment_evaluations
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_current_binding();

-- QA/운영 목적이 다른 근거 연결을 거부한다.
CREATE FUNCTION check_attachment_set_purpose() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM announcement_source_snapshots s
        WHERE s.id=NEW.source_id AND s.data_purpose_code=NEW.data_purpose_code) THEN
        RAISE EXCEPTION 'attachment source purpose mismatch' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_set_purpose BEFORE INSERT OR UPDATE ON announcement_source_attachment_sets
    FOR EACH ROW EXECUTE FUNCTION check_attachment_set_purpose();

-- 봉인 이후 근거/역할을 수정하지 않는다. source 삭제에 따른 cascade만 허용한다.
CREATE FUNCTION protect_attachment_evidence() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE set_key uuid; source_key uuid; sealed boolean;
BEGIN
    IF TG_TABLE_NAME = 'announcement_source_attachment_sets' THEN
        set_key := OLD.id; source_key := OLD.source_id;
        sealed := OLD.set_status_code = 'SEALED';
        IF TG_OP = 'UPDATE' AND NEW.set_status_code = 'SEALED' AND NOT sealed THEN
            IF NEW.discovered_count <> (SELECT count(1) FROM announcement_source_attachment_files WHERE set_id=NEW.id)
                OR EXISTS (SELECT 1 FROM announcement_source_attachment_files f WHERE f.set_id=NEW.id
                    AND (f.download_status_code='PENDING' OR (f.download_status_code='SUCCEEDED'
                        AND NOT EXISTS (SELECT 1 FROM announcement_source_attachment_extractions x WHERE x.file_id=f.id)))) THEN
                RAISE EXCEPTION 'attachment set contains unfinished files' USING ERRCODE='23514';
            END IF;
        END IF;
    ELSE
        set_key := COALESCE(NEW.set_id,OLD.set_id);
        source_key := COALESCE(NEW.source_id,OLD.source_id);
        SELECT set_status_code = 'SEALED' INTO sealed FROM announcement_source_attachment_sets WHERE id=set_key;
    END IF;
    IF TG_TABLE_NAME = 'announcement_source_attachment_extractions' AND TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'attachment extraction is immutable' USING ERRCODE='23514';
    END IF;
    IF sealed AND EXISTS (SELECT 1 FROM announcement_source_snapshots WHERE id=source_key) THEN
        RAISE EXCEPTION 'sealed attachment evidence is immutable' USING ERRCODE='23514';
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_set_immutable BEFORE UPDATE OR DELETE ON announcement_source_attachment_sets
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_evidence();
CREATE TRIGGER tr_att_file_immutable BEFORE INSERT OR UPDATE OR DELETE ON announcement_source_attachment_files
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_evidence();
CREATE TRIGGER tr_att_extract_immutable BEFORE INSERT OR UPDATE OR DELETE ON announcement_source_attachment_extractions
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_evidence();

CREATE FUNCTION protect_attachment_policy() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.policy_status_code <> 'DRAFT' AND
        (to_jsonb(NEW) - 'policy_status_code' - 'row_version' - 'updated_at') IS DISTINCT FROM
        (to_jsonb(OLD) - 'policy_status_code' - 'row_version' - 'updated_at') THEN
        RAISE EXCEPTION 'published attachment policy is immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.policy_status_code = 'RETIRED' OR (OLD.policy_status_code = 'ACTIVE' AND NEW.policy_status_code <> 'RETIRED') THEN
        RAISE EXCEPTION 'attachment policy transition is invalid' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_policy_immutable BEFORE UPDATE ON announcement_attachment_policies
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_policy();

CREATE FUNCTION check_attachment_evaluation_sealed() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM announcement_source_attachment_sets WHERE id=NEW.set_id AND set_status_code='SEALED') THEN
        RAISE EXCEPTION 'attachment evaluation requires a sealed set' USING ERRCODE='23514';
    END IF;
    IF TG_OP='UPDATE' AND (to_jsonb(NEW) - 'is_current') IS DISTINCT FROM (to_jsonb(OLD) - 'is_current') THEN
        RAISE EXCEPTION 'attachment evaluation is immutable' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_eval_sealed BEFORE INSERT OR UPDATE ON announcement_source_attachment_evaluations
    FOR EACH ROW EXECUTE FUNCTION check_attachment_evaluation_sealed();

CREATE FUNCTION check_attachment_source_purpose() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.data_purpose_code IS DISTINCT FROM NEW.data_purpose_code AND EXISTS (
        SELECT 1 FROM announcement_source_attachment_sets WHERE source_id=NEW.id AND data_purpose_code<>NEW.data_purpose_code) THEN
        RAISE EXCEPTION 'attachment source purpose mismatch' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_source_purpose BEFORE UPDATE OF data_purpose_code ON announcement_source_snapshots
    FOR EACH ROW EXECUTE FUNCTION check_attachment_source_purpose();
