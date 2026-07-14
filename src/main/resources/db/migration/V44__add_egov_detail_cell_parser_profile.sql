-- Add a safe parser for e-government notice tables whose posted-date cell is identified by its header label.
-- The detail URL is reconstructed only from the expected fn_search_detail call argument.

INSERT INTO local_government_notice_parser_profiles (
    id, profile_code, profile_name, parser_type_code,
    list_item_selector, title_selector, date_selector, link_selector, date_pattern,
    link_strategy_code, link_function_name, link_function_argument_count, link_url_template, is_enabled
) VALUES (
    '2a57f03e-2b48-4c3f-88cc-cc7bc1e14401',
    'SAFE_EGOV_DETAIL_CELL',
    '전자정부 등록일 셀 상세형 게시판',
    'GENERIC_TABLE',
    'table.table-default tbody tr:has(td.subject a[onclick*=fn_search_detail])',
    'td.subject a[onclick*=fn_search_detail]',
    'td[data-cell-header="등록일"]',
    'td.subject a[onclick*=fn_search_detail]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'fn_search_detail',
    1,
    'view.do?notAncmtMgtNo={arg:1}',
    true
)
ON CONFLICT (profile_code) DO NOTHING;
