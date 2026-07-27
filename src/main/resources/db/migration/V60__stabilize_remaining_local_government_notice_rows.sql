-- 광산구 공지 목록은 새글 표식이 제목 링크 앞에 올 수 있으므로 직접 자식 링크를 선택한다.
UPDATE local_government_notice_parser_profiles
SET title_selector = 'td.subject > a, td.title > a, td.bb-list-title > a',
    link_selector = 'td.subject > a, td.title > a, td.bb-list-title > a',
    updated_at = CURRENT_TIMESTAMP
WHERE profile_code = 'SUBJECT_NOTICE_TABLE';

-- 검색 폼의 내부 표와 상세 링크가 없는 행을 공고 후보에서 제외하는 좁은 프로필을 추가한다.
INSERT INTO local_government_notice_parser_profiles (
    id, profile_code, profile_name, parser_type_code,
    list_item_selector, title_selector, date_selector, link_selector, date_pattern,
    link_strategy_code, link_function_name, link_function_argument_count, link_url_template, is_enabled
) VALUES
(
    '7b9368c0-13cb-4be6-9c2a-cb5b14906001',
    'SEONGBUK_EMINWON_TABLE',
    '성북구 고시공고 표',
    'GENERIC_TABLE',
    'table.p-table.simple tbody.text_center > tr:has(> td.p-subject > a)',
    'td.p-subject > a',
    'td:last-child',
    'td.p-subject > a',
    'yyyy-MM-dd',
    'AUTO',
    NULL,
    NULL,
    NULL,
    true
),
(
    '7b9368c0-13cb-4be6-9c2a-cb5b14906002',
    'CHANGWON_GOSI_TABLE',
    '창원시 고시공고 표',
    'GENERIC_TABLE',
    'table.t3 tbody.tb > tr:has(> td.tal > a.a1)',
    'td.tal > a.a1',
    'td:nth-last-child(2)',
    'td.tal > a.a1',
    'yyyy-MM-dd',
    'AUTO',
    NULL,
    NULL,
    NULL,
    true
)
ON CONFLICT (profile_code) DO NOTHING;

UPDATE local_government_notice_sources
SET parser_profile_code = 'SEONGBUK_EMINWON_TABLE',
    updated_at = CURRENT_TIMESTAMP
WHERE public_code = 'LGS-000009'
  AND deleted_at IS NULL;

UPDATE local_government_notice_sources
SET parser_profile_code = 'CHANGWON_GOSI_TABLE',
    updated_at = CURRENT_TIMESTAMP
WHERE public_code = 'LGS-000224'
  AND deleted_at IS NULL;
