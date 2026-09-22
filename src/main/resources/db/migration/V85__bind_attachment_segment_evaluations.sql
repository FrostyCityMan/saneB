-- 기존 파일 단위 평가 입력/일치 이력은 NULL 결합 그대로 보존한다. 새 엔진만 구간 분석을 참조한다.
ALTER TABLE announcement_attachment_segment_analyses
    ADD CONSTRAINT uq_att_segment_extraction_binding UNIQUE (id,extraction_id,file_id,set_id,source_id);
ALTER TABLE announcement_source_attachment_evaluation_inputs
    ADD COLUMN segment_analysis_id uuid,
    ADD CONSTRAINT fk_att_input_segment FOREIGN KEY (segment_analysis_id,extraction_id,file_id,set_id,source_id)
        REFERENCES announcement_attachment_segment_analyses(id,extraction_id,file_id,set_id,source_id) ON DELETE CASCADE,
    ADD CONSTRAINT uq_att_input_segment_binding UNIQUE (evaluation_id,file_id,extraction_id,set_id,source_id,segment_analysis_id);
ALTER TABLE announcement_source_attachment_matches
    ADD COLUMN segment_analysis_id uuid,
    ADD COLUMN segment_index integer,
    ADD CONSTRAINT ck_att_match_segment CHECK ((segment_analysis_id IS NULL AND segment_index IS NULL)
        OR (segment_analysis_id IS NOT NULL AND segment_index BETWEEN 0 AND 199 AND segment_index IS NOT NULL)),
    ADD CONSTRAINT fk_att_match_segment_input FOREIGN KEY (evaluation_id,file_id,extraction_id,set_id,source_id,segment_analysis_id)
        REFERENCES announcement_source_attachment_evaluation_inputs(evaluation_id,file_id,extraction_id,set_id,source_id,segment_analysis_id) ON DELETE CASCADE;
CREATE INDEX ix_att_input_segment ON announcement_source_attachment_evaluation_inputs(segment_analysis_id) WHERE segment_analysis_id IS NOT NULL;

-- 새 평가의 근거는 생성 transaction에서만 추가한다. 기존 평가에는 이 결합을 소급하지 않는다.
ALTER TABLE announcement_source_attachment_evaluations ADD COLUMN segment_binding_xid xid8;
CREATE FUNCTION bind_attachment_segment_transaction() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.engine_version='attachment-segment-1.0.0' THEN
        NEW.segment_binding_xid:=pg_current_xact_id();
    ELSIF NEW.segment_binding_xid IS NOT NULL THEN
        RAISE EXCEPTION 'legacy attachment evaluation cannot bind segment transaction' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_segment_transaction BEFORE INSERT ON announcement_source_attachment_evaluations
    FOR EACH ROW EXECUTE FUNCTION bind_attachment_segment_transaction();

-- 게시 정책의 고정 구간 규칙과 실제 분석이 동일해야 한다. 정상 추출을 구간 분석 없이 새 엔진에 넣지 않는다.
CREATE FUNCTION check_attachment_segment_input() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE engine text; settings jsonb; analysis_version text; rules_hash text; binding_xid xid8;
BEGIN
    SELECT e.engine_version,p.settings_json,e.segment_binding_xid INTO engine,settings,binding_xid
    FROM announcement_source_attachment_evaluations e JOIN announcement_attachment_policies p ON p.id=e.policy_id
    WHERE e.id=NEW.evaluation_id AND e.set_id=NEW.set_id AND e.source_id=NEW.source_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'attachment segment evaluation binding missing' USING ERRCODE='23514'; END IF;
    IF engine<>'attachment-segment-1.0.0' THEN
        IF NEW.segment_analysis_id IS NOT NULL THEN RAISE EXCEPTION 'legacy attachment evaluation cannot bind segments' USING ERRCODE='23514'; END IF;
        RETURN NEW;
    END IF;
    IF settings->>'engineVersion' IS DISTINCT FROM engine THEN
        RAISE EXCEPTION 'attachment segment engine policy mismatch' USING ERRCODE='23514';
    END IF;
    IF NEW.segment_analysis_id IS NULL THEN
        IF NEW.input_status_code='COMPLETE_TEXT' THEN RAISE EXCEPTION 'complete attachment requires segment analysis' USING ERRCODE='23514'; END IF;
        IF binding_xid IS DISTINCT FROM pg_current_xact_id() THEN RAISE EXCEPTION 'attachment segment inputs require creation transaction' USING ERRCODE='23514'; END IF;
        RETURN NEW;
    END IF;
    SELECT a.analysis_version,a.rules_hash INTO analysis_version,rules_hash FROM announcement_attachment_segment_analyses a
    WHERE a.id=NEW.segment_analysis_id AND a.extraction_id=NEW.extraction_id AND a.file_id=NEW.file_id AND a.set_id=NEW.set_id AND a.source_id=NEW.source_id;
    IF NOT FOUND OR analysis_version IS DISTINCT FROM (settings->>'segmentRuleVersion') OR rules_hash IS DISTINCT FROM (settings->>'segmentRulesHash') THEN
        RAISE EXCEPTION 'attachment segment rules policy mismatch' USING ERRCODE='23514';
    END IF;
    IF binding_xid IS DISTINCT FROM pg_current_xact_id() THEN RAISE EXCEPTION 'attachment segment inputs require creation transaction' USING ERRCODE='23514'; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_input_segment BEFORE INSERT ON announcement_source_attachment_evaluation_inputs
    FOR EACH ROW EXECUTE FUNCTION check_attachment_segment_input();

