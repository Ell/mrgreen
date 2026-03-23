--liquibase formatted sql
--changeset mrgreen:003-fix-created-at-format

UPDATE remembered_entries
SET created_at = strftime('%Y-%m-%dT%H:%M:%SZ', created_at / 1000, 'unixepoch')
WHERE created_at GLOB '[0-9]*';
