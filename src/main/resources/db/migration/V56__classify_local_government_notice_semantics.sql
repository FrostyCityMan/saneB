-- Classify local-government source semantics and prevent unrelated notice collection.

ALTER TABLE local_government_notice_sources
    ADD COLUMN source_board_type_code varchar(40) NOT NULL DEFAULT 'UNVERIFIED',
    ADD COLUMN collection_policy_code varchar(30) NOT NULL DEFAULT 'EXCLUDED',
    ADD COLUMN is_semantically_verified boolean NOT NULL DEFAULT false,
    ADD COLUMN semantic_verified_at timestamptz,
    ADD COLUMN semantic_verified_by uuid,
    ADD COLUMN semantic_verification_note varchar(1000);

ALTER TABLE local_government_notice_sources
    ADD CONSTRAINT fk_local_government_notice_sources_semantic_verified_by
        FOREIGN KEY (semantic_verified_by) REFERENCES users (id),
    ADD CONSTRAINT ck_local_government_notice_sources_board_type
        CHECK (source_board_type_code IN (
            'LEGAL_NOTICE', 'SUPPORT_RECRUITMENT', 'GENERAL_NOTICE', 'PRESS_RELEASE', 'UNVERIFIED'
        )),
    ADD CONSTRAINT ck_local_government_notice_sources_collection_policy
        CHECK (collection_policy_code IN ('COLLECT_ALL', 'KEYWORD_FILTERED', 'EXCLUDED')),
    ADD CONSTRAINT ck_local_government_notice_sources_semantic_verification
        CHECK (
            (is_semantically_verified AND source_board_type_code <> 'UNVERIFIED' AND semantic_verified_at IS NOT NULL)
            OR
            (NOT is_semantically_verified AND source_board_type_code = 'UNVERIFIED')
        );

CREATE INDEX ix_local_government_notice_sources_semantic
    ON local_government_notice_sources (
        is_enabled, is_semantically_verified, collection_policy_code, source_board_type_code
    )
    WHERE deleted_at IS NULL;

CREATE TABLE announcement_source_semantic_keyword_rules (
    id uuid PRIMARY KEY,
    rule_code varchar(80) NOT NULL,
    rule_type_code varchar(20) NOT NULL,
    keyword_text varchar(100) NOT NULL,
    is_enabled boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_announcement_source_semantic_keyword_rules_code UNIQUE (rule_code),
    CONSTRAINT uq_announcement_source_semantic_keyword_rules_keyword UNIQUE (rule_type_code, keyword_text),
    CONSTRAINT ck_announcement_source_semantic_keyword_rules_type
        CHECK (rule_type_code IN ('INCLUDE', 'EXCLUDE')),
    CONSTRAINT ck_announcement_source_semantic_keyword_rules_sort CHECK (sort_order >= 0)
);

CREATE INDEX ix_announcement_source_semantic_keyword_rules_enabled
    ON announcement_source_semantic_keyword_rules (is_enabled, rule_type_code, sort_order);

ALTER TABLE local_government_notice_sources
    DROP CONSTRAINT ck_local_government_notice_sources_request_profile;
ALTER TABLE local_government_notice_sources
    ADD CONSTRAINT ck_local_government_notice_sources_request_profile CHECK (
        request_profile_code IN ('DEFAULT', 'BROWSER_HTTP1', 'LEGACY_BROWSER', 'SESSION_BROWSER')
    );

