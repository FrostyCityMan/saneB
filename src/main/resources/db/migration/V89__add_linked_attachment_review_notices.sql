-- 연결 공고에는 현재 판정을 적용하지 않고 별도 재검수 근거만 보존한다.
CREATE TABLE announcement_attachment_linked_review_notices (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id uuid NOT NULL UNIQUE,
    batch_id uuid NOT NULL REFERENCES announcement_attachment_batches(id),
    source_id uuid NOT NULL,
    set_id uuid,
    evaluation_id uuid,
    reason_code varchar(40) NOT NULL CHECK (reason_code IN ('EVIDENCE_READY','EVIDENCE_PARTIAL','COLLECTION_FAILED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_notice_job FOREIGN KEY(job_id,source_id)
        REFERENCES announcement_attachment_jobs(id,source_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_notice_set FOREIGN KEY(set_id,source_id)
        REFERENCES announcement_source_attachment_sets(id,source_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT fk_att_notice_evaluation FOREIGN KEY(evaluation_id,set_id,source_id)
        REFERENCES announcement_source_attachment_evaluations(id,set_id,source_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_att_notice_evidence CHECK (evaluation_id IS NULL OR set_id IS NOT NULL)
);
CREATE INDEX ix_att_notice_source ON announcement_attachment_linked_review_notices(source_id,created_at DESC,id DESC);
CREATE INDEX ix_att_notice_batch ON announcement_attachment_linked_review_notices(batch_id,created_at DESC,id DESC);

-- 별도 원문/개인정보 metadata는 복사하지 않는다. 완료 job과 동일 근거만 원자적으로 기록한다.
CREATE FUNCTION protect_attachment_linked_review_notice() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE job_row record;
BEGIN
    IF TG_OP='UPDATE' THEN
        RAISE EXCEPTION 'linked attachment review notice is immutable' USING ERRCODE='23514';
    END IF;
    IF TG_OP='DELETE' THEN
        IF EXISTS (SELECT 1 FROM announcement_attachment_jobs WHERE id=OLD.job_id) THEN
            RAISE EXCEPTION 'linked attachment review notice cannot be deleted' USING ERRCODE='23514';
        END IF;
        RETURN OLD;
    END IF;
    PERFORM id FROM announcement_source_snapshots WHERE id=NEW.source_id FOR UPDATE;
    PERFORM id FROM announcement_source_links WHERE source_id=NEW.source_id ORDER BY id FOR SHARE;
    SELECT j.id,j.source_id,j.batch_id,j.set_id,j.preview_evaluation_id,j.job_status_code INTO job_row
        FROM announcement_attachment_jobs j WHERE j.id=NEW.job_id FOR UPDATE;
    IF job_row.id IS NULL OR job_row.source_id IS DISTINCT FROM NEW.source_id
        OR job_row.batch_id IS DISTINCT FROM NEW.batch_id
        OR job_row.set_id IS DISTINCT FROM NEW.set_id
        OR job_row.preview_evaluation_id IS DISTINCT FROM NEW.evaluation_id
        OR NOT attachment_linked_job_connections_unchanged(NEW.job_id)
        OR NEW.reason_code IS DISTINCT FROM (CASE job_row.job_status_code
            WHEN 'SUCCEEDED' THEN 'EVIDENCE_READY'
            WHEN 'PARTIAL_FAILED' THEN 'EVIDENCE_PARTIAL'
            WHEN 'FAILED' THEN 'COLLECTION_FAILED' ELSE NULL END) THEN
        RAISE EXCEPTION 'linked attachment notice requires matching terminal job and unchanged connections' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_linked_review_notice BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_linked_review_notices
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_linked_review_notice();

-- 전용 서비스가 terminal job과 경고를 같은 transaction에 저장하도록 연결하기 전에는 자동 생성하지 않는다.
-- 일반 배치, source pointer, 운영 공고, 확인 이력, 기존 외부 요청 fence는 변경하지 않는다.
