-- Replace general-news collection targets with verified official legal-notice boards.
-- Changed sources stay disabled until transport and parser QA is completed.

-- Accept the public Saeol searchDetail call from either onclick or href without executing JavaScript.
UPDATE local_government_notice_parser_profiles
SET list_item_selector =
        'table tbody tr:has(td:nth-of-type(3) a[onclick*=searchDetail]), '
        || 'table tbody tr:has(td:nth-of-type(3) a[href*=searchDetail])',
    title_selector =
        'td:nth-of-type(3) a[onclick*=searchDetail], '
        || 'td:nth-of-type(3) a[href*=searchDetail]',
    link_selector =
        'td:nth-of-type(3) a[onclick*=searchDetail], '
        || 'td:nth-of-type(3) a[href*=searchDetail]',
    updated_at = CURRENT_TIMESTAMP
WHERE profile_code = 'SAFE_SAEOL_EMINWON';

-- Reuse the narrow Saeol profile for the common title-column-2 and posted-date-column-4 layout.
UPDATE local_government_notice_parser_profiles
SET list_item_selector =
        'table tbody tr:has(td:nth-of-type(2) a[onclick*=searchDetail]), '
        || 'table tbody tr:has(td:nth-of-type(2) a[href*=searchDetail])',
    title_selector =
        'td:nth-of-type(2) a[onclick*=searchDetail], '
        || 'td:nth-of-type(2) a[href*=searchDetail]',
    link_selector =
        'td:nth-of-type(2) a[onclick*=searchDetail], '
        || 'td:nth-of-type(2) a[href*=searchDetail]',
    updated_at = CURRENT_TIMESTAMP
WHERE profile_code = 'SAFE_SAEOL_EMINWON_COMPACT';

-- Add the same fixed-template parser for Saeol's responsive list markup.
INSERT INTO local_government_notice_parser_profiles (
    id, profile_code, profile_name, parser_type_code,
    list_item_selector, title_selector, date_selector, link_selector, date_pattern,
    link_strategy_code, link_function_name, link_function_argument_count, link_url_template, is_enabled
) VALUES (
    '67fbc597-1a98-4fca-b1a1-faa16ff80202',
    'SAFE_SAEOL_EMINWON_LIST',
    '새올 전자민원 반응형 목록',
    'GENERIC_LIST',
    'div.dbody > ul:has(li.title a[onclick*=searchDetail])',
    'li.title a[onclick*=searchDetail]',
    'li.col04',
    'li.title a[onclick*=searchDetail]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'searchDetail',
    1,
    '/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}&subCheck=Y',
    true
)
ON CONFLICT (profile_code) DO NOTHING;

-- Correct profiles whose official legal-notice pages changed markup after the earlier QA baseline.
UPDATE local_government_notice_parser_profiles
SET date_selector =
        '.wrap1t3 > span.t3:matchesOwn(^\s*등록일\s*:), '
        || '.t3wrap > span.t3:matchesOwn(^\s*등록일\s*:)',
    updated_at = CURRENT_TIMESTAMP
WHERE profile_code = 'SCMS_CARD_NOTICE';

UPDATE local_government_notice_parser_profiles
SET list_item_selector = 'table tbody tr:has(td.cell-subject a[href*=opView])',
    title_selector = 'td.cell-subject a[href*=opView]',
    date_selector = 'td.cell-date',
    link_selector = 'td.cell-subject a[href*=opView]',
    date_pattern = 'yyyy-MM-dd',
    link_strategy_code = 'SAFE_TEMPLATE',
    link_function_name = 'opView',
    link_function_argument_count = 1,
    link_url_template =
        'BD_selectBbs.do?q_bbsCode={query:q_bbsCode}&q_clCode={query:q_clCode}'
        || '&q_estnColumn1={query:q_estnColumn1}&q_ntceSiteCode={query:q_ntceSiteCode}'
        || '&q_bbscttSn={arg:1}',
    updated_at = CURRENT_TIMESTAMP
WHERE profile_code = 'NOWON_NOTICE_TABLE';

UPDATE local_government_notice_parser_profiles
SET list_item_selector = 'table tbody tr:has(td.p-subject a[onclick*=fnGoDetail])',
    date_selector = 'td:nth-of-type(5)',
    date_pattern = 'yyyy-MM-dd',
    updated_at = CURRENT_TIMESTAMP
WHERE profile_code = 'SAFE_ANSAN_BBS';

UPDATE local_government_notice_parser_profiles
SET list_item_selector = 'table tbody tr:has(td.cell-subject a[onclick*=jsView])',
    title_selector = 'td.cell-subject a[onclick*=jsView]',
    date_selector = 'td.cell-tit i',
    link_selector = 'td.cell-subject a[onclick*=jsView]',
    date_pattern = 'yyyy/MM/dd',
    link_url_template =
        'BD_board.view.do?bbsCd={arg:1}&seq={arg:2}&showSummaryYn={arg:4}',
    updated_at = CURRENT_TIMESTAMP
WHERE profile_code = 'SAFE_PAJU_SUMMARY';

UPDATE local_government_notice_parser_profiles
SET list_item_selector = 'table tbody tr:has(td.bL_tableTitle a[onclick*=fn_articleLink])',
    date_selector = 'td:nth-of-type(5)',
    date_pattern = 'yyyy-MM-dd',
    link_url_template =
        '/kor/boardView.do?IDX=154&BRD_ID=1023&BOARD_IDX={arg:1}&page=1',
    updated_at = CURRENT_TIMESTAMP
WHERE profile_code = 'SAFE_GORYEONG_BOARD';

