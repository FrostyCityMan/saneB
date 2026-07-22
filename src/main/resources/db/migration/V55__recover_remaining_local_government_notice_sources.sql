-- Recover the remaining local-government sources with current official notice pages and collection endpoints.
-- User-facing links stay on official HTTPS pages; public notice collection endpoints contain no secrets or user data.
-- Every source remains disabled until an operator explicitly approves collection.

WITH reviewed_source (public_code, notice_url, parser_profile_code) AS (
    VALUES
        (
            'LGS-000008',
            'https://www.jungnang.go.kr/portal/bbs/list/B0000117.do?menuNo=200475',
            'SPRING_BBS'
        ),
        (
            'LGS-000019',
            'https://www.geumcheon.go.kr/portal/tblSeolGosiDetailList.do?key=294&rep=1',
            'SAEOL_GOSI'
        ),
        (
            'LGS-000036',
            'https://www.haeundae.go.kr/board/list.do?boardId=BBS_0000038&menuCd=DOM_000000104001001000&contentsSid=100',
            'HEURISTIC_NOTICE'
        ),
        (
            'LGS-000089',
            'https://www.seongnam.go.kr/notice/publicNotice.do?menuIdx=1000499&returnURL=/main.do',
            'SAFE_SAEOL_EMINWON'
        ),
        (
            'LGS-000094',
            'https://www.anyang.go.kr/main/emwsWebList.do?key=4101&searchGosiSe=01%2C03%2C04',
            'SAFE_SAEOL_EMINWON_CELL'
        ),
        (
            'LGS-000122',
            'https://www.sokcho.go.kr/sc/portal/sokchonews/notification',
            'SAFE_SAEOL_EMINWON'
        ),
        (
            'LGS-000167',
            'https://www.jeongeup.go.kr/board/list.jeongeup?boardId=BBS_0000012&menuCd=DOM_000000101001001000&contentsSid=5&cpath=',
            'SPRING_BBS'
        ),
        (
            'LGS-000174',
            'https://www.imsil.go.kr/board/list.imsil?boardId=BBS_0000002&menuCd=DOM_000000103001001000&contentsSid=161&cpath=',
            'SUBJECT_NOTICE_TABLE'
        ),
        (
            'LGS-000224',
            'https://www.changwon.go.kr/cwportal/10310/10438/10439.web?section=gosi',
            'SPRING_BBS'
        )
)
UPDATE local_government_notice_sources AS source
SET notice_url = reviewed_source.notice_url,
    collection_endpoint_url = NULL,
    parser_profile_code = reviewed_source.parser_profile_code,
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
FROM reviewed_source
WHERE source.public_code = reviewed_source.public_code
  AND source.deleted_at IS NULL;

-- Seongnam's official page embeds the public Saeol board and requires the reviewed form POST.
UPDATE local_government_notice_sources
SET collection_endpoint_url = 'http://eminwon.seongnam.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do',
    request_profile_code = 'BROWSER_HTTP1',
    request_method_code = 'POST_FORM',
    updated_at = now()
WHERE public_code = 'LGS-000089'
  AND request_form_json IS NOT NULL
  AND deleted_at IS NULL;

-- Anyang exposes the same official rows through a public HTTP Saeol collection endpoint.
UPDATE local_government_notice_sources
SET collection_endpoint_url = 'http://eminwon.anyang.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C03%2C04&pageIndex=1&subCheck=Y&yyyy=',
    request_profile_code = 'BROWSER_HTTP1',
    request_method_code = 'GET',
    request_form_json = NULL,
    updated_at = now()
WHERE public_code = 'LGS-000094'
  AND deleted_at IS NULL;

-- Sokcho keeps the official HTTPS page for operators and uses its public HTTP Saeol endpoint for collection.
UPDATE local_government_notice_sources
SET collection_endpoint_url = 'http://eminwon.sokcho.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=Y&yyyy=',
    request_profile_code = 'BROWSER_HTTP1',
    request_method_code = 'GET',
    request_form_json = NULL,
    updated_at = now()
WHERE public_code = 'LGS-000122'
  AND deleted_at IS NULL;

-- Busan Nam-gu's HTTPS Saeol endpoint is intermittently slow; collect the same public rows over HTTP.
UPDATE local_government_notice_sources
SET collection_endpoint_url = 'http://eminwon.bsnamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=Y&yyyy=',
    request_method_code = 'GET',
    request_form_json = NULL,
    updated_at = now()
WHERE public_code = 'LGS-000034'
  AND deleted_at IS NULL;

-- Gangdong keeps its verified slow-site transport and collects the same public rows over HTTP.
UPDATE local_government_notice_sources
SET collection_endpoint_url = 'http://eminwon.gangdong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C02%2C04&pageIndex=1&subCheck=Y&yyyy=',
    request_method_code = 'GET',
    request_form_json = NULL,
    updated_at = now()
WHERE public_code = 'LGS-000026'
  AND deleted_at IS NULL;

UPDATE local_government_notice_sources
SET request_profile_code = 'LEGACY_BROWSER',
    updated_at = now()
WHERE public_code IN ('LGS-000034')
  AND deleted_at IS NULL;

-- Haeundae, Jeongeup, and Imsil now require HTTPS and browser-compatible headers.
UPDATE local_government_notice_sources
SET request_profile_code = 'BROWSER_HTTP1',
    request_method_code = 'GET',
    request_form_json = NULL,
    updated_at = now()
WHERE public_code IN ('LGS-000036', 'LGS-000167', 'LGS-000174')
  AND deleted_at IS NULL;
