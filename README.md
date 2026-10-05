# Xile Spa Admin

Hệ thống quản trị cho tiệm Spa & Giãn cơ Xile: quản lý dịch vụ, combo, sổ doanh thu hằng ngày, CRM khách hàng, và chốt sổ có đối soát ra Google Sheets.

> Dự án thay thế quy trình Excel theo tháng (file CRM + file doanh thu/tour) bằng một hệ thống nhập liệu một lần, lưu tập trung và có sao lưu.

## Trạng thái

Đang ở **Sprint 0** (đặc tả yêu cầu, thiết kế dữ liệu, dựng môi trường).

## Công nghệ (dự kiến, xem [ADR-0001](docs/adr/0001-tech-stack.md))

| Lớp | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3, Gradle (Kotlin DSL), Spring Data JPA, Spring Security (JWT), Flyway |
| Database | PostgreSQL |
| Frontend | React, TypeScript, Vite, Ant Design, TanStack Query |
| DevOps | Docker Compose, GitHub Actions, Caddy, VPS |

## Cấu trúc repo (dự kiến)

```
xile-spa/
├─ backend/     # Spring Boot (modular monolith, chia package theo tính năng)
├─ frontend/    # React + TypeScript (Vite)
├─ deploy/      # docker-compose, Caddyfile, script backup
└─ docs/
   ├─ srs/      # Đặc tả yêu cầu phần mềm (SRS)
   ├─ design/   # Thiết kế CSDL: ERD, sơ đồ sinh tự động, thiết kế đợt R2/R3
   └─ adr/      # Các quyết định kiến trúc (Architecture Decision Records)
```

## Tài liệu

- [SRS: mục lục và cách đọc](docs/srs/README.md)
- [Thiết kế CSDL (ERD)](docs/design/erd.md)
- [ADR-0001: Lựa chọn công nghệ](docs/adr/0001-tech-stack.md)
- [ADR-0002: Database là nguồn gốc, Google Sheets là bản sao](docs/adr/0002-source-of-truth-and-sheets-mirror.md)
- [ADR-0003: Sửa sổ đã chốt bằng bút toán điều chỉnh](docs/adr/0003-closed-day-corrections-via-adjustments.md)

## Quy trình làm việc

Scrum thu gọn cho nhóm hai người, sprint hai tuần, quản lý task trên Trello. Mỗi card Trello gắn với mã yêu cầu trong SRS (ví dụ `FR-SVC-02`). Mọi thay đổi vào nhánh chính phải qua Pull Request và có người còn lại duyệt. Chi tiết về nhánh, commit, PR: [CONTRIBUTING.md](CONTRIBUTING.md).
