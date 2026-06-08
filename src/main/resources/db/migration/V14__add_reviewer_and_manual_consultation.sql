-- Add read-only reviewer role and manual consultation assignment support.
ALTER TABLE roles
    DROP CONSTRAINT ck_roles_role_code;

ALTER TABLE roles
    ADD CONSTRAINT ck_roles_role_code
        CHECK (role_code IN ('USER', 'PARTNER', 'OPERATOR', 'APPROVER', 'REVIEWER', 'ADMIN'));

INSERT INTO roles (
    role_code,
    role_name,
    sort_order
) VALUES (
    'REVIEWER',
    '검수자',
    45
)
ON CONFLICT (role_code) DO UPDATE
SET
    role_name = EXCLUDED.role_name,
    sort_order = EXCLUDED.sort_order;

ALTER TABLE consultation_reservations
    ALTER COLUMN slot_id DROP NOT NULL,
    ALTER COLUMN partner_user_id DROP NOT NULL;

ALTER TABLE consultation_reservations
    DROP CONSTRAINT ck_consultation_reservations_status;

ALTER TABLE consultation_reservations
    ADD CONSTRAINT ck_consultation_reservations_status CHECK (
        status_code IN ('REQUESTED', 'ASSIGNED', 'CONFIRMED', 'CANCELED', 'COMPLETED', 'NO_SHOW')
    );

ALTER TABLE consultation_histories
    DROP CONSTRAINT ck_consultation_histories_before_status;

ALTER TABLE consultation_histories
    ADD CONSTRAINT ck_consultation_histories_before_status CHECK (
        before_status_code IS NULL
        OR before_status_code IN ('REQUESTED', 'ASSIGNED', 'CONFIRMED', 'CANCELED', 'COMPLETED', 'NO_SHOW')
    );

ALTER TABLE consultation_histories
    DROP CONSTRAINT ck_consultation_histories_after_status;

ALTER TABLE consultation_histories
    ADD CONSTRAINT ck_consultation_histories_after_status CHECK (
        after_status_code IN ('REQUESTED', 'ASSIGNED', 'CONFIRMED', 'CANCELED', 'COMPLETED', 'NO_SHOW')
    );

DELETE FROM progress_reminder_logs prl
USING (
    SELECT
        ranked.id
    FROM (
        SELECT
            kept.id,
            row_number() OVER (
                PARTITION BY kept.progress_id, kept.reminder_type_code
                ORDER BY kept.created_at ASC, kept.id::text ASC
            ) AS duplicate_rank
        FROM progress_reminder_logs kept
    ) ranked
    WHERE ranked.duplicate_rank > 1
) duplicate_rows
WHERE prl.id = duplicate_rows.id;

CREATE UNIQUE INDEX uq_progress_reminder_logs_progress_type
    ON progress_reminder_logs (progress_id, reminder_type_code);

CREATE INDEX ix_operation_tasks_open_resource_type
    ON operation_tasks (task_type_code, resource_type, resource_id)
    WHERE status_code IN ('OPEN', 'IN_PROGRESS', 'WAITING')
      AND resource_id IS NOT NULL;