INSERT INTO announcement_source_semantic_keyword_rules (
    id, rule_code, rule_type_code, keyword_text, sort_order
) VALUES
    (md5('semantic-include-001')::uuid, 'INCLUDE_SUPPORT', 'INCLUDE', '지원', 10),
    (md5('semantic-include-002')::uuid, 'INCLUDE_RECRUITMENT', 'INCLUDE', '모집', 20),
    (md5('semantic-include-003')::uuid, 'INCLUDE_APPLICATION', 'INCLUDE', '신청', 30),
    (md5('semantic-include-004')::uuid, 'INCLUDE_RECEIPT', 'INCLUDE', '접수', 40),
    (md5('semantic-include-005')::uuid, 'INCLUDE_LOAN', 'INCLUDE', '융자', 50),
    (md5('semantic-include-006')::uuid, 'INCLUDE_SUBSIDY', 'INCLUDE', '보조', 60),
    (md5('semantic-include-007')::uuid, 'INCLUDE_VOUCHER', 'INCLUDE', '바우처', 70),
    (md5('semantic-include-008')::uuid, 'INCLUDE_SCHOLARSHIP', 'INCLUDE', '장학', 80),
    (md5('semantic-include-009')::uuid, 'INCLUDE_POLICY_FUND', 'INCLUDE', '정책자금', 90),
    (md5('semantic-include-010')::uuid, 'INCLUDE_SMALL_BUSINESS', 'INCLUDE', '소상공인', 100),
    (md5('semantic-include-011')::uuid, 'INCLUDE_SME', 'INCLUDE', '중소기업', 110),
    (md5('semantic-include-012')::uuid, 'INCLUDE_STARTUP', 'INCLUDE', '창업', 120),
    (md5('semantic-include-013')::uuid, 'INCLUDE_YOUTH', 'INCLUDE', '청년', 130),
    (md5('semantic-include-014')::uuid, 'INCLUDE_FAMILY', 'INCLUDE', '가족', 140),
    (md5('semantic-include-015')::uuid, 'INCLUDE_CHILD', 'INCLUDE', '아동', 150),
    (md5('semantic-include-016')::uuid, 'INCLUDE_WELFARE', 'INCLUDE', '복지', 160),
    (md5('semantic-include-017')::uuid, 'INCLUDE_JOB', 'INCLUDE', '일자리', 170),
    (md5('semantic-include-018')::uuid, 'INCLUDE_HOUSING', 'INCLUDE', '주거', 180),
    (md5('semantic-include-019')::uuid, 'INCLUDE_EDUCATION', 'INCLUDE', '교육', 190),
    (md5('semantic-include-020')::uuid, 'INCLUDE_INSURANCE', 'INCLUDE', '건강보험', 200),
    (md5('semantic-include-021')::uuid, 'INCLUDE_GUARANTEE', 'INCLUDE', '보증', 210),
    (md5('semantic-include-022')::uuid, 'INCLUDE_EMPLOYMENT', 'INCLUDE', '고용', 220),
    (md5('semantic-include-023')::uuid, 'INCLUDE_CHILDCARE', 'INCLUDE', '육아', 230),
    (md5('semantic-include-024')::uuid, 'INCLUDE_BIRTH', 'INCLUDE', '출산', 240),
    (md5('semantic-include-025')::uuid, 'INCLUDE_LOW_INCOME', 'INCLUDE', '저소득', 250),
    (md5('semantic-include-026')::uuid, 'INCLUDE_MEDIAN_INCOME', 'INCLUDE', '중위소득', 260),
    (md5('semantic-include-027')::uuid, 'INCLUDE_CONTEST', 'INCLUDE', '공모사업', 270),
    (md5('semantic-exclude-001')::uuid, 'EXCLUDE_PRESS_RELEASE', 'EXCLUDE', '보도자료', 10),
    (md5('semantic-exclude-002')::uuid, 'EXCLUDE_BRIEFING', 'EXCLUDE', '브리핑', 20),
    (md5('semantic-exclude-003')::uuid, 'EXCLUDE_DRILL', 'EXCLUDE', '사격훈련', 30),
    (md5('semantic-exclude-004')::uuid, 'EXCLUDE_PUBLIC_NOTICE', 'EXCLUDE', '공시송달', 40),
    (md5('semantic-exclude-005')::uuid, 'EXCLUDE_ADMINISTRATIVE_ACTION', 'EXCLUDE', '행정처분', 50),
    (md5('semantic-exclude-006')::uuid, 'EXCLUDE_FINE', 'EXCLUDE', '과태료', 60),
    (md5('semantic-exclude-007')::uuid, 'EXCLUDE_BID', 'EXCLUDE', '입찰', 70),
    (md5('semantic-exclude-008')::uuid, 'EXCLUDE_PRIVATE_CONTRACT', 'EXCLUDE', '수의계약', 80),
    (md5('semantic-exclude-009')::uuid, 'EXCLUDE_SERVICE_CONTRACT', 'EXCLUDE', '용역', 90),
    (md5('semantic-exclude-010')::uuid, 'EXCLUDE_WATER_TEST', 'EXCLUDE', '수질검사', 100),
    (md5('semantic-exclude-011')::uuid, 'EXCLUDE_OZONE', 'EXCLUDE', '오존', 110),
    (md5('semantic-exclude-012')::uuid, 'EXCLUDE_IMPERSONATION', 'EXCLUDE', '사칭', 120),
    (md5('semantic-exclude-013')::uuid, 'EXCLUDE_RESIDENT_SURVEY', 'EXCLUDE', '주민등록 사실조사', 130),
    (md5('semantic-exclude-014')::uuid, 'EXCLUDE_PERSONNEL', 'EXCLUDE', '인사발령', 140),
    (md5('semantic-exclude-015')::uuid, 'EXCLUDE_ORGANIZATION', 'EXCLUDE', '조직개편', 150),
    (md5('semantic-exclude-016')::uuid, 'EXCLUDE_RECRUITMENT_JOB', 'EXCLUDE', '채용공고', 160);

