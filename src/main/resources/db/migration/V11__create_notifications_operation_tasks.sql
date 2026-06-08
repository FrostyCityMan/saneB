CREATE TABLE notification_templates (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    template_code varchar(80) NOT NULL,
    channel_code varchar(30) NOT NULL,
    title_template varchar(200) NOT NULL,
    body_template text NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT uq_notification_templates_code_channel UNIQUE (template_code, channel_code),
    CONSTRAINT fk_notification_templates_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_notification_templates_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_notification_templates_channel CHECK (channel_code IN ('IN_APP', 'EMAIL', 'SMS', 'KAKAO'))
);

CREATE INDEX ix_notification_templates_active ON notification_templates (is_active, template_code);

CREATE TABLE notification_messages (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id uuid NOT NULL,
    template_id uuid,
    channel_code varchar(30) NOT NULL,
    title varchar(200) NOT NULL,
    body text NOT NULL,
    status_code varchar(30) NOT NULL DEFAULT 'CREATED',
    resource_type varchar(100),
    resource_id uuid,
    read_at timestamptz,
    sent_at timestamptz,
    failure_code varchar(100),
    failure_message varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_notification_messages_recipient FOREIGN KEY (recipient_user_id) REFERENCES users (id),
    CONSTRAINT fk_notification_messages_template FOREIGN KEY (template_id) REFERENCES notification_templates (id),
    CONSTRAINT fk_notification_messages_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_notification_messages_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_notification_messages_channel CHECK (channel_code IN ('IN_APP', 'EMAIL', 'SMS', 'KAKAO')),
    CONSTRAINT ck_notification_messages_status CHECK (status_code IN ('CREATED', 'SENT', 'FAILED', 'CANCELED'))
);

CREATE INDEX ix_notification_messages_recipient_created_at ON notification_messages (recipient_user_id, created_at);
CREATE INDEX ix_notification_messages_recipient_read ON notification_messages (recipient_user_id, read_at);
CREATE INDEX ix_notification_messages_resource ON notification_messages (resource_type, resource_id);

CREATE TABLE notification_delivery_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id uuid NOT NULL,
    channel_code varchar(30) NOT NULL,
    provider_code varchar(30) NOT NULL,
    delivery_status_code varchar(30) NOT NULL,
    attempt_no integer NOT NULL DEFAULT 1,
    provider_message_key varchar(200),
    failure_code varchar(100),
    failure_message varchar(500),
    metadata_json jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_notification_delivery_logs_message FOREIGN KEY (message_id) REFERENCES notification_messages (id),
    CONSTRAINT ck_notification_delivery_logs_channel CHECK (channel_code IN ('IN_APP', 'EMAIL', 'SMS', 'KAKAO')),
    CONSTRAINT ck_notification_delivery_logs_provider CHECK (provider_code IN ('INTERNAL', 'EMAIL', 'SMS', 'KAKAO', 'MANUAL')),
    CONSTRAINT ck_notification_delivery_logs_status CHECK (delivery_status_code IN ('REQUESTED', 'SUCCESS', 'FAIL', 'SKIPPED')),
    CONSTRAINT ck_notification_delivery_logs_attempt CHECK (attempt_no > 0)
);

CREATE INDEX ix_notification_delivery_logs_message_created_at ON notification_delivery_logs (message_id, created_at);
CREATE INDEX ix_notification_delivery_logs_status_created_at ON notification_delivery_logs (delivery_status_code, created_at);

CREATE TABLE operation_tasks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_type_code varchar(50) NOT NULL,
    status_code varchar(30) NOT NULL DEFAULT 'OPEN',
    priority_code varchar(30) NOT NULL DEFAULT 'NORMAL',
    title varchar(200) NOT NULL,
    description text,
    resource_type varchar(100),
    resource_id uuid,
    due_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_operation_tasks_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_operation_tasks_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_operation_tasks_type CHECK (
        task_type_code IN ('DELAYED_PROGRESS', 'SUPPLEMENT_REQUEST', 'RECONTACT', 'PAYMENT_FAILED', 'CONSULTATION_PENDING', 'GENERAL')
    ),
    CONSTRAINT ck_operation_tasks_status CHECK (status_code IN ('OPEN', 'IN_PROGRESS', 'WAITING', 'DONE', 'CANCELED')),
    CONSTRAINT ck_operation_tasks_priority CHECK (priority_code IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'))
);

CREATE INDEX ix_operation_tasks_status_due_at ON operation_tasks (status_code, due_at);
CREATE INDEX ix_operation_tasks_resource ON operation_tasks (resource_type, resource_id);
CREATE INDEX ix_operation_tasks_type_status ON operation_tasks (task_type_code, status_code);

CREATE TABLE operation_task_comments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id uuid NOT NULL,
    author_user_id uuid NOT NULL,
    comment_text text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_operation_task_comments_task FOREIGN KEY (task_id) REFERENCES operation_tasks (id),
    CONSTRAINT fk_operation_task_comments_author FOREIGN KEY (author_user_id) REFERENCES users (id)
);

CREATE INDEX ix_operation_task_comments_task_created_at ON operation_task_comments (task_id, created_at);

CREATE TABLE operation_task_assignments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id uuid NOT NULL,
    assignee_user_id uuid NOT NULL,
    status_code varchar(30) NOT NULL DEFAULT 'ASSIGNED',
    assigned_by uuid NOT NULL,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    CONSTRAINT fk_operation_task_assignments_task FOREIGN KEY (task_id) REFERENCES operation_tasks (id),
    CONSTRAINT fk_operation_task_assignments_assignee FOREIGN KEY (assignee_user_id) REFERENCES users (id),
    CONSTRAINT fk_operation_task_assignments_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id),
    CONSTRAINT uq_operation_task_assignments_assignee UNIQUE (task_id, assignee_user_id),
    CONSTRAINT ck_operation_task_assignments_status CHECK (status_code IN ('ASSIGNED', 'DONE', 'CANCELED'))
);

CREATE INDEX ix_operation_task_assignments_assignee_status
    ON operation_task_assignments (assignee_user_id, status_code);
