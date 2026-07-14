-- Gangdong intermittently exceeds the standard HTTP/1.1 response timeout.
-- Use the explicit slow legacy transport while retaining the previously verified parser.

UPDATE local_government_notice_sources
SET request_profile_code = 'LEGACY_BROWSER',
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
WHERE public_code IN ('LGS-000026')
  AND deleted_at IS NULL;
