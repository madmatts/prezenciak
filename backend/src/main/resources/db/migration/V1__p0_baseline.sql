-- Wishka baseline (Phase 0).
-- citext: case-insensitive emails; pgcrypto: gen_random_uuid() and digest() for token hashes.
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

COMMENT ON SCHEMA public IS 'Wishka application schema. Changes only through Flyway migrations.';
