CREATE TABLE announcement_input_requirements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    field_key varchar(80) NOT NULL,
    field_label varchar(200) NOT NULL,
    field_type_code varchar(30) NOT NULL,
    scope_code varchar(30) NOT NULL,
    is_required boolean NOT NULL DEFAULT false,
    is_sensitive boolean NOT NULL DEFAULT false,
    sort_order integer NOT NULL DEFAULT 0,
    help_text text,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcement_input_requirements_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT fk_announcement_input_requirements_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_announcement_input_requirements_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_input_requirements_field_key UNIQUE (announcement_id, field_key),
    CONSTRAINT ck_announcement_input_requirements_field_type CHECK (
        field_type_code IN (
            'TEXT', 'TEXTAREA', 'NUMBER', 'AMOUNT', 'DATE', 'BOOLEAN', 'SELECT', 'RADIO', 'MULTI_SELECT'
        )
    ),
    CONSTRAINT ck_announcement_input_requirements_scope CHECK (
        scope_code IN ('BUSINESS', 'PERSONAL', 'SPOUSE', 'CHILD', 'PARENT', 'APPLICATION', 'SUPPORT')
    ),
    CONSTRAINT ck_announcement_input_requirements_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_announcement_input_requirements_announcement_sort
    ON announcement_input_requirements (announcement_id, sort_order);
CREATE INDEX ix_announcement_input_requirements_scope_sort
    ON announcement_input_requirements (announcement_id, scope_code, sort_order);
CREATE INDEX ix_announcement_input_requirements_created_by
    ON announcement_input_requirements (created_by);
CREATE INDEX ix_announcement_input_requirements_updated_by
    ON announcement_input_requirements (updated_by);

CREATE TABLE announcement_input_options (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id uuid NOT NULL,
    option_code varchar(80) NOT NULL,
    option_label varchar(200) NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_announcement_input_options_requirement FOREIGN KEY (requirement_id) REFERENCES announcement_input_requirements (id),
    CONSTRAINT fk_announcement_input_options_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_announcement_input_options_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_input_options_code UNIQUE (requirement_id, option_code),
    CONSTRAINT ck_announcement_input_options_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_announcement_input_options_requirement_sort
    ON announcement_input_options (requirement_id, sort_order);
CREATE INDEX ix_announcement_input_options_created_by
    ON announcement_input_options (created_by);
CREATE INDEX ix_announcement_input_options_updated_by
    ON announcement_input_options (updated_by);

CREATE TABLE application_input_values (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    progress_id uuid NOT NULL,
    requirement_id uuid NOT NULL,
    value_text text,
    value_number numeric(18, 2),
    value_date date,
    value_boolean boolean,
    option_code varchar(80),
    submitted_by uuid NOT NULL,
    submitted_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_application_input_values_progress FOREIGN KEY (progress_id) REFERENCES application_progresses (id),
    CONSTRAINT fk_application_input_values_requirement FOREIGN KEY (requirement_id) REFERENCES announcement_input_requirements (id),
    CONSTRAINT fk_application_input_values_submitted_by FOREIGN KEY (submitted_by) REFERENCES users (id),
    CONSTRAINT fk_application_input_values_option FOREIGN KEY (requirement_id, option_code)
        REFERENCES announcement_input_options (requirement_id, option_code),
    CONSTRAINT ck_application_input_values_single_value CHECK (
        (
            option_code IS NOT NULL
            AND value_text IS NULL
            AND value_number IS NULL
            AND value_date IS NULL
            AND value_boolean IS NULL
        )
        OR (
            option_code IS NULL
            AND (
                (value_text IS NOT NULL)::integer
                + (value_number IS NOT NULL)::integer
                + (value_date IS NOT NULL)::integer
                + (value_boolean IS NOT NULL)::integer
            ) = 1
        )
    )
);

CREATE UNIQUE INDEX uq_application_input_values_single_value
    ON application_input_values (progress_id, requirement_id)
    WHERE option_code IS NULL;
CREATE UNIQUE INDEX uq_application_input_values_option_value
    ON application_input_values (progress_id, requirement_id, option_code)
    WHERE option_code IS NOT NULL;
CREATE INDEX ix_application_input_values_progress_submitted_at
    ON application_input_values (progress_id, submitted_at);
CREATE INDEX ix_application_input_values_requirement
    ON application_input_values (requirement_id);
CREATE INDEX ix_application_input_values_submitted_by_submitted_at
    ON application_input_values (submitted_by, submitted_at);
