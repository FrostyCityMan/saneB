-- Add a constrained heuristic parser for local-government boards that do not match static table/list selectors.

ALTER TABLE local_government_notice_parser_profiles
    DROP CONSTRAINT ck_local_government_notice_parser_profiles_type;

ALTER TABLE local_government_notice_parser_profiles
    ADD CONSTRAINT ck_local_government_notice_parser_profiles_type CHECK (
        parser_type_code IN (
            'SAEOL_GOSI', 'SPRING_BBS', 'JSP_BBS', 'TC_GOSI',
            'GENERIC_TABLE', 'GENERIC_LIST', 'HEURISTIC_NOTICE', 'MANUAL_ONLY'
        )
    );

INSERT INTO local_government_notice_parser_profiles (
    id, profile_code, profile_name, parser_type_code,
    list_item_selector, title_selector, date_selector, link_selector, date_pattern, is_enabled
) VALUES (
    'f74aa573-7d6e-4d03-a0af-8319c48d5848',
    'HEURISTIC_NOTICE',
    '제한형 공고 링크 탐색',
    'HEURISTIC_NOTICE',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    true
)
ON CONFLICT (profile_code) DO NOTHING;
