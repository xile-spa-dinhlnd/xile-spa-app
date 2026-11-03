---
name: new-migration
description: Add a new Flyway migration (V{n}__description.sql) to Xile Spa with the right number and conventions, then update the entity, ERD and schema test. Use when a table, column, index, trigger or view must be added or changed, or when the user types /new-migration.
---

# /new-migration — change the schema with a new migration

Read first: `docs/design/erd.md` (section 2 conventions, section 5 design decisions) and the existing migrations in
`backend/src/main/resources/db/migration/`.

## Steps

1. **Next number.** Take the highest number in the folder **and** on develop:
   `git ls-tree --name-only origin/develop backend/src/main/resources/db/migration/`. New number = highest + 1. Remind
   the user to tell the other person this number (`CONTRIBUTING.md` section 4), since two branches can take the same one.
2. **Name** `V{n}__<english_snake_case_description>.sql`, for example `V3__service_group_sort_order.sql`.
3. **SQL** following V1's conventions:
   - Money `bigint` with `CHECK (... >= 0)` where it fits; timestamps `timestamptz`; business dates `date`.
   - Never `DROP` business data; deactivation is a status column (BR-09, BR-19).
   - Financial tables with `business_date`: the closed-day trigger like V1's tables (`XL001`).
   - Append-only tables (history, logs): trigger blocking `UPDATE`/`DELETE` (`XL002`).
   - Indexes on foreign keys and frequent filter columns; unique constraints named `ux_<table>_<column>`.
   - A header comment in English: what this migration is for, which card.
4. **Entity** updated to match (`ddl-auto: validate` fails on drift).
5. **Docs and schema test:**
   - `docs/design/erd.md`: tables, columns, new decisions (Vietnamese).
   - `backend/src/test/resources/sql/schema_smoke_test.sql`: checks for new constraints or triggers.
   - `docs/design/diagrams/` is generated: remind the user to rerun `docs/design/tools/gen_erd_mermaid.py` (ERD
     section 11); never edit it by hand.
6. **Run** (Docker on): `cd backend && ./gradlew test --tests 'com.xilespa.db.SchemaSmokeIT'`, then
   `./gradlew spotlessApply test`.

## Never

- Edit, rename or delete a migration already on develop (a hook blocks it).
- Set `spring.jpa.hibernate.ddl-auto` to `update` or `create`.
- Merge a migration without the code that uses it (`CONTRIBUTING.md` section 3).
