# Postman

- `xile-spa.postman_collection.json`: mọi endpoint của backend, kèm kiểm tra tự động (tab Tests).
- `xile-spa-dev.postman_environment.json`: biến cho máy dev (`baseUrl`, tài khoản chủ tiệm dev). Giá trị lấy từ
  `backend/src/main/resources/application-dev.yml`, **chỉ dùng cho dev**; không đưa tài khoản thật vào đây.

## Dùng

1. Postman bản desktop → Import → chọn cả hai file → chọn environment "Xile Spa - dev" (góc trên bên phải).
2. Bật database (`docker compose up -d`) và backend (IntelliJ).
3. Chạy từng request, hoặc chuột phải collection → Run collection để chạy hết theo thứ tự:
   hệ thống → đăng nhập → dùng API → thử lỗi → đăng xuất.

## Quy tắc

Thêm hoặc sửa endpoint thì cập nhật collection **trong cùng PR** (AGENTS.md mục 5): mỗi endpoint một request
trong đúng thư mục chức năng, có ví dụ body và test kiểm tra mã trạng thái. Sửa trong Postman xong thì
Export (Collection v2.1) đè lên file này.
