-- Apply the parser profile confirmed against the live Gongju notice list.
-- Keep the source disabled until an operator explicitly enables it.

UPDATE local_government_notice_sources
SET parser_profile_code = 'SAFE_EGOV_DETAIL_CELL',
    validation_status_code = 'VERIFIED',
    collection_status_code = 'READY',
    is_enabled = false,
    last_http_status = NULL,
    last_error_code = NULL,
    last_error_message = NULL,
    updated_at = now()
WHERE public_code = 'LGS-000149'
  AND deleted_at IS NULL;
