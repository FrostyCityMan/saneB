-- Support source cleanup and foreign-key maintenance without repeatedly scanning every run item.
CREATE INDEX IF NOT EXISTS ix_announcement_source_collection_run_items_source
    ON announcement_source_collection_run_items (source_id)
    WHERE source_id IS NOT NULL;

COMMENT ON INDEX ix_announcement_source_collection_run_items_source IS
    'Supports source-level redaction and deletion while preserving collection run history.';
