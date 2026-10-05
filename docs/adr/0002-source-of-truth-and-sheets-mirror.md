# ADR-0002: Cơ sở dữ liệu là nguồn gốc, Google Sheets là bản sao

- **Trạng thái:** Accepted (chi tiết cấu trúc Sheets còn chờ OQ-09). Cách sửa sổ đã chốt xem ADR-0003
- **Ngày:** 2026-10-01
- **Người quyết định:** nhóm phát triển

## Bối cảnh

Ý tưởng ban đầu: người dùng nhập dữ liệu vào cơ sở dữ liệu trong ngày, sửa được nếu sai sót. Cuối ngày (hoặc cuối tuần) thì **chốt sổ**, sau đó dữ liệu không còn sửa được và được đẩy lên Google Sheets. Lý do muốn dùng bên thứ ba là giảm tải cho máy chủ, nhưng gọi trực tiếp Google trong lúc nhập liệu thì quá chậm.

## Quyết định

1. **PostgreSQL là nguồn dữ liệu gốc duy nhất.** Mọi thao tác nhập và đọc đều đi qua cơ sở dữ liệu, nên nhanh và không phụ thuộc Google.
2. **Việc khóa dữ liệu thực thi trong hệ thống** (tầng API và tầng cơ sở dữ liệu), không dựa vào Google Sheets. Nếu chỉ dựa vào Sheets thì ai mở Sheets vẫn sửa được số mà hệ thống không biết.
3. **Google Sheets là bản sao chỉ để xem và lưu dự phòng**, đi một chiều từ cơ sở dữ liệu sang Sheets sau khi chốt sổ. Sửa trên Sheets không ảnh hưởng hệ thống.
4. **Chốt sổ theo ngày**, tổng hợp tuần và tháng từ các ngày đã chốt.
5. **Đồng bộ chạy nền**, không nằm trong luồng người dùng chờ. Có thử lại khi lỗi, có tính idempotent, có đối soát số dòng và tổng tiền sau khi đẩy.
6. **Sửa sai sau khi chốt bằng bút toán điều chỉnh** (giữ nguyên dòng gốc), thay vì sửa trực tiếp. Không có "mở lại sổ" (xem ADR-0003).

## Lý do

- Việc "giảm tải cho máy chủ" không phải lợi ích chính: với vài chục giao dịch mỗi ngày, VPS hiện tại xử lý dư sức. Lợi ích thật của Sheets là (a) có **một bản sao nằm ngoài VPS**, nên máy chủ hỏng cũng không mất sổ, và (b) chủ tiệm đã quen xem số liệu trên bảng tính.
- Tách đồng bộ ra tác vụ nền giải quyết vấn đề tốc độ mà nhóm đã nhận ra. Chốt sổ phản hồi ngay, Sheets cập nhật sau.
- Bút toán điều chỉnh là cách kế toán chuẩn để giữ lịch sử, thay vì ghi đè số.

## Hệ quả

- Cần bảng `sync_job` và worker, cần xử lý giới hạn tốc độ của Google Sheets API (gom ghi theo lô).
- Cần quản lý khóa dịch vụ Google (service account) an toàn qua biến môi trường.
- Dữ liệu cá nhân của khách đưa lên Sheets cần cân nhắc (NFR-PRV-04, OQ-09).
- Sheets không thay thế sao lưu: vẫn phải sao lưu cơ sở dữ liệu hằng đêm ra ngoài VPS (NFR-REL-01).

## Phương án đã loại

- **Ghi trực tiếp Google Sheets làm nơi lưu chính:** chậm, bị giới hạn tốc độ, khó truy vấn và đối soát, không có ràng buộc dữ liệu.
- **Chỉ khóa trên Sheets:** không thực sự khóa được (xem quyết định 2).
