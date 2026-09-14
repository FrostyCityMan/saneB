-- 텍스트 역할 판정은 해당 파일의 정확한 추출 근거와 함께 보관한다. 기존 정책/역할/봉인 이력은 갱신하지 않는다.
ALTER TABLE announcement_source_attachment_files
    DROP CONSTRAINT announcement_source_attachment_files_role_origin_code_check,
    ADD CONSTRAINT ck_att_file_role_origin CHECK (role_origin_code IN ('UNKNOWN','PROFILE','MANUAL','TEXT_RULE')),
    ADD COLUMN role_extraction_id uuid,
    ADD COLUMN role_assessment_json jsonb,
    ADD CONSTRAINT fk_att_file_role_extract FOREIGN KEY (role_extraction_id,id,set_id,source_id)
        REFERENCES announcement_source_attachment_extractions(id,file_id,set_id,source_id) DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT ck_att_file_role_assessment CHECK (
        (role_extraction_id IS NULL AND role_assessment_json IS NULL AND role_origin_code<>'TEXT_RULE')
        OR (role_extraction_id IS NOT NULL AND role_assessment_json IS NOT NULL
            AND role_origin_code IN ('TEXT_RULE','MANUAL') AND jsonb_typeof(role_assessment_json)='object'
            AND octet_length(role_assessment_json::text)<=32768));
CREATE INDEX ix_att_file_role_extract ON announcement_source_attachment_files(role_extraction_id) WHERE role_extraction_id IS NOT NULL;

-- JSON 표현의 공백/key 순서와 무관한 block 지문. Java 판정기의 동일 버전 정규 표현과 대응한다.
CREATE FUNCTION attachment_role_blocks_hash(blocks jsonb) RETURNS text LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT encode(digest(E'attachment-role-blocks-v1\n' || coalesce(string_agg(
        (b.item->>'index') || ':' || (b.item->>'startOffset') || ':' || (b.item->>'endOffset') || ':' ||
        replace(encode(convert_to(b.item->>'evidenceScopeId','UTF8'),'base64'),E'\n','') || ':' ||
        CASE WHEN (b.item->>'scopeReliable')::boolean THEN '1' ELSE '0' END || ':' ||
        replace(encode(convert_to(b.item->>'locator','UTF8'),'base64'),E'\n','') || E'\n', '' ORDER BY b.ordinal),''),'sha256'),'hex')
    FROM jsonb_array_elements(blocks) WITH ORDINALITY AS b(item,ordinal)
$$;

-- 원문 대신 지문/규칙 코드/실제 위치만 허용한다. 같은 file/set/source/extraction 및 정책 버전을 재검증한다.
CREATE FUNCTION validate_attachment_role_evidence(file_key uuid) RETURNS void LANGUAGE plpgsql AS $$
DECLARE assessment jsonb; role_code text; origin_code text; extraction_key uuid; source_key uuid; set_key uuid;
        text_value text; text_digest text; blocks jsonb; quality text; policy_settings jsonb; item jsonb; block jsonb;
        role_reason text; reused_key uuid; block_index integer; position_start integer; position_end integer; previous_end integer := 0;
        expected_index integer := 0; block_start integer; block_end integer; seen_scopes text[] := ARRAY[]::text[];
