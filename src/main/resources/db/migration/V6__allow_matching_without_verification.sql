ALTER TABLE matching_cases
    ALTER COLUMN verification_id DROP NOT NULL;

CREATE UNIQUE INDEX uq_matching_cases_without_verification
    ON matching_cases (announcement_id, member_user_id)
    WHERE verification_id IS NULL;
