-- Correct official legal-notice targets that were unstable or still pointed to general notices.
-- Existing snapshots, collection runs, run items and audit logs are intentionally preserved.
ALTER TABLE local_government_notice_sources
    DROP CONSTRAINT ck_local_government_notice_sources_request_profile;
ALTER TABLE local_government_notice_sources
    ADD CONSTRAINT ck_local_government_notice_sources_request_profile CHECK (
        request_profile_code IN (
            'DEFAULT', 'BROWSER_HTTP1', 'LEGACY_BROWSER', 'TLS12_BROWSER', 'SESSION_BROWSER'
        )
    );

WITH reviewed_url(public_code, notice_url) AS (
    VALUES
        (
            'LGS-000094',
            'https://www.anyang.go.kr/main/selectEminwonList.do?key=4101&notAncmtSeCode=01%2C04&pageIndex=1'
        ),
        (
            'LGS-000229',
            'https://miryang.go.kr/web/eMiryangMinwonList.do?mnNo=20903000000&nmmKind1=01&nmmKind2=02&nmmKind3=03&nmmKind4=04&nmmKind5=05&nmmKind6=06&nmmKind7=07&owd=&pageIndex=1&searchCondition=&searchKeyword='
        ),
        (
            'LGS-000239',
            'https://eminwon.hygn.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01%2C02%2C03%2C04%2C07'
        ),
        (
            'LGS-000230',
            'https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001&startPage=1'
        ),
        (
            'LGS-000059',
            'https://biz.namdong.go.kr/main/news/announce.jsp'
        ),
        (
            'LGS-000122',
            'https://www.sokcho.go.kr/sc/portal/sokchonews/notification'
        )
)
UPDATE local_government_notice_sources AS source
SET notice_url = reviewed_url.notice_url,
    page_type_code = 'public_notice_board',
    source_board_type_code = 'LEGAL_NOTICE',
    collection_policy_code = 'COLLECT_ALL',
    is_semantically_verified = true,
    semantic_verified_at = now(),
    semantic_verification_note = CASE reviewed_url.public_code
        WHEN 'LGS-000094'
            THEN '2026-07-27 안양시 공식 고시공고 서버 렌더링 목록과 최근 게시물 표본 확인'
        WHEN 'LGS-000229'
            THEN '2026-07-27 밀양시 공식 고시·공고·채용 메뉴와 최근 게시물 표본 확인'
        WHEN 'LGS-000239'
            THEN '2026-07-27 함양군 공식 고시·공고 메뉴의 새올 전자민원 연결과 최근 게시물 표본 확인'
        WHEN 'LGS-000230'
            THEN '2026-07-27 거제시 공식 고시공고 메뉴와 최근 게시물 표본 확인, HTTPS 유지용 목록 페이지 명시'
        WHEN 'LGS-000059'
            THEN '2026-07-27 남동구청 공식 남동구 고시공고 메뉴와 최근 게시물 표본 확인'
        WHEN 'LGS-000122'
            THEN '2026-07-27 속초시 공식 고시공고 화면과 공개 새올 목록의 최근 게시물 표본 확인'
    END,
    validation_status_code = 'VERIFIED',
    collection_status_code = 'READY',
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

-- Anyang's official HTTPS board requires TLS 1.2 and uses the existing standard table parser.
UPDATE local_government_notice_sources
SET collection_endpoint_url = 'https://www.anyang.go.kr/main/selectEminwonList.do?key=4101&notAncmtSeCode=01%2C04&pageIndex=1',
    parser_profile_code = 'SPRING_BBS',
    request_method_code = 'GET',
    request_form_json = NULL,
    updated_at = now()
WHERE public_code = 'LGS-000094'
  AND deleted_at IS NULL;

UPDATE local_government_notice_sources
SET request_profile_code = 'TLS12_BROWSER',
    updated_at = now()
WHERE public_code IN ('LGS-000094')
  AND deleted_at IS NULL;

-- Miryang exposes a stable server-rendered table on the official city host.
UPDATE local_government_notice_sources
SET homepage_url = 'https://miryang.go.kr/',
    collection_endpoint_url = NULL,
    parser_profile_code = 'SPRING_BBS',
    request_method_code = 'GET',
    request_form_json = NULL,
    updated_at = now()
WHERE public_code = 'LGS-000229'
  AND deleted_at IS NULL;

-- Hamyang's official page embeds Saeol. The public action endpoint returns rows only through form POST.
UPDATE local_government_notice_sources
SET homepage_url = 'https://www.hygn.go.kr/',
    parser_profile_code = 'SAFE_SAEOL_EMINWON',
    request_method_code = 'POST_FORM',
    request_form_json = '{
      "pageIndex":"1",
      "jndinm":"OfrNotAncmtEJB",
      "context":"NTIS",
      "method":"selectListOfrNotAncmt",
      "methodnm":"selectListOfrNotAncmtHomepage",
      "not_ancmt_mgt_no":"",
      "homepage_pbs_yn":"Y",
      "subCheck":"Y",
      "not_ancmt_se_code":"01,02,03,04,07",
      "title":"고시공고",
      "cha_dep_code_nm":"",
      "initValue":"",
      "countYn":"Y",
      "list_gubun":"",
      "not_ancmt_sj":"",
      "ofr_pageSize":"10",
      "yyyy":""
    }'::jsonb,
    updated_at = now()
WHERE public_code = 'LGS-000239'
  AND deleted_at IS NULL;

UPDATE local_government_notice_sources
SET collection_endpoint_url = 'https://eminwon.hygn.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do',
    updated_at = now()
WHERE public_code = 'LGS-000239'
  AND deleted_at IS NULL;

UPDATE local_government_notice_sources
SET request_profile_code = 'BROWSER_HTTP1',
    updated_at = now()
WHERE public_code IN ('LGS-000059', 'LGS-000078', 'LGS-000229', 'LGS-000239')
  AND deleted_at IS NULL;

-- Sokcho's public Saeol endpoint is valid but intermittently exceeds the standard timeout.
UPDATE local_government_notice_sources
SET request_profile_code = 'LEGACY_BROWSER',
    updated_at = now()
WHERE public_code IN ('LGS-000122')
  AND deleted_at IS NULL;