-- Add narrow parser profiles for official legal-notice boards with verified, stable DOM contracts.
INSERT INTO local_government_notice_parser_profiles (
    id, profile_code, profile_name, parser_type_code,
    list_item_selector, title_selector, date_selector, link_selector, date_pattern,
    link_strategy_code, link_function_name, link_function_argument_count, link_url_template, is_enabled
) VALUES
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80203',
    'MAPO_LEGAL_NOTICE_TABLE',
    '마포 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr:has(td.tal_l_i a[href*=''/nPortal/detail''])',
    'td.tal_l_i a[href*=''/nPortal/detail'']',
    'td:last-child',
    'td.tal_l_i a[href*=''/nPortal/detail'']',
    'yyyyMMdd',
    'AUTO', NULL, NULL, NULL, true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80204',
    'SAFE_DAEGU_LEGAL_NOTICE',
    '대구 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr:has(td[data-table-type=subject] a[href*=fn_goLinkView])',
    'td[data-table-type=subject] a[href*=fn_goLinkView]',
    'td[data-table-type=date]',
    'td[data-table-type=subject] a[href*=fn_goLinkView]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'fn_goLinkView',
    2,
    '?menu_id={query:menu_id}&menu_link=/front/daeguSidoGosi/daeguSidoGosiView.do'
        || '&sno={arg:1}&gosi_gbn={arg:2}',
    true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80205',
    'SAFE_INCHEON_CITYNET_NOTICE',
    '인천 시티넷 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr[onclick*=viewData]',
    'td:nth-of-type(2)',
    'td:nth-of-type(4)',
    'td:nth-of-type(2)',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'viewData',
    2,
    '/citynet/jsp/sap/SAPGosiBizProcess.do?command=searchDetail&flag=gosiGL'
        || '&svp=Y&sido=ic&sno={arg:1}&gosiGbn={arg:2}',
    true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80206',
    'SAFE_GWANGJU_NAMGU_NOTICE',
    '광주 남구 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr:has(td.AlignLeft a[onclick*=searchDetail])',
    'td.AlignLeft a[onclick*=searchDetail]',
    'td:nth-of-type(3)',
    'td.AlignLeft a[onclick*=searchDetail]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'searchDetail',
    1,
    '/api/eminwon/gosiView.es?mid={query:mid}&method=selectOfrNotAncmt'
        || '&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no={arg:1}',
    true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80207',
    'SAFE_DAEJEON_DATA_KEY_NOTICE',
    '대전 데이터키 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr:has(td.subject[data-key-no])',
    'td.subject[data-key-no] strong.bbs-subject-txt',
    'td[data-cell-header=''등록일'']',
    'td.subject[data-key-no]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    NULL,
    NULL,
    'view.do?notAncmtMgtNo={attr:data-key-no}',
    true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80208',
    'SAFE_YUSEONG_LEGAL_NOTICE',
    '대전 유성 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr:has(td.subject a[onclick*=fn_search_view])',
    'td.subject a[onclick*=fn_search_view]',
    'td:nth-of-type(5)',
    'td.subject a[onclick*=fn_search_view]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'fn_search_view',
    1,
    'view.do?notAncmtMgtNo={arg:1}',
    true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80209',
    'SAFE_HWASEONG_LEGAL_NOTICE',
    '화성 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr:has(td.ta_lft a[href*=opGosiView])',
    'td.ta_lft a[href*=opGosiView]',
    'td:nth-of-type(4)',
    'td.ta_lft a[href*=opGosiView]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'opGosiView',
    1,
    'BD_selectNoticeDetail.do?q_notAncmtMgtNo={arg:1}',
    true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80210',
    'SAFE_PORTAL_SAEOL_BOARD_VIEW',
    '포털 새올 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr:has(td a[onclick*=boardView])',
    'td a[onclick*=boardView]',
    'td.date, td.gosi_date',
    'td a[onclick*=boardView]',
    NULL,
    'SAFE_TEMPLATE',
    'boardView',
    2,
    '/portal/saeol/gosiView.do?notAncmtMgtNo={arg:2}&mId={query:mId}',
    true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80211',
    'SAFE_GWANGMYEONG_LEGAL_NOTICE',
    '광명 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr:has(td a[onclick*=opDetail])',
    'td a[onclick*=opDetail]',
    'td:nth-of-type(5)',
    'td a[onclick*=opDetail]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    'opDetail',
    1,
    'BD_selectNftcBbsDetail.do?q_nftcBbsCode=1001'
        || '&q_nftcBbsMgtno={arg:1}&q_currPage=1',
    true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80212',
    'BORYEONG_LEGAL_NOTICE',
    '보령 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr:has(td.left a[onclick*=popupCenter])',
    'td.left a[onclick*=popupCenter]',
    'td.date',
    'td.left a[onclick*=popupCenter]',
    'yyyy-MM-dd',
    'AUTO', NULL, NULL, NULL, true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80213',
    'SAFE_EGOV_DATA_LIST_NOTICE',
    '전자정부 데이터 목록번호 고시공고',
    'GENERIC_TABLE',
    'table tbody tr:has(td.subject button[data-list-no])',
    'td.subject button[data-list-no] strong.bbs-subject-txt',
    'td[data-cell-header=''등록일'']',
    'td.subject button[data-list-no]',
    'yyyy-MM-dd',
    'SAFE_TEMPLATE',
    NULL,
    NULL,
    'view.do?notAncmtMgtNo={attr:data-list-no}',
    true
),
(
    '67fbc597-1a98-4fca-b1a1-faa16ff80214',
    'YEONGCHEON_LEGAL_NOTICE',
    '영천 고시공고 목록',
    'GENERIC_TABLE',
    'table tbody tr:has(td.tit a[data-action*=notAncmtMgtNo])',
    'td.tit a[data-action*=notAncmtMgtNo]',
    'td.date',
    'td.tit a[data-action*=notAncmtMgtNo]',
    'yyyyMMdd',
    'AUTO', NULL, NULL, NULL, true
)
ON CONFLICT (profile_code) DO NOTHING;

