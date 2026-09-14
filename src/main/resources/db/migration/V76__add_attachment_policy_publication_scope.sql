-- 게시 준비 범위의 불변 원장이다. 정책 게시/승인/ENFORCE/worker 활성화는 수행하지 않는다.
-- 원문·URL·폼 인자는 보관하지 않는다. 대상 삭제 이후에도 비식별 ID/상태 hash만 감사 metadata로 보존한다.
CREATE TABLE announcement_attachment_policy_publication_scopes (
    id uuid PRIMARY KEY,
    policy_id uuid NOT NULL REFERENCES announcement_attachment_policies(id),
    policy_row_version integer NOT NULL CHECK (policy_row_version>=0),
    rule_release_id uuid NOT NULL REFERENCES announcement_source_classification_rule_releases(id),
    rule_row_version integer NOT NULL CHECK (rule_row_version>=0),
    mode_code varchar(20) NOT NULL CHECK (mode_code IN ('OFF','COLLECT_ONLY','ENFORCE')),
    qa_run_id uuid,
    qa_snapshot_hash varchar(64) CHECK (qa_snapshot_hash ~ '^[0-9a-f]{64}$'),
    scope_status_code varchar(20) NOT NULL DEFAULT 'OPEN' CHECK (scope_status_code IN ('OPEN','SEALED')),
    item_count bigint NOT NULL DEFAULT 0 CHECK (item_count>=0),
    scope_hash varchar(64) CHECK (scope_hash ~ '^[0-9a-f]{64}$'),
    requested_by uuid NOT NULL REFERENCES users(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    reason_hash varchar(64) NOT NULL CHECK (reason_hash ~ '^[0-9a-f]{64}$'),
    created_xid xid8 NOT NULL DEFAULT pg_current_xact_id(),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    expires_at timestamptz NOT NULL,
    FOREIGN KEY (qa_run_id,policy_id) REFERENCES announcement_attachment_policy_validation_runs(id,policy_id),
    CONSTRAINT ck_att_publication_scope_qa CHECK ((qa_run_id IS NULL)=(qa_snapshot_hash IS NULL)),
    CONSTRAINT ck_att_publication_scope_seal CHECK (
        (scope_status_code='OPEN' AND scope_hash IS NULL AND item_count=0)
        OR (scope_status_code='SEALED' AND scope_hash IS NOT NULL AND item_count>0)),
    CONSTRAINT ck_att_publication_scope_expiry CHECK (expires_at>created_at AND expires_at<=created_at+interval '15 minutes')
);
CREATE INDEX ix_att_publication_scope_policy ON announcement_attachment_policy_publication_scopes(policy_id,created_at DESC,id DESC);
CREATE INDEX ix_att_publication_scope_actor ON announcement_attachment_policy_publication_scopes(requested_by);
CREATE INDEX ix_att_publication_scope_rule ON announcement_attachment_policy_publication_scopes(rule_release_id);
CREATE INDEX ix_att_publication_scope_qa ON announcement_attachment_policy_publication_scopes(qa_run_id,policy_id) WHERE qa_run_id IS NOT NULL;

CREATE TABLE announcement_attachment_policy_publication_scope_items (
    scope_id uuid NOT NULL REFERENCES announcement_attachment_policy_publication_scopes(id),
    entity_type_code varchar(30) NOT NULL CHECK (entity_type_code IN ('POLICY','SOURCE','JOB','COLLECTION_PLAN','COLLECTOR')),
    entity_id uuid NOT NULL,
    state_hash varchar(64) NOT NULL CHECK (state_hash ~ '^[0-9a-f]{64}$'),
    PRIMARY KEY (scope_id,entity_type_code,entity_id)
);

-- 전역 OFF의 영향과 이전 정책에 고정된 작업까지 포함한다. 건수 합계/LIMIT로 실제 ID 범위를 대신하지 않는다.
-- 반환되는 값은 ID와 SHA-256뿐이다. 상태 해시 입력의 운영 원문·개인정보·인증 값은 제외한다.
CREATE FUNCTION attachment_policy_publication_members(target_policy uuid)
RETURNS TABLE(entity_type_code varchar,entity_id uuid,state_hash varchar) LANGUAGE sql STABLE AS $$
    SELECT 'POLICY'::varchar,p.id,encode(digest(jsonb_build_array(p.rule_release_id,p.row_version,p.policy_status_code,
        p.mode_code,p.policy_hash,p.settings_json,p.profile_manifest_json,r.row_version,r.release_status_code,r.rule_snapshot_hash)::text,'sha256'),'hex')::varchar
    FROM announcement_attachment_policies p JOIN announcement_source_classification_rule_releases r ON r.id=p.rule_release_id
    WHERE p.id=target_policy OR p.policy_status_code IN ('ACTIVE','RETIRED')
    UNION ALL
    SELECT 'SOURCE'::varchar,s.id,encode(digest(jsonb_build_array(s.provider_code,s.semantic_status_code,
        s.classification_row_version,s.attachment_row_version,s.attachment_policy_id,s.is_attachment_review_required,
        s.current_attachment_evaluation_id,
        (SELECT coalesce(jsonb_agg(l.id ORDER BY l.id),'[]'::jsonb) FROM announcement_source_links l WHERE l.source_id=s.id))::text,'sha256'),'hex')::varchar
    FROM announcement_source_snapshots s
    WHERE s.data_purpose_code='PRODUCTION' AND s.attachment_policy_id IS NOT NULL
    UNION ALL
    SELECT 'JOB'::varchar,j.id,encode(digest(jsonb_build_array(j.source_id,j.policy_id,j.rule_release_id,j.row_version,
        j.operation_code,j.job_status_code,j.application_status_code,j.rollback_status_code,j.rollback_action_id,
        j.expected_source_version,j.expected_attachment_version,j.set_id,j.reserved_download_bytes)::text,'sha256'),'hex')::varchar
    FROM announcement_attachment_jobs j JOIN announcement_source_snapshots s ON s.id=j.source_id
    WHERE s.data_purpose_code='PRODUCTION' AND (
        (j.operation_code IN ('COLLECT','RETRY_FILES') AND j.job_status_code IN ('SCOPE_READY','PENDING','RUNNING','RETRY_WAIT','PAUSED'))
        OR j.application_status_code='PENDING' OR (j.rollback_action_id IS NOT NULL AND j.rollback_status_code='NOT_REQUESTED'))
    UNION ALL
    SELECT 'COLLECTION_PLAN'::varchar,p.run_id,encode(digest(jsonb_build_array(p.policy_id,p.rule_release_id,p.plan_status_code)::text,'sha256'),'hex')::varchar
    FROM announcement_attachment_collection_plans p
    JOIN announcement_source_collection_runs r ON r.id=p.run_id
    JOIN announcement_source_collection_requests q ON q.id=r.request_id
    WHERE p.plan_status_code='FROZEN' AND q.data_purpose_code='PRODUCTION'
    UNION ALL
    SELECT 'COLLECTOR'::varchar,s.id,encode(digest(jsonb_build_array(s.public_code,s.parser_profile_code,s.notice_url,
        to_jsonb(p)-'created_at'-'updated_at'-'created_by'-'updated_by')::text,'sha256'),'hex')::varchar
    FROM local_government_notice_sources s
    LEFT JOIN local_government_notice_parser_profiles p ON p.profile_code=s.parser_profile_code
    WHERE s.is_enabled AND s.deleted_at IS NULL
$$;

-- 정렬 순서와 같은 건수의 대상 교체·상태 변경을 구분한다. 시각·요청 키는 scope hash에 넣지 않는다.
-- JSONB 직렬화와 hash 계산을 이 DB 함수로 통일하고 클라이언트 관측 hash를 재사용하지 않는다.
CREATE FUNCTION attachment_policy_publication_scope_hash(scope_key uuid) RETURNS varchar LANGUAGE sql STABLE AS $$
    SELECT encode(digest(jsonb_build_array('attachment-policy-publication-scope-v1',p.policy_id,p.policy_row_version,
        p.rule_release_id,p.rule_row_version,p.mode_code,p.qa_run_id,p.qa_snapshot_hash,
        coalesce((SELECT jsonb_agg(jsonb_build_array(i.entity_type_code,i.entity_id,i.state_hash)
            ORDER BY i.entity_type_code COLLATE "C",i.entity_id)
            FROM announcement_attachment_policy_publication_scope_items i WHERE i.scope_id=p.id),'[]'::jsonb))::text,'sha256'),'hex')::varchar
    FROM announcement_attachment_policy_publication_scopes p WHERE p.id=scope_key
$$;

CREATE FUNCTION protect_attachment_policy_publication_scope() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        RAISE EXCEPTION 'publication scope history is immutable' USING ERRCODE='23514';
    END IF;
    IF TG_OP='INSERT' THEN
        IF NEW.scope_status_code<>'OPEN' OR NEW.created_xid<>pg_current_xact_id()
            OR NEW.created_at<clock_timestamp()-interval '1 minute' OR NEW.created_at>clock_timestamp()
            OR NOT EXISTS (SELECT 1 FROM announcement_attachment_policies p
                JOIN announcement_source_classification_rule_releases r ON r.id=p.rule_release_id
                WHERE p.id=NEW.policy_id AND p.policy_status_code='DRAFT' AND p.row_version=NEW.policy_row_version
                    AND p.rule_release_id=NEW.rule_release_id AND r.row_version=NEW.rule_row_version
                    AND r.release_status_code='ACTIVE' AND p.mode_code=NEW.mode_code)
            OR (NEW.qa_run_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM announcement_attachment_policy_validation_runs v
                WHERE v.id=NEW.qa_run_id AND v.policy_id=NEW.policy_id AND v.snapshot_hash=NEW.qa_snapshot_hash
                    AND v.policy_row_version=NEW.policy_row_version AND v.rule_release_id=NEW.rule_release_id
                    AND v.rule_row_version=NEW.rule_row_version)) THEN
            RAISE EXCEPTION 'publication scope requires current draft and exact optional QA binding' USING ERRCODE='23514';
        END IF;
        RETURN NEW;
    END IF;
    IF OLD.created_xid<>pg_current_xact_id() OR OLD.scope_status_code<>'OPEN' OR NEW.scope_status_code<>'SEALED'
        OR (to_jsonb(NEW)-'scope_status_code'-'item_count'-'scope_hash') IS DISTINCT FROM
           (to_jsonb(OLD)-'scope_status_code'-'item_count'-'scope_hash')
        OR NEW.item_count<>(SELECT count(1) FROM announcement_attachment_policy_publication_scope_items WHERE scope_id=OLD.id)
        OR NEW.scope_hash IS DISTINCT FROM attachment_policy_publication_scope_hash(OLD.id) THEN
        RAISE EXCEPTION 'publication scope can only seal its full membership once in creation transaction' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_publication_scope_immutable BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_policy_publication_scopes
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_policy_publication_scope();

