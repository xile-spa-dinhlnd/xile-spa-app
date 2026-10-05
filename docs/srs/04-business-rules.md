# 4. Quy tắc nghiệp vụ

Các quy tắc này được rút ra từ hai file Excel tham khảo và từ các trao đổi với chủ tiệm. Excel chỉ là **nguồn tham khảo**, hệ thống mới không bắt buộc giữ nguyên cách làm trong Excel. Quy tắc đánh dấu **[Cần xác nhận]** là đề xuất của nhóm, chờ chủ tiệm đồng ý (xem [07-open-questions.md](07-open-questions.md)).

## Tiền tệ và thời gian

| ID | Quy tắc |
|---|---|
| BR-01 | Mọi số tiền lưu bằng **số nguyên đồng** (VND không có phần lẻ). Không dùng số thực. Hiển thị theo định dạng Việt Nam, ví dụ `1.234.000 đ`. |
| BR-02 | Múi giờ nghiệp vụ là `Asia/Ho_Chi_Minh`. "Ngày" của giao dịch là **ngày làm việc** do người dùng chọn (mặc định hôm nay theo giờ Việt Nam), lưu tách biệt với thời điểm tạo bản ghi. Ranh giới ngày là 00:00. **[Cần xác nhận, OQ-02]** |

## Giao dịch, giá và doanh thu

| ID | Quy tắc |
|---|---|
| BR-03 | **Chụp lại giá tại thời điểm bán.** Mỗi dòng bán lưu tên dịch vụ, giá niêm yết và tiền tour tại lúc bán. Đổi giá, đổi tên hay ngừng bán dịch vụ sau đó không làm thay đổi các giao dịch đã ghi. |
| BR-04 | **Giảm giá theo phần trăm** cho từng dòng bán, trong khoảng 0 đến 100 (tối đa 2 chữ số thập phân). Hệ thống chỉ **lưu giá niêm yết đã chụp (BR-03) và phần trăm giảm**. `tiền giảm = làm tròn đến đồng (nửa lên) của giá niêm yết × phần trăm ÷ 100` và `tiền khách thực trả = giá niêm yết − tiền giảm` là các giá trị **tính ra** từ hai số trên: hiển thị khi tính tiền cho khách và dùng để đối soát, không nhập tay và không lưu thành cột riêng do ứng dụng quản lý. Tổng của ngày được đóng băng khi chốt sổ (BR-06). *(Đã chốt, OQ-01)* |
| BR-05 | **Cách tính trong kỳ:** (1) **Tổng doanh thu** = Σ giá niêm yết của các dòng bán không bị hủy, cộng bút toán điều chỉnh loại doanh thu; (2) **Chi giảm giá** = Σ tiền giảm (BR-04); (3) **Tổng chi** = chi giảm giá + chi mua hàng + chi CTV + chi khác, cộng bút toán điều chỉnh loại chi; (4) **Thực nhận = Tổng doanh thu − Tổng chi.** Cách này cho cùng kết quả với việc lấy tổng tiền khách thực trả trừ đi chi mua hàng và chi CTV, nhưng giữ đúng cách trình bày quen thuộc của chủ tiệm. Tiền khách thực trả theo từng hình thức thanh toán (CK, TM) bằng tổng giá niêm yết trừ tổng tiền giảm của hình thức đó, tính khi truy vấn để đối chiếu tiền mặt và chuyển khoản. *(Đã chốt, OQ-03 và OQ-13; công thức do nhóm và chủ tiệm thống nhất)* |
| BR-09 | Hủy giao dịch là **xóa mềm** (trạng thái VOID). Chỉ hủy được khi ngày còn mở. Giao dịch đã hủy không tính vào doanh thu, số lần đến và tổng chi tiêu của khách. |
| BR-19 | Dịch vụ hoặc combo đã có giao dịch **không được xóa cứng**, chỉ được ngừng bán. |

## Chốt sổ

