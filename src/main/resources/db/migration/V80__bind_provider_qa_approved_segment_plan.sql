-- 새 Provider QA run의 전체 계획/분할 승인 metadata를 기존 원장과 같은 transaction에 묶는다.
CREATE TABLE announcement_attachment_provider_qa_run_plans (
    run_id uuid PRIMARY KEY REFERENCES announcement_attachment_provider_qa_runs(id),
    plan_hash varchar(64) NOT NULL CHECK (plan_hash ~ '^[0-9a-f]{64}$'),
    segment_no integer NOT NULL CHECK (segment_no>=1),
    segment_count integer NOT NULL CHECK (segment_count BETWEEN 1 AND 10000 AND segment_no<=segment_count),
    catalog_case_count integer NOT NULL CHECK (catalog_case_count BETWEEN 1 AND 10000),
    executable_case_count integer NOT NULL CHECK (executable_case_count BETWEEN 1 AND catalog_case_count),
    is_expectation_coverage_complete boolean NOT NULL,
    maximum_seconds_including_margin integer NOT NULL CHECK (maximum_seconds_including_margin BETWEEN 61 AND 82800),
    created_xid xid8 NOT NULL DEFAULT pg_current_xact_id(),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);

CREATE FUNCTION protect_attachment_provider_run_plan() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'provider QA approved plan is immutable' USING ERRCODE='23514'; END IF;
    IF NEW.created_xid<>pg_current_xact_id() OR NOT EXISTS (
        SELECT 1 FROM announcement_attachment_provider_qa_runs r WHERE r.id=NEW.run_id AND r.run_status_code='BUILDING'
            AND r.created_xid=pg_current_xact_id() AND r.expected_case_count<=NEW.executable_case_count) THEN
        RAISE EXCEPTION 'provider QA plan requires the same frozen preparation' USING ERRCODE='23514';
    END IF;
    NEW.created_at:=clock_timestamp();RETURN NEW;
END $$;
CREATE TRIGGER tr_att_provider_run_plan BEFORE INSERT OR UPDATE OR DELETE ON announcement_attachment_provider_qa_run_plans
    FOR EACH ROW EXECUTE FUNCTION protect_attachment_provider_run_plan();

CREATE FUNCTION require_attachment_provider_run_plan() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM announcement_attachment_provider_qa_run_plans p
        JOIN announcement_attachment_provider_qa_runs r ON r.id=p.run_id
        WHERE p.run_id=NEW.id AND p.created_xid=r.created_xid AND r.run_status_code<>'BUILDING'
          AND p.executable_case_count>=r.expected_case_count
          AND p.maximum_seconds_including_margin=(
              SELECT sum(c.maximum_seconds+60) FROM announcement_attachment_provider_qa_cases c WHERE c.run_id=r.id)) THEN
        RAISE EXCEPTION 'provider QA run requires its complete approved segment plan' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
-- 이전 이력은 갱신하지 않는다. 이 migration 이후 생성되는 run부터 승인 계획을 필수로 검사한다.
CREATE CONSTRAINT TRIGGER tr_att_provider_run_plan_required AFTER INSERT ON announcement_attachment_provider_qa_runs
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION require_attachment_provider_run_plan();
