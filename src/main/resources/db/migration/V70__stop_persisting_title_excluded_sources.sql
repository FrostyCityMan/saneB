-- Preserve only non-reversible identity hashes and rule references for title-excluded announcements.
-- The original title, body, URL, provider payload, and source snapshot are intentionally removed.

CREATE TABLE announcement_source_exclusion_tombstones (
    id uuid PRIMARY KEY,
    provider_code varchar(50) NOT NULL,
    identity_hash varchar(64) NOT NULL,
    last_raw_hash varchar(64) NOT NULL,
    run_id uuid,
    rule_release_id uuid,
    semantic_reason_code varchar(80) NOT NULL,
    title_stage_code varchar(40),
    body_stage_code varchar(40),
    decision_source_code varchar(30) NOT NULL,
    occurrence_count integer NOT NULL DEFAULT 1,
    first_excluded_at timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_announcement_source_exclusion_tombstones_identity
        UNIQUE (provider_code, identity_hash),
    CONSTRAINT fk_announcement_source_exclusion_tombstones_run
        FOREIGN KEY (run_id) REFERENCES announcement_source_collection_runs (id) ON DELETE SET NULL,
    CONSTRAINT fk_announcement_source_exclusion_tombstones_release
        FOREIGN KEY (rule_release_id)
        REFERENCES announcement_source_classification_rule_releases (id) ON DELETE SET NULL,
    CONSTRAINT ck_announcement_source_exclusion_tombstones_provider
        CHECK (provider_code IN ('BIZINFO', 'GOV24_PUBLIC_SERVICE', 'LOCAL_GOV_NOTICE')),
    CONSTRAINT ck_announcement_source_exclusion_tombstones_identity_hash
        CHECK (identity_hash ~ '^[0-9A-Fa-f]{64}$'),
    CONSTRAINT ck_announcement_source_exclusion_tombstones_raw_hash
        CHECK (last_raw_hash ~ '^[0-9A-Fa-f]{64}$'),
    CONSTRAINT ck_announcement_source_exclusion_tombstones_decision_source
        CHECK (decision_source_code IN ('V2_ENGINE', 'LEGACY_SEMANTIC')),
    CONSTRAINT ck_announcement_source_exclusion_tombstones_occurrence_count
        CHECK (occurrence_count > 0)
);

CREATE INDEX ix_announcement_source_exclusion_tombstones_recent
    ON announcement_source_exclusion_tombstones (provider_code, last_seen_at DESC);

