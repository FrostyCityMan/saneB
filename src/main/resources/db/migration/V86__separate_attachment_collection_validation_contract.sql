-- 수집 전용 검증은 전체 분류 검증과 구별한다. 기존 이력/정책/설정/worker는 변경하지 않는다.
-- 계약 코드는 기존 불변 snapshot과 hash에 결합된다. 과거 코드 없는 snapshot은 STRICT_V1이다.
CREATE FUNCTION attachment_validation_required_steps(snapshot jsonb) RETURNS text[]
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT CASE WHEN snapshot->>'validationContractCode'='COLLECTION_SAFETY_V1'
      THEN ARRAY['CLASSIFICATION_GOLDEN','COLLECTION_SAFETY','INSTALLED_RUNTIME','WORKER_DB_RECOVERY']
      ELSE ARRAY['CLASSIFICATION_GOLDEN','INSTALLED_RUNTIME','PROVIDER_PROFILES','WORKER_DB_RECOVERY'] END
$$;

-- 자동 생성된 긴 constraint 이름을 추측하지 않고 정확한 단일 컬럼 CHECK만 교체한다.
DO $$
DECLARE item record;
BEGIN
    FOR item IN
        SELECT c.conrelid::regclass AS relation_name,c.conname
        FROM pg_constraint c JOIN pg_attribute a ON a.attrelid=c.conrelid AND c.conkey=ARRAY[a.attnum]::smallint[]
        WHERE c.contype='c' AND
          ((c.conrelid='announcement_attachment_policy_validation_runs'::regclass AND a.attname='run_status_code')
           OR (c.conrelid='announcement_attachment_policy_validation_steps'::regclass AND a.attname='step_code'))
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',item.relation_name,item.conname);
    END LOOP;
END $$;
ALTER TABLE announcement_attachment_policy_validation_runs
    DROP CONSTRAINT ck_att_validation_lease,
    ADD CONSTRAINT ck_att_validation_status_v2 CHECK (run_status_code IN
      ('PENDING','RUNNING','CANCEL_REQUESTED','CANCELLED','INCOMPLETE','FAILED','CONFLICT','VERIFIED','COLLECTION_VERIFIED')),
    ADD CONSTRAINT ck_att_validation_contract_v2 CHECK (
      (coalesce(input_snapshot_json->>'validationContractCode','STRICT_V1') IN ('STRICT_V1','COLLECTION_SAFETY_V1')
       AND (NOT (input_snapshot_json ? 'validationContractCode') OR jsonb_typeof(input_snapshot_json->'validationContractCode')='string')
       AND (coalesce(input_snapshot_json->>'validationContractCode','STRICT_V1')='STRICT_V1'
            OR input_snapshot_json->>'modeCode'='COLLECT_ONLY')
       AND (run_status_code<>'VERIFIED' OR coalesce(input_snapshot_json->>'validationContractCode','STRICT_V1')='STRICT_V1')
       AND (run_status_code<>'COLLECTION_VERIFIED' OR input_snapshot_json->>'validationContractCode'='COLLECTION_SAFETY_V1')) IS TRUE),
    ADD CONSTRAINT ck_att_validation_lease CHECK (
        (run_status_code IN ('RUNNING','CANCEL_REQUESTED') AND lease_token IS NOT NULL AND lease_expires_at IS NOT NULL AND started_at IS NOT NULL AND completed_at IS NULL)
        OR (run_status_code='PENDING' AND lease_token IS NULL AND lease_expires_at IS NULL AND started_at IS NULL AND completed_at IS NULL)
        OR (run_status_code IN ('CANCELLED','INCOMPLETE','FAILED','CONFLICT','VERIFIED','COLLECTION_VERIFIED')
            AND lease_token IS NULL AND lease_expires_at IS NULL AND completed_at IS NOT NULL));
ALTER TABLE announcement_attachment_policy_validation_steps
    ADD CONSTRAINT ck_att_validation_step_v2 CHECK
      (step_code IN ('CLASSIFICATION_GOLDEN','INSTALLED_RUNTIME','PROVIDER_PROFILES','WORKER_DB_RECOVERY','COLLECTION_SAFETY'));

