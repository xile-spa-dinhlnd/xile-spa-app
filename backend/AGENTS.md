# backend/AGENTS.md — quy tắc riêng cho backend

Đọc `../AGENTS.md` trước. File này chỉ bổ sung phần riêng của backend.
Stack: Java 21, Spring Boot 4, Gradle (Kotlin DSL, dùng Gradle Wrapper), JPA, Spring Security, Flyway, PostgreSQL.

## Thư viện

Nguồn sự thật: `gradle/libs.versions.toml` (thư viện có trong BOM của Spring Boot thì không ghi version, nâng cùng
plugin `spring-boot`). Không có trong file đó là chưa có. Thêm thư viện: hỏi trước theo `../AGENTS.md` mục 3. Bản ghi
ở đây là bản chính đang chạy (`./gradlew dependencies`), đổi thì sửa bảng trong cùng PR (quy tắc 11).

| Thư viện | Bản | Dùng cho | Lưu ý API (tài liệu, ví dụ cũ hay viết theo bản trước) |
| --- | --- | --- | --- |
| Spring Boot (Framework 7, Security 7) | 4.1 | nền ứng dụng | Starter tách nhỏ: `spring-boot-starter-webmvc`, `-flyway`; test starter theo phần (`-webmvc-test`, `-data-jpa-test`...) |
| Gradle, Kotlin DSL | 9.7 | build | Version thư viện chỉ khai ở `libs.versions.toml`, không viết thẳng trong `build.gradle.kts` |
| Spring Data JPA, Hibernate ORM | 7.4 | truy cập dữ liệu | `jakarta.persistence`; `ddl-auto` chỉ `validate` hoặc `none` (quy tắc 2) |
| Jackson | 3.1 | JSON | Package `tools.jackson.databind`, không `com.fasterxml.jackson.databind` (mục 7 của `../AGENTS.md`) |
| Flyway (+ `flyway-database-postgresql`) | 12 | migration | `V{n}__...sql`, không sửa file đã có |
| PostgreSQL JDBC | BOM | driver | Database PostgreSQL 16 (dev và Testcontainers) |
| Spring Security OAuth2 JOSE (Nimbus JOSE JWT) | 10 | ký, kiểm JWT | Dùng `NimbusJwtEncoder`, `NimbusJwtDecoder` qua `security/`; không thêm jjwt hay tự viết HMAC |
| Bean Validation (`spring-boot-starter-validation`) | BOM | kiểm dữ liệu request | Thông báo tiếng Việt ngay trên annotation |
| Spring Boot Mail | BOM | email đặt lại mật khẩu | Chọn bean theo `xile.mail.sender` (`log`, `smtp`) |
| Spring Boot Actuator | BOM | health check | Chỉ mở `health` |
| MapStruct (+ `lombok-mapstruct-binding`) | 1.6 | mapper entity → DTO | Bốn quy tắc ở mục Cấu trúc; Lombok chạy trước MapStruct |
| Lombok | BOM | getter, constructor | Giới hạn trong `lombok.config` (mục Cấu trúc) |
| JUnit Jupiter | 6 | test | Spring Boot 4 kéo JUnit 6; import vẫn `org.junit.jupiter.api` |
| AssertJ, Mockito | 3.27, 5 | assert, mock | `assertThatThrownBy(...).extracting("errorCode")`; `@ExtendWith(MockitoExtension.class)` |
| Testcontainers | 2.0 | test tích hợp | Artifact `testcontainers-postgresql`, import `org.testcontainers.postgresql.PostgreSQLContainer` (bản 1.x là `org.testcontainers.containers`) |
| Spotless (google-java-format, kiểu AOSP) | 8 | format | `./gradlew spotlessApply` trước khi commit |

## Cấu trúc

**Chia module theo tính năng ở cấp ngoài, chia tầng bên trong mỗi module** (controller, service,
repository, entity, dto...). Đây là kiểu modular monolith phổ biến trong dự án Spring thực tế: bên trong
mỗi module vẫn quen thuộc như MVC truyền thống, nhưng mỗi người làm trọn một module thì ít đụng file của
nhau và sau này dễ tách. Tên package gốc: `com.xilespa` (chốt ở S0-04).

