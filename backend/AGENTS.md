# backend/AGENTS.md — backend coding conventions

Read `../AGENTS.md` first; this file adds only backend specifics. Stack: Java 21, Spring Boot 4, Gradle (Kotlin DSL,
Gradle Wrapper), JPA, Spring Security, Flyway, PostgreSQL. Code, comments and test names in English; validation and
error messages shown to users in Vietnamese.

## Libraries

Source of truth: `gradle/libs.versions.toml` (libraries in the Spring Boot BOM have no version and move with the
`spring-boot` plugin). Not there = not available. Adding one: ask first per `../AGENTS.md` section 3. Versions below are
the major versions in use (`./gradlew dependencies`); changes update this table in the same PR (rule 11).

| Library | Version | Used for | API notes (docs and old examples often target the previous version) |
| --- | --- | --- | --- |
| Spring Boot (Framework 7, Security 7) | 4.1 | application base | Split starters: `spring-boot-starter-webmvc`, `-flyway`; test starters per slice (`-webmvc-test`, `-data-jpa-test`...) |
| Gradle, Kotlin DSL | 9.7 | build | Versions only in `libs.versions.toml`, never inline in `build.gradle.kts` |
| Spring Data JPA, Hibernate ORM | 7.4 | data access | `jakarta.persistence`; `ddl-auto` only `validate` or `none` (rule 2) |
| Jackson | 3.1 | JSON | Package `tools.jackson.databind`, not `com.fasterxml.jackson.databind` (seen: old examples target Jackson 2) |
| Flyway (+ `flyway-database-postgresql`) | 12 | migrations | `V{n}__...sql`, never edit existing files |
| PostgreSQL JDBC | BOM | driver | PostgreSQL 16 (dev and Testcontainers) |
| Spring Security OAuth2 JOSE (Nimbus JOSE JWT) | 10 | sign, verify JWT | `NimbusJwtEncoder`, `NimbusJwtDecoder` via `security/`; no jjwt, no hand-rolled HMAC |
| Bean Validation (`spring-boot-starter-validation`) | BOM | request validation | Vietnamese message on the annotation |
| Spring Boot Mail | BOM | password reset email | Bean chosen by `xile.mail.sender` (`log`, `smtp`) |
| Spring Boot Actuator | BOM | health check | Only `health` exposed |
| MapStruct (+ `lombok-mapstruct-binding`) | 1.6 | entity → DTO mappers | Four rules under "Structure"; Lombok runs before MapStruct |
| Lombok | BOM | getters, constructors | Limits in `lombok.config` (see "Structure") |
| JUnit Jupiter | 6 | tests | Spring Boot 4 brings JUnit 6; imports stay `org.junit.jupiter.api` |
| AssertJ, Mockito | 3.27, 5 | asserts, mocks | `assertThatThrownBy(...).extracting("errorCode")`; `@ExtendWith(MockitoExtension.class)` |
| Testcontainers | 2.0 | integration tests | Artifact `testcontainers-postgresql`, import `org.testcontainers.postgresql.PostgreSQLContainer` (1.x used `org.testcontainers.containers`) |
| Spotless (google-java-format, AOSP style) | 8 | formatting | `./gradlew spotlessApply` before committing |

## Structure

**Modules by feature at the top level, layers inside each module** (modular monolith): each module looks like classic
MVC inside, and one person owning a module rarely touches the other's files. Root package `com.xilespa`.

```
backend/
├─ build.gradle.kts, settings.gradle.kts, gradlew, gradlew.bat
├─ gradle/libs.versions.toml         # version catalog: every library version
└─ src/
   ├─ main/java/com/xilespa/
   │  ├─ XileSpaApplication.java
   │  ├─ config/                      # Spring config: Jackson, CORS, Clock (Asia/Ho_Chi_Minh), scheduling
   │  ├─ security/                    # SecurityConfig, JwtService, filter reading JWT from the cookie
   │  ├─ common/                      # shared, NO business logic
   │  │  ├─ mapper/                   # MapStructConfig shared by all mappers
   │  │  ├─ exception/                # ApiError, GlobalExceptionHandler, BusinessException, XL001/XL002/XL004 mapping
   │  │  ├─ audit/                    # WRITES the audit log: AuditService.record, AuditLog, AuditAction
   │  │  ├─ response/                 # PageResponse, PageRequests (overflow-safe paging)
   │  │  ├─ validation/               # @VisibleText, VisibleTextValidator (blocks invisible characters)
   │  │  └─ util/                     # MoneyUtils, DateUtils, TextNormalizer (Unicode and whitespace normalization)
   │  └─ module/                      # one folder per business module
   │     ├─ auth/  catalog/  customer/  visit/  expense/  closing/  dashboard/
   │     ├─ staff/  payroll/  importer/  system/  sync/
   ├─ main/resources/
   │  ├─ application.yml              # shared config, no secrets
   │  ├─ application-dev.yml
   │  ├─ application-prod.yml         # reads ${ENV_VARS} only
   │  └─ db/migration/                # V1__, V2__, ... (Flyway)
   └─ test/
      ├─ java/com/xilespa/            # same package tree as main; support/ = Testcontainers base, shared fixtures
      └─ resources/sql/schema_smoke_test.sql
```

