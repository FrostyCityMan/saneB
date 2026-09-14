-- 전체 목록 분할과 기존 관리 배치를 일대일 연결한다. 연결은 수집/적용 승인이 아니다.
CREATE TABLE announcement_attachment_backfill_segment_batches (
    run_id uuid NOT NULL,
    segment_no bigint NOT NULL,
    batch_id uuid NOT NULL UNIQUE REFERENCES announcement_attachment_batches(id),
    deleted_before_reservation integer NOT NULL CHECK (deleted_before_reservation BETWEEN 0 AND 999),
    expected_run_version bigint NOT NULL CHECK (expected_run_version>=0),
    segment_hash varchar(64) NOT NULL CHECK (segment_hash ~ '^[0-9a-f]{64}$'),
    requested_by uuid NOT NULL REFERENCES users(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    reason_hash varchar(64) NOT NULL CHECK (reason_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY (run_id,segment_no),
    FOREIGN KEY (run_id,segment_no) REFERENCES announcement_attachment_backfill_segments(run_id,segment_no)
);

CREATE FUNCTION protect_attachment_backfill_segment_batch() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE target record;
BEGIN
    IF TG_OP<>'INSERT' THEN
        RAISE EXCEPTION 'backfill segment reservation receipt is immutable' USING ERRCODE='23514';
    END IF;
    SELECT s.item_count,s.deleted_item_count,r.row_version,r.policy_id,r.policy_snapshot_json
        INTO target FROM announcement_attachment_backfill_segments s JOIN announcement_attachment_backfill_runs r ON r.id=s.run_id
        WHERE s.run_id=NEW.run_id AND s.segment_no=NEW.segment_no;
    IF target IS NULL OR NEW.deleted_before_reservation<>target.deleted_item_count OR NEW.expected_run_version<>target.row_version
        OR NOT EXISTS (SELECT 1 FROM announcement_attachment_batches b WHERE b.id=NEW.batch_id
            AND b.batch_status_code='SCOPE_READY' AND b.scope_item_count=target.item_count-target.deleted_item_count
            AND b.deleted_item_count=0 AND b.policy_id=target.policy_id AND b.policy_snapshot_json=target.policy_snapshot_json
            AND b.requested_by=NEW.requested_by AND b.idempotency_key=NEW.idempotency_key
            AND b.scope_json->>'backfillRunId'=NEW.run_id::text AND b.scope_json->>'backfillSegmentNo'=NEW.segment_no::text)
        OR EXISTS (SELECT 1 FROM announcement_attachment_backfill_items i WHERE i.run_id=NEW.run_id AND i.segment_no=NEW.segment_no
            AND i.input_hash IS DISTINCT FROM attachment_backfill_input_hash(i.source_id)) THEN
        RAISE EXCEPTION 'backfill reservation requires exact unchanged segment and new matching batch' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_backfill_segment_batch_immutable BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_backfill_segment_batches
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_backfill_segment_batch();

-- 모든 예약/삭제 경로에서 source -> run -> segment 순서를 유지한다. V74의 불변/증가 제약은 유지한다.
CREATE OR REPLACE FUNCTION count_deleted_attachment_backfill_item() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    UPDATE announcement_attachment_backfill_runs SET deleted_item_count=deleted_item_count+1,row_version=row_version+1 WHERE id=OLD.run_id;
    UPDATE announcement_attachment_backfill_segments SET deleted_item_count=deleted_item_count+1 WHERE run_id=OLD.run_id AND segment_no=OLD.segment_no;
    RETURN OLD;
END $$;

-- cascade 순서 중간을 판정하지 않는다. 최종 transaction의 잔여 item/job·삭제 전후 분모가 일치해야 한다.
CREATE FUNCTION check_attachment_backfill_segment_batch() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE batch_key uuid; binding record; batch_scope jsonb;
BEGIN
    IF TG_TABLE_NAME='announcement_attachment_batches' THEN batch_key:=NEW.id;
    ELSIF TG_OP='DELETE' THEN batch_key:=OLD.batch_id;
    ELSE batch_key:=NEW.batch_id; END IF;
    IF batch_key IS NULL THEN RETURN NULL; END IF;
    SELECT scope_json INTO batch_scope FROM announcement_attachment_batches WHERE id=batch_key;
    SELECT l.run_id,l.segment_no,l.deleted_before_reservation,s.item_count,s.deleted_item_count,
        b.scope_item_count,b.deleted_item_count AS batch_deleted_count,r.policy_id,r.policy_snapshot_json
        INTO binding FROM announcement_attachment_backfill_segment_batches l
        JOIN announcement_attachment_backfill_segments s ON s.run_id=l.run_id AND s.segment_no=l.segment_no
        JOIN announcement_attachment_backfill_runs r ON r.id=l.run_id
        JOIN announcement_attachment_batches b ON b.id=l.batch_id WHERE l.batch_id=batch_key;
    IF binding IS NULL THEN
        IF batch_scope ? 'backfillRunId' OR batch_scope ? 'backfillSegmentNo' THEN
            RAISE EXCEPTION 'backfill marked batch requires its atomic segment receipt' USING ERRCODE='23514';
        END IF;
        RETURN NULL;
    END IF;
    IF binding.scope_item_count+binding.deleted_before_reservation<>binding.item_count
        OR binding.batch_deleted_count+binding.deleted_before_reservation<>binding.deleted_item_count
        OR (SELECT count(1) FROM announcement_attachment_jobs WHERE batch_id=batch_key)<>binding.item_count-binding.deleted_item_count
        OR EXISTS (SELECT 1 FROM announcement_attachment_jobs j WHERE j.batch_id=batch_key AND NOT EXISTS (
            SELECT 1 FROM announcement_attachment_backfill_items i WHERE i.run_id=binding.run_id AND i.segment_no=binding.segment_no
                AND i.source_id=j.source_id AND i.content_version_id=j.content_version_id AND i.base_evaluation_id=j.base_evaluation_id
                AND i.rule_release_id=j.rule_release_id AND i.provider_code=j.frozen_provider_code))
        OR EXISTS (SELECT 1 FROM announcement_attachment_backfill_items i WHERE i.run_id=binding.run_id AND i.segment_no=binding.segment_no
            AND NOT EXISTS (SELECT 1 FROM announcement_attachment_jobs j WHERE j.batch_id=batch_key AND j.source_id=i.source_id
                AND j.content_version_id=i.content_version_id AND j.base_evaluation_id=i.base_evaluation_id AND j.rule_release_id=i.rule_release_id)) THEN
        RAISE EXCEPTION 'backfill segment jobs and original deletion denominators must match exactly' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_backfill_segment_batch AFTER INSERT ON announcement_attachment_backfill_segment_batches
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_backfill_segment_batch();
CREATE CONSTRAINT TRIGGER ct_att_backfill_marked_batch AFTER INSERT OR UPDATE ON announcement_attachment_batches
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_backfill_segment_batch();
CREATE CONSTRAINT TRIGGER ct_att_backfill_fixed_job AFTER INSERT OR UPDATE OR DELETE ON announcement_attachment_jobs
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_backfill_segment_batch();

-- 연결된 전체 목록의 입력 지문은 예약/claim/매 HTTP 전에 확인한다. 일반 배치와 이전 계약은 그대로다.
CREATE FUNCTION attachment_backfill_batch_input_unchanged(job_key uuid) RETURNS boolean LANGUAGE sql STABLE AS $$
    SELECT coalesce((SELECT l.batch_id IS NULL OR EXISTS (
        SELECT 1 FROM announcement_attachment_backfill_items i WHERE i.run_id=l.run_id AND i.segment_no=l.segment_no
            AND i.source_id=j.source_id AND i.base_evaluation_id=j.base_evaluation_id AND i.content_version_id=j.content_version_id
            AND i.rule_release_id=j.rule_release_id AND i.input_hash=attachment_backfill_input_hash(i.source_id))
        FROM announcement_attachment_jobs j LEFT JOIN announcement_attachment_backfill_segment_batches l ON l.batch_id=j.batch_id
        WHERE j.id=job_key),false)
$$;
CREATE OR REPLACE FUNCTION attachment_batch_job_input_unchanged(job_key uuid) RETURNS boolean LANGUAGE sql STABLE AS $$
    SELECT coalesce((SELECT b.scope_item_count IS NULL OR (
        j.frozen_locator_hash=attachment_source_locator_hash(j.source_id)
        AND s.current_attachment_evaluation_id IS NOT DISTINCT FROM j.previous_attachment_evaluation_id
        AND s.attachment_policy_id IS NOT DISTINCT FROM j.previous_policy_id
        AND s.is_attachment_review_required IS NOT DISTINCT FROM j.previous_is_review_required
        AND (SELECT c.id FROM announcement_source_attachment_confirmations c WHERE c.source_id=j.source_id AND c.is_current)
            IS NOT DISTINCT FROM j.previous_confirmation_id
        AND NOT EXISTS (SELECT 1 FROM announcement_source_links l WHERE l.source_id=j.source_id)
        AND attachment_backfill_batch_input_unchanged(j.id))
        FROM announcement_attachment_jobs j JOIN announcement_source_snapshots s ON s.id=j.source_id
        LEFT JOIN announcement_attachment_batches b ON b.id=j.batch_id WHERE j.id=job_key),false)
$$;
