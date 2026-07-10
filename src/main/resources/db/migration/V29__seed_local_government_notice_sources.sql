-- Seed the initial local-government notice URL registry from the reviewed workbook.
-- All sources start disabled until their parser profile is verified by an operator.
SET client_encoding = 'UTF8';

INSERT INTO local_government_notice_parser_profiles (id, profile_code, profile_name, parser_type_code, list_item_selector, title_selector, date_selector, link_selector, date_pattern, is_enabled)
VALUES ('ce03f5fb-d1ae-4580-9ffa-0706c3f0a1ed', 'MANUAL_ONLY', '수동 검증 필요', 'MANUAL_ONLY', NULL, NULL, NULL, NULL, NULL, false)
ON CONFLICT (profile_code) DO NOTHING;

INSERT INTO local_government_notice_parser_profiles (id, profile_code, profile_name, parser_type_code, list_item_selector, title_selector, date_selector, link_selector, date_pattern, is_enabled)
VALUES ('c6f40d02-869c-4dbe-8f2c-6058870f003c', 'SAEOL_GOSI', '새올 고시공고', 'SAEOL_GOSI', 'table tbody tr', 'td a', 'td:last-child', 'td a', 'yyyy-MM-dd', true)
ON CONFLICT (profile_code) DO NOTHING;

INSERT INTO local_government_notice_parser_profiles (id, profile_code, profile_name, parser_type_code, list_item_selector, title_selector, date_selector, link_selector, date_pattern, is_enabled)
VALUES ('cc71210c-f7fc-41f4-a256-f6987428fb22', 'SPRING_BBS', '표준 게시판', 'SPRING_BBS', 'table tbody tr', 'td a', 'td:nth-last-child(2)', 'td a', 'yyyy-MM-dd', true)
ON CONFLICT (profile_code) DO NOTHING;

INSERT INTO local_government_notice_parser_profiles (id, profile_code, profile_name, parser_type_code, list_item_selector, title_selector, date_selector, link_selector, date_pattern, is_enabled)
VALUES ('401cbee5-53f7-4b90-be92-42e833eb81ae', 'JSP_BBS', 'JSP 게시판', 'JSP_BBS', 'table tbody tr', 'td a', 'td:nth-last-child(2)', 'td a', 'yyyy.MM.dd', true)
ON CONFLICT (profile_code) DO NOTHING;

INSERT INTO local_government_notice_parser_profiles (id, profile_code, profile_name, parser_type_code, list_item_selector, title_selector, date_selector, link_selector, date_pattern, is_enabled)
VALUES ('b47c4b8f-3cb0-428c-9d00-da7feafca0cb', 'TC_GOSI', 'TC 고시공고', 'TC_GOSI', 'table tbody tr', 'td a', 'td:nth-last-child(2)', 'td a', 'yyyy-MM-dd', true)
ON CONFLICT (profile_code) DO NOTHING;

INSERT INTO local_government_notice_parser_profiles (id, profile_code, profile_name, parser_type_code, list_item_selector, title_selector, date_selector, link_selector, date_pattern, is_enabled)
VALUES ('bcb79864-ff46-4bce-ad08-34d68455b172', 'GENERIC_TABLE', '일반 표형 게시판', 'GENERIC_TABLE', 'table tbody tr', 'td a', 'td:last-child', 'td a', NULL, true)
ON CONFLICT (profile_code) DO NOTHING;

