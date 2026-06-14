-- Add stage markers for the basic-info candidate flow and final admin-confirmed flow.
-- Existing v1 endpoints remain valid; the new columns separate broad user candidates from final progress candidates.

ALTER TABLE matching_cases
    ADD COLUMN matching_stage_code varchar(30) NOT NULL DEFAULT 'BASIC',
    ADD COLUMN matching_basis_code varchar(50) NOT NULL DEFAULT 'BASIC_INFO',
    ADD CONSTRAINT ck_matching_cases_stage CHECK (
        matching_stage_code IN ('BASIC', 'FINAL')
    ),
    ADD CONSTRAINT ck_matching_cases_basis CHECK (
        matching_basis_code IN ('BASIC_INFO', 'PARTNER_INPUT', 'DOCUMENT_INPUT')
    );

DROP INDEX IF EXISTS uq_matching_cases_without_verification;
DROP INDEX IF EXISTS uq_matching_cases_no_verification;

CREATE UNIQUE INDEX uq_matching_cases_stage_no_verification
    ON matching_cases (announcement_id, member_user_id, matching_stage_code)
    WHERE verification_id IS NULL;

CREATE INDEX ix_matching_cases_stage_status
    ON matching_cases (matching_stage_code, status_code, matched_at DESC);

CREATE INDEX ix_matching_cases_member_stage_status
    ON matching_cases (member_user_id, matching_stage_code, status_code);
