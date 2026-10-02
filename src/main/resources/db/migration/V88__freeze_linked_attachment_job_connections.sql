-- 연결 전체를 ID 쌍의 불변 snapshot으로 저장한다. 운영 공고의 본문/조건/개인정보는 복사하지 않는다.
CREATE FUNCTION attachment_source_link_snapshot(source_key uuid) RETURNS jsonb
LANGUAGE sql STABLE AS $$
    SELECT coalesce(jsonb_agg(jsonb_build_object('linkId',l.id,'announcementId',l.announcement_id) ORDER BY l.id),'[]'::jsonb)
    FROM announcement_source_links l WHERE l.source_id=source_key
$$;

CREATE TABLE announcement_attachment_linked_job_scopes (
    job_id uuid PRIMARY KEY,
    source_id uuid NOT NULL,
    links_json jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_linked_scope_job FOREIGN KEY (job_id,source_id)
        REFERENCES announcement_attachment_jobs(id,source_id) ON DELETE CASCADE,
    CONSTRAINT ck_att_linked_scope_links CHECK
        (CASE WHEN jsonb_typeof(links_json)='array' THEN jsonb_array_length(links_json)>0 ELSE false END)
);
CREATE INDEX ix_att_linked_scope_source ON announcement_attachment_linked_job_scopes(source_id,job_id);

-- live link의 FK를 두지 않는다. 연결 삭제·교체가 승인 당시 snapshot을 자동 삭제해서는 안 된다.
-- source 삭제는 기존 job cascade를 따라 식별자 snapshot까지 정리한다.
CREATE FUNCTION protect_attachment_linked_scope() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='UPDATE' THEN
        RAISE EXCEPTION 'linked attachment connection snapshot is immutable' USING ERRCODE='23514';
    END IF;
    IF TG_OP='DELETE' THEN
        IF EXISTS (SELECT 1 FROM announcement_attachment_jobs j WHERE j.id=OLD.job_id) THEN
            RAISE EXCEPTION 'linked attachment connection snapshot cannot be deleted' USING ERRCODE='23514';
        END IF;
        RETURN OLD;
    END IF;
    -- source FK를 사용하는 새 연결과 기존 연결 행의 변경을 예약 snapshot 저장 동안 직렬화한다.
    PERFORM s.id FROM announcement_source_snapshots s WHERE s.id=NEW.source_id FOR UPDATE;
    PERFORM l.id FROM announcement_source_links l WHERE l.source_id=NEW.source_id ORDER BY l.id FOR SHARE;
    IF NOT EXISTS (SELECT 1 FROM announcement_attachment_jobs j JOIN announcement_attachment_batches b ON b.id=j.batch_id
        WHERE j.id=NEW.job_id AND j.source_id=NEW.source_id AND j.job_status_code='SCOPE_READY'
            AND b.purpose_code='LINKED_EVIDENCE_ONLY' AND b.batch_status_code='SCOPE_READY') THEN
        RAISE EXCEPTION 'linked attachment snapshot requires its reserved evidence-only job' USING ERRCODE='23514';
    END IF;
    IF NEW.links_json IS DISTINCT FROM attachment_source_link_snapshot(NEW.source_id) THEN
        RAISE EXCEPTION 'linked attachment snapshot must contain all current connections' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_linked_scope BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_linked_job_scopes
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_linked_scope();

-- 정상 예약은 job과 전체 연결 snapshot을 한 transaction으로 기록해야 한다.
CREATE FUNCTION check_attachment_linked_scope_present() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM announcement_attachment_jobs j JOIN announcement_attachment_batches b ON b.id=j.batch_id
        WHERE j.id=NEW.id AND b.purpose_code='LINKED_EVIDENCE_ONLY'
            AND NOT EXISTS (SELECT 1 FROM announcement_attachment_linked_job_scopes x WHERE x.job_id=j.id AND x.source_id=j.source_id)) THEN
        RAISE EXCEPTION 'linked attachment job requires a frozen connection snapshot' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_linked_scope_present AFTER INSERT ON announcement_attachment_jobs
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_linked_scope_present();

-- 같은 개수의 연결 교체도 감지한다. 미연결·다른 목적·snapshot 누락은 false다.
CREATE FUNCTION attachment_linked_job_connections_unchanged(job_key uuid) RETURNS boolean
LANGUAGE sql STABLE AS $$
    SELECT coalesce((SELECT x.links_json=attachment_source_link_snapshot(j.source_id)
        FROM announcement_attachment_jobs j JOIN announcement_attachment_batches b ON b.id=j.batch_id
        JOIN announcement_attachment_linked_job_scopes x ON x.job_id=j.id AND x.source_id=j.source_id
        WHERE j.id=job_key AND b.purpose_code='LINKED_EVIDENCE_ONLY'),false)
$$;

-- 기존 외부 요청 fence는 교체하지 않는다. 전용 API/worker와 경고 원장 연결 전에는 실행 불가다.
