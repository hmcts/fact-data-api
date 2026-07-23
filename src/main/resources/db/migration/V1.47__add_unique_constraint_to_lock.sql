-- Guarantee only one lock can ever exist for subject_id/subject_type page combination
DO $$
DECLARE
    lock_table regclass := to_regclass(format('%I.%I', current_schema(), 'lock'));
BEGIN
    IF lock_table IS NOT NULL
        AND NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'unique_lock_subject_page'
              AND conrelid = lock_table
        ) THEN
        EXECUTE format(
            'ALTER TABLE %s ADD CONSTRAINT unique_lock_subject_page UNIQUE (subject_id, subject_type, page)',
            lock_table
        );
    END IF;
END
$$;
