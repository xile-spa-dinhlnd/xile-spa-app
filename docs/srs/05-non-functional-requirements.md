# 5. Yêu cầu phi chức năng

Các con số dưới đây là **mục tiêu khởi đầu**, được chọn cho quy mô một tiệm nhỏ. Điều chỉnh khi có số đo thực tế.

## 5.1 Bảo mật (SEC)

| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| NFR-SEC-01 | Toàn bộ truy cập qua HTTPS (chứng chỉ tự động gia hạn). Không phục vụ HTTP ngoài việc chuyển hướng. | M |
| NFR-SEC-02 | Mật khẩu băm bằng thuật toán chuyên dụng (argon2id hoặc bcrypt), không lưu và không ghi log mật khẩu gốc. | M |
| NFR-SEC-03 | Chính sách mật khẩu: tối thiểu 10 ký tự, chặn mật khẩu quá phổ biến. | M |
| NFR-SEC-04 | Token đặt trong cookie `httpOnly`, `Secure`, `SameSite`. Access token sống ngắn, refresh token xoay vòng và thu hồi được. | M |
| NFR-SEC-05 | Giới hạn tốc độ (rate limit) cho đăng nhập và quên mật khẩu. | S |
| NFR-SEC-06 | Chống các lỗi phổ biến theo OWASP Top 10 (SQL injection, XSS, CSRF, kiểm soát truy cập). Kiểm tra dữ liệu đầu vào ở phía máy chủ. | M |
| NFR-SEC-07 | Bí mật (khóa Google, mật khẩu cơ sở dữ liệu, khóa JWT) lưu ngoài mã nguồn, trong biến môi trường hoặc secret của hệ thống triển khai. Không commit lên Git. | M |
| NFR-SEC-08 | Máy chủ: tường lửa chỉ mở cổng cần thiết, đăng nhập SSH bằng khóa, tắt mật khẩu, cài fail2ban. | S |

## 5.2 Quyền riêng tư dữ liệu (PRV)

| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| NFR-PRV-01 | Dữ liệu khách hàng thật (tên, SĐT, tình trạng sức khỏe) không đưa vào repo, ảnh chụp màn hình công khai hay môi trường demo. Bản demo dùng dữ liệu giả sinh bằng script. | M |
| NFR-PRV-02 | Ghi chú sức khỏe là dữ liệu nhạy cảm: chỉ hiển thị trong hồ sơ khách, không đưa ra log hay email. | M |
| NFR-PRV-03 | Hỗ trợ ẩn hoặc xóa hồ sơ khách theo yêu cầu (FR-CUS-10). | S |
| NFR-PRV-04 | Bản sao trên Google Sheets nằm trong tài khoản của chủ tiệm, không chia sẻ công khai. Cân nhắc có đưa thông tin nhận dạng khách vào Sheets hay chỉ đưa số liệu tổng hợp (xem OQ-09). | M |

## 5.3 Độ tin cậy và sao lưu (REL)

| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| NFR-REL-01 | Sao lưu cơ sở dữ liệu tự động hằng đêm, **mã hóa** và lưu **ngoài VPS** (Google Drive hoặc kho đối tượng). Giữ tối thiểu 14 bản gần nhất. | M |
| NFR-REL-02 | Có tài liệu khôi phục từng bước và **thử khôi phục** ít nhất mỗi quý (một bản sao lưu không thử khôi phục thì chưa tính là sao lưu). | M |
| NFR-REL-03 | Mục tiêu khôi phục: mất tối đa 24 giờ dữ liệu (RPO), khôi phục trong 4 giờ (RTO). | S |
| NFR-REL-04 | Hệ thống tự khởi động lại khi máy chủ khởi động lại. Cơ sở dữ liệu dùng volume bền vững. | M |
| NFR-REL-05 | Lỗi từ dịch vụ ngoài (Google, email) không làm hỏng thao tác của người dùng. Việc đó được xử lý nền với thử lại. | M |

## 5.4 Hiệu năng (PERF)

| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| NFR-PERF-01 | Các thao tác danh sách và nhập liệu phản hồi trong dưới 500 ms ở phân vị 95 với dữ liệu đến 50.000 giao dịch. | S |
| NFR-PERF-02 | Dashboard tải trong dưới 2 giây với một năm dữ liệu. | S |
| NFR-PERF-03 | Toàn bộ hệ thống (ứng dụng, cơ sở dữ liệu, proxy) chạy ổn định trong khoảng 2,5 GB RAM để chừa chỗ cho hệ điều hành và các công cụ khác trên VPS 4 GB. | M |
| NFR-PERF-04 | Các truy vấn danh sách có phân trang và có chỉ mục phù hợp (ngày, mã khách). | M |

## 5.5 Khả năng sử dụng (USA)

| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| NFR-USA-01 | Giao diện responsive, dùng tốt từ màn hình 360 px (điện thoại) đến máy tính. Thiết kế ưu tiên điện thoại (mobile-first). | M |
| NFR-USA-02 | Vùng chạm tối thiểu 44 × 44 px cho nút và mục có thể bấm. | M |
| NFR-USA-03 | Ngôn ngữ giao diện là tiếng Việt. Định dạng ngày `dd/MM/yyyy`, tiền `1.234.000 đ`. | M |
| NFR-USA-04 | Hoạt động trên Safari (iPad, iPhone), Chrome (Android, máy tính). | M |
| NFR-USA-05 | Có thể cài lên màn hình chính như ứng dụng (PWA). | S |
| NFR-USA-06 | Nhập một giao dịch thông thường trong tối đa 5 thao tác chạm sau khi mở màn hình nhập. | S |
| NFR-USA-07 | Thông báo lỗi nói rõ nguyên nhân và cách sửa bằng ngôn ngữ dễ hiểu. | M |

## 5.6 Bảo trì và vận hành (MNT, OPS)

| ID | Yêu cầu | Ưu tiên |
|---|---|---|
| NFR-MNT-01 | Mã nguồn chia theo tính năng, có quy ước code chung và tài liệu API (OpenAPI). | M |
| NFR-MNT-02 | Kiểm thử tự động cho phần tính tiền, giảm giá, chốt sổ, khóa dữ liệu, bút toán điều chỉnh. Mục tiêu bao phủ phần logic tiền tệ từ 90% trở lên. | M |
| NFR-MNT-03 | Cơ sở dữ liệu thay đổi bằng migration có phiên bản, chạy được từ đầu trên máy sạch. | M |
| NFR-MNT-04 | CI chạy build và test cho mỗi Pull Request. Không gộp khi CI đỏ. | M |
| NFR-OPS-01 | Triển khai bằng Docker Compose, tự động hóa qua CI/CD. | S |
| NFR-OPS-02 | Có log có cấu trúc, health endpoint và cảnh báo khi hệ thống ngừng hoạt động. | S |
| NFR-OPS-03 | Dọn dẹp image và log định kỳ để không đầy ổ đĩa 35 GB. | S |
