-- Replace stale or unrelated seed URLs with currently published official notice lists.
-- Slow endpoints use the explicit legacy profile and remain disabled until isolated parser QA passes.

WITH reviewed_url (public_code, notice_url, request_profile_code) AS (
    VALUES
        (
            'LGS-000013',
            'https://eminwon.ep.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C02%2C04&pageIndex=1&subCheck=Y&yyyy=',
            'LEGACY_BROWSER'
        ),
        (
            'LGS-000119',
            'https://www.gn.go.kr/www/selectGosiNttList.do?key=263&pageIndex=1&pageUnit=10&searchCnd=all&searchGosiSe=01%2C04%2C06',
            'LEGACY_BROWSER'
        ),
        (
            'LGS-000180',
            'https://www.suncheon.go.kr/kr/news/0001/0001/?mode=list',
            'LEGACY_BROWSER'
        )
)
UPDATE local_government_notice_sources AS source
SET notice_url = reviewed_url.notice_url,
    request_profile_code = reviewed_url.request_profile_code,
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
SET request_profile_code = 'LEGACY_BROWSER',
    updated_at = now()
WHERE public_code IN ('LGS-000013', 'LGS-000119', 'LGS-000180')
  AND deleted_at IS NULL;
