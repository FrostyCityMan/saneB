CREATE TABLE ai_assist_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    assist_type_code varchar(50) NOT NULL,
    resource_type varchar(50) NOT NULL DEFAULT 'GENERAL',
    resource_id uuid,
    input_hash_sha256 varchar(64) NOT NULL,
    input_length integer NOT NULL DEFAULT 0,
    requested_by uuid NOT NULL,
    status_code varchar(30) NOT NULL DEFAULT 'REQUESTED',
    provider_code varchar(50) NOT NULL DEFAULT 'LOCAL_SAFE',
    model_code varchar(100) NOT NULL DEFAULT 'RULE_TEMPLATE_V1',
    created_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    failure_code varchar(100),
    failure_message varchar(500),
    CONSTRAINT fk_ai_assist_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT ck_ai_assist_requests_type CHECK (
        assist_type_code IN ('ANNOUNCEMENT_SUMMARY', 'DOCUMENT_DRAFT', 'OPERATION_MEMO_SUMMARY', 'USER_REPLY_DRAFT')
    ),
    CONSTRAINT ck_ai_assist_requests_resource CHECK (
        resource_type IN ('GENERAL', 'ANNOUNCEMENT', 'APPLICATION_PROGRESS', 'MATCHING_CASE', 'OPERATION_TASK', 'USER')
    ),
    CONSTRAINT ck_ai_assist_requests_status CHECK (status_code IN ('REQUESTED', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_ai_assist_requests_input_length CHECK (input_length >= 0)
);

CREATE INDEX ix_ai_assist_requests_requested_by_status
    ON ai_assist_requests (requested_by, status_code, created_at);
CREATE INDEX ix_ai_assist_requests_resource
    ON ai_assist_requests (resource_type, resource_id);
CREATE INDEX ix_ai_assist_requests_type_created_at
    ON ai_assist_requests (assist_type_code, created_at);

CREATE TABLE ai_assist_results (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id uuid NOT NULL,
    result_text text NOT NULL,
    review_status_code varchar(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    prompt_token_count integer NOT NULL DEFAULT 0,
    completion_token_count integer NOT NULL DEFAULT 0,
    latency_ms integer NOT NULL DEFAULT 0,
    metadata_json jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    reviewed_by uuid,
    reviewed_at timestamptz,
    CONSTRAINT uq_ai_assist_results_request UNIQUE (request_id),
    CONSTRAINT fk_ai_assist_results_request FOREIGN KEY (request_id) REFERENCES ai_assist_requests (id),
    CONSTRAINT fk_ai_assist_results_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT ck_ai_assist_results_review_status CHECK (
        review_status_code IN ('PENDING_REVIEW', 'ACCEPTED', 'DISCARDED')
    ),
    CONSTRAINT ck_ai_assist_results_prompt_tokens CHECK (prompt_token_count >= 0),
    CONSTRAINT ck_ai_assist_results_completion_tokens CHECK (completion_token_count >= 0),
    CONSTRAINT ck_ai_assist_results_latency CHECK (latency_ms >= 0)
);

CREATE INDEX ix_ai_assist_results_review_status_created_at
    ON ai_assist_results (review_status_code, created_at);
