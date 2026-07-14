-- Apply isolated live-site QA results for legacy boards recovered with LEGACY_BROWSER.
-- Sources remain disabled until an operator explicitly approves collection.

WITH qa_pass (public_code, parser_profile_code) AS (
    VALUES
        ('LGS-000093', 'SAFE_PYEONGTAEK_BOARD_RENEWAL'),
        ('LGS-000148', 'SAFE_EGOV_BOARD_BUTTON'),
        ('LGS-000158', 'SAFE_EGOV_DATA_BUTTON')
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

-- Jungnang's board is structurally parseable but has no notice posted within the current one-year window.
UPDATE local_government_notice_sources
SET parser_profile_code = 'JUNGNANG_CONTEST_BOARD',
    validation_status_code = 'CHECK_REQUIRED',
    collection_status_code = 'CHECK_REQUIRED',
    is_enabled = false,
    last_error_code = 'STALE_SOURCE_CONTENT',
    last_error_message = '최근 1년 이내 등록 공고가 없어 운영 적합성을 다시 확인해야 합니다.',
    updated_at = now()
WHERE public_code = 'LGS-000008'
  AND deleted_at IS NULL;
