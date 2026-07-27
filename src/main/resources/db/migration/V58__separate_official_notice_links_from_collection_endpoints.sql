-- Keep user-facing links on stable official portal hosts while preserving verified collection endpoints.
WITH reviewed_url(public_code, notice_url, homepage_url, verification_note) AS (
    VALUES
        (
            'LGS-000229',
            'https://www.miryang.go.kr/web/eMiryangMinwonList.do?mnNo=20903000000&nmmKind1=01&nmmKind2=02&nmmKind3=03&nmmKind4=04&nmmKind5=05&nmmKind6=06&nmmKind7=07&owd=&pageIndex=1&searchCondition=&searchKeyword=',
            'https://www.miryang.go.kr/',
            '2026-07-27 밀양시 공식 www 고시·공고·채용 목록과 최근 게시물 표본 확인, 운영 DNS 검증용 공식 호스트 확정'
        ),
        (
            'LGS-000239',
            'https://www.hygn.go.kr/00429/00543/00549.web',
            'https://www.hygn.go.kr/',
            '2026-07-27 함양군 대표누리집 공식 고시·공고 화면과 외부 새올 수집 endpoint 연결 확인'
        )
)
UPDATE local_government_notice_sources AS source
SET notice_url = reviewed_url.notice_url,
    homepage_url = reviewed_url.homepage_url,
    semantic_verification_note = reviewed_url.verification_note,
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

-- Hamyang's official portal embeds this public Saeol endpoint; collection remains server-side and review-only.
UPDATE local_government_notice_sources
SET collection_endpoint_url = 'https://eminwon.hygn.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do',
    updated_at = now()
WHERE public_code = 'LGS-000239'
  AND deleted_at IS NULL;
