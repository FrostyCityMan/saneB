\encoding UTF8
\set ON_ERROR_STOP on

-- Read-only dry-run. This script does not update or delete operational data.

-- 1. Explain the registry count by administrative unit type.
SELECT institution_type_code,
       count(1) AS source_count
FROM local_government_notice_sources
WHERE deleted_at IS NULL
GROUP BY institution_type_code
ORDER BY institution_type_code;

-- 2. Preview the source board type and collection policy produced by V56 rules.
WITH source_classified AS (
    SELECT id,
           public_code,
           institution_name,
           is_enabled,
           CASE
               WHEN public_code = 'LGS-000084' THEN 'PRESS_RELEASE'
               WHEN page_type_code IN ('small_business_support_page', 'dedicated_small_business_board')
                   THEN 'SUPPORT_RECRUITMENT'
               WHEN page_type_code = 'public_notice_board'
                    OR lower(notice_url) ~
                       '(eminwon|emwp|emws|saeol/gosi|selectgosi|/gosi([/.?]|$)|publicnotice|searchgosi|ofraction|notancmt|not_ancmt|seolcontent|section=gosi|bcd=gosi)'
                   THEN 'LEGAL_NOTICE'
               ELSE 'GENERAL_NOTICE'
           END AS source_board_type_code,
           CASE
               WHEN public_code = 'LGS-000084' THEN 'EXCLUDED'
               WHEN page_type_code IN (
                        'small_business_support_page',
                        'dedicated_small_business_board',
                        'public_notice_board'
                    )
                    OR lower(notice_url) ~
                       '(eminwon|emwp|emws|saeol/gosi|selectgosi|/gosi([/.?]|$)|publicnotice|searchgosi|ofraction|notancmt|not_ancmt|seolcontent|section=gosi|bcd=gosi)'
                   THEN 'COLLECT_ALL'
               ELSE 'KEYWORD_FILTERED'
           END AS collection_policy_code
    FROM local_government_notice_sources
    WHERE deleted_at IS NULL
)
SELECT source_board_type_code,
       collection_policy_code,
       count(1) AS source_count,
       count(1) FILTER (WHERE is_enabled) AS enabled_count
FROM source_classified
GROUP BY source_board_type_code, collection_policy_code
ORDER BY source_board_type_code, collection_policy_code;

-- 3. Preview existing LOCAL_GOV_NOTICE snapshot classification without changing review status.
WITH source_classified AS (
    SELECT id,
           CASE
               WHEN public_code = 'LGS-000084' THEN 'EXCLUDED'
               WHEN page_type_code IN (
                        'small_business_support_page',
                        'dedicated_small_business_board',
                        'public_notice_board'
                    )
                    OR lower(notice_url) ~
                       '(eminwon|emwp|emws|saeol/gosi|selectgosi|/gosi([/.?]|$)|publicnotice|searchgosi|ofraction|notancmt|not_ancmt|seolcontent|section=gosi|bcd=gosi)'
                   THEN 'COLLECT_ALL'
               ELSE 'KEYWORD_FILTERED'
           END AS collection_policy_code
    FROM local_government_notice_sources
    WHERE deleted_at IS NULL
),
snapshot_matches AS (
    SELECT ass.id,
           ass.public_code,
           ass.title,
           source.collection_policy_code,
           ARRAY(
               SELECT keyword
               FROM unnest(ARRAY[
                   '지원', '모집', '신청', '접수', '융자', '보조', '바우처', '장학',
                   '정책자금', '소상공인', '중소기업', '창업', '청년', '가족', '아동',
                   '복지', '일자리', '주거', '교육', '건강보험', '보증', '고용', '육아',
                   '출산', '저소득', '중위소득', '공모사업'
               ]) WITH ORDINALITY AS include_rule(keyword, sort_order)
               WHERE position(lower(include_rule.keyword) IN lower(ass.title)) > 0
               ORDER BY include_rule.sort_order
           ) AS include_keywords,
           ARRAY(
               SELECT keyword
               FROM unnest(ARRAY[
                   '보도자료', '브리핑', '사격훈련', '공시송달', '행정처분', '과태료',
                   '입찰', '수의계약', '용역', '수질검사', '오존', '사칭',
                   '주민등록 사실조사', '인사발령', '조직개편', '채용공고'
               ]) WITH ORDINALITY AS exclude_rule(keyword, sort_order)
               WHERE position(lower(exclude_rule.keyword) IN lower(ass.title)) > 0
               ORDER BY exclude_rule.sort_order
           ) AS exclude_keywords
    FROM announcement_source_snapshots ass
    INNER JOIN source_classified source ON source.id = ass.local_government_source_id
    WHERE ass.provider_code = 'LOCAL_GOV_NOTICE'
),
snapshot_classified AS (
    SELECT id,
           public_code,
           title,
           CASE
               WHEN collection_policy_code = 'COLLECT_ALL' THEN 'ACCEPTED'
               WHEN collection_policy_code = 'EXCLUDED' THEN 'EXCLUDED'
               WHEN cardinality(include_keywords) = 0 THEN 'EXCLUDED'
               WHEN cardinality(exclude_keywords) > 0 THEN 'REVIEW_REQUIRED'
               ELSE 'ACCEPTED'
           END AS semantic_status_code,
           CASE
               WHEN collection_policy_code = 'COLLECT_ALL' THEN 'SOURCE_POLICY_COLLECT_ALL'
               WHEN collection_policy_code = 'EXCLUDED' THEN 'SOURCE_POLICY_EXCLUDED'
               WHEN cardinality(include_keywords) = 0 AND cardinality(exclude_keywords) > 0
                   THEN 'EXCLUDE_KEYWORD_MATCHED'
               WHEN cardinality(include_keywords) = 0 THEN 'NO_INCLUDE_KEYWORD'
               WHEN cardinality(exclude_keywords) > 0 THEN 'INCLUDE_AND_EXCLUDE_KEYWORD'
               ELSE 'INCLUDE_KEYWORD_MATCHED'
           END AS semantic_reason_code
    FROM snapshot_matches
)
SELECT semantic_status_code,
       semantic_reason_code,
       count(1) AS snapshot_count
FROM snapshot_classified
GROUP BY semantic_status_code, semantic_reason_code
ORDER BY semantic_status_code, semantic_reason_code;