Modules map to FR codes: `auth` FR-AUTH, `catalog` FR-SVC/FR-CMB, `customer` FR-CUS/FR-PKG, `visit` FR-VIS, `expense`
FR-EXP, `closing` FR-CLS (closing, adjustments, `lock_business_day`), `dashboard` FR-DSH (read-only), `staff` FR-STF,
`payroll` FR-PAY (R3), `importer` FR-IMP, `system` FR-SYS (audit log API, system config), `sync` FR-SYN (R2).

Every module follows this template (example `module/catalog`):

```
catalog/
├─ controller/   ServiceItemController.java       # REST: DTO in/out, @Valid, no logic
├─ service/      ServiceItemService.java          # business + @Transactional + audit; other modules call only this
│  └─ model/                                      # internal service records, never returned by the API
├─ repository/   ServiceItemRepository.java       # Spring Data JPA
├─ entity/       ServiceItem.java                 # JPA entity for table `service` (never named "Service", see below)
├─ enums/                                         # enums for entity columns, one per file
├─ dto/request/  ServiceItemRequest.java          # record + validation (@VisibleText, @PositiveOrZero...)
├─ dto/response/ ServiceItemResponse.java         # record
├─ mapper/       CatalogMapper.java               # entity → DTO with MapStruct (@Mapper interface)
├─ exception/                                     # module exceptions (extend BusinessException), if needed
└─ devdata/      CatalogDevData.java              # dev seed data for the module ("Dev seed data")
```

Create folders only when they get a file. Small modules (`dashboard`) may have only `controller/`, `service/`, `dto/`.

Rules:

- One-way calls: `controller → service → repository → entity`. Controllers never call repositories; services know
  nothing about HTTP (`HttpServletRequest`, `ResponseEntity`).
- **Module boundary:** module A uses only `service/` and `dto/` of module B, never its `repository/`, `entity/`,
  `mapper/`. Cross-module references are ids (`Long serviceId`), no `@ManyToOne` into another module. No cycles between
  modules. (A future `ArchitectureTest` with ArchUnit would enforce this; not added yet.)
- Controllers never return entities. DTO names: `Create...Request`, `Update...Request`, `...Response`. POST and PUT
  with the same fields share one request DTO (PUT replaces everything).
- **MapStruct**, four rules: (1) mappers **only copy**; no money, discount, revenue or status logic, not even in
  `@Mapping(expression = ...)` (BR-01, BR-04, BR-05); (2) every mapper uses `@Mapper(config = MapStructConfig.class)`
  (`componentModel = "spring"`, `unmappedTargetPolicy = ERROR`); (3) never map a request into sensitive entity fields
  (id, status, locked price, `created_by`): `@Mapping(target = "...", ignore = true)` and set them in the service;
  (4) complex mappings may be hand-written (`default` method or a class) inside `mapper/`.
- Never name an entity like a Spring annotation (`Service`, `Component`, `Repository`): table `service` → `ServiceItem`.
- Current user: `security/CurrentUser.id()` (empty for system tasks).
- Entity column enums in `module/<name>/enums/`, one per file, `@Enumerated(EnumType.STRING)`.
- Internal service results that must not go to the API (for example containing a token) live in `service/model/`,
  named `...Result`.
