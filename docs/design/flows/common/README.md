# Luồng xử lý: common (phần chung)

Phần dùng chung cho mọi module backend, không chứa nghiệp vụ (`backend/AGENTS.md` mục Cấu trúc): định dạng lỗi
`ApiError` và `GlobalExceptionHandler`, tiện ích chuỗi và phân trang, cấu hình Jackson. Thay đổi ở đây ảnh hưởng mọi
API của cả Đình và Huy, nên luôn báo người kia và chạy cả bộ test.

| Tính năng                         | File                 |
| --------------------------------- | -------------------- |
| Chặn đầu vào bất thường           | `input-hardening.md` |