| ID | Quy tắc |
|---|---|
| BR-06 | Mỗi ngày có hai trạng thái: **OPEN** (đang mở, sửa được) và **CLOSED** (đã chốt). Chỉ chuyển một chiều OPEN → CLOSED. **Không có thao tác mở lại sổ** (xem ADR-0003). Không chốt ngày ở tương lai. |
| BR-07 | Dữ liệu của ngày CLOSED là **bất biến**: không tạo, sửa, hủy giao dịch hay chi phí mang ngày đó. Việc khóa phải được thực thi ở tầng cơ sở dữ liệu, không chỉ ở giao diện. |
| BR-08 | Sửa sai cho ngày đã chốt **chỉ bằng bút toán điều chỉnh** (chỉ thêm, không sửa, không xóa; sai thì ghi bút toán đảo). Bút toán **ghi nhận vào ngày đang mở gần nhất** (kể từ hôm nay trở đi) và lưu kèm **ngày gốc** của khoản được điều chỉnh. Nhờ vậy ngày đã chốt không bao giờ thay đổi, và bản sao trên Google Sheets của ngày đó không cần cập nhật lại. Báo cáo có thể xem theo ngày ghi nhận (mặc định) hoặc theo ngày gốc. *(Đã chốt, OQ-05, xem ADR-0003)* |
| BR-15 | **Đồng bộ Google Sheets** chỉ áp dụng cho ngày CLOSED, đi một chiều từ cơ sở dữ liệu sang Sheets, chạy nền và có tính idempotent. Cơ sở dữ liệu luôn là nguồn dữ liệu gốc. Sửa trên Sheets không ảnh hưởng hệ thống. |
| BR-16 | Dashboard hiển thị cả số liệu của ngày chưa chốt nhưng phải đánh dấu rõ. **[Cần xác nhận, OQ-14]** |

## Khách hàng và chăm sóc

| ID | Quy tắc |
|---|---|
| BR-10 | Khách được xác định bằng **số điện thoại** (chuẩn hóa về một định dạng, duy nhất). Giao dịch có thể ghi cho khách vãng lai không có hồ sơ. **[Cần xác nhận, OQ-08]** |
| BR-11 | Số ngày chưa quay lại = ngày hiện tại − ngày cuối đến (theo giờ Việt Nam). **Tính khi truy vấn, không lưu**. Nhóm cảnh báo theo mốc 14, 30, 45, 60 ngày. |
| BR-17 | Phân nhóm khách đề xuất theo Excel: **VIP** nếu tổng chi tiêu ≥ 3.000.000 đ; **Khách thân thiết** nếu số lần đến ≥ 3; **Khách mới** nếu đến 1 lần. Ngưỡng cấu hình được ở R2. **[Cần xác nhận, OQ-11]** |
| BR-12 | **Gói liệu trình:** tổng buổi = số buổi mua + buổi tặng; còn lại = tổng − đã dùng. Trạng thái: *Hết buổi* khi còn lại ≤ 0; *Hết hạn* khi quá ngày hết hạn; còn lại là *Đang dùng*. Cảnh báo "sắp hết" khi còn ≤ 2 buổi. |
| BR-20 | **Doanh thu của gói được tính đủ vào ngày bán gói** (tiền thu về lúc đó). Mỗi buổi dùng từ gói là một lượt đến bình thường (tính vào số lần đến và lịch sử khách) nhưng là dòng bán có **giá tính doanh thu = 0**, không cộng thêm vào doanh thu. Gói gắn với một nhóm dịch vụ để báo cáo biết doanh thu gói thuộc mảng nào. *(Đã chốt, OQ-04)* |

## Nhân viên, tour và lương

| ID | Quy tắc |
|---|---|
| BR-13 | **Tiền tour** là tiền công của người làm trên mỗi dòng bán. Mặc định lấy từ dịch vụ, được chụp lại theo dòng bán (BR-03) và chỉnh tay được. Khi người làm là chủ tiệm thì tour bằng 0. Tour của combo và liệu trình **[Cần xác nhận, OQ-06]**. |
| BR-14 | **Tách phát sinh khỏi chi trả.** Tiền tour phát sinh được cộng dồn từ các dòng bán. Các khoản đã trả (trả trong ngày, tạm ứng, trả cuối tháng) ghi riêng. `Còn nợ = phát sinh − đã trả`. Nhờ vậy cả hai cách trả lương đều theo dõi được. |

## Kiểm soát

| ID | Quy tắc |
|---|---|
| BR-18 | Mọi thay đổi trên dữ liệu tài chính và danh mục đều được ghi nhật ký (người, thời điểm, giá trị cũ và mới). Nhật ký không sửa, không xóa được. |