CREATE TABLE announcement_source_exclusion_rule_matches (
    id uuid PRIMARY KEY,
    exclusion_id uuid NOT NULL,
    keyword_rule_id uuid NOT NULL,
    keyword_term_id uuid NOT NULL,
    match_location_code varchar(20) NOT NULL,
    applied_action_code varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_exclusion_matches_exclusion
        FOREIGN KEY (exclusion_id)
        REFERENCES announcement_source_exclusion_tombstones (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_exclusion_matches_rule
        FOREIGN KEY (keyword_rule_id)
        REFERENCES announcement_source_classification_keyword_rules (id),
    CONSTRAINT fk_announcement_source_exclusion_matches_term_rule
        FOREIGN KEY (keyword_term_id, keyword_rule_id)
        REFERENCES announcement_source_classification_keyword_terms (id, keyword_rule_id),
    CONSTRAINT uq_announcement_source_exclusion_matches_evidence
        UNIQUE (
            exclusion_id,
            keyword_rule_id,
            keyword_term_id,
            match_location_code,
            applied_action_code
        ),
    CONSTRAINT ck_announcement_source_exclusion_matches_location
        CHECK (match_location_code IN ('TITLE', 'BODY')),
    CONSTRAINT ck_announcement_source_exclusion_matches_action
        CHECK (applied_action_code IN (
            'EXCLUDED', 'REVIEW_REQUIRED', 'TAG', 'CONTEXT_ONLY', 'MASK_ONLY'
        ))
);

-- Keep reclassification audit rows while severing all pointers to excluded source content.
ALTER TABLE announcement_source_reclassification_run_items
    ADD COLUMN exclusion_id uuid;

ALTER TABLE announcement_source_reclassification_run_items
    DROP CONSTRAINT fk_announcement_source_reclassification_run_items_source,
    DROP CONSTRAINT fk_announcement_source_reclassification_run_items_content,
    DROP CONSTRAINT fk_announcement_source_reclassification_run_items_previous_evaluation,
    DROP CONSTRAINT fk_announcement_source_reclassification_run_items_applied_evaluation;

ALTER TABLE announcement_source_reclassification_run_items
    ALTER COLUMN source_id DROP NOT NULL,
    ALTER COLUMN content_version_id DROP NOT NULL,
    ADD CONSTRAINT fk_announcement_source_reclassification_run_items_source
        FOREIGN KEY (source_id) REFERENCES announcement_source_snapshots (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_announcement_source_reclassification_run_items_content
        FOREIGN KEY (content_version_id)
        REFERENCES announcement_source_content_versions (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_announcement_source_reclassification_run_items_previous_evaluation
        FOREIGN KEY (previous_evaluation_id)
        REFERENCES announcement_source_classification_evaluations (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_announcement_source_reclassification_run_items_applied_evaluation
        FOREIGN KEY (applied_evaluation_id)
        REFERENCES announcement_source_classification_evaluations (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_announcement_source_reclassification_run_items_exclusion
        FOREIGN KEY (exclusion_id)
        REFERENCES announcement_source_exclusion_tombstones (id),
    ADD CONSTRAINT ck_announcement_source_reclassification_run_items_exclusion_redaction
        CHECK (
            exclusion_id IS NULL
            OR (
                source_id IS NULL
                AND content_version_id IS NULL
                AND previous_evaluation_id IS NULL
                AND applied_evaluation_id IS NULL
                AND previous_semantic_matched_keywords IS NULL
            )
        );

CREATE INDEX ix_announcement_source_reclassification_run_items_exclusion
    ON announcement_source_reclassification_run_items (exclusion_id, run_id);

-- Fail closed when an excluded source is already linked to an operating announcement.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM announcement_source_snapshots AS source
        INNER JOIN announcement_source_links AS link ON link.source_id = source.id
        WHERE source.semantic_status_code = 'EXCLUDED'
    ) THEN
        RAISE EXCEPTION 'Linked excluded announcement sources must be separated before V70 cleanup.';
    END IF;
END
$$;

-- Do not erase source content while a reclassification phase can still act on it.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM announcement_source_reclassification_run_items AS item
        INNER JOIN announcement_source_reclassification_runs AS run ON run.id = item.run_id
        INNER JOIN announcement_source_snapshots AS source ON source.id = item.source_id
        WHERE source.semantic_status_code = 'EXCLUDED'
          AND run.run_status_code NOT IN (
              'PREVIEW_COMPLETED', 'PREVIEW_PARTIAL_FAILED',
              'APPLY_COMPLETED', 'APPLY_PARTIAL_FAILED',
              'ROLLBACK_COMPLETED', 'ROLLBACK_PARTIAL_FAILED'
          )
    ) THEN
        RAISE EXCEPTION 'Active reclassification runs for excluded sources must finish before V70 cleanup.';
    END IF;
END
$$;

-- Backfill one non-reversible tombstone per excluded source before deleting source content.
WITH excluded_sources AS (
    SELECT
        source.id AS source_id,
        source.provider_code,
        encode(
            digest(
                source.provider_code || E'\n' ||
                CASE
                    WHEN nullif(btrim(source.provider_notice_id), '') IS NOT NULL
                        THEN 'PROVIDER_NOTICE_ID:' || btrim(source.provider_notice_id)
                    WHEN nullif(btrim(source.canonical_source_url), '') IS NOT NULL
                        THEN 'CANONICAL_URL:' || btrim(source.canonical_source_url)
                    WHEN nullif(btrim(source.source_url), '') IS NOT NULL
                        THEN 'SOURCE_URL:' || btrim(source.source_url)
                    ELSE 'RAW_HASH:' || source.raw_hash
                END,
                'sha256'
            ),
            'hex'
        ) AS identity_hash,
        source.raw_hash,
        source.semantic_reason_code,
        source.collected_at
    FROM announcement_source_snapshots AS source
    WHERE source.semantic_status_code = 'EXCLUDED'
),
latest_evaluations AS (
    SELECT DISTINCT ON (evaluation.source_id)
        evaluation.source_id,
        evaluation.run_id,
        evaluation.rule_release_id,
        evaluation.title_stage_code,
        evaluation.body_stage_code
    FROM announcement_source_classification_evaluations AS evaluation
    INNER JOIN excluded_sources AS source ON source.source_id = evaluation.source_id
    ORDER BY
        evaluation.source_id,
        evaluation.is_current DESC,
        evaluation.evaluated_at DESC,
        evaluation.id DESC
),
run_statistics AS (
    SELECT
        item.source_id,
        count(*) FILTER (WHERE item.item_status_code = 'EXCLUDED') AS occurrence_count,
        min(item.created_at) FILTER (WHERE item.item_status_code = 'EXCLUDED') AS first_excluded_at,
        max(item.created_at) FILTER (WHERE item.item_status_code = 'EXCLUDED') AS last_seen_at
    FROM announcement_source_collection_run_items AS item
    INNER JOIN excluded_sources AS source ON source.source_id = item.source_id
    GROUP BY item.source_id
),
latest_run_items AS (
    SELECT DISTINCT ON (item.source_id)
        item.source_id,
        item.run_id
    FROM announcement_source_collection_run_items AS item
    INNER JOIN excluded_sources AS source ON source.source_id = item.source_id
    WHERE item.item_status_code = 'EXCLUDED'
    ORDER BY item.source_id, item.created_at DESC, item.id DESC
)
INSERT INTO announcement_source_exclusion_tombstones (
    id,
    provider_code,
    identity_hash,
    last_raw_hash,
    run_id,
    rule_release_id,
    semantic_reason_code,
    title_stage_code,
    body_stage_code,
    decision_source_code,
    occurrence_count,
    first_excluded_at,
    last_seen_at
)
SELECT
    md5('announcement-source-exclusion-' || source.identity_hash)::uuid,
    source.provider_code,
    source.identity_hash,
    source.raw_hash,
    coalesce(evaluation.run_id, latest_item.run_id),
    evaluation.rule_release_id,
    source.semantic_reason_code,
    evaluation.title_stage_code,
    evaluation.body_stage_code,
    CASE WHEN evaluation.source_id IS NULL THEN 'LEGACY_SEMANTIC' ELSE 'V2_ENGINE' END,
    greatest(coalesce(statistics.occurrence_count, 0), 1),
    least(source.collected_at, coalesce(statistics.first_excluded_at, source.collected_at)),
    greatest(source.collected_at, coalesce(statistics.last_seen_at, source.collected_at))
FROM excluded_sources AS source
LEFT JOIN latest_evaluations AS evaluation ON evaluation.source_id = source.source_id
LEFT JOIN run_statistics AS statistics ON statistics.source_id = source.source_id
LEFT JOIN latest_run_items AS latest_item ON latest_item.source_id = source.source_id
ON CONFLICT (provider_code, identity_hash) DO UPDATE
SET last_raw_hash = EXCLUDED.last_raw_hash,
    run_id = coalesce(EXCLUDED.run_id, announcement_source_exclusion_tombstones.run_id),
    rule_release_id = coalesce(
        EXCLUDED.rule_release_id,
        announcement_source_exclusion_tombstones.rule_release_id
    ),
    semantic_reason_code = EXCLUDED.semantic_reason_code,
    title_stage_code = coalesce(
        EXCLUDED.title_stage_code,
        announcement_source_exclusion_tombstones.title_stage_code
    ),
    body_stage_code = coalesce(
        EXCLUDED.body_stage_code,
        announcement_source_exclusion_tombstones.body_stage_code
    ),
    decision_source_code = EXCLUDED.decision_source_code,
    occurrence_count = announcement_source_exclusion_tombstones.occurrence_count
        + EXCLUDED.occurrence_count,
    first_excluded_at = least(
        announcement_source_exclusion_tombstones.first_excluded_at,
        EXCLUDED.first_excluded_at
    ),
    last_seen_at = greatest(
        announcement_source_exclusion_tombstones.last_seen_at,
        EXCLUDED.last_seen_at
    ),
    updated_at = now();

-- Preserve rule and term references without retaining matched source text or offsets.
WITH excluded_sources AS (
    SELECT
        source.id AS source_id,
        source.provider_code,
        encode(
            digest(
                source.provider_code || E'\n' ||
                CASE
                    WHEN nullif(btrim(source.provider_notice_id), '') IS NOT NULL
                        THEN 'PROVIDER_NOTICE_ID:' || btrim(source.provider_notice_id)
                    WHEN nullif(btrim(source.canonical_source_url), '') IS NOT NULL
                        THEN 'CANONICAL_URL:' || btrim(source.canonical_source_url)
                    WHEN nullif(btrim(source.source_url), '') IS NOT NULL
                        THEN 'SOURCE_URL:' || btrim(source.source_url)
                    ELSE 'RAW_HASH:' || source.raw_hash
                END,
                'sha256'
            ),
            'hex'
        ) AS identity_hash
    FROM announcement_source_snapshots AS source
    WHERE source.semantic_status_code = 'EXCLUDED'
)
INSERT INTO announcement_source_exclusion_rule_matches (
    id,
    exclusion_id,
    keyword_rule_id,
    keyword_term_id,
    match_location_code,
    applied_action_code
)
SELECT DISTINCT
    md5(
        'announcement-source-exclusion-match-'
        || tombstone.id::text || '-'
        || classification_match.keyword_rule_id::text || '-'
        || classification_match.keyword_term_id::text || '-'
        || classification_match.match_location_code || '-'
        || classification_match.applied_action_code
    )::uuid,
    tombstone.id,
    classification_match.keyword_rule_id,
    classification_match.keyword_term_id,
    classification_match.match_location_code,
    classification_match.applied_action_code
FROM excluded_sources AS source
INNER JOIN announcement_source_exclusion_tombstones AS tombstone
        ON tombstone.provider_code = source.provider_code
       AND tombstone.identity_hash = source.identity_hash
INNER JOIN announcement_source_classification_evaluations AS evaluation
        ON evaluation.source_id = source.source_id
INNER JOIN announcement_source_classification_matches AS classification_match
        ON classification_match.evaluation_id = evaluation.id
ON CONFLICT (
    exclusion_id,
    keyword_rule_id,
    keyword_term_id,
    match_location_code,
    applied_action_code
) DO NOTHING;

-- Retain reclassification status and hashes, but replace excluded source pointers with tombstones.
WITH excluded_sources AS (
    SELECT
        source.id AS source_id,
        source.provider_code,
        encode(
            digest(
                source.provider_code || E'\n' ||
                CASE
                    WHEN nullif(btrim(source.provider_notice_id), '') IS NOT NULL
                        THEN 'PROVIDER_NOTICE_ID:' || btrim(source.provider_notice_id)
                    WHEN nullif(btrim(source.canonical_source_url), '') IS NOT NULL
                        THEN 'CANONICAL_URL:' || btrim(source.canonical_source_url)
                    WHEN nullif(btrim(source.source_url), '') IS NOT NULL
                        THEN 'SOURCE_URL:' || btrim(source.source_url)
                    ELSE 'RAW_HASH:' || source.raw_hash
                END,
                'sha256'
            ),
            'hex'
        ) AS identity_hash
    FROM announcement_source_snapshots AS source
    WHERE source.semantic_status_code = 'EXCLUDED'
)
UPDATE announcement_source_reclassification_run_items AS item
SET exclusion_id = tombstone.id,
    source_id = NULL,
    content_version_id = NULL,
    previous_evaluation_id = NULL,
    applied_evaluation_id = NULL,
    previous_semantic_matched_keywords = NULL,
    updated_at = now()
FROM excluded_sources AS source
INNER JOIN announcement_source_exclusion_tombstones AS tombstone
        ON tombstone.provider_code = source.provider_code
       AND tombstone.identity_hash = source.identity_hash
WHERE item.source_id = source.source_id;

-- Run counts and status codes remain, but references and plaintext for every finally excluded source do not.
UPDATE announcement_source_collection_run_items AS item
SET source_id = NULL,
    provider_notice_id = NULL,
    source_url = NULL,
    semantic_matched_keywords = NULL
WHERE item.item_status_code = 'EXCLUDED'
   OR EXISTS (
       SELECT 1
       FROM announcement_source_snapshots AS source
       WHERE source.id = item.source_id
         AND source.semantic_status_code = 'EXCLUDED'
   );

-- Cascades remove content versions, evaluations, original matched text, and other source children.
DELETE FROM announcement_source_snapshots
WHERE semantic_status_code = 'EXCLUDED';

-- The migration must not complete while any excluded plaintext remains in collection storage.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM announcement_source_snapshots
        WHERE semantic_status_code = 'EXCLUDED'
    ) OR EXISTS (
        SELECT 1
        FROM announcement_source_collection_run_items
        WHERE item_status_code = 'EXCLUDED'
          AND (
              source_id IS NOT NULL
              OR provider_notice_id IS NOT NULL
              OR source_url IS NOT NULL
              OR semantic_matched_keywords IS NOT NULL
          )
    ) OR EXISTS (
        SELECT 1
        FROM announcement_source_reclassification_run_items
        WHERE exclusion_id IS NOT NULL
          AND (
              source_id IS NOT NULL
              OR content_version_id IS NOT NULL
              OR previous_evaluation_id IS NOT NULL
              OR applied_evaluation_id IS NOT NULL
              OR previous_semantic_matched_keywords IS NOT NULL
          )
    ) THEN
        RAISE EXCEPTION 'Excluded announcement plaintext cleanup did not complete.';
    END IF;
END
$$;

COMMENT ON TABLE announcement_source_exclusion_tombstones IS
    'Non-reversible identity and decision evidence for title-excluded external announcements.';
COMMENT ON TABLE announcement_source_exclusion_rule_matches IS
    'Rule references for excluded announcements without matched source text or offsets.';
COMMENT ON COLUMN announcement_source_reclassification_run_items.exclusion_id IS
    'Non-reversible exclusion evidence replacing deleted source content; such items cannot be rolled back.';
