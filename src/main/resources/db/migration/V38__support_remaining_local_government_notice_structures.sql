-- Add bounded parser support for the remaining verified local-government notice structures.
-- Sources stay OFF after verification; an operator must enable them explicitly.

ALTER TABLE local_government_notice_parser_profiles
    DROP CONSTRAINT ck_local_government_notice_parser_profiles_type;

ALTER TABLE local_government_notice_parser_profiles
    ADD CONSTRAINT ck_local_government_notice_parser_profiles_type CHECK (
        parser_type_code IN (
            'SAEOL_GOSI', 'SPRING_BBS', 'JSP_BBS', 'TC_GOSI',
            'GENERIC_TABLE', 'GENERIC_LIST', 'HEURISTIC_NOTICE', 'GENERIC_JSON',
            'DAEJEON_EMINWON', 'MANUAL_ONLY'
        )
    );

INSERT INTO local_government_notice_parser_profiles (
    id, profile_code, profile_name, parser_type_code,
    list_item_selector, title_selector, date_selector, link_selector, date_pattern,
    response_type_code, json_items_path, json_title_field, json_date_field,
    json_link_field, json_link_template,
    link_strategy_code, link_function_name, link_function_argument_count,
    link_url_template, is_enabled
) VALUES
(
    '4f8b7d6a-97a0-4f6e-8e6a-665a4fa1c101',
    'RFC3_BOARD_NOTICE',
    'RFC3 직접 링크 게시판',
    'GENERIC_TABLE',
    'table tbody tr',
    'td.title > a[href*=''/board/view.'']',
    'td[data-cell-header=''작성일''], td.date, td:nth-of-type(4)',
    'td.title > a[href*=''/board/view.'']',
    NULL,
    'HTML', NULL, NULL, NULL, NULL, NULL,
    'AUTO', NULL, NULL, NULL, true
),
(
    '4f8b7d6a-97a0-4f6e-8e6a-665a4fa1c102',
    'SAFE_SAEOL_EMINWON_CELL',
    '새올 전자민원 셀 클릭형',
    'GENERIC_TABLE',
    'table tr:has(td:nth-of-type(3)[onclick*=searchDetail])',
    'td:nth-of-type(3)[onclick*=searchDetail]',
    'td:nth-of-type(5)',
    'td:nth-of-type(3)[onclick*=searchDetail]',
    'yyyy-MM-dd',
    'HTML', NULL, NULL, NULL, NULL, NULL,
    'SAFE_TEMPLATE', 'searchDetail', 1,
    '/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}&subCheck=Y',
    true
),
(
    '4f8b7d6a-97a0-4f6e-8e6a-665a4fa1c103',
    'DAMYANG_NOTICE_JSON',
    '담양군 공지사항 JSON',
    'GENERIC_JSON',
    NULL, NULL, NULL, NULL,
    'yyyy-MM-dd',
    'JSON',
    'RSLT_DATA.boardContentsList',
    'dataTitle',
    'registerDate',
    'dataSid',
    '/board/detail?dataSid={value}&boardId=BBS_0000001&domainId=DOM_0000001&contentsSid=1&menuCd=DOM_000000190001001000',
    'AUTO', NULL, NULL, NULL, true
),
(
    '4f8b7d6a-97a0-4f6e-8e6a-665a4fa1c104',
    'DAEJEON_EMINWON_AGGREGATOR',
    '대전시 구청 전자민원 통합 목록',
    'DAEJEON_EMINWON',
    'table tbody tr:has(td.subject a[onclick*=popupCenterNew])',
    'td.subject a[onclick*=popupCenterNew]',
    'td.date',
    'td.subject a[onclick*=popupCenterNew]',
    'yyyy-MM-dd',
    'HTML', NULL, NULL, NULL, NULL, NULL,
    'AUTO', NULL, NULL, NULL, true
)
ON CONFLICT (profile_code) DO NOTHING;

UPDATE local_government_notice_sources
SET collection_endpoint_url = 'https://www.damyang.go.kr/board/getContentsList?domainId=DOM_0000001&boardId=BBS_0000001&orderCondition=REGISTER_DATE&searchCondition=DATA_TITLE&searchKeyword=&getOfficeNm=true&ROW_CNT=10&BEGIN_ROW_IDX=1&CUR_PAGE_IDX=1',
    request_profile_code = 'BROWSER_HTTP1',
    updated_at = now()
WHERE public_code = 'LGS-000183'
  AND deleted_at IS NULL;

WITH qa_pass(public_code, parser_profile_code) AS (
    VALUES
        ('LGS-000071', 'DAEJEON_EMINWON_AGGREGATOR'),
        ('LGS-000174', 'SUBJECT_NOTICE_TABLE'),
        ('LGS-000183', 'DAMYANG_NOTICE_JSON'),
        ('LGS-000232', 'SUBJECT_NOTICE_TABLE'),
        ('LGS-000233', 'SAFE_SAEOL_EMINWON_CELL'),
        ('LGS-000235', 'SPRING_BBS')
)
UPDATE local_government_notice_sources AS source
SET parser_profile_code = qa_pass.parser_profile_code,
    validation_status_code = 'VERIFIED',
    collection_status_code = 'READY',
    is_enabled = false,
    last_http_status = NULL,
    last_error_code = NULL,
    last_error_message = NULL,
    updated_at = now()
FROM qa_pass
WHERE source.public_code = qa_pass.public_code
  AND source.deleted_at IS NULL;

-- Replace retired board paths with current official list endpoints discovered during the live QA.
WITH reviewed_url(public_code, notice_url) AS (
    VALUES
        ('LGS-000094', 'https://www.anyang.go.kr/main/emwsWebList.do?key=4101&searchGosiSe=01%2C03%2C04'),
        ('LGS-000108', 'https://pocheon.go.kr/www/selectEminwonList.do?key=3712&notAncmtSeCode=01'),
        ('LGS-000120', 'https://eminwon.dh.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=Y&yyyy='),
        ('LGS-000122', 'https://eminwon.sokcho.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=Y&yyyy='),
        ('LGS-000135', 'https://www.chungbuk.go.kr/www/selectGosiPblancList.do?key=422'),
        ('LGS-000145', 'https://eminwon.eumseong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=Y&yyyy='),
        ('LGS-000149', 'https://www.gongju.go.kr/prog/saeolGosi/GOSI_03/sub04_03_03/list.do'),
        ('LGS-000242', 'https://www.jeju.go.kr/news/news/news.htm')
)
UPDATE local_government_notice_sources AS source
SET notice_url = reviewed_url.notice_url,
    collection_endpoint_url = NULL,
    parser_profile_code = 'MANUAL_ONLY',
    validation_status_code = 'CHECK_REQUIRED',
    collection_status_code = 'CHECK_REQUIRED',
    is_enabled = false,
    last_http_status = NULL,
    last_error_code = NULL,
    last_error_message = NULL,
    etag = NULL,
    last_modified_value = NULL,
    last_content_fingerprint = NULL,
    updated_at = now()
FROM reviewed_url
WHERE source.public_code = reviewed_url.public_code
  AND source.deleted_at IS NULL;

UPDATE local_government_notice_sources
SET request_profile_code = 'BROWSER_HTTP1',
    updated_at = now()
WHERE public_code IN (
    'LGS-000094', 'LGS-000108', 'LGS-000120', 'LGS-000122',
    'LGS-000135', 'LGS-000145', 'LGS-000149', 'LGS-000242'
)
  AND deleted_at IS NULL;