```
backend/
├─ build.gradle.kts, settings.gradle.kts, gradlew, gradlew.bat
├─ gradle/
│  ├─ wrapper/
│  └─ libs.versions.toml              # version catalog: mọi phiên bản thư viện ở đây
├─ Dockerfile                          # thêm ở giai đoạn DevOps (D-02)
└─ src/
   ├─ main/java/com/xilespa/
   │  ├─ XileSpaApplication.java
   │  ├─ config/                      # cấu hình Spring: Jackson, CORS, Clock (Asia/Ho_Chi_Minh), scheduling
   │  ├─ security/                    # SecurityConfig, JwtService, filter đọc JWT từ cookie
   │  ├─ common/                      # dùng chung, KHÔNG chứa nghiệp vụ
   │  │  ├─ mapper/                   # MapStructConfig dùng chung cho mọi mapper
   │  │  ├─ exception/                # ApiError, GlobalExceptionHandler, BusinessException,
   │  │  │                            #   ánh xạ XL001/XL002/XL004 (S0-07)
   │  │  ├─ audit/                    # GHI nhật ký: AuditService.record, AuditLog, AuditLogRepository, AuditAction
   │  │  ├─ response/                 # PageResponse: định dạng chung cho API danh sách có phân trang
   │  │  └─ util/                     # MoneyUtils, DateUtils (không tính nghiệp vụ)
   │  └─ module/                      # mỗi thư mục con là một module nghiệp vụ
   │     ├─ auth/                     # FR-AUTH: đăng nhập, phiên, tài khoản
   │     ├─ catalog/                  # FR-SVC, FR-CMB: dịch vụ, nhóm dịch vụ, combo
   │     ├─ customer/                 # FR-CUS, FR-PKG: khách hàng, gói liệu trình
   │     ├─ visit/                    # FR-VIS: lượt khách, visit_item
   │     ├─ expense/                  # FR-EXP: chi phí
   │     ├─ closing/                  # FR-CLS: chốt sổ, bút toán điều chỉnh, lock_business_day
   │     ├─ dashboard/                # FR-DSH: chỉ đọc (view, truy vấn tổng hợp)
   │     ├─ staff/                    # FR-STF: nhân viên
   │     ├─ payroll/                  # FR-PAY: tiền tour, lương (R3)
   │     ├─ importer/                 # FR-IMP: nhập Excel
   │     ├─ system/                   # FR-SYS: API xem nhật ký thay đổi (đọc từ common/audit), cấu hình hệ thống
   │     └─ sync/                     # FR-SYN: đồng bộ Google Sheets (R2)
   ├─ main/resources/
   │  ├─ application.yml              # cấu hình chung, không có bí mật
   │  ├─ application-dev.yml
   │  ├─ application-prod.yml         # chỉ đọc ${BIẾN_MÔI_TRƯỜNG}
   │  └─ db/migration/                # V1__, V2__, ... (Flyway)
   └─ test/
      ├─ java/com/xilespa/            # cùng cây package với main
      │  ├─ support/                  # lớp nền Testcontainers, dữ liệu mẫu dùng chung
      │  └─ ArchitectureTest.java     # (chưa có) kiểm tra ranh giới module, xem quy tắc bên dưới
      └─ resources/sql/schema_smoke_test.sql
```

Một module bên trong (ví dụ `module/catalog`), **mọi module đều theo đúng khuôn này**:

```
catalog/
├─ controller/
│  ├─ ServiceItemController.java      # REST: nhận/trả DTO, @Valid, không có logic
│  └─ ServiceGroupController.java
├─ service/
│  ├─ ServiceItemService.java         # nghiệp vụ + @Transactional + ghi audit; module khác chỉ gọi vào đây
│  ├─ ServiceGroupService.java
│  └─ model/                          # kiểu dữ liệu nội bộ của service (record), không trả ra API
├─ repository/
│  ├─ ServiceItemRepository.java      # Spring Data JPA
│  ├─ ServiceGroupRepository.java
│  └─ ServicePriceHistoryRepository.java
├─ entity/
│  ├─ ServiceItem.java                # entity JPA của bảng service (không đặt tên "Service", xem dưới)
│  ├─ ServiceGroup.java
│  └─ ServicePriceHistory.java
├─ enums/                             # enum dùng cho cột của entity, mỗi enum một file
├─ dto/
│  ├─ request/
│  │  ├─ ServiceItemRequest.java      # record + annotation kiểm tra (@NotBlank, @PositiveOrZero...)
│  │  └─ ServiceGroupRequest.java
│  └─ response/
│     ├─ ServiceItemResponse.java     # record
│     └─ ServiceGroupResponse.java
├─ mapper/
│  └─ CatalogMapper.java              # entity → DTO bằng MapStruct (interface @Mapper)
├─ exception/                         # exception riêng của module (kế thừa BusinessException), nếu cần
└─ devdata/
   └─ CatalogDevData.java             # dữ liệu mẫu cho máy dev của module (mục "Dữ liệu mẫu cho máy dev")
```

