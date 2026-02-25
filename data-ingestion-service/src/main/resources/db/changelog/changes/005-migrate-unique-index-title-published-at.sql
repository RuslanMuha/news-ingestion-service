--liquibase formatted sql

--changeset tispace:005-migrate-unique-index-title-published-at
DROP INDEX IF EXISTS uk_articles_title;
CREATE UNIQUE INDEX IF NOT EXISTS uk_articles_title_published_at ON articles(title, published_at);