BEGIN
    SELECT f.role_assessment_json,f.document_role_code,f.role_origin_code,f.role_extraction_id,f.source_id,f.set_id,p.settings_json
    INTO assessment,role_code,origin_code,extraction_key,source_key,set_key,policy_settings
    FROM announcement_source_attachment_files f
    JOIN announcement_source_attachment_sets s ON s.id=f.set_id AND s.source_id=f.source_id
    JOIN announcement_attachment_policies p ON p.id=s.policy_id WHERE f.id=file_key;
    IF NOT FOUND OR assessment IS NULL THEN RETURN; END IF;
    IF (SELECT count(1) FROM jsonb_object_keys(assessment))<>7
        OR NOT (assessment ?& ARRAY['ruleVersion','rulesHash','textHash','blocksHash','roleCode','reasonCode','evidence'])
        OR jsonb_typeof(assessment->'ruleVersion') IS DISTINCT FROM 'string'
        OR jsonb_typeof(assessment->'rulesHash') IS DISTINCT FROM 'string'
        OR jsonb_typeof(assessment->'textHash') IS DISTINCT FROM 'string'
        OR jsonb_typeof(assessment->'blocksHash') IS DISTINCT FROM 'string'
        OR jsonb_typeof(assessment->'roleCode') IS DISTINCT FROM 'string'
        OR jsonb_typeof(assessment->'reasonCode') IS DISTINCT FROM 'string'
        OR jsonb_typeof(assessment->'evidence') IS DISTINCT FROM 'array' THEN
        RAISE EXCEPTION 'attachment role assessment shape invalid' USING ERRCODE='23514';
    END IF;
    IF (assessment->>'ruleVersion') !~ '^[A-Za-z0-9_.-]{1,40}$'
        OR (assessment->>'rulesHash') !~ '^[0-9a-f]{64}$' OR (assessment->>'textHash') !~ '^[0-9a-f]{64}$'
        OR (assessment->>'blocksHash') !~ '^[0-9a-f]{64}$' OR jsonb_array_length(assessment->'evidence')>100
        OR (assessment->>'roleCode') NOT IN ('NOTICE','GUIDE','FORM','REFERENCE','UNKNOWN')
        OR (assessment->>'ruleVersion') IS DISTINCT FROM (policy_settings->>'roleRuleVersion')
        OR (assessment->>'rulesHash') IS DISTINCT FROM (policy_settings->>'roleRulesHash')
        OR (origin_code='TEXT_RULE' AND role_code IS DISTINCT FROM (assessment->>'roleCode')) THEN
        RAISE EXCEPTION 'attachment role policy or result binding invalid' USING ERRCODE='23514';
    END IF;
    role_reason:=assessment->>'reasonCode';
    IF ((assessment->>'roleCode')='UNKNOWN' AND role_reason NOT IN ('STRUCTURE_UNCERTAIN','ROLE_ANALYSIS_LIMIT',
            'MIXED_DOCUMENT_ROLES','INITIAL_HEADING_REQUIRED','ROLE_STRUCTURE_INCOMPLETE'))
        OR ((assessment->>'roleCode')<>'UNKNOWN' AND (role_reason<>'ROLE_TEXT_STRUCTURE_MATCHED'
            OR jsonb_array_length(assessment->'evidence') NOT BETWEEN 3 AND 4)) THEN
        RAISE EXCEPTION 'attachment role reason invalid' USING ERRCODE='23514';
    END IF;
    SELECT x.extracted_text,x.text_hash,x.blocks_json,x.quality_code,x.reused_from_extraction_id INTO text_value,text_digest,blocks,quality,reused_key
    FROM announcement_source_attachment_extractions x
    WHERE x.id=extraction_key AND x.file_id=file_key AND x.set_id=set_key AND x.source_id=source_key;
    IF NOT FOUND OR quality<>'COMPLETE_TEXT' OR text_value IS NULL OR text_digest IS NULL
        OR text_digest IS DISTINCT FROM (assessment->>'textHash')
        OR encode(digest(text_value,'sha256'),'hex') IS DISTINCT FROM text_digest
        OR extraction_key IS DISTINCT FROM (SELECT x.id FROM announcement_source_attachment_extractions x
            WHERE x.file_id=file_key AND x.set_id=set_key AND x.source_id=source_key ORDER BY x.attempt_no DESC,x.id DESC LIMIT 1) THEN
        RAISE EXCEPTION 'attachment role extraction binding invalid' USING ERRCODE='23514';
    END IF;
    IF (origin_code='MANUAL' AND reused_key IS NULL) OR (reused_key IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM announcement_source_attachment_extractions original
        JOIN announcement_source_attachment_files original_file ON original_file.id=original.file_id AND original_file.source_id=original.source_id
        WHERE original.id=reused_key AND original.source_id=source_key AND original_file.role_extraction_id=original.id
            AND original_file.role_assessment_json=assessment)) THEN
        RAISE EXCEPTION 'attachment role reuse binding invalid' USING ERRCODE='23514';
    END IF;
    IF jsonb_typeof(blocks) IS DISTINCT FROM 'array' OR jsonb_array_length(blocks) NOT BETWEEN 1 AND 20000
        OR char_length(text_value)>1000000 THEN
        RAISE EXCEPTION 'attachment role input limit invalid' USING ERRCODE='23514';
    END IF;
    -- 완전한 텍스트 전체가 중복 없는 block 순서로 덮여 있어야 한다. 생략한 원문으로 역할을 확정하지 않는다.
    FOR block IN SELECT b.value FROM jsonb_array_elements(blocks) AS b(value) LOOP
        IF jsonb_typeof(block) IS DISTINCT FROM 'object'
            OR jsonb_typeof(block->'index') IS DISTINCT FROM 'number' OR jsonb_typeof(block->'startOffset') IS DISTINCT FROM 'number'
            OR jsonb_typeof(block->'endOffset') IS DISTINCT FROM 'number'
            OR coalesce(block->>'index','') !~ '^[0-9]{1,7}$' OR coalesce(block->>'startOffset','') !~ '^[0-9]{1,7}$'
            OR coalesce(block->>'endOffset','') !~ '^[0-9]{1,7}$' OR jsonb_typeof(block->'scopeReliable') IS DISTINCT FROM 'boolean'
            OR jsonb_typeof(block->'evidenceScopeId') IS DISTINCT FROM 'string' OR jsonb_typeof(block->'locator') IS DISTINCT FROM 'string'
            OR length(block->>'evidenceScopeId') NOT BETWEEN 1 AND 300 OR length(block->>'locator') NOT BETWEEN 1 AND 300
            OR (block->>'evidenceScopeId') ~ '^[[:space:]]*$' OR (block->>'locator') ~ '^[[:space:]]*$'
            OR (block->>'evidenceScopeId')=ANY(seen_scopes) THEN
            RAISE EXCEPTION 'attachment role block shape invalid' USING ERRCODE='23514';
        END IF;
        block_start:=(block->>'startOffset')::integer; block_end:=(block->>'endOffset')::integer;
        IF (block->>'index')::integer<>expected_index OR block_start<previous_end OR block_end<=block_start
            OR block_end>char_length(text_value) OR substring(text_value FROM previous_end+1 FOR block_start-previous_end) !~ '^[[:space:]]*$' THEN
            RAISE EXCEPTION 'attachment role block range invalid' USING ERRCODE='23514';
        END IF;
        previous_end:=block_end; expected_index:=expected_index+1;
        seen_scopes:=array_append(seen_scopes,block->>'evidenceScopeId');
    END LOOP;
    IF expected_index NOT BETWEEN 1 AND 20000 OR substring(text_value FROM previous_end+1) !~ '^[[:space:]]*$'
        OR attachment_role_blocks_hash(blocks) IS DISTINCT FROM (assessment->>'blocksHash') THEN
        RAISE EXCEPTION 'attachment role blocks digest invalid' USING ERRCODE='23514';
    END IF;
    FOR item IN SELECT e.value FROM jsonb_array_elements(assessment->'evidence') AS e(value) LOOP
        IF jsonb_typeof(item) IS DISTINCT FROM 'object' THEN
            RAISE EXCEPTION 'attachment role evidence shape invalid' USING ERRCODE='23514';
        END IF;
        IF (SELECT count(1) FROM jsonb_object_keys(item))<>4 OR NOT (item ?& ARRAY['ruleCode','blockIndex','startOffset','endOffset'])
            OR coalesce(item->>'ruleCode','') NOT IN ('NOTICE_HEADING','GUIDE_HEADING','FORM_HEADING','REFERENCE_HEADING',
                'TARGET_SECTION','SUPPORT_SECTION','APPLICATION_SECTION','APPLICANT_FIELD','SIGNATURE_FIELD','QUESTION_ITEM','ANSWER_ITEM')
            OR jsonb_typeof(item->'ruleCode') IS DISTINCT FROM 'string'
            OR jsonb_typeof(item->'blockIndex') IS DISTINCT FROM 'number' OR coalesce(item->>'blockIndex','') !~ '^[0-9]{1,7}$'
            OR jsonb_typeof(item->'startOffset') IS DISTINCT FROM 'number' OR coalesce(item->>'startOffset','') !~ '^[0-9]{1,7}$'
            OR jsonb_typeof(item->'endOffset') IS DISTINCT FROM 'number' OR coalesce(item->>'endOffset','') !~ '^[0-9]{1,7}$' THEN
            RAISE EXCEPTION 'attachment role evidence fields invalid' USING ERRCODE='23514';
        END IF;
        block_index:=(item->>'blockIndex')::integer; position_start:=(item->>'startOffset')::integer; position_end:=(item->>'endOffset')::integer;
        block:=blocks->block_index;
        IF block IS NULL OR (block->>'index')::integer<>block_index OR NOT (block->>'scopeReliable')::boolean
            OR position_start<(block->>'startOffset')::integer OR position_end>(block->>'endOffset')::integer OR position_end<=position_start THEN
            RAISE EXCEPTION 'attachment role evidence location invalid' USING ERRCODE='23514';
        END IF;
    END LOOP;
