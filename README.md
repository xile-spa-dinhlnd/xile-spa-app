# Xile Spa Admin

Hệ thống quản trị cho tiệm Spa & Giãn cơ Xile: quản lý dịch vụ, combo, sổ doanh thu hằng ngày, CRM khách hàng, và chốt sổ có đối soát ra Google Sheets.

> Dự án thay thế quy trình Excel theo tháng (file CRM + file doanh thu/tour) bằng một hệ thống nhập liệu một lần, lưu tập trung và có sao lưu.

## Trạng thái

Xong **Sprint 0**: khung backend và frontend chạy được trên máy dev, database tạo bằng migration, test chạy được ở máy. Tiếp theo là Sprint 1 (đăng nhập, bảng giá). Xem [backlog](docs/backlog/sprint-0-1.md).

## Công nghệ (xem [ADR-0001](docs/adr/0001-tech-stack.md))

| Lớp | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 4, Gradle (Kotlin DSL), Spring Data JPA, Spring Security, Flyway, MapStruct |
| Database | PostgreSQL 16 |
| Frontend | React 19, TypeScript, Vite, Ant Design 6, TanStack Query, React Router |
| Test | JUnit 5 + Testcontainers (PostgreSQL thật), Vitest |
| DevOps (để sau) | Docker Compose, GitHub Actions, Caddy, VPS |

## Cấu trúc repo

```
xile-spa-app/
├─ backend/             # Spring Boot (modular monolith), quy tắc: backend/AGENTS.md
├─ frontend/            # React + TypeScript (Vite), quy tắc: frontend/AGENTS.md
├─ docker-compose.yml   # chỉ PostgreSQL cho máy dev
├─ .env.example         # thông số database dev
├─ .agents/            # cấu hình Antigravity: rules, skills, hooks (AGENTS.md mục 8)
├─ tools/agent-hooks/   # hook dùng chung cho các agent AI
└─ docs/
   ├─ srs/              # Đặc tả yêu cầu phần mềm (SRS)
   ├─ design/           # Thiết kế CSDL: ERD, sơ đồ sinh tự động, thiết kế đợt R2/R3
   ├─ adr/              # Các quyết định kiến trúc (Architecture Decision Records)
   ├─ backlog/          # Card của từng sprint
   └─ guides/           # Sổ tay: dùng agent AI (ai-agents.md)
```

## Chạy trên máy dev (từ máy trống)

### 1. Cài công cụ

| Công cụ | Phiên bản | Ghi chú |
|---|---|---|
| Git | bất kỳ | |
| JDK | **21** | Ví dụ Eclipse Temurin 21. Gradle tự tìm JDK 21 đã cài; nếu máy có nhiều JDK thì đặt `JAVA_HOME` trỏ tới bản 21 |
| Docker Desktop | bản mới | Chỉ để chạy PostgreSQL và Testcontainers. Phải **đang mở** khi chạy database và khi chạy test backend |
| Node.js | **22 trở lên** (kèm npm) | Cho frontend |

Không cần cài Gradle (dùng `./gradlew`) và không cần cài PostgreSQL.

### 2. Lấy mã nguồn

```bash
git clone git@github.com:xile-spa-dinhlnd/xile-spa-app.git
cd xile-spa-app
```

Nhánh mặc định là `develop`. Quy trình nhánh, commit, PR: [CONTRIBUTING.md](CONTRIBUTING.md).

### 3. Bật database

```bash
docker compose up -d
```

Chạy backend với profile `dev` sẽ tự thêm dữ liệu mẫu (nhân viên, dịch vụ, combo); chạy lại không tạo trùng. Muốn
database trống thì đặt `XILE_DEV_DATA=false`. Quy tắc thêm dữ liệu mẫu: `backend/AGENTS.md` mục "Dữ liệu mẫu cho máy dev".

PostgreSQL 16 chạy ở `localhost:5433` (không dùng 5432 để khỏi đụng PostgreSQL cài sẵn trên máy, nếu có), database `xile_spa`, user `xile`, mật khẩu `xile_dev`. Đây là thông số **chỉ dùng cho dev**. Muốn đổi thì chép `.env.example` thành `.env` rồi sửa.

Tắt: `docker compose down` (giữ dữ liệu). Xóa sạch dữ liệu dev: `docker compose down -v`.

### 4. Chạy backend

```bash
cd backend
./gradlew bootRun
```

Backend chạy ở http://localhost:8080 với profile `dev`. Lần đầu khởi động, Flyway tự tạo bảng (migration `V1`, `V2`). Kiểm tra: http://localhost:8080/actuator/health trả về `"status":"UP"`. Các API khác trả 401 (chưa có đăng nhập, làm ở Sprint 1).