CREATE FUNCTION protect_attachment_policy_publication_scope_item() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent record;
BEGIN
    IF TG_OP<>'INSERT' THEN
        RAISE EXCEPTION 'publication scope members are immutable' USING ERRCODE='23514';
    END IF;
    SELECT scope_status_code,created_xid INTO parent FROM announcement_attachment_policy_publication_scopes WHERE id=NEW.scope_id;
    IF parent IS NULL OR parent.scope_status_code<>'OPEN' OR parent.created_xid<>pg_current_xact_id() THEN
        RAISE EXCEPTION 'publication scope members belong to the same open creation transaction' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_publication_scope_item_immutable BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_policy_publication_scope_items
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_policy_publication_scope_item();

-- 생성 transaction의 마지막 snapshot에서 양방향 차집합을 검사한다. 생략·다른 ID·같은 수의 교체를 거부한다.
-- 후속 게시 transaction의 최신성/동시 쓰기 잠금 검증을 대신하지 않는다.
CREATE FUNCTION check_attachment_policy_publication_scope() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE scope_row record;
BEGIN
    SELECT id,policy_id,policy_row_version,rule_release_id,rule_row_version,mode_code,scope_status_code,item_count,scope_hash INTO scope_row
        FROM announcement_attachment_policy_publication_scopes WHERE id=NEW.id;
    IF scope_row IS NULL OR scope_row.scope_status_code<>'SEALED'
        OR NOT EXISTS (SELECT 1 FROM announcement_attachment_policies p
            JOIN announcement_source_classification_rule_releases r ON r.id=p.rule_release_id
            WHERE p.id=scope_row.policy_id AND p.policy_status_code='DRAFT' AND p.row_version=scope_row.policy_row_version
                AND p.rule_release_id=scope_row.rule_release_id AND r.row_version=scope_row.rule_row_version
                AND r.release_status_code='ACTIVE' AND p.mode_code=scope_row.mode_code)
        OR scope_row.item_count<>(SELECT count(1) FROM announcement_attachment_policy_publication_scope_items WHERE scope_id=NEW.id)
        OR scope_row.scope_hash IS DISTINCT FROM attachment_policy_publication_scope_hash(NEW.id)
        OR EXISTS ((SELECT m.entity_type_code,m.entity_id,m.state_hash FROM attachment_policy_publication_members(scope_row.policy_id) m)
            EXCEPT (SELECT i.entity_type_code,i.entity_id,i.state_hash FROM announcement_attachment_policy_publication_scope_items i WHERE i.scope_id=NEW.id))
        OR EXISTS ((SELECT i.entity_type_code,i.entity_id,i.state_hash FROM announcement_attachment_policy_publication_scope_items i WHERE i.scope_id=NEW.id)
            EXCEPT (SELECT m.entity_type_code,m.entity_id,m.state_hash FROM attachment_policy_publication_members(scope_row.policy_id) m)) THEN
        RAISE EXCEPTION 'publication scope must seal every exact current member and state hash' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_publication_scope_complete AFTER INSERT OR UPDATE ON announcement_attachment_policy_publication_scopes
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_policy_publication_scope();
