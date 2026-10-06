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
└─ docs/
   ├─ srs/              # Đặc tả yêu cầu phần mềm (SRS)
   ├─ design/           # Thiết kế CSDL: ERD, sơ đồ sinh tự động, thiết kế đợt R2/R3
   ├─ adr/              # Các quyết định kiến trúc (Architecture Decision Records)
   └─ backlog/          # Card của từng sprint
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

- [SRS: mục lục và cách đọc](docs/srs/README.md)
- [Thiết kế CSDL (ERD)](docs/design/erd.md)
- [ADR-0001: Lựa chọn công nghệ](docs/adr/0001-tech-stack.md)
- [ADR-0002: Database là nguồn gốc, Google Sheets là bản sao](docs/adr/0002-source-of-truth-and-sheets-mirror.md)
- [ADR-0003: Sửa sổ đã chốt bằng bút toán điều chỉnh](docs/adr/0003-closed-day-corrections-via-adjustments.md)
- Quy tắc cho AI và cho từng phần: [AGENTS.md](AGENTS.md), [backend/AGENTS.md](backend/AGENTS.md), [frontend/AGENTS.md](frontend/AGENTS.md)

## Quy trình làm việc

Scrum thu gọn cho nhóm hai người, sprint hai tuần, quản lý task trên Trello. Mỗi card Trello gắn với mã yêu cầu trong SRS (ví dụ `FR-SVC-02`). Mọi thay đổi vào `develop` và `main` phải qua Pull Request; Đình là người duyệt. Chi tiết về nhánh (Git Flow), commit, PR: [CONTRIBUTING.md](CONTRIBUTING.md).