-- 키워드 위치는 원본 block과 해당 구간의 교집합 안에 있어야 한다. 서식/미확인/역할 충돌은 참고 근거뿐이다.
CREATE FUNCTION check_attachment_segment_match() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE expected_analysis uuid; segment jsonb; block jsonb; role_code text; file_role text; file_origin text;
BEGIN
    SELECT i.segment_analysis_id INTO expected_analysis FROM announcement_source_attachment_evaluation_inputs i
    WHERE i.evaluation_id=NEW.evaluation_id AND i.file_id=NEW.file_id AND i.extraction_id=NEW.extraction_id AND i.set_id=NEW.set_id AND i.source_id=NEW.source_id;
    IF NEW.segment_analysis_id IS DISTINCT FROM expected_analysis THEN
        RAISE EXCEPTION 'attachment segment match input mismatch' USING ERRCODE='23514';
    END IF;
    IF NEW.segment_analysis_id IS NULL THEN
        IF EXISTS (SELECT 1 FROM announcement_source_attachment_evaluations e WHERE e.id=NEW.evaluation_id
            AND e.engine_version='attachment-segment-1.0.0' AND e.segment_binding_xid IS DISTINCT FROM pg_current_xact_id()) THEN
            RAISE EXCEPTION 'attachment segment matches require creation transaction' USING ERRCODE='23514';
        END IF;
        RETURN NEW;
    END IF;
    SELECT a.analysis_json->'segments'->NEW.segment_index,x.blocks_json->NEW.block_index,f.document_role_code,f.role_origin_code
    INTO segment,block,file_role,file_origin
    FROM announcement_attachment_segment_analyses a
    JOIN announcement_source_attachment_extractions x ON x.id=a.extraction_id AND x.file_id=a.file_id AND x.set_id=a.set_id AND x.source_id=a.source_id
    JOIN announcement_source_attachment_files f ON f.id=a.file_id AND f.set_id=a.set_id AND f.source_id=a.source_id
    WHERE a.id=NEW.segment_analysis_id;
    IF segment IS NULL OR block IS NULL OR NEW.start_offset<greatest((segment->>'startOffset')::integer,(block->>'startOffset')::integer)
        OR NEW.end_offset>least((segment->>'endOffset')::integer,(block->>'endOffset')::integer) OR NEW.end_offset<=NEW.start_offset THEN
        RAISE EXCEPTION 'attachment segment match position invalid' USING ERRCODE='23514';
    END IF;
    role_code:=segment->>'roleCode';
    IF NEW.applied_action_code<>'CONTEXT_ONLY' AND (role_code NOT IN ('NOTICE','GUIDE') OR NOT (block->>'scopeReliable')::boolean
        OR (file_origin IN ('MANUAL','PROFILE') AND file_role<>role_code)) THEN
        RAISE EXCEPTION 'attachment segment context cannot become decision evidence' USING ERRCODE='23514';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM announcement_source_attachment_evaluations e WHERE e.id=NEW.evaluation_id
        AND e.segment_binding_xid=pg_current_xact_id()) THEN
        RAISE EXCEPTION 'attachment segment matches require creation transaction' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_match_segment BEFORE INSERT ON announcement_source_attachment_matches
    FOR EACH ROW EXECUTE FUNCTION check_attachment_segment_match();

