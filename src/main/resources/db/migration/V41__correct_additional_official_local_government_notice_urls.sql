-- Correct additional official notice-list URLs confirmed against the live institution sites.
-- Keep every source disabled until the parser QA confirms title, posted date and detail URL extraction.

WITH reviewed_url(public_code, notice_url) AS (
    VALUES
        ('LGS-000045', 'https://eminwon.jung.daegu.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=Y&yyyy='),
        ('LGS-000130', 'https://eminwon.ihc.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=N&yyyy='),
        ('LGS-000141', 'https://www.yd21.go.kr/kr/html/sub02/020103.html?GotoPage=1&mode=L'),
        ('LGS-000230', 'https://www.geoje.go.kr/index.geoje?menuCd=DOM_000008902001002001')
)
UPDATE local_government_notice_sources AS source
SET notice_url = reviewed_url.notice_url,
    collection_endpoint_url = NULL,
    parser_profile_code = 'MANUAL_ONLY',
    request_profile_code = 'BROWSER_HTTP1',
    request_method_code = 'GET',
    request_form_json = NULL,
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
