-- ATT-051의 별도 근거 수집 목적을 준비한다. 기존 배치는 STANDARD이며 실행 fence는 아직 완화하지 않는다.
ALTER TABLE announcement_attachment_batches
    ADD COLUMN purpose_code varchar(30) NOT NULL DEFAULT 'STANDARD',
    ADD CONSTRAINT ck_att_batch_purpose CHECK (purpose_code IN ('STANDARD','LINKED_EVIDENCE_ONLY')),
    ADD CONSTRAINT ck_att_linked_batch_scope CHECK (purpose_code='STANDARD' OR
        (scope_item_count IS NOT NULL AND scope_json->>'purposeCode'='LINKED_EVIDENCE_ONLY') IS TRUE),
    ADD CONSTRAINT ck_att_linked_batch_collection_only CHECK (purpose_code='STANDARD' OR
        batch_status_code IN ('SCOPE_READY','COLLECTION_PENDING','COLLECTING','COLLECTED',
            'COLLECTION_PARTIAL_FAILED','COLLECTION_PAUSED','CANCELLED'));

-- 일반 배치로 목적을 바꿔 적용·원복 제한을 우회하지 못한다.
CREATE FUNCTION protect_attachment_batch_purpose() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.purpose_code IS DISTINCT FROM OLD.purpose_code THEN
        RAISE EXCEPTION 'attachment batch purpose is immutable' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_batch_purpose BEFORE UPDATE ON announcement_attachment_batches
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_batch_purpose();

-- 연결 공고의 근거 전용 작업은 현재 판정 적용·원복·선택 적용 이력을 만들 수 없다.
-- 기존 STANDARD/일반 단건 job의 동작은 변경하지 않는다. batch_id 고정은 기존 trigger가 보장한다.
CREATE FUNCTION protect_attachment_linked_job_effect() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM announcement_attachment_batches b
        WHERE b.id=NEW.batch_id AND b.purpose_code='LINKED_EVIDENCE_ONLY') AND (
        NEW.operation_code IS DISTINCT FROM 'COLLECT'
        OR NEW.application_status_code IS DISTINCT FROM 'NOT_REQUESTED'
        OR NEW.rollback_status_code IS DISTINCT FROM 'NOT_REQUESTED'
        OR NEW.is_selected_for_application IS DISTINCT FROM false
        OR NEW.applied_evaluation_id IS NOT NULL
        OR NEW.applied_attachment_version IS NOT NULL
        OR NEW.applied_source_version IS NOT NULL
        OR NEW.applied_input_hash IS NOT NULL) THEN
        RAISE EXCEPTION 'linked attachment evidence cannot apply or rollback source bindings' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_linked_job_effect BEFORE INSERT OR UPDATE ON announcement_attachment_jobs
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_linked_job_effect();

-- 전체 연결 snapshot·경고 원장·전용 API와 입력 fence는 후속 구현이다.
-- 이 migration만으로 연결 원문을 수집할 수 없으며 기존 보호 조건/정책/worker 설정은 유지된다.
