-- 실행 식별자는 예약 시 고정한다. 기존 작업은 값을 추측하지 않고 미고정 상태로 보존한다.
ALTER TABLE announcement_attachment_jobs
    ADD COLUMN execution_snapshot_json jsonb
        CHECK (execution_snapshot_json IS NULL OR jsonb_typeof(execution_snapshot_json) = 'object'),
    ADD COLUMN download_budget_bytes bigint NOT NULL DEFAULT 83886080
        CHECK (download_budget_bytes BETWEEN 1 AND 83886080),
    ADD COLUMN reserved_download_bytes bigint NOT NULL DEFAULT 0
        CHECK (reserved_download_bytes >= 0),
    ADD CONSTRAINT ck_att_job_byte_budget CHECK (reserved_download_bytes <= download_budget_bytes);

-- 역할 변경은 이미 검증된 동일 source 근거만 재사용하며 외부 수집 작업과 구분한다.
ALTER TABLE announcement_attachment_jobs
    ADD COLUMN operation_code varchar(20) NOT NULL DEFAULT 'COLLECT' CHECK (operation_code IN ('COLLECT','ROLE_CHANGE','RETRY_FILES')),
    ADD COLUMN reference_set_id uuid,
    ADD COLUMN requested_by uuid REFERENCES users(id),
    ADD CONSTRAINT fk_att_job_reference_set FOREIGN KEY (reference_set_id,source_id,content_version_id,policy_id)
        REFERENCES announcement_source_attachment_sets(id,source_id,content_version_id,policy_id) DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT ck_att_job_role_operation CHECK (
        (operation_code='COLLECT' AND reference_set_id IS NULL) OR
        (operation_code='ROLE_CHANGE' AND reference_set_id IS NOT NULL AND requested_by IS NOT NULL
         AND set_id IS NOT NULL AND set_id<>reference_set_id AND reserved_download_bytes=0) OR
        (operation_code='RETRY_FILES' AND reference_set_id IS NOT NULL AND requested_by IS NOT NULL
         AND (set_id IS NULL OR set_id<>reference_set_id)));
CREATE INDEX ix_att_job_reference_set ON announcement_attachment_jobs(reference_set_id,source_id) WHERE reference_set_id IS NOT NULL;
CREATE INDEX ix_att_job_requested_by ON announcement_attachment_jobs(requested_by) WHERE requested_by IS NOT NULL;
CREATE FUNCTION check_attachment_role_job_sets() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.operation_code='RETRY_FILES' AND NOT EXISTS (
        SELECT 1 FROM announcement_source_attachment_sets original
        WHERE original.id=NEW.reference_set_id AND original.source_id=NEW.source_id
          AND original.content_version_id=NEW.content_version_id AND original.policy_id=NEW.policy_id
          AND original.profile_hash=NEW.execution_snapshot_json->>'profileHash' AND original.set_status_code='SEALED') THEN
        RAISE EXCEPTION 'attachment retry requires same-source sealed reference' USING ERRCODE='23514';
    END IF;
    IF (NEW.operation_code='ROLE_CHANGE' OR (NEW.operation_code='RETRY_FILES' AND NEW.set_id IS NOT NULL)) AND NOT EXISTS (
        SELECT 1 FROM announcement_source_attachment_sets original
        JOIN announcement_source_attachment_sets replacement
          ON replacement.source_id=original.source_id AND replacement.content_version_id=original.content_version_id
         AND replacement.policy_id=original.policy_id AND replacement.profile_hash=original.profile_hash
        WHERE original.id=NEW.reference_set_id AND replacement.id=NEW.set_id AND original.source_id=NEW.source_id
          AND original.set_status_code='SEALED' AND replacement.set_status_code='SEALED') THEN
        RAISE EXCEPTION 'attachment role job requires sealed same-source evidence' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_role_job_sets BEFORE INSERT OR UPDATE OF set_id,reference_set_id,operation_code ON announcement_attachment_jobs
    FOR EACH ROW EXECUTE FUNCTION check_attachment_role_job_sets();

-- 근거 재사용은 새 extraction 행으로 식별하되 실제 추출 시각/결과를 보존한다.
ALTER TABLE announcement_source_attachment_extractions
    ADD COLUMN reused_from_extraction_id uuid,
    ADD CONSTRAINT uq_att_extraction_source UNIQUE (id,source_id),
    ADD CONSTRAINT fk_att_extraction_reused FOREIGN KEY (reused_from_extraction_id,source_id)
        REFERENCES announcement_source_attachment_extractions(id,source_id) DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX ix_att_extraction_reused ON announcement_source_attachment_extractions(reused_from_extraction_id,source_id)
    WHERE reused_from_extraction_id IS NOT NULL;
CREATE FUNCTION check_attachment_extraction_reuse() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.reused_from_extraction_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM announcement_source_attachment_extractions original
        JOIN announcement_source_attachment_files original_file ON original_file.id=original.file_id AND original_file.source_id=original.source_id
        JOIN announcement_source_attachment_files destination_file ON destination_file.id=NEW.file_id AND destination_file.source_id=NEW.source_id
        WHERE original.id=NEW.reused_from_extraction_id AND original.source_id=NEW.source_id
          AND original.id<>NEW.id AND original.file_id<>NEW.file_id
          AND original_file.binary_hash=destination_file.binary_hash
          AND (original.extractor_code,original.extractor_version,original.extractor_config_hash,original.quality_code,
               original.extracted_text,original.text_hash,original.blocks_json,original.character_count,original.page_count,
               original.duration_ms,original.error_code,original.created_at)
              IS NOT DISTINCT FROM
              (NEW.extractor_code,NEW.extractor_version,NEW.extractor_config_hash,NEW.quality_code,
               NEW.extracted_text,NEW.text_hash,NEW.blocks_json,NEW.character_count,NEW.page_count,
               NEW.duration_ms,NEW.error_code,NEW.created_at)) THEN
        RAISE EXCEPTION 'attachment reuse requires same source binary and unchanged extraction' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_extraction_reuse BEFORE INSERT ON announcement_source_attachment_extractions
    FOR EACH ROW EXECUTE FUNCTION check_attachment_extraction_reuse();

-- 발견 실패 이유는 외부 예외/URL 대신 제한된 코드 목록으로 분리하여 봉인한다.
ALTER TABLE announcement_source_attachment_sets
    ADD COLUMN discovery_warning_codes_json jsonb NOT NULL DEFAULT '[]'::jsonb
        CHECK (jsonb_typeof(discovery_warning_codes_json)='array' AND jsonb_array_length(discovery_warning_codes_json)<=20);

-- 관리자 초안 생성의 멱등성과 개정 계보는 게시 이후에도 불변이다. 기존 정책은 NULL로 보존한다.
ALTER TABLE announcement_attachment_policies
    ADD COLUMN creation_idempotency_key uuid UNIQUE,
    ADD COLUMN creation_request_hash varchar(64),
    ADD COLUMN creation_operation_code varchar(20),
    ADD COLUMN copied_from_policy_id uuid REFERENCES announcement_attachment_policies(id),
    ADD CONSTRAINT ck_att_policy_creation CHECK (
        (creation_idempotency_key IS NULL AND creation_request_hash IS NULL AND creation_operation_code IS NULL AND copied_from_policy_id IS NULL)
        OR (creation_idempotency_key IS NOT NULL AND creation_request_hash IS NOT NULL
            AND creation_request_hash ~ '^[0-9a-f]{64}$' AND creation_operation_code IS NOT NULL
            AND ((creation_operation_code='CREATE' AND copied_from_policy_id IS NULL)
                OR (creation_operation_code='REVISION' AND copied_from_policy_id IS NOT NULL AND copied_from_policy_id<>id)))
    );
CREATE INDEX ix_att_policy_parent ON announcement_attachment_policies(copied_from_policy_id);
CREATE INDEX ix_att_policy_list ON announcement_attachment_policies(created_at DESC,id DESC);
CREATE FUNCTION check_attachment_policy_revision_parent() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.creation_operation_code='REVISION' AND NOT EXISTS (
        SELECT 1 FROM announcement_attachment_policies parent
        WHERE parent.id=NEW.copied_from_policy_id AND parent.policy_code=NEW.policy_code
          AND parent.version_no<NEW.version_no) THEN
        RAISE EXCEPTION 'attachment revision requires same family and later version' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_policy_revision_parent BEFORE INSERT ON announcement_attachment_policies
    FOR EACH ROW EXECUTE FUNCTION check_attachment_policy_revision_parent();
CREATE FUNCTION protect_attachment_policy_draft_identity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.id,NEW.policy_code,NEW.version_no,NEW.created_by,NEW.created_at,
        NEW.creation_idempotency_key,NEW.creation_request_hash,NEW.creation_operation_code,NEW.copied_from_policy_id)
       IS DISTINCT FROM
       (OLD.id,OLD.policy_code,OLD.version_no,OLD.created_by,OLD.created_at,
        OLD.creation_idempotency_key,OLD.creation_request_hash,OLD.creation_operation_code,OLD.copied_from_policy_id) THEN
        RAISE EXCEPTION 'attachment policy identity and creation request are immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.creation_idempotency_key IS NOT NULL AND NEW.row_version<>OLD.row_version+1 THEN
        RAISE EXCEPTION 'managed attachment policy version must advance once' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_policy_draft_identity BEFORE UPDATE ON announcement_attachment_policies
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_policy_draft_identity();

