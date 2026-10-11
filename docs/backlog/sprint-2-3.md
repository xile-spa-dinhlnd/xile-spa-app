# Backlog Sprint 2 và Sprint 3

Nguồn: `docs/srs/03-functional-requirements.md` (mã FR), `docs/srs/07-open-questions.md` (mã OQ),
`docs/design/erd.md`, `docs/roadmap.md`.

**Cách dùng file này:** giống `sprint-0-1.md`: đây là **sổ theo dõi**, mô tả chi tiết dán vào Trello (cột "Nội
dung dán vào card"). Cột Trello, nhãn, cách ước lượng và Definition of Done dùng chung, xem `sprint-0-1.md` mục 1.
Bắt đầu card thì chuyển sang "Đang làm", xong thì "Xong" ngay trong PR của card (AGENTS.md mục 5).

**Trạng thái:** bản kế hoạch, Đình duyệt ngày 11/10/2026. Chốt lại ở buổi planning đầu mỗi sprint.

---

## 1. Sprint 2: Tài khoản và danh mục (20/10–02/11, v0.3.0)

Mục tiêu: chủ tiệm tự quản lý được mật khẩu; có đủ danh mục (nhân viên, nhóm dịch vụ, combo, bảng giá nhập từ
Excel) để Sprint 3 nhập được giao dịch. Mỗi card gồm cả backend và giao diện, trừ khi ghi khác.

| Mã    | Card                                         | Điểm | Nhãn    | Người làm | Cột Trello      | Nội dung dán vào card                                                                                                                                                                                                   |
| ----- | -------------------------------------------- | ---- | ------- | --------- | --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| S2-01 | Đổi mật khẩu                                 | 3    | BE, FE  | Huy       | Product Backlog | FR-AUTH-04. Nhập mật khẩu cũ và mới; mật khẩu mới theo NFR-SEC-03 (tối thiểu 10 ký tự, chặn mật khẩu phổ biến); đổi xong thu hồi các phiên khác; ghi audit (không ghi mật khẩu)                                        |
| S2-02 | Quên mật khẩu qua email                      | 5    | BE, FE  | Huy       | Product Backlog | FR-AUTH-03. Luôn trả cùng một thông báo dù email có tồn tại hay không; token đặt lại chỉ lưu bản băm, hết hạn 30 phút, dùng một lần; đặt lại xong thu hồi mọi phiên. Email theo OQ-10 (đã chốt): máy dev chỉ ghi link ra log; production gửi qua SMTP, cấu hình bằng biến môi trường `MAIL_FROM`, `MAIL_USERNAME`, `MAIL_PASSWORD` (`.env.example` chỉ để giá trị mẫu) |
| S2-03 | Chống dò mật khẩu                            | 3    | BE      | Huy       | Product Backlog | FR-AUTH-05, NFR-SEC-05. Khóa tạm sau N lần sai liên tiếp (dùng `failedLoginCount`, `lockedUntil` đã có); thông báo và thời gian phản hồi không làm lộ email (như S1-01); có test                                         |
| S2-04 | Chuyển JWT sang JwtEncoder/JwtDecoder        | 3    | BE      | Huy       | Product Backlog | Nợ kỹ thuật từ review S1-01: thay phần ký và kiểm tra JWT tự viết bằng `NimbusJwtEncoder`/`NimbusJwtDecoder` của Spring Security; ghi ADR-0001; bộ test hiện có phải xanh nguyên. **Làm nếu dư thời gian**               |
| S2-05 | Nhân viên                                    | 3    | BE, FE  | Đình      | Product Backlog | FR-STF-01. Tên, vai trò (KTV, chủ tiệm), đang làm hoặc đã nghỉ; không xóa cứng; ghi audit. Sprint 3 cần để chọn người làm trong giao dịch                                                                            |
| S2-06 | Quản lý nhóm dịch vụ                         | 2    | BE, FE  | Đình      | Product Backlog | FR-SVC-05. Thêm, sửa tên và thứ tự, ngừng dùng nhóm; tên không trùng (`ux_service_group_name`); ghi audit                                                                                                            |
| S2-07 | Combo backend                                | 5    | BE      | Đình      | Product Backlog | FR-CMB-01 đến 05. Giá combo, dịch vụ thành phần và số lượng; tiền tour mặc định của combo (OQ-06, mặc định 0); backend tính tổng giá lẻ và mức tiết kiệm (giao diện chỉ hiển thị); sửa không ảnh hưởng giao dịch đã ghi; ngừng bán, bán lại; ghi audit; Postman             |
| S2-08 | Combo giao diện                              | 3    | FE      | Đình      | Product Backlog | FR-CMB-01 đến 05. Danh sách, form chọn dịch vụ thành phần, hiển thị tổng giá lẻ, giá combo, mức tiết kiệm; dùng `MoneyInput`                                                                                          |
| S2-09 | Nhập bảng giá từ Excel                       | 3    | BE, FE  | Đình      | Product Backlog | FR-IMP-01. Đọc sheet `LIST K XOÁ`, xem trước rồi mới lưu, báo lỗi theo dòng; thêm Apache POI, ghi ADR-0001. **Làm nếu dư thời gian**                                                                                  |

**Tổng điểm:** khoảng 30 (Huy 14, Đình 16), hơi quá năng lực 28. S2-04 và S2-09 là "làm nếu dư thời gian"; nếu
S1-06 bị dời sang Sprint 2 thì bỏ hai card này trước.

**Phụ thuộc:** S2-08 cần S2-07. S2-09 cần S2-06 (nhóm dịch vụ) và module catalog (S1-05).

**Xong khi:** chủ tiệm tự đặt lại được mật khẩu; bảng giá, combo, nhân viên đủ dữ liệu để nhập giao dịch;
phát hành v0.3.0.

---

## 2. Sprint 3: Khách hàng và giao dịch phần 1 (03–16/11, v0.4.0)

Mục tiêu: có khách hàng và nhập được giao dịch hằng ngày.

| Mã    | Card                                  | Điểm | Nhãn    | Người làm | Cột Trello      | Nội dung dán vào card                                                                                                                                                                                                                   |
| ----- | ------------------------------------- | ---- | ------- | --------- | --------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| S3-01 | Khách hàng backend                    | 5    | BE      | Huy       | Product Backlog | FR-CUS-01, 02. Tạo, sửa, tìm nhanh theo tên hoặc SĐT; chuẩn hóa SĐT và không trùng (BR-10); ghi chú sức khỏe là dữ liệu nhạy cảm, không ra log (NFR-PRV-02); khách vãng lai theo OQ-08; ghi audit; Postman                       |
| S3-02 | Khách hàng giao diện                  | 3    | FE      | Huy       | Product Backlog | FR-CUS-01, 02. Danh sách, form, ô tìm nhanh làm thành component dùng lại khi nhập giao dịch (S3-05)                                                                                                                                      |
| S3-03 | Hồ sơ khách 360                       | 5    | BE, FE  | Huy       | Product Backlog | FR-CUS-03. Lịch sử giao dịch, số lần đến, tổng chi tiêu, AOV, ngày đầu và cuối, số ngày chưa quay lại; tính khi truy vấn, không lưu (BR-11). Làm cuối sprint vì cần dữ liệu giao dịch                                             |
| S3-04 | Giao dịch backend                     | 8    | BE      | Đình      | Product Backlog | FR-VIS-01 đến 06, 09. Giao dịch nhiều dòng (dịch vụ hoặc combo); chụp tên và giá lúc bán (BR-03); giảm giá % theo dòng bằng `calc_discount` (BR-04); người làm, tiền tour (combo lấy mức mặc định của combo, OQ-06); khóa ngày `lock_business_day`; ngày làm việc mặc định theo giờ chốt 04:00 (OQ-02); khách vãng lai để trống khách (OQ-08); ghi audit; test với số liệu tháng 9 (OQ-03); Postman |
| S3-06 | Thanh toán nhiều hình thức           | 3    | BE, FE  | Đình      | Product Backlog | FR-VIS-04, OQ-07. Migration V3: bảng `visit_payment` (hình thức, số tiền), bỏ `visit.payment_method`, sửa tổng kết ngày; tổng các dòng thanh toán phải bằng số phải trả (backend kiểm tra); giao diện mặc định một hình thức, có nút "Chia hai hình thức"; cập nhật ERD; Postman |
| S3-05 | Giao diện nhập giao dịch nhanh        | 5    | FE      | Đình      | Product Backlog | FR-VIS-01 đến 06, 09, một phần FR-VIS-10. Tối ưu điện thoại và iPad, khoảng 5 lần chạm cho một giao dịch thường (NFR-USA-06); không tính tiền ở giao diện, hiển thị số backend trả về                                         |

**Tổng điểm:** 29 (Huy 13, Đình 16), hơi quá năng lực 28. Nếu chậm thì dời S3-03 (hồ sơ 360) sang đầu Sprint 4.

**Phụ thuộc:** S3-04 cần nhân viên (S2-05) và combo (S2-07); S3-06 làm cùng hoặc ngay sau S3-04. Khách hàng không bắt buộc trong giao dịch, nên S3-01
và S3-04 làm song song được; S3-05 dùng ô tìm khách của S3-02 khi có.

**Xong khi:** nhập được một ngày bán hàng mẫu tháng 9 và ra đúng số trong bảng đối soát (OQ-03); phát hành v0.4.0.

---

## 3. Câu hỏi mở phải chốt trước từng sprint

Cả năm câu đã được Đình chốt ngày 11/10/2026; chi tiết quyết định ở `docs/srs/07-open-questions.md` (mục "Đã quyết định").

| Câu hỏi | Nội dung (tóm tắt)                                                        | Chặn card     | Hạn chót                  | Trạng thái |
| ------- | ------------------------------------------------------------------------- | ------------- | ------------------------- | ---------- |
| OQ-10   | Tên miền và dịch vụ gửi email cho quên mật khẩu                           | S2-02         | Trước Sprint 2 (20/10)    | Đã chốt       |
| OQ-02   | Ranh giới ngày: khách sau nửa đêm tính vào ngày nào; giờ mở, đóng cửa      | S3-04         | Trước Sprint 3 (03/11)    | Đã chốt       |
| OQ-06   | Tiền tour khi KTV làm combo: theo combo hay theo từng dịch vụ thành phần   | S3-04 (S2-07) | Trước Sprint 3 (03/11)    | Đã chốt       |
| OQ-07   | Một giao dịch có trả bằng hai hình thức (CK cộng TM) không                 | S3-04         | Trước Sprint 3 (03/11)    | Đã chốt       |
| OQ-08   | Khách vãng lai không có SĐT; hai khách dùng chung một SĐT                  | S3-01, S3-04  | Trước Sprint 3 (03/11)    | Đã chốt       |

Câu hỏi nào chưa chốt kịp thì card đó làm theo giả định ghi rõ trong PR (AGENTS.md quy tắc 9) hoặc dời sang sprint
sau, không tự quyết nghiệp vụ.

## 4. Sau Sprint 3 (xem `docs/roadmap.md`)

- **Sprint 4 (v0.5.0):** giao dịch phần 2 (sửa, hủy, danh sách theo ngày, nhập nhanh hoàn chỉnh), chi phí, chốt sổ
  và bút toán điều chỉnh. Cần OQ-16.
- **Sprint 5 (v0.6.0):** dashboard. Cần OQ-14.