INSERT INTO local_government_notice_parser_profiles (id, profile_code, profile_name, parser_type_code, list_item_selector, title_selector, date_selector, link_selector, date_pattern, is_enabled)
VALUES ('5f968ed9-775d-4ee7-988f-3f919bcb88d7', 'GENERIC_LIST', '일반 목록형 게시판', 'GENERIC_LIST', 'ul li, ol li', 'a', 'time, .date', 'a', NULL, true)
ON CONFLICT (profile_code) DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a9c65a30-b325-4e85-92da-2399fd1bcd66', '1100000000', '서울특별시', '1100000000', '서울특별시',
    'SIDO', '서울특별시청', 'http://www.seoul.go.kr', 'http://www.seoul.go.kr/v2012/news/list.html?tr_code=gnb_news',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'b649f432-d2f3-4684-be36-87bd27c01b62', '1100000000', '서울특별시', '1111000000', '종로구',
    'BASIC_LOCAL_GOVERNMENT', '종로구청', 'http://www.jongno.go.kr', 'https://market.jongno.go.kr/support/support_jongno',
    'dedicated_small_business_board', 'MANUAL_ONLY', '종로사장 지원사업, 종로구청, 금융, 경영, 인력 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'b68c75eb-e62a-41b0-b328-34f7b29c3391', '1100000000', '서울특별시', '1114000000', '중구',
    'BASIC_LOCAL_GOVERNMENT', '중구청', 'http://www.junggu.seoul.kr/index.html', 'https://www.junggu.seoul.kr/content.do?cmsid=14203',
    'small_business_support_page', 'MANUAL_ONLY', '소상공인지원, 중소기업 육성기금 융자, 컨설팅, 온라인 마케팅 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'b0c7b5af-2ae0-41a8-b7fd-665bf4a5ad61', '1100000000', '서울특별시', '1117000000', '용산구',
    'BASIC_LOCAL_GOVERNMENT', '용산구청', 'http://www.yongsan.go.kr', 'https://health.yongsan.go.kr/portal/bbs/B0000095/list.do?menuNo=200233',
    'public_notice_board', 'MANUAL_ONLY', '고시/공고, 청년기업 융자지원, 중소기업육성기금, 소상공인 | 서울 25개 자치구 선행 검증 URL 병합', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '25b4f483-d432-4fbc-9964-8695f595804f', '1100000000', '서울특별시', '1120000000', '성동구',
    'BASIC_LOCAL_GOVERNMENT', '성동구청', 'https://www.sd.go.kr/main/index.do', 'https://www.sd.go.kr/main/selectBbsNttList.do?bbsNo=183&key=1472',
    'notice_search', 'MANUAL_ONLY', '새소식, 소상공인, 지역경제과, 특별신용보증 | 서울 25개 자치구 선행 검증 URL 병합', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '7dfa4327-c51e-492f-b4b9-a4bc9cb11ac8', '1100000000', '서울특별시', '1121500000', '광진구',
    'BASIC_LOCAL_GOVERNMENT', '광진구청', 'http://www.gwangjin.go.kr', 'https://www.gwangjin.go.kr/portal/main/contents.do?menuNo=200704',
    'small_business_support_page', 'MANUAL_ONLY', '소상공인 원스톱지원센터, 고시공고, 지역경제과 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'bca0134a-0f9c-4020-8f0b-832eb47c36fa', '1100000000', '서울특별시', '1123000000', '동대문구',
    'BASIC_LOCAL_GOVERNMENT', '동대문구청', 'http://www.ddm.go.kr', 'https://www.ddm.go.kr/www/contents.do?key=883',
    'small_business_support_page', 'MANUAL_ONLY', '중소기업육성기금, 소상공인, 구정소식, 경제진흥과 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '6fa7efef-e9bf-4600-ae3e-97a16c029d45', '1100000000', '서울특별시', '1126000000', '중랑구',
    'BASIC_LOCAL_GOVERNMENT', '중랑구청', 'http://www.jungnang.go.kr', 'https://www.jungnang.go.kr/portal/bbs/list/B0000422.do?menuNo=201111',
    'public_notice_board', 'MANUAL_ONLY', '공모사업 알림방, 유관기관소식, 소상공인, 중소기업육성기금 | 서울 25개 자치구 선행 검증 URL 병합', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '1e252da6-1b5b-4862-ad15-62c301858220', '1100000000', '서울특별시', '1129000000', '성북구',
    'BASIC_LOCAL_GOVERNMENT', '성북구청', 'http://www.sb.go.kr/', 'https://seongbuk.go.kr/www/selectBbsNttList.do?bbsNo=41&key=6350',
    'notice_search', 'MANUAL_ONLY', '새소식, 지역경제과, 소상공인 저금리 특별 융자 | 서울 25개 자치구 선행 검증 URL 병합', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e59c85c6-5b06-4c87-8b7e-4877ce5c2e7d', '1100000000', '서울특별시', '1130500000', '강북구',
    'BASIC_LOCAL_GOVERNMENT', '강북구청', 'http://www.gangbuk.go.kr', 'https://child.gangbuk.go.kr/portal/bbs/B0000145/list.do?menuNo=200081',
    'notice_search', 'MANUAL_ONLY', '새소식, 지역경제과, 소상공인, 중소기업ㆍ소상공인 지원사업 | 서울 25개 자치구 선행 검증 URL 병합', 'MEDIUM',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2223a885-0ae2-40d3-9a67-26f63850127a', '1100000000', '서울특별시', '1132000000', '도봉구',
    'BASIC_LOCAL_GOVERNMENT', '도봉구청', 'http://www.dobong.go.kr', 'https://www.dobong.go.kr/',
    'notice_search', 'MANUAL_ONLY', '공지사항, 지역경제과, 소상공인지원센터, 소상공인 | 서울 25개 자치구 선행 검증 URL 병합', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '354082ea-1b6d-4a5e-8b18-8c066573a0dd', '1100000000', '서울특별시', '1135000000', '노원구',
    'BASIC_LOCAL_GOVERNMENT', '노원구청', 'http://www.nowon.kr', 'https://www.nowon.kr/',
    'homepage_fallback', 'MANUAL_ONLY', '노원구청 게시판, 공지사항, 고시공고, 소상공인, 일자리경제과 | 서울 25개 자치구 선행 검증 URL 병합', 'LOW',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '84ff8474-46de-43c7-8fd4-39aeeec8bd0f', '1100000000', '서울특별시', '1138000000', '은평구',
    'BASIC_LOCAL_GOVERNMENT', '은평구청', 'https://www.ep.go.kr/www/index.do', 'https://www.ep.go.kr/dong/selectBbsNttList.do?bbsNo=42',
    'notice_search', 'MANUAL_ONLY', '공지사항(구청), 일자리경제과, 소상공인 | 서울 25개 자치구 선행 검증 URL 병합', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2103d029-ed29-4de3-9b57-bb65fb63c3f5', '1100000000', '서울특별시', '1141000000', '서대문구',
    'BASIC_LOCAL_GOVERNMENT', '서대문구청', 'http://www.sdm.go.kr', 'https://www.sdm.go.kr/news/news/notice.do',
    'notice_search', 'MANUAL_ONLY', '공지사항, 지역경제과, 소상공인 경영컨설팅, 온라인마케팅 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ffdf01fc-c5bc-4585-b4a6-1b7281599822', '1100000000', '서울특별시', '1144000000', '마포구',
    'BASIC_LOCAL_GOVERNMENT', '마포구청', 'http://www.mapo.go.kr', 'https://www.mapo.go.kr/',
    'homepage_fallback', 'MANUAL_ONLY', '구정소식, 고시공고, 지역경제과, 소상공인, 중소기업 | 서울 25개 자치구 선행 검증 URL 병합', 'LOW',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'be1529f7-b253-49b8-8c89-e7add72ca4e2', '1100000000', '서울특별시', '1147000000', '양천구',
    'BASIC_LOCAL_GOVERNMENT', '양천구청', 'http://www.yangcheon.go.kr', 'https://www.yangcheon.go.kr/site/yangcheon/ex/seol/seolCollectList.do',
    'public_notice_board', 'MANUAL_ONLY', '고시/공고, 소상공인, 간판개선, 중소기업육성기금 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '5b985b00-96d3-4c1c-b630-c1e9e6c8456e', '1100000000', '서울특별시', '1150000000', '강서구',
    'BASIC_LOCAL_GOVERNMENT', '강서구청', 'http://www.gangseo.seoul.kr', 'https://www.gangseo.seoul.kr/eco/eco060101',
    'small_business_support_page', 'MANUAL_ONLY', '소상공인 지원, 중소기업육성자금지원, 지역경제과 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'f97e796e-2a39-4693-b9e6-bd3db177c6d9', '1100000000', '서울특별시', '1153000000', '구로구',
    'BASIC_LOCAL_GOVERNMENT', '구로구청', 'http://www.guro.go.kr', 'https://www.guro.go.kr/www/selectBbsNttList.do?bbsNo=662&key=1790&pageIndex=1&pageUnit=10&searchCnd=SJ&searchKrwd=%EC%86%8C%EC%83%81%EA%B3%B5%EC%9D%B8',
    'notice_search', 'MANUAL_ONLY', '새소식 제목 검색: 소상공인 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '826cd322-c1e8-4793-adb9-3b76fee6dcda', '1100000000', '서울특별시', '1154500000', '금천구',
    'BASIC_LOCAL_GOVERNMENT', '금천구청', 'https://www.geumcheon.go.kr/portal/index.do', 'https://www.geumcheon.go.kr/portal/selectBbsNttList.do?bbsNo=150682&key=4099',
    'dedicated_small_business_board', 'MANUAL_ONLY', '소상공인 맞춤 지원 안내, 융자, 교육, 컨설팅, 폐업, 기타 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '056d5504-cf86-435a-8036-ebd39c8460f1', '1100000000', '서울특별시', '1156000000', '영등포구',
    'BASIC_LOCAL_GOVERNMENT', '영등포구청', 'http://www.ydp.go.kr', 'https://www.ydp.go.kr/www/contents.do?key=3336',
    'small_business_support_page', 'MANUAL_ONLY', '중소기업 지원사업, 소상공인, 우리구소식, 일자리경제과 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ddac6f19-5f48-4239-a306-92c2366efdea', '1100000000', '서울특별시', '1159000000', '동작구',
    'BASIC_LOCAL_GOVERNMENT', '동작구청', 'http://www.dongjak.go.kr', 'https://www.dongjak.go.kr/',
    'homepage_fallback', 'MANUAL_ONLY', '고시공고, 경제정책과, 소상공인 무이자 특별보증, 중소기업육성기금 | 서울 25개 자치구 선행 검증 URL 병합', 'LOW',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd26b930c-db84-4d77-8f0a-6c14dbc55d56', '1100000000', '서울특별시', '1162000000', '관악구',
    'BASIC_LOCAL_GOVERNMENT', '관악구청', 'http://www.gwanak.go.kr', 'https://www.gwanak.go.kr/site/gwanak/09/10905100000002023020811.jsp',
    'small_business_support_page', 'MANUAL_ONLY', '골목상권·소상공인 지원사업, 고시공고, 소상공인 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '221dace7-8dc6-4e0e-971a-740c5e89d546', '1100000000', '서울특별시', '1165000000', '서초구',
    'BASIC_LOCAL_GOVERNMENT', '서초구청', 'http://www.seocho.go.kr', 'https://www.seocho.go.kr/site/seocho/04/10409100000002021072101.jsp',
    'small_business_support_page', 'MANUAL_ONLY', '소상공인 지원사업, 중소기업육성기금, 대출이자 지원 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '41457332-507a-4673-8894-8cd306564c02', '1100000000', '서울특별시', '1168000000', '강남구',
    'BASIC_LOCAL_GOVERNMENT', '강남구청', 'http://www.gangnam.go.kr', 'https://www.gangnam.go.kr/board/B_000001/list.do?mid=ID05_040101',
    'notice_search', 'MANUAL_ONLY', '지역경제과, 소상공인, 중소기업, 융자, 동행마켓 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '6308d1a9-8bea-4749-ba19-3a1dcbb2ce49', '1100000000', '서울특별시', '1171000000', '송파구',
    'BASIC_LOCAL_GOVERNMENT', '송파구청', 'http://www.songpa.go.kr', 'https://www.songpa.go.kr/www/selectGosiList.do?key=2776',
    'public_notice_board', 'MANUAL_ONLY', '고시공고, 소상공인, 경제진흥과, 특별신용보증, 단체 지원사업 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2ce92efa-230f-4f96-a09e-352ee8aa4d62', '1100000000', '서울특별시', '1174000000', '강동구',
    'BASIC_LOCAL_GOVERNMENT', '강동구청', 'http://www.gangdong.go.kr', 'https://www.gangdong.go.kr/web/newportal/contents/gdp_005_008_004_002_001',
    'small_business_support_page', 'MANUAL_ONLY', '소상공인지원, 정책알림마당, 고시공고, 중소기업육성기금, 특별신용보증 | 서울 25개 자치구 선행 검증 URL 병합', 'HIGH',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd2fe1cce-2867-4fc1-b77c-22393ebe0aa9', '2600000000', '부산광역시', '2600000000', '부산광역시',
    'SIDO', '부산광역시청', 'http://www.busan.go.kr', 'http://www.busan.go.kr/nbnews',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'bb756d94-2cf9-42a6-9cf9-21d7f60e572f', '2600000000', '부산광역시', '2611000000', '중구',
    'BASIC_LOCAL_GOVERNMENT', '중구청', 'http://www.bsjunggu.go.kr', 'http://www.bsjunggu.go.kr/board/list.junggu?boardId=BBS_0000001&menuCd=DOM_000000103001001000&contentsSid=101',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a37c0da8-986b-471b-9d09-98030cae9aca', '2600000000', '부산광역시', '2614000000', '서구',
    'BASIC_LOCAL_GOVERNMENT', '서구청', 'https://www.bsseogu.go.kr/index.bsseogu', 'http://www.bsseogu.go.kr/board/list.bsseogu?boardId=BBS_0000039&menuCd=DOM_000000103001012000&contentsSid=877&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '1f714e65-cefd-41ee-8f19-aabfac79de2e', '2600000000', '부산광역시', '2617000000', '동구',
    'BASIC_LOCAL_GOVERNMENT', '동구청', 'http://www.bsdonggu.go.kr', 'http://www.bsdonggu.go.kr/board/list.donggu?boardId=BBS_0000023&menuCd=DOM_000000103001001000&contentsSid=276',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'c41e0cfb-1fa5-40cc-947a-7d0438548c8f', '2600000000', '부산광역시', '2620000000', '영도구',
    'BASIC_LOCAL_GOVERNMENT', '영도구청', 'http://www.yeongdo.go.kr', 'http://www.yeongdo.go.kr/00000/00007/00008.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'af55741b-afc8-421d-9cd4-0c089681430d', '2600000000', '부산광역시', '2623000000', '부산진구',
    'BASIC_LOCAL_GOVERNMENT', '부산진구청', 'http://www.busanjin.go.kr', 'https://www.busanjin.go.kr/board/list.busanjin?boardId=BBS_0000009&menuCd=DOM_000000110001001000&contentsSid=316&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'f1b130fb-15bb-4c9b-a041-70f43237ac34', '2600000000', '부산광역시', '2626000000', '동래구',
    'BASIC_LOCAL_GOVERNMENT', '동래구청', 'http://www.dongnae.go.kr', 'http://www.dongnae.go.kr/board/list.dongnae?boardId=BBS_0000012&menuCd=DOM_000000103001001000&contentsSid=41&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'dd07b1ad-b9ec-4d9c-9976-f3d4e1778653', '2600000000', '부산광역시', '2629000000', '남구',
    'BASIC_LOCAL_GOVERNMENT', '남구청', 'http://www.bsnamgu.go.kr', 'http://www.bsnamgu.go.kr/board/list.namgu?boardId=BBS_0000001&menuCd=DOM_000000105001001000&contentsSid=136&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '75d59bcd-7d21-4c89-9462-131e5cebea70', '2600000000', '부산광역시', '2632000000', '북구',
    'BASIC_LOCAL_GOVERNMENT', '북구청', 'http://www.bsbukgu.go.kr', 'https://www.bsbukgu.go.kr/board/list.bsbukgu?boardId=BBS_0000023&menuCd=DOM_000000105001001000&contentsSid=368&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '0ebc2e3e-a111-4842-ad7e-2f1982f2be92', '2600000000', '부산광역시', '2635000000', '해운대구',
    'BASIC_LOCAL_GOVERNMENT', '해운대구청', 'http://www.haeundae.go.kr', 'http://www.haeundae.go.kr/board/list.do?boardId=BBS_0000038&menuCd=DOM_000000104001001000&contentsSid=100',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '0702f408-a323-4e55-898e-3cb937ca0fa9', '2600000000', '부산광역시', '2638000000', '사하구',
    'BASIC_LOCAL_GOVERNMENT', '사하구청', 'http://www.saha.go.kr', 'http://www.saha.go.kr/portal/bbs/list.do?ptIdx=22&mId=0301010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '25fe1f26-d065-4b0e-bb21-0d00f4e91147', '2600000000', '부산광역시', '2641000000', '금정구',
    'BASIC_LOCAL_GOVERNMENT', '금정구청', 'http://www.geumjeong.go.kr', 'http://www.geumjeong.go.kr/board/list.geumj?boardId=BBS_0000004&menuCd=DOM_000000124002001000&contentsSid=3855&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '13e22d68-47d8-4219-9ffe-8492f3ed21aa', '2600000000', '부산광역시', '2644000000', '강서구',
    'BASIC_LOCAL_GOVERNMENT', '강서구청', 'http://www.bsgangseo.go.kr', 'https://www.bsgangseo.go.kr/portal/board/post/list.do?bcIdx=500&mid=0501010000&token=1739334701754',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'f5a9873d-9ad0-4dac-b16c-977f5f9cd8e7', '2600000000', '부산광역시', '2647000000', '연제구',
    'BASIC_LOCAL_GOVERNMENT', '연제구청', 'http://www.yeonje.go.kr', 'http://www.yeonje.go.kr/inews/main.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '96972c84-af90-48a5-84ec-de15751c6961', '2600000000', '부산광역시', '2650000000', '수영구',
    'BASIC_LOCAL_GOVERNMENT', '수영구청', 'http://www.suyeong.go.kr', 'http://www.suyeong.go.kr/board/list.suyeong?boardId=BBS_0000001&menuCd=DOM_000000103001001000&contentsSid=221',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '77326d2d-12dc-4a7c-b151-1df84f9b9f17', '2600000000', '부산광역시', '2653000000', '사상구',
    'BASIC_LOCAL_GOVERNMENT', '사상구청', 'http://www.sasang.go.kr', 'https://www.sasang.go.kr/news/index.sasang',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '90428936-6656-4474-9d64-b53a9de58dd9', '2600000000', '부산광역시', '2671000000', '기장군',
    'BASIC_LOCAL_GOVERNMENT', '기장군청', 'https://www.gijang.go.kr/index.gijang', 'http://www.gijang.go.kr/board/list.gijang?boardId=BBS_0000002&menuCd=DOM_000000101001001000&contentsSid=12&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e40c9a6e-c1b2-455f-8f60-8c4238722422', '2700000000', '대구광역시', '2700000000', '대구광역시',
    'SIDO', '대구광역시청', 'http://www.daegu.go.kr', 'https://www.daegu.go.kr/index.do?menu_id=00000854',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '4d2b1618-1e2b-41f9-ad45-7b92e0b44524', '2700000000', '대구광역시', '2711000000', '중구',
    'BASIC_LOCAL_GOVERNMENT', '중구청', 'http://jung.daegu.kr', 'http://www.jung.daegu.kr/new/pages/administration/page.html?mc=0157',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'aa2d2847-2571-45a3-a547-104d41abfccd', '2700000000', '대구광역시', '2714000000', '동구',
    'BASIC_LOCAL_GOVERNMENT', '동구청', 'http://www.dong.daegu.kr', 'https://www.dong.daegu.kr/portal/board/post/list.do?bcIdx=500&mid=0201010000&token=1706162215124',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a48d3cfe-5d97-4c36-8072-9e7c3c16b7be', '2700000000', '대구광역시', '2717000000', '서구',
    'BASIC_LOCAL_GOVERNMENT', '서구청', 'http://www.dgs.go.kr', 'https://www.dgs.go.kr/portal/board/post/list.do?bcIdx=566&mid=0601010000&token=1747370338925',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '80317cde-b29a-4d5d-b690-f49c9b0c9412', '2700000000', '대구광역시', '2720000000', '남구',
    'BASIC_LOCAL_GOVERNMENT', '남구청', 'http://nam.daegu.kr', 'https://nam.daegu.kr/index.do?menu_id=00000848',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2f6f42ae-623a-4e49-af6f-565dd75abe74', '2700000000', '대구광역시', '2723000000', '북구',
    'BASIC_LOCAL_GOVERNMENT', '북구청', 'https://www.buk.daegu.kr/', 'http://www.buk.daegu.kr/index.do?menu_id=00000195',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '40b2b29a-747d-4da8-9be1-be310bcc6905', '2700000000', '대구광역시', '2726000000', '수성구',
    'BASIC_LOCAL_GOVERNMENT', '수성구청', 'http://www.suseong.kr', 'https://www.suseong.kr/?menu_id=00000063',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'bee50573-a506-4b05-8691-3fff0f1ec99f', '2700000000', '대구광역시', '2729000000', '달서구',
    'BASIC_LOCAL_GOVERNMENT', '달서구청', 'https://www.dalseo.daegu.kr/', 'https://dalseo.daegu.kr/?menu_id=10000102',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '68db79e2-2064-45bf-a630-54d8a11190e6', '2700000000', '대구광역시', '2771000000', '달성군',
    'BASIC_LOCAL_GOVERNMENT', '달성군청', 'http://www.dalseong.daegu.kr', 'https://www.dalseong.daegu.kr/index.do?menu_id=00000194',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준 | 검토 과정에서 공식 공고 URL 보정', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e99c3c13-651e-4c6c-a7f5-928575e22b1f', '2700000000', '대구광역시', '2772000000', '군위군',
    'BASIC_LOCAL_GOVERNMENT', '군위군청', 'https://www.gunwi.go.kr/ko/index.do', 'https://www.gunwi.go.kr/ko/page.do?mnu_uid=101&',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a5c9340d-4e36-4463-bdf6-9308d00729dd', '2800000000', '인천광역시', '2800000000', '인천광역시',
    'SIDO', '인천광역시청', 'https://www.incheon.go.kr/index', 'https://www.incheon.go.kr/IC010101',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a81a0a6e-9718-47a6-bcb8-61956d819da2', '2800000000', '인천광역시', '2812500000', '제물포구',
    'BASIC_LOCAL_GOVERNMENT', '제물포구청', 'https://www.jemulpo.go.kr/', 'https://www.jemulpo.go.kr/main/information/news/notice.jsp',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'f319c89f-e30b-4c77-9b85-4e6a5ea387e5', '2800000000', '인천광역시', '2815500000', '영종구',
    'BASIC_LOCAL_GOVERNMENT', '영종구청', 'https://yeongjong.go.kr', 'https://yeongjong.go.kr/main/pst/list.do?pst_id=mn_ntc',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '924750c8-7e97-4c1f-bc13-e4cf2b3512b2', '2800000000', '인천광역시', '2817700000', '미추홀구',
    'BASIC_LOCAL_GOVERNMENT', '미추홀구청', 'https://www.michuhol.go.kr', 'https://www.michuhol.go.kr/main/board/list.do?board_code=board_1',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '41c08eca-f1c8-4dbb-9fd9-36b10443a691', '2800000000', '인천광역시', '2818500000', '연수구',
    'BASIC_LOCAL_GOVERNMENT', '연수구청', 'http://www.yeonsu.go.kr', 'http://www.yeonsu.go.kr/main/community/notify/notice.asp',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '234ebcba-30db-4b1a-90d0-0577d2e61a80', '2800000000', '인천광역시', '2820000000', '남동구',
    'BASIC_LOCAL_GOVERNMENT', '남동구청', 'http://www.namdong.go.kr', 'http://www.namdong.go.kr/main/bbs/bbsMsgList.do?bcd=notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '593097c1-c052-4358-bce9-5ed8cbcde1ec', '2800000000', '인천광역시', '2823700000', '부평구',
    'BASIC_LOCAL_GOVERNMENT', '부평구청', 'http://www.icbp.go.kr', 'http://www.icbp.go.kr/main/participation/news/notice.jsp',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '4a8afe99-132c-4111-a8ee-869d1890f2d9', '2800000000', '인천광역시', '2824500000', '계양구',
    'BASIC_LOCAL_GOVERNMENT', '계양구청', 'http://www.gyeyang.go.kr', 'http://www.gyeyang.go.kr/open_content/main/bbs/bbsMsgList.do?bcd=board_4',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e34de440-48c7-423a-a073-fbf12b07e2d9', '2800000000', '인천광역시', '2827500000', '서해구',
    'BASIC_LOCAL_GOVERNMENT', '서해구청', 'http://seo.incheon.kr', 'https://seohae.go.kr/open_content/main/community/news/gosi.jsp',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준 | 검토 과정에서 공식 공고 URL 보정', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '8e2700ad-e11b-40fc-be13-e570e04f22ea', '2800000000', '인천광역시', '2829000000', '검단구',
    'BASIC_LOCAL_GOVERNMENT', '검단구청', 'https://www.geomdan.go.kr/main/', 'https://www.geomdan.go.kr/main/community/news/notice.jsp',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '1b2676cb-2c8c-4121-a259-da634026bf85', '2800000000', '인천광역시', '2871000000', '강화군',
    'BASIC_LOCAL_GOVERNMENT', '강화군청', 'http://www.ganghwa.go.kr', 'http://www.ganghwa.go.kr/open_content/main/ganghwa/news/notice.jsp',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '5925864d-8ef7-4ad9-8480-d00fbb2644b2', '2800000000', '인천광역시', '2872000000', '옹진군',
    'BASIC_LOCAL_GOVERNMENT', '옹진군청', 'http://www.ongjin.go.kr', 'http://www.ongjin.go.kr/open_content/main/community/board/notice.jsp',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '17fac080-44fd-4353-93ce-28015fad8e58', '2900000000', '광주광역시', '2911000000', '동구',
    'BASIC_LOCAL_GOVERNMENT', '동구청', 'http://www.donggu.kr/', 'http://www.donggu.kr/board.es?mid=a10101010000&bid=0001',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2c4e311c-3d00-4db7-9372-2fa10dd628a2', '2900000000', '광주광역시', '2914000000', '서구',
    'BASIC_LOCAL_GOVERNMENT', '서구청', 'http://seogu.gwangju.kr', 'https://www.seogu.gwangju.kr/board.es?mid=a10311010100&bid=0034&act=listC&gon=C',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'c29f2b9b-ffe0-4193-bbfa-c0d7c60bcc57', '2900000000', '광주광역시', '2915500000', '남구',
    'BASIC_LOCAL_GOVERNMENT', '남구청', 'https://www.namgu.gwangju.kr/index.es?sid=a1', 'http://www.namgu.gwangju.kr/board.es?mid=a10604010000&bid=0001',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '07e9fb35-0d63-4f75-8b98-32eb8c6e7b1e', '2900000000', '광주광역시', '2917000000', '북구',
    'BASIC_LOCAL_GOVERNMENT', '북구청', 'https://bukgu.gwangju.kr/index.es?sid=a1', 'https://bukgu.gwangju.kr/board.es?mid=a10201010000&bid=0114',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '7293df66-af6b-436d-af03-a5f9991f9241', '2900000000', '광주광역시', '2920000000', '광산구',
    'BASIC_LOCAL_GOVERNMENT', '광산구청', 'http://www.gwangsan.go.kr', 'https://www.gwangsan.go.kr/boardList.do?boardId=NEWS_NEW&pageId=www3',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ed0c5449-19d8-408c-be37-a14cdd709475', '3000000000', '대전광역시', '3000000000', '대전광역시',
    'SIDO', '대전광역시청', 'http://www.daejeon.go.kr', 'http://www.daejeon.go.kr/drh/MediaList.do?menuSeq=2558',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'c61ffe24-9962-4504-97b5-c4afbb8f2fc8', '3000000000', '대전광역시', '3011000000', '동구',
    'BASIC_LOCAL_GOVERNMENT', '동구청', 'http://www.donggu.go.kr', 'https://www.donggu.go.kr/dg/kor/article/newsNotice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '4a053a0f-5ace-4c36-9625-0d6fe34aa8da', '3000000000', '대전광역시', '3014000000', '중구',
    'BASIC_LOCAL_GOVERNMENT', '중구청', 'http://www.djjunggu.go.kr', 'https://www.djjunggu.go.kr/bbs/BBSMSTR_000000000136/list.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '07c8ee58-9a93-463f-8cdc-93255fa0d7b4', '3000000000', '대전광역시', '3017000000', '서구',
    'BASIC_LOCAL_GOVERNMENT', '서구청', 'http://www.seogu.go.kr', 'https://www.seogu.go.kr/bbs/BBSMSTR_000000000275/list.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '6bb02267-febb-47dc-bd6e-a6cccc0a10bc', '3000000000', '대전광역시', '3020000000', '유성구',
    'BASIC_LOCAL_GOVERNMENT', '유성구청', 'http://www.yuseong.go.kr', 'https://www.yuseong.go.kr/bbs/BBSMSTR_000000000099/list.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '7ec5ad70-ad04-4730-8c5d-2778efdeeecb', '3000000000', '대전광역시', '3023000000', '대덕구',
    'BASIC_LOCAL_GOVERNMENT', '대덕구청', 'http://www.daedeok.go.kr', 'http://www.daedeok.go.kr/dpt/dpt04/DPT040101_cmmBoardList.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e2e8f444-7b38-43be-a837-f5cd6869f9f7', '3100000000', '울산광역시', '3100000000', '울산광역시',
    'SIDO', '울산광역시청', 'http://www.ulsan.go.kr', 'https://www.ulsan.go.kr/u/rep/bbs/list.ulsan?bbsId=BBS_0000000000000003&mId=001004001001000000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '5d8813bd-8276-4643-99ec-86ebbbb6ba65', '3100000000', '울산광역시', '3111000000', '중구',
    'BASIC_LOCAL_GOVERNMENT', '중구청', 'http://www.junggu.ulsan.kr', 'https://www.junggu.ulsan.kr/board/view.ulsan?boardId=BBS_0000057&contentsSid=1&paging=ok&startPage=1&dataSid=707524&menuCd=DOM_000000102003001000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '52fff0ba-3649-44c8-8b69-6e8675d3651d', '3100000000', '울산광역시', '3114000000', '남구',
    'BASIC_LOCAL_GOVERNMENT', '남구청', 'https://www.ulsannamgu.go.kr/cmm/main/mainPage.do', 'http://www.ulsannamgu.go.kr/cop/bbs/selectBoardList.do?bbsId=namguNews',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2b1af9e4-7e05-4f3d-9b7a-dc4e24e12012', '3100000000', '울산광역시', '3117000000', '동구',
    'BASIC_LOCAL_GOVERNMENT', '동구청', 'http://www.donggu.ulsan.kr', 'https://www.donggu.ulsan.kr/cop/bbs/selectBoardList.do?bbsId=BBSMSTR_000000000323',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'cdde074b-3f89-439a-b023-406339214019', '3100000000', '울산광역시', '3120000000', '북구',
    'BASIC_LOCAL_GOVERNMENT', '북구청', 'http://www.bukgu.ulsan.kr', 'http://www.bukgu.ulsan.kr/lay1/bbs/S1T62C83/A/1/list.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd3d775a5-7410-407f-8392-e40c764d533c', '3100000000', '울산광역시', '3171000000', '울주군',
    'BASIC_LOCAL_GOVERNMENT', '울주군청', 'http://www.ulju.ulsan.kr', 'https://www.ulju.ulsan.kr/ulju/bbs/list.do?ptIdx=145&mId=0404010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2baa8803-7abd-4f99-9c9a-f5c05abbe422', '3600000000', '세종특별자치시', '3611000000', '세종특별자치시',
    'SIDO', '세종특별자치시청', 'http://www.sejong.go.kr', 'http://www.sejong.go.kr/bbs/R0071/list.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'c587192e-6435-4a06-94f6-58f4d5f6a885', '4100000000', '경기도', '4100000000', '경기도',
    'SIDO', '경기도청', 'http://www.gg.go.kr', 'https://gnews.gg.go.kr/briefing/brief_gongbo.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ecf4420a-bd94-4331-9d0f-ddfea2c3ba0e', '4100000000', '경기도', '4111000000', '수원시',
    'BASIC_LOCAL_GOVERNMENT', '수원시청', 'http://www.suwon.go.kr', 'http://www.suwon.go.kr/web/board/BD_board.list.do?bbsCd=1042',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'c4ccea90-13a2-4c41-83ca-f10dc6397ce5', '4100000000', '경기도', '4146000000', '용인시',
    'BASIC_LOCAL_GOVERNMENT', '용인시청', 'http://www.yongin.go.kr', 'http://www.yongin.go.kr/user/bbs/BD_selectBbsList.do?q_bbsCode=1001&q_clCode=1',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2f9ab762-47c4-4add-944f-d60acd115912', '4100000000', '경기도', '4128000000', '고양시',
    'BASIC_LOCAL_GOVERNMENT', '고양시청', 'http://www.goyang.go.kr', 'https://www.goyang.go.kr/www/user/bbs/BD_selectBbsList.do?q_bbsCode=1030',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '211c4c57-9e88-4079-b481-4a0abc7121fb', '4100000000', '경기도', '4159000000', '화성시',
    'BASIC_LOCAL_GOVERNMENT', '화성시청', 'http://www.hscity.go.kr', 'http://www.hscity.go.kr/www/user/bbs/BD_selectBbsList.do?q_bbsCode=1019',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e945bc72-01a3-422f-b052-cb4fddb67373', '4100000000', '경기도', '4113000000', '성남시',
    'BASIC_LOCAL_GOVERNMENT', '성남시청', 'http://www.seongnam.go.kr', 'http://www.seongnam.go.kr/city/1000052/30001/bbsList.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e1c1b9ab-0107-4c8e-9cdb-2bd65311e12f', '4100000000', '경기도', '4119000000', '부천시',
    'BASIC_LOCAL_GOVERNMENT', '부천시청', 'http://www.bucheon.go.kr', 'https://www.bucheon.go.kr/site/program/board/basicboard/list?boardtypeid=26736&menuid=148002001001',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '90a54c0a-1dac-449c-b544-fff37f972a58', '4100000000', '경기도', '4136000000', '남양주시',
    'BASIC_LOCAL_GOVERNMENT', '남양주시청', 'http://www.nyj.go.kr', 'https://www.nyj.go.kr/www/selectBbsNttList.do?key=2481&bbsNo=62',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'c76103ea-c6f0-4feb-8fdd-f71a1cc577cd', '4100000000', '경기도', '4127000000', '안산시',
    'BASIC_LOCAL_GOVERNMENT', '안산시청', 'http://www.ansan.go.kr', 'https://www.ansan.go.kr/www/common/bbs/selectPageListBbs.do?bbs_code=B0214',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '7feaebd7-cfe5-4701-92a4-7da72b6491df', '4100000000', '경기도', '4122000000', '평택시',
    'BASIC_LOCAL_GOVERNMENT', '평택시청', 'https://www.pyeongtaek.go.kr/main.do', 'http://www.pyeongtaek.go.kr/pyeongtaek/bbs/list.do?ptIdx=41&mId=0401010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'bd653184-8f29-40ab-8ad3-16f1d2d20117', '4100000000', '경기도', '4117000000', '안양시',
    'BASIC_LOCAL_GOVERNMENT', '안양시청', 'https://www.anyang.go.kr/main/index.do', 'http://www.anyang.go.kr/main/selectBbsNttList.do?bbsNo=62&key=259',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'da2f405c-1d41-4fa7-b6e7-b1f1226bfbba', '4100000000', '경기도', '4139000000', '시흥시',
    'BASIC_LOCAL_GOVERNMENT', '시흥시청', 'http://www.siheung.go.kr', 'http://www.siheung.go.kr/main/bbs/list.do?ptIdx=46&mId=0401010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '0cfadf43-b632-4942-ab2f-8426c9ae14a8', '4100000000', '경기도', '4148000000', '파주시',
    'BASIC_LOCAL_GOVERNMENT', '파주시청', 'http://www.paju.go.kr', 'http://www.paju.go.kr/user/board/BD_board.list.do?bbsCd=2001',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '780c30eb-6945-4212-856f-41758c66e638', '4100000000', '경기도', '4157000000', '김포시',
    'BASIC_LOCAL_GOVERNMENT', '김포시청', 'http://www.gimpo.go.kr', 'https://www.gimpo.go.kr/portal/selectBbsNttList.do?bbsNo=292&key=999',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'de9b34c0-e614-4ce1-aa08-b795eb4a6c7a', '4100000000', '경기도', '4115000000', '의정부시',
    'BASIC_LOCAL_GOVERNMENT', '의정부시청', 'https://www.ui4u.go.kr/main.do', 'http://www.ui4u.go.kr/portal/bbs/list.do?ptIdx=35&mId=0301010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'df212a9a-e754-4ce1-bed0-93f4bdc6b98a', '4100000000', '경기도', '4161000000', '광주시',
    'BASIC_LOCAL_GOVERNMENT', '광주시청', 'http://www.gjcity.go.kr', 'http://www.gjcity.go.kr/portal/bbs/list.do?ptIdx=1&mId=0201010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd05996f2-09b8-4437-9ca2-914a56db45b2', '4100000000', '경기도', '4145000000', '하남시',
    'BASIC_LOCAL_GOVERNMENT', '하남시청', 'http://www.hanam.go.kr', 'http://www.hanam.go.kr/www/selectBbsNttList.do?bbsNo=30&key=170',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '31ea925a-5e44-4aad-bf0d-a1149b65d6af', '4100000000', '경기도', '4163000000', '양주시',
    'BASIC_LOCAL_GOVERNMENT', '양주시청', 'http://www.yangju.go.kr', 'http://www.yangju.go.kr/www/selectBbsNttList.do?bbsNo=13&key=202',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '27ffba14-aa5e-4073-9e8d-1e2c4fcaeb06', '4100000000', '경기도', '4121000000', '광명시',
    'BASIC_LOCAL_GOVERNMENT', '광명시청', 'http://www.gm.go.kr', 'http://www.gm.go.kr/pt/user/bbs/BD_selectBbsList.do?q_bbsCode=2032',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '4ed91762-1118-430a-8308-0ac78b93e791', '4100000000', '경기도', '4141000000', '군포시',
    'BASIC_LOCAL_GOVERNMENT', '군포시청', 'http://www.gunpo.go.kr', 'http://www.gunpo.go.kr/www/selectBbsNttList.do?bbsNo=675&key=3890',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'fc98f66f-2f8a-4d94-bf29-1d8a1fc1a496', '4100000000', '경기도', '4137000000', '오산시',
    'BASIC_LOCAL_GOVERNMENT', '오산시청', 'http://www.osan.go.kr', 'https://www.osan.go.kr/portal/saeol/gosi/list.do?mId=0302010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준 | 검토 과정에서 공식 공고 URL 보정', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ec8f0473-0bf5-4282-b5d2-af69aa40eb62', '4100000000', '경기도', '4150000000', '이천시',
    'BASIC_LOCAL_GOVERNMENT', '이천시청', 'http://www.icheon.go.kr', 'https://www.icheon.go.kr/portal/board/post/list.do?bcIdx=698&mid=0401010000&token=1717053488493',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'bf8276f3-957f-46c0-be9b-3983405f2f6b', '4100000000', '경기도', '4155000000', '안성시',
    'BASIC_LOCAL_GOVERNMENT', '안성시청', 'http://www.anseong.go.kr', 'https://www.anseong.go.kr/portal/bbs/list.do?ptIdx=16&mId=0501010000&token=1719299868283',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '5e8d4337-ab86-4ddd-9d88-d70e088e90d4', '4100000000', '경기도', '4131000000', '구리시',
    'BASIC_LOCAL_GOVERNMENT', '구리시청', 'https://www.guri.go.kr/www/index.do', 'https://www.guri.go.kr/www/selectBbsNttList.do?bbsNo=36&key=380',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd44faaa9-04d6-4474-a6e2-6f23bbf0cb10', '4100000000', '경기도', '4165000000', '포천시',
    'BASIC_LOCAL_GOVERNMENT', '포천시청', 'http://www.pocheon.go.kr', 'http://www.pocheon.go.kr/www/selectBbsNttList.do?bbsNo=18&key=3095&',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ca2567d6-e947-4854-9dbc-3fc0da833300', '4100000000', '경기도', '4143000000', '의왕시',
    'BASIC_LOCAL_GOVERNMENT', '의왕시청', 'http://www.uiwang.go.kr', 'http://www.uiwang.go.kr/UWKORINFO0101',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd79b8da8-2411-447c-9b56-e953749cf993', '4100000000', '경기도', '4183000000', '양평군',
    'BASIC_LOCAL_GOVERNMENT', '양평군청', 'http://www.yp21.go.kr', 'https://www.yp21.go.kr/www/selectBbsNttList.do?bbsNo=1&key=1111',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '527f01bb-387d-47b9-adf2-766957dcd01f', '4100000000', '경기도', '4167000000', '여주시',
    'BASIC_LOCAL_GOVERNMENT', '여주시청', 'https://www.yeoju.go.kr/www/index.do', 'https://www.yeoju.go.kr/www/selectBbsNttList.do?bbsNo=44&key=409',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'f95c8bbc-5fc3-4ecf-aa6e-96d8f3ed23da', '4100000000', '경기도', '4125000000', '동두천시',
    'BASIC_LOCAL_GOVERNMENT', '동두천시청', 'http://www.ddc.go.kr', 'http://www.ddc.go.kr/ddc/selectBbsNttList.do?bbsNo=24&key=104',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd290b46f-fded-48d1-a277-1ac6e7dc4718', '4100000000', '경기도', '4129000000', '과천시',
    'BASIC_LOCAL_GOVERNMENT', '과천시청', 'http://www.gccity.go.kr', 'https://www.gccity.go.kr/portal/bbs/list.do?ptIdx=111&mId=0301010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '6842d450-dd5c-4b65-acb4-19f2f34645f2', '4100000000', '경기도', '4182000000', '가평군',
    'BASIC_LOCAL_GOVERNMENT', '가평군청', 'http://www.gp.go.kr', 'http://www.gp.go.kr/portal/selectBbsNttList.do?bbsNo=150&key=501',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2acd428d-9cca-4dbe-8814-ef61e510e521', '4100000000', '경기도', '4180000000', '연천군',
    'BASIC_LOCAL_GOVERNMENT', '연천군청', 'http://www.yeoncheon.go.kr', 'https://www.yeoncheon.go.kr/www/selectBbsNttList.do?bbsNo=8&key=3386',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '98470aba-754a-4964-979d-872866708b77', '5100000000', '강원특별자치도', '5100000000', '강원특별자치도',
    'SIDO', '강원특별자치도청', 'https://state.gwd.go.kr/portal', 'https://state.gwd.go.kr/portal/bulletin/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'c3ad1101-5b40-4d64-9b7b-cc4e5e680f66', '5100000000', '강원특별자치도', '5111000000', '춘천시',
    'BASIC_LOCAL_GOVERNMENT', '춘천시청', 'https://www.chuncheon.go.kr/cityhall/', 'https://www.chuncheon.go.kr/cityhall/administrative-info/notice-info/notice-announcement/',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a0edfdc9-0b31-4a76-80f3-b497af6be45c', '5100000000', '강원특별자치도', '5113000000', '원주시',
    'BASIC_LOCAL_GOVERNMENT', '원주시청', 'http://www.wonju.go.kr', 'http://www.wonju.go.kr/www/selectBbsNttList.do?bbsNo=1&key=211',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '433fa37b-80e3-45bb-bc49-80bab08165af', '5100000000', '강원특별자치도', '5115000000', '강릉시',
    'BASIC_LOCAL_GOVERNMENT', '강릉시청', 'https://www.gn.go.kr/www/index.do', 'http://www.gn.go.kr/www/selectBbsNttList.do?bbsNo=12&key=258',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '66ab56e3-837a-4a71-b80f-fc6f76a29270', '5100000000', '강원특별자치도', '5117000000', '동해시',
    'BASIC_LOCAL_GOVERNMENT', '동해시청', 'http://dh.go.kr', 'https://www.dh.go.kr/www/selectBbsNttList.do?bbsNo=87&key=478',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준 | 검토 과정에서 공식 공고 URL 보정', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '90c9aa54-4bec-4b66-ae4c-2f32cf928455', '5100000000', '강원특별자치도', '5119000000', '태백시',
    'BASIC_LOCAL_GOVERNMENT', '태백시청', 'https://www.taebaek.go.kr/www/index.do', 'http://www.taebaek.go.kr/www/selectBbsNttList.do?bbsNo=24&key=351',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'b3a538d2-3631-4011-942a-a75b3af18db3', '5100000000', '강원특별자치도', '5121000000', '속초시',
    'BASIC_LOCAL_GOVERNMENT', '속초시청', 'http://www.sokcho.go.kr', 'https://www.sokcho.go.kr/sc/portal/sokchonews/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '86d50c3c-c56d-4c1d-b8f1-77230ee2acfc', '5100000000', '강원특별자치도', '5123000000', '삼척시',
    'BASIC_LOCAL_GOVERNMENT', '삼척시청', 'https://www.samcheok.go.kr/main.web', 'http://www.samcheok.go.kr/media/00083/00089.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '34e80a96-16a9-4e41-b5d4-054be1b9e541', '5100000000', '강원특별자치도', '5172000000', '홍천군',
    'BASIC_LOCAL_GOVERNMENT', '홍천군청', 'https://www.hongcheon.go.kr/www/index.do', 'https://www.hongcheon.go.kr/www/selectBbsNttList.do?bbsNo=1&key=255',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'fe7b75c0-29ca-4382-b3ef-ed176e201f5b', '5100000000', '강원특별자치도', '5173000000', '횡성군',
    'BASIC_LOCAL_GOVERNMENT', '횡성군청', 'https://www.hsg.go.kr/www/index.do', 'https://www.hsg.go.kr/www/selectBbsNttList.do?bbsNo=59&key=812&searchCtgry=%EB%8C%80%ED%91%9C&',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '834c01b3-ccb3-4f97-9b4e-37b194046263', '5100000000', '강원특별자치도', '5175000000', '영월군',
    'BASIC_LOCAL_GOVERNMENT', '영월군청', 'http://www.yw.go.kr', 'http://www.yw.go.kr/www/selectBbsNttList.do?bbsNo=15&key=25',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'f1484380-94ce-4922-8d10-394dac9ae5b8', '5100000000', '강원특별자치도', '5176000000', '평창군',
    'BASIC_LOCAL_GOVERNMENT', '평창군청', 'http://www.pc.go.kr/portal', 'http://www.pc.go.kr/portal/government/government-news/government-news-agency',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '715aed8c-549b-4360-9434-bd1222aadd5b', '5100000000', '강원특별자치도', '5177000000', '정선군',
    'BASIC_LOCAL_GOVERNMENT', '정선군청', 'https://www.jeongseon.go.kr/portal', 'http://www.jeongseon.go.kr/portal/openadmin/adminnews/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '786122ed-de7f-455f-a1a1-fdd8db990d87', '5100000000', '강원특별자치도', '5178000000', '철원군',
    'BASIC_LOCAL_GOVERNMENT', '철원군청', 'https://www.cwg.go.kr/www/index.do', 'https://www.cwg.go.kr/www/selectBbsNttList.do?bbsNo=24&key=206',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'cf1505da-ed60-42f1-bde6-e554e86c9291', '5100000000', '강원특별자치도', '5179000000', '화천군',
    'BASIC_LOCAL_GOVERNMENT', '화천군청', 'https://www.ihc.go.kr/www/index.do', 'http://www.ihc.go.kr/www/selectBbsNttList.do?bbsNo=11&key=2338',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'b9b5da94-ccff-4aed-8389-453e093ebfad', '5100000000', '강원특별자치도', '5180000000', '양구군',
    'BASIC_LOCAL_GOVERNMENT', '양구군청', 'https://www.yanggu.go.kr/', 'https://www.yanggu.go.kr/user_sub?gfnc=www&mu_idx=225',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a17699fa-ae46-4c14-bfd2-07ad58fc823e', '5100000000', '강원특별자치도', '5181000000', '인제군',
    'BASIC_LOCAL_GOVERNMENT', '인제군청', 'http://www.inje.go.kr', 'https://www.inje.go.kr/portal/adm/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '92350110-93fc-4c25-8cd4-fb8a7a023e86', '5100000000', '강원특별자치도', '5182000000', '고성군',
    'BASIC_LOCAL_GOVERNMENT', '고성군청', 'http://www.gwgs.go.kr', 'http://www.gwgs.go.kr/prog/bbsArticle/BBSMSTR_000000000412/list.do;jsessionid=06B856E72BB633499FEA49F8DCE9CC24',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '951463a5-2163-4d50-a600-c0b125fe274e', '5100000000', '강원특별자치도', '5183000000', '양양군',
    'BASIC_LOCAL_GOVERNMENT', '양양군청', 'https://www.yangyang.go.kr/gw/portal', 'https://www.yangyang.go.kr/gw/portal/yyc_news_notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '5af98a0b-4af5-491e-8fcf-a1d073670f95', '4300000000', '충청북도', '4300000000', '충청북도',
    'SIDO', '충청북도청', 'http://www.chungbuk.go.kr', 'https://www.chungbuk.go.kr/www/selectBbsNttList.do?bbsNo=60&key=421',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '95160506-e408-4adb-898d-d313c54a8553', '4300000000', '충청북도', '4311000000', '청주시',
    'BASIC_LOCAL_GOVERNMENT', '청주시청', 'https://intro.cheongju.go.kr/', 'https://www.cheongju.go.kr/www/selectBbsNttList.do?bbsNo=510&key=280&integrDeptCode=000100101',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '806a3bea-2018-4244-95a1-794d458761db', '4300000000', '충청북도', '4313000000', '충주시',
    'BASIC_LOCAL_GOVERNMENT', '충주시청', 'http://www.chungju.go.kr', 'http://www.chungju.go.kr/www/selectBbsNttList.do?bbsNo=5&key=506',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '67709fe3-329a-431e-9d88-e85e40eeaeca', '4300000000', '충청북도', '4315000000', '제천시',
    'BASIC_LOCAL_GOVERNMENT', '제천시청', 'http://www.jecheon.go.kr', 'http://www.jecheon.go.kr/www/selectBbsNttList.do?key=114&bbsNo=11',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'f51a05f9-c3df-475a-af45-fbf97db69fed', '4300000000', '충청북도', '4372000000', '보은군',
    'BASIC_LOCAL_GOVERNMENT', '보은군청', 'http://www.boeun.go.kr', 'https://www.boeun.go.kr/www/selectBbsNttList.do?bbsNo=4&key=134',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '51f894e6-40ac-4a39-bfda-267a34038631', '4300000000', '충청북도', '4373000000', '옥천군',
    'BASIC_LOCAL_GOVERNMENT', '옥천군청', 'http://www.oc.go.kr', 'http://www.oc.go.kr/www/selectBbsNttList.do?bbsNo=36&key=232&',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'aff2ce3b-8b30-41ea-a6c5-ee0b2119a21a', '4300000000', '충청북도', '4374000000', '영동군',
    'BASIC_LOCAL_GOVERNMENT', '영동군청', 'http://www.yd21.go.kr', 'http://www.yd21.go.kr/kr/html/sub02/020101.html',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a91db0dc-4346-4037-929f-cfc7e01755c4', '4300000000', '충청북도', '4374500000', '증평군',
    'BASIC_LOCAL_GOVERNMENT', '증평군청', 'http://www.jp.go.kr', 'http://www.jp.go.kr/kor/cop/bbs/BBSMSTR_000000000134/selectBoardList.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ef6783f9-81c3-4090-915f-0091346086eb', '4300000000', '충청북도', '4375000000', '진천군',
    'BASIC_LOCAL_GOVERNMENT', '진천군청', 'https://jincheon.go.kr', 'https://www.jincheon.go.kr/home/sub.do?menukey=2908',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2971da44-45b1-43ae-98de-9ad602890bd2', '4300000000', '충청북도', '4376000000', '괴산군',
    'BASIC_LOCAL_GOVERNMENT', '괴산군청', 'http://www.goesan.go.kr', 'https://www.goesan.go.kr/www/selectBbsNttList.do?bbsNo=190&key=135',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '0e51416a-9036-4b0d-a810-3d12377cc7b0', '4300000000', '충청북도', '4377000000', '음성군',
    'BASIC_LOCAL_GOVERNMENT', '음성군청', 'https://www.eumseong.go.kr/www/index.do', 'https://www.eumseong.go.kr/www/selectBbsNttList.do?bbsNo=6&key=78',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '38bed50a-8d8a-4919-ae32-29539f96e83a', '4300000000', '충청북도', '4380000000', '단양군',
    'BASIC_LOCAL_GOVERNMENT', '단양군청', 'http://www.danyang.go.kr', 'https://www.danyang.go.kr/dy21/975',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ed26e07c-7678-4143-a7b6-443152c38fa4', '4400000000', '충청남도', '4400000000', '충청남도',
    'SIDO', '충청남도청', 'http://www.chungnam.go.kr', 'https://www.chungnam.go.kr/cnportal/bbs/B0000230/list.do?menuNo=500497',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '7843d040-6d29-48eb-b707-c93a9bbe022b', '4400000000', '충청남도', '4413000000', '천안시',
    'BASIC_LOCAL_GOVERNMENT', '천안시청', 'http://www.cheonan.go.kr', 'http://www.cheonan.go.kr/cop/bbs/BBSMSTR_000000000462/selectBoardList.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2dde6f12-a4a7-4622-865d-f2dcaeb342e7', '4400000000', '충청남도', '4415000000', '공주시',
    'BASIC_LOCAL_GOVERNMENT', '공주시청', 'https://www.gongju.go.kr/kr/index.do', 'http://www.gongju.go.kr/bbs/BBSMSTR_000000000813/list.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'aee8d7b7-67de-46e5-8a7c-72ac09ead8f7', '4400000000', '충청남도', '4418000000', '보령시',
    'BASIC_LOCAL_GOVERNMENT', '보령시청', 'http://www.brcn.go.kr', 'http://www.brcn.go.kr/cop/bbs/BBSMSTR_000000000263/selectBoardList.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'dd2171c2-4724-4f95-809a-100a2787a04d', '4400000000', '충청남도', '4420000000', '아산시',
    'BASIC_LOCAL_GOVERNMENT', '아산시청', 'http://www.asan.go.kr', 'http://www.asan.go.kr/main/cms/?no=131',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '395b1ac5-9faf-429d-be5e-4386fa5b1d32', '4400000000', '충청남도', '4421000000', '서산시',
    'BASIC_LOCAL_GOVERNMENT', '서산시청', 'http://www.seosan.go.kr', 'http://www.seosan.go.kr/www/selectBbsNttList.do?bbsNo=97&key=1256',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '7e0955fe-8556-409c-ba37-fc02b4df0554', '4400000000', '충청남도', '4423000000', '논산시',
    'BASIC_LOCAL_GOVERNMENT', '논산시청', 'https://nonsan.go.kr/', 'http://www.nonsan.go.kr/kor/html/sub03/030101.html',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ed91fa6a-ceab-4b31-a0d6-769473463680', '4400000000', '충청남도', '4425000000', '계룡시',
    'BASIC_LOCAL_GOVERNMENT', '계룡시청', 'http://www.gyeryong.go.kr', 'http://www.gyeryong.go.kr/kr/html/sub03/030101.html',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '1086a4be-c7ce-4878-80d5-4baf4db906b2', '4400000000', '충청남도', '4427000000', '당진시',
    'BASIC_LOCAL_GOVERNMENT', '당진시청', 'http://www.dangjin.go.kr', 'http://www.dangjin.go.kr/cop/bbs/BBSMSTR_000000000013/selectBoardList.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '10ec7086-ec2d-4013-98af-5c20981fbdd2', '4400000000', '충청남도', '4471000000', '금산군',
    'BASIC_LOCAL_GOVERNMENT', '금산군청', 'http://www.geumsan.go.kr', 'https://www.geumsan.go.kr/kr/html/sub03/030101.html',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '0ffd5877-6a08-4392-b911-95fa68f58d24', '4400000000', '충청남도', '4476000000', '부여군',
    'BASIC_LOCAL_GOVERNMENT', '부여군청', 'https://www.buyeo.go.kr/html/kr/', 'https://www.buyeo.go.kr/_prog/_board/?code=news_01&site_dvs_cd=kr&menu_dvs_cd=0401',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '46c6d6da-50b7-4678-a2a7-e56ef1065184', '4400000000', '충청남도', '4477000000', '서천군',
    'BASIC_LOCAL_GOVERNMENT', '서천군청', 'http://www.seocheon.go.kr', 'http://www.seocheon.go.kr/cop/bbs/BBSMSTR_000000000056/selectBoardList.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '8478ed4e-c88f-4ef2-866e-7e3d0fa74e42', '4400000000', '충청남도', '4479000000', '청양군',
    'BASIC_LOCAL_GOVERNMENT', '청양군청', 'http://www.cheongyang.go.kr', 'http://www.cheongyang.go.kr/cop/bbs/BBSMSTR_000000000037/selectBoardList.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '67be225c-34ed-4e4b-a33d-4e15f5c8d578', '4400000000', '충청남도', '4480000000', '홍성군',
    'BASIC_LOCAL_GOVERNMENT', '홍성군청', 'http://www.hongseong.go.kr', 'https://www.hongseong.go.kr/prog/bbsArticle/BBSMSTR_000000000841/list.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '3e3ffece-38bf-4832-b77c-aec29f92704d', '4400000000', '충청남도', '4481000000', '예산군',
    'BASIC_LOCAL_GOVERNMENT', '예산군청', 'http://www.yesan.go.kr', 'https://www.yesan.go.kr/bbs/BBSMSTR_000000000046/list.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ab2860bb-9aed-4591-aa5d-07242fdd0133', '4400000000', '충청남도', '4482500000', '태안군',
    'BASIC_LOCAL_GOVERNMENT', '태안군청', 'http://www.taean.go.kr', 'http://www.taean.go.kr/cop/bbs/BBSMSTR_000000000036/selectBoardList.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '794c927b-324c-4a38-bde0-cd55d9ebc509', '5200000000', '전북특별자치도', '5200000000', '전북특별자치도',
    'SIDO', '전북특별자치도청', 'http://www.jeonbuk.go.kr', 'https://www.jeonbuk.go.kr/board/list.jeonbuk?boardId=BBS_0000005&menuCd=DOM_000000102001001000&contentsSid=76&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '79eeaf0b-84ce-4296-8df9-8ef7dad4ed9b', '5200000000', '전북특별자치도', '5211000000', '전주시',
    'BASIC_LOCAL_GOVERNMENT', '전주시청', 'http://www.jeonju.go.kr', 'https://www.jeonju.go.kr/planweb/board/list.9is?contentUid=ff8080818990c349018b041a87373953&boardUid=ff8080818990c349018b1dbaa78e4b41',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a532b2e1-ae3c-4f7f-be16-65fd5fb10d08', '5200000000', '전북특별자치도', '5213000000', '군산시',
    'BASIC_LOCAL_GOVERNMENT', '군산시청', 'http://www.gunsan.go.kr', 'http://www.gunsan.go.kr/main/m140',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '12c9ece3-dcc5-47b1-85d2-dedda4b992f3', '5200000000', '전북특별자치도', '5214000000', '익산시',
    'BASIC_LOCAL_GOVERNMENT', '익산시청', 'https://www.iksan.go.kr/index.do?menuUid=ff8080819a39930e019a4de8c1ae0afd', 'https://www.iksan.go.kr/board/post/list.do?boardUid=ff80808199dd1d7d0199e15235920a20&menuUid=ff80808198eafcbd019902aad8302bfa',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e73bec47-60d5-400b-9909-bbb739740581', '5200000000', '전북특별자치도', '5218000000', '정읍시',
    'BASIC_LOCAL_GOVERNMENT', '정읍시청', 'http://www.jeongeup.go.kr', 'http://www.jeongeup.go.kr/board/list.jeongeup?boardId=BBS_0000012&menuCd=DOM_000000101001001000&contentsSid=5&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2e69e3a6-a8f2-4bc1-b5b5-05a6aa2ed1a3', '5200000000', '전북특별자치도', '5219000000', '남원시',
    'BASIC_LOCAL_GOVERNMENT', '남원시청', 'http://www.namwon.go.kr', 'https://www.namwon.go.kr/board/post/list.do?boardUid=ff8080818ea1b850018ea1e3e9ad0081&menuUid=ff8080818e3beff0018e4075e410006e',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '12c8f327-5c35-4c03-aa4d-14ac823c9d77', '5200000000', '전북특별자치도', '5221000000', '김제시',
    'BASIC_LOCAL_GOVERNMENT', '김제시청', 'http://www.gimje.go.kr', 'https://www.gimje.go.kr/board/list.gimje?boardId=BBS_0000027&menuCd=DOM_000000104001000000&contentsSid=194&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '3fe96d7a-6498-4f00-81a4-231c031e72cf', '5200000000', '전북특별자치도', '5271000000', '완주군',
    'BASIC_LOCAL_GOVERNMENT', '완주군청', 'http://www.wanju.go.kr', 'https://www.wanju.go.kr/planweb/board/list.9is?contentUid=ff8080818b024d8e018b274f3fdd2ae2&boardUid=ff8080818a49961a018ab011af3543bc&categoryUid2=ff8080818bc7fa7c018bd69c596z9039&contentUid=ff8080818b024d8e018b274f3fdd2ae2&subPath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '8bc49f78-01ba-4084-a886-770e198907c1', '5200000000', '전북특별자치도', '5272000000', '진안군',
    'BASIC_LOCAL_GOVERNMENT', '진안군청', 'http://www.jinan.go.kr', 'https://www.jinan.go.kr/board/list.jinan?boardId=BBS_0000026&menuCd=DOM_000000107001000000&contentsSid=179&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a41040d9-7c61-40e8-9e90-9f10cd2bbdc7', '5200000000', '전북특별자치도', '5273000000', '무주군',
    'BASIC_LOCAL_GOVERNMENT', '무주군청', 'http://www.muju.go.kr', 'https://www.muju.go.kr/planweb/board/list.9is?contentUid=ff8080816c5f9d47016cbd3ae19f006b&boardUid=ff8080816d135a54016d1ecde9d8001a',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '09b53dad-6b02-4426-b4f4-f784920ff70c', '5200000000', '전북특별자치도', '5274000000', '장수군',
    'BASIC_LOCAL_GOVERNMENT', '장수군청', 'https://www.jangsu.go.kr/index.jangsu', 'https://www.jangsu.go.kr/board/list.jangsu?boardId=BBS_0000003&menuCd=DOM_000000102001001000&contentsSid=13&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '82dce31d-16af-47b6-be90-a8d21c8c32b5', '5200000000', '전북특별자치도', '5275000000', '임실군',
    'BASIC_LOCAL_GOVERNMENT', '임실군청', 'http://www.imsil.go.kr', 'http://www.imsil.go.kr/board/list.imsil?boardId=BBS_0000002&menuCd=DOM_000000103001001000&contentsSid=161&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'c1facd87-9bd7-4cec-a11d-ba3a206e437c', '5200000000', '전북특별자치도', '5277000000', '순창군',
    'BASIC_LOCAL_GOVERNMENT', '순창군청', 'http://sunchang.go.kr', 'http://www.sunchang.go.kr/board/list.sunchang?boardId=BBS_0000011&menuCd=DOM_000000110001001000&paging=ok&startPage=1',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'fcf7c7d8-b9db-407b-9532-0e00bb327d05', '5200000000', '전북특별자치도', '5279000000', '고창군',
    'BASIC_LOCAL_GOVERNMENT', '고창군청', 'http://www.gochang.go.kr', 'http://www.gochang.go.kr/board/list.gochang?boardId=BBS_0000083&menuCd=DOM_000000102001001000&paging=ok&startPage=1',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '3b56c1b3-ee52-4fe7-93fd-4c5e484f75a1', '5200000000', '전북특별자치도', '5280000000', '부안군',
    'BASIC_LOCAL_GOVERNMENT', '부안군청', 'http://www.buan.go.kr', 'http://www.buan.go.kr/board/list.buan?boardId=BBS_0000053&menuCd=DOM_000000103001001000&contentsSid=687&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '9b5dd695-4f29-4549-8e49-371dbf33d3c1', '4600000000', '전라남도', '4611000000', '목포시',
    'BASIC_LOCAL_GOVERNMENT', '목포시청', 'http://www.mokpo.go.kr', 'https://www.mokpo.go.kr/www/mokpo_news/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '159c52f9-f5e0-4bf6-bcb3-2d0a1f5f59ed', '4600000000', '전라남도', '4613000000', '여수시',
    'BASIC_LOCAL_GOVERNMENT', '여수시청', 'http://www.yeosu.go.kr', 'https://www.yeosu.go.kr/www/govt/news/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd9ed4713-2332-4be4-a122-379826a61df7', '4600000000', '전라남도', '4615000000', '순천시',
    'BASIC_LOCAL_GOVERNMENT', '순천시청', 'http://www.suncheon.go.kr', 'http://www.suncheon.go.kr/kr/news/0001/0001/',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '217ab999-44b8-4dc0-a901-e552160639c3', '4600000000', '전라남도', '4617000000', '나주시',
    'BASIC_LOCAL_GOVERNMENT', '나주시청', 'http://www.naju.go.kr', 'http://www.naju.go.kr/www/administration/new/notify',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e1c57647-931a-4d33-b67c-79db9196a7f3', '4600000000', '전라남도', '4623000000', '광양시',
    'BASIC_LOCAL_GOVERNMENT', '광양시청', 'https://gwangyang.go.kr/', 'https://gwangyang.go.kr/board.es?mid=a11001000000&bid=0001',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'b864b5c8-23e5-4a83-82d2-2a8d2c4b4e4b', '4600000000', '전라남도', '4671000000', '담양군',
    'BASIC_LOCAL_GOVERNMENT', '담양군청', 'http://www.damyang.go.kr', 'https://www.damyang.go.kr/board/list?boardId=BBS_0000001&domainId=DOM_0000001&contentsSid=1&menuCd=DOM_000000190001001000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '44973191-3b6a-4aab-bbdf-be557164c799', '4600000000', '전라남도', '4672000000', '곡성군',
    'BASIC_LOCAL_GOVERNMENT', '곡성군청', 'http://www.gokseong.go.kr', 'https://www.gokseong.go.kr/kr/board/list.do?bbsId=BBS_000000000000150&menuNo=102001001000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '5381eb53-325b-4667-827e-0da88e54b57c', '4600000000', '전라남도', '4673000000', '구례군',
    'BASIC_LOCAL_GOVERNMENT', '구례군청', 'https://www.gurye.go.kr/kr/main.do', 'https://www.gurye.go.kr/board/list.do?bbsId=BBS_0000000000000056&menuNo=115004001000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'eb852e35-d9b0-4aae-a63d-c293cfb1d8f7', '4600000000', '전라남도', '4677000000', '고흥군',
    'BASIC_LOCAL_GOVERNMENT', '고흥군청', 'https://www.goheung.go.kr/index.do', 'https://www.goheung.go.kr/boardList.do?pageId=www96&boardId=BD_00018',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '1453d321-6a1d-4d8a-a1ef-f6073c36d6d8', '4600000000', '전라남도', '4678000000', '보성군',
    'BASIC_LOCAL_GOVERNMENT', '보성군청', 'http://www.boseong.go.kr', 'http://www.boseong.go.kr/www/open_administration/city_news/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'a140b21a-df1c-40d5-931e-07701db4d0fb', '4600000000', '전라남도', '4679000000', '화순군',
    'BASIC_LOCAL_GOVERNMENT', '화순군청', 'https://www.hwasun.go.kr/', 'https://www.hwasun.go.kr/board.do?S=S01&M=020102000000&b_code=0000000002',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'c5924fdf-85f7-4b30-92e9-c8c8634564ef', '4600000000', '전라남도', '4680000000', '장흥군',
    'BASIC_LOCAL_GOVERNMENT', '장흥군청', 'http://www.jangheung.go.kr', 'http://www.jangheung.go.kr/www/organization/news/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '3512d948-aae2-4596-9e5c-b49c118e03e2', '4600000000', '전라남도', '4681000000', '강진군',
    'BASIC_LOCAL_GOVERNMENT', '강진군청', 'http://www.gangjin.go.kr', 'http://www.gangjin.go.kr/www/government/news/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'aea013a6-e090-44de-8c5a-ad83f0984485', '4600000000', '전라남도', '4682000000', '해남군',
    'BASIC_LOCAL_GOVERNMENT', '해남군청', 'http://www.haenam.go.kr', 'https://www.haenam.go.kr/planweb/board/list.9is?contentUid=18e3368f5d745106015de95ebe732057&boardUid=18e3368f5fb80fdc015fdc42b7e003e0&contentUid=18e3368f5d745106015de95ebe732057',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'CHECK_REQUIRED', false, 'CHECK_REQUIRED'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '870385b6-75f8-42c3-9d87-fe129346d745', '4600000000', '전라남도', '4683000000', '영암군',
    'BASIC_LOCAL_GOVERNMENT', '영암군청', 'http://www.yeongam.go.kr', 'http://www.yeongam.go.kr/home/www/open_information/yeongam_news/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '3244e413-3907-4f65-a9b6-b187d3d9ab1c', '4600000000', '전라남도', '4684000000', '무안군',
    'BASIC_LOCAL_GOVERNMENT', '무안군청', 'http://www.muan.go.kr', 'http://www.muan.go.kr/www/openmuan/new/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'acad801b-401f-4cbe-9a68-2b93acc73b73', '4600000000', '전라남도', '4686000000', '함평군',
    'BASIC_LOCAL_GOVERNMENT', '함평군청', 'http://www.hampyeong.go.kr', 'http://www.hampyeong.go.kr/boardList.do?pageId=www272&boardId=NOTICE',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '3704d975-75ca-4de6-a4c0-f39112642352', '4600000000', '전라남도', '4687000000', '영광군',
    'BASIC_LOCAL_GOVERNMENT', '영광군청', 'http://www.yeonggwang.go.kr', 'https://www.yeonggwang.go.kr/bbs/?b_id=news_notice&site=headquarter_new&mn=9054',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'f46f880d-d354-4774-9b29-9bf028cd3134', '4600000000', '전라남도', '4688000000', '장성군',
    'BASIC_LOCAL_GOVERNMENT', '장성군청', 'http://www.jangseong.go.kr', 'http://www.jangseong.go.kr/home/www/news/jangseong/notice',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '98c811f6-4ada-4d3c-b63c-369bdf75f610', '4600000000', '전라남도', '4689000000', '완도군',
    'BASIC_LOCAL_GOVERNMENT', '완도군청', 'https://www.wando.go.kr/intro/intro.htm', 'https://www.wando.go.kr/wando/sub.cs?m=298',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '8b54ec9e-4c85-4730-b036-bc4c506374ab', '4600000000', '전라남도', '4690000000', '진도군',
    'BASIC_LOCAL_GOVERNMENT', '진도군청', 'https://www.jindo.go.kr/intro.jsp', 'http://www.jindo.go.kr/home/board/B0052.cs?m=23',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd649cff5-35c9-47d9-b20b-e84b8496c6f4', '4600000000', '전라남도', '4691000000', '신안군',
    'BASIC_LOCAL_GOVERNMENT', '신안군청', 'http://www.shinan.go.kr', 'http://www.shinan.go.kr/home/www/openinfo/participation_07/participation_07_02',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2658fd72-01c7-4966-b2b2-068661cd0019', '4700000000', '경상북도', '4700000000', '경상북도',
    'SIDO', '경상북도청', 'http://www.gb.go.kr', 'https://www.gb.go.kr/Main/page.do?mnu_uid=6786&BD_CODE=bbs_gongji',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '9fd5d5e7-b983-4456-9f6e-df29c196c02d', '4700000000', '경상북도', '4711000000', '포항시',
    'BASIC_LOCAL_GOVERNMENT', '포항시청', 'https://www.pohang.go.kr', 'https://www.pohang.go.kr/portal/saeol/gosi/list.do?mid=0202010000&token=1710896483460',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e9ac78d6-8d6c-4896-a5f4-29bf7021b739', '4700000000', '경상북도', '4713000000', '경주시',
    'BASIC_LOCAL_GOVERNMENT', '경주시청', 'http://www.gyeongju.go.kr', 'http://www.gyeongju.go.kr/open_content/ko/page.do?mnu_uid=416',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'dce10b71-7ebc-44d5-b050-c60221d5d908', '4700000000', '경상북도', '4715000000', '김천시',
    'BASIC_LOCAL_GOVERNMENT', '김천시청', 'http://gc.go.kr', 'https://www.gc.go.kr/portal/bbs/list.do?ptIdx=1807&mId=1202100000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'fd78e758-1774-499d-97aa-6369299baee4', '4700000000', '경상북도', '4717000000', '안동시',
    'BASIC_LOCAL_GOVERNMENT', '안동시청', 'https://www.andong.go.kr/', 'https://www.andong.go.kr/portal/saeol/gosi/list.do?mId=0401020100',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'c3e91b2f-6e21-41c9-8ba7-a3f3763bb569', '4700000000', '경상북도', '4719000000', '구미시',
    'BASIC_LOCAL_GOVERNMENT', '구미시청', 'https://www.gumi.go.kr', 'https://www.gumi.go.kr/portal/board/post/list.do?bcIdx=1&mid=0401020000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '268fa41c-7674-4f59-aa3e-35ee7238c7ad', '4700000000', '경상북도', '4721000000', '영주시',
    'BASIC_LOCAL_GOVERNMENT', '영주시청', 'http://www.yeongju.go.kr', 'http://www.yeongju.go.kr/open_content/main/page.do?mnu_uid=3899&',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '64f39ea5-36a1-44e4-9202-1a287e89ac21', '4700000000', '경상북도', '4723000000', '영천시',
    'BASIC_LOCAL_GOVERNMENT', '영천시청', 'http://www.yc.go.kr', 'http://www.yc.go.kr/portal/bbs/list.do?ptIdx=544&mId=0301010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '8d048941-46ad-4dac-9259-fb990e886675', '4700000000', '경상북도', '4725000000', '상주시',
    'BASIC_LOCAL_GOVERNMENT', '상주시청', 'https://www.sangju.go.kr/main2.tc', 'https://www.sangju.go.kr/page/10297/10606.tc',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준 | 검토 과정에서 공식 공고 URL 보정', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '52965c53-ea2a-414e-9ef6-4490eeb0abe6', '4700000000', '경상북도', '4728000000', '문경시',
    'BASIC_LOCAL_GOVERNMENT', '문경시청', 'http://www.gbmg.go.kr', 'http://www.gbmg.go.kr/portal/bbs/list.do?ptIdx=73&mId=0301010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ec17c20a-1b81-4450-93f5-6dd6cdedae37', '4700000000', '경상북도', '4729000000', '경산시',
    'BASIC_LOCAL_GOVERNMENT', '경산시청', 'http://gbgs.go.kr/', 'https://www.gbgs.go.kr/open_content/ko/page.do?mnu_uid=2159&',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '9e82c1f0-0096-424f-b089-a11e3f1372cd', '4700000000', '경상북도', '4773000000', '의성군',
    'BASIC_LOCAL_GOVERNMENT', '의성군청', 'http://www.usc.go.kr', 'https://www.usc.go.kr/ko/page.do?mnu_uid=156&',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '207ac463-9391-4d01-8dfa-fc201a8df0c0', '4700000000', '경상북도', '4775000000', '청송군',
    'BASIC_LOCAL_GOVERNMENT', '청송군청', 'http://www.cs.go.kr', 'http://www.cs.go.kr/news/00002679/00002687.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '993af096-96fe-442e-85b4-382bb8283792', '4700000000', '경상북도', '4776000000', '영양군',
    'BASIC_LOCAL_GOVERNMENT', '영양군청', 'http://www.yyg.go.kr', 'http://www.yyg.go.kr/www/organization/yyg_news',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'bbfa423a-37d9-4610-aa15-fcc95511cd70', '4700000000', '경상북도', '4777000000', '영덕군',
    'BASIC_LOCAL_GOVERNMENT', '영덕군청', 'http://www.yd.go.kr', 'http://www.yd.go.kr/?page_id=752',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'f89e8142-9f40-43bc-99cb-8c96de9679d6', '4700000000', '경상북도', '4782000000', '청도군',
    'BASIC_LOCAL_GOVERNMENT', '청도군청', 'https://www.cheongdo.go.kr/open.content/ko/', 'https://www.cheongdo.go.kr/portal/board/post/list.do?bcIdx=510&mid=0301010000&token=1700628210004',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '86ed3756-f05a-4652-9ce2-1bb8bbb11784', '4700000000', '경상북도', '4783000000', '고령군',
    'BASIC_LOCAL_GOVERNMENT', '고령군청', 'http://www.goryeong.go.kr', 'http://www.goryeong.go.kr/kor/boardList.do?IDX=152&BRD_ID=1019',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'de83c255-88cc-4cc8-917f-100d5fb50571', '4700000000', '경상북도', '4784000000', '성주군',
    'BASIC_LOCAL_GOVERNMENT', '성주군청', 'http://www.sj.go.kr', 'http://www.sj.go.kr/page.do?mnu_uid=1024',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'e726511a-066c-4d1b-a85e-b90a654429cd', '4700000000', '경상북도', '4785000000', '칠곡군',
    'BASIC_LOCAL_GOVERNMENT', '칠곡군청', 'http://www.chilgok.go.kr', 'https://www.chilgok.go.kr/portal/bbs/list.do?ptIdx=111&mId=0201010000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'b3d47d75-ec2c-464f-ae2b-0e09d22ca3ae', '4700000000', '경상북도', '4790000000', '예천군',
    'BASIC_LOCAL_GOVERNMENT', '예천군청', 'http://www.ycg.kr', 'http://www.ycg.kr/open.content/ko/administrative/news/notice/',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '369b6d71-adca-4b7a-a848-f64c1ea43286', '4700000000', '경상북도', '4792000000', '봉화군',
    'BASIC_LOCAL_GOVERNMENT', '봉화군청', 'http://www.bonghwa.go.kr', 'http://www.bonghwa.go.kr/open.content/ko/news/news/board/',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '9267f438-3bbc-4a29-bfce-f07d83b8fc1a', '4700000000', '경상북도', '4793000000', '울진군',
    'BASIC_LOCAL_GOVERNMENT', '울진군청', 'http://www.uljin.go.kr', 'http://www.uljin.go.kr/board/list.uljin?boardId=BBS_NOTICE_UJ&menuCd=DOM_000000103002001000&contentsSid=68&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '00803c74-3bd0-437c-bd37-15498673f05d', '4700000000', '경상북도', '4794000000', '울릉군',
    'BASIC_LOCAL_GOVERNMENT', '울릉군청', 'http://www.ulleung.go.kr', 'https://www.ulleung.go.kr/ko/page.do?mnu_uid=570&',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'ab3f0810-e5f2-47a9-b3cb-33718cbfcc9b', '4800000000', '경상남도', '4800000000', '경상남도',
    'SIDO', '경상남도청', 'http://www.gyeongnam.go.kr', 'https://www.gyeongnam.go.kr/board/list.gyeong?boardId=BBS_0000057&menuCd=DOM_000000135001001000&contentsSid=6951&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '4110be5d-2145-4082-ba13-c86a5e03d602', '4800000000', '경상남도', '4812000000', '창원시',
    'BASIC_LOCAL_GOVERNMENT', '창원시청', 'http://www.changwon.go.kr', 'https://www.changwon.go.kr/cwportal/10310/10429/10430.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '833af43a-20fb-4f67-8bd6-59346adb0714', '4800000000', '경상남도', '4817000000', '진주시',
    'BASIC_LOCAL_GOVERNMENT', '진주시청', 'http://www.jinju.go.kr', 'http://www.jinju.go.kr/00130/02730/00136.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'b422e869-497d-47f3-962c-87ee5ca41b25', '4800000000', '경상남도', '4822000000', '통영시',
    'BASIC_LOCAL_GOVERNMENT', '통영시청', 'http://www.tongyeong.go.kr', 'http://www.tongyeong.go.kr/00852/00853/00854.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'f8850156-746c-41d3-94d0-cf7dd2f68933', '4800000000', '경상남도', '4824000000', '사천시',
    'BASIC_LOCAL_GOVERNMENT', '사천시청', 'http://www.sacheon.go.kr', 'http://www.sacheon.go.kr/news/00009/00010.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '00a812a6-08bc-40bd-a37d-651aceac051c', '4800000000', '경상남도', '4825000000', '김해시',
    'BASIC_LOCAL_GOVERNMENT', '김해시청', 'http://www.gimhae.go.kr', 'http://www.gimhae.go.kr/03360/00023/00024.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '4a24e29d-2b54-45fe-bd6a-b48552011e19', '4800000000', '경상남도', '4827000000', '밀양시',
    'BASIC_LOCAL_GOVERNMENT', '밀양시청', 'http://www.miryang.go.kr', 'https://www.miryang.go.kr/web/bbs/selectNoticeList.do?mnNo=20901000000',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '46c00ce4-a2e3-4926-90b5-6e2dd4e6e3f6', '4800000000', '경상남도', '4831000000', '거제시',
    'BASIC_LOCAL_GOVERNMENT', '거제시청', 'http://www.geoje.go.kr', 'https://www.geoje.go.kr/board/list.geoje?boardId=BBS_0000008&menuCd=DOM_000008902001001000&contentsSid=9633&cpath',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '18a567ee-c2cb-46bb-8ef5-dad73a5dabab', '4800000000', '경상남도', '4833000000', '양산시',
    'BASIC_LOCAL_GOVERNMENT', '양산시청', 'http://www.yangsan.go.kr', 'https://www.yangsan.go.kr/portal/board/post/list.do?bcIdx=293&mid=0101010000&token=1717053985359',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd08df800-1613-488a-ab1d-d2ef80a87f3f', '4800000000', '경상남도', '4872000000', '의령군',
    'BASIC_LOCAL_GOVERNMENT', '의령군청', 'http://www.uiryeong.go.kr', 'https://www.uiryeong.go.kr/board/list.uiryeong?boardId=BBS_0000085&menuCd=DOM_000000203001001000&contentsSid=185',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '23a95918-a668-4b81-becc-d4a9f94ef510', '4800000000', '경상남도', '4873000000', '함안군',
    'BASIC_LOCAL_GOVERNMENT', '함안군청', 'http://www.haman.go.kr', 'http://www.haman.go.kr/02385/02386/02387.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '09df0ff0-45b2-4065-b282-357d0ce2b5e7', '4800000000', '경상남도', '4874000000', '창녕군',
    'BASIC_LOCAL_GOVERNMENT', '창녕군청', 'http://www.cng.go.kr', 'https://www.cng.go.kr/03516/01549.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'b92093e1-ca61-4c80-8002-59f7d18b8002', '4800000000', '경상남도', '4882000000', '고성군',
    'BASIC_LOCAL_GOVERNMENT', '고성군청', 'http://www.goseong.go.kr', 'https://www.goseong.go.kr/board/list.goseong?boardId=BBS_0000118&menuCd=DOM_000000102002001000&contentsSid=28&cpath=',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '1290146b-fee2-4822-adea-ba88b7eae76c', '4800000000', '경상남도', '4884000000', '남해군',
    'BASIC_LOCAL_GOVERNMENT', '남해군청', 'http://www.namhae.go.kr', 'http://www.namhae.go.kr/socialm/board/List.do?gcode=1131&&pageCd=SM010101000&siteGubun=socialm',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '3914cb44-6ad1-4459-9285-53c0fa3e47cc', '4800000000', '경상남도', '4885000000', '하동군',
    'BASIC_LOCAL_GOVERNMENT', '하동군청', 'http://www.hadong.go.kr', 'http://www.hadong.go.kr/media/00008/00009.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd4cfb182-1b20-4d83-ad12-81cb8cecad75', '4800000000', '경상남도', '4886000000', '산청군',
    'BASIC_LOCAL_GOVERNMENT', '산청군청', 'http://www.sancheong.go.kr', 'http://www.sancheong.go.kr/www/selectBbsNttList.do?key=157&bbsNo=1',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'd46f64da-d802-452f-bb9b-e40ab94b160e', '4800000000', '경상남도', '4887000000', '함양군',
    'BASIC_LOCAL_GOVERNMENT', '함양군청', 'http://www.hygn.go.kr', 'https://www.hygn.go.kr/00429/00543/00547.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2cd23007-917f-4070-9c7d-68d5a5f042c7', '4800000000', '경상남도', '4888000000', '거창군',
    'BASIC_LOCAL_GOVERNMENT', '거창군청', 'http://www.geochang.go.kr', 'http://www.geochang.go.kr/news/board/List.do?gcode=1002&&pageCd=NW0101000000&siteGubun=portal',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '2d2efb95-aae7-49b6-bd4d-a4dcec7fd2db', '4800000000', '경상남도', '4889000000', '합천군',
    'BASIC_LOCAL_GOVERNMENT', '합천군청', 'http://www.hc.go.kr', 'https://www.hc.go.kr/04923/04924/04945.web',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '353dbc6c-41e8-4212-9082-2cb644a16130', '5000000000', '제주특별자치도', '5000000000', '제주특별자치도',
    'SIDO', '제주특별자치도청', 'http://www.jeju.go.kr', 'http://www.jeju.go.kr/news/news/news.htm',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    '3c498218-9ed1-4abb-8f8e-bbb9a031852e', '5000000000', '제주특별자치도', '5011000000', '제주시',
    'ADMINISTRATIVE_CITY', '제주시청', 'http://www.jejusi.go.kr/index.ac', 'http://www.jejusi.go.kr/information/intro/news.do',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

