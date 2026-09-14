-- 검증된 준비 범위의 실제 정책 교체 영수증. 이 migration 자체는 어떤 정책도 게시하지 않는다.
CREATE TABLE announcement_attachment_policy_publications (
    id uuid PRIMARY KEY,
    scope_id uuid NOT NULL UNIQUE REFERENCES announcement_attachment_policy_publication_scopes(id),
    policy_id uuid NOT NULL UNIQUE REFERENCES announcement_attachment_policies(id),
    policy_row_version integer NOT NULL CHECK (policy_row_version>0),
    policy_hash varchar(64) NOT NULL CHECK (policy_hash ~ '^[0-9a-f]{64}$'),
    runtime_hash varchar(64) NOT NULL CHECK (runtime_hash ~ '^[0-9a-f]{64}$'),
    previous_policy_id uuid REFERENCES announcement_attachment_policies(id),
    previous_policy_row_version integer CHECK (previous_policy_row_version>=0),
    qa_run_id uuid NOT NULL REFERENCES announcement_attachment_policy_validation_runs(id),
    evidence_hash varchar(64) NOT NULL CHECK (evidence_hash ~ '^[0-9a-f]{64}$'),
    published_by uuid NOT NULL REFERENCES users(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    reason_hash varchar(64) NOT NULL CHECK (reason_hash ~ '^[0-9a-f]{64}$'),
    created_xid xid8 NOT NULL DEFAULT pg_current_xact_id(),
    published_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CHECK ((previous_policy_id IS NULL)=(previous_policy_row_version IS NULL)),
    CHECK (previous_policy_id IS NULL OR previous_policy_id<>policy_id)
);
CREATE INDEX ix_att_publication_previous ON announcement_attachment_policy_publications(previous_policy_id);
CREATE INDEX ix_att_publication_qa ON announcement_attachment_policy_publications(qa_run_id);
CREATE INDEX ix_att_publication_actor ON announcement_attachment_policy_publications(published_by);

-- 전역 영향의 membership 추가/삭제와 규칙·QA 변경을 함께 차단한다. 대기/자동 재시도하지 않는다.
-- EXCLUSIVE는 일반 SELECT를 허용하되 DML과 SELECT FOR UPDATE/SHARE를 막는다.
CREATE FUNCTION attachment_policy_publication_lock() RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    LOCK TABLE announcement_attachment_policies,announcement_attachment_policy_validation_runs,
        announcement_attachment_policy_validation_steps,announcement_attachment_policy_publication_scopes,
        announcement_attachment_policy_publication_scope_items,announcement_attachment_policy_publications,
        announcement_source_classification_rule_releases,announcement_source_classification_rule_groups,
        announcement_source_classification_keyword_rules,announcement_source_classification_keyword_terms,
        announcement_source_snapshots,announcement_source_links,announcement_attachment_jobs,
        announcement_attachment_collection_plans,announcement_source_collection_runs,announcement_source_collection_requests,
        local_government_notice_sources,local_government_notice_parser_profiles IN EXCLUSIVE MODE NOWAIT;
END $$;

CREATE FUNCTION protect_attachment_policy_publication() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE approved record;
BEGIN
    IF TG_OP<>'INSERT' THEN
        RAISE EXCEPTION 'policy publication receipt is immutable' USING ERRCODE='23514';
    END IF;
    -- 호출자가 먼저 잡은 짧은 잠금을 재확인한다. 직접 INSERT도 같은 경계를 통과해야 한다.
    PERFORM attachment_policy_publication_lock();
    SELECT s.id,s.policy_id,s.policy_row_version,s.rule_release_id,s.rule_row_version,s.mode_code,s.qa_run_id,s.qa_snapshot_hash,
        v.input_snapshot_json->'installed'->>'runtimeHash' AS runtime_hash
      INTO approved FROM announcement_attachment_policy_publication_scopes s
      JOIN announcement_attachment_policies p ON p.id=s.policy_id
      JOIN announcement_source_classification_rule_releases r ON r.id=s.rule_release_id
      JOIN announcement_attachment_policy_validation_runs v ON v.id=s.qa_run_id AND v.policy_id=s.policy_id
      WHERE s.id=NEW.scope_id AND s.policy_id=NEW.policy_id AND s.requested_by=NEW.published_by
        AND s.scope_status_code='SEALED' AND s.expires_at>clock_timestamp()
        AND p.policy_status_code='DRAFT' AND p.row_version=s.policy_row_version AND p.mode_code=s.mode_code
        AND p.rule_release_id=s.rule_release_id AND r.row_version=s.rule_row_version AND r.release_status_code='ACTIVE'
        AND v.run_status_code='VERIFIED' AND v.snapshot_hash=s.qa_snapshot_hash
        AND v.policy_row_version=s.policy_row_version AND v.rule_row_version=s.rule_row_version AND v.rule_release_id=s.rule_release_id
        AND NOT EXISTS (SELECT 1 FROM announcement_attachment_policy_validation_runs newer WHERE newer.policy_id=s.policy_id
            AND (newer.created_at,newer.id)>(v.created_at,v.id))
        AND (SELECT count(1) FROM announcement_attachment_policy_validation_steps WHERE run_id=v.id AND status_code='PASSED')=4
        AND NOT EXISTS ((SELECT m.entity_type_code,m.entity_id,m.state_hash FROM attachment_policy_publication_members(s.policy_id) m)
            EXCEPT (SELECT i.entity_type_code,i.entity_id,i.state_hash FROM announcement_attachment_policy_publication_scope_items i WHERE i.scope_id=s.id))
        AND NOT EXISTS ((SELECT i.entity_type_code,i.entity_id,i.state_hash FROM announcement_attachment_policy_publication_scope_items i WHERE i.scope_id=s.id)
            EXCEPT (SELECT m.entity_type_code,m.entity_id,m.state_hash FROM attachment_policy_publication_members(s.policy_id) m));
    IF approved IS NULL OR NEW.created_xid<>pg_current_xact_id() OR NEW.policy_row_version<>approved.policy_row_version+1
        OR NEW.qa_run_id<>approved.qa_run_id OR NEW.policy_hash<>approved.qa_snapshot_hash
        OR NEW.runtime_hash IS DISTINCT FROM approved.runtime_hash
        OR NEW.previous_policy_id IS DISTINCT FROM (SELECT id FROM announcement_attachment_policies WHERE rule_release_id=approved.rule_release_id AND policy_status_code='ACTIVE')
        OR NEW.previous_policy_row_version IS DISTINCT FROM (SELECT row_version FROM announcement_attachment_policies WHERE id=NEW.previous_policy_id) THEN
        RAISE EXCEPTION 'publication requires latest verified QA and exact current approved scope' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tr_att_publication_immutable BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_policy_publications
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_policy_publication();

-- 영수증만 남거나 정책 교체 일부만 commit되는 것을 거부한다. 기존 원문/작업의 판정은 바꾸지 않는다.
CREATE FUNCTION check_attachment_policy_publication_complete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM announcement_attachment_policies p
        JOIN announcement_attachment_policy_publication_scopes s ON s.id=NEW.scope_id
        JOIN announcement_attachment_policy_validation_runs v ON v.id=NEW.qa_run_id
        WHERE p.id=NEW.policy_id AND p.policy_status_code='ACTIVE' AND p.row_version=NEW.policy_row_version
            AND p.policy_hash=NEW.policy_hash AND p.published_at=NEW.published_at AND p.rule_release_id=s.rule_release_id AND p.mode_code=s.mode_code
            AND p.settings_json->>'extractorConfigHash'=NEW.runtime_hash
            AND p.settings_json-'extractorConfigHash'=(v.input_snapshot_json->'settings')-'extractorConfigHash')
        OR (NEW.previous_policy_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM announcement_attachment_policies p
            WHERE p.id=NEW.previous_policy_id AND p.policy_status_code='RETIRED' AND p.row_version=NEW.previous_policy_row_version+1)) THEN
        RAISE EXCEPTION 'publication and previous policy retirement must commit atomically' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER ct_att_publication_complete AFTER INSERT ON announcement_attachment_policy_publications
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_attachment_policy_publication_complete();
