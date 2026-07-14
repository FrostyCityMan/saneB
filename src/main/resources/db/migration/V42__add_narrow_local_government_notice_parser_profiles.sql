-- Add narrow reusable profiles for recurring local-government board structures.
-- The selectors intentionally require a detail-link signature so navigation and paging links are excluded.

INSERT INTO local_government_notice_parser_profiles (
    id, profile_code, profile_name, parser_type_code,
    list_item_selector, title_selector, date_selector, link_selector, date_pattern,
    link_strategy_code, link_function_name, link_function_argument_count, link_url_template, is_enabled
) VALUES
(
    '2a57f03e-2b48-4c3f-88cc-cc7bc1e14201',
    'SAFE_SAEOL_EMINWON_LEGACY',
    '구형 새올 전자민원 고시공고',
    'GENERIC_TABLE',
    'table[summary*=고시공고] tr:has(td:nth-of-type(3) a[onclick*=searchDetail])',
    'td:nth-of-type(3) a[onclick*=searchDetail]',
    'td:nth-of-type(5)',
    'td:nth-of-type(3) a[onclick*=searchDetail]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'searchDetail',
    1,
    '/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}&subCheck={query:subCheck}',
    true
),
(
    '2a57f03e-2b48-4c3f-88cc-cc7bc1e14202',
    'SAFE_SAEOL_EMINWON_HREF',
    '새올 전자민원 링크 호출형',
    'GENERIC_TABLE',
    'table.table1 tbody tr:has(td.title a[href*=searchDetail])',
    'td.title a[href*=searchDetail]',
    'td:nth-of-type(5)',
    'td.title a[href*=searchDetail]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'searchDetail',
    1,
    '/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}&subCheck={query:subCheck}',
    true
),
(
    '2a57f03e-2b48-4c3f-88cc-cc7bc1e14203',
    'SAFE_EGOV_DETAIL_BUTTON',
    '전자정부 버튼 상세형 게시판',
    'GENERIC_TABLE',
    'table tbody tr:has(td.subject button[onclick*=fn_search_detail])',
    'td.subject button[onclick*=fn_search_detail]',
    'td.regDate',
    'td.subject button[onclick*=fn_search_detail]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'fn_search_detail',
    1,
    'view.do?nttId={arg:1}',
    true
),
(
    '2a57f03e-2b48-4c3f-88cc-cc7bc1e14204',
    'RFC_BLOGLIST_NOTICE',
    'RFC 블로그 목록형 게시판',
    'GENERIC_LIST',
    'div.bloglist-wrap > ul > li:has(a[href*=dataSid])',
    'span.btxt',
    'span.date',
    'a[href*=dataSid]',
    'yyyy.MM.dd',
    'AUTO',
    NULL,
    NULL,
    NULL,
    true
),
(
    '2a57f03e-2b48-4c3f-88cc-cc7bc1e14205',
    'GURYE_BOARD_NOTICE',
    '제목 작성일 셀형 게시판',
    'GENERIC_TABLE',
    'table tbody tr:has(td.tit a[href*=nttId])',
    'td.tit a[href*=nttId]',
    'td.date',
    'td.tit a[href*=nttId]',
    'yyyy-MM-dd',
    'AUTO',
    NULL,
    NULL,
    NULL,
    true
)
ON CONFLICT (profile_code) DO NOTHING;
