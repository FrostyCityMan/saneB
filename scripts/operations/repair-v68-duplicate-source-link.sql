-- V68 적용을 차단하는 동일·미사용 DRAFT의 후속 source link 한 행만 분리합니다.
-- 공고 자체는 삭제하지 않으며, 복구에 필요한 link metadata는 audit_logs에 기록합니다.
BEGIN;

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

LOCK TABLE announcement_source_links IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    duplicate_source_count bigint;
    source_link_count bigint;
    target_source_id uuid;
    retained_link announcement_source_links%ROWTYPE;
    removed_link announcement_source_links%ROWTYPE;
    retained_announcement announcements%ROWTYPE;
    removed_announcement announcements%ROWTYPE;
    source_review_status varchar(30);
    usage_count bigint;
    retained_config_count bigint;
    removed_config_count bigint;
    retained_target_count bigint;
    removed_target_count bigint;
    target_mismatch_count bigint;
    retained_create_audit_count bigint;
    removed_create_audit_count bigint;
    deleted_count bigint;
    remaining_duplicate_count bigint;
BEGIN
    SELECT count(1)
    INTO duplicate_source_count
    FROM (
        SELECT source_id
        FROM announcement_source_links
        GROUP BY source_id
        HAVING count(1) > 1
    ) duplicated_sources;

    IF duplicate_source_count <> 1 THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_DUPLICATE_SOURCE_COUNT';
    END IF;

    SELECT source_id
    INTO target_source_id
    FROM announcement_source_links
    GROUP BY source_id
    HAVING count(1) > 1;

    SELECT count(1)
    INTO source_link_count
    FROM announcement_source_links
    WHERE source_id = target_source_id;

    IF source_link_count <> 2 THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_SOURCE_LINK_COUNT';
    END IF;

    SELECT *
    INTO retained_link
    FROM announcement_source_links
    WHERE source_id = target_source_id
    ORDER BY linked_at ASC, id ASC
    LIMIT 1;

    SELECT *
    INTO removed_link
    FROM announcement_source_links
    WHERE source_id = target_source_id
    ORDER BY linked_at DESC, id DESC
    LIMIT 1;

    IF retained_link.id = removed_link.id THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_DISTINCT_LINKS';
    END IF;

    SELECT review_status_code
    INTO source_review_status
    FROM announcement_source_snapshots
    WHERE id = target_source_id
    FOR UPDATE;

    IF source_review_status IS DISTINCT FROM 'CONDITION_INPUT_REQUIRED' THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_SOURCE_STATUS';
    END IF;

    SELECT *
    INTO retained_announcement
    FROM announcements
    WHERE id = retained_link.announcement_id
    FOR UPDATE;

    SELECT *
    INTO removed_announcement
    FROM announcements
    WHERE id = removed_link.announcement_id
    FOR UPDATE;

    IF retained_announcement.id IS NULL
            OR removed_announcement.id IS NULL
            OR retained_announcement.approval_status_code <> 'DRAFT'
            OR removed_announcement.approval_status_code <> 'DRAFT'
            OR retained_announcement.manual_status_code <> 'NORMAL'
            OR removed_announcement.manual_status_code <> 'NORMAL' THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_ANNOUNCEMENT_STATUS';
    END IF;

    IF retained_announcement.title IS DISTINCT FROM removed_announcement.title
            OR retained_announcement.agency_name IS DISTINCT FROM removed_announcement.agency_name
            OR retained_announcement.target_type_code IS DISTINCT FROM removed_announcement.target_type_code
            OR retained_announcement.summary IS DISTINCT FROM removed_announcement.summary
            OR retained_announcement.application_start_date IS DISTINCT FROM removed_announcement.application_start_date
            OR retained_announcement.application_end_date IS DISTINCT FROM removed_announcement.application_end_date
            OR retained_announcement.income_judgement_code IS DISTINCT FROM removed_announcement.income_judgement_code
            OR retained_announcement.min_amount IS DISTINCT FROM removed_announcement.min_amount
            OR retained_announcement.max_amount IS DISTINCT FROM removed_announcement.max_amount THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_ANNOUNCEMENT_CONTENT';
    END IF;

    IF retained_announcement.updated_at > retained_announcement.created_at + interval '1 second'
            OR removed_announcement.updated_at > removed_announcement.created_at + interval '1 second' THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_ANNOUNCEMENT_EDITED';
    END IF;

    SELECT
        (SELECT count(1) FROM matching_cases WHERE announcement_id IN (
            retained_link.announcement_id, removed_link.announcement_id
        ))
        + (SELECT count(1) FROM application_progresses WHERE announcement_id IN (
            retained_link.announcement_id, removed_link.announcement_id
        ))
    INTO usage_count;

    IF usage_count <> 0 THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_ANNOUNCEMENT_IN_USE';
    END IF;

    SELECT
        (SELECT count(1) FROM announcement_options WHERE announcement_id = retained_link.announcement_id)
        + (SELECT count(1) FROM announcement_approval_requests WHERE announcement_id = retained_link.announcement_id)
        + (SELECT count(1) FROM announcement_status_histories WHERE announcement_id = retained_link.announcement_id)
        + (SELECT count(1) FROM announcement_industry_conditions WHERE announcement_id = retained_link.announcement_id)
        + (SELECT count(1) FROM announcement_numeric_conditions WHERE announcement_id = retained_link.announcement_id)
        + (SELECT count(1) FROM announcement_option_conditions WHERE announcement_id = retained_link.announcement_id)
        + (SELECT count(1) FROM announcement_document_requirements WHERE announcement_id = retained_link.announcement_id)
        + (SELECT count(1) FROM announcement_progress_steps WHERE announcement_id = retained_link.announcement_id)
        + (SELECT count(1) FROM announcement_input_requirements WHERE announcement_id = retained_link.announcement_id)
        + (SELECT count(1) FROM announcement_target_category_assignments WHERE announcement_id = retained_link.announcement_id)
        + (SELECT count(1) FROM announcement_support_type_assignments WHERE announcement_id = retained_link.announcement_id)
    INTO retained_config_count;

    SELECT
        (SELECT count(1) FROM announcement_options WHERE announcement_id = removed_link.announcement_id)
        + (SELECT count(1) FROM announcement_approval_requests WHERE announcement_id = removed_link.announcement_id)
        + (SELECT count(1) FROM announcement_status_histories WHERE announcement_id = removed_link.announcement_id)
        + (SELECT count(1) FROM announcement_industry_conditions WHERE announcement_id = removed_link.announcement_id)
        + (SELECT count(1) FROM announcement_numeric_conditions WHERE announcement_id = removed_link.announcement_id)
        + (SELECT count(1) FROM announcement_option_conditions WHERE announcement_id = removed_link.announcement_id)
        + (SELECT count(1) FROM announcement_document_requirements WHERE announcement_id = removed_link.announcement_id)
        + (SELECT count(1) FROM announcement_progress_steps WHERE announcement_id = removed_link.announcement_id)
        + (SELECT count(1) FROM announcement_input_requirements WHERE announcement_id = removed_link.announcement_id)
        + (SELECT count(1) FROM announcement_target_category_assignments WHERE announcement_id = removed_link.announcement_id)
        + (SELECT count(1) FROM announcement_support_type_assignments WHERE announcement_id = removed_link.announcement_id)
    INTO removed_config_count;

    IF retained_config_count <> 1 OR removed_config_count <> 1 THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_CONFIG_COUNT';
    END IF;

    SELECT count(1)
    INTO retained_target_count
    FROM announcement_target_category_assignments
    WHERE announcement_id = retained_link.announcement_id;

    SELECT count(1)
    INTO removed_target_count
    FROM announcement_target_category_assignments
    WHERE announcement_id = removed_link.announcement_id;

    IF retained_target_count <> 1 OR removed_target_count <> 1 THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_TARGET_COUNT';
    END IF;

    SELECT count(1)
    INTO target_mismatch_count
    FROM (
        ((
            SELECT target_category_id, is_primary, assignment_source_code
            FROM announcement_target_category_assignments
            WHERE announcement_id = retained_link.announcement_id
        ) EXCEPT ALL (
            SELECT target_category_id, is_primary, assignment_source_code
            FROM announcement_target_category_assignments
            WHERE announcement_id = removed_link.announcement_id
        ))
        UNION ALL
        ((
            SELECT target_category_id, is_primary, assignment_source_code
            FROM announcement_target_category_assignments
            WHERE announcement_id = removed_link.announcement_id
        ) EXCEPT ALL (
            SELECT target_category_id, is_primary, assignment_source_code
            FROM announcement_target_category_assignments
            WHERE announcement_id = retained_link.announcement_id
        ))
    ) target_differences;

    IF target_mismatch_count <> 0 THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_TARGET_VALUE';
    END IF;

    SELECT count(1)
    INTO retained_create_audit_count
    FROM audit_logs
    WHERE resource_type = 'ANNOUNCEMENT_SOURCE'
      AND resource_id = target_source_id
      AND action_code IN ('ANNOUNCEMENT_SOURCE_DRAFT_CREATE', 'ANNOUNCEMENT_SOURCE_V2_DRAFT_CREATE')
      AND metadata_json ->> 'announcementId' = retained_link.announcement_id::text;

    SELECT count(1)
    INTO removed_create_audit_count
    FROM audit_logs
    WHERE resource_type = 'ANNOUNCEMENT_SOURCE'
      AND resource_id = target_source_id
      AND action_code IN ('ANNOUNCEMENT_SOURCE_DRAFT_CREATE', 'ANNOUNCEMENT_SOURCE_V2_DRAFT_CREATE')
      AND metadata_json ->> 'announcementId' = removed_link.announcement_id::text;

    IF retained_create_audit_count <> 1 OR removed_create_audit_count <> 1 THEN
        RAISE EXCEPTION 'REPAIR_PRECONDITION_CREATE_AUDIT';
    END IF;

    DELETE FROM announcement_source_links
    WHERE id = removed_link.id
      AND source_id = target_source_id;

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    IF deleted_count <> 1 THEN
        RAISE EXCEPTION 'REPAIR_DELETE_COUNT';
    END IF;

    INSERT INTO audit_logs (
        actor_user_id,
        action_code,
        resource_type,
        resource_id,
        result_code,
        metadata_json
    ) VALUES (
        NULL,
        'ANNOUNCEMENT_SOURCE_LINK_V68_REPAIR',
        'ANNOUNCEMENT_SOURCE',
        target_source_id,
        'SUCCESS',
        jsonb_build_object(
            'repairCode', 'V68_DUPLICATE_SOURCE_LINK_REPAIR',
            'reasonCode', 'IDENTICAL_UNUSED_DRAFT_LINK',
            'retainedLinkId', retained_link.id,
            'retainedAnnouncementId', retained_link.announcement_id,
            'removedLinkId', removed_link.id,
            'removedAnnouncementId', removed_link.announcement_id,
            'removedLinkedBy', removed_link.linked_by,
            'removedLinkedAt', removed_link.linked_at
        )
    );

    SELECT count(1)
    INTO remaining_duplicate_count
    FROM (
        SELECT source_id
        FROM announcement_source_links
        GROUP BY source_id
        HAVING count(1) > 1
    ) duplicated_sources;

    IF remaining_duplicate_count <> 0 THEN
        RAISE EXCEPTION 'REPAIR_POSTCONDITION_DUPLICATE_REMAINS';
    END IF;
END
$$;

COMMIT;

SELECT 'REPAIR_STATUS=SUCCESS';
SELECT 'DUPLICATE_SOURCE_COUNT_AFTER=' || count(1)
FROM (
    SELECT source_id
    FROM announcement_source_links
    GROUP BY source_id
    HAVING count(1) > 1
) duplicated_sources;
SELECT 'REPAIR_AUDIT_COUNT=' || count(1)
FROM audit_logs
WHERE action_code = 'ANNOUNCEMENT_SOURCE_LINK_V68_REPAIR'
  AND result_code = 'SUCCESS';
