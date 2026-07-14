-- Apply only safe-link parser profiles that passed list extraction and representative detail-link QA.

WITH qa_pass(public_code, parser_profile_code) AS (
    VALUES
        ('LGS-000016', 'SAFE_YANGCHEON_SEOL'),
        ('LGS-000037', 'SAFE_BOARD_VIEW'),
        ('LGS-000039', 'SAFE_YH_BOARD_POST'),
        ('LGS-000044', 'SAFE_ICMS_BOARD'),
        ('LGS-000046', 'SAFE_YH_BOARD_POST'),
        ('LGS-000048', 'SAFE_ICMS_BOARD'),
        ('LGS-000049', 'SAFE_ICMS_BOARD_EXTENDED'),
        ('LGS-000050', 'SAFE_ICMS_BOARD'),
        ('LGS-000051', 'SAFE_ICMS_BOARD'),
        ('LGS-000082', 'SAFE_GOTO_VIEW'),
        ('LGS-000085', 'SAFE_OPENWORKS_BOARD'),
        ('LGS-000087', 'SAFE_BD_SELECT_BBS'),
        ('LGS-000092', 'SAFE_ANSAN_BBS'),
        ('LGS-000095', 'SAFE_GOTO_VIEW'),
        ('LGS-000098', 'SAFE_BOARD_VIEW_SITE'),
        ('LGS-000099', 'SAFE_GOTO_VIEW'),
        ('LGS-000105', 'SAFE_YH_BOARD_POST'),
        ('LGS-000106', 'SAFE_GOTO_VIEW'),
        ('LGS-000113', 'SAFE_GOTO_VIEW'),
        ('LGS-000116', 'SAFE_GWD_BULLETIN'),
        ('LGS-000203', 'SAFE_GOTO_VIEW_EXTENDED'),
        ('LGS-000205', 'SAFE_YH_BOARD_POST'),
        ('LGS-000208', 'SAFE_SANGJU_GOSI'),
        ('LGS-000209', 'SAFE_BOARD_VIEW_SITE'),
        ('LGS-000215', 'SAFE_YH_BOARD_POST'),
        ('LGS-000216', 'SAFE_GORYEONG_BOARD'),
        ('LGS-000218', 'SAFE_GOTO_VIEW'),
        ('LGS-000220', 'SAFE_YH_BOARD_POST'),
        ('LGS-000231', 'SAFE_YH_BOARD_POST')
)
UPDATE local_government_notice_sources source
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
