-- Add a narrow parser for SAEOL pages whose title and date columns are compacted by hidden cells.

INSERT INTO local_government_notice_parser_profiles (
    id, profile_code, profile_name, parser_type_code,
    list_item_selector, title_selector, date_selector, link_selector, date_pattern,
    link_strategy_code, link_function_name, link_function_argument_count, link_url_template, is_enabled
) VALUES (
    '67fbc597-1a98-4fca-b1a1-faa16ff80201',
    'SAFE_SAEOL_EMINWON_COMPACT',
    '새올 전자민원 축약 열 게시판',
    'GENERIC_TABLE',
    'table.board1 tbody tr:has(td:nth-of-type(2) a[onclick*=searchDetail])',
    'td:nth-of-type(2) a[onclick*=searchDetail]',
    'td:nth-of-type(4)',
    'td:nth-of-type(2) a[onclick*=searchDetail]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'searchDetail',
    1,
    '/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}&subCheck=Y',
    true
)
ON CONFLICT (profile_code) DO NOTHING;