CREATE OR REPLACE FUNCTION protect_attachment_validation_run() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        RAISE EXCEPTION 'validation history is immutable' USING ERRCODE='23514';
    END IF;
    IF TG_OP='INSERT' THEN
        IF NEW.run_status_code<>'PENDING' OR NEW.row_version<>0 OR NOT EXISTS (
            SELECT 1 FROM announcement_attachment_policies p
            JOIN announcement_source_classification_rule_releases r ON r.id=p.rule_release_id
            WHERE p.id=NEW.policy_id AND p.policy_status_code='DRAFT' AND p.row_version=NEW.policy_row_version
              AND (coalesce(NEW.input_snapshot_json->>'validationContractCode','STRICT_V1')='STRICT_V1' OR p.mode_code='COLLECT_ONLY')
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
        OR (OLD.run_status_code='RUNNING' AND NEW.run_status_code NOT IN ('CANCEL_REQUESTED','CANCELLED','INCOMPLETE','FAILED','CONFLICT','VERIFIED','COLLECTION_VERIFIED'))
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
    IF NEW.run_status_code IN ('VERIFIED','COLLECTION_VERIFIED') AND
        (SELECT array_agg(step_code::text ORDER BY step_code) FROM announcement_attachment_policy_validation_steps
          WHERE run_id=NEW.id AND status_code='PASSED')
        IS DISTINCT FROM attachment_validation_required_steps(NEW.input_snapshot_json) THEN
        RAISE EXCEPTION 'exact contract validation scopes must pass' USING ERRCODE='23514';
    END IF;
    IF NEW.run_status_code IN ('VERIFIED','COLLECTION_VERIFIED') AND NOT EXISTS (
        SELECT 1 FROM announcement_attachment_policies p
        JOIN announcement_source_classification_rule_releases r ON r.id=p.rule_release_id
        WHERE p.id=NEW.policy_id AND p.policy_status_code='DRAFT' AND p.row_version=NEW.policy_row_version
          AND (coalesce(NEW.input_snapshot_json->>'validationContractCode','STRICT_V1')='STRICT_V1' OR p.mode_code='COLLECT_ONLY')
          AND r.id=NEW.rule_release_id AND r.row_version=NEW.rule_row_version AND r.release_status_code IN ('DRAFT','ACTIVE')) THEN
        RAISE EXCEPTION 'validation completion requires current policy and rule inputs' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION protect_attachment_validation_step() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP<>'INSERT' THEN
        RAISE EXCEPTION 'validation evidence is immutable' USING ERRCODE='23514';
    END IF;
    PERFORM 1 FROM announcement_attachment_policy_validation_runs
        WHERE id=NEW.run_id AND NEW.step_code=ANY(attachment_validation_required_steps(input_snapshot_json)) AND run_status_code='RUNNING' AND lease_expires_at>clock_timestamp() FOR UPDATE;
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

CREATE OR REPLACE FUNCTION protect_attachment_policy_publication() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE approved record;
BEGIN
    IF TG_OP<>'INSERT' THEN
        RAISE EXCEPTION 'policy publication receipt is immutable' USING ERRCODE='23514';
    END IF;
    -- 호출자가 먼저 잡은 짧은 잠금을 재확인한다. 직접 INSERT도 같은 경계를 통과해야 한다.
    PERFORM attachment_policy_publication_lock();
    SELECT s.id,s.policy_id,s.policy_row_version,s.rule_release_id,s.rule_row_version,s.mode_code,s.qa_run_id,s.qa_snapshot_hash,
        v.input_snapshot_json->'installed'->>'runtimeHash' AS runtime_hash
      INTO approved FROM announcement_attachment_policy_publication_scopes s
      JOIN announcement_attachment_policies p ON p.id=s.policy_id
      JOIN announcement_source_classification_rule_releases r ON r.id=s.rule_release_id
      JOIN announcement_attachment_policy_validation_runs v ON v.id=s.qa_run_id AND v.policy_id=s.policy_id
      WHERE s.id=NEW.scope_id AND s.policy_id=NEW.policy_id AND s.requested_by=NEW.published_by
        AND s.scope_status_code='SEALED' AND s.expires_at>clock_timestamp()
        AND p.policy_status_code='DRAFT' AND p.row_version=s.policy_row_version AND p.mode_code=s.mode_code
        AND p.rule_release_id=s.rule_release_id AND r.row_version=s.rule_row_version AND r.release_status_code='ACTIVE'
        AND ((v.run_status_code='VERIFIED' AND coalesce(v.input_snapshot_json->>'validationContractCode','STRICT_V1')='STRICT_V1')
          OR (v.run_status_code='COLLECTION_VERIFIED' AND v.input_snapshot_json->>'validationContractCode'='COLLECTION_SAFETY_V1'
              AND v.input_snapshot_json->>'modeCode'='COLLECT_ONLY' AND p.mode_code='COLLECT_ONLY')) AND v.snapshot_hash=s.qa_snapshot_hash
        AND v.policy_row_version=s.policy_row_version AND v.rule_row_version=s.rule_row_version AND v.rule_release_id=s.rule_release_id
        AND NOT EXISTS (SELECT 1 FROM announcement_attachment_policy_validation_runs newer WHERE newer.policy_id=s.policy_id
            AND (newer.created_at,newer.id)>(v.created_at,v.id))
        AND (SELECT array_agg(step_code::text ORDER BY step_code) FROM announcement_attachment_policy_validation_steps
             WHERE run_id=v.id AND status_code='PASSED')=attachment_validation_required_steps(v.input_snapshot_json)
        AND NOT EXISTS ((SELECT m.entity_type_code,m.entity_id,m.state_hash FROM attachment_policy_publication_members(s.policy_id) m)
            EXCEPT (SELECT i.entity_type_code,i.entity_id,i.state_hash FROM announcement_attachment_policy_publication_scope_items i WHERE i.scope_id=s.id))
        AND NOT EXISTS ((SELECT i.entity_type_code,i.entity_id,i.state_hash FROM announcement_attachment_policy_publication_scope_items i WHERE i.scope_id=s.id)
            EXCEPT (SELECT m.entity_type_code,m.entity_id,m.state_hash FROM attachment_policy_publication_members(s.policy_id) m));
    IF approved IS NULL OR NEW.created_xid<>pg_current_xact_id() OR NEW.policy_row_version<>approved.policy_row_version+1
        OR NEW.qa_run_id<>approved.qa_run_id OR NEW.policy_hash<>approved.qa_snapshot_hash
        OR NEW.runtime_hash IS DISTINCT FROM approved.runtime_hash
        OR NEW.previous_policy_id IS DISTINCT FROM (SELECT id FROM announcement_attachment_policies WHERE rule_release_id=approved.rule_release_id AND policy_status_code='ACTIVE')
        OR NEW.previous_policy_row_version IS DISTINCT FROM (SELECT row_version FROM announcement_attachment_policies WHERE id=NEW.previous_policy_id) THEN
        RAISE EXCEPTION 'publication requires latest verified QA and exact current approved scope' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
