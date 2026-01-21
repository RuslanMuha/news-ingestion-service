--liquibase formatted sql

--changeset tispace:004-change-varchar-to-text
-- Change VARCHAR columns to TEXT to avoid length constraints

ALTER TABLE articles ALTER COLUMN title TYPE TEXT;
ALTER TABLE articles ALTER COLUMN author TYPE TEXT;
ALTER TABLE articles ALTER COLUMN category TYPE TEXT;

