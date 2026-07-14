-- Add narrow parser profiles for legacy boards recovered by the LEGACY_BROWSER transport.
-- Script-based detail links are reconstructed only from reviewed literal arguments or data attributes.

INSERT INTO local_government_notice_parser_profiles (
    id, profile_code, profile_name, parser_type_code,
    list_item_selector, title_selector, date_selector, link_selector, date_pattern,
    link_strategy_code, link_function_name, link_function_argument_count, link_url_template, is_enabled
) VALUES
(
    '51e73ecb-8eec-47a8-a238-3b96df802101',
    'JUNGNANG_CONTEST_BOARD',
    '중랑구 공모사업 게시판',
    'GENERIC_TABLE',
    'table.inc_head tbody tr.noticeTitlte',
    'td.tit a[href*="/portal/bbs/view/"]',
    'td:nth-of-type(3)',
    'td.tit a[href*="/portal/bbs/view/"]',
    'yyyy-MM-dd',
    'AUTO',
    NULL,
    NULL,
    NULL,
    true
),
(
    '51e73ecb-8eec-47a8-a238-3b96df802102',
    'SAFE_PYEONGTAEK_BOARD_RENEWAL',
    '평택시 갱신형 게시판',
    'GENERIC_TABLE',
    'table tbody tr:has(td.col_title a[onclick*=boardViewRenewal])',
    'td.col_title a[onclick*=boardViewRenewal]',
    'td.col_date',
    'td.col_title a[onclick*=boardViewRenewal]',
    'yyyy.MM.dd',
    'SAFE_TEMPLATE',
    'boardViewRenewal',
    7,
    '/pyeongtaek/board/post/view.do?bcIdx={arg:4}&idx={arg:5}&mid={arg:6}',
    true
),
(
    '51e73ecb-8eec-47a8-a238-3b96df802103',
    'SAFE_EGOV_BOARD_BUTTON',
    '전자정부 제목 버튼형 게시판',
    'GENERIC_TABLE',
    'table tbody tr:has(td.board__table--title button[onclick*=fn_search_detail])',
    'td.board__table--title button[onclick*=fn_search_detail]',
    'td.board__table--date',
    'td.board__table--title button[onclick*=fn_search_detail]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'fn_search_detail',
    1,
    'view.do?nttId={arg:1}',
    true
),
(
    '51e73ecb-8eec-47a8-a238-3b96df802104',
    'SAFE_EGOV_DATA_BUTTON',
    '전자정부 데이터 버튼형 게시판',
    'GENERIC_TABLE',
    'table tbody tr:has(td.board__table--title button[data-ntt-id])',
    'td.board__table--title button[data-ntt-id]',
    'td.board__table--date',
    'td.board__table--title button[data-ntt-id]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    NULL,
    NULL,
    'view.do?nttId={attr:data-ntt-id}',
    true
)
ON CONFLICT (profile_code) DO NOTHING;