WITH reviewed_source (
    public_code, notice_url, collection_endpoint_url, verification_note
) AS (
    VALUES
    ('LGS-000001', 'https://www.seoul.go.kr/news/news_notice.do?bbsId=001&bbsNo=277', NULL, '서울소식 | 서울특별시 및 공식 메뉴 링크'),
    ('LGS-000005', 'https://www.sd.go.kr/main/selectBbsNttList.do?bbsNo=184&key=1473&', NULL, '고시공고/입법예고 - 성동소식 - 열린성동 - 성동구 - 더불어 행복한 스마트포용도시 성동 - 게시판 목록 및 공식 메뉴 링크'),
    ('LGS-000010', 'https://child.gangbuk.go.kr/portal/bbs/B0000245/list.do?menuNo=200082', NULL, '고시공고(목록) | 강북소식 | 강북소개 | 포털사이트 및 공식 메뉴 링크'),
    ('LGS-000011', 'https://www.dobong.go.kr/Contents.asp?code=10008772', NULL, '열린행정>알림마당>고시/공고>고시/공고(목록) 및 공식 메뉴 링크'),
    ('LGS-000012', 'https://www.nowon.kr/www/user/bbs/BD_selectBbsList.do?q_bbsCode=1003&q_clCode=0&q_estnColumn1=11&q_ntceSiteCode=11', NULL, '노원구청 : 노원구청 > 노원소개 > 알림마당 > 고시공고 및 공식 메뉴 링크'),
    ('LGS-000014', 'https://www.sdm.go.kr/news/notice/notice.do', NULL, '서대문구청 구정소식 > 공고 > 고시공고 및 공식 메뉴 링크'),
    ('LGS-000015', 'https://www.mapo.go.kr/site/main/nPortal/list', NULL, '고시공고 | 고시공고 | 마포소식 | 마포구 소개 | 대표사이트 및 공식 메뉴 링크'),
    ('LGS-000018', 'https://www.guro.go.kr/www/selectBbsNttList.do?bbsNo=663&key=1791&', NULL, '고시공고 - 구로구청 및 공식 메뉴 링크'),
    ('LGS-000021', 'https://www.dongjak.go.kr/portal/bbs/B0001297/list.do?menuNo=201317', 'https://dongjak.eminwon.seoul.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시·공고(목록) < 새소식 < 우리동작 < 포털사이트 및 공식 메뉴 링크'),
    ('LGS-000024', 'https://www.gangnam.go.kr/notice/list.do?mid=ID05_040201', NULL, '강남구청 > 행정·정보 > 공고·입법예고 > 고시공고 | 공식 목록과 최근 공고 표본 확인'),
    ('LGS-000027', 'https://www.busan.go.kr/nbgosi', NULL, '부산소식 : 공고 : 고시공고 : 부산광역시 및 공식 메뉴 링크'),
    ('LGS-000028', 'https://www.bsjunggu.go.kr/index.junggu?menuCd=DOM_000000103001002000', 'https://eminwon.bsjunggu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '부산 중구청 대표 누리집 고시공고 메뉴와 새올 공개 목록'),
    ('LGS-000029', 'https://www.bsseogu.go.kr/index.bsseogu?menuCd=DOM_000000103001013000', 'https://eminwon.bsseogu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시공고 < 서구소식 < 정보공개 및 공식 메뉴 링크'),
    ('LGS-000030', 'https://www.bsdonggu.go.kr/index.donggu?menuCd=DOM_000000103001002000', 'https://eminwon.bsdonggu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시/공고 < 동구소식 < 정보공개 < 부산광역시 동구청 및 공식 메뉴 링크'),
    ('LGS-000031', 'https://www.yeongdo.go.kr/00000/00007/00013.web', NULL, '고시공고 | 영도구청 및 공식 메뉴 링크'),
    ('LGS-000032', 'https://www.busanjin.go.kr/index.busanjin?menuCd=DOM_000000110002001000', 'https://eminwon.busanjin.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '소통참여 > 공고 > 고시공고 | 부산 진구청 및 공식 메뉴 링크'),
    ('LGS-000033', 'https://www.dongnae.go.kr/index.dongnae?menuCd=DOM_000000103001003000', 'https://eminwon.dongnae.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '행정정보 < 동래소식 < 고시/공고 및 공식 메뉴 링크'),
    ('LGS-000035', 'https://www.bsbukgu.go.kr/index.bsbukgu?menuCd=DOM_000000105001005000', 'https://eminwon.bsbukgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '정보공개 < 북구소식 < 고시/공고 및 공식 메뉴 링크'),
    ('LGS-000036', 'https://www.haeundae.go.kr/index.do?menuCd=DOM_000000104001002000', 'https://eminwon.haeundae.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시공고 < 새소식 < 공개 < 해운대구청 및 공식 메뉴 링크'),
    ('LGS-000037', 'http://www.saha.go.kr/portal/contents.do?mId=0301030000', 'http://eminwon.saha.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시공고 | 사하알림 | 정보공개 | 부산광역시 사하구 및 공식 메뉴 링크'),
    ('LGS-000038', 'https://www.geumjeong.go.kr/index.geumj?menuCd=DOM_000000124002003000', 'https://eminwon.geumjeong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '( 고시공고 ) | 공지/입찰/고시 | 정보공개 | 부산광역시 금정구청 및 공식 메뉴 링크'),
    ('LGS-000039', 'https://www.bsgangseo.go.kr/portal/contents.do?mid=0501020000', 'https://eminwon.bsgangseo.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시공고 | 행정알림 | 정보공개 | 부산 강서구청 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000040', 'https://www.yeonje.go.kr/portal/contents.do?mId=0206030000', NULL, '고시/공고 | 알림마당 | 연제구소개 및 공식 메뉴 링크'),
    ('LGS-000041', 'https://www.suyeong.go.kr/index.suyeong?menuCd=DOM_000000103001002000', 'https://eminwon.suyeong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '게재기간중인글 | 고시·공고 | 수영소식 |알림마당 |부산광역시 수영구청 및 공식 메뉴 링크'),
    ('LGS-000042', 'https://www.sasang.go.kr/index.sasang?menuCd=DOM_000000101003001000', 'https://eminwon.sasang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '사상구청 대표 누리집 고시공고 메뉴와 새올 공개 목록'),
    ('LGS-000043', 'https://www.gijang.go.kr/index.gijang?menuCd=DOM_000000101001002000', 'https://eminwon.gijang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '기장군 > 정보공개 > 행정알림 > 고시/공고 및 공식 메뉴 링크'),
    ('LGS-000044', 'https://www.daegu.go.kr/index.do?menu_id=00940170', NULL, '대구광역시 정보공개 알림정보 고시공고 목록'),
    ('LGS-000046', 'https://www.dong.daegu.kr/portal/contents.do?mid=0201020000', NULL, '공고/고시 목록 | 구정소식 | 소통참여 | 대구광역시 동구청 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000047', 'https://www.dgs.go.kr/portal/contents.do?mid=0601020000', NULL, '게재기간중인 자료보기 | 고시공고 | 서구소식 | 소통참여 | 대구 서구청 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000048', 'https://nam.daegu.kr/index.do?menu_id=00000851', 'https://eminwon.nam.daegu.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '소통/참여 > 알림마당 > 고시/공고 | 대구광역시 남구청 및 공식 메뉴 링크'),
    ('LGS-000049', 'https://www.buk.daegu.kr/index.do?menu_id=00000198', 'https://eminwon.buk.daegu.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '대구광역시 북구 > 소통참여 > 알림광장 > 고시/공고 및 공식 메뉴 링크'),
    ('LGS-000050', 'https://www.suseong.kr/index.do?menu_id=00000064', 'https://eminwon.suseong.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시/공고 | 수성구청 - 품격있는 사람 배려하는 도시 행복수성 및 공식 메뉴 링크'),
    ('LGS-000051', 'https://dalseo.daegu.kr/index.do?menu_id=10000104', 'https://eminwon.dalseo.daegu.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시/공고 | 대구광역시 달서구 및 공식 메뉴 링크'),
    ('LGS-000053', 'https://www.gunwi.go.kr/ko/page.do?mnu_uid=666&boardType=notice', NULL, '고시공고 | 군정소식 | 군위군청 및 공식 메뉴 링크'),
    ('LGS-000054', 'http://announce.incheon.go.kr/citynet/jsp/sap/SAPGosiBizProcess.do?command=searchList&flag=gosiGL&svp=Y&sido=ic', NULL, '고시/공고 조회 및 공식 메뉴 링크'),
    ('LGS-000055', 'https://www.jemulpo.go.kr/main/information/news/announce.jsp', NULL, '고시공고 목록 | 제물포구>정보공개>소식/알림>고시공고 및 공식 메뉴 링크'),
    ('LGS-000056', 'https://www.yeongjong.go.kr/main/pst/list.do?pst_id=mn_pub_ntc', 'https://eminwon.yeongjong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '인천광역시 영종구청 - 고시공고 및 공식 메뉴 링크'),
    ('LGS-000057', 'https://www.michuhol.go.kr/main/board/list.do?board_code=board_13', NULL, '인천광역시 미추홀구 - 고시/공고 및 공식 메뉴 링크'),
    ('LGS-000058', 'https://www.yeonsu.go.kr/main/community/notify/gosi.asp', 'https://eminwon.yeonsu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시/공고 | 참여·알림 알림광장 고시/공고 | 연수구청 및 공식 메뉴 링크'),
    ('LGS-000060', 'https://www.icbp.go.kr/main/participation/news/announce.jsp', NULL, '고시/공고/입법예고 목록 | 인천광역시 부평구청>소통과 참여>부평알림 및 공식 메뉴 링크'),
    ('LGS-000061', 'https://www.gyeyang.go.kr/open_content/main/open_info/admin/gosi.jsp', NULL, '고시/공고 목록 | 계양구청>정보공개>행정소식 및 공식 메뉴 링크'),
    ('LGS-000064', 'https://www.ganghwa.go.kr/open_content/main/ganghwa/news/announce.jsp', NULL, '고시공고 목록 | 강화군청>강화소개>강화소식>고시공고 및 공식 메뉴 링크'),
    ('LGS-000065', 'https://www.ongjin.go.kr/open_content/main/community/board/announce.jsp', NULL, '고시공고 목록 | main>함께하는군정>알림마당 및 공식 메뉴 링크'),
    ('LGS-000066', 'https://www.donggu.kr/menu.es?mid=a10101030100', 'https://eminwon.donggu.gwangju.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시공고 | 고시공고 | 알림마당 | 소통/참여 : 인문도시 광주동구 전남광주통합특별시 동구 및 공식 메뉴 링크'),
    ('LGS-000067', 'https://www.seogu.gwangju.kr/menu.es?mid=a10807010000', NULL, '목록 | 현재 고시/공고 | 고시/공고 | 구정소식 : 전남광주통합특별시 서구청 #착한도시 서구 및 공식 메뉴 링크'),
    ('LGS-000068', 'https://www.namgu.gwangju.kr/menu.es?mid=a10604020100', NULL, '고시공고 | 고시공고 | 알림마당 | 우리남구 : 전남광주통합특별시 남구 GWANGJU NAMGU 및 공식 메뉴 링크'),
    ('LGS-000069', 'https://bukgu.gwangju.kr/menu.es?mid=a10201050100', 'https://eminwon.bukgu.gwangju.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?not_ancmt_se_code=01,03,04&jndinm=OfrNotAncmtEJB&context=NTIS&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&ofr_pageSize=10&homepage_pbs_yn=Y&subCheck=Y&yyyy=2015', '입법/고시/공고 | 입법/고시/공고 | 알림마당 | 소통광장 : 전남광주통합특별시 북구 및 공식 메뉴 링크'),
    ('LGS-000070', 'https://www.gwangsan.go.kr/notList.do?pageId=www12&searchNotSe=01', NULL, '고시/공고/입법 | 광산구청 및 공식 메뉴 링크'),
    ('LGS-000071', 'https://www.daejeon.go.kr/drh/MediaList.do?notiType=NOTI_06&menuSeq=2564', NULL, '고시공고 | 대전광역시청 및 공식 메뉴 링크'),
    ('LGS-000072', 'https://www.donggu.go.kr/dg/kor/contents/916', 'https://eminwon.donggu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '대전 동구청 > 동구소식 > 고시공고 > 고시공고 | 공식 메뉴와 새올 공개 목록 확인'),
    ('LGS-000073', 'https://www.djjunggu.go.kr/prog/saeolGosi/GOSI/sub03_06/list.do', NULL, '고시/공고 >중구소식 > 대전광역시 중구청 및 공식 메뉴 링크'),
    ('LGS-000074', 'https://www.seogu.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/list.do', NULL, '고시공고 > 알림마당 > 소식알림 > 대전광역시 서구청 및 공식 메뉴 링크'),
    ('LGS-000075', 'https://www.yuseong.go.kr/prog/saeolGosi/GOSI/kor/sub04_02_01/list.do', NULL, '고시·공고 > 고시·공고 > 소식 > 유성구청 및 공식 메뉴 링크'),
    ('LGS-000076', 'https://www.daedeok.go.kr/dpt/dpt04/DPT040204_cmmBoardList.do', NULL, '대덕구청 구정소식 고시공고 목록'),
    ('LGS-000077', 'https://www.ulsan.go.kr/u/rep/contents.ulsan?mId=001004002000000000', NULL, '울산광역시 대표누리집 > 시정소식 > 고시공고(입법예고 포함) 및 공식 메뉴 링크'),
    ('LGS-000078', 'https://www.junggu.ulsan.kr/index.ulsan?menuCd=DOM_000000102004001000', 'https://eminwon.junggu.ulsan.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '울산 중구청 고시공고 메뉴와 페이지 제목'),
    ('LGS-000079', 'https://www.ulsannamgu.go.kr/cop/bbs/selectSaeolGosiList.do', 'https://eminwon.ulsannamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시공고 | 울산광역시 남구청 및 공식 메뉴 링크'),
    ('LGS-000080', 'https://www.donggu.ulsan.kr/donggu/dongguNews/gosi/contents.do', 'https://eminwon.donggu.ulsan.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '울산광역시 동구청 | 동구소식 > 고시/공고 > 고시공고 및 공식 메뉴 링크'),
    ('LGS-000081', 'https://www.bukgu.ulsan.kr/lay1/S1T1903C86/sublink.do', 'https://eminwon.bukgu.ulsan.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '홈 >구정소식>공고>고시공고>고시공고 및 공식 메뉴 링크'),
    ('LGS-000082', 'https://www.ulju.ulsan.kr/ulju/contents.do?mId=0403010000', NULL, '고시공고 | 고시공고 | 열린군정 | 울산광역시 울주군청 대표누리집 및 공식 메뉴 링크'),
    ('LGS-000083', 'https://www.sejong.go.kr/prog/publicNotice/kor/sub02_030301/C1_1/list.do;jsessionid=F7A34374C9F611A4D88FEFBCAC3BEBF5.portal1', NULL, '일반공고 > 고시/공고 > 세종소식 > 세종소개 및 공식 메뉴 링크'),
    ('LGS-000085', 'http://www.suwon.go.kr/web/saeallOfr/BD_ofrList.do', NULL, '공고/고시/입법예고 : HOME > 수원소식 > 공고/고시/입법예고 및 공식 메뉴 링크'),
    ('LGS-000086', 'https://www.yongin.go.kr/home/yiNw/yiNwStable/yiNwStable02/yiNwStable02_01.jsp', 'https://eminwon.yongin.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '용인시청 > 용인소식 > 입법예고·공고 > 고시공고 | 공식 메뉴와 새올 공개 목록 확인'),
    ('LGS-000087', 'https://eminwon.goyang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&countYn=Y&epcCheck=Y&homepage_pbs_yn=Y&initValue=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04%2C05&ofr_pageSize=10&subCheck=Y&title=%EA%B3%A0%EC%8B%9C%EA%B3%B5%EA%B3%A0&yyyymmdd=', NULL, '고양시 새올 공개 고시공고 목록'),
    ('LGS-000088', 'https://www.hscity.go.kr/www/gosi/BD_notice.do', NULL, '화성특례시청 > 행정정보 > 공고고시 > 고시 및 공식 메뉴 링크'),
    ('LGS-000090', 'https://www.bucheon.go.kr/site/program/gosi/list?menuid=148002003001', NULL, '고시·공고·입법예고 목록 | 공고·입법예고 | 부천소식 | 부천시청 및 공식 메뉴 링크'),
    ('LGS-000091', 'https://www.nyj.go.kr/www/selectEminwonWebList.do?key=2492&sa1=01&sa1=02&sa1=04&sa1=05&sc4=2024', NULL, '고시공고 - 고시/공고 - 남양주소식 - 남양주시청 및 공식 메뉴 링크'),
    ('LGS-000092', 'https://www.ansan.go.kr/www/common/bbs/selectPageListBbs.do?bbs_code=WWW13', NULL, '시민과 함께 자유로운 혁신도시 안산 및 공식 메뉴 링크'),
    ('LGS-000093', 'https://www.pyeongtaek.go.kr/pyeongtaek/contents.do?mid=0401020000', NULL, '고시공고 | 고시공고 | 시정소식 | 알림마당 | 평택시 및 공식 메뉴 링크'),
    ('LGS-000095', 'http://www.siheung.go.kr/main/contents.do?mId=0401040000', NULL, '고시/공고 목록 목록 | 고시/공고 | 시정정보 | 시정소식 | 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000096', 'https://www.paju.go.kr/user/board/BD_board.list.do?bbsCd=1022&q_ctgCd=4063', NULL, '고시공고(목록) : HOME > 공고·홍보 > 공고·입법예고 > 고시공고 및 공식 메뉴 링크'),
    ('LGS-000097', 'https://www.gimpo.go.kr/portal/ntfcPblancList.do?key=1004&cate_cd=1&searchCnd=40900000000', NULL, '고시공고목록 - 김포시청 및 공식 메뉴 링크'),
    ('LGS-000098', 'https://www.ui4u.go.kr/portal/contents.do?mId=0301040000', NULL, '고시/공고 | 알림마당 | 시정소식 | 의정부시청 대표 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000099', 'https://www.gjcity.go.kr/portal/contents.do?mId=0202010000', NULL, '고시공고 | 공고 | 광주소식 | 광주시청 및 공식 메뉴 링크'),
    ('LGS-000100', 'https://www.hanam.go.kr/www/selectGosiList.do?key=171&not_ancmt_se_code=01,04', NULL, '고시공고 - 하남시청 및 공식 메뉴 링크'),
    ('LGS-000101', 'http://www.yangju.go.kr/www/selectEminwonList.do?key=4075', NULL, '고시공고 목록 - 시정소식 - 열린시정 - 양주시청 및 공식 메뉴 링크'),
    ('LGS-000102', 'http://www.gm.go.kr/pt/user/nftcBbs/BD_selectNftcBbsList.do?q_nftcBbsCode=1001', NULL, '고시공고 (목록) > 광명시청 > 뉴스/정보공개 > 새소식 > 고시/공고/입법예고 > 고시공고 및 공식 메뉴 링크'),
    ('LGS-000103', 'https://www.gunpo.go.kr/www/selectEminwonNoticeList.do?key=3907&Not_ancmt_se_code=01&list_gubun=N&ofr_pageSize=10&notAncmtSeCd=01&pageUnit=10', NULL, '고시공고 - 군포시청 및 공식 메뉴 링크'),
    ('LGS-000105', 'https://www.icheon.go.kr/portal/contents.do?mid=0402000000', NULL, '고시 목록 | 고시/공고 | 이천소식 | 이천시청 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000106', 'https://www.anseong.go.kr/portal/saeol/gosiList.do?mId=0501040000', NULL, '안성시청 고시공고 서버 렌더링 목록'),
    ('LGS-000107', 'https://www.guri.go.kr/www/selectGosiNttList.do?key=387&searchGosiSe=01,04,06', NULL, '구리소식 > 고시/공고 - 구리시청 및 공식 메뉴 링크'),
    ('LGS-000109', 'https://www.uiwang.go.kr/UWKORINFO0701', 'https://eminwon.uiwang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시/공고 | 의왕시 및 공식 메뉴 링크'),
    ('LGS-000110', 'https://www.yp21.go.kr/www/selectBbsNttList.do?bbsNo=5&key=1119', NULL, '고시/공고 - 양평군청 및 공식 메뉴 링크'),
    ('LGS-000111', 'https://www.yeoju.go.kr/www/selectEminwonList.do?key=413', NULL, '고시·공고·입법예고 목록 - 여주시청 및 공식 메뉴 링크'),
    ('LGS-000112', 'http://www.ddc.go.kr/ddc/selectGosiList.do?key=340&not_ancmt_se_code=04', NULL, '일반공고 - 대표 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000113', 'https://www.gccity.go.kr/portal/contents.do?mId=0301040000', NULL, '고시/공고 | 알림마당 | 과천소식 | 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000114', 'https://www.gp.go.kr/portal/selectGosiList.do?key=2148&not_ancmt_se_code=01', NULL, '고시공고 -가평군청 및 공식 메뉴 링크'),
    ('LGS-000115', 'https://www.yeoncheon.go.kr/www/selectGosiList.do?key=3393&not_ancmt_se_code=01', NULL, '연천군 고시/공고 목록 - 연천군청 및 공식 메뉴 링크'),
    ('LGS-000116', 'https://state.gwd.go.kr/portal/bulletin/notification', NULL, '공고/고시 목록 - 도정마당 | 강원특별자치도청 - 새로운 강원! 특별 자치시대! 및 공식 메뉴 링크'),
    ('LGS-000117', 'https://www.chuncheon.go.kr/cityhall/administrative-info/notice-info/notice-announcement/', NULL, '고시/공고 | 춘천시청 및 공식 메뉴 링크'),
    ('LGS-000118', 'https://www.wonju.go.kr/www/selectBbsNttList.do?bbsNo=140&key=216&', NULL, '원주시 공고(목록) - 공고/고시 - 원주소식 - 원주시청 및 공식 메뉴 링크'),
    ('LGS-000121', 'http://www.taebaek.go.kr/www/selectBbsNttList.do?bbsNo=25&key=352', NULL, '공고/고시 목록 - 알림마당 - 시정소식 - 태백시청 및 공식 메뉴 링크'),
    ('LGS-000123', 'https://www.samcheok.go.kr/media/00084/00095.web', NULL, '입법/공고/고시 | 삼척시청 및 공식 메뉴 링크'),
    ('LGS-000124', 'https://www.hongcheon.go.kr/www/selectEminwonList.do?key=278', NULL, '고시공고 목록 - 고시/공고 - 소식·알림 - 홍천군청 및 공식 메뉴 링크'),
    ('LGS-000125', 'https://www.hsg.go.kr/www/selectBbsNttList.do?bbsNo=65&key=821&', NULL, '고시공고 - 횡성군청 및 공식 메뉴 링크'),
    ('LGS-000126', 'https://www.yw.go.kr/www/selectBbsNttList.do?bbsNo=17&key=273', NULL, '대표홈페이지 - 고시/공고 목록 및 공식 메뉴 링크'),
    ('LGS-000127', 'https://www.pc.go.kr/portal/government/government-notification', NULL, '고시/공고 | 평창군청 > 열린 평창군정 > 고시/공고 및 공식 메뉴 링크'),
    ('LGS-000128', 'https://www.jeongseon.go.kr/portal/openadmin/adminnews/notification', 'https://eminwon.jeongseon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '공고/고시 | 정선군청 > 열린군정 > 군정소식 > 공고/고시 및 공식 메뉴 링크'),
    ('LGS-000129', 'https://www.cwg.go.kr/www/selectBbsNttList.do?bbsNo=25&key=1226', NULL, '고시/공고 - 철원군청 및 공식 메뉴 링크'),
    ('LGS-000131', 'https://www.yanggu.go.kr/user_sub?gfnc=www&mu_idx=226', NULL, '양구군청 군정소식 고시공고 목록과 최신 게시물 표본'),
    ('LGS-000132', 'https://www.inje.go.kr/portal/adm/bulletin', NULL, '고시공고 목록 | 인제군청 > 행정정보 > 고시공고 및 공식 메뉴 링크'),
    ('LGS-000134', 'https://www.yangyang.go.kr/gw/portal/yyc_news_notifi', 'https://eminwon.yangyang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '공고/고시 | 양양군청 > 군정소식 > 공고/고시 및 공식 메뉴 링크'),
    ('LGS-000136', 'https://www.cheongju.go.kr/www/selectEminwonNoticeList.do?key=281', NULL, '고시공고 < 새소식 < 시정소식 - 청주시청 및 공식 메뉴 링크'),
    ('LGS-000137', 'https://www.chungju.go.kr/www/selectEminwonList.do?key=510&ofr_pageSize=10&ancmt_se_code=01,02,04,05&pageIndex=1', NULL, '충주시청 공고 고시 입찰 새올 목록'),
    ('LGS-000138', 'https://www.jecheon.go.kr/www/selectBbsNttList.do?bbsNo=18&key=5233', NULL, '고시공고 게시판(목록) 및 공식 메뉴 링크'),
    ('LGS-000139', 'https://www.boeun.go.kr/www/selectBbsNttList.do?bbsNo=66&key=194', NULL, '고시/공고 - 보은군청 및 공식 메뉴 링크'),
    ('LGS-000140', 'https://www.oc.go.kr/www/selectBbsNttList.do?bbsNo=40&key=236&', NULL, '고시/공고 - 옥천군청 및 공식 메뉴 링크'),
    ('LGS-000141', 'https://www.yd21.go.kr/kr/html/sub02/020103.html?GotoPage=1&mode=L', NULL, '목록 > 고시공고 > 군정소식 > 소식 &middot; 참여 > 영동군청 및 공식 메뉴 링크'),
    ('LGS-000142', 'https://www.jp.go.kr/kor/sub03_01_03.do', 'https://eminwon.jp.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시공고 < 모집/공고 < 군정소식 < 증평군청 및 공식 메뉴 링크'),
    ('LGS-000143', 'https://www.jincheon.go.kr/home/sub.do?menukey=235', 'https://eminwon.jincheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', ' 및 공식 메뉴 링크'),
    ('LGS-000144', 'https://www.goesan.go.kr/www/contents.do?key=1438', 'https://eminwon.goesan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '공공정보 > 고시/공고/입법예고 > 고시/공고/입법예고 > 전체 > 괴산군청 및 공식 메뉴 링크'),
    ('LGS-000146', 'https://www.danyang.go.kr/dy21/976', 'https://eminwon.danyang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시공고 < 알림마당 < 열린마당 : 단양군 및 공식 메뉴 링크'),
    ('LGS-000147', 'https://www.chungnam.go.kr/cnportal/province/province/list.do?menuNo=500487', NULL, '충청남도 도 및 산하기관 공고 고시 목록'),
    ('LGS-000148', 'https://www.cheonan.go.kr/prog/saeolGosi/GOSI/kor/sub02_02_01/list.do', NULL, '공고/고시 < 공고알림 < 소식알림 < 천안시청 및 공식 메뉴 링크'),
    ('LGS-000149', 'https://www.gongju.go.kr/prog/saeolGosi/GOSI_03/sub04_03_03/list.do', NULL, '일반공고 > 고시공고 > 시정소식 > 공주시청 및 공식 메뉴 링크'),
    ('LGS-000150', 'http://www.brcn.go.kr/prog/eminwon/kor/BB/sub07_01_05/list.do', NULL, '[ 고시/공고 < 기획감사실 < 실과별누리집 < 보령시청 ] 및 공식 메뉴 링크'),
    ('LGS-000151', 'https://www.asan.go.kr/main/cms/?no=257', NULL, '고시공고 | 아산시청 및 공식 메뉴 링크'),
    ('LGS-000152', 'https://www.seosan.go.kr/www/contents.do?key=1258', 'https://www.seosan.go.kr/common/program/eminwonView.jsp?pageIndex=&jndinm=OfrNotAncmtEJB&context=NTIS&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&homepage_pbs_yn=Y&subCheck=Y&ofr_pageSize=10&not_ancmt_se_code=01%2C04%2C06&title=%EA%B3%A0%EC%8B%9C%EA%B3%B5%EA%B3%A0&cha_dep_code_nm=&initValue=&countYn=Y&list_gubun=&not_ancmt_sj=&not_ancmt_cn=&dept_nm=&mobile_code=00&Key=B_Subject&not_ancmt_mgt_no=&temp=', '공고/고시 - 서산시청 및 공식 메뉴 링크'),
    ('LGS-000153', 'https://www.nonsan.go.kr/kor/html/sub03/03010201.html', 'https://eminwon.nonsan21.net/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '공고/고시 > 공고/고시 > 시정소식 > 소식&정보 > 논산시청 및 공식 메뉴 링크'),
    ('LGS-000154', 'https://www.gyeryong.go.kr/kr/html/sub03/030102.html', NULL, '목록 > 고시/공고 > 계룡소식 > 소통/참여 > 계룡시청 및 공식 메뉴 링크'),
    ('LGS-000155', 'https://www.dangjin.go.kr/kor/sub03_02_01_01.do', 'https://eminwon.dangjin.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '당진시청 > 소식 정보 >공고 알림 >고시/공고 > 및 공식 메뉴 링크'),
    ('LGS-000156', 'https://www.geumsan.go.kr/kr/html/sub03/030302.html', NULL, '금산군 목록 > 고시/공고 > 군정알리미 > 소통참여 > 대표 및 공식 메뉴 링크'),
    ('LGS-000157', 'https://www.buyeo.go.kr/html/kr/news/news_040202.html', NULL, '고시공고 > 고시/공고 > 군정소식 > 부여군청 및 공식 메뉴 링크'),
    ('LGS-000158', 'https://www.seocheon.go.kr/prog/saeolGosi/03/kor/sub04_06_03/list.do', 'https://eminwon.seocheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '서천군청 > 서천소식 > 고시·공고 > 일반공고 | 공식 메뉴와 새올 공개 목록 확인'),
    ('LGS-000159', 'https://www.cheongyang.go.kr/kor/sub04_02_03.do', 'https://eminwon.cheongyang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '[ 고시/공고 > 고시/공고 > 군정소식 > ] 및 공식 메뉴 링크'),
    ('LGS-000160', 'https://www.hongseong.go.kr/prog/saeolGosi/kor/sub03_0204/GOSI_ALL/list.do', NULL, '공고/고시 > 군정소식 > 군정소식 > 홍성군청 및 공식 메뉴 링크'),
    ('LGS-000161', 'https://www.yesan.go.kr/prog/saeolGosi/GOSI/kor/sub04_03_01/list.do', NULL, '고시·공고 < 공고 < 예산소식 < 예산군청 및 공식 메뉴 링크'),
    ('LGS-000162', 'https://www.taean.go.kr/kor/sub02_03_01.do', 'https://eminwon.taean.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '소통/참여 >고시/공고 >고시공고> 태안군청 및 공식 메뉴 링크'),
    ('LGS-000163', 'https://www.jeonbuk.go.kr/index.jeonbuk?menuCd=DOM_000000102002000000', NULL, '전북소식 > 공고/고시 > 전북특별자치도 > 목록 | 전북특별자치도 및 공식 메뉴 링크'),
    ('LGS-000164', 'https://www.jeonju.go.kr/index.9is?contentUid=ff8080818990c349018b041a879f395a', NULL, '전주시 대표사이트 > 전주소식 > 공고 > 고시/공고(목록) 및 공식 메뉴 링크'),
    ('LGS-000165', 'http://eminwon.gunsan.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,02,03,04,05', NULL, '고시공고 및 공식 메뉴 링크'),
    ('LGS-000166', 'https://eminwon.iksan.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,02,03,04,05&cpath=', NULL, '고시공고 및 공식 메뉴 링크'),
    ('LGS-000167', 'http://eminwon.jeongeup.go.kr/emwp/jsp/ofr/OfrNotAncmtL.jsp?not_ancmt_se_code=01,02,03,04,05', NULL, '고시공고 및 공식 메뉴 링크'),
    ('LGS-000168', 'https://www.namwon.go.kr/index.do?menuUid=ff8080818e3beff0018e4077131b007a', NULL, '남원시 대표 누리집 > 소통과참여 > 시정소식 > 고시공고 및 공식 메뉴 링크'),
    ('LGS-000169', 'https://www.gimje.go.kr/index.gimje?menuCd=DOM_000000104003000000', NULL, '김제소식 > 고시/공고 - 목록 | 김제시청 및 공식 메뉴 링크'),
    ('LGS-000170', 'https://www.wanju.go.kr/index.9is?contentUid=ff8080818b024d8e018b274f41c32af7', 'https://eminwon.wanju.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '완주군 > 소통참여 > 알림정보 > 고시공고 > 고시공고 및 공식 메뉴 링크'),
    ('LGS-000171', 'https://www.jinan.go.kr/index.jinan?menuCd=DOM_000000107001014000', 'https://eminwon.jinan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '진안군청 : 소통/참여 > 군정소식 > 공고/고시(행정) 및 공식 메뉴 링크'),
    ('LGS-000172', 'https://www.muju.go.kr/index.9is?contentUid=ff8080816c5f9d47016cbd3b2a4a006f', 'https://eminwon.muju.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '무주군청 > 알림마당 > 무주소식 > 고시공고 및 공식 메뉴 링크'),
    ('LGS-000173', 'https://www.jangsu.go.kr/index.jangsu?menuCd=DOM_000000102001005000', 'https://eminwon.jangsu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '소통참여 > 장수소식 > 고시·공고·예고 | 장수군청 및 공식 메뉴 링크'),
    ('LGS-000174', 'https://www.imsil.go.kr/index.imsil?menuCd=DOM_000000103001005000', 'https://eminwon.imsil.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '소통·참여 > 군정소식 > 고시/공고 및 공식 메뉴 링크'),
    ('LGS-000175', 'https://eminwon.sunchang.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,02,03,04,05', NULL, '순창고시공고 및 공식 메뉴 링크'),
    ('LGS-000176', 'http://www.gochang.go.kr/index.gochang?menuCd=DOM_000000102003000000', NULL, '소통·참여 > 고시/공고 > 고시공고 목록 : 고창군 및 공식 메뉴 링크'),
    ('LGS-000177', 'https://www.buan.go.kr/index.buan?menuCd=DOM_000000103001003000', NULL, '소통/참여 > 부안소식 > 고시·공고·입법예고 목록 페이지 및 공식 메뉴 링크'),
    ('LGS-000178', 'https://www.mokpo.go.kr/www/mokpo_news/notification', NULL, '1 페이지 목록보기 < 고시/공고 < 고시/공고 < 열린시정 - 목포시 및 공식 메뉴 링크'),
    ('LGS-000179', 'https://www.yeosu.go.kr/www/govt/news/notify', NULL, '1 페이지 목록보기 < (전체) < 여수시 < 고시공고 < 고시공고 < 여수소식 < 열린시정 - 여수시청 및 공식 메뉴 링크'),
    ('LGS-000180', 'https://www.suncheon.go.kr/kr/news/0004', NULL, '전체-순천시청 및 공식 메뉴 링크'),
    ('LGS-000181', 'https://www.naju.go.kr/www/administration/notice/gosi_new', NULL, '1 페이지 목록보기 < 고시/공고/입법예고 < 공고 < 열린시정 - 나주시청 및 공식 메뉴 링크'),
    ('LGS-000182', 'https://gwangyang.go.kr/menu.es?mid=a10909010000', NULL, '고시 | 고시/공고 | 열린혁신 정보공개 : 대한민국 산업수도 광양, 전남광주통합특별시 광양시 소개, 전자민원, 정보공개, 뉴스소식, 시민참여, 분야별정보, 문화관광 등 주요 행정서비스를 제공합니다. 및 공식 메뉴 링크'),
    ('LGS-000183', 'https://www.damyang.go.kr/eminwon/searchList?domainId=DOM_0000001&contentsSid=2&menuCd=DOM_000000190001002001&boardType=special&listType=01', NULL, '담양군청 대표 누리집이 연결하는 고시공고 목록'),
    ('LGS-000184', 'https://www.gokseong.go.kr/board/GosiList.do?menuNo=102001003000', NULL, '열린군정 > 곡성소식 > 고시공고 및 공식 메뉴 링크'),
    ('LGS-000185', 'https://www.gurye.go.kr/board/GosiList.do?not_ancmt_se_code=01,04,06,07&menuNo=115004002001&pageIndex=1', NULL, '고시공고 < 고시공고 < 구례소식 < 행정정보 구례군청 및 공식 메뉴 링크'),
    ('LGS-000186', 'https://www.goheung.go.kr/contentsView.do?pageId=www99', NULL, '고시/공고 | 고흥군청 및 공식 메뉴 링크'),
    ('LGS-000187', 'https://www.boseong.go.kr/www/open_administration/city_news/notification', NULL, '1 페이지 목록보기 < 고시 ·공고 < 새소식 < 열린행정 - 보성군청 및 공식 메뉴 링크'),
    ('LGS-000188', 'https://www.hwasun.go.kr/contents.do?S=S01&M=020104000000', NULL, '고시공고 및 공식 메뉴 링크'),
    ('LGS-000189', 'https://www.jangheung.go.kr/www/organization/news/notification', NULL, '1 페이지 목록보기 < (전체) < 고시공고 < 새소식 < 군정정보 - 장흥군청 및 공식 메뉴 링크'),
    ('LGS-000190', 'https://www.gangjin.go.kr/www/government/notice/gosi', NULL, '1 페이지 목록보기 < 고시/공고/입법예고 < 공고 < 행정정보 - 강진군청 및 공식 메뉴 링크'),
    ('LGS-000191', 'https://www.haenam.go.kr/index.9is?contentUid=18e3368f5d745106015de95f1ccd205c', NULL, '해남군청 > 고시공고(새올) 및 공식 메뉴 링크'),
    ('LGS-000192', 'https://www.yeongam.go.kr/home/www/open_information/yeongam_news/announcement/yeongam.go', NULL, '고시/공고 < 고시/공고 < 영암소식 < 열린군정 < 영암군 및 공식 메뉴 링크'),
    ('LGS-000193', 'https://www.muan.go.kr/www/openmuan/new/announcement', NULL, '1 페이지 목록보기 < 고시공고 < 알림마당 < 행정공개 - 무안군청 및 공식 메뉴 링크'),
    ('LGS-000194', 'https://www.hampyeong.go.kr/pg/GosiList.do?pageId=www273', NULL, '고시공고/입법예고 | 함평군 대표 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000195', 'https://www.yeonggwang.go.kr/bbs/?b_id=gosigonggo&site=headquarter_new&mn=9059', NULL, '고시/공고>군정소식>열린군정>영광군청 및 공식 메뉴 링크'),
    ('LGS-000196', 'https://www.jangseong.go.kr/home/www/news/jangseong/announcement', NULL, '고시/공고 < 장성소식 < 뉴스·소식 < 장성군청 및 공식 메뉴 링크'),
    ('LGS-000197', 'https://www.wando.go.kr/wando/sub.cs?m=318', NULL, '고시공고<행정정보<군정정보<완도대표홈페이지 및 공식 메뉴 링크'),
    ('LGS-000198', 'https://www.jindo.go.kr/home/gosi/general.cs?m=878', NULL, '고시·공고 : 진도군청 및 공식 메뉴 링크'),
    ('LGS-000199', 'https://www.shinan.go.kr/home/www/openinfo/participation_07/participation_07_04/page.wscms', NULL, '고시/공고 < 신안군뉴스 < 열린군정 < 신안군청 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000200', 'https://www.gb.go.kr/Main/page.do?mnu_uid=6789&BD_CODE=gosi_notice', NULL, '경상북도 도정소식 고시공고 목록'),
    ('LGS-000202', 'https://www.gyeongju.go.kr/open_content/ko/page.do?mnu_uid=2912&', NULL, '고시/공고 | 고시/공고 | 경주소식 | 경주시 - Golden City ( Beautiful Gyeongju ) 및 공식 메뉴 링크'),
    ('LGS-000203', 'https://www.gc.go.kr/portal/contents.do?mId=1202180000', NULL, '고시공고목록 | 고시공고 | 시정현황 | 김천시정 | 누리집 및 공식 메뉴 링크'),
    ('LGS-000205', 'https://www.gumi.go.kr/portal/contents.do?mid=0401040000', NULL, '고시ㆍ공고ㆍ입법 목록 | 행정 알림 | 행정 정보 | 구미시청 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000206', 'https://www.yeongju.go.kr/open_content/main/page.do?mnu_uid=10619&boardType=notice', NULL, '고시/공고 < 알림마당 < 정보공개 < 영주시청 및 공식 메뉴 링크'),
    ('LGS-000207', 'https://www.yc.go.kr/portal/contents.do?mId=0301040000', NULL, '고시/공고 | 시정소식 | 정보공개/개방 | 영천시청 대표포털 및 공식 메뉴 링크'),
    ('LGS-000208', 'https://www.sangju.go.kr/page/10297/10606.tc', NULL, '시민광장 > 시소식 > 고시/공고 | 상주시 및 공식 메뉴 링크'),
    ('LGS-000209', 'https://www.gbmg.go.kr/portal/contents.do?mId=0301060000', NULL, '고시/공고 | 문경소식 | 소식알리미 | 문경시청 및 공식 메뉴 링크'),
    ('LGS-000210', 'https://www.gbgs.go.kr/open_content/ko/page.do?mnu_uid=2160&', NULL, '고시/공고 > 고시공고 > 시정알림 > 경산시청 및 공식 메뉴 링크'),
    ('LGS-000211', 'https://www.usc.go.kr/ko/page.do?mnu_uid=157&boardType=notice', NULL, '고시/공고 | 소통참여 | 의성군 및 공식 메뉴 링크'),
    ('LGS-000212', 'https://www.cs.go.kr/news/00002679/00006203.web', NULL, '청송군청 알림광장 군정게시판 고시공고 목록'),
    ('LGS-000213', 'https://www.yyg.go.kr/www/organization/yyg_news/notification', NULL, '영양군청 군정정보 고시 공고 입찰 목록'),
    ('LGS-000214', 'https://www.yd.go.kr/?page_id=763', NULL, '고시/공고 및 공식 메뉴 링크'),
    ('LGS-000215', 'https://www.cheongdo.go.kr/portal/contents.do?mid=0301020000', NULL, '목록 | 고시/공고 | 행정소식 | 행정정보 | 청도군청 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000216', 'http://www.goryeong.go.kr/kor/boardList.do?IDX=154&BRD_ID=1023', NULL, '고시/공고 : 고령군 및 공식 메뉴 링크'),
    ('LGS-000217', 'https://www.sj.go.kr/page.do?mnu_uid=1044&', NULL, '고시, 공고, 입법예고 > 성주소식 > 성주소개 > 성주군청 및 공식 메뉴 링크'),
    ('LGS-000218', 'https://www.chilgok.go.kr/portal/contents.do?mId=0201030000', NULL, '공고/고시 | 알림마당 | 칠곡소식 | 칠곡군 및 공식 메뉴 링크'),
    ('LGS-000219', 'https://www.ycg.kr/open.content/ko/administrative/news/announcement/', NULL, '전체보기 - 공고/고시 - 예천군 및 공식 메뉴 링크'),
    ('LGS-000220', 'https://www.bonghwa.go.kr/portal/contents.do?mid=0201030000', NULL, '고시/공고 목록 | 군정소식 | 행정정보 | 봉화군청 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000221', 'https://www.uljin.go.kr/index.uljin?menuCd=DOM_000000103002007000', NULL, '행정·소식 > 알림 및 소식 > 고시/공고 > 고시/공고 및 공식 메뉴 링크'),
    ('LGS-000222', 'https://www.ulleung.go.kr/ko/page.do?mnu_uid=571&boardType=notice', NULL, '고시공고 | 알림마당 | 울릉소식/정보 | 아름다운 신비의 섬 울릉군 및 공식 메뉴 링크'),
    ('LGS-000223', 'https://www.gyeongnam.go.kr/index.gyeong?menuCd=DOM_000000135003009000', NULL, '경남소식 > 공고 > 고시공고 > 고시공고 - 경상남도 대표 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000225', 'https://www.jinju.go.kr/00130/02730/05586.web', NULL, '고시/공고 | 진주시청 및 공식 메뉴 링크'),
    ('LGS-000226', 'https://www.tongyeong.go.kr/00858.web', 'http://eminwon.tongyeong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시/공고 | 통영시 및 공식 메뉴 링크'),
    ('LGS-000227', 'https://www.sacheon.go.kr/news/00009/00014.web', NULL, '사천시청 시정소식 공고 고시 시험 목록'),
    ('LGS-000228', 'https://www.gimhae.go.kr/03360/00023/00029.web', NULL, '고시공고 | 김해시청 및 공식 메뉴 링크'),
    ('LGS-000231', 'https://www.yangsan.go.kr/portal/contents.do?mid=0102010000', NULL, '고시/공고 목록 | 고시ㆍ공고 | 양산소식 | 양산시청 홈페이지 및 공식 메뉴 링크'),
    ('LGS-000232', 'https://www.uiryeong.go.kr/index.uiryeong?menuCd=DOM_000000203003000000', NULL, '군정소식 > 공고/고시 > 공고/고시 > 공고/고시 | 의령군청 및 공식 메뉴 링크'),
    ('LGS-000234', 'https://www.cng.go.kr/03517/01553.web', NULL, '고시공고 >고시/공고 | 창녕소식 및 공식 메뉴 링크'),
    ('LGS-000235', 'https://www.goseong.go.kr/index.goseong?menuCd=DOM_000000103001014000', NULL, '정보공개 > 정보공개 > 고시공고 | 경상남도 고성군청 및 공식 메뉴 링크'),
    ('LGS-000236', 'https://www.namhae.go.kr/socialm/Index.do?c=SM010110000', NULL, '공고/고시 | 남해군 소셜뉴스미디어 및 공식 메뉴 링크'),
    ('LGS-000237', 'https://www.hadong.go.kr/media/00012.web', NULL, '공고고시 | 하동군청 및 공식 메뉴 링크'),
    ('LGS-000238', 'https://www.sancheong.go.kr/www/selectBbsNttList.do?bbsNo=118&key=158', NULL, '고시/공고 - 모두가 행복한 산청군 및 공식 메뉴 링크'),
    ('LGS-000240', 'https://www.geochang.go.kr/00445/00451.web', NULL, '뉴스미디어포털 > 군정소식> 입법/공고/고시 및 공식 메뉴 링크'),
    ('LGS-000241', 'https://www.hc.go.kr/04923/04924/04948.web', 'https://eminwon.hc.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '고시공고 | 합천군 및 공식 메뉴 링크'),
    ('LGS-000242', 'https://www.jeju.go.kr/news/news/law.htm', NULL, '도정뉴스 > 도정소식 > 입법·고시·공고 > 제주특별자치도 공고 - 제주특별자치도 및 공식 메뉴 링크'),
    ('LGS-000243', 'https://www.jejusi.go.kr/information/intro/notice.do', NULL, '제주시청 제주시소식 입찰 고시 공고 목록'),
    ('LGS-000244', 'https://seogwipo.go.kr/info/news/law.htm', 'https://eminwon.seogwipo.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '서귀포시청 시정소식 공고 고시 일반공고 목록')
)
UPDATE local_government_notice_sources AS source
SET notice_url = reviewed_source.notice_url,
    collection_endpoint_url = reviewed_source.collection_endpoint_url,
    page_type_code = 'public_notice_board',
    source_board_type_code = 'LEGAL_NOTICE',
    collection_policy_code = 'KEYWORD_FILTERED',
    validation_status_code = 'VERIFIED',
    is_semantically_verified = true,
    semantic_verified_at = CURRENT_TIMESTAMP,
    semantic_verified_by = NULL,
    semantic_verification_note =
        '2026-07-28 공식 고시공고 URL 전수 보정: ' || reviewed_source.verification_note,
    parser_profile_code = 'MANUAL_ONLY',
    request_profile_code = CASE
        WHEN reviewed_source.collection_endpoint_url IS NULL THEN 'DEFAULT'
        ELSE 'BROWSER_HTTP1'
    END,
    request_method_code = 'GET',
    request_form_json = NULL,
    is_enabled = false,
    collection_status_code = 'DISABLED',
    last_collected_at = NULL,
    last_http_status = NULL,
    last_error_code = NULL,
    last_error_message = NULL,
    etag = NULL,
    last_modified_value = NULL,
    last_content_fingerprint = NULL,
    updated_at = CURRENT_TIMESTAMP
FROM reviewed_source
WHERE source.public_code = reviewed_source.public_code
  AND source.deleted_at IS NULL;

-- Follow the public Saeol bootstrap form with the same fixed, non-personal query fields used by the browser.
WITH saeol_post_source (
    public_code, collection_endpoint_url, request_form_json
) AS (
    VALUES
    ('LGS-000021', 'https://dongjak.eminwon.seoul.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","cha_dep_code_nm":""}'::jsonb),
    ('LGS-000028', 'https://eminwon.bsjunggu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","homepagetype":"new"}'::jsonb),
    ('LGS-000029', 'https://eminwon.bsseogu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","homepagetype":"home"}'::jsonb),
    ('LGS-000030', 'https://eminwon.bsdonggu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","yyyy":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000032', 'https://eminwon.busanjin.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","homepagetype":"home","epcCheck":"Y"}'::jsonb),
    ('LGS-000033', 'https://eminwon.dongnae.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"12","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000035', 'https://eminwon.bsbukgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000036', 'https://eminwon.haeundae.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"12","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000037', 'http://eminwon.saha.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","is_mobile":""}'::jsonb),
    ('LGS-000038', 'https://eminwon.geumjeong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","homepagetype":"new"}'::jsonb),
    ('LGS-000039', 'https://eminwon.bsgangseo.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000041', 'https://eminwon.suyeong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","homepagetype":"new","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","recent_mm":"12","last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000042', 'https://eminwon.sasang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","epcCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","is_mobile":"","yyyy":"","recent_mm":"12"}'::jsonb),
    ('LGS-000043', 'https://eminwon.gijang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,04","list_gubun":""}'::jsonb),
    ('LGS-000048', 'https://eminwon.nam.daegu.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":""}'::jsonb),
    ('LGS-000049', 'https://eminwon.buk.daegu.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"Y","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000050', 'https://eminwon.suseong.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000051', 'https://eminwon.dalseo.daegu.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000056', 'https://eminwon.yeongjong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,05,06","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000058', 'https://eminwon.yeonsu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"15","not_ancmt_se_code":"01,02,03,04,06,07","list_gubun":"","dept_nm":"","not_ancmt_sj":""}'::jsonb),
    ('LGS-000066', 'https://eminwon.donggu.gwangju.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000072', 'https://eminwon.donggu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":""}'::jsonb),
    ('LGS-000078', 'https://eminwon.junggu.ulsan.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000079', 'https://eminwon.ulsannamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","yyyy":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000080', 'https://eminwon.donggu.ulsan.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,03,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","yyyy":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000081', 'https://eminwon.bukgu.ulsan.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,03,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","nodate_recent_mm":"12","epcCheck":"Y","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000086', 'https://eminwon.yongin.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","epcCheck":"Y"}'::jsonb),
    ('LGS-000109', 'https://eminwon.uiwang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,04,06","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000128', 'https://eminwon.jeongseon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04,05,06","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000134', 'https://eminwon.yangyang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04,05,06","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000142', 'https://eminwon.jp.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,03,04,06","title":"고시/공고/입법예고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":""}'::jsonb),
    ('LGS-000143', 'https://eminwon.jincheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01","title":"고시","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":""}'::jsonb),
    ('LGS-000144', 'https://eminwon.goesan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","recent_mm":"36"}'::jsonb),
    ('LGS-000146', 'https://eminwon.danyang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000153', 'https://eminwon.nonsan21.net/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,05,04,06","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","yyyy":"2012"}'::jsonb),
    ('LGS-000155', 'https://eminwon.dangjin.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,05,06","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000158', 'https://eminwon.seocheon.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"04","title":"일반공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":""}'::jsonb),
    ('LGS-000159', 'https://eminwon.cheongyang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,04,06,02","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000162', 'https://eminwon.taean.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000165', 'http://eminwon.gunsan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","yyyy":""}'::jsonb),
    ('LGS-000166', 'https://eminwon.iksan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","recent_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000167', 'http://eminwon.jeongeup.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000170', 'https://eminwon.wanju.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","yyyy":""}'::jsonb),
    ('LGS-000171', 'https://eminwon.jinan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":""}'::jsonb),
    ('LGS-000172', 'https://eminwon.muju.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","yyyy":""}'::jsonb),
    ('LGS-000173', 'https://eminwon.jangsu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,02,04,05,03","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"2017","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000174', 'https://eminwon.imsil.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000175', 'https://eminwon.sunchang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"20","not_ancmt_se_code":"01,02,03,04,05","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","yyyy":""}'::jsonb),
    ('LGS-000188', 'https://eminwon.hwasun.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"Y","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,05,06,07","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000226', 'http://eminwon.tongyeong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,04,05,06","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"A","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"20160523","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb),
    ('LGS-000241', 'https://eminwon.hc.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"Y","ofr_pageSize":"10","not_ancmt_se_code":"01,02,03,04,06"}'::jsonb),
    ('LGS-000244', 'https://eminwon.seogwipo.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do', '{"epcCheck":"","pageIndex":"","jndinm":"OfrNotAncmtEJB","context":"NTIS","method":"selectListOfrNotAncmt","methodnm":"selectListOfrNotAncmtHomepage","not_ancmt_mgt_no":"","homepage_pbs_yn":"Y","subCheck":"N","ofr_pageSize":"10","not_ancmt_se_code":"01,04,07","title":"고시공고","cha_dep_code_nm":"","initValue":"","countYn":"Y","list_gubun":"","not_ancmt_sj":"","not_ancmt_cn":"","dept_nm":"","cgg_code":"","yyyy":"","yyyymmdd":"","recent_mm":"","last_mm":"","nodate_recent_mm":"","nodate_last_mm":"","not_ancmt_reg_no":""}'::jsonb)
)
UPDATE local_government_notice_sources AS source
SET collection_endpoint_url = saeol_post_source.collection_endpoint_url,
    request_profile_code = 'BROWSER_HTTP1',
    request_method_code = 'POST_FORM',
    request_form_json = saeol_post_source.request_form_json,
    updated_at = CURRENT_TIMESTAMP
FROM saeol_post_source
WHERE source.public_code = saeol_post_source.public_code
  AND source.deleted_at IS NULL;

-- Override legacy GET-only request profiles for the newly verified Saeol POST boards.
UPDATE local_government_notice_sources
SET request_profile_code = 'BROWSER_HTTP1',
    updated_at = CURRENT_TIMESTAMP
WHERE public_code IN (
    'LGS-000072',
    'LGS-000086',
    'LGS-000158'
)
  AND deleted_at IS NULL;
