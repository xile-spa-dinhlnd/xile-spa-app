# ADR-0001: Lựa chọn công nghệ

- **Trạng thái:** Accepted
- **Ngày:** 2026-10-01
- **Người quyết định:** nhóm phát triển (2 thành viên)

## Bối cảnh

Hệ thống quản trị cho một tiệm nhỏ, một người dùng chính (chủ tiệm), dùng trên điện thoại, iPad và máy tính. Nhóm hai người, chia việc theo tính năng (mỗi người làm cả giao diện lẫn backend của tính năng mình nhận). Chạy trên một VPS 2 vCPU, 4 GB RAM, 35 GB NVMe. Mục tiêu kép: có sản phẩm cho gia đình dùng thật, và học DevOps cùng ôn lại Spring Boot, React.

## Các lựa chọn đã cân nhắc

| Hạng mục | Chọn | Đã xem xét | Lý do chọn |
|---|---|---|---|
| Ngôn ngữ và khung backend | **Java 21 + Spring Boot 3** | NestJS, Go | Cả nhóm đã học Spring Boot. Phần cần làm (transaction, bảo mật, tác vụ nền, migration) đều có sẵn trong hệ sinh thái. Thị trường tuyển dụng Java tại Việt Nam lớn. |
| Công cụ build | **Gradle (Kotlin DSL)** | Maven | Cấu hình ngắn gọn, build tăng dần và cache nhanh hơn, phổ biến ở các dự án mới. Dùng Gradle Wrapper, version catalog. |
| Kiến trúc | **Modular monolith** chia package theo tính năng | Microservices | Một đội hai người, một tiệm nhỏ. Microservices làm tăng chi phí vận hành và RAM mà không có lợi ích tương ứng. Chia package theo tính năng khớp với cách chia việc và để dành khả năng tách sau này. |
| Cơ sở dữ liệu | **PostgreSQL** + Flyway | MySQL | Ràng buộc và kiểu dữ liệu mạnh, phổ biến trong hệ thống mới. Flyway để thay đổi cấu trúc có phiên bản. |
| Frontend | **React + TypeScript (Vite)** | Next.js, Astro | Trang quản trị không cần SEO nên không cần kết xuất phía máy chủ. Nhóm đã quen React và TypeScript. |
| Thư viện giao diện | **Ant Design** | shadcn/ui, MUI | Có sẵn bảng dữ liệu, biểu mẫu, chọn ngày, hỗ trợ tiếng Việt. Cho giao diện nhất quán mà không đòi hỏi con mắt thiết kế. |
| Gọi API và biểu mẫu | TanStack Query, React Hook Form, Zod | | Quản lý trạng thái máy chủ và kiểm tra dữ liệu gọn nhẹ. |
| Biểu đồ | Recharts | | Đủ cho dashboard và dễ dùng với React. |
| Xác thực | Spring Security + JWT trong cookie `httpOnly` | Session cổ điển | Phù hợp ứng dụng một trang, không lộ token cho JavaScript. |
| Tác vụ nền | `@Scheduled` kèm bảng `sync_job` trong cơ sở dữ liệu | Redis hoặc RabbitMQ ngay từ đầu | Đủ cho quy mô hiện tại, ít thành phần phải vận hành. Có thể nâng cấp sau. |
| Kiểm thử | JUnit 5, Testcontainers (backend); Vitest, Playwright (frontend) | | Kiểm thử với PostgreSQL thật cho phần tính tiền và khóa dữ liệu. |
| Triển khai | Docker Compose, GitHub Actions, Caddy (HTTPS) | Kubernetes | Phù hợp một VPS 4 GB. Kubernetes tốn tài nguyên và chưa cần thiết. |
| Quản lý công việc | Trello (Scrum rút gọn, sprint 2 tuần) | GitHub Projects | Nhóm đã chọn. Mã yêu cầu SRS dùng làm tiêu đề thẻ. |

## Hệ quả

**Tích cực:**
- Giảm rủi ro kỹ thuật vì dùng công nghệ nhóm đã biết, dành sức cho nghiệp vụ và DevOps.
- Dễ chia việc theo tính năng, ít xung đột mã.

**Đánh đổi:**
- JVM tốn bộ nhớ hơn Node hoặc Go. Cần giới hạn heap (ví dụ `-Xmx512m`) và đo thực tế trên VPS (NFR-PERF-03).
- Ant Design là bộ giao diện nặng, nên cần bật cắt bớt mã không dùng (tree-shaking) khi build.
- Chưa dùng hàng đợi chuyên dụng nên khả năng mở rộng tác vụ nền bị giới hạn. Chấp nhận được ở quy mô một tiệm.

## Điều kiện xem xét lại

- Nếu RAM thực tế không đủ cho JVM cùng các thành phần khác, cân nhắc giảm thành phần hoặc nâng cấp VPS.
- Nếu cần nhiều tác vụ nền hơn hoặc nhiều người dùng, cân nhắc thêm Redis và hàng đợi.
