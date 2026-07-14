-- Recover reviewed legacy public boards that reject Java HttpClient framing or request headers.
-- The profile is assigned only after an isolated URLConnection request reproduced HTTP 200.

ALTER TABLE local_government_notice_sources
    DROP CONSTRAINT ck_local_government_notice_sources_request_profile;

ALTER TABLE local_government_notice_sources
    ADD CONSTRAINT ck_local_government_notice_sources_request_profile CHECK (
        request_profile_code IN ('DEFAULT', 'BROWSER_HTTP1', 'LEGACY_BROWSER')
    );

UPDATE local_government_notice_sources
SET request_profile_code = 'LEGACY_BROWSER',
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
WHERE public_code IN (
    'LGS-000008',
    'LGS-000093',
    'LGS-000148',
    'LGS-000158'
)
  AND deleted_at IS NULL;
