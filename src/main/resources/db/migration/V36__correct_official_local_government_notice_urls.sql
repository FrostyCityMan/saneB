-- Replace stale navigation pages with current official local-government notice list URLs.
-- Every corrected source stays disabled until live parser QA verifies list and detail links.

WITH reviewed_url (public_code, notice_url) AS (
    VALUES
        ('LGS-000001', 'https://www.seoul.go.kr/news/news_notice.do?bbsId=001&bbsNo=277'),
        ('LGS-000003', 'https://www.junggu.seoul.kr/content.do?cmsid=14232&mode=list'),
        ('LGS-000006', 'https://www.gwangjin.go.kr/portal/bbs/B0000003/list.do?menuNo=200192'),
        ('LGS-000007', 'https://www.ddm.go.kr/www/selectEminwonWebList.do?key=3291&searchNotAncmtSeCode=01%2C02%2C04%2C05%2C06%2C07'),
        ('LGS-000009', 'https://www.sb.go.kr/www/selectEminwonList.do?bbsNo=41&key=6920'),
        ('LGS-000014', 'https://www.sdm.go.kr/news/notice.do'),
        ('LGS-000017', 'https://www.gangseo.seoul.kr/gs040301'),
        ('LGS-000020', 'https://www.ydp.go.kr/www/selectEminwonList.do?key=2851&menuFlag=01'),
        ('LGS-000022', 'https://www.gwanak.go.kr/site/gwanak/ex/bbsNew/List.do?typeCode=1'),
        ('LGS-000023', 'https://eminwon.seocho.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C02%2C04&pageIndex=1&subCheck=Y&yyyy='),
        ('LGS-000026', 'https://eminwon.gangdong.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C02%2C04&pageIndex=1&subCheck=Y&yyyy='),
        ('LGS-000053', 'https://www.gunwi.go.kr/ko/page.do?mnu_uid=666&boardType=notice'),
        ('LGS-000133', 'https://eminwon.gwgs.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectListOfrNotAncmt&methodnm=selectListOfrNotAncmtHomepage&not_ancmt_se_code=01%2C04&pageIndex=1&subCheck=Y&yyyy='),
        ('LGS-000141', 'https://www.yd21.go.kr/kr/html/sub02/020103.html')
)
UPDATE local_government_notice_sources AS source
SET notice_url = reviewed_url.notice_url,
    collection_endpoint_url = NULL,
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

-- Older Saeol e-minwon endpoints are tested through the constrained browser-compatible HTTP/1.1 profile.
UPDATE local_government_notice_sources
SET request_profile_code = 'BROWSER_HTTP1',
    updated_at = now()
WHERE public_code IN (
    'LGS-000001', 'LGS-000003', 'LGS-000014', 'LGS-000023',
    'LGS-000026', 'LGS-000053', 'LGS-000133', 'LGS-000141'
)
  AND deleted_at IS NULL;