Thư mục nào chưa có file thì chưa tạo. Module nhỏ (như `dashboard`) có thể không có `entity/`, `repository/`
mà chỉ có `controller/`, `service/`, `dto/`.

Luồng gọi và quy tắc:

- Chiều gọi một chiều: `controller → service → repository → entity`. Controller không gọi repository,
  service không biết gì về HTTP (`HttpServletRequest`, `ResponseEntity`).
- **Ranh giới module:** module A chỉ được dùng `service/` và `dto/` của module B. **Không** import
  `repository/`, `entity/`, `mapper/` của module khác. Giữa hai module tham chiếu bằng id (`Long serviceId`),
  không `@ManyToOne` sang entity của module khác. `ArchitectureTest` kiểm tra tự động quy tắc này
  (đề xuất dùng ArchUnit, chỉ ở phạm vi test; chưa thêm, khi thêm thì ghi trong PR theo ADR-0001).
- Không để hai module gọi vòng tròn lẫn nhau. Nếu cần, tách phần chung ra hoặc đổi chiều phụ thuộc.
- Controller không trả entity ra ngoài; luôn trả DTO. Tên DTO: `Create...Request`, `Update...Request`,
  `...Response`. Chuyển đổi đặt ở `mapper/`, không rải trong controller.
- Mapper dùng **MapStruct**, theo bốn quy tắc:
  1. Mapper **chỉ chép dữ liệu**. Không tính tiền, giảm giá, doanh thu, trạng thái trong mapper (kể cả trong
     `@Mapping(expression = ...)`); những việc đó ở service (BR-01, BR-04, BR-05).
  2. Mọi mapper dùng `@Mapper(config = MapStructConfig.class)` (`common/mapper/`): `componentModel = "spring"`,
     `unmappedTargetPolicy = ERROR` để build báo lỗi khi quên một trường.
  3. Không map từ Request vào các trường nhạy cảm của entity (id, trạng thái, giá đã chốt, `created_by`...):
     đánh dấu `@Mapping(target = "...", ignore = true)` rồi gán trong service.
  4. Mapping quá phức tạp thì được viết tay (`default` method hoặc lớp riêng) trong cùng `mapper/`.
- Không đặt tên entity trùng annotation của Spring (`Service`, `Component`, `Repository`...): ví dụ bảng
  `service` ánh xạ entity `ServiceItem`.
- POST và PUT nhận cùng các trường thì dùng chung một request DTO (`ServiceItemRequest`); PUT là thay thế
  toàn bộ. Khác trường thì tách `Create...Request` / `Update...Request`.
- Người đang đăng nhập: `security/CurrentUser.id()` (rỗng nếu là tác vụ hệ thống).
- Enum dùng cho cột của entity đặt ở `module/<tên>/enums/`, mỗi enum một file; lưu bằng
  `@Enumerated(EnumType.STRING)`.
- Kết quả nội bộ của service mà không được trả thẳng ra API (ví dụ chứa token) đặt ở `service/model/`,
  tên `...Result`, không đặt trong `dto/`.
