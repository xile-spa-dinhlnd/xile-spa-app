# 3. Yêu cầu chức năng

Cách đọc: mỗi module có một bảng tóm tắt (mã, yêu cầu, độ ưu tiên, đợt phát hành), tiếp theo là phần **chi tiết** (user story và tiêu chí chấp nhận, viết tắt **AC**) cho các yêu cầu quan trọng. Các yêu cầu còn lại sẽ được viết chi tiết ở sprint planning trước khi làm.

Đợt phát hành: **MVP** = bản đầu tiên chủ tiệm dùng thật (chưa đồng bộ Google Sheets, dữ liệu vẫn được sao lưu hằng đêm theo NFR-REL-01); **R2** = đợt 2; **R3** = đợt 3.

Mọi yêu cầu có trạng thái ban đầu là `Draft`. Quy tắc nghiệp vụ nhắc tới dưới dạng `BR-xx` nằm ở [04-business-rules.md](04-business-rules.md).

---

## 3.1 Xác thực (AUTH)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-AUTH-01 | Đăng nhập bằng email và mật khẩu | M | MVP |
| FR-AUTH-02 | Duy trì phiên (access token ngắn hạn, refresh token) và đăng xuất | M | MVP |
| FR-AUTH-03 | Quên mật khẩu: gửi email chứa liên kết đặt lại, có hạn dùng và chỉ dùng một lần | M | MVP |
| FR-AUTH-04 | Đổi mật khẩu khi đã đăng nhập | M | MVP |
| FR-AUTH-05 | Chống dò mật khẩu: giới hạn số lần đăng nhập sai, khóa tạm thời | S | MVP |
| FR-AUTH-06 | Xác thực hai bước (2FA) | C | R3 |
| FR-AUTH-07 | Khởi tạo tài khoản chủ tiệm lần đầu bằng lệnh hoặc biến môi trường. Không có trang đăng ký công khai | M | MVP |

**FR-AUTH-01 Đăng nhập**
- *Story:* Là chủ tiệm, tôi muốn đăng nhập bằng email và mật khẩu để vào hệ thống an toàn.
- *AC:*
  - Đúng thông tin: vào trang chủ, nhận phiên đăng nhập.
  - Sai thông tin: hiện thông báo chung "Email hoặc mật khẩu không đúng", không tiết lộ email có tồn tại hay không.
  - Mật khẩu không bao giờ được lưu hay ghi log ở dạng gốc.
  - Chưa đăng nhập thì mọi trang và API (trừ đăng nhập, quên mật khẩu) bị từ chối.

**FR-AUTH-03 Quên mật khẩu**
- *Story:* Là chủ tiệm, tôi muốn tự đặt lại mật khẩu khi quên để không phụ thuộc người phát triển.
- *AC:*
  - Nhập email: luôn trả về cùng một thông báo "Nếu email tồn tại, chúng tôi đã gửi hướng dẫn", dù email có tồn tại hay không.
  - Liên kết đặt lại hết hạn sau 30 phút (giá trị cấu hình được) và chỉ dùng được một lần.
  - Sau khi đặt lại thành công, toàn bộ phiên cũ bị thu hồi.
  - Mật khẩu mới phải đạt chính sách ở NFR-SEC-03.

---

## 3.2 Dịch vụ (SVC)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-SVC-01 | Danh sách dịch vụ, tìm theo tên, lọc theo nhóm và trạng thái (đang bán, ngừng bán) | M | MVP |
| FR-SVC-02 | Tạo dịch vụ: tên (duy nhất), nhóm, thời lượng, giá niêm yết, tiền tour mặc định | M | MVP |
| FR-SVC-03 | Sửa dịch vụ. Khi đổi giá thì lưu lịch sử giá, giao dịch cũ không bị ảnh hưởng | M | MVP |
| FR-SVC-04 | Ngừng bán hoặc bán lại dịch vụ. Dịch vụ đã có giao dịch không được xóa cứng | M | MVP |
| FR-SVC-05 | Quản lý nhóm dịch vụ (Gội đầu dưỡng sinh, Chăm sóc da, Stretching/Rehab, Khác) | S | MVP |
| FR-SVC-06 | Xem lịch sử thay đổi giá của một dịch vụ | S | R2 |

