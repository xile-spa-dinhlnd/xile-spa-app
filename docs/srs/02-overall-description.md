# 2. Mô tả tổng quan

## 2.1 Bối cảnh sản phẩm

Hiện tiệm dùng hai file Excel, mỗi tháng làm lại:

1. **File CRM** (10 sheet): danh mục, dữ liệu khách hàng, lịch sử dịch vụ, quản lý gói, chăm sóc khách, sinh nhật, quảng cáo, dashboard.
2. **File tháng** (4 sheet): bảng giá và tiền tour, sổ doanh thu và chi phí hằng ngày, chấm tour của KTV, phiếu lương.

Hệ thống mới gộp hai nguồn này vào một cơ sở dữ liệu. Mỗi lần khách đến chỉ nhập **một lần**, từ đó sinh ra doanh thu, lịch sử khách, trừ buổi gói và tiền tour.

## 2.2 Vấn đề của quy trình hiện tại

| # | Vấn đề | Hậu quả |
|---|---|---|
| P1 | Dữ liệu nằm trong file lưu trên máy cá nhân | Nguy cơ mất dữ liệu, không có bản sao lưu |
| P2 | Mỗi tháng một file mới | Khó so sánh giữa các tháng, phải dựng lại công thức |
| P3 | Cùng một giao dịch nhập ở hai file (CRM và sổ doanh thu) | Nhập trùng, dễ lệch số |
| P4 | Tên dịch vụ nhập tay không thống nhất (`GỘI 60P` và `GỘI 60p`) | Công thức tra giá lỗi, dòng báo lỗi |
| P5 | Tiền tour được tra theo **giá tiền** thay vì theo dịch vụ | Hai dịch vụ cùng giá sẽ tra nhầm tour |
| P6 | Có lỗi nhập liệu (ví dụ ngày `01/09/1026`) | Báo cáo theo ngày sai |
| P7 | Cột "Tổng doanh thu" lấy số **sau giảm** nhưng "Tổng chi" lại cộng thêm "chi giảm giá", nên giảm giá bị trừ hai lần trong THỰC NHẬN (xem OQ-03) | Con số thực nhận bị thấp hơn thực tế (tháng 9: 315.880 đ so với 448.440 đ) |
| P8 | Cảnh báo khách (số ngày chưa quay lại) dùng `TODAY()` | Kết quả đổi theo ngày mở file, không có ảnh chụp tại thời điểm |
| P9 | Không phân quyền, không nhật ký thay đổi | Ai mở file cũng sửa được, không biết ai đã sửa gì |
| P10 | Sổ cũ vẫn sửa được | Không có cơ chế "chốt sổ" thật sự |
| P11 | Có ô công thức bị gõ đè số tay (tháng 9, dòng 46 cột "thu sau giảm" có số 69.000 đ nhập tay, không có tên dịch vụ) | Tổng doanh thu lẫn số không truy được nguồn |

## 2.3 Chức năng chính (tóm tắt)

```
Đăng nhập ─┬─ Dịch vụ ── Combo
           ├─ Khách hàng ── Gói liệu trình
           ├─ Giao dịch (bán hàng) ── Chi phí
           ├─ Chốt sổ ── Đồng bộ Google Sheets (nền)
           ├─ Dashboard doanh thu
           └─ (sau) Nhân viên: tour và lương, Quảng cáo, Báo cáo
```

## 2.4 Đặc điểm người dùng

- Chủ tiệm quen Excel và Google Sheets, không chuyên kỹ thuật.
- Dùng xen kẽ máy tính, điện thoại và iPad, thường nhập liệu ngay tại quầy trong giờ làm việc.
- Giao diện phải đơn giản, ít thao tác, chữ và nút đủ lớn để chạm.

## 2.5 Ràng buộc

| Mã | Ràng buộc |
|---|---|
| C-01 | Hạ tầng: một VPS (2 vCPU, 4 GB RAM, 35 GB NVMe). Mọi dịch vụ phải chạy vừa trong giới hạn này. |
| C-02 | Công nghệ chính: Java/Spring Boot, React, PostgreSQL (xem ADR-0001). |
| C-03 | Nhóm 2 người, mỗi người khoảng 2 đến 3 giờ mỗi ngày, không có hạn chót cứng. |
| C-04 | Dữ liệu khách hàng là dữ liệu cá nhân, không đưa vào repo công khai hay môi trường demo. |
| C-05 | Gọi Google Sheets API là chậm và có giới hạn tốc độ, nên không được thực hiện trong luồng xử lý yêu cầu của người dùng. |
| C-06 | Chủ tiệm là người dùng duy nhất ở MVP nhưng vẫn cần đăng nhập, quên mật khẩu đầy đủ. |

## 2.6 Giả định và phụ thuộc

| Mã | Giả định | Nếu sai thì |
|---|---|---|
| A-01 | Tiệm có một cơ sở, một múi giờ (`Asia/Ho_Chi_Minh`) | Phải thêm khái niệm chi nhánh |
| A-02 | Mỗi khách xác định bằng số điện thoại | Cần thêm cách phân biệt khách trùng số |
| A-03 | Hiện có 3 người làm dịch vụ: chủ tiệm, 1 KTV da, 1 KTV giãn cơ | Danh sách nhân viên cấu hình được, không hard-code |
| A-04 | Combo và liệu trình da do chủ tiệm tự làm nên tiền tour = 0 | Tour phải cấu hình được theo dịch vụ và theo dòng bán (xem OQ-06) |
| A-05 | Có tên miền và dịch vụ gửi email cho tính năng quên mật khẩu | Chưa có thì không bật được FR-AUTH-03 ở môi trường thật |
| A-06 | Chủ tiệm chấp nhận dùng Google Sheets làm bản sao ngoài | Đổi sang kho lưu trữ khác (Drive, R2) |
