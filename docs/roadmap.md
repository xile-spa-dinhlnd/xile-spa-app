# Lộ trình phiên bản (roadmap)

Trang này trả lời: **mỗi phiên bản chứa gì, khi nào gọi là xong, còn bao xa tới bản dùng thật (v1.0.0)**.
Chỉ ghi mốc và phạm vi. Card chi tiết ở [backlog](backlog/sprint-0-1.md) và Trello; yêu cầu chi tiết ở
[SRS](srs/03-functional-requirements.md) (cột "Đợt": MVP, R2, R3).

- **Đánh số:** `v0.MINOR.PATCH` cho tới khi chủ tiệm dùng thật; mỗi sprint xong tăng MINOR, hotfix tăng PATCH
  ([CONTRIBUTING.md](../CONTRIBUTING.md) mục 6). `v1.0.0` = MVP chạy thật.
- **Nhịp:** sprint 2 tuần, năng lực khoảng 28 điểm/sprint (backlog mục 1).
- **Cập nhật:** ở mỗi buổi sprint planning (chọn phạm vi mốc tới) và sprint review (đánh dấu mốc vừa xong,
  dời mốc sau nếu cần). Từ Sprint 2 trở đi mọi mốc và ngày đều là **dự kiến**: phụ thuộc tốc độ thật của nhóm
  và câu trả lời của chủ tiệm cho các câu hỏi mở (OQ).

## Tổng quan

| Phiên bản | Sprint | Thời gian (dự kiến) | Mục tiêu | Trạng thái |
| --------- | ------ | ------------------- | -------- | ---------- |
| v0.1.0 | Sprint 0 | 01–06/10/2026 | Nền móng: khung backend, frontend, database, quy trình | Đã phát hành |
| v0.2.0 | Sprint 1 | 06–19/10/2026 | Đăng nhập, bảng giá dịch vụ, nhật ký thay đổi | Đang làm |
| v0.3.0 | Sprint 2 | 20/10–02/11 | Hoàn thiện tài khoản, nhân viên, nhóm dịch vụ, combo, nhập bảng giá | Dự kiến |
| v0.4.0 | Sprint 3 | 03–16/11 | Khách hàng cơ bản, nhập giao dịch (phần 1) | Dự kiến |
| v0.5.0 | Sprint 4 | 17–30/11 | Giao dịch (phần 2), chi phí, chốt sổ và bút toán | Dự kiến |
| v0.6.0 | Sprint 5 | 01–14/12 | Dashboard, hoàn thiện cho dùng thử | Dự kiến |
| v0.7.0 → v0.9.x | Giai đoạn DevOps + dùng thử | 15/12–cuối 01/2027 | Lên VPS, HTTPS, sao lưu; chủ tiệm dùng song song với Excel | Dự kiến |
| **v1.0.0** | | khoảng cuối 01/2027 | **MVP: chủ tiệm dùng hằng ngày thay Excel** | |
| v1.x | R2 | sau v1.0.0 | Gói liệu trình, đồng bộ Google Sheets, CRM nâng cao | Chưa lên lịch |
| v1.x / v2.0 | R3 | sau R2 | Lương và tiền tour, quảng cáo, báo cáo, hỗ trợ nội dung bằng AI | Chưa lên lịch |

## Chi tiết từng mốc

### v0.1.0 — Sprint 0: Nền móng (đã phát hành 06/10/2026)

Card S0-01 đến S0-08. Khung Spring Boot 4 và React + Ant Design, PostgreSQL bằng docker compose, migration V1 và V2,
kiểm thử lược đồ và ánh xạ lỗi XL với Postgres thật, Git Flow, README chạy từ máy trống.

### v0.2.0 — Sprint 1: Đăng nhập và bảng giá

- **Phạm vi:** FR-AUTH-01, 02, 07 · FR-SYS-01 · FR-SVC-01 đến 04 (card S1-01 đến S1-06).
- **Xong khi:** demo trên máy dev: đăng nhập, tạo dịch vụ, đổi giá, ngừng bán, xem nhật ký thay đổi.
- **Cần chốt trước:** không.

### v0.3.0 — Sprint 2: Tài khoản, nhân viên, danh mục

- **Phạm vi:** FR-AUTH-03, 04, 05 (quên, đổi mật khẩu, chống dò) · FR-STF-01 · FR-SVC-05 · FR-CMB-01 đến 05 ·
  FR-IMP-01 (nhập bảng giá từ Excel).
- **Xong khi:** chủ tiệm tự đặt lại được mật khẩu qua email; bảng giá, combo và nhân viên đã đủ để nhập giao dịch.
- **Cần chốt trước:** OQ-10 (dịch vụ email, tên miền). Nếu chưa chốt kịp thì dời FR-AUTH-03 sang mốc sau.