-- Geomdan-gu opened on 2026-07-01 and replaced the seeded 404 notice page with its official gosi board.
WITH reviewed_source (public_code, notice_url, parser_profile_code) AS (
    VALUES (
        'LGS-000063',
        'https://www.geomdan.go.kr/main/community/news/gosi.jsp',
        'HEURISTIC_NOTICE'
    )
)
UPDATE local_government_notice_sources AS source
SET homepage_url = 'https://www.geomdan.go.kr/',
    notice_url = reviewed_source.notice_url,
    collection_endpoint_url = NULL,
    parser_profile_code = reviewed_source.parser_profile_code,
    validation_status_code = 'VERIFIED',
    last_http_status = NULL,
    last_error_code = NULL,
    last_error_message = NULL,
    etag = NULL,
    last_modified_value = NULL,
    last_content_fingerprint = NULL,
    updated_at = now()
FROM reviewed_source
WHERE source.public_code = reviewed_source.public_code
  AND source.deleted_at IS NULL;

UPDATE local_government_notice_sources
SET request_profile_code = 'SESSION_BROWSER',
    updated_at = now()
WHERE public_code IN ('LGS-000063')
  AND deleted_at IS NULL;

-- The classification is a static result of the official URL and latest-item QA completed on 2026-07-27.
UPDATE local_government_notice_sources
SET source_board_type_code = CASE
        WHEN public_code = 'LGS-000084' THEN 'PRESS_RELEASE'
        WHEN page_type_code IN ('small_business_support_page', 'dedicated_small_business_board')
            THEN 'SUPPORT_RECRUITMENT'
        WHEN page_type_code = 'public_notice_board'
             OR lower(notice_url) ~ '(eminwon|emwp|emws|saeol/gosi|selectgosi|/gosi([/.?]|$)|publicnotice|searchgosi|ofraction|notancmt|not_ancmt|seolcontent|section=gosi|bcd=gosi)'
            THEN 'LEGAL_NOTICE'
        ELSE 'GENERAL_NOTICE'
    END,
    collection_policy_code = CASE
        WHEN public_code = 'LGS-000084' THEN 'EXCLUDED'
        WHEN page_type_code IN ('small_business_support_page', 'dedicated_small_business_board', 'public_notice_board')
             OR lower(notice_url) ~ '(eminwon|emwp|emws|saeol/gosi|selectgosi|/gosi([/.?]|$)|publicnotice|searchgosi|ofraction|notancmt|not_ancmt|seolcontent|section=gosi|bcd=gosi)'
            THEN 'COLLECT_ALL'
        ELSE 'KEYWORD_FILTERED'
    END,
    is_semantically_verified = true,
    semantic_verified_at = now(),
    semantic_verification_note = '2026-07-27 공식 URL·메뉴 경로·최근 게시물 표본 정적 QA',
    is_enabled = CASE WHEN public_code = 'LGS-000084' THEN false ELSE is_enabled END,
    collection_status_code = CASE WHEN public_code = 'LGS-000084' THEN 'DISABLED' ELSE collection_status_code END,
    updated_at = now()
WHERE deleted_at IS NULL;

ALTER TABLE local_government_notice_sources
    DROP CONSTRAINT ck_local_government_notice_sources_enabled;
ALTER TABLE local_government_notice_sources
    ADD CONSTRAINT ck_local_government_notice_sources_enabled CHECK (
        NOT is_enabled
        OR (
            validation_status_code = 'VERIFIED'
            AND parser_profile_code IS NOT NULL
            AND deleted_at IS NULL
            AND is_semantically_verified
            AND source_board_type_code <> 'UNVERIFIED'
            AND collection_policy_code <> 'EXCLUDED'
        )
    );

ALTER TABLE announcement_source_snapshots
    ADD COLUMN semantic_status_code varchar(30) NOT NULL DEFAULT 'ACCEPTED',
    ADD COLUMN semantic_reason_code varchar(80) NOT NULL DEFAULT 'PROVIDER_TRUSTED',
    ADD COLUMN semantic_matched_keywords varchar(1000);
ALTER TABLE announcement_source_snapshots
    ADD CONSTRAINT ck_announcement_source_snapshots_semantic_status
        CHECK (semantic_status_code IN ('ACCEPTED', 'EXCLUDED', 'REVIEW_REQUIRED'));
CREATE INDEX ix_announcement_source_snapshots_semantic
    ON announcement_source_snapshots (provider_code, semantic_status_code, review_status_code, collected_at DESC);

ALTER TABLE announcement_source_collection_runs
    ADD COLUMN excluded_count integer NOT NULL DEFAULT 0;
ALTER TABLE announcement_source_collection_runs
    DROP CONSTRAINT ck_announcement_source_collection_runs_counts;