**FR-SVC-02 Tạo dịch vụ**
- *Story:* Là chủ tiệm, tôi muốn thêm một dịch vụ mới vào bảng giá để chọn nó khi ghi giao dịch.
- *AC:*
  - Bắt buộc: tên, nhóm, giá niêm yết (số nguyên ≥ 0, đơn vị đồng). Tên là duy nhất, không phân biệt hoa thường và khoảng trắng thừa (`GỘI 60P` và `GỘI 60p` bị coi là trùng).
  - Tiền tour mặc định là tùy chọn, mặc định 0.
  - Có thể tạo dịch vụ chưa có giá (giá = 0) và cập nhật sau, vì bảng giá hiện tại có hạng mục để trống.
  - Tạo thành công thì dịch vụ xuất hiện ngay ở danh sách và ở ô chọn dịch vụ khi nhập giao dịch.

**FR-SVC-03 Sửa dịch vụ và đổi giá**
- *AC:*
  - Đổi giá tạo một bản ghi lịch sử (giá cũ, giá mới, thời điểm, người đổi).
  - Giao dịch đã ghi trước đó giữ nguyên giá tại thời điểm bán (BR-03).
  - Đổi tên dịch vụ không làm đổi tên trong các giao dịch cũ (đã chụp lại, BR-03).

---

## 3.3 Combo (CMB)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-CMB-01 | Danh sách combo, tìm kiếm, lọc theo trạng thái | M | MVP |
| FR-CMB-02 | Tạo combo: tên, giá combo, danh sách dịch vụ thành phần và số lượng | M | MVP |
| FR-CMB-03 | Hiển thị tổng giá lẻ của các dịch vụ thành phần, giá combo và mức tiết kiệm | M | MVP |
| FR-CMB-04 | Sửa combo. Thay đổi không ảnh hưởng giao dịch đã ghi | M | MVP |
| FR-CMB-05 | Ngừng bán hoặc bán lại combo | M | MVP |
| FR-CMB-06 | Tiền tour của combo (theo combo hoặc chia theo dịch vụ thành phần). Chờ OQ-06 | C | R2 |

**FR-CMB-02 Tạo combo**
- *Story:* Là chủ tiệm, tôi muốn gom nhiều dịch vụ thành combo với một giá riêng để bán nhanh.
- *AC:*
  - Combo phải có ít nhất một dịch vụ thành phần. Mỗi thành phần có số lượng ≥ 1.
  - Chỉ chọn được dịch vụ đang bán.
  - Giá combo là số nguyên ≥ 0.
  - Trong một combo, không lặp cùng một dịch vụ ở hai dòng (gộp bằng số lượng).

**FR-CMB-03 Tiết kiệm của combo**
- *AC:* Hiển thị `tổng giá lẻ = Σ(giá dịch vụ × số lượng)`, `tiết kiệm = tổng giá lẻ − giá combo`. Nếu giá combo cao hơn tổng giá lẻ thì hiện cảnh báo nhưng vẫn cho lưu.

---

