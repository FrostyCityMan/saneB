-- Support the official form-POST flow used by a small number of local-government notice boards.
-- Form values are public board query values only. Secrets and user data are not stored here.

ALTER TABLE local_government_notice_sources
    ADD COLUMN request_method_code varchar(20) NOT NULL DEFAULT 'GET',
    ADD COLUMN request_form_json jsonb;

ALTER TABLE local_government_notice_sources
    ADD CONSTRAINT ck_local_government_notice_sources_request_method CHECK (
        (request_method_code = 'GET' AND request_form_json IS NULL)
        OR (
            request_method_code = 'POST_FORM'
            AND request_form_json IS NOT NULL
            AND jsonb_typeof(request_form_json) = 'object'
        )
    );

WITH reviewed_url(public_code, notice_url) AS (
    VALUES
        ('LGS-000014', 'https://www.sdm.go.kr/news/notice/notice.do'),
        ('LGS-000089', 'https://eminwon.seongnam.go.kr/emwp/jsp/ofr/OfrNotAncmtLSub.jsp?not_ancmt_se_code=01%2C02%2C03%2C04%2C05%2C06%2C07')
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

UPDATE local_government_notice_sources
SET collection_endpoint_url = 'https://eminwon.seongnam.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do',
    request_method_code = 'POST_FORM',
    request_form_json = '{
      "pageIndex":"",
      "jndinm":"OfrNotAncmtEJB",
      "context":"NTIS",
      "method":"selectListOfrNotAncmt",
      "methodnm":"selectListOfrNotAncmtHomepage",
      "not_ancmt_mgt_no":"",
      "homepage_pbs_yn":"Y",
      "subCheck":"Y",
      "not_ancmt_se_code":"01,02,03,04,05,06,07",
      "title":"고시공고",
      "cha_dep_code_nm":"",
      "initValue":"",
      "countYn":"Y",
      "list_gubun":"",
      "not_ancmt_sj":"",
      "cgg_code":"",
      "not_ancmt_cn":"",
      "dept_nm":"",
      "epcCheck":"Y",
      "yyyy":"",
      "nodate_recent_mm":"",
      "ofr_pageSize":"10",
      "Key":"B_Subject",
      "temp":""
    }'::jsonb,
    updated_at = now()
WHERE public_code = 'LGS-000089'
  AND deleted_at IS NULL;

-- These sources passed the same live QA harness for title, posted date and safe detail URL.
-- Keep every source OFF until an operator explicitly enables it in the deployment environment.
WITH qa_pass(public_code, parser_profile_code) AS (
    VALUES
        ('LGS-000014', 'SAFE_SEODAEMUN_NOTICE'),
        ('LGS-000089', 'SAFE_SAEOL_EMINWON'),
        ('LGS-000120', 'SAFE_SAEOL_EMINWON_CELL'),
        ('LGS-000145', 'SAFE_SAEOL_EMINWON_CELL'),
        ('LGS-000242', 'SPRING_BBS')
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
