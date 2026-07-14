-- Apply isolated live-site QA after switching Gangdong to the slow legacy transport.

UPDATE local_government_notice_sources
SET parser_profile_code = 'SAFE_SAEOL_EMINWON_HREF',
    validation_status_code = 'VERIFIED',
    collection_status_code = 'READY',
    is_enabled = false,
    last_error_code = NULL,
    last_error_message = NULL,
    updated_at = now()
WHERE public_code = 'LGS-000026'
  AND deleted_at IS NULL;