## 3.4 Khách hàng (CUS)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-CUS-01 | Tạo và sửa khách: tên, SĐT (duy nhất), ngày sinh, giới tính, Zalo, Facebook, nguồn khách, nghề nghiệp, dịch vụ quan tâm, ghi chú sức khỏe và sở thích | M | MVP |
| FR-CUS-02 | Tìm khách nhanh theo tên hoặc SĐT (dùng khi nhập giao dịch) | M | MVP |
| FR-CUS-03 | Hồ sơ khách 360: lịch sử giao dịch, số lần đến, tổng chi tiêu, AOV, ngày đầu và ngày cuối đến, số ngày chưa quay lại | M | MVP |
| FR-CUS-04 | Nhóm cảnh báo theo số ngày chưa quay lại (0–14, 15–30, 31–45, 46–60, trên 60) | S | R2 |
| FR-CUS-05 | Phân nhóm khách (mới, thân thiết, VIP) theo ngưỡng cấu hình được | S | R2 |
| FR-CUS-06 | Danh sách khách cần chăm sóc và ghi nhận lần liên hệ (ngày, kênh, kết quả, ngày hẹn tiếp) | S | R2 |
| FR-CUS-07 | Nhắc sinh nhật trong 7 ngày tới | S | R2 |
| FR-CUS-08 | Trạng thái chăm sóc (Mới, Đang chăm sóc, Đã đặt lịch, Đã mua gói, VIP, Ngủ đông, Mất khách) | C | R2 |
| FR-CUS-09 | Gộp hai hồ sơ khách trùng | C | R3 |
| FR-CUS-10 | Ẩn hoặc xóa hồ sơ khách theo yêu cầu của khách (quyền riêng tư), giữ lại số liệu doanh thu dạng ẩn danh | S | R2 |

**FR-CUS-01 Tạo và sửa khách**
- *Story:* Là chủ tiệm, tôi muốn lưu thông tin khách để theo dõi và chăm sóc lại.
- *AC:*
  - Bắt buộc: tên và SĐT. Các trường còn lại tùy chọn.
  - SĐT được chuẩn hóa về một định dạng thống nhất (BR-10) và là duy nhất. Nhập SĐT đã tồn tại thì báo và gợi ý mở hồ sơ hiện có.
  - Có thể ghi nhận giao dịch cho khách vãng lai mà không tạo hồ sơ (xem OQ-08).

**FR-CUS-03 Hồ sơ khách 360**
- *AC:*
  - Số lần đến, tổng chi tiêu và AOV được tính từ các giao dịch chưa bị hủy. Không nhập tay.
  - Số ngày chưa quay lại được tính theo ngày hiện tại (múi giờ Việt Nam) khi xem, không lưu cố định (BR-11).
  - Hồ sơ liệt kê giao dịch theo thứ tự mới nhất trước, có phân trang.

---

## 3.5 Gói liệu trình (PKG)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-PKG-01 | Bán gói cho khách: tên gói, số buổi mua, buổi tặng, giá, hạn dùng | S | R2 |
| FR-PKG-02 | Dùng một buổi của gói trong giao dịch, tự động trừ buổi | S | R2 |
| FR-PKG-03 | Trạng thái gói (Đang dùng, Hết buổi, Hết hạn) và cảnh báo sắp hết buổi, sắp hết hạn | S | R2 |
| FR-PKG-04 | Danh sách gói cần chăm sóc | S | R2 |

*Đã chốt (OQ-04, BR-20): doanh thu của gói được tính đủ vào ngày bán gói. Mỗi buổi dùng từ gói là một lượt đến có giá tính doanh thu = 0. Gói gắn với một nhóm dịch vụ để báo cáo. Chi tiết tiêu chí chấp nhận sẽ viết ở sprint planning.*

---