- **Lombok** (dùng có giới hạn, cấu hình trong `backend/lombok.config`):
  - Entity: chỉ `@Getter` và `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. Thay đổi trạng thái qua
    method nghiệp vụ có tên rõ nghĩa (`deactivate()`, `changePrice(...)`, `recordFailedLogin(...)`), không setter.
  - Service, controller, component: `@RequiredArgsConstructor` cho các trường `final`.
  - **Cấm** `@Data`, `@Setter`, `@Builder`, `@AllArgsConstructor` (`lombok.config` báo lỗi khi build);
    không dùng `@EqualsAndHashCode`, `@ToString` trên entity. DTO vẫn dùng `record`.
- Thời điểm tạo, sửa trong entity lấy từ `Clock` (truyền `Instant now` từ service), không gọi
  `Instant.now()` trong entity.
- `@Transactional` đặt ở service, không ở controller hay repository. Ghi nhận cần giữ lại kể cả khi request
  kết thúc bằng lỗi (đếm đăng nhập sai, thu hồi phiên...) chạy trong service riêng với
  `Propagation.REQUIRES_NEW` (ví dụ `AuthSecurityAuditService`).
- **Ghi nhật ký thay đổi (BR-18):** mọi service đổi dữ liệu tài chính hoặc danh mục gọi
  `AuditService.record(action, entityType, entityId, before, after)` (`common/audit`), **trong cùng
  transaction** với thay đổi gốc (hàm dùng `Propagation.MANDATORY`, gọi ngoài transaction sẽ báo lỗi).
  - `entityType` là tên bảng (`service`, `combo`, `visit`...); `action` lấy từ `AuditAction` (thêm giá trị khi cần).
  - `before`, `after` truyền DTO, record hoặc `Map`, **không truyền entity JPA**. Trường nhạy cảm (mật khẩu,
    token) tự được che; vẫn không cố ý đưa chúng vào.
  - Người thực hiện lấy từ phiên đăng nhập; tác vụ hệ thống (không có người đăng nhập) để trống.
  - Xem nhật ký: `GET /api/audit-logs` ở `module/system` (đọc qua `AuditLogQueryService`).
  - Nhật ký chỉ thêm: không có API sửa hay xóa. Test không dọn được bảng này, nên test dùng `entityType`
    riêng (ví dụ ngẫu nhiên) thay vì đếm toàn bảng.
- URL: `/api/{module}/{tài-nguyên}`, danh từ số nhiều, kebab-case (`/api/catalog/services`,
  `/api/visits`). Hành động nghiệp vụ dùng `POST` lên tài nguyên con:
  `POST /api/catalog/services/{id}/deactivate`, `POST /api/closings/{date}`.
- `common`, `config`, `security` không phụ thuộc vào module nghiệp vụ nào. Không để logic nghiệp vụ vào
  `common`.
- Module mới: tạo thư mục trong `module/` theo đúng khuôn trên, cập nhật cây thư mục ở file này và tên
  feature tương ứng ở `frontend/AGENTS.md`.
- Test tích hợp dùng chung một PostgreSQL trong cả lượt chạy: **không `deleteAll()` các bảng dùng chung**
  (app_user, service...), vì bảng chỉ thêm (audit_log, lịch sử giá) có thể tham chiếu tới chúng. Mỗi test tạo
  dữ liệu có tên hoặc tiền tố riêng (ví dụ ngẫu nhiên) và chỉ dọn dữ liệu của chính nó.
- Test đặt cùng package với lớp được test: `ServiceServiceTest` (đơn vị, Mockito), `ServiceControllerIT`
  (tích hợp, Testcontainers + MockMvc).

## Cơ sở dữ liệu

- Schema chỉ đổi bằng Flyway (`src/main/resources/db/migration/`). **Không sửa `V1`, `V2`**; thêm `V3__...`.
- `spring.jpa.hibernate.ddl-auto` luôn là `validate` hoặc `none`, không bao giờ `update`/`create`.
- Múi giờ: JVM chạy với múi giờ mặc định UTC (`XileSpaApplication.main`, task test). Múi giờ nghiệp vụ
  `Asia/Ho_Chi_Minh` chỉ lấy từ `Clock` (`config/TimeConfig`); không dùng `ZoneId.systemDefault()`,
  `LocalDate.now()` hay `Instant.now()` không qua `Clock`.
- Tiền: `long` (Java) / `bigint` (SQL). Không `BigDecimal` cho số tiền đồng, không `double`.
- Các ràng buộc và trigger ở DB là rào chắn cuối. Không viết code vòng qua chúng.

## Lỗi từ cơ sở dữ liệu

Trigger ném các mã SQLSTATE sau; backend bắt và dịch thành lỗi nghiệp vụ có thông báo tiếng Việt (S0-07, `docs/design/erd.md` mục 5.5):

| SQLSTATE | Ý nghĩa                                                         | `code` trả về        | HTTP |
| -------- | --------------------------------------------------------------- | -------------------- | ---- |
| `XL001`  | Ngày đã chốt: không được thêm, sửa, hủy (kể cả bút toán vào ngày đó) | `DAY_CLOSED`         | 409  |
| `XL002`  | Bảng chỉ thêm (append-only), hoặc cấm xóa cứng                  | `RECORD_IMMUTABLE`   | 409  |
| `XL004`  | Không chốt sổ ngày tương lai                                    | `FUTURE_DAY_CLOSING` | 400  |

Việc dịch nằm ở `common/exception/DatabaseErrors`, được `GlobalExceptionHandler` gọi cho
`DataAccessException` và `TransactionSystemException`. Lỗi cơ sở dữ liệu khác vẫn là `INTERNAL_ERROR` (500,
có ghi log). Trigger là rào chắn cuối: service vẫn nên tự kiểm tra trước (ví dụ ngày đã chốt chưa).

Định dạng lỗi chung (S0-04), mọi API dùng lại, frontend dựa vào đây (S0-08):

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu không hợp lệ, vui lòng kiểm tra lại.",
  "fieldErrors": [{ "field": "price", "message": "Giá không được âm" }],
  "path": "/api/catalog/services",
  "timestamp": "2026-10-06T22:08:14.894+07:00"
}
```

