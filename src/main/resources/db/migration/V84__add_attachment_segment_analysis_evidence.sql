-- 파일 단위 역할/기존 봉인 이력과 분리한 구간 분석 이력이다. 저장만으로 evaluation/정책을 변경하지 않는다.
CREATE TABLE announcement_attachment_segment_analyses (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    extraction_id uuid NOT NULL,
    file_id uuid NOT NULL,
    set_id uuid NOT NULL,
    source_id uuid NOT NULL,
    analysis_version varchar(40) NOT NULL CHECK (analysis_version ~ '^[A-Za-z0-9_.-]{1,40}$'),
    rules_hash varchar(64) NOT NULL CHECK (rules_hash ~ '^[0-9a-f]{64}$'),
    text_hash varchar(64) NOT NULL CHECK (text_hash ~ '^[0-9a-f]{64}$'),
    blocks_hash varchar(64) NOT NULL CHECK (blocks_hash ~ '^[0-9a-f]{64}$'),
    analysis_json jsonb NOT NULL CHECK (jsonb_typeof(analysis_json)='object' AND octet_length(analysis_json::text)<=262144),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_segment_extraction FOREIGN KEY (extraction_id,file_id,set_id,source_id)
        REFERENCES announcement_source_attachment_extractions(id,file_id,set_id,source_id) ON DELETE CASCADE,
    CONSTRAINT uq_att_segment_version UNIQUE (extraction_id,analysis_version,rules_hash)
);
CREATE INDEX ix_att_segment_source_set ON announcement_attachment_segment_analyses(source_id,set_id,created_at,id);

-- 원문 대신 버전/지문/정확한 위치만 저장한다. JSON 필드 추가도 새 계약의 검증 대상이다.
CREATE FUNCTION validate_attachment_segment_analysis() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE a jsonb := NEW.analysis_json; s jsonb; e jsonb; b jsonb; text_value text; text_digest text;
    blocks jsonb; quality text; total integer; expected_index integer := 0; previous_end integer := 0;
    start_pos integer; end_pos integer; block_start integer; block_end integer; evidence_start integer; evidence_end integer;
    expected_block integer := 0; previous_block_end integer := 0; scopes text[] := ARRAY[]::text[];
    has_unknown boolean := false; role_code text; reason_code text; evidence_codes text[];
