-- Abort explicitly when legacy data violates the one-source-one-announcement contract.
DO $$
DECLARE
    duplicate_source_count bigint;
BEGIN
    SELECT count(1)
    INTO duplicate_source_count
    FROM (
        SELECT source_id
        FROM announcement_source_links
        GROUP BY source_id
        HAVING count(1) > 1
    ) duplicate_sources;

    IF duplicate_source_count > 0 THEN
        RAISE EXCEPTION USING
            ERRCODE = '23505',
            MESSAGE = format(
                    'V68 cannot enforce one announcement link per source: %s duplicated source_id value(s) exist.',
                    duplicate_source_count
            ),
            HINT = 'Review duplicate source links manually before rerunning. V68 does not modify existing data.';
    END IF;
END
$$;

-- A collected source can be converted to only one operational announcement.
ALTER TABLE announcement_source_links
    ADD CONSTRAINT uq_announcement_source_links_source UNIQUE (source_id);