END $$;

-- file이 먼저 들어오고 extraction이 뒤따르므로 commit 시 양쪽의 최신 결합을 검증한다.
CREATE FUNCTION check_attachment_role_evidence() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME='announcement_source_attachment_files' THEN PERFORM validate_attachment_role_evidence(NEW.id);
    ELSE PERFORM validate_attachment_role_evidence(NEW.file_id); END IF;
    RETURN NEW;
END $$;
CREATE CONSTRAINT TRIGGER tr_att_file_role_binding AFTER INSERT OR UPDATE ON announcement_source_attachment_files
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_role_evidence();
CREATE CONSTRAINT TRIGGER tr_att_extract_role_binding AFTER INSERT ON announcement_source_attachment_extractions
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_role_evidence();

-- OPEN이어도 한번 기록한 역할 근거를 덮어쓰지 않는다. 역할 변경은 기존 새 set/새 file 복제 경로를 따른다.
CREATE FUNCTION protect_attachment_role_assessment() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.role_assessment_json IS NOT NULL AND (NEW.role_extraction_id,NEW.role_assessment_json,NEW.document_role_code,NEW.role_origin_code)
        IS DISTINCT FROM (OLD.role_extraction_id,OLD.role_assessment_json,OLD.document_role_code,OLD.role_origin_code) THEN
        RAISE EXCEPTION 'attachment role assessment is immutable' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_role_assessment_immutable BEFORE UPDATE ON announcement_source_attachment_files
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_role_assessment();
