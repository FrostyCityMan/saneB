-- 전체 후보 목록 고정은 수집/적용 승인이 아니다. 기존 V1~V73과 운영 source를 변경하지 않는다.
CREATE TABLE announcement_attachment_backfill_runs (
    id uuid PRIMARY KEY,
    policy_id uuid NOT NULL REFERENCES announcement_attachment_policies(id),
    scope_json jsonb NOT NULL CHECK (jsonb_typeof(scope_json)='object' AND octet_length(scope_json::text)<=32768),
    policy_snapshot_json jsonb NOT NULL CHECK (jsonb_typeof(policy_snapshot_json)='object' AND octet_length(policy_snapshot_json::text)<=524288),
    scope_hash varchar(64) NOT NULL CHECK (scope_hash ~ '^[0-9a-f]{64}$'),
    candidate_hash varchar(64) NOT NULL CHECK (candidate_hash ~ '^[0-9a-f]{64}$'),
    candidate_count bigint NOT NULL CHECK (candidate_count>0),
    segment_size integer NOT NULL CHECK (segment_size BETWEEN 1 AND 1000),
    segment_count bigint NOT NULL CHECK (segment_count>0 AND segment_count=(candidate_count-1)/segment_size+1),
    deleted_item_count bigint NOT NULL DEFAULT 0 CHECK (deleted_item_count BETWEEN 0 AND candidate_count),
    row_version bigint NOT NULL DEFAULT 0 CHECK (row_version>=0),
    requested_by uuid NOT NULL REFERENCES users(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    reason_hash varchar(64) NOT NULL CHECK (reason_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    created_xid xid8 NOT NULL DEFAULT pg_current_xact_id()
);
CREATE TABLE announcement_attachment_backfill_segments (
    run_id uuid NOT NULL REFERENCES announcement_attachment_backfill_runs(id),
    segment_no bigint NOT NULL CHECK (segment_no>0),
    item_count integer NOT NULL CHECK (item_count BETWEEN 1 AND 1000),
    deleted_item_count integer NOT NULL DEFAULT 0 CHECK (deleted_item_count BETWEEN 0 AND item_count),
    PRIMARY KEY (run_id,segment_no)
);
CREATE TABLE announcement_attachment_backfill_items (
    run_id uuid NOT NULL,
    segment_no bigint NOT NULL,
    ordinal bigint NOT NULL CHECK (ordinal>0),
    source_id uuid NOT NULL,
    content_version_id uuid NOT NULL,
    base_evaluation_id uuid NOT NULL,
    rule_release_id uuid NOT NULL,
    provider_code varchar(30) NOT NULL CHECK (provider_code IN ('BIZINFO','GOV24','LOCAL_GOV_NOTICE')),
    input_hash varchar(64) NOT NULL CHECK (input_hash ~ '^[0-9a-f]{64}$'),
    PRIMARY KEY (run_id,source_id),
    UNIQUE (run_id,ordinal),
    FOREIGN KEY (run_id,segment_no) REFERENCES announcement_attachment_backfill_segments(run_id,segment_no),
    FOREIGN KEY (base_evaluation_id,source_id,content_version_id,rule_release_id)
        REFERENCES announcement_source_classification_evaluations(id,source_id,content_version_id,rule_release_id) ON DELETE CASCADE
);
CREATE INDEX ix_att_backfill_run_list ON announcement_attachment_backfill_runs(created_at DESC,id DESC);
CREATE INDEX ix_att_backfill_segment_items ON announcement_attachment_backfill_items(run_id,segment_no,ordinal);
CREATE INDEX ix_att_backfill_item_base ON announcement_attachment_backfill_items(base_evaluation_id,source_id,content_version_id,rule_release_id);

-- 제목/본문/URL 원문을 반환하거나 저장하지 않는다. 범위 변경·검수 변경·출처 변경을 지문으로 비교한다.
CREATE FUNCTION attachment_backfill_input_hash(source_key uuid) RETURNS text LANGUAGE sql STABLE AS $$
    SELECT encode(digest(jsonb_build_array(s.id,s.provider_code,s.data_purpose_code,
        extract(epoch FROM s.collected_at),s.application_end_date,s.semantic_status_code,
        e.id,e.content_version_id,e.rule_release_id,e.decision_status_code,e.title_stage_code,
        s.classification_row_version,s.attachment_row_version,s.current_attachment_evaluation_id,
        s.attachment_policy_id,s.is_attachment_review_required,c.id,attachment_source_locator_hash(s.id),
        attachment_normal_reservation_context_hash(s.id,e.id))::text,'sha256'),'hex')
    FROM announcement_source_snapshots s
    JOIN announcement_source_classification_evaluations e ON e.source_id=s.id AND e.is_current
    LEFT JOIN announcement_source_attachment_confirmations c ON c.source_id=s.id AND c.is_current
    WHERE s.id=source_key AND s.data_purpose_code='PRODUCTION' AND s.semantic_status_code<>'EXCLUDED'
      AND e.decision_status_code<>'EXCLUDED' AND e.title_stage_code IN ('GROUP_A_MATCHED','COMBINATION_MATCHED')
      AND NOT EXISTS (SELECT 1 FROM announcement_source_links l WHERE l.source_id=s.id)
$$;

CREATE FUNCTION protect_attachment_backfill_inventory() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE run_key uuid; run_xid xid8;
BEGIN
    IF TG_OP='DELETE' THEN
        RAISE EXCEPTION 'backfill inventory history cannot be deleted' USING ERRCODE='23514';
    END IF;
    IF TG_TABLE_NAME='announcement_attachment_backfill_runs' THEN
        IF TG_OP='INSERT' THEN
            IF NEW.created_xid<>pg_current_xact_id() OR NEW.deleted_item_count<>0 OR NEW.row_version<>0 THEN
                RAISE EXCEPTION 'backfill inventory starts without deletions' USING ERRCODE='23514';
            END IF;
        ELSIF pg_trigger_depth()<2 OR NEW.deleted_item_count<>OLD.deleted_item_count+1 OR NEW.row_version<>OLD.row_version+1
            OR (to_jsonb(NEW)-'deleted_item_count'-'row_version') IS DISTINCT FROM (to_jsonb(OLD)-'deleted_item_count'-'row_version') THEN
            RAISE EXCEPTION 'backfill frozen scope is immutable; only cascade counters may advance' USING ERRCODE='23514';
        END IF;
    ELSE
        IF TG_OP='INSERT' THEN
            SELECT created_xid INTO run_xid FROM announcement_attachment_backfill_runs WHERE id=NEW.run_id;
            IF run_xid IS DISTINCT FROM pg_current_xact_id() OR NEW.deleted_item_count<>0 THEN
                RAISE EXCEPTION 'backfill segments can only be materialized with their run' USING ERRCODE='23514';
            END IF;
        ELSIF pg_trigger_depth()<2 OR NEW.deleted_item_count<>OLD.deleted_item_count+1
            OR (to_jsonb(NEW)-'deleted_item_count') IS DISTINCT FROM (to_jsonb(OLD)-'deleted_item_count') THEN
            RAISE EXCEPTION 'backfill segment membership is immutable' USING ERRCODE='23514';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_backfill_run_immutable BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_backfill_runs
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_backfill_inventory();
CREATE TRIGGER tr_att_backfill_segment_immutable BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_backfill_segments
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_backfill_inventory();

CREATE FUNCTION protect_attachment_backfill_item() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE inventory announcement_attachment_backfill_runs;
BEGIN
    IF TG_OP='DELETE' THEN
        IF EXISTS (SELECT 1 FROM announcement_source_classification_evaluations WHERE id=OLD.base_evaluation_id) THEN
            RAISE EXCEPTION 'backfill item removal requires source or base cascade' USING ERRCODE='23514';
        END IF;
        RETURN OLD;
    END IF;
    IF TG_OP='UPDATE' THEN
        RAISE EXCEPTION 'backfill item input and segment are immutable' USING ERRCODE='23514';
    END IF;
    SELECT id,policy_id,scope_json,policy_snapshot_json,scope_hash,candidate_hash,candidate_count,segment_size,
        segment_count,deleted_item_count,row_version,requested_by,idempotency_key,request_hash,reason_hash,created_at,created_xid
        INTO inventory FROM announcement_attachment_backfill_runs WHERE id=NEW.run_id;
    IF inventory.id IS NULL OR inventory.created_xid<>pg_current_xact_id()
        OR NEW.ordinal>inventory.candidate_count OR NEW.segment_no<>(NEW.ordinal-1)/inventory.segment_size+1
        OR NEW.input_hash IS DISTINCT FROM attachment_backfill_input_hash(NEW.source_id)
        OR NOT EXISTS (SELECT 1 FROM announcement_source_classification_evaluations e
            JOIN announcement_source_snapshots s ON s.id=e.source_id
            WHERE e.id=NEW.base_evaluation_id AND e.is_current AND s.provider_code=NEW.provider_code) THEN
        RAISE EXCEPTION 'backfill item must match frozen inventory transaction and current input' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_backfill_item_immutable BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_backfill_items
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_backfill_item();

-- 삭제된 ID를 별도 보존하지 않고 최초 분모와 삭제 수만 유지한다. 실패 시 cascade 전체가 rollback된다.
CREATE FUNCTION count_deleted_attachment_backfill_item() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    UPDATE announcement_attachment_backfill_segments SET deleted_item_count=deleted_item_count+1
        WHERE run_id=OLD.run_id AND segment_no=OLD.segment_no;
    UPDATE announcement_attachment_backfill_runs SET deleted_item_count=deleted_item_count+1,row_version=row_version+1 WHERE id=OLD.run_id;
    RETURN OLD;
END $$;
CREATE TRIGGER tr_att_backfill_item_deleted AFTER DELETE ON announcement_attachment_backfill_items
    FOR EACH ROW EXECUTE FUNCTION count_deleted_attachment_backfill_item();

-- 최초 저장 transaction을 한 번 검증한다. 항목마다 전체 목록을 다시 집계하는 O(N²) trigger를 만들지 않는다.
CREATE FUNCTION check_attachment_backfill_inventory_complete() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE actual_count bigint; actual_hash text;
BEGIN
    SELECT count(1),encode(digest(coalesce(string_agg(
        encode(digest(jsonb_build_array(source_id,ordinal,input_hash,provider_code)::text,'sha256'),'hex'),'' ORDER BY ordinal),''),'sha256'),'hex')
        INTO actual_count,actual_hash FROM announcement_attachment_backfill_items WHERE run_id=NEW.id;
    IF actual_count<>NEW.candidate_count OR actual_hash<>NEW.candidate_hash
        OR (SELECT count(1) FROM announcement_attachment_backfill_segments WHERE run_id=NEW.id)<>NEW.segment_count
        OR EXISTS (SELECT 1 FROM announcement_attachment_backfill_segments s WHERE s.run_id=NEW.id AND
            (s.segment_no>NEW.segment_count OR s.deleted_item_count<>0 OR s.item_count<>
                (SELECT count(1) FROM announcement_attachment_backfill_items i WHERE i.run_id=s.run_id AND i.segment_no=s.segment_no))) THEN
        RAISE EXCEPTION 'backfill inventory must contain every frozen candidate exactly once' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_backfill_inventory_complete AFTER INSERT ON announcement_attachment_backfill_runs
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_backfill_inventory_complete();
