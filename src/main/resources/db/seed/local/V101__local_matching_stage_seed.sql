-- Align local matching smoke data with BASIC candidate and FINAL progress stages.

UPDATE matching_cases
SET
    matching_stage_code = 'FINAL',
    matching_basis_code = 'DOCUMENT_INPUT',
    updated_at = now(),
    updated_by = '10000000-0000-0000-0000-000000000002'
WHERE id = '60000000-0000-0000-0000-000000000003';

INSERT INTO matching_cases (
    id,
    announcement_id,
    member_user_id,
    verification_id,
    status_code,
    matching_stage_code,
    matching_basis_code,
    matched_at,
    reviewed_by,
    reviewed_at,
    created_by,
    updated_by
) VALUES (
    '60000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000003',
    null,
    'MATCHED',
    'BASIC',
    'BASIC_INFO',
    now(),
    '10000000-0000-0000-0000-000000000002',
    now(),
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002'
)
ON CONFLICT (announcement_id, member_user_id, matching_stage_code)
WHERE verification_id IS NULL
DO UPDATE
SET
    status_code = EXCLUDED.status_code,
    matching_basis_code = EXCLUDED.matching_basis_code,
    matched_at = EXCLUDED.matched_at,
    reviewed_by = EXCLUDED.reviewed_by,
    reviewed_at = EXCLUDED.reviewed_at,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;
