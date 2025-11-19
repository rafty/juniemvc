-- Flyway V4: Add optional description column to beer table
-- H2-compatible SQL. Column added as nullable; no foreign keys involved.

ALTER TABLE beer ADD COLUMN IF NOT EXISTS description VARCHAR(255);