BEGIN
    SELECT x.extracted_text,x.text_hash,x.blocks_json,x.quality_code INTO text_value,text_digest,blocks,quality
    FROM announcement_source_attachment_extractions x
    JOIN announcement_source_attachment_sets aset ON aset.id=x.set_id AND aset.source_id=x.source_id
    WHERE x.id=NEW.extraction_id AND x.file_id=NEW.file_id AND x.set_id=NEW.set_id AND x.source_id=NEW.source_id
        AND aset.set_status_code='SEALED';
    IF NOT FOUND OR text_value IS NULL OR text_digest IS NULL OR length(btrim(text_value))=0
        OR text_digest IS DISTINCT FROM NEW.text_hash OR encode(digest(text_value,'sha256'),'hex') IS DISTINCT FROM NEW.text_hash
        OR jsonb_typeof(blocks) IS DISTINCT FROM 'array' OR jsonb_array_length(blocks) NOT BETWEEN 1 AND 20000 THEN
        RAISE EXCEPTION 'attachment segment extraction binding invalid' USING ERRCODE='23514';
    END IF;
    total:=char_length(text_value);
    IF total NOT BETWEEN 1 AND 1000000 OR attachment_role_blocks_hash(blocks) IS DISTINCT FROM NEW.blocks_hash
        OR (SELECT count(1) FROM jsonb_object_keys(a))<>8
        OR NOT (a ?& ARRAY['analysisVersion','rulesHash','textHash','blocksHash','textLength','statusCode','reasonCode','segments'])
        OR jsonb_typeof(a->'analysisVersion') IS DISTINCT FROM 'string' OR jsonb_typeof(a->'rulesHash') IS DISTINCT FROM 'string'
        OR jsonb_typeof(a->'textHash') IS DISTINCT FROM 'string' OR jsonb_typeof(a->'blocksHash') IS DISTINCT FROM 'string'
        OR jsonb_typeof(a->'statusCode') IS DISTINCT FROM 'string' OR jsonb_typeof(a->'reasonCode') IS DISTINCT FROM 'string'
        OR a->>'analysisVersion' IS DISTINCT FROM NEW.analysis_version OR a->>'rulesHash' IS DISTINCT FROM NEW.rules_hash
        OR a->>'textHash' IS DISTINCT FROM NEW.text_hash OR a->>'blocksHash' IS DISTINCT FROM NEW.blocks_hash
        OR jsonb_typeof(a->'textLength') IS DISTINCT FROM 'number' OR coalesce(a->>'textLength','') !~ '^[0-9]{1,7}$'
        OR jsonb_typeof(a->'segments') IS DISTINCT FROM 'array' OR jsonb_array_length(a->'segments') NOT BETWEEN 1 AND 200
        OR coalesce(a->>'statusCode','') NOT IN ('RESOLVED','REVIEW_REQUIRED')
        OR coalesce(a->>'reasonCode','') NOT IN ('SEGMENTS_RESOLVED','SEGMENT_CONTEXT_REQUIRED','COMPLETE_TEXT_REQUIRED','STRUCTURE_UNCERTAIN','SEGMENT_ANALYSIS_LIMIT') THEN
        RAISE EXCEPTION 'attachment segment analysis shape invalid' USING ERRCODE='23514';
    END IF;
    IF (a->>'textLength')::integer<>total THEN
        RAISE EXCEPTION 'attachment segment text length invalid' USING ERRCODE='23514';
    END IF;
    -- 참조 block의 전체 원문 포함/순서/유일성을 검증한다. 빠진 구간을 공백으로 가장할 수 없다.
    FOR b IN SELECT item.value FROM jsonb_array_elements(blocks) item(value) LOOP
        IF jsonb_typeof(b) IS DISTINCT FROM 'object' OR jsonb_typeof(b->'index') IS DISTINCT FROM 'number'
            OR coalesce(b->>'index','') !~ '^[0-9]{1,7}$' OR jsonb_typeof(b->'startOffset') IS DISTINCT FROM 'number'
            OR coalesce(b->>'startOffset','') !~ '^[0-9]{1,7}$' OR jsonb_typeof(b->'endOffset') IS DISTINCT FROM 'number'
            OR coalesce(b->>'endOffset','') !~ '^[0-9]{1,7}$' OR jsonb_typeof(b->'scopeReliable') IS DISTINCT FROM 'boolean'
            OR jsonb_typeof(b->'evidenceScopeId') IS DISTINCT FROM 'string' OR jsonb_typeof(b->'locator') IS DISTINCT FROM 'string'
            OR length(b->>'evidenceScopeId') NOT BETWEEN 1 AND 300 OR length(b->>'locator') NOT BETWEEN 1 AND 300
            OR (b->>'evidenceScopeId') ~ '^[[:space:]]*$' OR (b->>'locator') ~ '^[[:space:]]*$'
            OR (b->>'evidenceScopeId')=ANY(scopes) THEN
            RAISE EXCEPTION 'attachment segment block shape invalid' USING ERRCODE='23514';
        END IF;
        block_start:=(b->>'startOffset')::integer; block_end:=(b->>'endOffset')::integer;
        IF (b->>'index')::integer<>expected_block OR block_start<previous_block_end OR block_end<=block_start OR block_end>total
            OR substring(text_value FROM previous_block_end+1 FOR block_start-previous_block_end) !~ '^[[:space:]]*$' THEN
            RAISE EXCEPTION 'attachment segment block coverage invalid' USING ERRCODE='23514';
        END IF;
        previous_block_end:=block_end; expected_block:=expected_block+1; scopes:=array_append(scopes,b->>'evidenceScopeId');
    END LOOP;
    IF substring(text_value FROM previous_block_end+1) !~ '^[[:space:]]*$' THEN
        RAISE EXCEPTION 'attachment segment trailing text omitted' USING ERRCODE='23514';
    END IF;
    FOR s IN SELECT item.value FROM jsonb_array_elements(a->'segments') item(value) LOOP
        IF jsonb_typeof(s) IS DISTINCT FROM 'object' THEN
            RAISE EXCEPTION 'attachment segment shape invalid' USING ERRCODE='23514';
        END IF;
        IF (SELECT count(1) FROM jsonb_object_keys(s))<>6 OR NOT (s ?& ARRAY['index','startOffset','endOffset','roleCode','reasonCode','evidence'])
            OR jsonb_typeof(s->'index') IS DISTINCT FROM 'number' OR coalesce(s->>'index','') !~ '^[0-9]{1,7}$'
            OR jsonb_typeof(s->'startOffset') IS DISTINCT FROM 'number' OR coalesce(s->>'startOffset','') !~ '^[0-9]{1,7}$'
            OR jsonb_typeof(s->'endOffset') IS DISTINCT FROM 'number' OR coalesce(s->>'endOffset','') !~ '^[0-9]{1,7}$'
            OR coalesce(s->>'roleCode','') NOT IN ('NOTICE','GUIDE','FORM','REFERENCE','UNKNOWN')
            OR jsonb_typeof(s->'evidence') IS DISTINCT FROM 'array' OR jsonb_array_length(s->'evidence')>100 THEN
            RAISE EXCEPTION 'attachment segment fields invalid' USING ERRCODE='23514';
        END IF;
        start_pos:=(s->>'startOffset')::integer; end_pos:=(s->>'endOffset')::integer;
        role_code:=s->>'roleCode'; reason_code:=s->>'reasonCode';
        IF (s->>'index')::integer<>expected_index OR start_pos<>previous_end OR end_pos<=start_pos OR end_pos>total
            OR reason_code IS NULL OR (role_code='UNKNOWN' AND reason_code NOT IN ('STRUCTURE_UNCERTAIN','ROLE_ANALYSIS_LIMIT',
                'MIXED_DOCUMENT_ROLES','INITIAL_HEADING_REQUIRED','ROLE_STRUCTURE_INCOMPLETE','COMPLETE_TEXT_REQUIRED','SEGMENT_ANALYSIS_LIMIT'))
            OR (role_code<>'UNKNOWN' AND (quality<>'COMPLETE_TEXT' OR reason_code<>'ROLE_TEXT_STRUCTURE_MATCHED'
                OR jsonb_array_length(s->'evidence')<>CASE WHEN role_code IN ('NOTICE','GUIDE') THEN 4 ELSE 3 END)) THEN
            RAISE EXCEPTION 'attachment segment range or role invalid' USING ERRCODE='23514';
        END IF;
        -- 미확인 block을 포함한 구간을 확정 역할로 저장하지 않는다.
        IF role_code<>'UNKNOWN' AND EXISTS (SELECT 1 FROM jsonb_array_elements(blocks) item(value)
                WHERE (item.value->>'startOffset')::integer<end_pos AND (item.value->>'endOffset')::integer>start_pos
                    AND NOT (item.value->>'scopeReliable')::boolean) THEN
            RAISE EXCEPTION 'attachment segment unreliable scope promoted' USING ERRCODE='23514';
        END IF;
        evidence_codes:=ARRAY[]::text[];
        FOR e IN SELECT item.value FROM jsonb_array_elements(s->'evidence') item(value) LOOP
            IF jsonb_typeof(e) IS DISTINCT FROM 'object' THEN
                RAISE EXCEPTION 'attachment segment evidence shape invalid' USING ERRCODE='23514';
            END IF;
            IF (SELECT count(1) FROM jsonb_object_keys(e))<>4 OR NOT (e ?& ARRAY['ruleCode','blockIndex','startOffset','endOffset'])
                OR coalesce(e->>'ruleCode','') NOT IN ('NOTICE_HEADING','GUIDE_HEADING','FORM_HEADING','REFERENCE_HEADING',
                    'TARGET_SECTION','SUPPORT_SECTION','APPLICATION_SECTION','APPLICANT_FIELD','SIGNATURE_FIELD','QUESTION_ITEM','ANSWER_ITEM')
                OR jsonb_typeof(e->'blockIndex') IS DISTINCT FROM 'number' OR coalesce(e->>'blockIndex','') !~ '^[0-9]{1,7}$'
                OR jsonb_typeof(e->'startOffset') IS DISTINCT FROM 'number' OR coalesce(e->>'startOffset','') !~ '^[0-9]{1,7}$'
                OR jsonb_typeof(e->'endOffset') IS DISTINCT FROM 'number' OR coalesce(e->>'endOffset','') !~ '^[0-9]{1,7}$' THEN
                RAISE EXCEPTION 'attachment segment evidence fields invalid' USING ERRCODE='23514';
            END IF;
            b:=blocks->(e->>'blockIndex')::integer; evidence_start:=(e->>'startOffset')::integer; evidence_end:=(e->>'endOffset')::integer;
            IF b IS NULL OR NOT (b->>'scopeReliable')::boolean OR evidence_start<greatest(start_pos,(b->>'startOffset')::integer)
                OR evidence_end>least(end_pos,(b->>'endOffset')::integer) OR evidence_end<=evidence_start THEN
                RAISE EXCEPTION 'attachment segment evidence location invalid' USING ERRCODE='23514';
            END IF;
            evidence_codes:=array_append(evidence_codes,e->>'ruleCode');
        END LOOP;
        IF role_code<>'UNKNOWN' AND NOT (evidence_codes @> CASE role_code
                WHEN 'NOTICE' THEN ARRAY['NOTICE_HEADING','TARGET_SECTION','SUPPORT_SECTION','APPLICATION_SECTION']
                WHEN 'GUIDE' THEN ARRAY['GUIDE_HEADING','TARGET_SECTION','SUPPORT_SECTION','APPLICATION_SECTION']
                WHEN 'FORM' THEN ARRAY['FORM_HEADING','APPLICANT_FIELD','SIGNATURE_FIELD']
                ELSE ARRAY['REFERENCE_HEADING','QUESTION_ITEM','ANSWER_ITEM'] END) THEN
            RAISE EXCEPTION 'attachment segment required evidence missing' USING ERRCODE='23514';
        END IF;
        has_unknown:=has_unknown OR role_code='UNKNOWN'; previous_end:=end_pos; expected_index:=expected_index+1;
    END LOOP;
    IF previous_end<>total OR (a->>'statusCode'='RESOLVED') IS DISTINCT FROM (NOT has_unknown)
        OR (a->>'statusCode'='RESOLVED' AND (a->>'reasonCode'<>'SEGMENTS_RESOLVED' OR quality<>'COMPLETE_TEXT'))
        OR (a->>'statusCode'='REVIEW_REQUIRED' AND a->>'reasonCode'='SEGMENTS_RESOLVED') THEN
        RAISE EXCEPTION 'attachment segment completeness invalid' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_segment_validate BEFORE INSERT ON announcement_attachment_segment_analyses
    FOR EACH ROW EXECUTE FUNCTION validate_attachment_segment_analysis();

-- 분석 결과는 덮어쓰지 않는다. 새 규칙 버전은 기존 FK/근거를 보존하는 새 행이다.
CREATE FUNCTION protect_attachment_segment_analysis() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    -- 기존 원문 삭제의 cascade 계약만 보존한다. 살아 있는 원문의 분석 단독 삭제는 금지한다.
    IF TG_OP='DELETE' AND NOT EXISTS (SELECT 1 FROM announcement_source_snapshots WHERE id=OLD.source_id) THEN RETURN OLD; END IF;
    RAISE EXCEPTION 'attachment segment analysis is immutable' USING ERRCODE='23514';
END $$;
CREATE TRIGGER tr_att_segment_immutable BEFORE UPDATE OR DELETE ON announcement_attachment_segment_analyses
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_segment_analysis();