- **Lombok** (limited, enforced by `backend/lombok.config`): entities only `@Getter` and
  `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, state changes through named business methods (`deactivate()`,
  `changePrice(...)`), no setters; services, controllers, components use `@RequiredArgsConstructor`. **Banned:**
  `@Data`, `@Setter`, `@Builder`, `@AllArgsConstructor`; no `@EqualsAndHashCode`, `@ToString` on entities. DTOs are records.
- Entity timestamps come from `Clock` (service passes `Instant now`); never `Instant.now()` in an entity.
- `@Transactional` on services only. Records that must survive a failed request (failed-login counter, session
  revocation) run in a separate service with `Propagation.REQUIRES_NEW` (example `AuthSecurityAuditService`).
- **Audit log (BR-18):** every service changing financial or catalog data calls
  `AuditService.record(action, entityType, entityId, before, after)` **in the same transaction** (`Propagation.MANDATORY`
  fails outside one). `entityType` is the table name; `action` from `AuditAction`; `before`/`after` are DTOs, records or
  maps, **never JPA entities** (sensitive fields are masked anyway, but do not pass them). The log is append-only and
  read via `GET /api/audit-logs` (`module/system`, `AuditLogQueryService`).
- URLs: `/api/{module}/{resource}`, plural nouns, kebab-case (`/api/catalog/services`, `/api/visits`). Business
  actions are `POST` on a sub-resource: `POST /api/catalog/services/{id}/deactivate`, `POST /api/closings/{date}`.
- `common`, `config`, `security` depend on no business module; no business logic in `common`.
- New module: create it under `module/` with the template, update the tree here and the feature list in
  `frontend/AGENTS.md`.

## Database

- JVM runs in UTC (`XileSpaApplication.main`, test task). Business timezone `Asia/Ho_Chi_Minh` only via `Clock`
  (`config/TimeConfig`); never `ZoneId.systemDefault()`, `LocalDate.now()` or `Instant.now()` without `Clock`.
- DB constraints and triggers are the last line of defense; never code around them, and still check first in services.

Triggers raise these SQLSTATEs; `common/exception/DatabaseErrors` (called by `GlobalExceptionHandler` for
`DataAccessException` and `TransactionSystemException`) translates them (`docs/design/erd.md` section 5.5):

| SQLSTATE | Meaning                                                  | `code`               | HTTP |
| -------- | -------------------------------------------------------- | -------------------- | ---- |
| `XL001`  | Closed day: no insert, update, void (adjustments too)    | `DAY_CLOSED`         | 409  |
| `XL002`  | Append-only table, or hard delete forbidden              | `RECORD_IMMUTABLE`   | 409  |
| `XL004`  | Closing a future day                                     | `FUTURE_DAY_CLOSING` | 400  |

Other DB errors stay `INTERNAL_ERROR` (500, logged).

## Errors

Shared error format, used by every API and parsed by the frontend:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu không hợp lệ, vui lòng kiểm tra lại.",
  "fieldErrors": [{ "field": "price", "message": "Giá không được âm" }],
  "path": "/api/catalog/services",
  "timestamp": "2026-10-06T22:08:14.894+07:00"
}
```

- `code` comes from `ErrorCode` (`common/exception`); the frontend switches on `code` and shows `message`. New business
  errors add a code; never free-text errors. Standard codes: `VALIDATION_FAILED` (400), `BAD_REQUEST` (400),
  `NOT_ACCEPTABLE` (406), `UNSUPPORTED_MEDIA_TYPE` (415).
- Throw `BusinessException` (or a module subclass); never build error `ResponseEntity`s in controllers.
- Validation messages in Vietnamese on the annotation. Name fields use `@VisibleText(message = "...")` instead of
  `@NotBlank` to block invisible characters.
- A 500 never returns technical details; details go to the log only.

## Closing and concurrency

- Writing a visit and closing the same day use `lock_business_day(d)` inside the transaction (ERD 5.5).
- Google Sheets sync runs in the background, idempotent, only for `CLOSED` days (BR-15).

## Security

Assets, threats (`T-xx`), attack surface, secrets and the per-feature checklist: `docs/security/`.

- Passwords hashed with Argon2 or BCrypt. Never log passwords, tokens, refresh tokens.
- Refresh tokens stored **only as SHA-256 hashes** (`security/TokenHasher`). Access and refresh tokens live in
  `HttpOnly`, `SameSite=Strict`, `Secure` (prod) cookies.
- Every endpoint except login and forgot-password requires authentication; unauthenticated → 401.
- Wrong-login responses never reveal whether the email exists, timing included: an unknown email still runs bcrypt
  against a dummy hash.
- **BCrypt DoS (NFR-SEC-05):** hashing costs ~300 ms; rate-limit per IP at the reverse proxy (Caddy, card D-05) or a
  Spring filter before BCrypt.