### v0.4.0 — Sprint 3: Khách hàng và giao dịch (phần 1)

- **Phạm vi:** FR-CUS-01 đến 03 · FR-VIS-01 đến 06, FR-VIS-09 (tạo giao dịch nhiều dòng, giảm giá, thanh toán,
  người làm, tiền tour, kiểm tra dữ liệu).
- **Xong khi:** nhập được một ngày bán hàng mẫu tháng 9 và ra đúng số trong bảng đối soát (OQ-03).
- **Cần chốt trước:** OQ-02 (ranh giới ngày), OQ-06 (tour cho combo), OQ-07 (thanh toán hai hình thức), OQ-08
  (khách vãng lai).

### v0.5.0 — Sprint 4: Giao dịch (phần 2), chi phí, chốt sổ

- **Phạm vi:** FR-VIS-07, 08, 10, 11 · FR-EXP-01 đến 03 · FR-CLS-01 đến 06 (xem trước, chốt sổ, khóa dữ liệu, bút
  toán điều chỉnh, cảnh báo ngày chưa chốt, lịch sử chốt).
- **Xong khi:** chạy trọn một ngày: nhập giao dịch và chi phí, chốt sổ, sửa sai bằng bút toán; số khớp Excel.
- **Cần chốt trước:** OQ-16 (hủy lượt đến đã chốt).

### v0.6.0 — Sprint 5: Dashboard

- **Phạm vi:** FR-DSH-01, 03, 06, 07, 09 · các yêu cầu phi chức năng còn thiếu cho MVP (tài liệu API OpenAPI
  NFR-MNT-01, vùng chạm và responsive NFR-USA-01, 02, 06).
- **Xong khi:** chủ tiệm xem được doanh thu, chi phí, thực nhận theo ngày, tuần, tháng trên điện thoại.
- **Cần chốt trước:** OQ-14 (dashboard có tính ngày chưa chốt không).

### v0.7.0 → v0.9.x — Giai đoạn DevOps và dùng thử

- **Phạm vi:** card D-01 đến D-07 ([backlog](backlog/sprint-0-1.md) mục 5): CI, Docker hóa, VPS, Caddy và HTTPS,
  sao lưu hằng đêm (FR-SYS-02, NFR-REL-01), tự động deploy. Nhập dữ liệu cũ nếu cần (OQ-12).
- **Cách làm:** chia bước nhỏ, làm cặp. Có thể bắt đầu D-01 (CI) và D-02 (Docker) song song từ Sprint 4–5.
- Mỗi lần deploy lên VPS gắn một tag `v0.7.0`, `v0.8.0`... Lỗi phát hiện khi dùng thử sửa bằng `v0.x.y`.

### v1.0.0 — MVP (bản dùng thật)

**Điều kiện phát hành** (tất cả phải đúng):

1. Đủ mọi yêu cầu mức M của đợt MVP trong SRS; các yêu cầu mức S chưa làm được ghi rõ và có kế hoạch.
2. Chạy trên VPS qua HTTPS (NFR-SEC-01), máy chủ đã khóa SSH và tường lửa (NFR-SEC-08).
3. Sao lưu hằng đêm, mã hóa, lưu ngoài VPS, **đã thử khôi phục thành công** (NFR-REL-01, 02).
4. CI xanh cho mỗi PR (NFR-MNT-04); test phần tiền, chốt sổ, bút toán đầy đủ (NFR-MNT-02).
5. Chủ tiệm dùng **song song với Excel ít nhất 1 tuần**, số liệu các ngày đã chốt khớp Excel.
6. Chủ tiệm đồng ý chuyển hẳn sang hệ thống.

## Sau MVP

### R2 — Gói liệu trình, Google Sheets, CRM nâng cao

FR-PKG-01 đến 04, FR-VIS-12 · FR-SYN-01 đến 07 · FR-CUS-04 đến 08, 10 · FR-DSH-02, 04, 05, 08 · FR-IMP-02, 03 ·
FR-SVC-06 · FR-SYS-04 · FR-CLS-08 · FR-CMB-06. Cần chốt: OQ-09 (cấu trúc Sheets), OQ-11 (ngưỡng phân nhóm khách),
OQ-12 (dữ liệu cũ).

### R3 — Lương, quảng cáo, báo cáo, AI

FR-PAY-01 đến 04 · FR-ADS-01, 02 · FR-RPT-01, 02 · FR-AUTH-06 (2FA) · FR-CUS-09 · FR-AI-01 đến 05. Cần chốt:
OQ-15, OQ-17 (lương), OQ-18 đến OQ-20 (AI). Phần AI cần VPS mạnh hơn nếu tự host mô hình (OQ-20).

## Lịch sử cập nhật

| Ngày | Thay đổi |
| ---- | -------- |
| 06/10/2026 | Bản đầu tiên, sau khi phát hành v0.1.0 |
