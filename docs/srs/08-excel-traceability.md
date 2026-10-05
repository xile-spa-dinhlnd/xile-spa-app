# 8. Ánh xạ từ Excel hiện tại sang yêu cầu mới

Bảng này giúp chắc chắn không bỏ sót chức năng đang dùng trong Excel, và giúp chủ tiệm thấy "cái cũ nằm ở đâu trong hệ thống mới".

## File CRM: `CRM_Mini_Spa_Rehab_V3_PRO_Xile.xlsx`

| Sheet | Nội dung hiện tại | Thay bằng | Yêu cầu | Ghi chú |
|---|---|---|---|---|
| `DANH MỤC` | Các danh sách cho ô chọn (dịch vụ, nguồn khách, trạng thái, KTV, kết quả CSKH, kênh ads) | Danh mục trong cơ sở dữ liệu, quản lý trên giao diện | FR-SVC-05, FR-STF-01, FR-SYS-04 | Hết cảnh báo "không xóa sheet này". Giá trị phân loại dùng danh mục để khỏi gõ sai |
| `DATA KHÁCH HÀNG` | Hồ sơ khách, kèm các cột công thức (số lần đến, tổng chi, AOV, ngày chưa quay lại, nhóm cảnh báo, nhóm khách) | `customer`, hồ sơ 360 | FR-CUS-01..05 | Các cột công thức trở thành giá trị tính khi truy vấn (BR-11, BR-17) |
| `LỊCH SỬ DỊCH VỤ` | Mỗi lần đến một dòng: ngày, khách, dịch vụ, KTV, giá, thực thu (nay tính ra), giảm giá, thanh toán, nguồn | `visit` và `visit_item` | FR-VIS-01..09 | Gộp với sổ doanh thu của file tháng, nhập một lần |
| `QUẢN LÝ GÓI` | Thẻ liệu trình: buổi mua, tặng, đã dùng, còn lại, hạn, cảnh báo | `customer_package` | FR-PKG-01..04 | Đã dùng tự đếm từ giao dịch thay vì đếm bằng COUNTIFS |
| `CSKH-RE` | Danh sách khách cần chăm sóc, ghi nhận liên hệ | `customer_contact`, danh sách cần chăm sóc | FR-CUS-04, FR-CUS-06 | |
| `SINH NHẬT` | Nhắc sinh nhật 7 ngày | Danh sách sinh nhật | FR-CUS-07 | Công thức cũ dùng `+365` nên sai ở năm nhuận, hệ thống mới tính theo lịch |
| `ADS-MARKETING` | Chi phí ads, lead, CPA, ROAS | `marketing_spend` | FR-ADS-01, FR-ADS-02 | Ưu tiên thấp |
| `DASHBOARD` | Thẻ tổng khách, doanh thu, khách VIP, việc cần làm hôm nay | Dashboard | FR-DSH-01..09 | |
| `BÁO CÁO` | Doanh thu theo nguồn khách và theo dịch vụ | Dashboard | FR-DSH-03, FR-DSH-04 | |
| `HƯỚNG DẪN` | Hướng dẫn 7 bước dùng file | Giao diện tự mô tả, tài liệu người dùng | (ngoài SRS) | Viết hướng dẫn sử dụng ngắn khi bàn giao |

## File tháng: `THÁNG 9 NĂM 2026 - SOURCE.xlsx`

| Sheet | Nội dung hiện tại | Thay bằng | Yêu cầu | Ghi chú |
|---|---|---|---|---|
| `LIST K XOÁ` | Bảng giá dịch vụ, combo, tiền tour (một số hạng mục chưa có giá) | `service`, `combo` | FR-SVC-01..04, FR-CMB-01..05, FR-IMP-01 | Nhập sẵn từ sheet này khi khởi tạo |
| `DOANH THU THÁNG` | Sổ ngày: cách thanh toán, dịch vụ, doanh thu, bán gói, giảm giá %, chi mua hàng, chi CTV, thực nhận, cột kiểm tra lỗi | `visit`, `visit_item`, `expense`, chốt sổ, dashboard | FR-VIS, FR-EXP, FR-CLS, FR-DSH-07 | Cột "Thông Báo" kiểm tra thiếu dữ liệu trở thành kiểm tra khi nhập (FR-VIS-09). **Bỏ cột "Thu sau giảm"**: Tổng doanh thu lấy theo giá trước giảm, "Chi giảm giá" giữ trong Tổng chi, Thực nhận = Tổng doanh thu − Tổng chi (OQ-03, BR-05) |
| `TOUR` | Chấm tour từng ngày của KTV, tra tour theo giá tiền | `visit_item.tour_fee` | FR-VIS-05, FR-VIS-06, FR-PAY-01 | Cách tra theo giá sẽ nhầm khi hai dịch vụ cùng giá (P5), hệ thống mới gắn tour vào từng dịch vụ |
| `PHIẾU LƯƠNG` | Tổng hợp số tour và tiền từng loại gội, thực lĩnh | Báo cáo lương | FR-PAY-01..04 | Mới có 4 loại gội 30, 60, 80, 110 phút. Chưa có nội dung cho combo và liệu trình (OQ-06) |

## Chức năng mới (Excel hiện tại không có)

| Chức năng | Yêu cầu |
|---|---|
| Đăng nhập, quên mật khẩu, nhật ký thay đổi | FR-AUTH, FR-SYS-01 |
| Chốt sổ có khóa dữ liệu và bút toán điều chỉnh | FR-CLS |
| Đồng bộ bản sao ra Google Sheets có đối soát | FR-SYN |
| Sao lưu hằng đêm ra ngoài máy chủ | FR-SYS-02, NFR-REL |
| Theo dõi khoản đã trả lương tách khỏi tour phát sinh | FR-PAY-02, FR-PAY-03, BR-14 |
