# 1. Giới thiệu

## 1.1 Mục đích tài liệu

Tài liệu này mô tả yêu cầu của hệ thống **Xile Spa Admin**, để hai thành viên phát triển và chủ tiệm cùng hiểu một điều giống nhau về việc hệ thống sẽ làm gì, trước khi xây dựng.

Đối tượng đọc:
- Nhóm phát triển (2 người): dùng làm cơ sở thiết kế, chia task và viết test.
- Chủ tiệm: xác nhận các quy tắc nghiệp vụ và quy trình sử dụng.

## 1.2 Phạm vi sản phẩm

**Xile Spa Admin** là ứng dụng web quản trị cho một tiệm spa và giãn cơ nhỏ, thay thế việc quản lý bằng các file Excel theo tháng.

**Trong phạm vi (giai đoạn MVP):**
- Quản lý dịch vụ và combo.
- Ghi nhận giao dịch hằng ngày (doanh thu, giảm giá, chi phí).
- Chốt sổ theo ngày, khóa dữ liệu và đồng bộ bản sao lên Google Sheets.
- Dashboard theo dõi doanh thu.
- CRM cơ bản: hồ sơ khách, lịch sử, nhắc chăm sóc, nhắc sinh nhật, gói liệu trình.
- Đăng nhập, quên mật khẩu, nhật ký thay đổi.

**Ngoài phạm vi (chưa làm ở MVP):**
- Cổng đặt lịch trực tuyến cho khách hàng.
- Thanh toán trực tuyến.
- Ứng dụng di động riêng (thay bằng web responsive/PWA).
- Kế toán thuế, hóa đơn điện tử.
- Quản lý kho vật tư chi tiết (hiện chỉ ghi nhận chi phí mua hàng).

**Các giai đoạn sau (xem độ ưu tiên ở mục 3):** tiền tour và lương, theo dõi chi phí quảng cáo, nhập dữ liệu từ Excel, xuất báo cáo, nhắn tin tự động qua Zalo/SMS.

## 1.3 Thuật ngữ

| Thuật ngữ | Giải thích |
|---|---|
| KTV | Kỹ thuật viên, người thực hiện dịch vụ |
| Tour | Tiền công tính cho KTV trên mỗi lượt làm dịch vụ (cột TOUR trong bảng giá) |
| Dịch vụ | Một hạng mục có giá niêm yết, ví dụ "GỘI 60p" |
| Combo | Gói gồm nhiều dịch vụ bán với một giá chung |
| Gói liệu trình (thẻ) | Khách mua trước nhiều buổi (có thể có buổi tặng) và dùng dần trong thời hạn |
| Giao dịch (visit) | Một lần khách đến, gồm một hoặc nhiều dòng bán |
| Dòng bán | Một dịch vụ, combo hoặc gói trong một giao dịch |
| Chốt sổ | Hành động đóng sổ doanh thu của một ngày, sau đó dữ liệu bị khóa |
| Tổng doanh thu | Tổng giá niêm yết của các dòng bán trong kỳ, trước giảm giá |
| Giảm giá | Tổng tiền giảm (giá × % giảm) trong kỳ. Được tính vào Tổng chi |
| Tổng chi | Giảm giá + Chi mua hàng + Chi CTV + chi khác |
| Thực nhận | Tổng doanh thu − Tổng chi |
| Tiền khách thực trả | Tổng doanh thu − Giảm giá. Chỉ dùng để đối chiếu tiền mặt và chuyển khoản |
| Bút toán điều chỉnh | Dòng ghi thêm (cộng hoặc trừ) để sửa sai cho ngày đã chốt, giữ nguyên dòng gốc |
| CK / TM | Chuyển khoản / Tiền mặt |
| CSKH | Chăm sóc khách hàng |
| AOV | Giá trị trung bình mỗi lần đến (tổng chi tiêu ÷ số lần đến) |
| CPA, ROAS | Chi phí trên mỗi lead hoặc khách đến; doanh thu trên mỗi đồng chi quảng cáo |
| SRS | Đặc tả yêu cầu phần mềm |

## 1.4 Tác nhân (người dùng)

| Tác nhân | Mô tả | Giai đoạn |
|---|---|---|
| Chủ tiệm (OWNER) | Người dùng duy nhất ở MVP. Xem và nhập mọi dữ liệu, chốt sổ, xem doanh thu. Dùng máy tính, điện thoại, iPad. | MVP |
| Nhân viên (STAFF) | Dự kiến sau này, chỉ nhập giao dịch, không xem doanh thu tổng và lương của người khác. Chưa làm, nhưng hệ thống thiết kế sẵn trường vai trò. | Tương lai |
| Hệ thống (SYSTEM) | Các tác vụ nền: đồng bộ Google Sheets, gửi email, sao lưu | MVP |
| Google Sheets, dịch vụ email | Hệ thống bên ngoài mà ứng dụng gọi tới | MVP |

## 1.5 Tài liệu tham chiếu

- Hai file Excel hiện tại của tiệm: `CRM_Mini_Spa_Rehab_V3_PRO_Xile.xlsx` và `THÁNG 9 NĂM 2026 - SOURCE.xlsx` (ánh xạ chi tiết ở [08-excel-traceability.md](08-excel-traceability.md)).
- ISO/IEC/IEEE 29148 (khung viết yêu cầu).
- Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân (tham khảo khi xử lý dữ liệu khách hàng; đây không phải tư vấn pháp lý).