## 3.6 Giao dịch (VIS)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-VIS-01 | Tạo giao dịch: ngày (mặc định hôm nay), khách (không bắt buộc), nhiều dòng bán | M | MVP |
| FR-VIS-02 | Dòng bán chọn dịch vụ hoặc combo. Giá niêm yết tự điền và được chụp lại tại thời điểm bán | M | MVP |
| FR-VIS-03 | Giảm giá theo phần trăm cho từng dòng. Hệ thống tự tính tiền giảm (giá × %) | M | MVP |
| FR-VIS-04 | Chọn hình thức thanh toán: chuyển khoản hoặc tiền mặt | M | MVP |
| FR-VIS-05 | Chọn người thực hiện (KTV hoặc chủ tiệm) cho từng dòng bán | M | MVP |
| FR-VIS-06 | Lưu tiền tour tại thời điểm bán, tự điền từ dịch vụ và chỉnh tay được | M | MVP |
| FR-VIS-07 | Sửa hoặc hủy giao dịch khi ngày chưa chốt. Hủy là đánh dấu, không xóa cứng | M | MVP |
| FR-VIS-08 | Danh sách giao dịch theo ngày kèm tổng của ngày | M | MVP |
| FR-VIS-09 | Kiểm tra dữ liệu khi nhập (thiếu thanh toán, thiếu dịch vụ, giảm giá ngoài 0–100, ngày không hợp lệ) | M | MVP |
| FR-VIS-10 | Giao diện nhập nhanh tối ưu cho điện thoại và iPad (nút lớn, ít bước) | S | MVP |
| FR-VIS-11 | Ghi chú cho giao dịch và nguồn khách của giao dịch | S | MVP |
| FR-VIS-12 | Bán gói (giá tính doanh thu = giá gói, tính vào doanh thu ngày bán) và dùng gói (giá tính doanh thu = 0, trừ một buổi) trong giao dịch, theo BR-20 | S | R2 |

**FR-VIS-01 Tạo giao dịch**
- *Story:* Là chủ tiệm, tôi muốn ghi nhanh một lần khách đến để doanh thu và lịch sử khách được cập nhật cùng lúc.
- *AC:*
  - Ngày mặc định là hôm nay theo giờ Việt Nam. Có thể chọn ngày trong quá khứ nếu ngày đó chưa chốt. Không cho chọn ngày tương lai.
  - Không cho chọn ngày đã chốt (BR-06).
  - Một giao dịch có ít nhất một dòng bán.
  - Lưu thành công thì giao dịch xuất hiện trong danh sách của ngày và trong hồ sơ khách (nếu có chọn khách).

**FR-VIS-03 Giảm giá theo phần trăm**
- *Story:* Là chủ tiệm, tôi muốn nhập giảm giá theo phần trăm vì đó là cách tôi quen dùng.
- *AC:*
  - Nhập phần trăm trong khoảng 0 đến 100. Giá trị ngoài khoảng bị từ chối.
  - Hệ thống tính `tiền giảm` theo BR-04 và hiển thị ngay khi nhập, kèm số tiền khách phải trả.
  - Cả phần trăm và số tiền giảm đều được lưu. Doanh thu về sau tính từ số tiền đã lưu, không tính lại từ phần trăm.

**FR-VIS-07 Sửa và hủy giao dịch**
- *AC:*
  - Giao dịch của ngày OPEN: sửa được mọi trường, mỗi lần sửa ghi nhật ký (FR-SYS-01).
  - Giao dịch của ngày CLOSED: mọi thao tác sửa và hủy bị từ chối ở cả API lẫn cơ sở dữ liệu. Hiển thị gợi ý dùng bút toán điều chỉnh (FR-CLS-04).
  - Hủy giao dịch: giao dịch chuyển sang trạng thái VOID, không tính vào doanh thu, vẫn xem được.

---

## 3.7 Chi phí (EXP)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-EXP-01 | Ghi chi phí: ngày, loại (mua hàng, CTV, khác), số tiền, nội dung | M | MVP |
| FR-EXP-02 | Sửa hoặc hủy chi phí khi ngày chưa chốt | M | MVP |
| FR-EXP-03 | Danh sách chi phí theo ngày và theo tháng | M | MVP |

*Loại chi phí nhập tay mặc định: mua hàng, CTV, khác (danh mục thêm được). Giảm giá không nhập tay: hệ thống tự tính từ các dòng bán và cộng vào Tổng chi (BR-05).*

---

