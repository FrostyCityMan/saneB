-- Correct reviewed local-government notice URLs and add a constrained per-source request profile.
-- Sources remain disabled until the full parser QA assigns a verified parser profile.

ALTER TABLE local_government_notice_sources
    ADD COLUMN request_profile_code varchar(30) NOT NULL DEFAULT 'DEFAULT',
    ADD COLUMN collection_endpoint_url text;

ALTER TABLE local_government_notice_sources
    ADD CONSTRAINT ck_local_government_notice_sources_request_profile CHECK (
        request_profile_code IN ('DEFAULT', 'BROWSER_HTTP1')
    );

ALTER TABLE local_government_notice_parser_profiles
    ADD COLUMN response_type_code varchar(20) NOT NULL DEFAULT 'HTML',
    ADD COLUMN json_items_path varchar(200),
    ADD COLUMN json_title_field varchar(100),
    ADD COLUMN json_date_field varchar(100),
    ADD COLUMN json_link_field varchar(100),
    ADD COLUMN json_link_template text;

ALTER TABLE local_government_notice_parser_profiles
    ADD CONSTRAINT ck_local_government_notice_parser_profiles_response_type CHECK (
        response_type_code IN ('HTML', 'JSON')
    );

ALTER TABLE local_government_notice_parser_profiles
    DROP CONSTRAINT ck_local_government_notice_parser_profiles_type;

ALTER TABLE local_government_notice_parser_profiles
    ADD CONSTRAINT ck_local_government_notice_parser_profiles_type CHECK (
        parser_type_code IN (
            'SAEOL_GOSI', 'SPRING_BBS', 'JSP_BBS', 'TC_GOSI',
            'GENERIC_TABLE', 'GENERIC_LIST', 'HEURISTIC_NOTICE', 'GENERIC_JSON', 'MANUAL_ONLY'
        )
    );

INSERT INTO local_government_notice_parser_profiles (
    id, profile_code, profile_name, parser_type_code,
    list_item_selector, title_selector, date_selector, link_selector, date_pattern, is_enabled,
    response_type_code, json_items_path, json_title_field, json_date_field, json_link_field, json_link_template
) VALUES (
    'a12cf84c-e91f-47c0-875e-5066a205de18',
    'CHUNCHEON_NOTICE_JSON',
    '춘천시 고시공고 JSON',
    'GENERIC_JSON',
    NULL,
    NULL,
    NULL,
    NULL,
    'yyyy-MM-dd',
    true,
    'JSON',
    'noticeList',
    'notAncmtSj',
    'pbsHopYmd',
    'notAncmtMgtNo',
    '/cityhall/administrative-info/notice-info/notice-announcement/view/?notAncmtMgtNo={value}'
)
ON CONFLICT (profile_code) DO NOTHING;

WITH reviewed_url (public_code, notice_url) AS (
    VALUES
    ('LGS-000011', 'https://www.dobong.go.kr/bbs.asp?code=10008769'),
    ('LGS-000012', 'https://www.nowon.kr/www/user/bbs/BD_selectBbsList.do?q_bbsCode=1001&q_estnColumn1=11'),
    ('LGS-000021', 'https://www.dongjak.go.kr/portal/bbs/B0000022/list.do?menuNo=200641'),
    ('LGS-000040', 'https://www.yeonje.go.kr/portal/bbs/list.do?ptIdx=10&mId=0206010000'),
    ('LGS-000042', 'https://www.sasang.go.kr/board/list.sasang?boardId=BBS_0000001&menuCd=DOM_000000104008001000&startPage=1'),
    ('LGS-000052', 'https://eminwon.dalseong.daegu.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,04'),
    ('LGS-000078', 'https://www.junggu.ulsan.kr/board/list.ulsan?boardId=BBS_0000057&contentsSid=1&menuCd=DOM_000000102003001000'),
    ('LGS-000089', 'https://www.seongnam.go.kr/city/1000052/30001/bbsList.do'),
    ('LGS-000093', 'https://www.pyeongtaek.go.kr/pyeongtaek/board/post/list.do?bcIdx=41&mid=0401010000'),
    ('LGS-000145', 'https://www.eumseong.go.kr/www/selectBbsNttList.do?bbsNo=6&key=350'),
    ('LGS-000148', 'https://www.cheonan.go.kr/bbs/BBSMSTR_000000000450/list.do'),
    ('LGS-000158', 'https://www.seocheon.go.kr/bbs/BBSMSTR_000000000268/list.do'),
    ('LGS-000175', 'https://www.sunchang.go.kr/index.do?menuUid=ff8080819a2f0e3b019a5d1b0c40164a'),
    ('LGS-000220', 'https://www.bonghwa.go.kr/portal/board/post/list.do?bcIdx=100&mid=0201010000'),
    ('LGS-000233', 'https://eminwon.haman.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01,04'),
    ('LGS-000240', 'https://www.geochang.go.kr/00445/00451.web')
)
UPDATE local_government_notice_sources AS source
SET notice_url = reviewed_url.notice_url,
    parser_profile_code = 'MANUAL_ONLY',
    validation_status_code = 'CHECK_REQUIRED',
    is_enabled = false,
    collection_status_code = 'CHECK_REQUIRED',
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
    'LGS-000008', 'LGS-000013', 'LGS-000034', 'LGS-000094', 'LGS-000108',
    'LGS-000120', 'LGS-000122', 'LGS-000128', 'LGS-000130', 'LGS-000133',
    'LGS-000135', 'LGS-000141', 'LGS-000149', 'LGS-000242'
)
  AND deleted_at IS NULL;

UPDATE local_government_notice_sources
SET collection_endpoint_url = 'https://www.chuncheon.go.kr/_chuncheon/noticeList.do?pageIndex=1&searchWrd=&searchCnd=',
    request_profile_code = 'BROWSER_HTTP1',
    parser_profile_code = 'MANUAL_ONLY',
    validation_status_code = 'CHECK_REQUIRED',
    is_enabled = false,
    collection_status_code = 'CHECK_REQUIRED',
    updated_at = now()
WHERE public_code = 'LGS-000117'
  AND deleted_at IS NULL;
