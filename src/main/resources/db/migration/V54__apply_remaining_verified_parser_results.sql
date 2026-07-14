-- Promote the final three sources whose live parser QA passes but whose seed state remained MANUAL_ONLY.
-- Sources remain disabled until an operator explicitly approves collection.

WITH qa_pass (public_code, parser_profile_code) AS (
    VALUES
        ('LGS-000034', 'SAFE_SAEOL_EMINWON_LEGACY'),
        ('LGS-000108', 'SAEOL_GOSI'),
        ('LGS-000135', 'SAEOL_GOSI')
)
UPDATE local_government_notice_sources AS source
SET parser_profile_code = qa_pass.parser_profile_code,
    validation_status_code = 'VERIFIED',
    collection_status_code = 'READY',
    is_enabled = false,
    last_error_code = NULL,
    last_error_message = NULL,
    updated_at = now()
FROM qa_pass
WHERE source.public_code = qa_pass.public_code
  AND source.deleted_at IS NULL;