- **CSRF:** no CSRF token (`SecurityConfig` disables it); `SameSite=Strict` cookies protect because frontend and API are
  same-site (Vite proxy in dev, same domain in prod). Revisit if they ever split domains.
- **Data-changing APIs never use `GET`.** Secrets come from environment variables per profile; never committed.

## Configuration

- Behavior per environment via properties in `application-{profile}.yml` and `@ConditionalOnProperty`; no negated
  `@Profile` (such as `@Profile("!prod")`). `@Profile` only for dev- or test-only beans.
- Defaults live once in `application.yml`; `application-prod.yml` only reads environment variables, no defaults.
- Tests use the `test` profile layered on `application.yml`; never create `src/test/resources/application.yml` or copy
  `spring.*` keys into test files.

## Dev seed data

Lets you try features without creating data by hand. Enabled by `xile.dev-data.enabled` (`false` in `application.yml`,
`true` in `application-dev.yml`; `XILE_DEV_DATA=false` disables). To start over, wipe the dev DB (`README.md`, "Bật
database") and restart.

- **Never put seed data in Flyway migrations** (they run in production). `V2` holds only reference catalogs.
- **One class per module** `module/<name>/devdata/<Name>DevData.java` (`ApplicationRunner`,
  `@ConditionalOnProperty(name = "xile.dev-data.enabled", havingValue = "true")`). Add records to your own module's
  class; never edit another module's class or an old migration.
- **Call the module's service, never its repository**, so seed data passes validation, business rules and audit.
- **Idempotent**: skip records whose business key exists, so new seed rows only need a restart.
- **Order** with `@Order`: `10` staff, `20` catalog, `30` customers, `40` visits, `50` expenses and closing.
- **Fake data only** (NFR-PRV-01); include edge cases for the UI: inactive records, price 0, combo dearer than its parts.
- **Test** like a service test with Mockito (model `CatalogDevDataTest`); never run seed data in integration tests.

## Test

- Integration tests use **Testcontainers with real PostgreSQL**, never H2. Money, closing, adjustments always tested.
  Schema checks: `src/test/resources/sql/schema_smoke_test.sql`.
- Cover the groups of `AGENTS.md` section 10, by layer:
  - **Service:** Mockito unit tests (`@ExtendWith(MockitoExtension.class)`), real mapper via `Mappers.getMapper(...)`,
    `Clock.fixed(...)`; one test per branch; error cases check `ErrorCode` plus
    `verify(repository, never()).save(any())` and `verify(auditService, never())...`. Model: `StaffServiceTest`.
  - **Controller:** `...ControllerIT` with `@IntegrationTest`: HTTP status, JSON (`jsonPath`), `fieldErrors`, 401 when
    logged out, real data and `audit_log` in the DB; own test user, random data prefix. Model: `StaffControllerIT`.
  - **Entity:** plain unit tests for business methods. **Triggers, DB constraints:** `schema_smoke_test.sql`.
- Shared PostgreSQL per run: **never `deleteAll()` shared tables** (append-only tables reference them); each test uses
  its own prefix and cleans only its own data; never count the whole `audit_log` (use a random `entityType`).
- Tests live in the same package as the class: `ServiceServiceTest` (unit), `ServiceControllerIT` (integration).
- AssertJ (`assertThat`, `assertThatThrownBy(...).extracting("errorCode")`); money written `150_000L`; repeated input
  shapes use `@ParameterizedTest`.
- Probe odd input with real requests in `...ControllerIT`, never infer from annotations: on 2026-10-25 Jackson turned
  `1.7` into `1` for a `Long` money field, `@NotBlank` accepted an NBSP-only name, `null` in a list caused 500, all with
  green tests (S3-08). Model: `InputHandlingIT`.
- `@IntegrationTest` (`src/test/java/com/xilespa/support`) boots the app with Postgres 16 and MockMvc; needs **Docker
  running** — mass connection failures mean Docker is off, not broken code.
- Run (in `backend/`, JDK 21): `./gradlew spotlessApply test`; one class or method:
  `./gradlew test --tests 'com.xilespa.module.staff.service.StaffServiceTest[.method]'` (wildcards allowed). Fixing a
  bug: run the red test alone first, then the whole suite. App: `./gradlew bootRun` (profile `dev`).
- Format: Spotless (google-java-format AOSP, 4-space indent); run `spotlessApply` before committing.
