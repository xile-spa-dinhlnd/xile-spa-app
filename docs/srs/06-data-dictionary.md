# 6. Từ điển dữ liệu (mức khái niệm)

Đây là danh sách thực thể mức khái niệm để thống nhất ngôn ngữ. **Thiết kế chi tiết** (khóa, quan hệ, kiểu dữ liệu, ràng buộc, trigger, sơ đồ ERD) nằm ở [`docs/design/erd.md`](../design/erd.md) và migration `backend/src/main/resources/db/migration/`. Khi hai nơi khác nhau, migration là đúng.

Quy ước chung cho mọi bảng: có `id`, `created_at`, `updated_at`. Số tiền là số nguyên đồng (BR-01). Bảng tài chính dùng xóa mềm (BR-09), không xóa cứng.

## Danh mục

| Thực thể | Mục đích | Trường chính | Yêu cầu liên quan |
|---|---|---|---|
| `service_group` | Nhóm dịch vụ | tên | FR-SVC-05 |
| `service` | Dịch vụ trong bảng giá | tên (duy nhất), nhóm, thời lượng (phút), giá niêm yết, tiền tour mặc định, đang bán | FR-SVC-01..04 |
| `service_price_history` | Lịch sử đổi giá | dịch vụ, giá cũ, giá mới, thời điểm, người đổi | FR-SVC-03, FR-SVC-06 |
| `combo` | Combo | tên (duy nhất), giá combo, đang bán | FR-CMB-01..05 |
| `combo_item` | Thành phần combo | combo, dịch vụ, số lượng | FR-CMB-02 |
| `staff` | Người thực hiện dịch vụ | tên, vai trò, đang làm | FR-STF-01 |

## Khách hàng

| Thực thể | Mục đích | Trường chính | Yêu cầu liên quan |
|---|---|---|---|
| `customer` | Hồ sơ khách | tên, SĐT (duy nhất, đã chuẩn hóa), ngày sinh, giới tính, Zalo, Facebook, nguồn khách, nghề nghiệp, dịch vụ quan tâm, ghi chú sức khỏe/sở thích, trạng thái chăm sóc | FR-CUS-01..03 |
| `customer_contact` | Lần liên hệ chăm sóc | khách, ngày, kênh, kết quả, ngày hẹn tiếp, ghi chú | FR-CUS-06 |
| `customer_package` | Gói liệu trình khách đã mua | khách, tên gói, nhóm dịch vụ (để báo cáo), số buổi mua, buổi tặng, giá, ngày mua, hạn dùng | FR-PKG-01..04 |

*Các giá trị như số lần đến, tổng chi tiêu, AOV, số ngày chưa quay lại, nhóm cảnh báo, nhóm khách **không lưu**, được tính khi truy vấn (BR-11).*

## Giao dịch và sổ

| Thực thể | Mục đích | Trường chính | Yêu cầu liên quan |
|---|---|---|---|
| `visit` | Một lần khách đến | ngày làm việc, khách (tùy chọn), hình thức thanh toán, nguồn khách, ghi chú, trạng thái (ACTIVE, VOID) | FR-VIS-01, 04, 07, 11 |
| `visit_item` | Dòng bán | giao dịch, loại (dịch vụ, combo, bán gói, dùng gói), mã tham chiếu, **tên chụp lại**, **giá niêm yết chụp lại**, phần trăm giảm (**tiền giảm không lưu**, tính qua view, BR-04), người thực hiện, **tiền tour chụp lại**, gói được dùng (nếu có) | FR-VIS-02, 03, 05, 06 |
| `expense` | Chi phí | ngày, loại, số tiền, nội dung, trạng thái | FR-EXP-01..03 |
| `daily_closing` | Ảnh chụp tổng kết bất biến của một ngày **đã chốt** (có hàng = đã chốt) | ngày (khóa chính),  tổng doanh thu, giảm giá, chi mua hàng, chi CTV, chi khác, tổng chi, thực nhận, tiền khách thực trả theo hình thức thanh toán, người chốt, thời điểm chốt | FR-CLS-01..06 |
| `adjustment` | Bút toán điều chỉnh (chỉ thêm, không sửa) | ngày ghi nhận (ngày đang mở gần nhất), ngày gốc, loại (thu, chi), số tiền (có dấu), lý do, tham chiếu dòng gốc | FR-CLS-04 |

## Đồng bộ, tiền công và hệ thống

| Thực thể | Mục đích | Trường chính | Yêu cầu liên quan |
|---|---|---|---|
| `sync_job` | Tác vụ đồng bộ Google Sheets của một ngày | ngày, trạng thái, số lần thử, lỗi gần nhất, mã lô, thời điểm thành công, kết quả đối soát | FR-SYN-01..06 |
| `staff_payment` | Khoản đã trả cho nhân viên | nhân viên, ngày, số tiền, loại (trả trong ngày, tạm ứng, trả cuối tháng), ghi chú | FR-PAY-02 |
| `marketing_spend` | Chi phí quảng cáo | ngày, kênh, chi phí, lead, đặt lịch, khách đến, doanh thu | FR-ADS-01 |
| `app_user` | Tài khoản | email (duy nhất), mật khẩu băm, vai trò, trạng thái khóa, số lần sai | FR-AUTH-01..07 |
| `refresh_token`, `password_reset_token` | Phiên và đặt lại mật khẩu | băm token, hạn dùng, đã dùng | FR-AUTH-02, 03 |
| `audit_log` | Nhật ký thay đổi (chỉ thêm) | người thực hiện, thời điểm, loại và mã đối tượng, hành động, giá trị trước và sau (JSON) | FR-SYS-01 |
| `app_setting` | Cấu hình | khóa, giá trị | FR-SYS-04 |

## Các điểm thiết kế cần nhớ

- **Snapshot trong `visit_item`:** tên, giá, tiền tour được sao chép tại lúc bán (BR-03). Tham chiếu đến `service`/`combo` chỉ để thống kê, không dùng để tính lại tiền.
- **`daily_closing` chỉ có hàng cho ngày đã chốt:** ngày chưa có hàng là OPEN. Khóa dữ liệu thực thi bằng trigger ở cơ sở dữ liệu (BR-07).
- **Bổ sung từ thiết kế ERD:** `expense_category` (loại chi phí PURCHASE, CTV, OTHER), `customer_source` (nguồn khách), `service_price_history`, `combo_item`, `staff`; `adjustment` có cờ `voids_visit` (OQ-16).
- **`adjustment` và `audit_log` chỉ thêm:** cấp quyền ghi cho tài khoản ứng dụng chỉ gồm INSERT và SELECT.
- **Giá trị phân loại** (hình thức thanh toán, loại chi phí, nguồn khách) lưu dưới dạng danh mục hoặc kiểu liệt kê, thay cho chuỗi nhập tay, để tránh lỗi gõ khác nhau như ở Excel (P4).