- `code` lấy từ enum `ErrorCode` (`common/exception`); frontend xử lý theo `code`, hiển thị `message`.
  Lỗi nghiệp vụ mới thì thêm mã vào `ErrorCode`, không trả chuỗi tự do.
- Ném lỗi nghiệp vụ bằng `BusinessException` (hoặc lớp con trong `exception/` của module), không tự dựng
  `ResponseEntity` lỗi trong controller.
- Thông báo lỗi validation viết tiếng Việt ngay trên annotation: `@NotBlank(message = "Tên không được để trống")`.
- Lỗi 500 không bao giờ trả chi tiết kỹ thuật; chi tiết chỉ ghi vào log.

## Chốt sổ và đồng thời

- Ghi giao dịch và chốt sổ cùng ngày dùng `lock_business_day(d)` trong transaction (xem ERD 5.5).
- Đồng bộ Google Sheets chạy nền, idempotent, chỉ cho ngày `CLOSED` (BR-15).

## Bảo mật

Tài sản, mối đe dọa (`T-xx`), bề mặt tấn công, bí mật và checklist cho mỗi tính năng: `docs/security/`.

- Mật khẩu băm Argon2 hoặc BCrypt. Không log mật khẩu, token, refresh token.
- Refresh token **chỉ lưu bản băm** SHA-256 (`security/TokenHasher`). Access token và refresh token nằm
  trong cookie `HttpOnly`, `SameSite=Strict`, `Secure` (bật ở prod).
- Mọi endpoint ngoài đăng nhập/quên mật khẩu đều yêu cầu xác thực; chưa đăng nhập trả 401.
- Thông báo sai đăng nhập không tiết lộ email có tồn tại hay không, kể cả qua thời gian phản hồi: khi
  không tìm thấy email vẫn chạy bcrypt với một hash giả.
- **Chống DoS do BCrypt (NFR-SEC-05):** Do việc chạy hash giả (và hash thật) tốn CPU (~300ms/lần), nguy cơ
  DoS cạn kiệt tài nguyên VPS cần được ngăn chặn bằng Rate Limiting (giới hạn số request/phút theo IP) ở reverse
  proxy (Caddy, card D-05) hoặc Spring filter trước khi chạm tới BCrypt.
- **CSRF:** không dùng CSRF token (`SecurityConfig` tắt `csrf`). Chống CSRF bằng cookie `SameSite=Strict`, vì
  frontend và API cùng một site (khi dev qua proxy của Vite, khi chạy thật cùng tên miền). Nếu sau này
  frontend và API khác tên miền gốc thì phải xem lại quyết định này.
- **API thay đổi dữ liệu không bao giờ dùng `GET`**, chỉ dùng `POST`, `PUT`, `PATCH`, `DELETE`.
- Bí mật đọc từ biến môi trường theo profile `dev`/`prod`; không commit.

## Cấu hình

- Chọn hành vi theo môi trường bằng thuộc tính trong `application-{profile}.yml` và `@ConditionalOnProperty`; không dùng `@Profile` phủ định (như `@Profile("!prod")`).
- `@Profile` chỉ dùng cho bean dành riêng dev hoặc test. Dữ liệu mẫu dev bật bằng thuộc tính `xile.dev-data.enabled`,
  không bằng `@Profile` (mục dưới).
- Giá trị mặc định đặt một chỗ trong `application.yml`; `application-prod.yml` chỉ đọc biến môi trường, không có mặc định.

## Dữ liệu mẫu cho máy dev

Để thử tính năng mà không phải tạo tay qua Postman. Bật bằng `xile.dev-data.enabled` (mặc định `false` ở
`application.yml`, `true` ở `application-dev.yml`; tắt bằng `XILE_DEV_DATA=false`). Muốn làm lại từ đầu thì xóa sạch
database dev (lệnh ở `README.md` mục "Bật database") rồi chạy lại backend.

