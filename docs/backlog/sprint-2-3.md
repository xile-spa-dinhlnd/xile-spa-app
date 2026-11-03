# Backlog Sprint 2 và Sprint 3

Nguồn: `docs/srs/03-functional-requirements.md` (mã FR), `docs/srs/07-open-questions.md` (mã OQ),
`docs/design/erd.md`, `docs/roadmap.md`.

**Cách dùng file này:** giống `sprint-0-1.md`: đây là **sổ theo dõi**, mô tả chi tiết dán vào Trello (cột "Nội
dung dán vào card"). Cột Trello, nhãn, cách ước lượng và Definition of Done dùng chung, xem `sprint-0-1.md` mục 1.
Bắt đầu card thì chuyển sang "Đang làm", xong thì "Xong" ngay trong PR của card (AGENTS.md mục 5).

**Trạng thái:** bản kế hoạch, Đình duyệt ngày 11/10/2026. Chốt lại ở buổi planning đầu mỗi sprint. Sprint 3 chốt lại
ngày 03/11/2026 (mục 2). Từ Sprint 3, Trello và file này chạy song song một sprint; lệch nhau thì báo Đình.

---

## 1. Sprint 2: Tài khoản và danh mục (20/10–02/11, v0.3.0)

Mục tiêu: chủ tiệm tự quản lý được mật khẩu; có đủ danh mục (nhân viên, nhóm dịch vụ, combo, bảng giá nhập từ
Excel) để Sprint 3 nhập được giao dịch. Mỗi card gồm cả backend và giao diện, trừ khi ghi khác.

| Mã    | Card                                         | Điểm | Nhãn    | Người làm | Cột Trello      | Nội dung dán vào card                                                                                                                                                                                                   |
| ----- | -------------------------------------------- | ---- | ------- | --------- | --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| S2-01 | Đổi mật khẩu                                 | 3    | BE, FE  | Huy       | Xong            | FR-AUTH-04. Nhập mật khẩu cũ và mới; mật khẩu mới theo NFR-SEC-03 (tối thiểu 10 ký tự, chặn mật khẩu phổ biến); đổi xong thu hồi các phiên khác; ghi audit (không ghi mật khẩu)                                        |
| S2-02 | Quên mật khẩu qua email                      | 5    | BE, FE  | Huy       | Xong            | FR-AUTH-03. Luôn trả cùng một thông báo dù email có tồn tại hay không; token đặt lại chỉ lưu bản băm, hết hạn 30 phút, dùng một lần; đặt lại xong thu hồi mọi phiên. Email theo OQ-10 (đã chốt): máy dev chỉ ghi link ra log; production gửi qua SMTP, cấu hình bằng biến môi trường `MAIL_FROM`, `MAIL_USERNAME`, `MAIL_PASSWORD` (`.env.example` chỉ để giá trị mẫu) |
| S2-03 | Chống dò mật khẩu                            | 3    | BE      | Huy       | Xong            | FR-AUTH-05, NFR-SEC-05. Khóa tạm sau N lần sai liên tiếp (dùng `failedLoginCount`, `lockedUntil` đã có); thông báo và thời gian phản hồi không làm lộ email (như S1-01); có test                                         |
| S2-04 | Chuyển JWT sang JwtEncoder/JwtDecoder        | 3    | BE      | Huy       | Xong            | Nợ kỹ thuật từ review S1-01: thay phần ký và kiểm tra JWT tự viết bằng `NimbusJwtEncoder`/`NimbusJwtDecoder` của Spring Security; ghi ADR-0001; bộ test hiện có phải xanh nguyên. **Làm nếu dư thời gian**               |
| S2-05 | Nhân viên                                    | 3    | BE, FE  | Đình      | Xong            | FR-STF-01. Tên, vai trò (KTV, chủ tiệm), đang làm hoặc đã nghỉ; không xóa cứng; ghi audit. Sprint 3 cần để chọn người làm trong giao dịch                                                                            |
| S2-06 | Quản lý nhóm dịch vụ                         | 2    | BE, FE  | Đình      | Xong            | FR-SVC-05. Thêm, sửa tên và thứ tự, ngừng dùng nhóm; tên không trùng (`ux_service_group_name`); ghi audit                                                                                                            |
| S2-07 | Combo backend                                | 5    | BE      | Đình      | Xong            | FR-CMB-01 đến 05. Giá combo, dịch vụ thành phần và số lượng; tiền tour mặc định của combo (OQ-06, mặc định 0); backend tính tổng giá lẻ và mức tiết kiệm (giao diện chỉ hiển thị); sửa không ảnh hưởng giao dịch đã ghi; ngừng bán, bán lại; ghi audit; Postman             |
| S2-08 | Combo giao diện                              | 3    | FE      | Đình      | Xong            | FR-CMB-01 đến 05. Danh sách, form chọn dịch vụ thành phần, hiển thị tổng giá lẻ, giá combo, mức tiết kiệm; dùng `MoneyInput`                                                                                          |
| S2-09 | Nhập bảng giá từ Excel                       | 3    | BE, FE  | Đình      | Dời             | FR-IMP-01. Đọc sheet `LIST K XOÁ`, xem trước rồi mới lưu, báo lỗi theo dòng; thêm Apache POI, ghi ADR-0001; file nhập theo T-15 (giới hạn kích thước, chặn file nén bất thường, ô bắt đầu bằng `=` coi là chữ). **Dời ngày 03/11/2026** sang giai đoạn DevOps và dùng thử (v0.7), xem mục 4 |
| S2-10 | Chuyển style inline sang CSS Modules        | 3    | FE      | Đình      | Xong            | Nợ kỹ thuật (Đình chốt 22/10/2026, `frontend/AGENTS.md` mục CSS). Chuyển 112 chỗ `style={{}}` trong 14 file (`AppLayout`, các trang và form của `auth`, `catalog`, `staff`, `dashboard`, `MoneyInput`) sang `<Component>.module.css` cạnh component; màu, khoảng cách dùng biến trong `src/styles/tokens.css`. Giao diện không đổi (chụp trước/sau cỡ máy tính và điện thoại); `npm test` xanh nguyên. Chỉ giữ `style` cho giá trị tính lúc chạy |

**Tổng điểm:** khoảng 33 (Huy 14, Đình 19), quá năng lực 28. S2-04 và S2-09 là "làm nếu dư thời gian"; nếu
S1-06 bị dời sang Sprint 2 thì bỏ hai card này trước. S2-10 thêm ngày 22/10/2026, Đình tự nhận; chưa kịp thì dời
sang đầu Sprint 3. S3-08 (3 điểm) kéo sớm vào cuối Sprint 2 ngày 25/10/2026: sửa lỗi bảo mật đã có trên `develop`,
và S3-01 của Huy cần `TextNormalizer`, `@VisibleText` ngay ngày đầu Sprint 3. Đình làm S3-08 rồi S2-08; S2-09 vẫn
là "làm nếu dư thời gian". Hết Sprint 2, S2-09 chưa bắt đầu; ngày 03/11/2026 Đình chốt dời sang giai đoạn DevOps và
dùng thử, không đưa vào Sprint 3 (lý do ở mục 4).

**Phụ thuộc:** S2-08 cần S2-07. S2-09 cần S2-06 (nhóm dịch vụ) và module catalog (S1-05).

**Xong khi:** chủ tiệm tự đặt lại được mật khẩu; bảng giá, combo, nhân viên đủ dữ liệu để nhập giao dịch;
phát hành v0.3.0.

---

## 2. Sprint 3: Khách hàng và giao dịch phần 1 (03–16/11, v0.4.0)

Mục tiêu: có khách hàng và nhập được giao dịch hằng ngày.

**Chốt lại ngày 03/11/2026** (Đình duyệt, đã ghi lên Trello): chỉnh điểm; tách S3-04 (8 điểm) thành S3-04, S3-10,
S3-11; S3-06 chỉ làm backend, phần giao diện chuyển vào S3-05; S3-09 lên 3 điểm, giao Đình. Cùng ngày Đình quyết định
giữ S3-03 và S3-05 trong Sprint 3 (không dời sang Sprint 4), và thêm S3-12, S3-13: Đình refactor toàn bộ giao diện
theo một bộ quy tắc chung (phổ màu, quy tắc code frontend) để giao diện mới đồng nhất. S3-08 xong ở cuối Sprint 2,
không tính vào tổng Sprint 3.

| Mã    | Card                                  | Điểm | Nhãn    | Người làm | Cột Trello      | Nội dung dán vào card                                                                                                                                                                                                                   |
| ----- | ------------------------------------- | ---- | ------- | --------- | --------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| S3-12 | Bộ quy tắc giao diện                  | 3    | FE      | Đình      | Sprint Backlog  | Làm **đầu Sprint 3**; merge trước khi Huy bắt đầu S3-02 và trước S3-05, S3-13, để mọi giao diện mới theo cùng một phổ màu và quy tắc. (1) Theme: chạy `/frontend-design` (Antigravity) lấy đề xuất, Đình duyệt, chốt vào `frontend/src/styles/theme.ts` và `tokens.css`: bảng màu đủ vai trò (thương hiệu, nền, chữ, thành công, cảnh báo, lỗi; tương phản chữ ≥ 4.5:1), phông chữ đủ dấu tiếng Việt, thang cỡ chữ, bo góc, khoảng cách, độ dày đặc của bảng và form. (2) Quy tắc viết: `docs/design/ux-guidelines.md` mục 5; `frontend/AGENTS.md`: khung chuẩn trang danh sách, drawer form, trạng thái rỗng / đang tải / lỗi, hiển thị tiền trong bảng. (3) Component dùng chung chỉ cho mẫu đã lặp từ 3 lần (AGENTS.md mục 9), ví dụ khung trang danh sách. (4) Kiểm bằng máy: mở rộng `frontend/scripts/check-css-tokens.mjs` chặn mã màu viết thẳng trong `.tsx` và `style={{}}` trừ giá trị tính lúc chạy; test kiểm `theme.ts` và `tokens.css` cùng giá trị màu. Thêm phông tải từ ngoài hay gói npm thì hỏi trước (AGENTS.md mục 3). Thiết kế: `/plan-feature S3-12` |
| S3-13 | Áp quy tắc giao diện lên màn hình có sẵn | 5  | FE      | Đình      | Sprint Backlog  | Áp bộ quy tắc của S3-12 lên toàn bộ giao diện có sẵn (khoảng 3.460 dòng TSX trong 30 file). Chia 4 phần, mỗi phần soát riêng: (1) `AppLayout`, đăng nhập, quên mật khẩu, đặt lại mật khẩu, đổi mật khẩu; (2) dịch vụ và nhóm dịch vụ; (3) combo; (4) nhân viên, dashboard, trang 404. Mỗi phần: ảnh chụp trước/sau cỡ máy tính và điện thoại; không đổi hành vi; `npm test` xanh nguyên. Phần 1 đụng `AppLayout`/`providers.tsx` giống S3-07 của Huy: báo nhau ai làm trước. Cần S3-12. Chậm thì các phần còn lại làm tiếp đầu Sprint 4 |
| S3-01 | Khách hàng backend                    | 5    | BE      | Huy       | Sprint Backlog  | FR-CUS-01, 02. Tạo, sửa, tìm nhanh theo tên hoặc SĐT; chuẩn hóa SĐT và không trùng (BR-10); ghi chú sức khỏe là dữ liệu nhạy cảm, không ra log (NFR-PRV-02); `audit_log` chỉ ghi ghi chú sức khỏe "đã thay đổi", không ghi nội dung (OQ-23); khách vãng lai theo OQ-08; ghi audit; Postman. Dùng sẵn của S3-08 (`docs/design/flows/common/input-hardening.md`): tên khách `@VisibleText` + `TextNormalizer.normalizeName`, phân trang `PageRequests`; ghi chú nhiều dòng **không** dùng `normalizeName` (gộp mất xuống dòng), thiết kế cách giữ xuống dòng mà vẫn bỏ ký tự vô hình; `likePattern` chuyển vào `common` thay vì chép bản thứ ba                       |
| S3-02 | Khách hàng giao diện                  | 3    | FE      | Huy       | Sprint Backlog  | FR-CUS-01, 02. Danh sách, form, ô tìm nhanh làm thành component dùng lại khi nhập giao dịch (S3-05)                                                                                                                                      |
| S3-03 | Hồ sơ khách 360                       | 5    | BE, FE  | Huy       | Sprint Backlog  | FR-CUS-03. Lịch sử giao dịch, số lần đến, tổng chi tiêu, AOV, ngày đầu và cuối, số ngày chưa quay lại; tính khi truy vấn, không lưu (BR-11). Làm cuối sprint vì cần dữ liệu giao dịch (S3-04) |
| S3-04 | Giao dịch backend: tạo giao dịch      | 5    | BE      | Đình      | Sprint Backlog  | FR-VIS-01, 02, 03, 09. Giao dịch nhiều dòng (dịch vụ hoặc combo); chụp tên và giá lúc bán (BR-03); giảm giá % theo dòng bằng `calc_discount` (BR-04); kiểm tra dữ liệu khi nhập (thiếu dịch vụ, thiếu thanh toán, giảm giá ngoài 0–100, ngày không hợp lệ); khách vãng lai để trống khách (OQ-08); `businessDate` bắt buộc gửi lên (ngày mặc định theo giờ chốt làm ở S3-11); một hình thức thanh toán theo cột hiện có `visit.payment_method` (S3-06 chuyển sang `visit_payment`); xem lại giao dịch theo id; ghi audit; Postman |
| S3-10 | Người làm và tiền tour theo dòng      | 3    | BE      | Đình      | Sprint Backlog  | FR-VIS-05, 06, OQ-06. Chọn người thực hiện cho từng dòng bán: chỉ nhân viên đang làm, chặn người đã nghỉ (AGENTS.md mục 7: xét mọi method đưa nhân viên vào dòng bán). Tiền tour tự điền từ dịch vụ hoặc `combo.tour_fee`, sửa tay được, chụp lại theo dòng (BR-03); chủ tiệm làm thì tour = 0. Ghi audit; Postman. Cần S3-04 |
| S3-06 | Thanh toán nhiều hình thức           | 3    | BE      | Đình      | Sprint Backlog  | FR-VIS-04, OQ-07. **Chỉ backend** (phần giao diện chuyển vào S3-05). Migration V3: bảng `visit_payment` (hình thức, số tiền), bỏ `visit.payment_method`, sửa các view đang dùng cột này (`visit_item_amounts`, `v_daily_summary`); tổng các dòng thanh toán phải bằng số phải trả (backend kiểm tra); cập nhật ERD; Postman. Làm sau S3-04 và S3-10, trước S3-11 |
| S3-11 | Ngày làm việc và đối soát tháng 9     | 3    | BE      | Đình      | Sprint Backlog  | OQ-02, OQ-03, BR-02. Ngày làm việc mặc định theo giờ chốt 04:00 (`xile.business-day.cutoff`, dùng `Clock`): giao dịch nhập trước 04:00 thuộc ngày hôm trước; người dùng đổi được ngày khi ngày đó chưa chốt. Test ca biên quanh 04:00; ngày đã chốt trả 409 `DAY_CLOSED` (trigger `lock_business_day`). Test tích hợp đối soát: nhập một ngày mẫu từ `THÁNG 9 NĂM 2026 - SOURCE.xlsx` rồi so `v_daily_summary` với bảng đối soát (OQ-03). Dữ liệu mẫu chỉ giữ dịch vụ và số tiền, không đưa tên, SĐT khách thật vào repo (AGENTS.md mục 4, quy tắc 6). Làm sau S3-06 vì view tổng kết đổi theo `visit_payment` |
| S3-05 | Giao diện nhập giao dịch nhanh        | 5    | FE      | Đình      | Sprint Backlog  | FR-VIS-01 đến 06, 09, một phần FR-VIS-10. Tối ưu điện thoại và iPad, khoảng 5 lần chạm cho một giao dịch thường (NFR-USA-06); không tính tiền ở giao diện, hiển thị số backend trả về. Gồm cả phần giao diện thanh toán chuyển từ S3-06: mặc định một hình thức, có nút "Chia hai hình thức". Dùng ô tìm khách của S3-02. Cần backend S3-04, S3-10, S3-06 |
| S3-07 | Phiên hết hạn: về trang đăng nhập     | 2    | FE      | Huy       | Sprint Backlog  | FR-AUTH-02. Khi làm mới phiên thất bại, `notifySessionExpired` (`shared/api/client.ts`) chưa có nơi đăng ký `onSessionExpired`, nên trang đang mở chỉ hiện lỗi 401; người dùng chỉ về `/login` khi query `me` chạy lại (tới 5 phút) hoặc tải lại trang (`docs/design/flows/auth/session-refresh.md`). Đăng ký listener một lần ở tầng app (`AppLayout` hoặc `app/providers.tsx`): xóa cache TanStack Query, chuyển về `/login?from=<trang hiện tại>`, báo "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."; hủy đăng ký khi unmount. Test Vitest: refresh 401 thì chuyển trang và xóa cache; refresh thành công thì không chuyển; nhiều request 401 cùng lúc chỉ chuyển và báo một lần. Cập nhật sơ đồ trong `session-refresh.md`. Một PR, khoảng 150 dòng. Làm sớm trong sprint |
| S3-08 | Chặn đầu vào bất thường              | 3    | BE      | Đình      | Xong            | Sửa 7 lỗi tìm ra khi dò ca biên ngày 25/10/2026, thiết kế ở `docs/design/flows/common/input-hardening.md` (chia 3 phần): tiền, số lượng nhận số thập phân và chuỗi số (cắt mất phần lẻ); tên chỉ có NBSP hoặc ký tự vô hình; tên trông giống hệt vẫn qua kiểm tra trùng; `null` trong danh sách combo gây 500; sai Content-Type, `Accept` không phải JSON gây 500; `page` cực lớn gây 500; enum sai không có `fieldErrors`. Đổi `GlobalExceptionHandler`, Jackson toàn cục và phân trang của `AuditLogQueryService`: báo Huy trước. Làm trước S3-01 để khách hàng dùng ngay `TextNormalizer`, `@VisibleText` |
| S3-09 | CI: GitHub Actions cho mỗi PR          | 3    | DevOps  | Đình      | Sprint Backlog  | Workflow chạy khi mở hoặc cập nhật PR vào `develop`: backend `./gradlew spotlessCheck test` (Testcontainers chạy được trên runner GitHub), frontend `npm ci`, `format:check`, `lint`, `typecheck`, `test`, tool cho agent `node --test "tools/**/*.test.mjs"` và `node tools/context-check/check.mjs`. Bật quy tắc nhánh: PR vào `develop` phải xanh mới merge. Lý do: "toàn bộ test phải pass" hiện chỉ dựa vào người chạy tay |

**Tổng điểm:** 45 (Huy 15, Đình 30), quá năng lực 28 khoảng 17 điểm. Đình chốt ngày 03/11/2026: giữ S3-03, S3-05 và
thêm S3-12, S3-13, chấp nhận trễ. Nếu chậm thì phần còn lại của S3-13 làm tiếp đầu Sprint 4 (các phần độc lập nhau), rồi
tới S3-03 và S3-05.

**Phụ thuộc:** S3-12 làm đầu tiên: S3-02, S3-05, S3-13 theo bộ quy tắc của nó. S3-04 cần nhân viên (S2-05) và combo
(S2-07). Thứ tự giao dịch: S3-04 → S3-10 → S3-06 → S3-11. Khách
hàng không bắt buộc trong giao dịch, nên S3-01 và S3-04 làm song song được. S3-05 cần backend S3-04, S3-10, S3-06 và
dùng ô tìm khách của S3-02; S3-03 cần dữ liệu giao dịch (S3-04).

**Xong khi:** nhập được một ngày bán hàng mẫu tháng 9 và ra đúng số trong bảng đối soát (OQ-03, test S3-11); phát
hành v0.4.0.

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
- **Giai đoạn DevOps và dùng thử (v0.7):** S2-09 nhập bảng giá từ Excel (FR-IMP-01), làm ngay trước khi chủ tiệm dùng
  thử. Lý do dời (Đình chốt 03/11/2026): Sprint 3 đã 45 điểm so với năng lực 28; nhập bảng giá là việc một lần lúc
  bắt đầu dùng thật (cơ sở dữ liệu là nguồn chính, ADR-0002), nhập sớm thì giá trong Excel có thể đổi và phải nhập
  lại; cột của dịch vụ, combo, tiền tour còn có thể đổi trong Sprint 3–4; phần chung của việc nhập file (Apache POI,
  xem trước, báo lỗi theo dòng, T-15) làm một lần cùng FR-IMP-02 và dữ liệu cũ (OQ-12). Xuất bảng giá chưa có trong
  SRS: hỏi chủ tiệm trước khi thêm card.
