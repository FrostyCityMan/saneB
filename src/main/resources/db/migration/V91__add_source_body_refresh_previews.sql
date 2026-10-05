-- 본문 복구 미리보기는 원문 저장소에 보관한다. 감사 로그에는 본문을 복사하지 않는다.
CREATE TABLE announcement_source_body_refresh_previews (
    id uuid PRIMARY KEY,
    source_id uuid NOT NULL REFERENCES announcement_source_snapshots(id) ON DELETE CASCADE,
    requested_by uuid NOT NULL REFERENCES users(id),
    base_evaluation_id uuid NOT NULL,
    content_version_id uuid NOT NULL,
    rule_release_id uuid NOT NULL REFERENCES announcement_source_classification_rule_releases(id),
    source_version integer NOT NULL CHECK (source_version >= 0),
    attachment_version integer NOT NULL CHECK (attachment_version >= 0),
    body_text text NOT NULL CHECK (length(body_text) BETWEEN 1 AND 2000000),
    body_hash varchar(64) NOT NULL CHECK (body_hash ~ '^[0-9a-f]{64}$'),
    extractor_version varchar(40) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    expires_at timestamptz NOT NULL DEFAULT (clock_timestamp() + interval '30 minutes'),
    applied_evaluation_id uuid,
    applied_at timestamptz,
    FOREIGN KEY (base_evaluation_id,source_id) REFERENCES announcement_source_classification_evaluations(id,source_id) ON DELETE CASCADE,
    FOREIGN KEY (content_version_id,source_id) REFERENCES announcement_source_content_versions(id,source_id) ON DELETE CASCADE,
    FOREIGN KEY (applied_evaluation_id,source_id) REFERENCES announcement_source_classification_evaluations(id,source_id) ON DELETE CASCADE,
    CHECK (expires_at > created_at),
    CHECK ((applied_evaluation_id IS NULL) = (applied_at IS NULL))
);
CREATE INDEX ix_source_body_refresh_source ON announcement_source_body_refresh_previews(source_id,created_at DESC);
COMMENT ON TABLE announcement_source_body_refresh_previews IS '승인 전 본문 복구 근거. 적용 시 판정 버전 충돌 검사, 기존 이력 보존';

-- 미리보기 본문·요청자·버전은 수정하지 못한다. 완료 영수증만 한 번 기록한다.
CREATE FUNCTION guard_source_body_refresh_preview() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (to_jsonb(NEW)-'applied_evaluation_id'-'applied_at') IS DISTINCT FROM
       (to_jsonb(OLD)-'applied_evaluation_id'-'applied_at')
       OR OLD.applied_evaluation_id IS NOT NULL
       OR NEW.applied_evaluation_id IS NULL OR NEW.applied_at IS NULL THEN
        RAISE EXCEPTION 'BODY_REFRESH_PREVIEW_IMMUTABLE';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_source_body_refresh_preview BEFORE UPDATE ON announcement_source_body_refresh_previews
FOR EACH ROW EXECUTE FUNCTION guard_source_body_refresh_preview();
