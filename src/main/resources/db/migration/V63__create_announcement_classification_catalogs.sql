-- Create classification catalogs and multi-value assignments for operational announcements.
-- announcements.target_type_code remains the /api/v1 compatibility projection.

CREATE TABLE announcement_target_categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_code varchar(30) NOT NULL,
    category_name varchar(100) NOT NULL,
    is_enabled boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_announcement_target_categories_code UNIQUE (category_code),
    CONSTRAINT uq_announcement_target_categories_name UNIQUE (category_name),
    CONSTRAINT ck_announcement_target_categories_code CHECK (
        category_code IN ('BUSINESS', 'PERSONAL', 'SPOUSE', 'CHILD', 'PARENT')
    ),
    CONSTRAINT ck_announcement_target_categories_sort CHECK (sort_order >= 0)
);

CREATE INDEX ix_announcement_target_categories_enabled
    ON announcement_target_categories (is_enabled, sort_order, category_code);

CREATE TABLE announcement_support_types (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    support_type_code varchar(40) NOT NULL,
    support_type_name varchar(100) NOT NULL,
    is_enabled boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_announcement_support_types_code UNIQUE (support_type_code),
    CONSTRAINT uq_announcement_support_types_name UNIQUE (support_type_name),
    CONSTRAINT ck_announcement_support_types_code CHECK (
        support_type_code IN (
            'GENERAL_SUPPORT', 'GRANT_SUBSIDY', 'POLICY_FINANCE', 'GUARANTEE',
            'INTEREST_SUPPORT', 'VOUCHER_BENEFIT', 'REFUND_REDUCTION'
        )
    ),
    CONSTRAINT ck_announcement_support_types_sort CHECK (sort_order >= 0)
);

CREATE INDEX ix_announcement_support_types_enabled
    ON announcement_support_types (is_enabled, sort_order, support_type_code);

CREATE TABLE announcement_target_category_assignments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    target_category_id uuid NOT NULL,
    is_primary boolean NOT NULL DEFAULT false,
    assignment_source_code varchar(20) NOT NULL,
    assigned_by uuid,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_target_category_assignments_announcement
        FOREIGN KEY (announcement_id) REFERENCES announcements (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_target_category_assignments_category
        FOREIGN KEY (target_category_id) REFERENCES announcement_target_categories (id),
    CONSTRAINT fk_announcement_target_category_assignments_assigned_by
        FOREIGN KEY (assigned_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_target_category_assignments_category
        UNIQUE (announcement_id, target_category_id),
    CONSTRAINT ck_announcement_target_category_assignments_source CHECK (
        assignment_source_code IN ('MANUAL', 'SOURCE_CONFIRMED', 'LEGACY_BACKFILL')
    )
);

CREATE UNIQUE INDEX uq_announcement_target_category_assignments_primary
    ON announcement_target_category_assignments (announcement_id)
    WHERE is_primary = true;
CREATE INDEX ix_announcement_target_category_assignments_category
    ON announcement_target_category_assignments (target_category_id, announcement_id);

CREATE TABLE announcement_support_type_assignments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id uuid NOT NULL,
    support_type_id uuid NOT NULL,
    assignment_source_code varchar(20) NOT NULL,
    assigned_by uuid,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_announcement_support_type_assignments_announcement
        FOREIGN KEY (announcement_id) REFERENCES announcements (id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_support_type_assignments_support_type
        FOREIGN KEY (support_type_id) REFERENCES announcement_support_types (id),
    CONSTRAINT fk_announcement_support_type_assignments_assigned_by
        FOREIGN KEY (assigned_by) REFERENCES users (id),
    CONSTRAINT uq_announcement_support_type_assignments_type
        UNIQUE (announcement_id, support_type_id),
    CONSTRAINT ck_announcement_support_type_assignments_source CHECK (
        assignment_source_code IN ('MANUAL', 'SOURCE_CONFIRMED', 'LEGACY_BACKFILL')
    )
);

CREATE INDEX ix_announcement_support_type_assignments_type
    ON announcement_support_type_assignments (support_type_id, announcement_id);

INSERT INTO announcement_target_categories (
    id, category_code, category_name, is_enabled, sort_order
) VALUES
    (md5('announcement-target-category-BUSINESS')::uuid, 'BUSINESS', '사업자', true, 10),
    (md5('announcement-target-category-PERSONAL')::uuid, 'PERSONAL', '본인(개인)', true, 20),
    (md5('announcement-target-category-SPOUSE')::uuid, 'SPOUSE', '배우자', true, 30),
    (md5('announcement-target-category-CHILD')::uuid, 'CHILD', '자녀', true, 40),
    (md5('announcement-target-category-PARENT')::uuid, 'PARENT', '부모님', true, 50);

INSERT INTO announcement_support_types (
    id, support_type_code, support_type_name, is_enabled, sort_order
) VALUES
    (md5('announcement-support-type-GENERAL_SUPPORT')::uuid, 'GENERAL_SUPPORT', '일반 지원', true, 10),
    (md5('announcement-support-type-GRANT_SUBSIDY')::uuid, 'GRANT_SUBSIDY', '지원금·보조금', true, 20),
    (md5('announcement-support-type-POLICY_FINANCE')::uuid, 'POLICY_FINANCE', '정책자금·금융', true, 30),
    (md5('announcement-support-type-GUARANTEE')::uuid, 'GUARANTEE', '보증', true, 40),
    (md5('announcement-support-type-INTEREST_SUPPORT')::uuid, 'INTEREST_SUPPORT', '이자 지원', true, 50),
    (md5('announcement-support-type-VOUCHER_BENEFIT')::uuid, 'VOUCHER_BENEFIT', '바우처·혜택', true, 60),
    (md5('announcement-support-type-REFUND_REDUCTION')::uuid, 'REFUND_REDUCTION', '환급·감면', true, 70);

-- Deterministically preserve every existing /api/v1 target as the primary assignment.
INSERT INTO announcement_target_category_assignments (
    id,
    announcement_id,
    target_category_id,
    is_primary,
    assignment_source_code,
    assigned_by,
    assigned_at
)
SELECT
    md5('announcement-target-backfill-' || announcement.id::text)::uuid,
    announcement.id,
    category.id,
    true,
    'LEGACY_BACKFILL',
    NULL,
    announcement.created_at
FROM announcements AS announcement
JOIN announcement_target_categories AS category
  ON category.category_code = announcement.target_type_code;

-- Existing rows do not contain a trustworthy support-type source, so no support assignment is inferred.