## 3.8 Chốt sổ (CLS)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-CLS-01 | Xem trước bản tổng kết ngày trước khi chốt: tổng doanh thu, giảm giá, chi mua hàng, chi CTV, tổng chi, thực nhận, và tiền khách thực trả theo hình thức thanh toán | M | MVP |
| FR-CLS-02 | Chốt sổ một ngày: khóa dữ liệu và lưu ảnh chụp tổng kết, trong một transaction | M | MVP |
| FR-CLS-03 | Chặn mọi sửa đổi dữ liệu của ngày đã chốt, ở tầng API và tầng cơ sở dữ liệu | M | MVP |
| FR-CLS-04 | Bút toán điều chỉnh cho ngày đã chốt: số tiền cộng hoặc trừ, lý do bắt buộc, tham chiếu dòng gốc | M | MVP |
| FR-CLS-05 | Cảnh báo khi còn ngày trước đó chưa chốt | S | MVP |
| FR-CLS-06 | Lịch sử chốt sổ: ai chốt, lúc nào | M | MVP |
| FR-CLS-07 | ~~Mở lại sổ có lý do và ghi nhật ký~~ (Đã hủy: quyết định không làm, xem ADR-0003) | W | — |
| FR-CLS-08 | Tổng hợp tuần và tháng từ các ngày đã chốt | C | R2 |

**FR-CLS-02 Chốt sổ**
- *Story:* Là chủ tiệm, tôi muốn chốt sổ cuối ngày để số liệu của ngày đó không bị thay đổi ngoài ý muốn.
- *AC:*
  - Chỉ chốt được ngày không ở tương lai. Ngày đã chốt rồi thì không chốt lại.
  - Trước khi chốt, hệ thống hiển thị bản tổng kết (FR-CLS-01) và yêu cầu xác nhận.
  - Khi chốt: trạng thái ngày chuyển sang CLOSED, tổng kết được lưu thành ảnh chụp bất biến, ghi người chốt và thời điểm. Tất cả diễn ra trong một transaction: hoặc xong hết hoặc không gì thay đổi.
  - Nếu sau khi chốt thành công mà tác vụ đồng bộ Google Sheets lỗi thì ngày vẫn ở trạng thái CLOSED (đồng bộ là việc riêng, xem 3.9).

**FR-CLS-03 Khóa dữ liệu**
- *AC:*
  - Mọi lệnh tạo, sửa, hủy giao dịch và chi phí mang ngày đã chốt trả về lỗi rõ ràng.
  - Có kiểm thử tự động chứng minh việc khóa hoạt động cả khi gọi trực tiếp tầng dữ liệu, không chỉ qua giao diện.

**FR-CLS-04 Bút toán điều chỉnh**
- *Story:* Là chủ tiệm, khi phát hiện sai sót ở ngày đã chốt, tôi muốn ghi một khoản điều chỉnh có lý do thay vì sửa trực tiếp.
- *AC:*
  - Một bút toán gồm: ngày ghi nhận, ngày gốc được điều chỉnh, loại (doanh thu hoặc chi phí), số tiền (dương hoặc âm), lý do (bắt buộc), tham chiếu giao dịch hoặc chi phí gốc (tùy chọn).
  - Không sửa và không xóa được bút toán sau khi tạo. Sai thì ghi bút toán đảo.
  - Báo cáo hiển thị bút toán điều chỉnh tách riêng để thấy được sự thay đổi.
  - Bút toán được ghi nhận vào ngày đang mở gần nhất, kèm ngày gốc (BR-08). Giao diện nói rõ "khoản này sẽ được ghi nhận vào ngày dd/MM" trước khi lưu.
  - Bút toán chỉ sửa số tiền. Việc loại một lượt đến nhập nhầm khỏi thống kê khách còn để ngỏ (OQ-16).

---

