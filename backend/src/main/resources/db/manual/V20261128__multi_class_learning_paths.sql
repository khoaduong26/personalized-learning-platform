-- Run once against the application database before restarting the backend.
-- Preserve students.classroom_id as the legacy first membership and copy it
-- into the new many-to-many membership table.

CREATE TABLE IF NOT EXISTS classroom_students (
    student_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    PRIMARY KEY (student_id, classroom_id),
    KEY ix_classroom_students_classroom (classroom_id),
    CONSTRAINT fk_classroom_students_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_classroom_students_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id)
);

INSERT IGNORE INTO classroom_students (student_id, classroom_id)
SELECT id, classroom_id FROM students WHERE classroom_id IS NOT NULL;

SET @path_classroom_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'learning_paths' AND column_name = 'classroom_id'
);
SET @ddl = IF(@path_classroom_column_exists = 0,
    'ALTER TABLE learning_paths ADD COLUMN classroom_id BIGINT NULL', 'SELECT 1');
PREPARE multi_class_stmt FROM @ddl;
EXECUTE multi_class_stmt;
DEALLOCATE PREPARE multi_class_stmt;

SET @path_classroom_fk_exists = (
    SELECT COUNT(*) FROM information_schema.key_column_usage
    WHERE constraint_schema = DATABASE() AND table_name = 'learning_paths' AND column_name = 'classroom_id'
      AND referenced_table_name = 'classrooms' AND referenced_column_name = 'id'
);
SET @ddl = IF(@path_classroom_fk_exists = 0,
    'ALTER TABLE learning_paths ADD CONSTRAINT fk_learning_path_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id)', 'SELECT 1');
PREPARE multi_class_stmt FROM @ddl;
EXECUTE multi_class_stmt;
DEALLOCATE PREPARE multi_class_stmt;

-- Attach existing student-only paths to their previous primary classroom.
UPDATE learning_paths lp
JOIN students s ON s.id = lp.student_id
SET lp.classroom_id = s.classroom_id
WHERE lp.classroom_id IS NULL AND s.classroom_id IS NOT NULL;
