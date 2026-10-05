# ADR-0003: Sửa sổ đã chốt bằng bút toán điều chỉnh, không mở lại sổ

- **Trạng thái:** Accepted
- **Ngày:** 2026-10-01
- **Người quyết định:** nhóm phát triển (chủ tiệm ủy quyền chọn giải pháp, xem OQ-05)

## Bối cảnh

Sau khi chốt sổ một ngày, dữ liệu của ngày đó không còn được sửa (BR-07) và được đẩy lên Google Sheets (ADR-0002). Tuy vậy thực tế vẫn có lúc phát hiện sai sót ở ngày đã chốt (nhập nhầm giá, quên một khoản chi). Cần một cách sửa mà vẫn giữ được tính bất biến của sổ.

## Các lựa chọn đã cân nhắc

| Lựa chọn | Ưu điểm | Nhược điểm |
|---|---|---|
| **A. Bút toán điều chỉnh, ghi vào ngày đang mở gần nhất (chọn)** | Ngày đã chốt không bao giờ đổi. Bản sao trên Google Sheets không cần cập nhật lại. Lịch sử rõ ràng, dễ kiểm toán. Đơn giản để cài đặt và kiểm thử | Không sửa trực tiếp được số cũ. Số liệu của ngày gốc không "đẹp" lại, báo cáo phải có cách xem theo ngày gốc |
| B. Bút toán điều chỉnh, ghi ngược vào ngày gốc | Báo cáo theo ngày gốc luôn đúng | Làm thay đổi tổng của ngày đã chốt, buộc phải đồng bộ lại Sheets và phá vỡ tính bất biến |
| C. Cho phép "mở lại sổ" (có lý do, có nhật ký) | Người dùng sửa trực tiếp, quen thuộc | Phá vỡ tính bất biến, cần tạo phiên bản và đồng bộ lại Sheets, dễ bị lạm dụng, phức tạp hơn nhiều |

## Quyết định

Chọn **phương án A**:

1. Không có thao tác mở lại sổ (BR-06).
2. Sai sót ở ngày đã chốt được sửa bằng **bút toán điều chỉnh** (số tiền có dấu, lý do bắt buộc, tham chiếu dòng gốc nếu có). Bút toán chỉ thêm, không sửa, không xóa; sai thì ghi bút toán đảo (BR-08).
3. Bút toán được **ghi nhận vào ngày đang mở gần nhất** và lưu kèm ngày gốc.
4. Báo cáo hiển thị bút toán tách riêng, có thể xem theo ngày ghi nhận (mặc định) hoặc theo ngày gốc.

## Lý do

- Giữ được một bất biến đơn giản và kiểm thử được: **ngày đã chốt không bao giờ thay đổi**. Kéo theo đó, bản sao Google Sheets của ngày đó chỉ cần đẩy một lần, không phải lo cập nhật hay xung đột.
- Đây là cách kế toán chuẩn xử lý sai sót, nên dễ giải thích với chủ tiệm và với người xem dự án.
- Chủ tiệm là người dùng duy nhất, thao tác ít, nên chi phí của việc phải ghi một bút toán là chấp nhận được.

## Hệ quả

- Cần bảng `adjustment` chỉ cho phép INSERT và SELECT (06-data-dictionary).
- Giao diện phải nói rõ "khoản này sẽ được ghi nhận vào ngày dd/MM".
- Bút toán chỉ sửa được số tiền. Việc loại một lượt đến nhập nhầm khỏi thống kê khách còn để ngỏ (OQ-16).
- Báo cáo cần hai cách nhìn (theo ngày ghi nhận, theo ngày gốc), có thể làm ở R2.

## Điều kiện xem xét lại

Nếu sau một thời gian dùng thật, chủ tiệm thấy bút toán quá bất tiện hoặc sai sót xảy ra thường xuyên, xem xét thêm "mở lại sổ" có nhật ký kèm cơ chế đồng bộ lại Sheets theo phiên bản (phương án C), bằng một ADR mới thay thế ADR này.