## 3.9 Đồng bộ Google Sheets (SYN)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-SYN-01 | Sau khi chốt sổ, đẩy dữ liệu của ngày đó lên Google Sheets bằng tác vụ chạy nền | M | R2 |
| FR-SYN-02 | Hiển thị trạng thái đồng bộ của từng ngày (chờ, đang chạy, thành công, lỗi) | M | R2 |
| FR-SYN-03 | Tự động thử lại khi lỗi, có giãn cách tăng dần, ghi lý do lỗi | M | R2 |
| FR-SYN-04 | Chạy lại không tạo dòng trùng (idempotent) | M | R2 |
| FR-SYN-05 | Đối soát sau khi đẩy: số dòng và tổng tiền trên Sheets khớp với cơ sở dữ liệu | S | R2 |
| FR-SYN-06 | Đồng bộ lại thủ công cho một ngày | S | R2 |
| FR-SYN-07 | Cấu hình spreadsheet đích và thông tin xác thực qua biến môi trường, không lưu bí mật trong cơ sở dữ liệu | M | R2 |

**FR-SYN-01 Đẩy dữ liệu lên Google Sheets**
- *Story:* Là chủ tiệm, tôi muốn số liệu đã chốt có thêm một bản trên Google Sheets để xem quen thuộc và có bản sao ngoài máy chủ.
- *AC:*
  - Việc chốt sổ trả kết quả cho người dùng ngay, không chờ Google. Đồng bộ chạy nền.
  - Chỉ đẩy ngày đã chốt (BR-15). Dữ liệu đi một chiều từ cơ sở dữ liệu sang Sheets.
  - Mỗi tháng có một spreadsheet (cấu trúc chốt ở OQ-09).
  - Lỗi từ Google (quá tải, hết quyền, mất mạng) không làm mất dữ liệu và không làm hỏng ngày đã chốt.

**FR-SYN-04 Idempotent**
- *AC:* Chạy đồng bộ cùng một ngày nhiều lần cho kết quả giống chạy một lần (không trùng dòng). Có kiểm thử cho trường hợp job bị gián đoạn giữa chừng rồi chạy lại.

---

## 3.10 Dashboard (DSH)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-DSH-01 | Doanh thu theo ngày, tuần, tháng trong khoảng ngày chọn | M | MVP |
| FR-DSH-02 | So sánh với kỳ trước (tháng trước, cùng kỳ) | S | R2 |
| FR-DSH-03 | Top dịch vụ theo doanh thu và theo số lượt (số lượt gồm cả buổi dùng từ gói) | M | MVP |
| FR-DSH-04 | Doanh thu theo nguồn khách | S | R2 |
| FR-DSH-05 | Doanh thu theo người thực hiện | S | R2 |
| FR-DSH-06 | Cơ cấu thanh toán chuyển khoản và tiền mặt | S | MVP |
| FR-DSH-07 | Tổng doanh thu, giảm giá, tổng chi và **Thực nhận** (= tổng doanh thu − tổng chi) theo kỳ, tính theo BR-05 | M | MVP |
| FR-DSH-08 | Thẻ chỉ số CRM: tổng khách, khách trên 30 ngày chưa quay lại, sinh nhật 7 ngày, gói cần chăm | S | R2 |
| FR-DSH-09 | Đánh dấu rõ phần số liệu thuộc ngày chưa chốt | M | MVP |

**FR-DSH-01 Doanh thu theo kỳ**
- *Story:* Là chủ tiệm, tôi muốn xem doanh thu theo từng ngày trong tháng để biết tiệm đang tốt hay xấu.
- *AC:*
  - Chọn khoảng ngày (mặc định tháng hiện tại). Biểu đồ và bảng cùng khớp số liệu.
  - Số liệu là *tổng doanh thu* theo BR-05, đã gồm bút toán điều chỉnh.
  - Tổng trên dashboard khớp tổng của danh sách giao dịch (có kiểm thử đối chiếu).
  - Hiển thị tốt trên màn hình 360px trở lên (NFR-USA-01).

**FR-DSH-09 Dữ liệu chưa chốt**
- *AC:* Ngày chưa chốt được đánh dấu khác biệt (ví dụ nhãn "Chưa chốt"). Có lựa chọn ẩn hoặc hiện số liệu chưa chốt (xem OQ-14).

---

