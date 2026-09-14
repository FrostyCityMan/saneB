-- V75의 양방향 상관 소속 탐색을 정렬된 전체 tuple 비교로 바꾼다.
-- 기존 지연 trigger/행 수/삭제 분모/원자적 receipt 검증을 그대로 유지하며 원문·이력을 수정하지 않는다.
CREATE OR REPLACE FUNCTION check_attachment_backfill_segment_batch() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE batch_key uuid; binding record; batch_scope jsonb;
BEGIN
    IF TG_TABLE_NAME='announcement_attachment_batches' THEN batch_key:=NEW.id;
    ELSIF TG_OP='DELETE' THEN batch_key:=OLD.batch_id;
    ELSE batch_key:=NEW.batch_id; END IF;
    IF batch_key IS NULL THEN RETURN NULL; END IF;
    SELECT scope_json INTO batch_scope FROM announcement_attachment_batches WHERE id=batch_key;
    SELECT l.run_id,l.segment_no,l.deleted_before_reservation,s.item_count,s.deleted_item_count,
        b.scope_item_count,b.deleted_item_count AS batch_deleted_count,r.policy_id,r.policy_snapshot_json
        INTO binding FROM announcement_attachment_backfill_segment_batches l
        JOIN announcement_attachment_backfill_segments s ON s.run_id=l.run_id AND s.segment_no=l.segment_no
        JOIN announcement_attachment_backfill_runs r ON r.id=l.run_id
        JOIN announcement_attachment_batches b ON b.id=l.batch_id WHERE l.batch_id=batch_key;
    IF binding IS NULL THEN
        IF batch_scope ? 'backfillRunId' OR batch_scope ? 'backfillSegmentNo' THEN
            RAISE EXCEPTION 'backfill marked batch requires its atomic segment receipt' USING ERRCODE='23514';
        END IF;
        RETURN NULL;
    END IF;
    IF binding.scope_item_count+binding.deleted_before_reservation<>binding.item_count
        OR binding.batch_deleted_count+binding.deleted_before_reservation<>binding.deleted_item_count
        OR (SELECT count(1) FROM announcement_attachment_jobs WHERE batch_id=batch_key)<>binding.item_count-binding.deleted_item_count
        -- 다섯 필드의 전체 값을 비교한다. 해시 충돌·중복 제거·NULL 생략으로 불일치를 감추지 않는다.
        OR coalesce((SELECT jsonb_agg(jsonb_build_array(j.source_id,j.content_version_id,j.base_evaluation_id,
                    j.rule_release_id,j.frozen_provider_code)
                ORDER BY j.source_id,j.content_version_id,j.base_evaluation_id,j.rule_release_id,j.frozen_provider_code)
            FROM announcement_attachment_jobs j WHERE j.batch_id=batch_key),'[]'::jsonb)
            IS DISTINCT FROM
            coalesce((SELECT jsonb_agg(jsonb_build_array(i.source_id,i.content_version_id,i.base_evaluation_id,
                    i.rule_release_id,i.provider_code)
                ORDER BY i.source_id,i.content_version_id,i.base_evaluation_id,i.rule_release_id,i.provider_code)
            FROM announcement_attachment_backfill_items i
            WHERE i.run_id=binding.run_id AND i.segment_no=binding.segment_no),'[]'::jsonb) THEN
        RAISE EXCEPTION 'backfill segment jobs and original deletion denominators must match exactly' USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END $$;