ALTER TABLE announcement_source_collection_runs
    ADD CONSTRAINT ck_announcement_source_collection_runs_counts CHECK (
        total_count >= 0
        AND collected_count >= 0
        AND skipped_ended_count >= 0
        AND duplicate_count >= 0
        AND failed_count >= 0
        AND excluded_count >= 0
    );

ALTER TABLE announcement_source_collection_run_items
    ADD COLUMN semantic_reason_code varchar(80),
    ADD COLUMN semantic_matched_keywords varchar(1000);
ALTER TABLE announcement_source_collection_run_items
    DROP CONSTRAINT ck_announcement_source_collection_run_items_status;
ALTER TABLE announcement_source_collection_run_items
    ADD CONSTRAINT ck_announcement_source_collection_run_items_status
        CHECK (item_status_code IN ('COLLECTED', 'DUPLICATE', 'SKIPPED_ENDED', 'EXCLUDED', 'FAILED'));

ALTER TABLE announcement_source_collection_source_results
    ADD COLUMN excluded_count integer NOT NULL DEFAULT 0;
ALTER TABLE announcement_source_collection_source_results
    DROP CONSTRAINT ck_announcement_source_collection_source_results_counts;
ALTER TABLE announcement_source_collection_source_results
    ADD CONSTRAINT ck_announcement_source_collection_source_results_counts CHECK (
        discovered_count >= 0
        AND new_count >= 0
        AND duplicate_count >= 0
        AND failed_count >= 0
        AND excluded_count >= 0
    );

-- Reclassify existing local-government snapshots with the same static rules.
WITH semantic_matches AS (
    SELECT ass.id,
           lgns.collection_policy_code,
           coalesce((
               SELECT string_agg(rule.keyword_text, ', ' ORDER BY rule.sort_order)
               FROM announcement_source_semantic_keyword_rules rule
               WHERE rule.is_enabled
                 AND rule.rule_type_code = 'INCLUDE'
                 AND position(lower(rule.keyword_text) IN lower(ass.title)) > 0
           ), '') AS include_keywords,
           coalesce((
               SELECT string_agg(rule.keyword_text, ', ' ORDER BY rule.sort_order)
               FROM announcement_source_semantic_keyword_rules rule
               WHERE rule.is_enabled
                 AND rule.rule_type_code = 'EXCLUDE'
                 AND position(lower(rule.keyword_text) IN lower(ass.title)) > 0
           ), '') AS exclude_keywords
    FROM announcement_source_snapshots ass
    INNER JOIN local_government_notice_sources lgns ON lgns.id = ass.local_government_source_id
    WHERE ass.provider_code = 'LOCAL_GOV_NOTICE'
),
classified AS (
    SELECT id,
           CASE
               WHEN collection_policy_code = 'COLLECT_ALL' THEN 'ACCEPTED'
               WHEN collection_policy_code = 'EXCLUDED' THEN 'EXCLUDED'
               WHEN include_keywords = '' THEN 'EXCLUDED'
               WHEN exclude_keywords <> '' THEN 'REVIEW_REQUIRED'
               ELSE 'ACCEPTED'
           END AS semantic_status_code,
           CASE
               WHEN collection_policy_code = 'COLLECT_ALL' THEN 'SOURCE_POLICY_COLLECT_ALL'
               WHEN collection_policy_code = 'EXCLUDED' THEN 'SOURCE_POLICY_EXCLUDED'
               WHEN include_keywords = '' AND exclude_keywords <> '' THEN 'EXCLUDE_KEYWORD_MATCHED'
               WHEN include_keywords = '' THEN 'NO_INCLUDE_KEYWORD'
               WHEN exclude_keywords <> '' THEN 'INCLUDE_AND_EXCLUDE_KEYWORD'
               ELSE 'INCLUDE_KEYWORD_MATCHED'
           END AS semantic_reason_code,
           nullif(concat_ws(', ', nullif(include_keywords, ''), nullif(exclude_keywords, '')), '') AS matched_keywords
    FROM semantic_matches
)
UPDATE announcement_source_snapshots ass
SET semantic_status_code = classified.semantic_status_code,
    semantic_reason_code = classified.semantic_reason_code,
    semantic_matched_keywords = classified.matched_keywords,
    review_status_code = CASE
        WHEN classified.semantic_status_code = 'EXCLUDED'
             AND ass.review_status_code IN ('COLLECTED', 'REVIEW_PENDING', 'CONDITION_INPUT_REQUIRED')
             AND NOT EXISTS (
                 SELECT 1 FROM announcement_source_links link WHERE link.source_id = ass.id
             )
            THEN 'ARCHIVED'
        ELSE ass.review_status_code
    END,
    updated_at = now()
FROM classified
WHERE ass.id = classified.id;