## 3.11 Nhân viên (STF), tiền tour và lương (PAY)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-STF-01 | Quản lý nhân viên: tên, vai trò (KTV, chủ tiệm), đang làm hoặc đã nghỉ | M | MVP |
| FR-PAY-01 | Báo cáo tiền tour phát sinh theo nhân viên và theo kỳ (từ các dòng bán) | S | R3 |
| FR-PAY-02 | Ghi nhận khoản đã trả cho nhân viên: ngày, người nhận, số tiền, loại (trả trong ngày, tạm ứng, trả cuối tháng) | S | R3 |
| FR-PAY-03 | Số còn nợ = tiền tour phát sinh − đã trả | S | R3 |
| FR-PAY-04 | Phiếu lương theo tháng, xem và xuất PDF | S | R3 |

**FR-PAY-02 và FR-PAY-03 Trả lương linh hoạt**
- *Story:* Là chủ tiệm, tôi có hôm trả lương ngay trong ngày, có hôm trả cuối tháng, tôi muốn hệ thống theo dõi được cả hai cách.
- *AC:*
  - Tiền tour phát sinh lấy tự động từ các dòng bán (BR-13), không nhập tay.
  - Mỗi lần trả tiền là một dòng riêng, độc lập với việc phát sinh (BR-14).
  - Xem được số còn nợ của từng nhân viên tại bất kỳ ngày nào.
  - Phiếu lương cuối tháng chỉ là báo cáo tổng hợp, không phải một cách nhập liệu khác.
  - *Lưu ý:* dữ liệu cho FR-PAY được thu thập từ MVP (FR-VIS-05, FR-VIS-06) để không phải sửa dữ liệu cũ sau này.

---

## 3.12 Nhập dữ liệu từ Excel (IMP)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-IMP-01 | Nhập danh sách dịch vụ và giá từ file Excel hiện tại (sheet `LIST K XOÁ`) | S | MVP |
| FR-IMP-02 | Nhập khách hàng và lịch sử dịch vụ từ file CRM, có báo cáo lỗi theo dòng. Không nhập cột "THỰC NHẬN" cũ mà tính lại theo BR-05 | S | R2 |
| FR-IMP-03 | Xem trước kết quả nhập và hoàn tác một lần nhập | C | R2 |

---

## 3.13 Quảng cáo (ADS) và báo cáo (RPT)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-ADS-01 | Ghi chi phí quảng cáo theo ngày và kênh: chi phí, lead, đặt lịch, khách đến | C | R3 |
| FR-ADS-02 | Tính CPA/lead, CPA/khách đến, tỷ lệ đến, ROAS | C | R3 |
| FR-RPT-01 | Xuất báo cáo doanh thu theo tháng ra Excel | C | R3 |
| FR-RPT-02 | Xuất báo cáo ra PDF | C | R3 |

---

## 3.14 Hệ thống (SYS)

| ID | Yêu cầu | Ưu tiên | Đợt |
|---|---|---|---|
| FR-SYS-01 | Nhật ký thay đổi (audit log): ai, khi nào, đối tượng nào, giá trị cũ và mới | M | MVP |
| FR-SYS-02 | Sao lưu tự động hằng đêm và tài liệu hướng dẫn khôi phục | M | MVP |
| FR-SYS-03 | Điểm kiểm tra sức khỏe hệ thống (health endpoint) cho giám sát | S | MVP |
| FR-SYS-04 | Màn hình cấu hình (ngưỡng phân nhóm khách, mốc cảnh báo, tên tiệm) | S | R2 |

**FR-SYS-01 Nhật ký thay đổi**
- *AC:*
  - Ghi lại mọi thao tác tạo, sửa, hủy trên: dịch vụ, combo, giao dịch, chi phí, chốt sổ, bút toán điều chỉnh, tài khoản.
  - Mỗi bản ghi có: người thực hiện, thời điểm, loại đối tượng, mã đối tượng, giá trị trước và sau (dạng JSON).
  - Nhật ký không có chức năng sửa hay xóa trên giao diện.