- **Không bao giờ để dữ liệu mẫu trong migration Flyway**: migration chạy cả ở production. `V2` chỉ chứa danh mục gốc.
- **Mỗi module một lớp** `module/<tên>/devdata/<Tên>DevData.java` (`ApplicationRunner`,
  `@ConditionalOnProperty(name = "xile.dev-data.enabled", havingValue = "true")`). Tính năng mới thêm bản ghi vào lớp
  của chính module mình; module mới thì tạo lớp mới. **Không sửa lớp của module khác, không sửa migration cũ.**
- **Gọi service của module, không gọi repository**, để dữ liệu mẫu qua đủ validation, quy tắc nghiệp vụ và audit.
  Module sau cần bản ghi của module trước (giao dịch cần nhân viên, dịch vụ) thì đọc qua service của module đó.
- **Chạy lại không tạo trùng**: bản ghi đã có cùng tên (khóa nghiệp vụ) thì bỏ qua; nhờ vậy thêm bản ghi mẫu mới chỉ
  cần chạy lại backend, không phải xóa database.
- **Thứ tự** bằng `@Order`: `10` nhân viên, `20` danh mục (dịch vụ, combo), `30` khách hàng, `40` giao dịch, `50` chi
  phí, chốt sổ. Module mới chọn số theo phụ thuộc và ghi vào danh sách này.
- **Chỉ dữ liệu giả** (tên, SĐT, ghi chú không phải của khách thật, NFR-PRV-01). Nên có sẵn ca biên để thử giao diện:
  bản ghi ngừng dùng, giá 0, combo đắt hơn giá lẻ...
- **Test** bằng Mockito như test service (mẫu `CatalogDevDataTest`): tạo đủ khi trống, không tạo lại bản ghi đã có.
  Không chạy dữ liệu mẫu trong test tích hợp (`src/test/resources/application.yml` không bật).

## Test

- Test tích hợp dùng **Testcontainers với PostgreSQL thật**, không dùng H2 (trigger và kiểu dữ liệu của Postgres khác H2).
- Phần tiền, chốt sổ, bút toán luôn có test. Bộ test schema: `src/test/resources/sql/schema_smoke_test.sql`.
- Đủ các nhóm ca theo `AGENTS.md` mục 10. Phân tầng:
  - **Service:** test đơn vị Mockito (`@ExtendWith(MockitoExtension.class)`), mapper thật qua
    `Mappers.getMapper(...)`, `Clock.fixed(...)`. Mỗi nhánh nghiệp vụ một test; ca lỗi kiểm tra `ErrorCode` và
    `verify(repository, never()).save(any())`, `verify(auditService, never())...`. Mẫu: `StaffServiceTest`.
  - **Controller:** `...ControllerIT` với `@IntegrationTest`: mã HTTP, JSON trả về (`jsonPath`), `fieldErrors`,
    401 khi chưa đăng nhập, dữ liệu và `audit_log` thật trong DB. Người dùng test tạo riêng, dữ liệu có tiền tố
    ngẫu nhiên (`prefix`). Mẫu: `StaffControllerIT`.
  - **Entity:** test đơn vị cho method nghiệp vụ (`deactivate()`, `changePrice(...)`) không cần Spring.
  - **Trigger, ràng buộc DB:** thêm vào `schema_smoke_test.sql`.
- Assert bằng AssertJ (`assertThat`, `assertThatThrownBy(...).extracting("errorCode")`). Tiền viết `150_000L`.
- Ca nhiều bộ dữ liệu cùng một kiểu (các chuỗi không hợp lệ, các mức tiền biên) dùng `@ParameterizedTest`.
- Test tích hợp gắn `@IntegrationTest` (`src/test/java/com/xilespa/support`): chạy cả ứng dụng với Postgres 16
  thật và MockMvc. Cần **Docker Desktop đang chạy**.
- Chạy (trong `backend/`, cần JDK 21): `./gradlew spotlessApply test`. Chạy ứng dụng: `./gradlew bootRun`
  (profile `dev`, cần Postgres ở máy, card S0-05).
- Chạy riêng một lớp hoặc một method test: `./gradlew test --tests 'com.xilespa.module.staff.service.StaffServiceTest'`
  (thêm `.tenMethod` cho một method; dùng `*` được, ví dụ `'*StaffControllerIT'`). Sửa lỗi thì chạy riêng test đỏ
  trước, xanh rồi mới chạy cả bộ.
- Format: Spotless (google-java-format kiểu AOSP, thụt 4 khoảng). Chạy `spotlessApply` trước khi commit.
- Spring Boot 4 dùng Jackson 3: import `tools.jackson.databind...`, không phải `com.fasterxml.jackson.databind...`.
