-- 기존 일반 배치의 연결 원문 제외를 유지하고, 명시 목적의 고정 연결만 예외로 허용한다.
-- ACTIVE/OFF·lease·제목·버전·quota·관리자 실행 승인 조건은 호출 경로에 그대로 남는다.
CREATE OR REPLACE FUNCTION attachment_batch_job_input_unchanged(job_key uuid) RETURNS boolean LANGUAGE sql STABLE AS $$
    SELECT coalesce((SELECT b.scope_item_count IS NULL OR (
        j.frozen_locator_hash=attachment_source_locator_hash(j.source_id)
        AND s.current_attachment_evaluation_id IS NOT DISTINCT FROM j.previous_attachment_evaluation_id
        AND s.attachment_policy_id IS NOT DISTINCT FROM j.previous_policy_id
        AND s.is_attachment_review_required IS NOT DISTINCT FROM j.previous_is_review_required
        AND (SELECT c.id FROM announcement_source_attachment_confirmations c WHERE c.source_id=j.source_id AND c.is_current)
            IS NOT DISTINCT FROM j.previous_confirmation_id
        AND CASE WHEN b.purpose_code='LINKED_EVIDENCE_ONLY' THEN attachment_linked_job_connections_unchanged(j.id)
            ELSE NOT EXISTS (SELECT 1 FROM announcement_source_links l WHERE l.source_id=j.source_id) END
        AND attachment_backfill_batch_input_unchanged(j.id))
        FROM announcement_attachment_jobs j JOIN announcement_source_snapshots s ON s.id=j.source_id
        LEFT JOIN announcement_attachment_batches b ON b.id=j.batch_id WHERE j.id=job_key),false)
$$;
