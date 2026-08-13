-- Separate production evidence from explicitly created QA artifacts.
-- Existing rows remain PRODUCTION; no historical row is inferred to be QA.

ALTER TABLE announcement_source_collection_requests
    ADD COLUMN data_purpose_code varchar(20) NOT NULL DEFAULT 'PRODUCTION';

ALTER TABLE announcement_source_collection_requests
    ADD CONSTRAINT ck_announcement_source_collection_requests_data_purpose CHECK (
        data_purpose_code IN ('PRODUCTION', 'QA')
    );

CREATE INDEX ix_announcement_source_collection_requests_data_purpose
    ON announcement_source_collection_requests (
        data_purpose_code, provider_code, request_status_code, requested_at DESC
    );

ALTER TABLE announcement_source_snapshots
    ADD COLUMN data_purpose_code varchar(20) NOT NULL DEFAULT 'PRODUCTION';

ALTER TABLE announcement_source_snapshots
    ADD CONSTRAINT ck_announcement_source_snapshots_data_purpose CHECK (
        data_purpose_code IN ('PRODUCTION', 'QA')
    );

CREATE INDEX ix_announcement_source_snapshots_data_purpose
    ON announcement_source_snapshots (
        data_purpose_code, provider_code, review_status_code, collected_at DESC
    );

-- QA cleanup callers must explicitly filter data_purpose_code = 'QA'.
-- This migration intentionally does not relabel or delete any existing source row.
