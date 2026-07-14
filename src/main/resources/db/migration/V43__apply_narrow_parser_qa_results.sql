-- Apply only parser assignments confirmed by live title, posted-date and safe detail-URL QA.
-- Sources remain disabled until an operator explicitly enables them in the deployment environment.

WITH qa_pass(public_code, parser_profile_code) AS (
    VALUES
        ('LGS-000029', 'RFC_BLOGLIST_NOTICE'),
        ('LGS-000045', 'SAFE_SAEOL_EMINWON_LEGACY'),
        ('LGS-000074', 'SAFE_EGOV_DETAIL_BUTTON'),
        ('LGS-000130', 'SAFE_SAEOL_EMINWON_LEGACY'),
        ('LGS-000141', 'SAEOL_GOSI'),
        ('LGS-000161', 'SAFE_EGOV_DETAIL_BUTTON'),
        ('LGS-000185', 'GURYE_BOARD_NOTICE'),
        ('LGS-000230', 'SAEOL_GOSI')
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
