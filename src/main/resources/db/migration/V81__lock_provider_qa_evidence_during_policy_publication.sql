-- 게시 직전 재검증과 commit 사이에 새 Provider QA 시도·결과가 끼어들지 못하게 한다.
-- V77의 기존 잠금 순서·NOWAIT를 유지하며 기존 migration과 운영 데이터를 변경하지 않는다.
CREATE OR REPLACE FUNCTION attachment_policy_publication_lock() RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    LOCK TABLE announcement_attachment_policies,announcement_attachment_policy_validation_runs,
        announcement_attachment_policy_validation_steps,announcement_attachment_policy_publication_scopes,
        announcement_attachment_policy_publication_scope_items,announcement_attachment_policy_publications,
        announcement_source_classification_rule_releases,announcement_source_classification_rule_groups,
        announcement_source_classification_keyword_rules,announcement_source_classification_keyword_terms,
        announcement_source_snapshots,announcement_source_links,announcement_attachment_jobs,
        announcement_attachment_collection_plans,announcement_source_collection_runs,announcement_source_collection_requests,
        local_government_notice_sources,local_government_notice_parser_profiles,
        announcement_attachment_provider_qa_runs,announcement_attachment_provider_qa_cases,
        announcement_attachment_provider_qa_run_plans IN EXCLUSIVE MODE NOWAIT;
END $$;
