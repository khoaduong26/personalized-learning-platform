-- Run once against the application database before restarting the backend.
-- Existing subjects keep owner_id = NULL and remain shared.

SET @owner_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'subjects' AND column_name = 'owner_id'
);
SET @ddl = IF(@owner_column_exists = 0,
    'ALTER TABLE subjects ADD COLUMN owner_id BIGINT NULL', 'SELECT 1');
PREPARE curriculum_stmt FROM @ddl;
EXECUTE curriculum_stmt;
DEALLOCATE PREPARE curriculum_stmt;

SET @owner_fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE constraint_schema = DATABASE() AND table_name = 'subjects' AND column_name = 'owner_id'
      AND referenced_table_name = 'users' AND referenced_column_name = 'id'
);
SET @ddl = IF(@owner_fk_exists = 0,
    'ALTER TABLE subjects ADD CONSTRAINT fk_subject_owner FOREIGN KEY (owner_id) REFERENCES users(id)', 'SELECT 1');
PREPARE curriculum_stmt FROM @ddl;
EXECUTE curriculum_stmt;
DEALLOCATE PREPARE curriculum_stmt;

-- Remove the old global unique index on name; copies may share a name across owners.
SET @old_name_index = (
    SELECT index_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'subjects' AND column_name = 'name' AND non_unique = 0
      AND index_name <> 'PRIMARY'
    GROUP BY index_name
    HAVING COUNT(*) = 1
    LIMIT 1
);
SET @ddl = IF(@old_name_index IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE subjects DROP INDEX `', REPLACE(@old_name_index, '`', '``'), '`'));
PREPARE curriculum_stmt FROM @ddl;
EXECUTE curriculum_stmt;
DEALLOCATE PREPARE curriculum_stmt;

SET @owner_name_index_exists = (
    SELECT COUNT(*) FROM (
        SELECT index_name FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'subjects' AND non_unique = 0
        GROUP BY index_name
        HAVING COUNT(*) = 2 AND SUM(column_name = 'owner_id') = 1 AND SUM(column_name = 'name') = 1
    ) indexes_found
);
SET @ddl = IF(@owner_name_index_exists = 0,
    'ALTER TABLE subjects ADD CONSTRAINT uq_subject_owner_name UNIQUE (owner_id, name)', 'SELECT 1');
PREPARE curriculum_stmt FROM @ddl;
EXECUTE curriculum_stmt;
DEALLOCATE PREPARE curriculum_stmt;
