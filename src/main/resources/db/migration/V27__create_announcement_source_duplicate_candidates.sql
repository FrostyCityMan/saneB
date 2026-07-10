-- Create duplicate/similar candidate table between collected sources and active operational announcements.
-- Candidates are review aids only and do not create or update matching conditions automatically.

CREATE TABLE announcement_source_duplicate_candidates (
    id uuid PRIMARY KEY,
    source_id uuid NOT NULL,
    announcement_id uuid NOT NULL,
    match_type_code varchar(30) NOT NULL,
    title_matched boolean NOT NULL DEFAULT false,
    agency_matched boolean NOT NULL DEFAULT false,
    provider_notice_matched boolean NOT NULL DEFAULT false,
    period_matched boolean NOT NULL DEFAULT false,
    source_url_matched boolean NOT NULL DEFAULT false,
    similarity_reason text,
    decision_status_code varchar(30) NOT NULL DEFAULT 'PENDING',
    decided_by uuid,
    decided_at timestamptz,
    decision_note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_duplicate_candidates_source FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_duplicate_candidates_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT fk_announcement_source_duplicate_candidates_decided_by FOREIGN KEY (decided_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_source_duplicate_candidates_source_announcement UNIQUE (source_id, announcement_id),
    CONSTRAINT ck_announcement_source_duplicate_candidates_match_type CHECK (match_type_code IN ('EXACT_DUPLICATE', 'SIMILAR')),
    CONSTRAINT ck_announcement_source_duplicate_candidates_decision CHECK (decision_status_code IN ('PENDING', 'CREATE_NEW_SELECTED', 'UPDATE_EXISTING_SELECTED', 'IGNORED'))
);

CREATE INDEX ix_announcement_source_duplicate_candidates_source
    ON announcement_source_duplicate_candidates (source_id, match_type_code, decision_status_code);
CREATE INDEX ix_announcement_source_duplicate_candidates_announcement
    ON announcement_source_duplicate_candidates (announcement_id, decision_status_code);
