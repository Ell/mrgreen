--liquibase formatted sql
--changeset mrgreen:002-remembered-entries

CREATE TABLE remembered_entries (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    "key"      TEXT    NOT NULL,
    "value"    TEXT    NOT NULL,
    guild_id   TEXT    NOT NULL,
    created_by TEXT    NOT NULL,
    created_at TEXT    NOT NULL
);

CREATE INDEX idx_remembered_entries_guild_key ON remembered_entries (guild_id, key);
