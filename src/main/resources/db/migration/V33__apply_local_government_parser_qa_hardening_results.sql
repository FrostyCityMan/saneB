-- Apply the additional parser passes verified by the 2026-07-13 full 244-source QA.
-- The migration assigns parsers only; operators must enable each source separately.

WITH qa_pass (public_code, parser_profile_code) AS (
    VALUES
    ('LGS-000021', 'SPRING_BBS'),
    ('LGS-000032', 'SPRING_BBS'),
    ('LGS-000033', 'SPRING_BBS'),
    ('LGS-000035', 'SPRING_BBS'),
    ('LGS-000040', 'SPRING_BBS'),
    ('LGS-000042', 'HEURISTIC_NOTICE'),
    ('LGS-000043', 'HEURISTIC_NOTICE'),
    ('LGS-000066', 'HEURISTIC_NOTICE'),
    ('LGS-000069', 'HEURISTIC_NOTICE'),
    ('LGS-000078', 'SPRING_BBS'),
    ('LGS-000104', 'SAEOL_GOSI'),
    ('LGS-000117', 'CHUNCHEON_NOTICE_JSON'),
    ('LGS-000175', 'SPRING_BBS'),
    ('LGS-000177', 'HEURISTIC_NOTICE'),
    ('LGS-000200', 'SAEOL_GOSI'),
    ('LGS-000201', 'SAEOL_GOSI'),
    ('LGS-000207', 'SPRING_BBS'),
    ('LGS-000223', 'SPRING_BBS'),
    ('LGS-000239', 'HEURISTIC_NOTICE')
)
UPDATE local_government_notice_sources AS source
SET parser_profile_code = qa_pass.parser_profile_code,
    validation_status_code = 'VERIFIED',
    collection_status_code = CASE WHEN source.is_enabled THEN source.collection_status_code ELSE 'READY' END,
    last_error_code = NULL,
    last_error_message = NULL,
    updated_at = now()
FROM qa_pass
WHERE source.public_code = qa_pass.public_code
  AND source.deleted_at IS NULL;
