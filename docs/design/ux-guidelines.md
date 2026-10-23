# Hướng dẫn giao diện và câu chữ

Người dùng chính là chủ tiệm: không rành máy tính, dùng điện thoại lúc đang đứng quầy, iPad và máy tính khi rảnh tay
làm sổ cuối ngày. Mỗi màn hình phải dùng được ngay mà không cần hướng dẫn. File này là chuẩn để thiết kế (mục "Màn
hình" của file luồng, `docs/design/flows/README.md`) và để review. Kỹ thuật (Ant Design, CSS Modules, token):
`frontend/AGENTS.md`.

## 1. Bố cục theo cỡ màn hình

| Cỡ                       | Breakpoint Ant Design | Danh sách                    | Form                                   |
| ------------------------ | --------------------- | ---------------------------- | -------------------------------------- |
| Điện thoại (từ 375 px)   | dưới `md` (768 px)    | thẻ (`Card`), mỗi thẻ một bản ghi | drawer toàn màn hình (`size="100%"`) |
| iPad, máy tính           | từ `md`               | bảng (`Table`)               | drawer 480 px                          |

- Thiết kế cho điện thoại trước: thứ quan trọng nhất (tên, số tiền, trạng thái) nằm trên cùng của thẻ.
- Không cuộn ngang ở điện thoại. Bảng nhiều cột thì chuyển sang thẻ, không thu nhỏ chữ.
- Nút chính (Thêm, Lưu) dùng `size="large"`; vùng chạm tối thiểu 44 × 44 px; hai nút cạnh nhau cách ít nhất
  `--xl-space-sm`.
- Nút hành động chính của trang đặt ở góc trên bên phải tiêu đề; ở điện thoại xuống dòng dưới tiêu đề (`Space wrap`).

## 2. Bốn trạng thái của mọi màn hình có dữ liệu

| Trạng thái  | Hiển thị                                                                                             |
| ----------- | ---------------------------------------------------------------------------------------------------- |
| Đang tải    | `Spin` hoặc `Skeleton` đúng chỗ dữ liệu sẽ hiện; không để trang trắng                                |
| Lỗi         | `Alert` loại `error`, tiêu đề "Không tải được <cái gì>", nút **"Thử lại"**; không kèm trạng thái trống |
| Trống       | `Empty` nói rõ vì sao trống: "Chưa có <cái gì> nào" hoặc "Không có kết quả khớp bộ lọc" kèm "Xóa bộ lọc" |
| Có dữ liệu  | danh sách, phân trang khi cần                                                                        |

Thiết kế ghi đủ cả bốn; test có ít nhất ca lỗi và ca trống.

## 3. Câu chữ

- Tiếng Việt có dấu, câu ngắn, giọng trung tính, lịch sự. Không dùng chữ kỹ thuật (`ID`, `null`, `request`, mã lỗi).
  Tránh xưng "bạn", "chúng tôi" khi câu vẫn rõ nghĩa mà không cần.
- **Báo thành công:** `Đã <động từ> <đối tượng>.`, kết thúc bằng dấu chấm, không dùng "!": "Đã thêm dịch vụ.", "Đã lưu
  thay đổi nhóm dịch vụ.", "Đã ngừng bán dịch vụ.". Động từ thống nhất: **thêm** (tạo mới), **lưu thay đổi** (sửa),
  **ngừng bán / bán lại** (dịch vụ, combo), **ngừng dùng / dùng lại** (nhóm, danh mục), **cho nghỉ / cho làm lại**
  (nhân viên).
- **Báo lỗi:** nói lỗi gì **và** làm gì tiếp. "Nhóm còn dịch vụ đang bán. Hãy ngừng bán hoặc chuyển các dịch vụ sang
  nhóm khác trước." chứ không "Không thể ngừng dùng nhóm.". Thông báo nghiệp vụ lấy từ backend (`message` của lỗi);
  giao diện không tự viết lại.
- **Lỗi ở ô nhập** hiện ngay dưới ô đó (`Form.Item` `help`), không dùng `message.error` cho lỗi của một ô.
- **Xác nhận** (`Popconfirm`) chỉ cho việc khó quay lại hoặc ảnh hưởng chỗ khác (ngừng bán, chốt sổ, hủy giao dịch).
  Tiêu đề là câu hỏi ("Ngừng bán dịch vụ?"); mô tả nói hệ quả ("Dịch vụ sẽ không còn để chọn khi nhập giao dịch.
  Giao dịch cũ không bị ảnh hưởng."); nút đồng ý lặp lại đúng động từ ("Ngừng bán"), nút còn lại là "Hủy". Việc
  nguy hiểm dùng `okButtonProps={{ danger: true }}`.
- **Placeholder** là ví dụ thật của tiệm ("Ví dụ: Gội 30 phút"), không lặp lại nhãn.
- Nhãn trạng thái dùng `Tag`: màu `success` cho trạng thái đang hoạt động, không màu cho trạng thái đã ngừng.

## 4. Số, tiền, ngày

- Tiền: `formatMoney` → `1.234.000 đ`; ô nhập tiền luôn là `MoneyInput`. Không tự tính tiền ở giao diện.
- Ngày: `dd/MM/yyyy` theo giờ `Asia/Ho_Chi_Minh`; thời điểm thêm `HH:mm`.
- Số tiền trong bảng canh phải để dễ so; trong thẻ ở điện thoại để cạnh nhãn.

## 5. Màu, khoảng cách

Chỉ dùng token trong `frontend/src/styles/tokens.css` (`--xl-color-brand`, `--xl-space-*`, `--xl-radius-*`) hoặc token
của Ant Design. Màu đỏ chỉ cho lỗi và việc nguy hiểm; màu thương hiệu cho hành động chính.

## 6. Khi review giao diện

- Xem ở 375, 768 và 1280 px. Không chữ tràn, không nút bị che, không cuộn ngang.
- Đủ bốn trạng thái ở mục 2; câu chữ đúng mục 3; số, ngày đúng mục 4.
- Bấm được hết bằng ngón tay ở cỡ điện thoại.

Câu chữ cũ chưa theo file này (ví dụ "Đã tạo dịch vụ.", "Đổi mật khẩu thành công!") thì sửa khi card sau đụng vào
màn hình đó, không mở card riêng chỉ để sửa chữ.
