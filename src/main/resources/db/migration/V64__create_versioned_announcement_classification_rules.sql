-- Create immutable-release classification rules. An ACTIVE release is never edited in place.

CREATE TABLE announcement_source_classification_rule_releases (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    release_code varchar(40) NOT NULL,
    version_no integer NOT NULL,
    row_version integer NOT NULL DEFAULT 0,
    release_status_code varchar(20) NOT NULL DEFAULT 'DRAFT',
    rule_snapshot_hash varchar(64),
    combination_operator_code varchar(10) NOT NULL DEFAULT 'AND',
    body_unavailable_action_code varchar(30) NOT NULL DEFAULT 'REVIEW_REQUIRED',
    attachment_analysis_enabled boolean NOT NULL DEFAULT false,
    auto_activation_enabled boolean NOT NULL DEFAULT false,
    change_note varchar(1000),
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    activated_by uuid,
    activated_at timestamptz,
    retired_at timestamptz,
    CONSTRAINT fk_announcement_source_classification_releases_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_announcement_source_classification_releases_activated_by
        FOREIGN KEY (activated_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_source_classification_releases_code UNIQUE (release_code),
    CONSTRAINT uq_announcement_source_classification_releases_version UNIQUE (version_no),
    CONSTRAINT ck_announcement_source_classification_releases_version CHECK (version_no > 0),
    CONSTRAINT ck_announcement_source_classification_releases_row_version CHECK (row_version >= 0),
    CONSTRAINT ck_announcement_source_classification_releases_status CHECK (
        release_status_code IN ('DRAFT', 'ACTIVE', 'RETIRED')
    ),
    CONSTRAINT ck_announcement_source_classification_releases_hash CHECK (
        rule_snapshot_hash IS NULL OR rule_snapshot_hash ~ '^[0-9a-f]{64}$'
    ),
    CONSTRAINT ck_announcement_source_classification_releases_operator CHECK (
        combination_operator_code = 'AND'
    ),
    CONSTRAINT ck_announcement_source_classification_releases_body_action CHECK (
        body_unavailable_action_code = 'REVIEW_REQUIRED'
    ),
    CONSTRAINT ck_announcement_source_classification_releases_attachment CHECK (
        attachment_analysis_enabled = false
    ),
    CONSTRAINT ck_announcement_source_classification_releases_auto_activation CHECK (
        auto_activation_enabled = false
    ),
    CONSTRAINT ck_announcement_source_classification_releases_lifecycle CHECK (
        (release_status_code = 'DRAFT' AND activated_at IS NULL AND retired_at IS NULL)
        OR (release_status_code = 'ACTIVE' AND activated_at IS NOT NULL AND retired_at IS NULL)
        OR (release_status_code = 'RETIRED' AND activated_at IS NOT NULL AND retired_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_announcement_source_classification_releases_active
    ON announcement_source_classification_rule_releases (release_status_code)
    WHERE release_status_code = 'ACTIVE';
CREATE INDEX ix_announcement_source_classification_releases_status
    ON announcement_source_classification_rule_releases (release_status_code, version_no DESC);

CREATE TABLE announcement_source_classification_rule_groups (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    release_id uuid NOT NULL,
    group_code varchar(80) NOT NULL,
    group_name varchar(150) NOT NULL,
    group_kind_code varchar(30) NOT NULL,
    target_category_id uuid,
    support_type_id uuid,
    title_action_code varchar(30) NOT NULL,
    body_action_code varchar(30) NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    is_enabled boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_classification_groups_release
        FOREIGN KEY (release_id) REFERENCES announcement_source_classification_rule_releases (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_classification_groups_target
        FOREIGN KEY (target_category_id) REFERENCES announcement_target_categories (id),
    CONSTRAINT fk_announcement_source_classification_groups_support
        FOREIGN KEY (support_type_id) REFERENCES announcement_support_types (id),
    CONSTRAINT uq_announcement_source_classification_groups_code UNIQUE (release_id, group_code),
    CONSTRAINT ck_announcement_source_classification_groups_kind CHECK (
        group_kind_code IN (
            'TARGET', 'SUPPORT_TYPE', 'REVIEW_A', 'AUTO_EXCLUDE_B', 'CONTEXT',
            'PROTECTED_METADATA'
        )
    ),
    CONSTRAINT ck_announcement_source_classification_groups_title_action CHECK (
        title_action_code IN ('EXCLUDED', 'REVIEW_REQUIRED', 'TAG', 'CONTEXT_ONLY', 'MASK_ONLY')
    ),
    CONSTRAINT ck_announcement_source_classification_groups_body_action CHECK (
        body_action_code IN ('EXCLUDED', 'REVIEW_REQUIRED', 'TAG', 'CONTEXT_ONLY', 'MASK_ONLY')
    ),
    CONSTRAINT ck_announcement_source_classification_groups_shape CHECK (
        (group_kind_code = 'TARGET' AND target_category_id IS NOT NULL AND support_type_id IS NULL)
        OR (group_kind_code = 'SUPPORT_TYPE' AND target_category_id IS NULL AND support_type_id IS NOT NULL)
        OR (group_kind_code NOT IN ('TARGET', 'SUPPORT_TYPE')
            AND target_category_id IS NULL AND support_type_id IS NULL)
    ),
    CONSTRAINT ck_announcement_source_classification_groups_actions CHECK (
        (group_kind_code = 'AUTO_EXCLUDE_B'
            AND title_action_code = 'EXCLUDED' AND body_action_code = 'REVIEW_REQUIRED')
        OR (group_kind_code = 'REVIEW_A'
            AND title_action_code = 'REVIEW_REQUIRED' AND body_action_code = 'REVIEW_REQUIRED')
        OR (group_kind_code IN ('TARGET', 'SUPPORT_TYPE')
            AND title_action_code = 'TAG' AND body_action_code = 'TAG')
        OR (group_kind_code = 'CONTEXT'
            AND title_action_code = 'CONTEXT_ONLY' AND body_action_code = 'CONTEXT_ONLY')
        OR (group_kind_code = 'PROTECTED_METADATA'
            AND title_action_code = 'MASK_ONLY' AND body_action_code = 'MASK_ONLY')
    ),
    CONSTRAINT ck_announcement_source_classification_groups_sort CHECK (sort_order >= 0)
);

CREATE INDEX ix_announcement_source_classification_groups_release
    ON announcement_source_classification_rule_groups (
        release_id, is_enabled, group_kind_code, sort_order, group_code
    );

CREATE TABLE announcement_source_classification_keyword_rules (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id uuid NOT NULL,
    rule_code varchar(100) NOT NULL,
    strength_code varchar(30) NOT NULL,
    is_enabled boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    row_version integer NOT NULL DEFAULT 0,
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_classification_rules_group
        FOREIGN KEY (group_id) REFERENCES announcement_source_classification_rule_groups (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_classification_rules_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_announcement_source_classification_rules_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_source_classification_rules_code UNIQUE (group_id, rule_code),
    CONSTRAINT uq_announcement_source_classification_rules_id_group UNIQUE (id, group_id),
    CONSTRAINT ck_announcement_source_classification_rules_strength CHECK (
        strength_code IN ('STRONG', 'SUPPLEMENTARY')
    ),
    CONSTRAINT ck_announcement_source_classification_rules_sort CHECK (sort_order >= 0),
    CONSTRAINT ck_announcement_source_classification_rules_row_version CHECK (row_version >= 0)
);

CREATE INDEX ix_announcement_source_classification_rules_group
    ON announcement_source_classification_keyword_rules (group_id, is_enabled, sort_order, rule_code);

CREATE TABLE announcement_source_classification_keyword_terms (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    keyword_rule_id uuid NOT NULL,
    group_id uuid NOT NULL,
    term_type_code varchar(20) NOT NULL,
    term_text varchar(200) NOT NULL,
    normalized_term_text varchar(200) NOT NULL,
    match_mode_code varchar(30) NOT NULL,
    is_discovery_term boolean NOT NULL DEFAULT false,
    discovery_order integer,
    is_classification_term boolean NOT NULL DEFAULT true,
    is_enabled boolean NOT NULL DEFAULT true,
    created_by uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_source_classification_terms_rule_group
        FOREIGN KEY (keyword_rule_id, group_id)
        REFERENCES announcement_source_classification_keyword_rules (id, group_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_announcement_source_classification_terms_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_announcement_source_classification_terms_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_source_classification_terms_group_text_mode
        UNIQUE (group_id, normalized_term_text, match_mode_code),
    CONSTRAINT uq_announcement_source_classification_terms_id_rule
        UNIQUE (id, keyword_rule_id),
    CONSTRAINT ck_announcement_source_classification_terms_type CHECK (
        term_type_code IN ('CANONICAL', 'SYNONYM')
    ),
    CONSTRAINT ck_announcement_source_classification_terms_mode CHECK (
        match_mode_code IN ('NORMALIZED_PHRASE', 'TOKEN', 'EXACT_TITLE')
    ),
    CONSTRAINT ck_announcement_source_classification_terms_text CHECK (
        length(btrim(term_text)) > 0 AND length(btrim(normalized_term_text)) > 0
    ),
    CONSTRAINT ck_announcement_source_classification_terms_discovery CHECK (
        (is_discovery_term AND discovery_order IS NOT NULL AND discovery_order > 0)
        OR (NOT is_discovery_term AND discovery_order IS NULL)
    )
);

CREATE UNIQUE INDEX uq_announcement_source_classification_terms_canonical
    ON announcement_source_classification_keyword_terms (keyword_rule_id)
    WHERE term_type_code = 'CANONICAL';
CREATE UNIQUE INDEX uq_announcement_source_classification_terms_discovery_order
    ON announcement_source_classification_keyword_terms (group_id, discovery_order)
    WHERE is_discovery_term = true;
CREATE INDEX ix_announcement_source_classification_terms_lookup
    ON announcement_source_classification_keyword_terms (
        group_id, is_enabled, is_classification_term, match_mode_code, normalized_term_text
    );