-- 같은 transaction에서 파일 전체가 입력에 남아야 한다. 실패 파일/미확인 구간을 빼서 성공 판정을 만들지 않는다.
CREATE FUNCTION check_attachment_segment_evaluation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.engine_version<>'attachment-segment-1.0.0' THEN RETURN NULL; END IF;
    -- 원문 삭제 cascade 중 이미 제거된 평가에는 검사할 최종 상태가 없다.
    IF NOT EXISTS (SELECT 1 FROM announcement_source_attachment_evaluations WHERE id=NEW.id) THEN RETURN NULL; END IF;
    IF NOT EXISTS (SELECT 1 FROM announcement_attachment_policies p WHERE p.id=NEW.policy_id
        AND p.settings_json->>'engineVersion'=NEW.engine_version
        AND coalesce(p.settings_json->>'segmentRuleVersion','') ~ '^[A-Za-z0-9_.-]{1,40}$'
        AND coalesce(p.settings_json->>'segmentRulesHash','') ~ '^[0-9a-f]{64}$') THEN
        RAISE EXCEPTION 'attachment segment evaluation policy missing' USING ERRCODE='23514';
    END IF;
    IF (SELECT count(1) FROM announcement_source_attachment_evaluation_inputs WHERE evaluation_id=NEW.id)
        <> (SELECT count(1) FROM announcement_source_attachment_files WHERE set_id=NEW.set_id AND source_id=NEW.source_id) THEN
        RAISE EXCEPTION 'attachment segment evaluation omits files' USING ERRCODE='23514';
    END IF;
    IF NEW.decision_status_code='ACCEPTED' AND EXISTS (
        SELECT 1 FROM announcement_source_attachment_evaluation_inputs i
        LEFT JOIN announcement_attachment_segment_analyses a ON a.id=i.segment_analysis_id
        WHERE i.evaluation_id=NEW.id AND (i.input_status_code<>'COMPLETE_TEXT' OR a.id IS NULL OR a.analysis_json->>'statusCode'<>'RESOLVED')) THEN
        RAISE EXCEPTION 'attachment segment incomplete input cannot be accepted' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_segment_evaluation AFTER INSERT ON announcement_source_attachment_evaluations
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_segment_evaluation();

-- 새 결합을 사후에 바꾸거나 떼어내지 않는다. 기존 원문 삭제 cascade만 허용한다.
CREATE FUNCTION protect_attachment_segment_binding() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM announcement_source_snapshots WHERE id=OLD.source_id) AND TG_OP='DELETE' THEN RETURN OLD; END IF;
    IF OLD.segment_analysis_id IS NOT NULL OR (TG_OP='UPDATE' AND NEW.segment_analysis_id IS NOT NULL)
        OR EXISTS (SELECT 1 FROM announcement_source_attachment_evaluations WHERE id=OLD.evaluation_id AND engine_version='attachment-segment-1.0.0') THEN
        RAISE EXCEPTION 'attachment segment binding is immutable' USING ERRCODE='23514';
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_input_segment_immutable BEFORE UPDATE OR DELETE ON announcement_source_attachment_evaluation_inputs
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_segment_binding();
CREATE TRIGGER tr_att_match_segment_immutable BEFORE UPDATE OR DELETE ON announcement_source_attachment_matches
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_segment_binding();
CREATE FUNCTION protect_attachment_segment_evaluation_delete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.engine_version='attachment-segment-1.0.0' AND EXISTS (SELECT 1 FROM announcement_source_snapshots WHERE id=OLD.source_id) THEN
        RAISE EXCEPTION 'attachment segment evaluation is immutable' USING ERRCODE='23514';
    END IF;
    RETURN OLD;
END $$;
CREATE TRIGGER tr_att_segment_evaluation_delete BEFORE DELETE ON announcement_source_attachment_evaluations
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_segment_evaluation_delete();
