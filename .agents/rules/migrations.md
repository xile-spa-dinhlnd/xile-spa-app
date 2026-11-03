---
trigger: glob
globs: backend/src/main/resources/db/migration/**
---

# Flyway migrations

- A migration already on `develop` is immutable: never edit, rename or delete it (`AGENTS.md` rule 2). A hook blocks it.
- Schema changes go in a new `V{n}__description.sql` via the `/new-migration` skill.
- Money is `bigint`, timestamps `timestamptz`, business dates `date`. No hard deletes of business data.
- Financial tables need the closed-day trigger (`XL001`) like the existing ones; see `docs/design/erd.md` section 5.5.
- Update `docs/design/erd.md` and `backend/src/test/resources/sql/schema_smoke_test.sql` in the same PR.
