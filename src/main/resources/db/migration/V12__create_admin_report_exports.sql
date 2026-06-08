CREATE TABLE report_exports (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    report_type_code varchar(50) NOT NULL,
    format_code varchar(30) NOT NULL,
    status_code varchar(30) NOT NULL DEFAULT 'REQUESTED',
    requested_by uuid NOT NULL,
    row_count integer NOT NULL DEFAULT 0,
    file_name varchar(255),
    content_text text,
    requested_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    failure_code varchar(100),
    failure_message varchar(500),
    CONSTRAINT fk_report_exports_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT ck_report_exports_type CHECK (report_type_code IN ('OPERATION_SUMMARY')),
    CONSTRAINT ck_report_exports_format CHECK (format_code IN ('CSV', 'EXCEL')),
    CONSTRAINT ck_report_exports_status CHECK (status_code IN ('REQUESTED', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_report_exports_row_count CHECK (row_count >= 0)
);

CREATE INDEX ix_report_exports_requested_at ON report_exports (requested_at);
CREATE INDEX ix_report_exports_requested_by_status ON report_exports (requested_by, status_code);

CREATE TABLE admin_report_snapshots (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_type_code varchar(50) NOT NULL,
    snapshot_json jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    CONSTRAINT fk_admin_report_snapshots_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_admin_report_snapshots_type CHECK (snapshot_type_code IN ('OPERATION_SUMMARY'))
);

CREATE INDEX ix_admin_report_snapshots_type_created_at
    ON admin_report_snapshots (snapshot_type_code, created_at);
