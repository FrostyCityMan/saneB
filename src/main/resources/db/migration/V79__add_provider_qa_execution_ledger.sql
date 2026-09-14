-- Provider QA는 운영 원문/정책 활성화와 분리된 불변 분할 실행 원장이다.
CREATE TABLE announcement_attachment_provider_qa_runs (
    id uuid PRIMARY KEY,
    policy_id uuid NOT NULL REFERENCES announcement_attachment_policies(id),
    policy_row_version integer NOT NULL CHECK (policy_row_version>=0),
    rule_release_id uuid NOT NULL REFERENCES announcement_source_classification_rule_releases(id),
    rule_row_version integer NOT NULL CHECK (rule_row_version>=0),
    snapshot_hash varchar(64) NOT NULL CHECK (snapshot_hash ~ '^[0-9a-f]{64}$'),
    catalog_hash varchar(64) NOT NULL CHECK (catalog_hash ~ '^[0-9a-f]{64}$'),
    execution_code_hash varchar(64) NOT NULL CHECK (execution_code_hash ~ '^[0-9a-f]{64}$'),
    runtime_hash varchar(64) NOT NULL CHECK (runtime_hash ~ '^[0-9a-f]{64}$'),
    required_scope_json jsonb NOT NULL CHECK (jsonb_typeof(required_scope_json)='array' AND jsonb_array_length(required_scope_json) BETWEEN 2 AND 1002
        AND octet_length(required_scope_json::text)<=2097152),
    expected_case_count integer NOT NULL CHECK (expected_case_count BETWEEN 1 AND 10000),
    maximum_requests bigint NOT NULL CHECK (maximum_requests BETWEEN 1 AND 440000),
    maximum_bytes bigint NOT NULL CHECK (maximum_bytes BETWEEN 1 AND 838860800000),
    request_reservations bigint NOT NULL DEFAULT 0 CHECK (request_reservations BETWEEN 0 AND maximum_requests),
    reserved_bytes bigint NOT NULL DEFAULT 0 CHECK (reserved_bytes BETWEEN 0 AND maximum_bytes),
    run_status_code varchar(30) NOT NULL DEFAULT 'BUILDING'
        CHECK (run_status_code IN ('BUILDING','READY','RUNNING','CANCEL_REQUESTED','CANCELLED','COMPLETED','FAILED')),
    row_version integer NOT NULL DEFAULT 0 CHECK (row_version>=0),
    requested_by uuid NOT NULL REFERENCES users(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    created_xid xid8 NOT NULL DEFAULT pg_current_xact_id(),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    expires_at timestamptz NOT NULL DEFAULT clock_timestamp()+interval '24 hours',
    completed_at timestamptz,
    CONSTRAINT ck_att_provider_run_terminal CHECK ((run_status_code IN ('COMPLETED','FAILED','CANCELLED'))=(completed_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_att_provider_run_active ON announcement_attachment_provider_qa_runs((1))
    WHERE run_status_code IN ('BUILDING','READY','RUNNING','CANCEL_REQUESTED');
CREATE INDEX ix_att_provider_run_policy ON announcement_attachment_provider_qa_runs(policy_id,created_at DESC,id);
CREATE INDEX ix_att_provider_run_rule ON announcement_attachment_provider_qa_runs(rule_release_id);
CREATE INDEX ix_att_provider_run_actor ON announcement_attachment_provider_qa_runs(requested_by);

CREATE TABLE announcement_attachment_provider_qa_cases (
    id uuid PRIMARY KEY,
    run_id uuid NOT NULL REFERENCES announcement_attachment_provider_qa_runs(id),
    ordinal integer NOT NULL CHECK (ordinal BETWEEN 1 AND 10000),
    case_code varchar(100) NOT NULL CHECK (case_code ~ '^[A-Z0-9_-]{1,100}$'),
    input_hash varchar(64) NOT NULL CHECK (input_hash ~ '^[0-9a-f]{64}$'),
    profile_hash varchar(64) NOT NULL CHECK (profile_hash ~ '^[0-9a-f]{64}$'),
    expected_file_count integer NOT NULL CHECK (expected_file_count BETWEEN 0 AND 10),
    maximum_seconds integer NOT NULL CHECK (maximum_seconds BETWEEN 1 AND 420),
    maximum_requests integer NOT NULL CHECK (maximum_requests BETWEEN 1 AND 44),
    maximum_bytes bigint NOT NULL CHECK (maximum_bytes BETWEEN 1 AND 83886080),
    request_reservations integer NOT NULL DEFAULT 0 CHECK (request_reservations BETWEEN 0 AND maximum_requests),
    reserved_bytes bigint NOT NULL DEFAULT 0 CHECK (reserved_bytes BETWEEN 0 AND maximum_bytes),
    case_status_code varchar(20) NOT NULL DEFAULT 'PENDING' CHECK (case_status_code IN ('PENDING','RUNNING','PASSED','FAILED','CANCELLED')),
    row_version integer NOT NULL DEFAULT 0 CHECK (row_version>=0),
    lease_token uuid,
    lease_expires_at timestamptz,
    started_at timestamptz,
    completed_at timestamptz,
    error_code varchar(80) CHECK (error_code ~ '^[A-Z][A-Z0-9_]{0,79}$'),
    evidence_json jsonb CHECK (jsonb_typeof(evidence_json)='object' AND octet_length(evidence_json::text)<=32768),
    evidence_hash varchar(64) CHECK (evidence_hash ~ '^[0-9a-f]{64}$'),
    UNIQUE(run_id,ordinal), UNIQUE(run_id,case_code),
    CONSTRAINT ck_att_provider_case_lease CHECK (
        (case_status_code='PENDING' AND lease_token IS NULL AND lease_expires_at IS NULL AND started_at IS NULL AND completed_at IS NULL)
        OR (case_status_code='RUNNING' AND lease_token IS NOT NULL AND lease_expires_at IS NOT NULL AND started_at IS NOT NULL AND completed_at IS NULL)
        OR (case_status_code IN ('PASSED','FAILED','CANCELLED') AND lease_token IS NULL AND lease_expires_at IS NULL AND completed_at IS NOT NULL)),
    CONSTRAINT ck_att_provider_case_evidence CHECK ((evidence_json IS NULL)=(evidence_hash IS NULL))
);
CREATE UNIQUE INDEX uq_att_provider_case_running ON announcement_attachment_provider_qa_cases((1)) WHERE case_status_code='RUNNING';

-- 기존 다운로드2/호스트1/추출1 hard cap을 공유한다. 별도의 QA 자원 풀을 만들지 않는다.
ALTER TABLE announcement_attachment_resource_leases
    ADD COLUMN provider_qa_case_id uuid REFERENCES announcement_attachment_provider_qa_cases(id),
    ADD COLUMN provider_qa_lease_token uuid,
    DROP CONSTRAINT ck_att_resource_owner,
    ADD CONSTRAINT ck_att_resource_owner CHECK (
        (job_id IS NOT NULL AND job_lease_token IS NOT NULL AND policy_validation_id IS NULL AND policy_validation_lease_token IS NULL
            AND provider_qa_case_id IS NULL AND provider_qa_lease_token IS NULL)
        OR (job_id IS NULL AND job_lease_token IS NULL AND policy_validation_id IS NOT NULL AND policy_validation_lease_token IS NOT NULL
            AND provider_qa_case_id IS NULL AND provider_qa_lease_token IS NULL AND resource_code='EXTRACTION' AND resource_key='GLOBAL' AND slot_no=1)
        OR (job_id IS NULL AND job_lease_token IS NULL AND policy_validation_id IS NULL AND policy_validation_lease_token IS NULL
            AND provider_qa_case_id IS NOT NULL AND provider_qa_lease_token IS NOT NULL));
CREATE INDEX ix_att_resource_provider_case ON announcement_attachment_resource_leases(provider_qa_case_id,provider_qa_lease_token);

CREATE FUNCTION protect_attachment_provider_run() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN RAISE EXCEPTION 'provider QA history is immutable' USING ERRCODE='23514'; END IF;
    IF TG_OP='INSERT' THEN
        IF NEW.run_status_code<>'BUILDING' OR NEW.row_version<>0 OR NEW.request_reservations<>0 OR NEW.reserved_bytes<>0
            OR NEW.created_xid<>pg_current_xact_id() OR NOT EXISTS (
                SELECT 1 FROM announcement_attachment_policies p JOIN announcement_source_classification_rule_releases r ON r.id=p.rule_release_id
                WHERE p.id=NEW.policy_id AND p.row_version=NEW.policy_row_version AND p.policy_status_code='DRAFT'
                  AND r.id=NEW.rule_release_id AND r.row_version=NEW.rule_row_version AND r.release_status_code IN ('DRAFT','ACTIVE')) THEN
            RAISE EXCEPTION 'provider QA requires current draft and zero initial usage' USING ERRCODE='23514';
        END IF;
        NEW.created_at:=clock_timestamp(); NEW.expires_at:=NEW.created_at+interval '24 hours'; RETURN NEW;
    END IF;
    IF OLD.run_status_code IN ('COMPLETED','FAILED','CANCELLED') OR NEW.row_version<>OLD.row_version+1
        OR (to_jsonb(NEW)-ARRAY['row_version','run_status_code','request_reservations','reserved_bytes','completed_at'])
            IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['row_version','run_status_code','request_reservations','reserved_bytes','completed_at'])
        OR NEW.request_reservations<OLD.request_reservations OR NEW.reserved_bytes<OLD.reserved_bytes
        OR (OLD.run_status_code='BUILDING' AND (NEW.run_status_code<>'READY' OR OLD.created_xid<>pg_current_xact_id()))
        OR (OLD.run_status_code='READY' AND NEW.run_status_code NOT IN ('RUNNING','CANCEL_REQUESTED','FAILED'))
        OR (OLD.run_status_code='RUNNING' AND NEW.run_status_code NOT IN ('RUNNING','CANCEL_REQUESTED','COMPLETED','FAILED'))
        OR (OLD.run_status_code='CANCEL_REQUESTED' AND NEW.run_status_code<>'CANCELLED') THEN
        RAISE EXCEPTION 'provider QA scope usage and transitions are fenced' USING ERRCODE='23514';
    END IF;
    IF NEW.run_status_code='READY' AND NOT EXISTS (
        SELECT 1 FROM announcement_attachment_provider_qa_cases c WHERE c.run_id=NEW.id
        HAVING count(1)=NEW.expected_case_count AND min(ordinal)=1 AND max(ordinal)=NEW.expected_case_count
            AND sum(maximum_requests)=NEW.maximum_requests AND sum(maximum_bytes)=NEW.maximum_bytes) THEN
        RAISE EXCEPTION 'provider QA needs all frozen cases and exact budgets' USING ERRCODE='23514';
    END IF;
    IF NEW.run_status_code IN ('COMPLETED','FAILED','CANCELLED') AND EXISTS (
        SELECT 1 FROM announcement_attachment_provider_qa_cases c WHERE c.run_id=NEW.id AND c.case_status_code IN ('PENDING','RUNNING')) THEN
        RAISE EXCEPTION 'provider QA cannot hide unfinished cases' USING ERRCODE='23514';
    END IF;
    IF NEW.run_status_code='COMPLETED' AND EXISTS (
        SELECT 1 FROM announcement_attachment_provider_qa_cases c WHERE c.run_id=NEW.id AND c.case_status_code<>'PASSED') THEN
        RAISE EXCEPTION 'provider QA completed means every frozen case matched' USING ERRCODE='23514';
    END IF;
    IF NEW.run_status_code IN ('COMPLETED','FAILED','CANCELLED') AND NOT EXISTS (
        SELECT 1 FROM announcement_attachment_provider_qa_cases c WHERE c.run_id=NEW.id
        HAVING sum(request_reservations)=NEW.request_reservations AND sum(reserved_bytes)=NEW.reserved_bytes) THEN
        RAISE EXCEPTION 'provider QA final usage must equal all case usage' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_provider_run BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_provider_qa_runs
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_provider_run();

CREATE FUNCTION require_attachment_provider_ready() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM announcement_attachment_provider_qa_runs WHERE id=NEW.id AND run_status_code='BUILDING') THEN
        RAISE EXCEPTION 'provider QA preparation must seal in the same transaction' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER tr_att_provider_ready AFTER INSERT ON announcement_attachment_provider_qa_runs
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION require_attachment_provider_ready();

CREATE FUNCTION protect_attachment_provider_case() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent announcement_attachment_provider_qa_runs%ROWTYPE;
BEGIN
    IF TG_OP='DELETE' THEN RAISE EXCEPTION 'provider QA cases are immutable' USING ERRCODE='23514'; END IF;
    SELECT id,policy_id,policy_row_version,rule_release_id,rule_row_version,snapshot_hash,catalog_hash,execution_code_hash,runtime_hash,
        required_scope_json,expected_case_count,maximum_requests,maximum_bytes,request_reservations,reserved_bytes,run_status_code,row_version,
        requested_by,idempotency_key,request_hash,created_xid,created_at,expires_at,completed_at
        INTO parent FROM announcement_attachment_provider_qa_runs WHERE id=NEW.run_id FOR UPDATE;
    IF TG_OP='INSERT' THEN
        IF parent.run_status_code<>'BUILDING' OR parent.created_xid<>pg_current_xact_id() OR NEW.case_status_code<>'PENDING'
            OR NEW.row_version<>0 OR NEW.request_reservations<>0 OR NEW.reserved_bytes<>0 OR NEW.evidence_json IS NOT NULL OR NEW.error_code IS NOT NULL THEN
            RAISE EXCEPTION 'provider QA cases require atomic frozen preparation' USING ERRCODE='23514';
        END IF;
        RETURN NEW;
    END IF;
    IF NEW.row_version<>OLD.row_version+1 OR OLD.case_status_code IN ('PASSED','FAILED','CANCELLED')
        OR (to_jsonb(NEW)-ARRAY['row_version','case_status_code','request_reservations','reserved_bytes','lease_token','lease_expires_at','started_at','completed_at','error_code','evidence_json','evidence_hash'])
           IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['row_version','case_status_code','request_reservations','reserved_bytes','lease_token','lease_expires_at','started_at','completed_at','error_code','evidence_json','evidence_hash'])
        OR NEW.request_reservations<OLD.request_reservations OR NEW.reserved_bytes<OLD.reserved_bytes THEN
        RAISE EXCEPTION 'provider QA case input and terminal results are immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.case_status_code='PENDING' THEN
        IF NEW.case_status_code NOT IN ('RUNNING','CANCELLED','FAILED') OR NEW.request_reservations<>0 OR NEW.reserved_bytes<>0
            OR NEW.evidence_json IS NOT NULL
            OR (NEW.case_status_code='RUNNING' AND (parent.run_status_code<>'RUNNING' OR NEW.lease_expires_at<=clock_timestamp()
                OR NEW.lease_expires_at>clock_timestamp()+interval '8 minutes' OR NEW.lease_expires_at>parent.expires_at)) THEN
            RAISE EXCEPTION 'provider QA claim is bounded and starts without evidence' USING ERRCODE='23514';
        END IF;
    ELSE
        IF NEW.started_at IS DISTINCT FROM OLD.started_at OR NEW.case_status_code NOT IN ('RUNNING','PASSED','FAILED','CANCELLED')
            OR (NEW.case_status_code='RUNNING' AND (parent.run_status_code<>'RUNNING' OR OLD.lease_expires_at<=clock_timestamp()
                OR (NEW.lease_token,NEW.lease_expires_at,NEW.evidence_json,NEW.error_code) IS DISTINCT FROM (OLD.lease_token,OLD.lease_expires_at,OLD.evidence_json,OLD.error_code)))
            OR (OLD.lease_expires_at<=clock_timestamp() AND NOT (NEW.case_status_code='FAILED' AND NEW.error_code IS NOT DISTINCT FROM 'LEASE_EXPIRED')) THEN
            RAISE EXCEPTION 'provider QA lease cannot extend or accept a late response' USING ERRCODE='23514';
        END IF;
    END IF;
    IF NEW.case_status_code='PASSED' AND (parent.run_status_code<>'RUNNING' OR parent.expires_at<=clock_timestamp()
        OR NEW.error_code IS NOT NULL OR NEW.evidence_json IS NULL OR NOT COALESCE(
            NEW.evidence_json->>'scope'='SINGLE_FIXED_NOTICE_PROVIDER_QA' AND NEW.evidence_json->>'status'='PASSED'
            AND NEW.evidence_json->>'caseId'=NEW.case_code AND NEW.evidence_json->>'inputHash'=NEW.input_hash
            AND NEW.evidence_json->>'profileHash'=NEW.profile_hash AND NEW.evidence_json->>'runtimeHash'=parent.runtime_hash
            AND NEW.evidence_json->'originalFilesRemoved'='true'::jsonb AND NEW.evidence_json->'isPolicyQaPassed'='false'::jsonb
            AND NEW.evidence_json->'expectedFileCount'=to_jsonb(NEW.expected_file_count)
            AND jsonb_array_length(NEW.evidence_json->'files')=NEW.expected_file_count
            AND NEW.evidence_json->'requestReservations'=to_jsonb(NEW.request_reservations)
            AND NEW.evidence_json->'reservedBytes'=to_jsonb(NEW.reserved_bytes),false)
        OR NOT EXISTS (SELECT 1 FROM announcement_attachment_policies p JOIN announcement_source_classification_rule_releases r ON r.id=p.rule_release_id
            WHERE p.id=parent.policy_id AND p.policy_status_code='DRAFT' AND p.row_version=parent.policy_row_version
              AND r.id=parent.rule_release_id AND r.row_version=parent.rule_row_version AND r.release_status_code IN ('DRAFT','ACTIVE'))) THEN
        RAISE EXCEPTION 'provider QA success requires matching cleaned evidence and current inputs' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_provider_case BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_provider_qa_cases
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_provider_case();
