-- Add human-readable public codes for user-facing resources.
-- Internal UUID primary keys remain the source of truth for joins and authorization.

ALTER TABLE users ADD COLUMN public_code varchar(32);
ALTER TABLE announcements ADD COLUMN public_code varchar(32);
ALTER TABLE matching_cases ADD COLUMN public_code varchar(32);
ALTER TABLE application_progresses ADD COLUMN public_code varchar(32);
ALTER TABLE partner_verifications ADD COLUMN public_code varchar(32);
ALTER TABLE consultation_reservations ADD COLUMN public_code varchar(32);

WITH numbered AS (
    SELECT
        id,
        row_number() OVER (ORDER BY created_at ASC, id ASC) AS seq
    FROM users
)
UPDATE users target
SET public_code = 'USR-' || lpad(numbered.seq::text, 6, '0')
FROM numbered
WHERE target.id = numbered.id;

WITH numbered AS (
    SELECT
        id,
        row_number() OVER (ORDER BY created_at ASC, id ASC) AS seq
    FROM announcements
)
UPDATE announcements target
SET public_code = 'ANN-' || lpad(numbered.seq::text, 6, '0')
FROM numbered
WHERE target.id = numbered.id;

WITH numbered AS (
    SELECT
        id,
        row_number() OVER (ORDER BY created_at ASC, id ASC) AS seq
    FROM matching_cases
)
UPDATE matching_cases target
SET public_code = 'MCH-' || lpad(numbered.seq::text, 6, '0')
FROM numbered
WHERE target.id = numbered.id;

WITH numbered AS (
    SELECT
        id,
        row_number() OVER (ORDER BY created_at ASC, id ASC) AS seq
    FROM application_progresses
)
UPDATE application_progresses target
SET public_code = 'APP-' || lpad(numbered.seq::text, 6, '0')
FROM numbered
WHERE target.id = numbered.id;

WITH numbered AS (
    SELECT
        id,
        row_number() OVER (ORDER BY created_at ASC, id ASC) AS seq
    FROM partner_verifications
)
UPDATE partner_verifications target
SET public_code = 'VRF-' || lpad(numbered.seq::text, 6, '0')
FROM numbered
WHERE target.id = numbered.id;

WITH numbered AS (
    SELECT
        id,
        row_number() OVER (ORDER BY created_at ASC, id ASC) AS seq
    FROM consultation_reservations
)
UPDATE consultation_reservations target
SET public_code = 'CNS-' || lpad(numbered.seq::text, 6, '0')
FROM numbered
WHERE target.id = numbered.id;

CREATE SEQUENCE users_public_code_seq;
CREATE SEQUENCE announcements_public_code_seq;
CREATE SEQUENCE matching_cases_public_code_seq;
CREATE SEQUENCE application_progresses_public_code_seq;
CREATE SEQUENCE partner_verifications_public_code_seq;
CREATE SEQUENCE consultation_reservations_public_code_seq;

SELECT setval(
    'users_public_code_seq',
    COALESCE((SELECT max(substring(public_code from '[0-9]+$')::bigint) FROM users), 0) + 1,
    false
);
SELECT setval(
    'announcements_public_code_seq',
    COALESCE((SELECT max(substring(public_code from '[0-9]+$')::bigint) FROM announcements), 0) + 1,
    false
);
SELECT setval(
    'matching_cases_public_code_seq',
    COALESCE((SELECT max(substring(public_code from '[0-9]+$')::bigint) FROM matching_cases), 0) + 1,
    false
);
SELECT setval(
    'application_progresses_public_code_seq',
    COALESCE((SELECT max(substring(public_code from '[0-9]+$')::bigint) FROM application_progresses), 0) + 1,
    false
);
SELECT setval(
    'partner_verifications_public_code_seq',
    COALESCE((SELECT max(substring(public_code from '[0-9]+$')::bigint) FROM partner_verifications), 0) + 1,
    false
);
SELECT setval(
    'consultation_reservations_public_code_seq',
    COALESCE((SELECT max(substring(public_code from '[0-9]+$')::bigint) FROM consultation_reservations), 0) + 1,
    false
);

ALTER TABLE users
    ALTER COLUMN public_code SET DEFAULT ('USR-' || lpad(nextval('users_public_code_seq')::text, 6, '0')),
    ALTER COLUMN public_code SET NOT NULL;
ALTER TABLE announcements
    ALTER COLUMN public_code SET DEFAULT ('ANN-' || lpad(nextval('announcements_public_code_seq')::text, 6, '0')),
    ALTER COLUMN public_code SET NOT NULL;
ALTER TABLE matching_cases
    ALTER COLUMN public_code SET DEFAULT ('MCH-' || lpad(nextval('matching_cases_public_code_seq')::text, 6, '0')),
    ALTER COLUMN public_code SET NOT NULL;
ALTER TABLE application_progresses
    ALTER COLUMN public_code SET DEFAULT ('APP-' || lpad(nextval('application_progresses_public_code_seq')::text, 6, '0')),
    ALTER COLUMN public_code SET NOT NULL;
ALTER TABLE partner_verifications
    ALTER COLUMN public_code SET DEFAULT ('VRF-' || lpad(nextval('partner_verifications_public_code_seq')::text, 6, '0')),
    ALTER COLUMN public_code SET NOT NULL;
ALTER TABLE consultation_reservations
    ALTER COLUMN public_code SET DEFAULT ('CNS-' || lpad(nextval('consultation_reservations_public_code_seq')::text, 6, '0')),
    ALTER COLUMN public_code SET NOT NULL;

ALTER TABLE users ADD CONSTRAINT uq_users_public_code UNIQUE (public_code);
ALTER TABLE announcements ADD CONSTRAINT uq_announcements_public_code UNIQUE (public_code);
ALTER TABLE matching_cases ADD CONSTRAINT uq_matching_cases_public_code UNIQUE (public_code);
ALTER TABLE application_progresses ADD CONSTRAINT uq_application_progresses_public_code UNIQUE (public_code);
ALTER TABLE partner_verifications ADD CONSTRAINT uq_partner_verifications_public_code UNIQUE (public_code);
ALTER TABLE consultation_reservations ADD CONSTRAINT uq_consultation_reservations_public_code UNIQUE (public_code);