INSERT INTO local_government_notice_sources (
    id, sido_code, sido_name, sigungu_code, sigungu_name, institution_type_code, institution_name,
    homepage_url, notice_url, page_type_code, parser_profile_code, collection_hint, confidence_code,
    validation_status_code, is_enabled, collection_status_code
) VALUES (
    'dc3c6a95-d863-44de-9144-a0841c82021d', '5000000000', '제주특별자치도', '5013000000', '서귀포시',
    'ADMINISTRATIVE_CITY', '서귀포시청', 'https://www.seogwipo.go.kr/index.htm', 'http://www.seogwipo.go.kr/info/news/notice.htm',
    'official_news_url', 'MANUAL_ONLY', '소상공인, 중소기업육성기금, 특별신용보증, 경영안정, 융자, 고시공고, 공지사항 | 행정안전부 내고장알리미 대표 누리집/새소식 URL 기준', 'MEDIUM',
    'VERIFIED', false, 'READY'
) ON CONFLICT DO NOTHING;

-- Seed verification: fail the migration if the reviewed registry count is not preserved.
DO $$
BEGIN
    IF (SELECT count(1) FROM local_government_notice_sources WHERE deleted_at IS NULL) <> 244 THEN
        RAISE EXCEPTION 'Expected 244 local-government notice sources';
    END IF;
    IF (SELECT count(DISTINCT sigungu_code) FROM local_government_notice_sources WHERE deleted_at IS NULL) <> 244 THEN
        RAISE EXCEPTION 'Expected 244 unique local-government district codes';
    END IF;
END $$;
