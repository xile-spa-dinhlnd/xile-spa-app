# Đặc tả yêu cầu phần mềm (SRS): Xile Spa Admin

| | |
|---|---|
| Phiên bản | 0.5 (bản nháp Sprint 0) |
| Trạng thái | Draft, chờ nhóm và chủ tiệm rà soát |
| Cách viết | Tham khảo khung ISO/IEC/IEEE 29148, rút gọn cho dự án nhỏ |

## Lịch sử thay đổi

| Phiên bản | Ngày | Thay đổi |
|---|---|---|
| 0.1 | 01/10/2026 | Bản đầu tiên |
| 0.2 | 01/10/2026 | Chốt OQ-01, 03, 04, 05, 13: làm tròn đến đồng; bỏ "chi giảm giá", đổi "Thực nhận" thành "Còn lại"; doanh thu gói tính lúc bán; sửa sổ chỉ bằng bút toán điều chỉnh (ADR-0003). Thêm OQ-16 |
| 0.3 | 01/10/2026 | Sửa cách tính sổ theo đề xuất của chủ tiệm: bỏ cột "thu sau giảm", Tổng doanh thu là số trước giảm, giảm giá nằm trong Tổng chi, Thực nhận = Tổng doanh thu − Tổng chi (BR-05). Bỏ tên "Còn lại". Sửa số liệu tháng 9 (448.440 đ) và ghi nhận dòng 46 gõ tay 69.000 đ |
| 0.4 | 01/10/2026 | Thêm thiết kế ERD (`docs/design/erd.md`) và migration V1, V2. Từ điển dữ liệu: tiền giảm tính ra, ngày đã chốt là có hàng `daily_closing`. OQ-16 có đề xuất, thêm OQ-17 |
| 0.5 | 06/10/2026 | Thêm mục 3.15 Hỗ trợ nội dung bằng AI (FR-AI-01 đến 05, ưu tiên C, đợt R3), NFR-PRV-05 và OQ-18 đến OQ-20 |

## Mục lục

| Tệp | Nội dung |
|---|---|
| [01-introduction.md](01-introduction.md) | Mục đích, phạm vi, thuật ngữ, tác nhân |
| [02-overall-description.md](02-overall-description.md) | Bối cảnh, hiện trạng (Excel), ràng buộc, giả định |
| [03-functional-requirements.md](03-functional-requirements.md) | Yêu cầu chức năng theo module (có mã, độ ưu tiên, tiêu chí chấp nhận) |
| [04-business-rules.md](04-business-rules.md) | Quy tắc nghiệp vụ (tiền tệ, giảm giá, chốt sổ, tour) |
| [05-non-functional-requirements.md](05-non-functional-requirements.md) | Bảo mật, hiệu năng, sao lưu, giao diện, vận hành |
| [06-data-dictionary.md](06-data-dictionary.md) | Danh sách thực thể và trường dữ liệu (bản nháp trước ERD) |
| [07-open-questions.md](07-open-questions.md) | Câu hỏi còn mở cần hỏi chủ tiệm hoặc nhóm quyết định |
| [08-excel-traceability.md](08-excel-traceability.md) | Ánh xạ từ từng sheet Excel hiện tại sang yêu cầu mới |

## Quy ước

**Mã yêu cầu**

| Tiền tố | Ý nghĩa |
|---|---|
| `FR-<MODULE>-<số>` | Yêu cầu chức năng, ví dụ `FR-VIS-03` |
| `BR-<số>` | Quy tắc nghiệp vụ |
| `NFR-<NHÓM>-<số>` | Yêu cầu phi chức năng |
| `OQ-<số>` | Câu hỏi mở |

Mã đã đặt thì **không đổi và không tái sử dụng**. Nếu bỏ yêu cầu, đánh dấu `(Đã hủy)` thay vì xóa. Card Trello và test case tham chiếu theo mã này.

**Mã module:** `AUTH`, `SVC` (dịch vụ), `CMB` (combo), `CUS` (khách hàng), `PKG` (gói liệu trình), `VIS` (giao dịch), `EXP` (chi phí), `CLS` (chốt sổ), `SYN` (đồng bộ Google Sheets), `DSH` (dashboard), `STF` (nhân viên), `PAY` (tiền tour và lương), `IMP` (nhập dữ liệu từ Excel), `ADS` (chi phí quảng cáo), `RPT` (xuất báo cáo), `SYS` (hệ thống).

**Độ ưu tiên (MoSCoW):** `M` = Must (bắt buộc cho MVP), `S` = Should, `C` = Could, `W` = Won't (chưa làm ở giai đoạn này).

**Đợt phát hành:** `MVP` (bản cho gia đình dùng thật), `R2` (đợt 2), `R3` (đợt 3).

**Trạng thái yêu cầu:** `Draft` → `Agreed` (cả nhóm đồng ý) → `Validated` (chủ tiệm xác nhận).

## Cách dùng tài liệu này trong Scrum

1. Product Backlog trên Trello được sinh từ file `03-functional-requirements.md`: mỗi `FR` là một card (hoặc nhiều card nếu lớn), ghi mã `FR` ở tiêu đề.
2. Tiêu chí chấp nhận (AC) trong SRS là checklist của card và là cơ sở viết test.
3. Khi sprint review, chủ tiệm xác nhận các `FR` đã xong thì đổi trạng thái sang `Validated`.
4. Thay đổi yêu cầu đi qua Pull Request vào thư mục `docs/srs/`, kèm lý do ở mô tả PR.
5. Mục `07-open-questions.md` là danh sách cần giải quyết trước sprint tương ứng, không để yêu cầu `Must` còn câu hỏi mở khi bắt đầu sprint.
