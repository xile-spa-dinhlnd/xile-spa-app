# backend/AGENTS.md — quy tắc riêng cho backend

Đọc `../AGENTS.md` trước. File này chỉ bổ sung phần riêng của backend.
Stack: Java 21, Spring Boot 4, Gradle (Kotlin DSL, dùng Gradle Wrapper), JPA, Spring Security, Flyway, PostgreSQL.

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
   │  │  ├─ audit/                    # AuditService ghi audit_log (S1-04)
   │  │  ├─ response/                 # PageResponse
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
│  ├─ ServiceController.java          # REST: nhận/trả DTO, @Valid, không có logic
│  └─ ComboController.java
├─ service/
│  ├─ ServiceService.java             # nghiệp vụ + @Transactional; module khác chỉ gọi vào đây
│  └─ ComboService.java
├─ repository/
│  └─ ServiceRepository.java          # Spring Data JPA
├─ entity/
│  ├─ Service.java                    # entity JPA, ánh xạ đúng bảng trong migration
│  └─ ServicePriceHistory.java
├─ dto/
│  ├─ request/
│  │  ├─ CreateServiceRequest.java    # record + annotation kiểm tra (@NotBlank, @PositiveOrZero...)
│  │  └─ UpdateServiceRequest.java
│  └─ response/
│     └─ ServiceResponse.java         # record
├─ mapper/
│  └─ ServiceMapper.java              # entity ↔ DTO bằng MapStruct (interface @Mapper)
└─ exception/
   └─ ServiceNotFoundException.java   # kế thừa BusinessException ở common
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
- `@Transactional` đặt ở service, không ở controller hay repository.
- URL: `/api/{module}/{tài-nguyên}`, danh từ số nhiều, kebab-case (`/api/catalog/services`,
  `/api/visits`). Hành động nghiệp vụ dùng `POST` lên tài nguyên con:
  `POST /api/catalog/services/{id}/deactivate`, `POST /api/closings/{date}`.
- `common`, `config`, `security` không phụ thuộc vào module nghiệp vụ nào. Không để logic nghiệp vụ vào
  `common`.
- Module mới: tạo thư mục trong `module/` theo đúng khuôn trên, cập nhật cây thư mục ở file này và tên
  feature tương ứng ở `frontend/AGENTS.md`.
- Test đặt cùng package với lớp được test: `ServiceServiceTest` (đơn vị, Mockito), `ServiceControllerIT`
  (tích hợp, Testcontainers + MockMvc).

## Cơ sở dữ liệu

- Schema chỉ đổi bằng Flyway (`src/main/resources/db/migration/`). **Không sửa `V1`, `V2`**; thêm `V3__...`.
- `spring.jpa.hibernate.ddl-auto` luôn là `validate` hoặc `none`, không bao giờ `update`/`create`.
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

- Mật khẩu băm Argon2 hoặc BCrypt. Không log mật khẩu, token, refresh token.
- Refresh token **chỉ lưu bản băm**. Access token trong cookie `httpOnly`, `Secure`, `SameSite`.
- Mọi endpoint ngoài đăng nhập/quên mật khẩu đều yêu cầu xác thực; chưa đăng nhập trả 401.
- Thông báo sai đăng nhập không tiết lộ email có tồn tại hay không.
- Bí mật đọc từ biến môi trường theo profile `dev`/`prod`; không commit.

## Test

- Test tích hợp dùng **Testcontainers với PostgreSQL thật**, không dùng H2 (trigger và kiểu dữ liệu của Postgres khác H2).
- Phần tiền, chốt sổ, bút toán luôn có test. Bộ test schema: `src/test/resources/sql/schema_smoke_test.sql`.
- Test tích hợp gắn `@IntegrationTest` (`src/test/java/com/xilespa/support`): chạy cả ứng dụng với Postgres 16
  thật và MockMvc. Cần **Docker Desktop đang chạy**.
- Chạy (trong `backend/`, cần JDK 21): `./gradlew spotlessApply test`. Chạy ứng dụng: `./gradlew bootRun`
  (profile `dev`, cần Postgres ở máy, card S0-05).
- Format: Spotless (google-java-format kiểu AOSP, thụt 4 khoảng). Chạy `spotlessApply` trước khi commit.
- Spring Boot 4 dùng Jackson 3: import `tools.jackson.databind...`, không phải `com.fasterxml.jackson.databind...`.