### 5. Chạy frontend

Mở terminal khác:

```bash
cd frontend
npm ci
npm run dev
```

Mở http://localhost:5173. Trang **Tổng quan** báo "Máy chủ đang hoạt động bình thường" là frontend đã nối được backend (Vite chuyển `/api` và `/actuator` sang cổng 8080).

### 6. Thử "Quên mật khẩu" ở máy dev

Máy dev không gửi email thật (OQ-10). Ở trang đăng nhập bấm **Quên mật khẩu?**, nhập email tài khoản chủ tiệm, rồi
mở log của backend (cửa sổ chạy `./gradlew bootRun` hoặc IntelliJ) và tìm khối `[DEV EMAIL SENDER (OQ-10)]`. Dòng
"Liên kết đặt lại mật khẩu" chứa link dạng `http://localhost:5173/reset-password?token=...`; mở link đó để đặt
mật khẩu mới. Link hết hạn sau 30 phút, dùng một lần; mỗi tài khoản tối đa 3 yêu cầu trong 15 phút.

## Chạy ở môi trường prod (biến môi trường)

Profile `prod` (`application-prod.yml`) không có giá trị mặc định: thiếu biến nào thì backend báo lỗi ngay lúc
khởi động. Đặt các biến sau trên máy chủ, **không** ghi vào repo:

| Biến | Ý nghĩa |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | **Bắt buộc.** Quên đặt thì backend chạy profile `dev` (khóa JWT và tài khoản dev, không gửi email thật) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Kết nối PostgreSQL |
| `JWT_SECRET` | Khóa ký JWT, chuỗi ngẫu nhiên ít nhất 32 byte |
| `JWT_COOKIE_SECURE=true` | Cookie chỉ gửi qua HTTPS |
| `MAIL_HOST`, `MAIL_PORT` | Máy chủ SMTP, ví dụ Gmail `smtp.gmail.com`, `587` |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Tài khoản SMTP; với Gmail dùng App Password, không dùng mật khẩu đăng nhập |
| `MAIL_FROM` | Địa chỉ người gửi email đặt lại mật khẩu |
| `AUTH_RESET_PASSWORD_BASE_URL` | Đường dẫn trang đặt lại mật khẩu của frontend, ví dụ `https://<tên-miền>/reset-password` |

Giá trị mẫu xem `.env.example`.

## Kiểm tra trước khi commit

Backend (cần Docker Desktop đang mở, test dùng PostgreSQL thật qua Testcontainers):

```bash
cd backend && ./gradlew spotlessApply test
```

Frontend:

```bash
cd frontend && npm run format && npm run lint && npm run typecheck && npm test
```

## Lỗi hay gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `docker compose up` báo cổng 5433 đã dùng | Đặt `POSTGRES_PORT` khác trong `.env`, và chạy backend với `DB_URL=jdbc:postgresql://localhost:<cổng>/xile_spa` |
| Test backend báo không tìm thấy Docker | Mở Docker Desktop rồi chạy lại |
| Gradle báo sai phiên bản Java | Cài JDK 21, hoặc chạy `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` (macOS) |
| Trang Tổng quan báo lỗi hệ thống | Backend chưa chạy hoặc database chưa bật (bước 3, 4) |

## Tài liệu

- [Lộ trình phiên bản (roadmap)](docs/roadmap.md)
- [Postman collection để thử API](docs/postman/README.md)
- [SRS: mục lục và cách đọc](docs/srs/README.md)
- [Thiết kế CSDL (ERD)](docs/design/erd.md)
- [ADR-0001: Lựa chọn công nghệ](docs/adr/0001-tech-stack.md)
- [ADR-0002: Database là nguồn gốc, Google Sheets là bản sao](docs/adr/0002-source-of-truth-and-sheets-mirror.md)
- [ADR-0003: Sửa sổ đã chốt bằng bút toán điều chỉnh](docs/adr/0003-closed-day-corrections-via-adjustments.md)
- Quy tắc cho AI và cho từng phần: [AGENTS.md](AGENTS.md), [backend/AGENTS.md](backend/AGENTS.md), [frontend/AGENTS.md](frontend/AGENTS.md)

## Quy trình làm việc

Scrum thu gọn cho nhóm hai người, sprint hai tuần, quản lý task trên Trello. Mỗi card Trello gắn với mã yêu cầu trong SRS (ví dụ `FR-SVC-02`). Mọi thay đổi vào `develop` và `main` phải qua Pull Request; Đình là người duyệt. Chi tiết về nhánh (Git Flow), commit, PR: [CONTRIBUTING.md](CONTRIBUTING.md).