-- 분류 정답 세트의 성공은 전체 QA/게시 승인과 분리한다. 버전 고정 이력만 저장한다.
CREATE TABLE announcement_attachment_policy_checks (
    id uuid PRIMARY KEY,
    policy_id uuid NOT NULL REFERENCES announcement_attachment_policies(id),
    policy_row_version integer NOT NULL CHECK (policy_row_version>=0),
    policy_snapshot_hash varchar(64) NOT NULL CHECK (policy_snapshot_hash ~ '^[0-9a-f]{64}$'),
    rule_release_id uuid NOT NULL REFERENCES announcement_source_classification_rule_releases(id),
    rule_row_version integer NOT NULL CHECK (rule_row_version>=0),
    rule_snapshot_hash varchar(64) NOT NULL CHECK (rule_snapshot_hash ~ '^[0-9a-f]{64}$'),
    rule_content_hash varchar(64) NOT NULL CHECK (rule_content_hash ~ '^[0-9a-f]{64}$'),
    check_type_code varchar(30) NOT NULL CHECK (check_type_code='CLASSIFICATION_GOLDEN'),
    suite_version varchar(40) NOT NULL,
    engine_version varchar(40) NOT NULL,
    result_hash varchar(64) NOT NULL CHECK (result_hash ~ '^[0-9a-f]{64}$'),
    case_count integer NOT NULL CHECK (case_count BETWEEN 1 AND 1000),
    case_ids_json jsonb NOT NULL CHECK (jsonb_typeof(case_ids_json)='array' AND jsonb_array_length(case_ids_json)=case_count),
    requested_by uuid NOT NULL REFERENCES users(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_att_policy_check_history ON announcement_attachment_policy_checks(policy_id,created_at DESC,id DESC);
CREATE INDEX ix_att_policy_check_rule ON announcement_attachment_policy_checks(rule_release_id);
CREATE INDEX ix_att_policy_check_actor ON announcement_attachment_policy_checks(requested_by);
CREATE FUNCTION check_attachment_policy_check_binding() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM announcement_attachment_policies p
        JOIN announcement_source_classification_rule_releases r ON r.id=p.rule_release_id
        WHERE p.id=NEW.policy_id AND p.policy_status_code='DRAFT' AND p.row_version=NEW.policy_row_version
          AND r.id=NEW.rule_release_id AND r.row_version=NEW.rule_row_version AND r.release_status_code IN ('DRAFT','ACTIVE')) THEN
        RAISE EXCEPTION 'attachment policy check must bind current draft and rule version' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_policy_check_binding BEFORE INSERT ON announcement_attachment_policy_checks
    FOR EACH ROW EXECUTE FUNCTION check_attachment_policy_check_binding();
CREATE FUNCTION protect_attachment_policy_check() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'attachment policy check is immutable' USING ERRCODE='23514';
END $$;
CREATE TRIGGER tr_att_policy_check_immutable BEFORE UPDATE ON announcement_attachment_policy_checks
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_policy_check();

-- 수집 시작 시 선택한 정책은 같은 run의 항목 처리 도중 교체하지 않는다.
CREATE TABLE announcement_attachment_collection_plans (
    run_id uuid PRIMARY KEY REFERENCES announcement_source_collection_runs(id) ON DELETE CASCADE,
    rule_release_id uuid NOT NULL REFERENCES announcement_source_classification_rule_releases(id),
    policy_id uuid REFERENCES announcement_attachment_policies(id),
    plan_status_code varchar(20) NOT NULL CHECK (plan_status_code IN ('FROZEN','OFF','NO_POLICY')),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_att_collection_plan_binding UNIQUE (run_id,policy_id,rule_release_id),
    CHECK ((plan_status_code='NO_POLICY' AND policy_id IS NULL) OR (plan_status_code<>'NO_POLICY' AND policy_id IS NOT NULL))
);
CREATE INDEX ix_att_collection_plan_policy ON announcement_attachment_collection_plans(policy_id,rule_release_id);
CREATE FUNCTION protect_attachment_collection_plan() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'attachment collection plan is immutable' USING ERRCODE='23514';
END $$;
CREATE TRIGGER tr_att_collection_plan BEFORE UPDATE ON announcement_attachment_collection_plans
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_collection_plan();
ALTER TABLE announcement_attachment_jobs
    ADD COLUMN collection_run_id uuid,
    ADD CONSTRAINT fk_att_job_collection_plan FOREIGN KEY (collection_run_id,policy_id,rule_release_id)
        REFERENCES announcement_attachment_collection_plans(run_id,policy_id,rule_release_id);
CREATE INDEX ix_att_job_collection_plan ON announcement_attachment_jobs(collection_run_id,policy_id,rule_release_id);
ALTER TABLE announcement_source_snapshots
    ADD COLUMN attachment_intake_status_code varchar(40) NOT NULL DEFAULT 'NOT_REQUESTED'
        CHECK (attachment_intake_status_code IN ('NOT_REQUESTED','QUEUED','PROFILE_REQUIRED',
            'BASE_RECLASSIFICATION_REQUIRED','POLICY_BINDING_CHANGED','PROTECTED_LINK','RECHECK_NOT_DUE'));
-- 현재 일반 작업 조회와 자동 재수집 간격 확인을 source 범위로 제한한다.
CREATE INDEX ix_att_job_source_recent ON announcement_attachment_jobs(source_id,created_at DESC) WHERE batch_id IS NULL;
CREATE INDEX ix_att_job_source_version ON announcement_attachment_jobs(source_id,expected_attachment_version DESC) WHERE batch_id IS NULL;

-- 모든 worker가 같은 슬롯을 임대한다. 기관 식별자는 host의 SHA-256이며 URL 원문이 아니다.
CREATE TABLE announcement_attachment_resource_leases (
    resource_code varchar(20) NOT NULL CHECK (resource_code IN ('DOWNLOAD','HOST','EXTRACTION')),
    resource_key varchar(64) NOT NULL,
    slot_no integer NOT NULL,
    job_id uuid NOT NULL REFERENCES announcement_attachment_jobs(id) ON DELETE CASCADE,
    job_lease_token uuid NOT NULL,
    resource_lease_token uuid NOT NULL UNIQUE,
    lease_expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (resource_code, resource_key, slot_no),
    CONSTRAINT ck_att_resource_slots CHECK (
        (resource_code = 'DOWNLOAD' AND resource_key = 'GLOBAL' AND slot_no BETWEEN 1 AND 2)
        OR (resource_code = 'EXTRACTION' AND resource_key = 'GLOBAL' AND slot_no = 1)
        OR (resource_code = 'HOST' AND resource_key ~ '^[0-9a-f]{64}$' AND slot_no = 1)
    )
);
CREATE INDEX ix_att_resource_job ON announcement_attachment_resource_leases(job_id, job_lease_token);
CREATE INDEX ix_att_resource_expiry ON announcement_attachment_resource_leases(lease_expires_at);

-- 정책 QA는 원문 수집 job과 별도 이력이다. 입력을 고정하며 부분 검증을 전체 성공으로 저장하지 않는다.
CREATE TABLE announcement_attachment_policy_validation_runs (
    id uuid PRIMARY KEY,
    policy_id uuid NOT NULL REFERENCES announcement_attachment_policies(id),
    policy_row_version integer NOT NULL CHECK (policy_row_version>=0),
    rule_release_id uuid NOT NULL REFERENCES announcement_source_classification_rule_releases(id),
    rule_row_version integer NOT NULL CHECK (rule_row_version>=0),
    snapshot_hash varchar(64) NOT NULL CHECK (snapshot_hash ~ '^[0-9a-f]{64}$'),
    input_snapshot_json jsonb NOT NULL CHECK (jsonb_typeof(input_snapshot_json)='object' AND octet_length(input_snapshot_json::text)<=2097152),
    run_status_code varchar(30) NOT NULL DEFAULT 'PENDING'
        CHECK (run_status_code IN ('PENDING','RUNNING','CANCEL_REQUESTED','CANCELLED','INCOMPLETE','FAILED','CONFLICT','VERIFIED')),
    row_version integer NOT NULL DEFAULT 0 CHECK (row_version>=0),
    requested_by uuid NOT NULL REFERENCES users(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    lease_token uuid,
    lease_expires_at timestamptz,
    error_code varchar(80) CHECK (error_code ~ '^[A-Z][A-Z0-9_]{0,79}$'),
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    completed_at timestamptz,
    CONSTRAINT uq_att_policy_validation_binding UNIQUE (id,policy_id),
    CONSTRAINT ck_att_validation_lease CHECK (
        (run_status_code IN ('RUNNING','CANCEL_REQUESTED') AND lease_token IS NOT NULL AND lease_expires_at IS NOT NULL AND started_at IS NOT NULL AND completed_at IS NULL)
        OR (run_status_code='PENDING' AND lease_token IS NULL AND lease_expires_at IS NULL AND started_at IS NULL AND completed_at IS NULL)
        OR (run_status_code IN ('CANCELLED','INCOMPLETE','FAILED','CONFLICT','VERIFIED') AND lease_token IS NULL AND lease_expires_at IS NULL AND completed_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_att_validation_active ON announcement_attachment_policy_validation_runs ((1))
    WHERE run_status_code IN ('PENDING','RUNNING','CANCEL_REQUESTED');
CREATE INDEX ix_att_validation_history ON announcement_attachment_policy_validation_runs(policy_id,created_at DESC,id DESC);
CREATE INDEX ix_att_validation_rule ON announcement_attachment_policy_validation_runs(rule_release_id);
CREATE INDEX ix_att_validation_actor ON announcement_attachment_policy_validation_runs(requested_by);
CREATE TABLE announcement_attachment_policy_validation_steps (
    run_id uuid NOT NULL REFERENCES announcement_attachment_policy_validation_runs(id),
    step_code varchar(30) NOT NULL CHECK (step_code IN ('CLASSIFICATION_GOLDEN','INSTALLED_RUNTIME','PROVIDER_PROFILES','WORKER_DB_RECOVERY')),
    status_code varchar(20) NOT NULL CHECK (status_code IN ('PASSED','FAILED','MISSING')),
    evidence_json jsonb NOT NULL CHECK (jsonb_typeof(evidence_json)='object' AND octet_length(evidence_json::text)<=32768),
    evidence_hash varchar(64) NOT NULL CHECK (evidence_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(run_id,step_code)
);
CREATE FUNCTION protect_attachment_validation_run() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        RAISE EXCEPTION 'validation history is immutable' USING ERRCODE='23514';
    END IF;
    IF TG_OP='INSERT' THEN
        IF NEW.run_status_code<>'PENDING' OR NEW.row_version<>0 OR NOT EXISTS (
            SELECT 1 FROM announcement_attachment_policies p
            JOIN announcement_source_classification_rule_releases r ON r.id=p.rule_release_id
            WHERE p.id=NEW.policy_id AND p.policy_status_code='DRAFT' AND p.row_version=NEW.policy_row_version
              AND r.id=NEW.rule_release_id AND r.row_version=NEW.rule_row_version AND r.release_status_code IN ('DRAFT','ACTIVE')) THEN
            RAISE EXCEPTION 'validation reservation requires current draft and rule' USING ERRCODE='23514';
        END IF;
        RETURN NEW;
    END IF;
    IF OLD.run_status_code NOT IN ('PENDING','RUNNING','CANCEL_REQUESTED') OR NEW.row_version<>OLD.row_version+1
        OR (NEW.id,NEW.policy_id,NEW.policy_row_version,NEW.rule_release_id,NEW.rule_row_version,NEW.snapshot_hash,
            NEW.input_snapshot_json,NEW.requested_by,NEW.idempotency_key,NEW.request_hash,NEW.created_at)
           IS DISTINCT FROM
           (OLD.id,OLD.policy_id,OLD.policy_row_version,OLD.rule_release_id,OLD.rule_row_version,OLD.snapshot_hash,
            OLD.input_snapshot_json,OLD.requested_by,OLD.idempotency_key,OLD.request_hash,OLD.created_at)
        OR (OLD.run_status_code='PENDING' AND NEW.run_status_code NOT IN ('RUNNING','CANCELLED','CONFLICT','FAILED'))
        OR (OLD.run_status_code='RUNNING' AND NEW.run_status_code NOT IN ('CANCEL_REQUESTED','CANCELLED','INCOMPLETE','FAILED','CONFLICT','VERIFIED'))
        OR (OLD.run_status_code='CANCEL_REQUESTED' AND NEW.run_status_code NOT IN ('CANCELLED','FAILED')) THEN
        RAISE EXCEPTION 'validation input, terminal result or transition is immutable' USING ERRCODE='23514';
    END IF;
    -- 취소는 실행 소유권/시각을 바꾸지 않는다. 만료된 실행은 만료 실패로만 닫는다.
    IF (OLD.run_status_code IN ('RUNNING','CANCEL_REQUESTED') AND NEW.started_at IS DISTINCT FROM OLD.started_at)
        OR (NEW.run_status_code='CANCEL_REQUESTED' AND
            (NEW.lease_token,NEW.lease_expires_at) IS DISTINCT FROM (OLD.lease_token,OLD.lease_expires_at))
        OR (OLD.run_status_code='PENDING' AND NEW.run_status_code='RUNNING' AND
            (NEW.lease_expires_at<=clock_timestamp() OR NEW.lease_expires_at>clock_timestamp()+interval '8 minutes'))
        OR (OLD.run_status_code='PENDING' AND NEW.run_status_code<>'RUNNING' AND NEW.started_at IS NOT NULL)
        OR (OLD.run_status_code IN ('RUNNING','CANCEL_REQUESTED') AND NEW.run_status_code<>'CANCEL_REQUESTED' AND OLD.lease_expires_at<=clock_timestamp()
            AND NOT (NEW.run_status_code='FAILED' AND NEW.error_code IS NOT DISTINCT FROM 'LEASE_EXPIRED')) THEN
        RAISE EXCEPTION 'validation execution ownership and expiry are fenced' USING ERRCODE='23514';
    END IF;
    IF NEW.run_status_code='VERIFIED' AND (SELECT count(1) FROM announcement_attachment_policy_validation_steps
        WHERE run_id=NEW.id AND status_code='PASSED')<>4 THEN
        RAISE EXCEPTION 'all four validation scopes must pass' USING ERRCODE='23514';
    END IF;
    IF NEW.run_status_code='VERIFIED' AND NOT EXISTS (
        SELECT 1 FROM announcement_attachment_policies p
        JOIN announcement_source_classification_rule_releases r ON r.id=p.rule_release_id
        WHERE p.id=NEW.policy_id AND p.policy_status_code='DRAFT' AND p.row_version=NEW.policy_row_version
          AND r.id=NEW.rule_release_id AND r.row_version=NEW.rule_row_version AND r.release_status_code IN ('DRAFT','ACTIVE')) THEN
        RAISE EXCEPTION 'validation completion requires current policy and rule inputs' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_validation_run BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_policy_validation_runs
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_validation_run();
CREATE FUNCTION protect_attachment_validation_step() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP<>'INSERT' THEN
        RAISE EXCEPTION 'validation evidence is immutable' USING ERRCODE='23514';
    END IF;
    PERFORM 1 FROM announcement_attachment_policy_validation_runs
        WHERE id=NEW.run_id AND run_status_code='RUNNING' AND lease_expires_at>clock_timestamp() FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'validation evidence requires live execution and is immutable' USING ERRCODE='23514';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM announcement_attachment_resource_leases l
        JOIN announcement_attachment_policy_validation_runs v
          ON l.policy_validation_id=v.id AND l.policy_validation_lease_token=v.lease_token
        WHERE v.id=NEW.run_id AND l.lease_expires_at>clock_timestamp()) THEN
        RAISE EXCEPTION 'validation evidence requires its owned extraction slot' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_validation_step BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_policy_validation_steps
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_validation_step();

-- QA와 운영 worker가 동일한 전역 추출 슬롯을 사용한다. 둘 중 한 소유자만 허용한다.
ALTER TABLE announcement_attachment_resource_leases
    ALTER COLUMN job_id DROP NOT NULL,
    ALTER COLUMN job_lease_token DROP NOT NULL,
    ADD COLUMN policy_validation_id uuid REFERENCES announcement_attachment_policy_validation_runs(id),
    ADD COLUMN policy_validation_lease_token uuid,
    ADD CONSTRAINT ck_att_resource_owner CHECK (
        (job_id IS NOT NULL AND job_lease_token IS NOT NULL AND policy_validation_id IS NULL AND policy_validation_lease_token IS NULL)
        OR (job_id IS NULL AND job_lease_token IS NULL AND policy_validation_id IS NOT NULL AND policy_validation_lease_token IS NOT NULL
            AND resource_code='EXTRACTION' AND resource_key='GLOBAL' AND slot_no=1));
CREATE INDEX ix_att_resource_policy_validation ON announcement_attachment_resource_leases(policy_validation_id);

-- 고정 배치 범위는 jobs로 물질화한다. source 삭제 뒤에는 복원 가능한 식별자 대신 건수만 남긴다.
ALTER TABLE announcement_attachment_batches
    ADD COLUMN scope_item_count integer CHECK (scope_item_count BETWEEN 1 AND 1000),
    ADD COLUMN policy_snapshot_json jsonb CHECK (jsonb_typeof(policy_snapshot_json)='object' AND octet_length(policy_snapshot_json::text)<=524288),
    ADD COLUMN collection_started_at timestamptz,
    ADD COLUMN collection_approval_hash varchar(64) CHECK (collection_approval_hash ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT ck_att_batch_scope_snapshot CHECK ((scope_item_count IS NULL)=(policy_snapshot_json IS NULL) AND scope_item_count<=maximum_count),
    ADD CONSTRAINT ck_att_batch_collection_approval CHECK (scope_item_count IS NULL OR
        (batch_status_code IN ('SCOPE_READY','CANCELLED') AND approved_by IS NULL AND collection_started_at IS NULL AND collection_approval_hash IS NULL) OR
        (batch_status_code NOT IN ('SCOPE_READY','CANCELLED') AND approved_by IS NOT NULL AND collection_started_at IS NOT NULL AND collection_approval_hash IS NOT NULL)),
    ADD CONSTRAINT uq_att_batch_policy_binding UNIQUE (id,policy_id),
    DROP CONSTRAINT announcement_attachment_batches_batch_status_code_check,
    ADD CONSTRAINT ck_att_batch_status CHECK (batch_status_code IN (
        'SCOPE_READY','COLLECTION_PENDING','COLLECTING','COLLECTED','COLLECTION_PARTIAL_FAILED','COLLECTION_PAUSED',
        'PREVIEW_RUNNING','PREVIEW_READY','PREVIEW_PARTIAL_FAILED','APPLYING','APPLIED','APPLY_PARTIAL_FAILED','APPLY_PAUSED',
        'ROLLING_BACK','ROLLED_BACK','ROLLBACK_PARTIAL_FAILED','CANCELLED'));
ALTER TABLE announcement_attachment_jobs ADD COLUMN frozen_provider_code varchar(30)
    CHECK (frozen_provider_code IN ('BIZINFO','GOV24','LOCAL_GOV_NOTICE'));
ALTER TABLE announcement_attachment_jobs ADD COLUMN frozen_locator_hash varchar(64)
    CHECK (frozen_locator_hash ~ '^[0-9a-f]{64}$');
-- URL을 작업 이력에 복사하지 않고 현재 출처 연결의 PostgreSQL 정규화 지문만 비교한다.
CREATE FUNCTION attachment_source_locator_hash(source_key uuid) RETURNS text LANGUAGE sql STABLE AS $$
    SELECT encode(digest(jsonb_build_array(s.provider_code,s.provider_notice_id,s.source_url,
        l.public_code,l.parser_profile_code)::text,'sha256'),'hex')
    FROM announcement_source_snapshots s LEFT JOIN local_government_notice_sources l ON l.id=s.local_government_source_id
    WHERE s.id=source_key AND (s.provider_code<>'LOCAL_GOV_NOTICE' OR (l.is_enabled AND l.deleted_at IS NULL))
$$;
-- 관리 batch의 매 HTTP 직전/lease claim에서 고정 출처·이전 검수 binding을 재확인한다.
CREATE FUNCTION attachment_batch_job_input_unchanged(job_key uuid) RETURNS boolean LANGUAGE sql STABLE AS $$
    SELECT coalesce((SELECT b.scope_item_count IS NULL OR (
        j.frozen_locator_hash=attachment_source_locator_hash(j.source_id)
        AND s.current_attachment_evaluation_id IS NOT DISTINCT FROM j.previous_attachment_evaluation_id
        AND s.attachment_policy_id IS NOT DISTINCT FROM j.previous_policy_id
        AND s.is_attachment_review_required IS NOT DISTINCT FROM j.previous_is_review_required
        AND (SELECT c.id FROM announcement_source_attachment_confirmations c WHERE c.source_id=j.source_id AND c.is_current)
            IS NOT DISTINCT FROM j.previous_confirmation_id
        AND NOT EXISTS (SELECT 1 FROM announcement_source_links l WHERE l.source_id=j.source_id))
        FROM announcement_attachment_jobs j JOIN announcement_source_snapshots s ON s.id=j.source_id
        LEFT JOIN announcement_attachment_batches b ON b.id=j.batch_id WHERE j.id=job_key),false)
$$;
ALTER TABLE announcement_attachment_jobs ADD CONSTRAINT fk_att_job_batch_policy FOREIGN KEY (batch_id,policy_id)
    REFERENCES announcement_attachment_batches(id,policy_id);
CREATE INDEX ix_att_batch_list ON announcement_attachment_batches(created_at DESC,id DESC);
CREATE INDEX ix_att_source_backfill_scope ON announcement_source_snapshots(provider_code,collected_at,id)
    WHERE data_purpose_code='PRODUCTION';
CREATE FUNCTION protect_attachment_batch_scope() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        IF OLD.scope_item_count IS NOT NULL THEN
            RAISE EXCEPTION 'managed attachment batch history cannot be deleted' USING ERRCODE='23514';
        END IF;
        RETURN OLD;
    END IF;
    IF OLD.scope_item_count IS NOT NULL AND (
        (NEW.id,NEW.batch_type_code,NEW.policy_id,NEW.scope_json,NEW.scope_hash,NEW.maximum_count,NEW.requested_by,
         NEW.scope_fixed_at,NEW.reason_hash,NEW.idempotency_key,NEW.request_hash,NEW.created_at,NEW.scope_item_count,NEW.policy_snapshot_json)
        IS DISTINCT FROM
        (OLD.id,OLD.batch_type_code,OLD.policy_id,OLD.scope_json,OLD.scope_hash,OLD.maximum_count,OLD.requested_by,
         OLD.scope_fixed_at,OLD.reason_hash,OLD.idempotency_key,OLD.request_hash,OLD.created_at,OLD.scope_item_count,OLD.policy_snapshot_json)
        OR NEW.row_version<>OLD.row_version+1 OR NEW.deleted_item_count<OLD.deleted_item_count
        OR (OLD.batch_status_code='CANCELLED' AND NEW.batch_status_code<>'CANCELLED')
        OR (NEW.batch_status_code='CANCELLED' AND OLD.batch_status_code NOT IN ('SCOPE_READY','CANCELLED'))
        OR (OLD.batch_status_code='SCOPE_READY' AND NEW.batch_status_code NOT IN ('SCOPE_READY','COLLECTION_PENDING','CANCELLED'))
        OR (OLD.batch_status_code<>'SCOPE_READY' AND NEW.batch_status_code='SCOPE_READY')
        OR (OLD.collection_started_at IS NOT NULL AND
            (NEW.approved_by,NEW.collection_started_at,NEW.collection_approval_hash) IS DISTINCT FROM
            (OLD.approved_by,OLD.collection_started_at,OLD.collection_approval_hash))) THEN
        RAISE EXCEPTION 'attachment batch frozen scope and cancellation are immutable' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_batch_scope BEFORE UPDATE OR DELETE ON announcement_attachment_batches
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_scope();
CREATE FUNCTION count_deleted_attachment_batch_item() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.batch_id IS NOT NULL THEN
        UPDATE announcement_attachment_batches SET deleted_item_count=deleted_item_count+1,row_version=row_version+1,updated_at=clock_timestamp()
            WHERE id=OLD.batch_id AND scope_item_count IS NOT NULL;
    END IF;
    RETURN OLD;
END $$;
CREATE TRIGGER tr_att_batch_item_deleted AFTER DELETE ON announcement_attachment_jobs
    FOR EACH ROW EXECUTE FUNCTION count_deleted_attachment_batch_item();
CREATE FUNCTION check_attachment_batch_scope_count() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE batch_key uuid; expected integer; deleted integer; batch_state varchar(30);
BEGIN
    IF TG_TABLE_NAME='announcement_attachment_batches' THEN batch_key:=NEW.id;
    ELSIF TG_OP='DELETE' THEN batch_key:=OLD.batch_id;
    ELSE batch_key:=NEW.batch_id; END IF;
    IF batch_key IS NULL THEN RETURN NULL; END IF;
    SELECT scope_item_count,deleted_item_count,batch_status_code INTO expected,deleted,batch_state
        FROM announcement_attachment_batches WHERE id=batch_key;
    IF expected IS NOT NULL AND (
        expected<>(SELECT count(1) FROM announcement_attachment_jobs WHERE batch_id=batch_key)+deleted
        OR EXISTS (SELECT 1 FROM announcement_attachment_jobs WHERE batch_id=batch_key AND
            (frozen_provider_code IS NULL OR frozen_locator_hash IS NULL OR execution_snapshot_json IS NULL
             OR (batch_state='SCOPE_READY' AND job_status_code<>'SCOPE_READY')
             OR (batch_state NOT IN ('SCOPE_READY','CANCELLED') AND job_status_code='SCOPE_READY')
             OR (batch_state='CANCELLED' AND job_status_code<>'CANCELLED')))) THEN
        RAISE EXCEPTION 'attachment batch materialized scope or reserved execution state mismatch' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_batch_scope_count AFTER INSERT OR UPDATE ON announcement_attachment_batches
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_batch_scope_count();
CREATE CONSTRAINT TRIGGER ct_att_batch_job_count AFTER INSERT OR UPDATE OR DELETE ON announcement_attachment_jobs
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_batch_scope_count();

-- 예약 입력은 재시도/적용/원복 중에도 불변이다. 운영 정책이나 규칙을 활성화하지 않는다.
CREATE FUNCTION protect_attachment_job_execution() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.source_id, NEW.content_version_id, NEW.base_evaluation_id, NEW.rule_release_id,
        NEW.policy_id, NEW.batch_id, NEW.generation, NEW.expected_source_version,
        NEW.expected_attachment_version, NEW.idempotency_key, NEW.request_hash,
        NEW.execution_snapshot_json, NEW.download_budget_bytes, NEW.collection_run_id,
        NEW.operation_code, NEW.reference_set_id, NEW.requested_by,NEW.frozen_provider_code,NEW.frozen_locator_hash)
       IS DISTINCT FROM
       (OLD.source_id, OLD.content_version_id, OLD.base_evaluation_id, OLD.rule_release_id,
        OLD.policy_id, OLD.batch_id, OLD.generation, OLD.expected_source_version,
        OLD.expected_attachment_version, OLD.idempotency_key, OLD.request_hash,
        OLD.execution_snapshot_json, OLD.download_budget_bytes, OLD.collection_run_id,
        OLD.operation_code, OLD.reference_set_id, OLD.requested_by,OLD.frozen_provider_code,OLD.frozen_locator_hash) THEN
        RAISE EXCEPTION 'attachment job execution binding is immutable' USING ERRCODE='23514';
    END IF;
    IF NEW.reserved_download_bytes < OLD.reserved_download_bytes THEN
        RAISE EXCEPTION 'attachment retry budget cannot be refunded' USING ERRCODE='23514';
    END IF;
    IF OLD.frozen_provider_code IS NOT NULL AND
        (NEW.previous_attachment_evaluation_id,NEW.previous_confirmation_id,NEW.previous_policy_id,NEW.previous_is_review_required)
        IS DISTINCT FROM (OLD.previous_attachment_evaluation_id,OLD.previous_confirmation_id,OLD.previous_policy_id,OLD.previous_is_review_required) THEN
        RAISE EXCEPTION 'attachment batch previous bindings are immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.operation_code='ROLE_CHANGE' AND NEW.set_id IS DISTINCT FROM OLD.set_id THEN
        RAISE EXCEPTION 'attachment role job sealed set is immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.operation_code='RETRY_FILES' AND OLD.set_id IS NOT NULL AND NEW.set_id IS DISTINCT FROM OLD.set_id THEN
        RAISE EXCEPTION 'attachment retry sealed set is immutable' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_job_execution BEFORE UPDATE ON announcement_attachment_jobs
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_job_execution();

-- 재시도 중 성공 파일 원문은 접근 제한된 작업 근거에만 잠시 보존한다. 최종/감사 근거와 구분한다.
ALTER TABLE announcement_attachment_jobs ADD CONSTRAINT uq_att_job_source UNIQUE (id,source_id);
ALTER TABLE announcement_attachment_jobs ADD CONSTRAINT uq_att_job_retry_reference UNIQUE (id,source_id,reference_set_id);
CREATE TABLE announcement_attachment_retry_files (
    job_id uuid NOT NULL,
    source_id uuid NOT NULL,
    reference_set_id uuid NOT NULL,
    file_id uuid NOT NULL,
    PRIMARY KEY (job_id,file_id),
    CONSTRAINT fk_att_retry_job FOREIGN KEY (job_id,source_id,reference_set_id)
        REFERENCES announcement_attachment_jobs(id,source_id,reference_set_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_retry_file FOREIGN KEY (file_id,reference_set_id,source_id)
        REFERENCES announcement_source_attachment_files(id,set_id,source_id) ON DELETE CASCADE
);
CREATE INDEX ix_att_retry_reference_file ON announcement_attachment_retry_files(file_id,reference_set_id,source_id);
CREATE FUNCTION protect_attachment_retry_scope() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        IF EXISTS (SELECT 1 FROM announcement_attachment_jobs j JOIN announcement_source_snapshots s ON s.id=j.source_id WHERE j.id=OLD.job_id) THEN
            RAISE EXCEPTION 'attachment retry scope is immutable' USING ERRCODE='23514';
        END IF;
        RETURN OLD;
    END IF;
    IF TG_OP='UPDATE' THEN RAISE EXCEPTION 'attachment retry scope is immutable' USING ERRCODE='23514'; END IF;
    PERFORM 1 FROM announcement_attachment_jobs WHERE id=NEW.job_id AND source_id=NEW.source_id
      AND reference_set_id=NEW.reference_set_id AND operation_code='RETRY_FILES' AND job_status_code='PENDING' AND set_id IS NULL FOR UPDATE;
    IF NOT FOUND OR (SELECT count(1) FROM announcement_attachment_retry_files WHERE job_id=NEW.job_id)>=10 THEN
        RAISE EXCEPTION 'attachment retry scope requires pending job and at most ten files' USING ERRCODE='23514';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM announcement_source_attachment_files f
        LEFT JOIN LATERAL (SELECT x.quality_code FROM announcement_source_attachment_extractions x
            WHERE x.file_id=f.id AND x.source_id=f.source_id AND x.set_id=f.set_id ORDER BY x.attempt_no DESC,x.id DESC LIMIT 1) quality ON true
        WHERE f.id=NEW.file_id AND f.source_id=NEW.source_id AND f.set_id=NEW.reference_set_id
          AND (f.download_status_code IN ('FAILED','CANCELLED') OR (f.download_status_code='SUCCEEDED'
            AND quality.quality_code IN ('PARTIAL_TEXT','CORRUPT','LIMIT_EXCEEDED','TIMEOUT','FAILED','ISOLATION_UNAVAILABLE')))) THEN
        RAISE EXCEPTION 'attachment retry scope must select failed files only' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_retry_scope_immutable BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_retry_files
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_retry_scope();
CREATE FUNCTION check_attachment_retry_scope_present() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.operation_code='RETRY_FILES' AND EXISTS (SELECT 1 FROM announcement_attachment_jobs WHERE id=NEW.id)
        AND NOT EXISTS (SELECT 1 FROM announcement_attachment_retry_files WHERE job_id=NEW.id) THEN
        RAISE EXCEPTION 'attachment retry requires selected file scope' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER tr_att_retry_scope_present AFTER INSERT ON announcement_attachment_jobs
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_retry_scope_present();
CREATE TABLE announcement_attachment_file_checkpoints (
    job_id uuid NOT NULL,
    source_id uuid NOT NULL,
    stable_locator_hash varchar(64) NOT NULL CHECK (stable_locator_hash ~ '^[0-9a-f]{64}$'),
    file_result_json jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY (job_id,stable_locator_hash),
    CONSTRAINT fk_att_checkpoint_job FOREIGN KEY (job_id,source_id)
        REFERENCES announcement_attachment_jobs(id,source_id) ON DELETE CASCADE,
    CONSTRAINT ck_att_checkpoint_complete CHECK (
        jsonb_typeof(file_result_json)='object'
        AND octet_length(file_result_json::text)<=16777216
        AND (file_result_json->>'downloadStatus') IS NOT DISTINCT FROM 'SUCCEEDED'
        AND (file_result_json#>>'{extraction,quality}') IS NOT DISTINCT FROM 'COMPLETE_TEXT'
        AND (file_result_json#>>'{extraction,completedAtEpochMs}') IS NOT NULL)
);
CREATE INDEX ix_att_checkpoint_source ON announcement_attachment_file_checkpoints(source_id);
CREATE FUNCTION protect_attachment_file_checkpoint() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='UPDATE' THEN
        RAISE EXCEPTION 'attachment checkpoint is immutable' USING ERRCODE='23514';
    END IF;
    PERFORM 1 FROM announcement_attachment_jobs
      WHERE id=NEW.job_id AND source_id=NEW.source_id AND operation_code IN ('COLLECT','RETRY_FILES')
        AND job_status_code='RUNNING' AND lease_expires_at>clock_timestamp() AND set_id IS NULL FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'attachment checkpoint requires running unsealed collection' USING ERRCODE='23514';
    END IF;
    -- 정상 목록은 최대 10개이며, 3회 시도 중 목록 교체까지 포함해도 30개를 넘기지 않는다.
    IF NOT EXISTS (SELECT 1 FROM announcement_attachment_file_checkpoints WHERE job_id=NEW.job_id AND stable_locator_hash=NEW.stable_locator_hash)
        AND (SELECT count(*) FROM announcement_attachment_file_checkpoints WHERE job_id=NEW.job_id)>=30 THEN
        RAISE EXCEPTION 'attachment checkpoint file limit exceeded' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_checkpoint_immutable BEFORE INSERT OR UPDATE ON announcement_attachment_file_checkpoints
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_file_checkpoint();
CREATE FUNCTION delete_finished_attachment_checkpoints() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.set_id IS NOT NULL OR NEW.job_status_code IN ('SUCCEEDED','PARTIAL_FAILED','FAILED','CONFLICT','CANCELLED') THEN
        DELETE FROM announcement_attachment_file_checkpoints WHERE job_id=NEW.id;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_checkpoint_finished AFTER UPDATE OF set_id,job_status_code ON announcement_attachment_jobs
    FOR EACH ROW EXECUTE FUNCTION delete_finished_attachment_checkpoints();

-- 정상 재수집 등으로 base가 바뀌면 이전 첨부 확정과 실행 권한을 함께 만료시킨다.
-- 기존 검수 요구/정책 연결은 유지하며, 이력은 삭제하지 않는다.
CREATE FUNCTION invalidate_attachment_on_base_change() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE has_active_job boolean;
BEGIN
    SELECT EXISTS (SELECT 1 FROM announcement_attachment_jobs WHERE source_id=OLD.id
        AND job_status_code IN ('SCOPE_READY','PENDING','RUNNING','RETRY_WAIT','PAUSED')) INTO has_active_job;
    IF OLD.is_attachment_review_required OR OLD.current_attachment_evaluation_id IS NOT NULL OR has_active_job THEN
        NEW.attachment_row_version := OLD.attachment_row_version + 1;
        NEW.current_attachment_evaluation_id := NULL;
        UPDATE announcement_source_attachment_evaluations SET is_current=false WHERE source_id=OLD.id AND is_current;
        UPDATE announcement_source_attachment_confirmations SET is_current=false WHERE source_id=OLD.id AND is_current;
        UPDATE announcement_attachment_jobs
        SET job_status_code='CONFLICT', error_code='SOURCE_CHANGED', lease_token=NULL, lease_expires_at=NULL,
            row_version=row_version+1, updated_at=clock_timestamp()
        WHERE source_id=OLD.id AND job_status_code IN ('SCOPE_READY','PENDING','RUNNING','RETRY_WAIT','PAUSED');
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_base_changed BEFORE UPDATE OF classification_row_version ON announcement_source_snapshots
    FOR EACH ROW WHEN (OLD.classification_row_version IS DISTINCT FROM NEW.classification_row_version)
    EXECUTE FUNCTION invalidate_attachment_on_base_change();

-- 새 검수 확인은 확인 직후의 source/첨부 버전을 기록한다. 기존 확인의 버전을 추측해 채우지 않는다.
ALTER TABLE announcement_source_attachment_confirmations
    ADD COLUMN confirmed_source_version integer CHECK (confirmed_source_version >= 0),
    ADD COLUMN confirmed_attachment_version integer CHECK (confirmed_attachment_version >= 0),
    ADD CONSTRAINT ck_att_confirmation_versions CHECK (
        (confirmed_source_version IS NULL) = (confirmed_attachment_version IS NULL));
CREATE FUNCTION protect_attachment_confirmation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (to_jsonb(NEW) - 'is_current') IS DISTINCT FROM (to_jsonb(OLD) - 'is_current') THEN
        RAISE EXCEPTION 'attachment confirmation is immutable' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_confirmation_immutable BEFORE UPDATE ON announcement_source_attachment_confirmations
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_confirmation();

-- 기존 link UNIQUE/기존 V1 입력은 유지한다. 새 종합 전환만 확인과 요청 hash를 함께 저장한다.
ALTER TABLE announcement_source_links
    ADD COLUMN attachment_confirmation_id uuid,
    ADD COLUMN attachment_request_hash varchar(64) CHECK (attachment_request_hash ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT fk_att_link_confirmation FOREIGN KEY (attachment_confirmation_id,source_id)
        REFERENCES announcement_source_attachment_confirmations(id,source_id),
    ADD CONSTRAINT ck_att_link_confirmation_request CHECK (
        (attachment_confirmation_id IS NULL) = (attachment_request_hash IS NULL));
CREATE INDEX ix_att_link_confirmation ON announcement_source_links(attachment_confirmation_id,source_id)
    WHERE attachment_confirmation_id IS NOT NULL;
CREATE FUNCTION protect_attachment_link_request() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.attachment_confirmation_id IS NOT NULL AND
        (NEW.source_id,NEW.announcement_id,NEW.attachment_confirmation_id,NEW.attachment_request_hash)
        IS DISTINCT FROM (OLD.source_id,OLD.announcement_id,OLD.attachment_confirmation_id,OLD.attachment_request_hash) THEN
        RAISE EXCEPTION 'attachment conversion request is immutable' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_link_request_immutable BEFORE UPDATE ON announcement_source_links
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_link_request();

-- source별 불변 판정 이력을 현재 partial unique index와 분리해 안정적으로 페이지 조회한다.
CREATE INDEX ix_att_eval_source_history ON announcement_source_attachment_evaluations(source_id,evaluated_at DESC,id DESC);

-- 자동 수집/역할 변경과 구분하여 운영자의 전체 수집·선택 재시도 요청을 합산한다.
CREATE INDEX ix_att_job_manual_network_window ON announcement_attachment_jobs(source_id,created_at DESC)
    WHERE requested_by IS NOT NULL AND operation_code IN ('COLLECT','RETRY_FILES');

-- 봉인 결과와 명시적 선택을 매번 불변 preview 이력으로 남긴다. 원문은 job 삭제와 함께 제거한다.
ALTER TABLE announcement_attachment_jobs ADD CONSTRAINT uq_att_job_batch_identity UNIQUE(id,batch_id);
CREATE TABLE announcement_attachment_batch_previews (
    id uuid PRIMARY KEY,
    batch_id uuid NOT NULL REFERENCES announcement_attachment_batches(id),
    preview_status_code varchar(30) NOT NULL CHECK (preview_status_code IN ('PREVIEW_READY','PREVIEW_PARTIAL_FAILED')),
    scope_hash varchar(64) NOT NULL CHECK (scope_hash ~ '^[0-9a-f]{64}$'),
    input_hash varchar(64) NOT NULL CHECK (input_hash ~ '^[0-9a-f]{64}$'),
    preview_hash varchar(64) NOT NULL CHECK (preview_hash ~ '^[0-9a-f]{64}$'),
    batch_version integer NOT NULL CHECK (batch_version>=0),
    scope_item_count integer NOT NULL CHECK (scope_item_count BETWEEN 1 AND 1000),
    remaining_item_count integer NOT NULL CHECK (remaining_item_count>=0),
    deleted_item_count integer NOT NULL CHECK (deleted_item_count>=0),
    eligible_item_count integer NOT NULL CHECK (eligible_item_count>=0 AND eligible_item_count<=remaining_item_count),
    selected_item_count integer NOT NULL CHECK (selected_item_count>=0 AND selected_item_count<=eligible_item_count),
    actor_id uuid NOT NULL REFERENCES users(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    reason_hash varchar(64) NOT NULL CHECK (reason_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CHECK (scope_item_count=remaining_item_count+deleted_item_count),
    CHECK ((preview_status_code='PREVIEW_READY')=(eligible_item_count=scope_item_count)),
    UNIQUE(id,batch_id),UNIQUE(batch_id,preview_hash)
);
CREATE INDEX ix_att_batch_preview_history ON announcement_attachment_batch_previews(batch_id,created_at DESC,id DESC);
CREATE INDEX ix_att_batch_preview_actor ON announcement_attachment_batch_previews(actor_id);
CREATE TABLE announcement_attachment_batch_preview_items (
    preview_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    job_id uuid NOT NULL,
    readiness_code varchar(40) NOT NULL CHECK (readiness_code IN ('READY','COLLECTION_NOT_SUCCESSFUL','EVIDENCE_INCOMPLETE','PROTECTED_LINK','ACTIVE_JOB','SOURCE_CHANGED')),
    is_eligible boolean NOT NULL,
    is_selected boolean NOT NULL,
    input_hash varchar(64) NOT NULL CHECK (input_hash ~ '^[0-9a-f]{64}$'),
    evidence_json jsonb NOT NULL CHECK (jsonb_typeof(evidence_json)='object' AND octet_length(evidence_json::text)<=65536),
    PRIMARY KEY(preview_id,job_id),
    FOREIGN KEY(preview_id,batch_id) REFERENCES announcement_attachment_batch_previews(id,batch_id),
    FOREIGN KEY(job_id,batch_id) REFERENCES announcement_attachment_jobs(id,batch_id) ON DELETE CASCADE,
    CHECK (is_eligible=(readiness_code='READY')),
    CHECK (NOT is_selected OR is_eligible)
);
CREATE INDEX ix_att_batch_preview_item_job ON announcement_attachment_batch_preview_items(job_id,batch_id);
ALTER TABLE announcement_attachment_batches ADD COLUMN current_preview_id uuid,
    ADD CONSTRAINT fk_att_batch_current_preview FOREIGN KEY(current_preview_id,id)
        REFERENCES announcement_attachment_batch_previews(id,batch_id) DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT ck_att_managed_preview_pointer CHECK (scope_item_count IS NULL OR batch_status_code NOT IN ('PREVIEW_READY','PREVIEW_PARTIAL_FAILED')
        OR (current_preview_id IS NOT NULL AND preview_hash IS NOT NULL));

CREATE FUNCTION protect_attachment_batch_preview_history() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE batch_state varchar(30); current_version integer; fixed_scope varchar(64); fixed_count integer; final_version integer;
BEGIN
    IF TG_OP='DELETE' AND TG_TABLE_NAME='announcement_attachment_batch_preview_items' THEN
        IF NOT EXISTS (SELECT 1 FROM announcement_attachment_jobs WHERE id=OLD.job_id) THEN RETURN OLD; END IF;
    END IF;
    IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'attachment batch preview history is immutable' USING ERRCODE='23514'; END IF;
    SELECT batch_status_code,row_version,scope_hash,scope_item_count INTO batch_state,current_version,fixed_scope,fixed_count
        FROM announcement_attachment_batches WHERE id=NEW.batch_id FOR UPDATE;
    IF batch_state IS DISTINCT FROM 'PREVIEW_RUNNING' THEN
        RAISE EXCEPTION 'attachment preview writes require preview transaction' USING ERRCODE='23514';
    END IF;
    IF TG_TABLE_NAME='announcement_attachment_batch_previews' THEN
        IF NEW.batch_version<>current_version+1 OR NEW.scope_hash<>fixed_scope OR NEW.scope_item_count<>fixed_count THEN
            RAISE EXCEPTION 'attachment preview frozen batch mismatch' USING ERRCODE='23514';
        END IF;
    ELSE
        SELECT batch_version INTO final_version FROM announcement_attachment_batch_previews WHERE id=NEW.preview_id AND batch_id=NEW.batch_id;
        IF final_version IS DISTINCT FROM current_version+1 THEN
            RAISE EXCEPTION 'attachment preview items cannot be appended to old history' USING ERRCODE='23514';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_batch_preview_history BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_batch_previews
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_preview_history();
CREATE TRIGGER tr_att_batch_preview_item_history BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_batch_preview_items
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_preview_history();

CREATE FUNCTION check_attachment_batch_preview_complete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM announcement_attachment_batches b WHERE b.id=NEW.batch_id AND b.current_preview_id=NEW.id AND b.preview_hash=NEW.preview_hash
        AND b.row_version=NEW.batch_version AND b.batch_status_code=NEW.preview_status_code AND b.deleted_item_count=NEW.deleted_item_count)
        OR NEW.remaining_item_count<>(SELECT count(1) FROM announcement_attachment_batch_preview_items WHERE preview_id=NEW.id)
        OR NEW.eligible_item_count<>(SELECT count(1) FROM announcement_attachment_batch_preview_items WHERE preview_id=NEW.id AND is_eligible)
        OR NEW.selected_item_count<>(SELECT count(1) FROM announcement_attachment_batch_preview_items WHERE preview_id=NEW.id AND is_selected) THEN
        RAISE EXCEPTION 'attachment preview must contain every frozen item and exact selection' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_batch_preview_complete AFTER INSERT ON announcement_attachment_batch_previews
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_batch_preview_complete();

CREATE FUNCTION protect_attachment_batch_selection() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.is_selected_for_application IS DISTINCT FROM OLD.is_selected_for_application AND OLD.frozen_provider_code IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM announcement_attachment_batches b WHERE b.id=OLD.batch_id AND b.batch_status_code='PREVIEW_RUNNING') THEN
        RAISE EXCEPTION 'attachment selection requires a new preview snapshot' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_batch_selection BEFORE UPDATE ON announcement_attachment_jobs
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_selection();
CREATE FUNCTION check_attachment_batch_current_selection() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE batch_key uuid; selected_preview uuid;
BEGIN
    IF TG_TABLE_NAME='announcement_attachment_batches' THEN batch_key:=NEW.id; ELSE batch_key:=NEW.batch_id; END IF;
    SELECT p.id INTO selected_preview FROM announcement_attachment_batches b JOIN announcement_attachment_batch_previews p
        ON p.id=b.current_preview_id AND p.batch_id=b.id AND p.preview_hash=b.preview_hash
        WHERE b.id=batch_key AND b.batch_status_code IN ('PREVIEW_READY','PREVIEW_PARTIAL_FAILED');
    IF selected_preview IS NULL AND EXISTS (SELECT 1 FROM announcement_attachment_batches b WHERE b.id=batch_key AND b.scope_item_count IS NOT NULL
        AND b.batch_status_code IN ('PREVIEW_READY','PREVIEW_PARTIAL_FAILED')) THEN
        RAISE EXCEPTION 'attachment current preview pointer and hash mismatch' USING ERRCODE='23514';
    END IF;
    IF selected_preview IS NOT NULL AND EXISTS (
        SELECT 1 FROM announcement_attachment_jobs j LEFT JOIN announcement_attachment_batch_preview_items i
            ON i.preview_id=selected_preview AND i.job_id=j.id
        WHERE j.batch_id=batch_key AND (i.job_id IS NULL OR i.is_selected IS DISTINCT FROM j.is_selected_for_application)) THEN
        RAISE EXCEPTION 'attachment job selection and current preview mismatch' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_batch_current_selection AFTER UPDATE ON announcement_attachment_batches
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_batch_current_selection();
CREATE CONSTRAINT TRIGGER ct_att_job_current_selection AFTER UPDATE ON announcement_attachment_jobs
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_batch_current_selection();

-- 명시적 적용 승인은 불변 미리보기·전체/선택/삭제 수에 묶는다. 기존 배치에는 승인을 추측해 채우지 않는다.
CREATE TABLE announcement_attachment_batch_application_actions (
    id uuid PRIMARY KEY,
    batch_id uuid NOT NULL REFERENCES announcement_attachment_batches(id),
    preview_id uuid NOT NULL,
    action_code varchar(10) NOT NULL CHECK (action_code IN ('START','PAUSE','RESUME')),
    expected_version integer NOT NULL CHECK (expected_version BETWEEN 0 AND 2147483646),
    scope_item_count integer NOT NULL CHECK (scope_item_count BETWEEN 1 AND 1000),
    selected_item_count integer NOT NULL CHECK (selected_item_count BETWEEN 1 AND scope_item_count),
    deleted_item_count integer NOT NULL CHECK (deleted_item_count BETWEEN 0 AND scope_item_count),
    actor_id uuid NOT NULL REFERENCES users(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    reason_hash varchar(64) NOT NULL CHECK (reason_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    UNIQUE(id,batch_id),UNIQUE(batch_id,expected_version),
    FOREIGN KEY(preview_id,batch_id) REFERENCES announcement_attachment_batch_previews(id,batch_id)
);
CREATE UNIQUE INDEX uq_att_batch_application_start ON announcement_attachment_batch_application_actions(batch_id) WHERE action_code='START';
CREATE INDEX ix_att_batch_application_action_preview ON announcement_attachment_batch_application_actions(preview_id,batch_id);
CREATE INDEX ix_att_batch_application_actor ON announcement_attachment_batch_application_actions(actor_id);
ALTER TABLE announcement_attachment_batches ADD COLUMN application_approval_id uuid,
    ADD CONSTRAINT fk_att_batch_application_approval FOREIGN KEY(application_approval_id,id)
        REFERENCES announcement_attachment_batch_application_actions(id,batch_id) DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT ck_att_managed_batch_application_approval CHECK(scope_item_count IS NULL
        OR batch_status_code NOT IN ('APPLYING','APPLY_PAUSED','APPLIED','APPLY_PARTIAL_FAILED') OR application_approval_id IS NOT NULL);
ALTER TABLE announcement_attachment_jobs ADD COLUMN application_preview_id uuid,
    ADD COLUMN applied_source_version integer CHECK(applied_source_version>=0),
    ADD COLUMN applied_input_hash varchar(64) CHECK(applied_input_hash ~ '^[0-9a-f]{64}$'),
    ADD COLUMN application_error_code varchar(80),
    ADD COLUMN application_attempt_count integer NOT NULL DEFAULT 0 CHECK(application_attempt_count BETWEEN 0 AND 3),
    ADD COLUMN application_next_attempt_at timestamptz,
    ADD CONSTRAINT fk_att_job_application_preview FOREIGN KEY(application_preview_id,batch_id)
        REFERENCES announcement_attachment_batch_previews(id,batch_id) DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT ck_att_job_application_result CHECK(application_preview_id IS NULL OR application_status_code<>'APPLIED'
        OR (applied_evaluation_id IS NOT NULL AND applied_attachment_version IS NOT NULL AND applied_source_version IS NOT NULL AND applied_input_hash IS NOT NULL));
CREATE INDEX ix_att_job_application_preview ON announcement_attachment_jobs(application_preview_id,batch_id);
CREATE INDEX ix_att_job_application_queue ON announcement_attachment_jobs(batch_id,application_next_attempt_at,source_id)
    WHERE application_status_code='PENDING' AND application_preview_id IS NOT NULL;
CREATE FUNCTION protect_attachment_batch_application_action() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE state varchar(30); version integer; current_preview uuid; item_count integer; deleted_count integer; selected_count integer;
BEGIN
    IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'attachment application approval history is immutable' USING ERRCODE='23514'; END IF;
    SELECT b.batch_status_code,b.row_version,b.current_preview_id,b.scope_item_count,b.deleted_item_count,p.selected_item_count
        INTO state,version,current_preview,item_count,deleted_count,selected_count
        FROM announcement_attachment_batches b JOIN announcement_attachment_batch_previews p ON p.id=b.current_preview_id AND p.batch_id=b.id
        WHERE b.id=NEW.batch_id FOR UPDATE OF b;
    IF version IS NULL OR (version,current_preview,item_count,deleted_count,selected_count)
        IS DISTINCT FROM (NEW.expected_version,NEW.preview_id,NEW.scope_item_count,NEW.deleted_item_count,NEW.selected_item_count)
        OR (NEW.action_code='START' AND state NOT IN ('PREVIEW_READY','PREVIEW_PARTIAL_FAILED'))
        OR (NEW.action_code='PAUSE' AND state<>'APPLYING') OR (NEW.action_code='RESUME' AND state<>'APPLY_PAUSED') THEN
        RAISE EXCEPTION 'attachment application approval scope or state changed' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_batch_application_action BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_batch_application_actions
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_application_action();
CREATE FUNCTION check_attachment_batch_application_action_complete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM announcement_attachment_batches b WHERE b.id=NEW.batch_id AND b.current_preview_id=NEW.preview_id
        AND b.row_version=NEW.expected_version+1 AND b.batch_status_code=CASE WHEN NEW.action_code='PAUSE' THEN 'APPLY_PAUSED' ELSE 'APPLYING' END
        AND b.scope_item_count=NEW.scope_item_count AND b.deleted_item_count=NEW.deleted_item_count
        AND (NEW.action_code<>'START' OR (b.application_approval_id=NEW.id AND
            (SELECT count(1) FROM announcement_attachment_jobs j WHERE j.batch_id=b.id AND j.application_preview_id=NEW.preview_id
                AND j.is_selected_for_application AND j.application_status_code='PENDING')=NEW.selected_item_count))) THEN
        RAISE EXCEPTION 'attachment application action must commit its complete scope transition' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_batch_application_action_complete AFTER INSERT ON announcement_attachment_batch_application_actions
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_batch_application_action_complete();
CREATE FUNCTION protect_attachment_batch_application_binding() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.application_approval_id IS NOT NULL AND (NEW.application_approval_id,NEW.current_preview_id,NEW.preview_hash)
        IS DISTINCT FROM (OLD.application_approval_id,OLD.current_preview_id,OLD.preview_hash) THEN
        RAISE EXCEPTION 'attachment approved preview binding is immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.application_approval_id IS NULL AND NEW.application_approval_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM announcement_attachment_batch_application_actions a WHERE a.id=NEW.application_approval_id AND a.batch_id=NEW.id
          AND a.action_code='START' AND a.preview_id=NEW.current_preview_id AND a.expected_version=OLD.row_version
          AND NEW.row_version=OLD.row_version+1 AND NEW.batch_status_code='APPLYING') THEN
        RAISE EXCEPTION 'attachment application requires matching explicit start approval' USING ERRCODE='23514';
    END IF;
    IF OLD.application_approval_id IS NOT NULL AND NEW.batch_status_code IS DISTINCT FROM OLD.batch_status_code THEN
        IF NOT ((OLD.batch_status_code='APPLYING' AND NEW.batch_status_code IN ('APPLY_PAUSED','APPLIED','APPLY_PARTIAL_FAILED'))
            OR (OLD.batch_status_code='APPLY_PAUSED' AND NEW.batch_status_code='APPLYING')
            OR (OLD.batch_status_code IN ('APPLIED','APPLY_PARTIAL_FAILED','APPLY_PAUSED') AND NEW.batch_status_code='ROLLING_BACK')
            OR (OLD.batch_status_code='ROLLING_BACK' AND NEW.batch_status_code IN ('ROLLED_BACK','ROLLBACK_PARTIAL_FAILED'))) THEN
            RAISE EXCEPTION 'attachment application state cannot be reset' USING ERRCODE='23514';
        END IF;
        IF NEW.batch_status_code IN ('APPLY_PAUSED','APPLYING') AND NOT EXISTS (
            SELECT 1 FROM announcement_attachment_batch_application_actions a WHERE a.batch_id=NEW.id AND a.preview_id=NEW.current_preview_id
              AND a.expected_version=OLD.row_version AND a.action_code=CASE WHEN NEW.batch_status_code='APPLY_PAUSED' THEN 'PAUSE' ELSE 'RESUME' END) THEN
            RAISE EXCEPTION 'attachment pause or resume requires matching action' USING ERRCODE='23514';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_batch_application_binding BEFORE UPDATE ON announcement_attachment_batches
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_application_binding();
CREATE FUNCTION protect_attachment_batch_job_application() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.frozen_provider_code IS NULL THEN RETURN NEW; END IF;
    IF OLD.application_preview_id IS NOT NULL AND (NEW.application_preview_id,NEW.set_id,NEW.preview_evaluation_id,NEW.preview_hash)
        IS DISTINCT FROM (OLD.application_preview_id,OLD.set_id,OLD.preview_evaluation_id,OLD.preview_hash) THEN
        RAISE EXCEPTION 'attachment application evidence is frozen' USING ERRCODE='23514';
    END IF;
    IF OLD.application_status_code IN ('APPLIED','CONFLICT','FAILED') AND
        (NEW.application_status_code,NEW.applied_evaluation_id,NEW.applied_attachment_version,NEW.applied_source_version,NEW.applied_input_hash)
        IS DISTINCT FROM (OLD.application_status_code,OLD.applied_evaluation_id,OLD.applied_attachment_version,OLD.applied_source_version,OLD.applied_input_hash) THEN
        RAISE EXCEPTION 'attachment terminal application result is immutable' USING ERRCODE='23514';
    END IF;
    IF NEW.application_status_code IS DISTINCT FROM OLD.application_status_code OR NEW.application_preview_id IS DISTINCT FROM OLD.application_preview_id THEN
        IF NOT ((OLD.application_status_code='NOT_REQUESTED' AND NEW.application_status_code='PENDING')
            OR (OLD.application_status_code='PENDING' AND NEW.application_status_code IN ('APPLIED','CONFLICT','FAILED')))
            OR NOT EXISTS (SELECT 1 FROM announcement_attachment_batches b
                JOIN announcement_attachment_batch_application_actions a ON a.id=b.application_approval_id AND a.batch_id=b.id AND a.action_code='START'
                JOIN announcement_attachment_batch_preview_items i ON i.preview_id=a.preview_id AND i.batch_id=b.id AND i.job_id=NEW.id
                WHERE b.id=NEW.batch_id AND (b.batch_status_code='APPLYING' OR (b.batch_status_code='ROLLING_BACK'
                    AND b.rollback_approval_id IS NOT NULL AND NEW.application_status_code='CONFLICT' AND NEW.application_error_code='APPLICATION_CANCELLED_BY_ROLLBACK'))
                  AND NEW.application_preview_id=a.preview_id
                  AND b.current_preview_id=a.preview_id AND NEW.is_selected_for_application AND i.is_selected AND i.is_eligible) THEN
            RAISE EXCEPTION 'attachment application requires an approved selected item' USING ERRCODE='23514';
        END IF;
    END IF;
    IF OLD.application_status_code<>'APPLIED' AND NEW.application_status_code='APPLIED' AND NOT EXISTS (
        SELECT 1 FROM announcement_source_snapshots s JOIN announcement_source_attachment_evaluations e ON e.id=s.current_attachment_evaluation_id AND e.source_id=s.id AND e.is_current
        WHERE s.id=NEW.source_id AND s.is_attachment_review_required AND s.attachment_policy_id=NEW.policy_id
          AND e.id=NEW.preview_evaluation_id AND NEW.applied_evaluation_id=e.id AND NEW.applied_attachment_version=s.attachment_row_version
          AND NEW.applied_source_version=s.classification_row_version AND NEW.applied_attachment_version=NEW.expected_attachment_version+1
          AND NEW.applied_source_version=NEW.expected_source_version
          AND NOT EXISTS(SELECT 1 FROM announcement_source_attachment_confirmations c WHERE c.source_id=s.id AND c.is_current)) THEN
        RAISE EXCEPTION 'attachment applied result and current binding mismatch' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_batch_job_application BEFORE UPDATE ON announcement_attachment_jobs
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_job_application();

-- 조건부 원복의 확인 유효성 근거. 최초 검수의 시각·작성자·버전·태그는 수정하지 않는다.
-- 이 테이블은 원복 승인/실행 API가 아니다. 승인 coordinator가 전체 입력 지문을 재검증해야 한다.
CREATE TABLE announcement_attachment_confirmation_restorations (
    id uuid PRIMARY KEY,
    job_id uuid NOT NULL UNIQUE,
    source_id uuid NOT NULL,
    confirmation_id uuid NOT NULL,
    source_version integer NOT NULL CHECK(source_version>=0),
    attachment_version integer NOT NULL CHECK(attachment_version>0),
    applied_input_hash varchar(64) NOT NULL CHECK(applied_input_hash ~ '^[0-9a-f]{64}$'),
    restored_by uuid NOT NULL REFERENCES users(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK(request_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT fk_att_confirmation_restore_job FOREIGN KEY(job_id,source_id)
        REFERENCES announcement_attachment_jobs(id,source_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_confirmation_restore_confirmation FOREIGN KEY(confirmation_id,source_id)
        REFERENCES announcement_source_attachment_confirmations(id,source_id) ON DELETE CASCADE,
    CONSTRAINT uq_att_confirmation_restore_version UNIQUE(source_id,attachment_version)
);
CREATE INDEX ix_att_confirmation_restore_lookup ON announcement_attachment_confirmation_restorations(confirmation_id,source_id,attachment_version DESC);
CREATE INDEX ix_att_confirmation_restore_actor ON announcement_attachment_confirmation_restorations(restored_by);

-- 완료된 원복만 유효 버전으로 반환한다. 같은 확인을 재적용/재원복한 경우 가장 큰 단조 증가 버전이다.
CREATE FUNCTION attachment_confirmation_restored_binding(p_source_id uuid,p_confirmation_id uuid)
RETURNS TABLE(restoration_id uuid,confirmation_id uuid,source_id uuid,source_version integer,attachment_version integer)
LANGUAGE sql STABLE AS $$
    SELECT r.id,r.confirmation_id,r.source_id,r.source_version,r.attachment_version
    FROM announcement_attachment_confirmation_restorations r
    JOIN announcement_attachment_jobs j ON j.id=r.job_id AND j.source_id=r.source_id
        AND j.application_status_code='APPLIED' AND j.rollback_status_code='ROLLED_BACK'
        AND j.previous_confirmation_id=r.confirmation_id AND j.applied_input_hash=r.applied_input_hash
        AND j.applied_source_version=r.source_version AND j.applied_attachment_version::bigint+1=r.attachment_version
    JOIN announcement_source_attachment_confirmations c ON c.id=r.confirmation_id AND c.source_id=r.source_id
        AND c.confirmed_source_version=r.source_version AND c.confirmed_attachment_version<r.attachment_version
    WHERE r.source_id=p_source_id AND r.confirmation_id=p_confirmation_id
    ORDER BY r.attachment_version DESC LIMIT 1
$$;

CREATE FUNCTION protect_attachment_confirmation_restoration() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE j record; s record; c record; prior_source integer; prior_attachment integer;
BEGIN
    IF TG_OP='DELETE' THEN
        IF EXISTS(SELECT 1 FROM announcement_source_snapshots WHERE id=OLD.source_id) THEN
            RAISE EXCEPTION 'attachment restoration evidence cannot be deleted independently' USING ERRCODE='23514';
        END IF;
        RETURN OLD;
    END IF;
    IF TG_OP='UPDATE' THEN RAISE EXCEPTION 'attachment restoration evidence is immutable' USING ERRCODE='23514'; END IF;
    -- 원문→작업 잠금으로 재검수/전환/다른 적용과 직렬화한다. base 버전은 되돌리거나 추정하지 않는다.
    SELECT id,classification_row_version,attachment_row_version,current_attachment_evaluation_id,attachment_policy_id,is_attachment_review_required
      INTO s FROM announcement_source_snapshots WHERE id=NEW.source_id FOR UPDATE;
    SELECT id,source_id,batch_id,frozen_provider_code,application_status_code,rollback_status_code,previous_confirmation_id,
        previous_attachment_evaluation_id,previous_is_review_required,previous_policy_id,applied_source_version,applied_attachment_version,
        applied_input_hash,applied_evaluation_id,policy_id,expected_source_version,expected_attachment_version,base_evaluation_id,content_version_id,rule_release_id
      INTO j FROM announcement_attachment_jobs WHERE id=NEW.job_id AND source_id=NEW.source_id FOR UPDATE;
    SELECT id,source_id,evaluation_id,set_hash,is_current,confirmed_source_version,confirmed_attachment_version
      INTO c FROM announcement_source_attachment_confirmations WHERE id=NEW.confirmation_id AND source_id=NEW.source_id;
    IF s.id IS NULL OR j.id IS NULL OR c.id IS NULL OR j.frozen_provider_code IS NULL OR j.batch_id IS NULL
        OR j.application_status_code<>'APPLIED' OR j.rollback_status_code<>'NOT_REQUESTED'
        OR j.previous_confirmation_id IS DISTINCT FROM c.id OR j.previous_attachment_evaluation_id IS DISTINCT FROM c.evaluation_id
        OR j.previous_is_review_required IS DISTINCT FROM true OR c.is_current
        OR j.applied_source_version IS DISTINCT FROM NEW.source_version
        OR j.applied_attachment_version IS NULL OR j.applied_attachment_version::bigint+1<>NEW.attachment_version
        OR j.applied_input_hash IS DISTINCT FROM NEW.applied_input_hash
        OR s.classification_row_version IS DISTINCT FROM j.applied_source_version
        OR s.attachment_row_version IS DISTINCT FROM j.applied_attachment_version
        OR s.current_attachment_evaluation_id IS DISTINCT FROM j.applied_evaluation_id
        OR s.attachment_policy_id IS DISTINCT FROM j.policy_id OR NOT s.is_attachment_review_required
        OR EXISTS(SELECT 1 FROM announcement_source_links WHERE source_id=s.id)
        OR EXISTS(SELECT 1 FROM announcement_source_attachment_confirmations WHERE source_id=s.id AND is_current)
        OR EXISTS(SELECT 1 FROM announcement_attachment_jobs q WHERE q.source_id=s.id AND q.id<>j.id
            AND q.job_status_code IN ('SCOPE_READY','PENDING','RUNNING','RETRY_WAIT','PAUSED')) THEN
        RAISE EXCEPTION 'attachment restoration requires unchanged applied source and previous confirmation' USING ERRCODE='23514';
    END IF;
    SELECT coalesce(r.source_version,c.confirmed_source_version),coalesce(r.attachment_version,c.confirmed_attachment_version)
      INTO prior_source,prior_attachment FROM (VALUES(1)) anchor(n)
      LEFT JOIN LATERAL attachment_confirmation_restored_binding(c.source_id,c.id) r ON true;
    IF prior_source IS NULL OR prior_attachment IS NULL OR prior_source IS DISTINCT FROM j.expected_source_version
        OR prior_attachment IS DISTINCT FROM j.expected_attachment_version
        OR NOT EXISTS(SELECT 1 FROM announcement_source_attachment_evaluations e
            JOIN announcement_source_attachment_sets aset ON aset.id=e.set_id AND aset.source_id=e.source_id AND aset.set_status_code='SEALED'
            JOIN announcement_source_classification_evaluations b ON b.id=e.base_evaluation_id AND b.source_id=e.source_id AND b.is_current
            WHERE e.id=c.evaluation_id AND e.source_id=s.id AND e.base_evaluation_id=j.base_evaluation_id
                AND e.content_version_id=j.content_version_id AND e.rule_release_id=j.rule_release_id
                AND e.policy_id=j.previous_policy_id AND aset.manifest_hash=c.set_hash) THEN
        RAISE EXCEPTION 'stale or unversioned prior confirmation cannot be revalidated by restoration' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_confirmation_restoration BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_confirmation_restorations
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_confirmation_restoration();

CREATE FUNCTION check_attachment_confirmation_restoration_complete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM announcement_source_snapshots WHERE id=NEW.source_id) THEN RETURN NULL; END IF;
    IF NOT EXISTS(SELECT 1 FROM announcement_attachment_jobs j
        JOIN announcement_source_snapshots s ON s.id=j.source_id
        JOIN announcement_source_attachment_evaluations e ON e.id=j.previous_attachment_evaluation_id AND e.source_id=s.id AND e.is_current
        JOIN announcement_source_attachment_confirmations c ON c.id=NEW.confirmation_id AND c.source_id=s.id AND c.evaluation_id=e.id AND c.is_current
        WHERE j.id=NEW.job_id AND j.source_id=NEW.source_id AND j.rollback_status_code='ROLLED_BACK' AND j.application_status_code='APPLIED'
            AND s.classification_row_version=NEW.source_version AND s.attachment_row_version=NEW.attachment_version
            AND s.current_attachment_evaluation_id=e.id AND s.attachment_policy_id=j.previous_policy_id
            AND s.is_attachment_review_required=j.previous_is_review_required
            AND NOT EXISTS(SELECT 1 FROM announcement_source_links WHERE source_id=s.id)) THEN
        RAISE EXCEPTION 'attachment restoration must complete source evaluation confirmation and job atomically' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_confirmation_restoration_complete AFTER INSERT ON announcement_attachment_confirmation_restorations
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_confirmation_restoration_complete();

-- 만료된 확인을 is_current=true만으로 부활시킬 수 없다. 원복 근거와 현재 증가 버전이 함께 있어야 한다.
CREATE OR REPLACE FUNCTION protect_attachment_confirmation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (to_jsonb(NEW)-'is_current') IS DISTINCT FROM (to_jsonb(OLD)-'is_current') THEN
        RAISE EXCEPTION 'attachment confirmation is immutable' USING ERRCODE='23514';
    END IF;
    IF NOT OLD.is_current AND NEW.is_current AND NOT EXISTS(
        SELECT 1 FROM announcement_attachment_confirmation_restorations r
        JOIN announcement_source_snapshots s ON s.id=r.source_id
        WHERE r.confirmation_id=NEW.id AND r.source_id=NEW.source_id
            AND s.classification_row_version=r.source_version AND s.attachment_row_version=r.attachment_version
            AND s.current_attachment_evaluation_id=NEW.evaluation_id AND s.is_attachment_review_required) THEN
        RAISE EXCEPTION 'stale confirmation requires matching restoration evidence' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;

-- 원복 승인과 고정된 적용 완료분 전체. 기존 scope/적용 이력을 지우거나 다시 초기화하지 않는다.
CREATE TABLE announcement_attachment_batch_rollback_actions (
    id uuid PRIMARY KEY,batch_id uuid NOT NULL UNIQUE REFERENCES announcement_attachment_batches(id),
    expected_version integer NOT NULL CHECK(expected_version>=0 AND expected_version<2147483647),
    preview_hash varchar(64) NOT NULL CHECK(preview_hash ~ '^[0-9a-f]{64}$'),
    scope_count integer NOT NULL CHECK(scope_count BETWEEN 1 AND 1000),
    target_count integer NOT NULL CHECK(target_count BETWEEN 1 AND scope_count),
    eligible_count integer NOT NULL CHECK(eligible_count BETWEEN 1 AND target_count),
    deleted_count integer NOT NULL CHECK(deleted_count BETWEEN 0 AND scope_count),
    base_reopen_count integer NOT NULL CHECK(base_reopen_count BETWEEN 0 AND eligible_count),
    confirmation_restore_count integer NOT NULL CHECK(confirmation_restore_count BETWEEN 0 AND eligible_count),
    cancel_pending_count integer NOT NULL CHECK(cancel_pending_count BETWEEN 0 AND scope_count),
    actor_id uuid NOT NULL REFERENCES users(id),idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK(request_hash ~ '^[0-9a-f]{64}$'),
    reason_hash varchar(64) NOT NULL CHECK(reason_hash ~ '^[0-9a-f]{64}$'),created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    UNIQUE(id,batch_id),CHECK(target_count+deleted_count+cancel_pending_count<=scope_count)
);
CREATE INDEX ix_att_batch_rollback_actor ON announcement_attachment_batch_rollback_actions(actor_id);
ALTER TABLE announcement_attachment_batches ADD COLUMN rollback_approval_id uuid,
    ADD CONSTRAINT fk_att_batch_rollback_approval FOREIGN KEY(rollback_approval_id,id) REFERENCES announcement_attachment_batch_rollback_actions(id,batch_id) DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT ck_att_batch_rollback_approval CHECK(scope_item_count IS NULL OR batch_status_code NOT IN ('ROLLING_BACK','ROLLED_BACK','ROLLBACK_PARTIAL_FAILED') OR rollback_approval_id IS NOT NULL);
CREATE INDEX ix_att_batch_rollback_approval ON announcement_attachment_batches(rollback_approval_id,id);
CREATE TABLE announcement_attachment_batch_rollback_items (
    action_id uuid NOT NULL,batch_id uuid NOT NULL,job_id uuid NOT NULL,
    input_hash varchar(64) NOT NULL CHECK(input_hash ~ '^[0-9a-f]{64}$'),
    readiness_code varchar(80) NOT NULL,is_eligible boolean NOT NULL,is_base_reopened boolean NOT NULL,is_confirmation_restored boolean NOT NULL,
    PRIMARY KEY(action_id,job_id),FOREIGN KEY(action_id,batch_id) REFERENCES announcement_attachment_batch_rollback_actions(id,batch_id),
    FOREIGN KEY(job_id,batch_id) REFERENCES announcement_attachment_jobs(id,batch_id) ON DELETE CASCADE,
    CHECK(is_eligible=(readiness_code='READY')),CHECK(NOT is_base_reopened OR is_eligible),CHECK(NOT is_confirmation_restored OR is_eligible),
    CHECK(NOT(is_base_reopened AND is_confirmation_restored))
);
CREATE INDEX ix_att_batch_rollback_item_job ON announcement_attachment_batch_rollback_items(job_id,batch_id);
ALTER TABLE announcement_attachment_jobs
    ADD COLUMN rollback_action_id uuid,ADD COLUMN rollback_error_code varchar(80),
    ADD COLUMN rollback_attempt_count integer NOT NULL DEFAULT 0 CHECK(rollback_attempt_count BETWEEN 0 AND 3),
    ADD COLUMN rollback_next_attempt_at timestamptz,ADD COLUMN restored_source_version integer CHECK(restored_source_version>=0),
    ADD COLUMN restored_attachment_version integer CHECK(restored_attachment_version>=0),ADD COLUMN restored_confirmation_id uuid,
    ADD CONSTRAINT fk_att_job_rollback_action FOREIGN KEY(rollback_action_id,batch_id) REFERENCES announcement_attachment_batch_rollback_actions(id,batch_id),
    ADD CONSTRAINT fk_att_job_restored_confirmation FOREIGN KEY(restored_confirmation_id,source_id) REFERENCES announcement_source_attachment_confirmations(id,source_id),
    ADD CONSTRAINT ck_att_job_rollback_result CHECK(rollback_action_id IS NULL OR rollback_status_code<>'ROLLED_BACK' OR (restored_source_version IS NOT NULL AND restored_attachment_version IS NOT NULL));
CREATE INDEX ix_att_job_rollback_action ON announcement_attachment_jobs(rollback_action_id,batch_id);
CREATE INDEX ix_att_job_restored_confirmation ON announcement_attachment_jobs(restored_confirmation_id,source_id) WHERE restored_confirmation_id IS NOT NULL;
CREATE INDEX ix_att_job_rollback_pending ON announcement_attachment_jobs(rollback_next_attempt_at,batch_id,id) WHERE rollback_action_id IS NOT NULL AND rollback_status_code='NOT_REQUESTED';

CREATE FUNCTION protect_attachment_batch_rollback_action() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE b record;
BEGIN
    IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'attachment rollback approval is immutable' USING ERRCODE='23514'; END IF;
    SELECT id,row_version,batch_status_code,scope_item_count,deleted_item_count,rollback_approval_id INTO b FROM announcement_attachment_batches WHERE id=NEW.batch_id FOR UPDATE;
    IF b.id IS NULL OR b.row_version<>NEW.expected_version OR b.scope_item_count IS DISTINCT FROM NEW.scope_count OR b.deleted_item_count<>NEW.deleted_count
        OR b.rollback_approval_id IS NOT NULL OR b.batch_status_code NOT IN ('APPLIED','APPLY_PARTIAL_FAILED','APPLY_PAUSED')
        OR NEW.target_count<>(SELECT count(1) FROM announcement_attachment_jobs WHERE batch_id=b.id AND application_status_code='APPLIED' AND rollback_status_code='NOT_REQUESTED')
        OR NEW.cancel_pending_count<>(SELECT count(1) FROM announcement_attachment_jobs WHERE batch_id=b.id AND application_status_code='PENDING') THEN
        RAISE EXCEPTION 'attachment rollback approval does not match full applied scope' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_batch_rollback_action BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_batch_rollback_actions FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_rollback_action();

CREATE FUNCTION protect_attachment_batch_rollback_item() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        IF EXISTS(SELECT 1 FROM announcement_attachment_jobs WHERE id=OLD.job_id) THEN RAISE EXCEPTION 'attachment rollback scope cannot be reduced' USING ERRCODE='23514'; END IF;
        RETURN OLD;
    END IF;
    IF TG_OP='UPDATE' THEN RAISE EXCEPTION 'attachment rollback target is immutable' USING ERRCODE='23514'; END IF;
    IF NOT EXISTS(SELECT 1 FROM announcement_attachment_batch_rollback_actions a JOIN announcement_attachment_batches b ON b.id=a.batch_id
        JOIN announcement_attachment_jobs j ON j.id=NEW.job_id AND j.batch_id=b.id
        WHERE a.id=NEW.action_id AND a.batch_id=NEW.batch_id AND b.row_version=a.expected_version AND b.rollback_approval_id IS NULL
            AND j.application_status_code='APPLIED' AND j.rollback_status_code='NOT_REQUESTED' AND j.rollback_action_id IS NULL) THEN
        RAISE EXCEPTION 'attachment rollback target must belong to new approval' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_batch_rollback_item BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_batch_rollback_items FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_rollback_item();

CREATE FUNCTION check_attachment_batch_rollback_action_complete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM announcement_attachment_batches b WHERE b.id=NEW.batch_id AND b.rollback_approval_id=NEW.id
        AND b.batch_status_code='ROLLING_BACK' AND b.row_version=NEW.expected_version+1)
        OR NEW.target_count<>(SELECT count(1) FROM announcement_attachment_batch_rollback_items WHERE action_id=NEW.id)
        OR NEW.eligible_count<>(SELECT count(1) FROM announcement_attachment_batch_rollback_items WHERE action_id=NEW.id AND is_eligible)
        OR NEW.base_reopen_count<>(SELECT count(1) FROM announcement_attachment_batch_rollback_items WHERE action_id=NEW.id AND is_base_reopened)
        OR NEW.confirmation_restore_count<>(SELECT count(1) FROM announcement_attachment_batch_rollback_items WHERE action_id=NEW.id AND is_confirmation_restored)
        OR NEW.target_count<>(SELECT count(1) FROM announcement_attachment_jobs WHERE batch_id=NEW.batch_id AND rollback_action_id=NEW.id AND rollback_status_code='NOT_REQUESTED')
        OR NEW.cancel_pending_count<>(SELECT count(1) FROM announcement_attachment_jobs WHERE batch_id=NEW.batch_id AND application_status_code='CONFLICT' AND application_error_code='APPLICATION_CANCELLED_BY_ROLLBACK') THEN
        RAISE EXCEPTION 'attachment rollback approval scope queue and cancellation must complete atomically' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_batch_rollback_action_complete AFTER INSERT ON announcement_attachment_batch_rollback_actions DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_batch_rollback_action_complete();

CREATE FUNCTION protect_attachment_batch_rollback_binding() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.rollback_approval_id IS NOT NULL AND NEW.rollback_approval_id IS DISTINCT FROM OLD.rollback_approval_id THEN
        RAISE EXCEPTION 'attachment rollback approval binding is immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.rollback_approval_id IS NULL AND NEW.rollback_approval_id IS NOT NULL AND NOT EXISTS(
        SELECT 1 FROM announcement_attachment_batch_rollback_actions a WHERE a.id=NEW.rollback_approval_id AND a.batch_id=NEW.id
            AND a.expected_version=OLD.row_version AND NEW.row_version=OLD.row_version+1 AND NEW.batch_status_code='ROLLING_BACK') THEN
        RAISE EXCEPTION 'attachment rollback requires matching approval' USING ERRCODE='23514';
    END IF;
    IF OLD.rollback_approval_id IS NOT NULL AND NEW.batch_status_code IS DISTINCT FROM OLD.batch_status_code
        AND NOT(OLD.batch_status_code='ROLLING_BACK' AND NEW.batch_status_code IN ('ROLLED_BACK','ROLLBACK_PARTIAL_FAILED')) THEN
        RAISE EXCEPTION 'attachment rollback cannot reset or resume application' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_batch_rollback_binding BEFORE UPDATE ON announcement_attachment_batches FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_rollback_binding();

CREATE FUNCTION protect_attachment_job_rollback() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.frozen_provider_code IS NULL THEN RETURN NEW; END IF;
    IF OLD.rollback_status_code IN ('ROLLED_BACK','CONFLICT','FAILED') AND
        (NEW.rollback_status_code,NEW.rollback_action_id,NEW.restored_source_version,NEW.restored_attachment_version,NEW.restored_confirmation_id)
        IS DISTINCT FROM (OLD.rollback_status_code,OLD.rollback_action_id,OLD.restored_source_version,OLD.restored_attachment_version,OLD.restored_confirmation_id) THEN
        RAISE EXCEPTION 'attachment rollback terminal result is immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.rollback_action_id IS NOT NULL AND NEW.rollback_action_id IS DISTINCT FROM OLD.rollback_action_id THEN
        RAISE EXCEPTION 'attachment rollback job approval is immutable' USING ERRCODE='23514';
    END IF;
    IF NEW.rollback_action_id IS DISTINCT FROM OLD.rollback_action_id OR NEW.rollback_status_code IS DISTINCT FROM OLD.rollback_status_code THEN
        IF NEW.application_status_code<>'APPLIED' OR NOT EXISTS(SELECT 1 FROM announcement_attachment_batches b
            JOIN announcement_attachment_batch_rollback_items i ON i.action_id=b.rollback_approval_id AND i.batch_id=b.id AND i.job_id=NEW.id
            WHERE b.id=NEW.batch_id AND b.batch_status_code='ROLLING_BACK' AND NEW.rollback_action_id=i.action_id) THEN
            RAISE EXCEPTION 'attachment rollback job requires approved scope' USING ERRCODE='23514';
        END IF;
    END IF;
    IF OLD.rollback_status_code<>'ROLLED_BACK' AND NEW.rollback_status_code='ROLLED_BACK' AND NOT EXISTS(
        SELECT 1 FROM announcement_source_snapshots s WHERE s.id=NEW.source_id
            AND s.classification_row_version=NEW.applied_source_version AND s.attachment_row_version::bigint=NEW.applied_attachment_version::bigint+1
            AND NEW.restored_source_version=s.classification_row_version AND NEW.restored_attachment_version=s.attachment_row_version
            AND s.current_attachment_evaluation_id IS NOT DISTINCT FROM NEW.previous_attachment_evaluation_id
            AND s.attachment_policy_id IS NOT DISTINCT FROM NEW.previous_policy_id AND s.is_attachment_review_required IS NOT DISTINCT FROM NEW.previous_is_review_required
            AND NOT EXISTS(SELECT 1 FROM announcement_source_links WHERE source_id=s.id)
            AND (NEW.previous_attachment_evaluation_id IS NULL OR EXISTS(SELECT 1 FROM announcement_source_attachment_evaluations e WHERE e.id=NEW.previous_attachment_evaluation_id AND e.source_id=s.id AND e.is_current))
            AND (SELECT c.id FROM announcement_source_attachment_confirmations c WHERE c.source_id=s.id AND c.is_current) IS NOT DISTINCT FROM NEW.restored_confirmation_id) THEN
        RAISE EXCEPTION 'attachment rollback result does not match restored source' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_job_rollback BEFORE UPDATE ON announcement_attachment_jobs FOR EACH ROW EXECUTE FUNCTION protect_attachment_job_rollback();

-- 일반 작업은 예약 때 첨부 버전 +1, 판정 저장 때 +1이다. 배치의 예약 버전과 혼용하지 않는다.
-- V72 과거 작업은 소급해서 복구 가능하다고 표시하지 않는다. V73 실행 snapshot이 있는 신규 일반 작업만 고정한다.
ALTER TABLE announcement_attachment_jobs
    ADD COLUMN reservation_source_version integer CHECK(reservation_source_version>=0),
    ADD COLUMN reservation_attachment_version integer CHECK(reservation_attachment_version>=0),
    ADD COLUMN reservation_locator_hash varchar(64) CHECK(reservation_locator_hash ~ '^[0-9a-f]{64}$'),
    ADD COLUMN reservation_context_hash varchar(64) CHECK(reservation_context_hash ~ '^[0-9a-f]{64}$'),
    ADD COLUMN is_reservation_previous_evaluation_current boolean,
    ADD COLUMN is_reservation_confirmation_valid boolean,
    ADD CONSTRAINT ck_att_normal_reservation_evidence CHECK(reservation_source_version IS NULL OR
        (batch_id IS NULL AND reservation_attachment_version IS NOT NULL AND reservation_locator_hash IS NOT NULL AND reservation_context_hash IS NOT NULL
         AND is_reservation_previous_evaluation_current IS NOT NULL AND is_reservation_confirmation_valid IS NOT NULL
         AND expected_source_version=reservation_source_version
         AND expected_attachment_version::bigint=reservation_attachment_version::bigint+1)),
    ADD CONSTRAINT ck_att_normal_application_evidence CHECK(reservation_source_version IS NULL OR application_status_code<>'APPLIED'
        OR (applied_source_version IS NOT NULL AND applied_evaluation_id IS NOT NULL AND applied_attachment_version IS NOT NULL AND applied_input_hash IS NOT NULL));

CREATE FUNCTION capture_attachment_normal_reservation() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE s record; confirmation_key uuid;
BEGIN
    IF NEW.batch_id IS NOT NULL OR NEW.execution_snapshot_json IS NULL THEN RETURN NEW; END IF;
    SELECT id,classification_row_version,attachment_row_version,current_attachment_evaluation_id,
        attachment_policy_id,is_attachment_review_required INTO s FROM announcement_source_snapshots WHERE id=NEW.source_id FOR UPDATE;
    SELECT id INTO confirmation_key FROM announcement_source_attachment_confirmations WHERE source_id=NEW.source_id AND is_current;
    IF s.id IS NULL OR s.attachment_row_version>2147483645
        OR NEW.job_status_code<>'PENDING' OR NEW.application_status_code NOT IN ('PENDING','NOT_REQUESTED')
        OR NEW.expected_source_version IS DISTINCT FROM s.classification_row_version
        OR NEW.expected_attachment_version::bigint IS DISTINCT FROM s.attachment_row_version::bigint+1
        OR (NEW.previous_attachment_evaluation_id,NEW.previous_policy_id,NEW.previous_is_review_required,NEW.previous_confirmation_id)
            IS DISTINCT FROM (s.current_attachment_evaluation_id,s.attachment_policy_id,s.is_attachment_review_required,confirmation_key) THEN
        RAISE EXCEPTION 'attachment normal reservation must capture the exact previous binding' USING ERRCODE='23514';
    END IF;
    NEW.reservation_source_version:=s.classification_row_version;
    NEW.reservation_attachment_version:=s.attachment_row_version;
    NEW.reservation_locator_hash:=attachment_source_locator_hash(s.id);
    NEW.reservation_context_hash:=attachment_normal_reservation_context_hash(s.id,NEW.base_evaluation_id);
    IF NEW.reservation_locator_hash IS NULL OR NEW.reservation_context_hash IS NULL THEN
        RAISE EXCEPTION 'attachment normal reservation requires an enabled source locator' USING ERRCODE='23514';
    END IF;
    NEW.is_reservation_previous_evaluation_current:=EXISTS(SELECT 1 FROM announcement_source_attachment_evaluations e
        WHERE e.id=s.current_attachment_evaluation_id AND e.source_id=s.id AND e.is_current);
    NEW.is_reservation_confirmation_valid:=EXISTS(SELECT 1 FROM announcement_source_attachment_confirmations c
        LEFT JOIN LATERAL attachment_confirmation_restored_binding(c.source_id,c.id) r ON true
        WHERE c.id=confirmation_key AND c.source_id=s.id AND c.is_current AND s.is_attachment_review_required
            AND c.evaluation_id=s.current_attachment_evaluation_id AND NEW.is_reservation_previous_evaluation_current
            AND EXISTS(SELECT 1 FROM announcement_source_attachment_evaluations e
                JOIN announcement_source_attachment_sets aset ON aset.id=e.set_id AND aset.source_id=e.source_id AND aset.set_status_code='SEALED'
                WHERE e.id=c.evaluation_id AND e.source_id=s.id AND e.base_evaluation_id=NEW.base_evaluation_id
                    AND e.content_version_id=NEW.content_version_id AND e.rule_release_id=NEW.rule_release_id
                    AND e.policy_id=s.attachment_policy_id AND aset.manifest_hash=c.set_hash)
            AND coalesce(r.source_version,c.confirmed_source_version)=s.classification_row_version
            AND coalesce(r.attachment_version,c.confirmed_attachment_version)=s.attachment_row_version);
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_normal_reservation BEFORE INSERT ON announcement_attachment_jobs FOR EACH ROW EXECUTE FUNCTION capture_attachment_normal_reservation();

-- 외부 요청·원문 복사 없이 현재 적용 연결의 정규화 지문만 계산한다.
-- ACTIVE/OFF/퇴역 상태와 실행 횟수는 제외한다. 정책 OFF 자체는 이미 적용된 검수 의무의 원복이 아니다.
CREATE FUNCTION attachment_normal_job_application_hash(job_key uuid,evaluation_key uuid) RETURNS text LANGUAGE sql STABLE AS $$
    SELECT encode(digest(jsonb_build_array('attachment-normal-application-v1',
        j.id,j.source_id,j.content_version_id,j.base_evaluation_id,j.rule_release_id,j.policy_id,j.set_id,
        j.operation_code,j.reference_set_id,j.request_hash,j.execution_snapshot_json,
        j.reservation_source_version,j.reservation_attachment_version,j.reservation_locator_hash,j.reservation_context_hash,
        j.is_reservation_previous_evaluation_current,j.is_reservation_confirmation_valid,
        j.previous_attachment_evaluation_id,j.previous_confirmation_id,j.previous_policy_id,j.previous_is_review_required,
        s.classification_row_version,s.attachment_row_version,s.current_attachment_evaluation_id,s.attachment_policy_id,
        s.is_attachment_review_required,s.semantic_status_code,s.data_purpose_code,attachment_source_locator_hash(s.id),
        encode(digest(to_jsonb(s.agency_name)::text,'sha256'),'hex'),
        base.id,base.is_current,base.decision_status_code,base.title_stage_code,
        e.id,e.is_current,e.input_hash,e.decision_hash,aset.manifest_hash,p.policy_hash,r.rule_snapshot_hash,
        (SELECT c.id FROM announcement_source_attachment_confirmations c WHERE c.source_id=s.id AND c.is_current),
        EXISTS(SELECT 1 FROM announcement_source_links l WHERE l.source_id=s.id),
        EXISTS(SELECT 1 FROM announcement_attachment_jobs other WHERE other.source_id=s.id AND other.id<>j.id
            AND other.job_status_code IN ('PENDING','RUNNING','RETRY_WAIT','SCOPE_READY','PAUSED'))
    )::text,'sha256'),'hex')
    FROM announcement_attachment_jobs j JOIN announcement_source_snapshots s ON s.id=j.source_id
    JOIN announcement_source_classification_evaluations base ON base.id=j.base_evaluation_id AND base.source_id=s.id
    JOIN announcement_source_attachment_evaluations e ON e.id=evaluation_key AND e.source_id=s.id
        AND e.base_evaluation_id=j.base_evaluation_id AND e.content_version_id=j.content_version_id
        AND e.policy_id=j.policy_id AND e.rule_release_id=j.rule_release_id AND e.set_id=j.set_id
    JOIN announcement_source_attachment_sets aset ON aset.id=e.set_id AND aset.source_id=s.id AND aset.set_status_code='SEALED'
    JOIN announcement_attachment_policies p ON p.id=j.policy_id
    JOIN announcement_source_classification_rule_releases r ON r.id=j.rule_release_id
    WHERE j.id=job_key AND j.batch_id IS NULL AND j.reservation_source_version IS NOT NULL
$$;

CREATE FUNCTION protect_attachment_normal_job_evidence() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.reservation_source_version,NEW.reservation_attachment_version,NEW.reservation_locator_hash,NEW.reservation_context_hash,
        NEW.is_reservation_previous_evaluation_current,NEW.is_reservation_confirmation_valid)
        IS DISTINCT FROM (OLD.reservation_source_version,OLD.reservation_attachment_version,OLD.reservation_locator_hash,OLD.reservation_context_hash,
        OLD.is_reservation_previous_evaluation_current,OLD.is_reservation_confirmation_valid) THEN
        RAISE EXCEPTION 'attachment normal reservation evidence is immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.reservation_source_version IS NULL THEN RETURN NEW; END IF;
    IF (NEW.previous_attachment_evaluation_id,NEW.previous_confirmation_id,NEW.previous_policy_id,NEW.previous_is_review_required)
        IS DISTINCT FROM (OLD.previous_attachment_evaluation_id,OLD.previous_confirmation_id,OLD.previous_policy_id,OLD.previous_is_review_required) THEN
        RAISE EXCEPTION 'attachment normal previous bindings are immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.application_status_code='APPLIED' AND
        (NEW.application_status_code,NEW.applied_evaluation_id,NEW.applied_attachment_version,NEW.applied_source_version,NEW.applied_input_hash,
         NEW.preview_evaluation_id,NEW.preview_hash,NEW.set_id)
        IS DISTINCT FROM (OLD.application_status_code,OLD.applied_evaluation_id,OLD.applied_attachment_version,OLD.applied_source_version,OLD.applied_input_hash,
         OLD.preview_evaluation_id,OLD.preview_hash,OLD.set_id) THEN
        RAISE EXCEPTION 'attachment normal applied evidence is immutable' USING ERRCODE='23514';
    END IF;
    IF NEW.application_status_code IS DISTINCT FROM OLD.application_status_code THEN
        IF OLD.application_status_code<>'PENDING' OR NEW.application_status_code<>'APPLIED' OR OLD.job_status_code<>'RUNNING'
            OR NEW.job_status_code NOT IN ('SUCCEEDED','PARTIAL_FAILED') OR OLD.lease_expires_at IS NULL OR OLD.lease_expires_at<=clock_timestamp()
            OR OLD.lease_token IS NULL OR NEW.lease_token IS DISTINCT FROM OLD.lease_token
            OR NEW.applied_evaluation_id IS DISTINCT FROM NEW.preview_evaluation_id
            OR NEW.applied_source_version IS DISTINCT FROM NEW.reservation_source_version
            OR NEW.applied_attachment_version::bigint IS DISTINCT FROM NEW.reservation_attachment_version::bigint+2
            OR NEW.applied_input_hash IS NULL
            OR NEW.applied_input_hash IS DISTINCT FROM attachment_normal_job_application_hash(OLD.id,NEW.applied_evaluation_id)
            OR NOT EXISTS(SELECT 1 FROM announcement_source_snapshots s
                JOIN announcement_source_attachment_evaluations e ON e.id=s.current_attachment_evaluation_id AND e.source_id=s.id AND e.is_current
                WHERE s.id=NEW.source_id AND s.classification_row_version=NEW.applied_source_version
                    AND s.attachment_row_version=NEW.applied_attachment_version AND e.id=NEW.applied_evaluation_id
                    AND s.is_attachment_review_required AND s.attachment_policy_id=NEW.policy_id
                    AND attachment_source_locator_hash(s.id)=NEW.reservation_locator_hash
                    AND NOT EXISTS(SELECT 1 FROM announcement_source_links l WHERE l.source_id=s.id)
                    AND NOT EXISTS(SELECT 1 FROM announcement_source_attachment_confirmations c WHERE c.source_id=s.id AND c.is_current)) THEN
            RAISE EXCEPTION 'attachment normal application evidence or current binding mismatch' USING ERRCODE='23514';
        END IF;
    END IF;
    -- 일반 원복은 단일 source의 명시적 승인과 복구를 같은 transaction에서 완료한다.
    IF (NEW.rollback_status_code,NEW.rollback_action_id,NEW.normal_rollback_action_id,NEW.restored_source_version,NEW.restored_attachment_version,NEW.restored_confirmation_id)
        IS DISTINCT FROM (OLD.rollback_status_code,OLD.rollback_action_id,OLD.normal_rollback_action_id,OLD.restored_source_version,OLD.restored_attachment_version,OLD.restored_confirmation_id) THEN
        IF OLD.rollback_status_code<>'NOT_REQUESTED' OR NEW.rollback_status_code<>'ROLLED_BACK'
            OR OLD.normal_rollback_action_id IS NOT NULL OR NEW.rollback_action_id IS NOT NULL
            OR NOT EXISTS(SELECT 1 FROM announcement_attachment_normal_rollback_actions a
                WHERE a.id=NEW.normal_rollback_action_id AND a.job_id=NEW.id AND a.source_id=NEW.source_id
                    AND NEW.restored_source_version=a.expected_source_version AND NEW.restored_attachment_version::bigint=a.expected_attachment_version::bigint+1
                    AND NEW.restored_confirmation_id IS NOT DISTINCT FROM CASE WHEN a.is_confirmation_restored THEN NEW.previous_confirmation_id ELSE NULL END) THEN
            RAISE EXCEPTION 'attachment normal rollback requires a dedicated approval contract' USING ERRCODE='23514';
        END IF;
    END IF;
    IF OLD.normal_rollback_action_id IS NOT NULL AND (NEW.job_status_code,NEW.set_id,NEW.preview_evaluation_id,NEW.preview_hash)
        IS DISTINCT FROM (OLD.job_status_code,OLD.set_id,OLD.preview_evaluation_id,OLD.preview_hash) THEN
        RAISE EXCEPTION 'attachment recovered normal execution cannot be restarted' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_normal_job_evidence BEFORE UPDATE ON announcement_attachment_jobs FOR EACH ROW EXECUTE FUNCTION protect_attachment_normal_job_evidence();

-- 실패 예약도 당시 기관/원문 입력이 바뀌면 이전 검수로 덮어쓰지 않는다. 원문은 복사하지 않는다.
CREATE FUNCTION attachment_normal_reservation_context_hash(source_key uuid,base_key uuid) RETURNS text LANGUAGE sql STABLE AS $$
    SELECT encode(digest(jsonb_build_array(attachment_source_locator_hash(s.id),s.agency_name,
        b.id,b.content_version_id,b.rule_release_id,b.decision_status_code,b.title_stage_code,b.is_current)::text,'sha256'),'hex')
    FROM announcement_source_snapshots s JOIN announcement_source_classification_evaluations b ON b.source_id=s.id
    WHERE s.id=source_key AND b.id=base_key AND b.is_current
$$;

CREATE TABLE announcement_attachment_normal_rollback_actions (
    id uuid PRIMARY KEY,job_id uuid NOT NULL UNIQUE,source_id uuid NOT NULL,
    mode_code varchar(30) NOT NULL CHECK(mode_code IN ('APPLIED','FAILED_RESERVATION')),
    expected_source_version integer NOT NULL CHECK(expected_source_version>=0),
    expected_attachment_version integer NOT NULL CHECK(expected_attachment_version BETWEEN 0 AND 2147483646),
    preview_hash varchar(64) NOT NULL CHECK(preview_hash ~ '^[0-9a-f]{64}$'),
    is_base_reopened boolean NOT NULL,is_confirmation_restored boolean NOT NULL,
    actor_id uuid NOT NULL REFERENCES users(id),idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK(request_hash ~ '^[0-9a-f]{64}$'),
    reason_hash varchar(64) NOT NULL CHECK(reason_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    FOREIGN KEY(job_id,source_id) REFERENCES announcement_attachment_jobs(id,source_id) ON DELETE CASCADE,
    UNIQUE(id,job_id),CHECK(NOT(is_base_reopened AND is_confirmation_restored))
);
CREATE INDEX ix_att_normal_rollback_actor ON announcement_attachment_normal_rollback_actions(actor_id);
CREATE INDEX ix_att_normal_rollback_source ON announcement_attachment_normal_rollback_actions(source_id);
ALTER TABLE announcement_attachment_jobs ADD COLUMN normal_rollback_action_id uuid,
    ADD CONSTRAINT fk_att_normal_rollback_action FOREIGN KEY(normal_rollback_action_id,id)
        REFERENCES announcement_attachment_normal_rollback_actions(id,job_id) DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT ck_att_normal_rollback_scope CHECK(normal_rollback_action_id IS NULL OR (batch_id IS NULL AND rollback_action_id IS NULL));
CREATE INDEX ix_att_normal_rollback_action ON announcement_attachment_jobs(normal_rollback_action_id,id);

-- 읽기 미리보기와 승인 INSERT가 동일한 중앙 판정 함수를 사용한다. JSON에는 식별자·상태·hash만 있다.
CREATE FUNCTION attachment_normal_job_recovery_state(job_key uuid) RETURNS jsonb LANGUAGE sql STABLE AS $$
WITH facts AS (
    SELECT j.id,j.source_id,j.row_version,j.job_status_code,j.application_status_code,j.rollback_status_code,
        j.previous_attachment_evaluation_id,j.previous_confirmation_id,j.previous_is_review_required,
        j.reservation_source_version,j.reservation_attachment_version,j.reservation_context_hash,
        s.classification_row_version,s.attachment_row_version,
        CASE WHEN j.application_status_code='APPLIED' AND j.job_status_code IN ('SUCCEEDED','PARTIAL_FAILED') THEN 'APPLIED'
            WHEN j.application_status_code='PENDING' AND j.job_status_code IN ('FAILED','CONFLICT','CANCELLED') THEN 'FAILED_RESERVATION' ELSE 'UNAVAILABLE' END AS mode_code,
        (s.data_purpose_code='PRODUCTION' AND s.semantic_status_code<>'EXCLUDED' AND base.is_current
            AND base.decision_status_code<>'EXCLUDED' AND base.title_stage_code IN ('COMBINATION_MATCHED','GROUP_A_MATCHED')
            AND s.classification_row_version=j.reservation_source_version AND s.attachment_row_version<2147483647
            AND s.is_attachment_review_required AND s.attachment_policy_id=j.policy_id
            AND j.reservation_context_hash=attachment_normal_reservation_context_hash(s.id,j.base_evaluation_id)
            AND NOT EXISTS(SELECT 1 FROM announcement_source_links l WHERE l.source_id=s.id)
            AND NOT EXISTS(SELECT 1 FROM announcement_source_attachment_confirmations c WHERE c.source_id=s.id AND c.is_current)
            AND NOT EXISTS(SELECT 1 FROM announcement_attachment_jobs other WHERE other.source_id=s.id AND other.id<>j.id
                AND other.job_status_code IN ('SCOPE_READY','PENDING','RUNNING','RETRY_WAIT','PAUSED'))
            AND ((j.application_status_code='APPLIED' AND s.current_attachment_evaluation_id=j.applied_evaluation_id
                    AND s.classification_row_version=j.applied_source_version AND s.attachment_row_version=j.applied_attachment_version
                    AND j.applied_input_hash=attachment_normal_job_application_hash(j.id,j.applied_evaluation_id))
                OR (j.application_status_code='PENDING' AND s.current_attachment_evaluation_id IS NULL
                    AND j.applied_evaluation_id IS NULL AND j.preview_evaluation_id IS NULL AND s.attachment_row_version=j.expected_attachment_version))) AS current_valid,
        (j.previous_is_review_required IS NOT NULL AND ((j.previous_attachment_evaluation_id IS NULL AND j.previous_confirmation_id IS NULL)
            OR (j.is_reservation_previous_evaluation_current AND previous.id IS NOT NULL
                AND previous.base_evaluation_id=j.base_evaluation_id AND previous.content_version_id=j.content_version_id
                AND previous.rule_release_id=j.rule_release_id AND previous_set.set_status_code='SEALED'
                AND (NOT j.previous_is_review_required OR previous.policy_id=j.previous_policy_id)))) AS previous_valid,
        (j.is_reservation_confirmation_valid AND previous_confirmation.id IS NOT NULL AND NOT previous_confirmation.is_current
            AND previous_confirmation.evaluation_id=previous.id AND previous_confirmation.set_hash=previous_set.manifest_hash
            AND coalesce(restored.source_version,previous_confirmation.confirmed_source_version)=j.reservation_source_version
            AND coalesce(restored.attachment_version,previous_confirmation.confirmed_attachment_version)=j.reservation_attachment_version) AS confirmation_valid,
        encode(digest(jsonb_build_array(j.request_hash,j.execution_snapshot_json,j.error_code,j.operation_code,j.set_id,
            j.applied_input_hash,attachment_normal_job_application_hash(j.id,j.applied_evaluation_id),
            j.previous_policy_id,j.previous_attachment_evaluation_id,j.previous_confirmation_id,j.previous_is_review_required,
            attachment_normal_reservation_context_hash(s.id,j.base_evaluation_id))::text,'sha256'),'hex') AS input_hash
    FROM announcement_attachment_jobs j JOIN announcement_source_snapshots s ON s.id=j.source_id
    JOIN announcement_source_classification_evaluations base ON base.id=j.base_evaluation_id AND base.source_id=s.id
    LEFT JOIN announcement_source_attachment_evaluations previous ON previous.id=j.previous_attachment_evaluation_id AND previous.source_id=s.id
    LEFT JOIN announcement_source_attachment_sets previous_set ON previous_set.id=previous.set_id AND previous_set.source_id=s.id
    LEFT JOIN announcement_source_attachment_confirmations previous_confirmation ON previous_confirmation.id=j.previous_confirmation_id AND previous_confirmation.source_id=s.id
    LEFT JOIN LATERAL attachment_confirmation_restored_binding(s.id,j.previous_confirmation_id) restored ON true
    WHERE j.id=job_key AND j.batch_id IS NULL
), classified AS (
    SELECT id,source_id,row_version,job_status_code,application_status_code,rollback_status_code,mode_code,
        classification_row_version,attachment_row_version,previous_attachment_evaluation_id,previous_confirmation_id,
        coalesce(confirmation_valid,false) AS confirmation_valid,coalesce(previous_is_review_required,true) AS previous_required,input_hash,
        CASE WHEN reservation_source_version IS NULL OR reservation_context_hash IS NULL THEN 'RECOVERY_EVIDENCE_MISSING'
            WHEN rollback_status_code<>'NOT_REQUESTED' THEN 'ALREADY_RECOVERED'
            WHEN mode_code='UNAVAILABLE' THEN 'JOB_NOT_TERMINAL'
            WHEN current_valid IS DISTINCT FROM true THEN 'CURRENT_BINDING_CHANGED'
            WHEN previous_valid IS DISTINCT FROM true THEN 'PREVIOUS_BINDING_INVALID' ELSE 'READY' END AS readiness_code
    FROM facts
), payload AS (
    SELECT jsonb_build_object('jobId',id,'sourceId',source_id,'jobVersion',row_version,
        'jobStatusCode',job_status_code,'applicationStatusCode',application_status_code,'rollbackStatusCode',rollback_status_code,
        'modeCode',mode_code,'sourceVersion',classification_row_version,'attachmentVersion',attachment_row_version,
        'previousEvaluationId',previous_attachment_evaluation_id,'previousConfirmationId',previous_confirmation_id,
        'readinessCode',readiness_code,'baseReopens',readiness_code='READY' AND NOT previous_required,
        'confirmationRestores',readiness_code='READY' AND confirmation_valid,
        'staleConfirmationRemains',readiness_code='READY' AND previous_confirmation_id IS NOT NULL AND NOT confirmation_valid,
        'inputHash',input_hash) AS data FROM classified
) SELECT data||jsonb_build_object('previewHash',encode(digest(data::text,'sha256'),'hex')) FROM payload
$$;

CREATE FUNCTION protect_attachment_normal_rollback_action() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE state jsonb;
BEGIN
    IF TG_OP='DELETE' THEN
        IF EXISTS(SELECT 1 FROM announcement_source_snapshots WHERE id=OLD.source_id) THEN
            RAISE EXCEPTION 'normal rollback receipt cannot be deleted independently' USING ERRCODE='23514';
        END IF; RETURN OLD;
    END IF;
    IF TG_OP='UPDATE' THEN RAISE EXCEPTION 'normal rollback approval is immutable' USING ERRCODE='23514'; END IF;
    PERFORM 1 FROM announcement_source_snapshots WHERE id=NEW.source_id FOR UPDATE;
    PERFORM 1 FROM announcement_attachment_jobs WHERE id=NEW.job_id AND source_id=NEW.source_id AND batch_id IS NULL FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'normal rollback source job mismatch' USING ERRCODE='23514'; END IF;
    state:=attachment_normal_job_recovery_state(NEW.job_id);
    IF state IS NULL OR state->>'readinessCode'<>'READY' OR state->>'previewHash' IS DISTINCT FROM NEW.preview_hash
        OR state->>'modeCode' IS DISTINCT FROM NEW.mode_code
        OR (state->>'sourceVersion')::integer IS DISTINCT FROM NEW.expected_source_version
        OR (state->>'attachmentVersion')::integer IS DISTINCT FROM NEW.expected_attachment_version
        OR (state->>'baseReopens')::boolean IS DISTINCT FROM NEW.is_base_reopened
        OR (state->>'confirmationRestores')::boolean IS DISTINCT FROM NEW.is_confirmation_restored THEN
        RAISE EXCEPTION 'normal rollback requires unchanged preview and explicit effects' USING ERRCODE='23514';
    END IF; RETURN NEW;
END $$;
CREATE TRIGGER tr_att_normal_rollback_action BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_normal_rollback_actions
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_normal_rollback_action();

CREATE FUNCTION check_attachment_normal_rollback_complete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM announcement_source_snapshots WHERE id=NEW.source_id) THEN RETURN NULL; END IF;
    IF NOT EXISTS(SELECT 1 FROM announcement_attachment_jobs j JOIN announcement_source_snapshots s ON s.id=j.source_id
        WHERE j.id=NEW.job_id AND j.source_id=NEW.source_id AND j.normal_rollback_action_id=NEW.id AND j.rollback_status_code='ROLLED_BACK'
            AND j.restored_source_version=NEW.expected_source_version AND j.restored_attachment_version::bigint=NEW.expected_attachment_version::bigint+1
            AND s.classification_row_version=j.restored_source_version AND s.attachment_row_version=j.restored_attachment_version
            AND s.current_attachment_evaluation_id IS NOT DISTINCT FROM j.previous_attachment_evaluation_id
            AND s.attachment_policy_id IS NOT DISTINCT FROM j.previous_policy_id AND s.is_attachment_review_required=j.previous_is_review_required
            AND (j.previous_attachment_evaluation_id IS NULL OR EXISTS(SELECT 1 FROM announcement_source_attachment_evaluations e WHERE e.id=j.previous_attachment_evaluation_id AND e.source_id=s.id AND e.is_current))
            AND (SELECT c.id FROM announcement_source_attachment_confirmations c WHERE c.source_id=s.id AND c.is_current)
                IS NOT DISTINCT FROM CASE WHEN NEW.is_confirmation_restored THEN j.previous_confirmation_id ELSE NULL END
            AND j.restored_confirmation_id IS NOT DISTINCT FROM CASE WHEN NEW.is_confirmation_restored THEN j.previous_confirmation_id ELSE NULL END
            AND ((NEW.mode_code='APPLIED' AND j.application_status_code='APPLIED') OR (NEW.mode_code='FAILED_RESERVATION' AND j.application_status_code='PENDING' AND j.job_status_code IN ('FAILED','CONFLICT','CANCELLED')))
            AND NOT EXISTS(SELECT 1 FROM announcement_source_links WHERE source_id=s.id)) THEN
        RAISE EXCEPTION 'normal rollback must restore source evaluation confirmation and receipt atomically' USING ERRCODE='23514';
    END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_normal_rollback_complete AFTER INSERT ON announcement_attachment_normal_rollback_actions
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_normal_rollback_complete();

-- 완료된 일반 복구와 배치 복구를 같은 유효 버전 조회 계약으로 제공한다. 원래 검수 영수증은 불변이다.
CREATE OR REPLACE FUNCTION attachment_confirmation_restored_binding(p_source_id uuid,p_confirmation_id uuid)
RETURNS TABLE(restoration_id uuid,confirmation_id uuid,source_id uuid,source_version integer,attachment_version integer)
LANGUAGE sql STABLE AS $$
    SELECT bindings.restoration_id,bindings.confirmation_id,bindings.source_id,bindings.source_version,bindings.attachment_version FROM (
        SELECT r.id AS restoration_id,r.confirmation_id,r.source_id,r.source_version,r.attachment_version
        FROM announcement_attachment_confirmation_restorations r JOIN announcement_attachment_jobs j ON j.id=r.job_id AND j.source_id=r.source_id
            AND j.application_status_code='APPLIED' AND j.rollback_status_code='ROLLED_BACK'
            AND j.previous_confirmation_id=r.confirmation_id AND j.applied_input_hash=r.applied_input_hash
            AND j.applied_source_version=r.source_version AND j.applied_attachment_version::bigint+1=r.attachment_version
        JOIN announcement_source_attachment_confirmations c ON c.id=r.confirmation_id AND c.source_id=r.source_id
            AND c.confirmed_source_version=r.source_version AND c.confirmed_attachment_version<r.attachment_version
        WHERE r.source_id=p_source_id AND r.confirmation_id=p_confirmation_id
        UNION ALL
        SELECT a.id,j.previous_confirmation_id,j.source_id,j.restored_source_version,j.restored_attachment_version
        FROM announcement_attachment_normal_rollback_actions a JOIN announcement_attachment_jobs j ON j.id=a.job_id AND j.source_id=a.source_id
            AND j.normal_rollback_action_id=a.id AND j.rollback_status_code='ROLLED_BACK' AND a.is_confirmation_restored
            AND j.restored_confirmation_id=j.previous_confirmation_id AND j.restored_source_version=a.expected_source_version
            AND j.restored_attachment_version::bigint=a.expected_attachment_version::bigint+1
        JOIN announcement_source_attachment_confirmations c ON c.id=j.previous_confirmation_id AND c.source_id=j.source_id
            AND c.confirmed_source_version=j.restored_source_version AND c.confirmed_attachment_version<j.restored_attachment_version
        WHERE j.source_id=p_source_id AND j.previous_confirmation_id=p_confirmation_id
    ) bindings ORDER BY bindings.attachment_version DESC LIMIT 1
$$;
CREATE OR REPLACE FUNCTION protect_attachment_confirmation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (to_jsonb(NEW)-'is_current') IS DISTINCT FROM (to_jsonb(OLD)-'is_current') THEN
        RAISE EXCEPTION 'attachment confirmation is immutable' USING ERRCODE='23514';
    END IF;
    IF NOT OLD.is_current AND NEW.is_current AND NOT EXISTS(
        SELECT 1 FROM announcement_attachment_confirmation_restorations r JOIN announcement_source_snapshots s ON s.id=r.source_id
        WHERE r.confirmation_id=NEW.id AND r.source_id=NEW.source_id AND s.classification_row_version=r.source_version
            AND s.attachment_row_version=r.attachment_version AND s.current_attachment_evaluation_id=NEW.evaluation_id AND s.is_attachment_review_required)
        AND NOT EXISTS(SELECT 1 FROM announcement_attachment_normal_rollback_actions a
            JOIN announcement_attachment_jobs j ON j.id=a.job_id AND j.source_id=a.source_id
            JOIN announcement_source_snapshots s ON s.id=j.source_id
            WHERE a.is_confirmation_restored AND j.previous_confirmation_id=NEW.id AND j.source_id=NEW.source_id
                AND j.previous_attachment_evaluation_id=NEW.evaluation_id AND s.current_attachment_evaluation_id=NEW.evaluation_id
                AND s.is_attachment_review_required AND s.classification_row_version=a.expected_source_version
                AND s.attachment_row_version::bigint=a.expected_attachment_version::bigint+1
                AND j.rollback_status_code='NOT_REQUESTED' AND j.normal_rollback_action_id IS NULL) THEN
        RAISE EXCEPTION 'stale confirmation requires matching restoration evidence' USING ERRCODE='23514';
    END IF; RETURN NEW;
END $$;
