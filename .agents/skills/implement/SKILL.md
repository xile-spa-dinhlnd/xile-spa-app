---
name: implement
description: Code theo một thiết kế đã duyệt của Xile Spa - đầu vào là đường dẫn file luồng docs/design/flows/<module>/<tính-năng>.md hoặc một khối sửa từ review; tự tạo nhánh card, viết backend, frontend, mỗi tầng có test viết bằng /write-tests, Postman, cập nhật file luồng cho khớp code và backlog, rồi dừng. Dùng khi người dùng gõ /implement <file luồng>, dán khối "Sửa theo ..." từ review, hoặc duyệt xong /plan-task, /fix-bug.
---

# /implement <file luồng>: code theo thiết kế đã duyệt

Đầu vào là một trong hai:

- **File luồng** `docs/design/flows/<module>/<tính-năng>.md` có `Trạng thái: thiết kế (đã duyệt)` (Đình: do Claude
  viết; Huy: do `/plan-task` viết). File còn "Câu hỏi mở" hoặc còn "chờ duyệt" thì dừng và hỏi.
- **Khối sửa** bắt đầu bằng `# Sửa theo ...` (từ `/self-review` hoặc `/review-pr`): làm đúng từng bước của khối, không
  làm thêm ngoài khối.

**File có mục "Các phần"** (`docs/design/flows/README.md` mục "Chia phần"): người dùng gõ `/implement <file luồng>
phần <N>`. Thiếu số phần thì hỏi, không tự làm cả card. Chỉ làm phần N:

- Phần trước chưa `đã soát` (Huy: chưa `đã code`) thì dừng và hỏi.
- Theo "Thứ tự làm" bên dưới nhưng chỉ với lớp, method, test, Postman ghi ở phần N; không viết trước code của phần sau.
  Bước 1 (backlog "Đang làm") chỉ ở phần 1.
- Xong phần: bảng "Hành vi và test" ghi tên test thật cho các dòng của phần N, trạng thái phần N trong bảng "Các
  phần" thành `đã code`, rồi dừng và báo cáo. Bước 5 đến 9 (ảnh hưởng còn lại, file luồng `khớp code`, backlog "Xong")
  chỉ làm ở phần cuối.

Code đúng như thiết kế. Thấy thiết kế sai, thiếu hoặc không làm được (thêm bảng, đổi API, đụng module của người
kia) thì **dừng và hỏi**; được đồng ý thì sửa file luồng trước rồi mới code khác đi. Cần thư viện chưa có trong
`package.json` hoặc `libs.versions.toml` cũng **dừng và hỏi** theo bốn điểm của AGENTS.md mục 3, không tự `npm install`
hay thêm vào `libs.versions.toml`. Viết code theo phiên bản và lưu ý ở bảng "Thư viện" của `backend/AGENTS.md`,
`frontend/AGENTS.md`.

**Không rẽ sang việc khác.** Trong `/implement` chỉ gọi `/write-tests` và `/new-migration`; không tự gọi `/fix-bug`,
`/plan-task`, `/pre-pr-check`. Test đỏ ngoài dự kiến (test có sẵn, test của phần khác) thì **dừng và báo**: test nào,
lỗi in ra, nghi nguyên nhân; không sửa cấu hình, hạ tầng test (`src/test/resources`, `support/`) hay test của phần
khác chỉ để cho xanh (S3-08: chép cấu hình Jackson sang file test để test xanh). Thiết kế chỉ nói kết quả mà chưa nói
cách làm thì chọn cách đơn giản nhất với code đang có, ghi vào dòng "Khác thiết kế" của báo cáo.

## Bước 0. Nhánh

Tạo hoặc kiểm tra nhánh theo mục "Nhánh" của `.agents/rules/writer-role.md`, trước khi ghi file nào.

## Thứ tự làm

Làm từng bước. **Bước nào có code nghiệp vụ thì có bước test ngay sau, gọi `/write-tests`** theo bảng "Hành vi và
test" của file luồng: viết test, thử làm đỏ, chạy xanh. Test của bước chưa xanh thì không sang bước sau.

1. **Backlog:** card sang "Đang làm" trong `docs/backlog/sprint-*.md` (nếu chưa).
2. **DB** (nếu có): dùng skill `/new-migration`.
3. **Backend**, theo khuôn module ở `backend/AGENTS.md`:
   1. `entity/` (+ `enums/`): Lombok giới hạn, method nghiệp vụ thay cho setter, thời điểm truyền từ `Clock`.
      Có method nghiệp vụ thì **test entity** bằng `/write-tests`.
   2. `repository/`.
   3. `service/`: `@Transactional`, kiểm tra nghiệp vụ, `AuditService.record(...)` trong cùng transaction,
      lỗi bằng `BusinessException` + mã trong `ErrorCode`. **Test service** (Mockito) bằng `/write-tests`.
   4. `dto/request`, `dto/response` (record, thông báo validation tiếng Việt), `mapper/` (MapStruct, chỉ chép dữ liệu).
   5. `controller/`: URL theo quy ước, không có logic. **Test `...ControllerIT`** (`@IntegrationTest`) bằng
      `/write-tests`. Xem `endpoint-checklist.md` trong thư mục skill này cho từng endpoint.
4. **Frontend**, theo `frontend/AGENTS.md`: `types.ts` → `api.ts` (query key, hook) → `schemas.ts` (Zod khớp
   backend) → component, page → `routes.tsx` → `index.ts`. **Test schema Zod và component, page** (Vitest, Testing
   Library) bằng `/write-tests`. Màn hình theo mục "Màn hình" của file luồng và `docs/design/ux-guidelines.md` (bố
   cục, bốn trạng thái, câu chữ đúng từng chữ). Kiểm tra cỡ điện thoại, iPad, máy tính. Style bằng CSS Modules cạnh component,
   không `style={{}}` (`frontend/AGENTS.md` mục CSS).
5. **Ảnh hưởng:** làm hết các dòng ở mục "Ảnh hưởng" của file luồng (nơi gọi ở frontend, test cũ, Postman, method
   khác chạm cùng trạng thái). Trước khi coi là xong, grep lại URL và tên method đã đổi để chắc không còn chỗ sót
   (`AGENTS.md` mục 7).
6. **Postman:** thêm hoặc sửa request trong `docs/postman/xile-spa.postman_collection.json` (body mẫu, kiểm tra mã
   trạng thái). Trường hợp lỗi chính cũng có request riêng.
7. **Tài liệu:** SRS, ERD, README nếu hành vi đổi; `AGENTS.md` nếu đổi quy ước (AGENTS.md quy tắc 11). **Bảo mật:**
   code theo mục "Bảo mật" của file luồng và phần B của `docs/security/security-checklist.md`; làm phần D (cập nhật
   `docs/security/` nếu thêm endpoint công khai, bí mật, mối đe dọa).
8. **File luồng khớp code** (chốt chặn): sửa sơ đồ, quy tắc, lỗi theo **code vừa viết**; bảng "Hành vi và test"
   ghi tên test thật đang có trong code (không ghi dự định); nhóm không áp dụng ghi "không áp dụng: <lý do>". Đổi
   trạng thái thành `khớp code`, thêm dòng lịch sử `<dd/MM/yyyy> | <tên người dùng> (Antigravity) | <card> | Khớp
   code`. Còn ô trống thì quay lại `/write-tests`, chưa được sang bước 9.
9. **Backlog:** card sang "Xong" khi đủ Definition of Done.
10. **Dừng.** Báo cáo theo mục dưới. Không tự chạy `/pre-pr-check`, không đưa lệnh git (chỉ câu commit gợi ý).

Chạy theo khối sửa thì chỉ làm các bước của khối, rồi bước 8 nếu khối có đổi hành vi so với file luồng.

## Báo cáo

Một lần, không lặp lại:

- Nhánh đang làm (và đã tạo mới hay chưa).
- Mỗi bước của file luồng hoặc của khối sửa: đã làm hay chưa, khác kế hoạch chỗ nào và vì sao.
- **Skill đã gọi** (ví dụ `/write-tests` 3 lần) và **Khác thiết kế**: cách làm tự chọn, test thêm ngoài bảng, test đỏ
  ngoài dự kiến; không có thì ghi "không".
- **Code mới và nơi gọi** (mọi lần chạy, kể cả từng phần): mỗi method, lớp, bản nạp chồng mới kèm chỗ gọi trong
  `src/main` (grep tên). Chỉ test gọi hoặc không ai gọi thì xóa trước khi báo (`AGENTS.md` mục 7, 9; S3-08: thêm
  `PageRequests.of` 3 tham số "để sau này dùng").
- File đã sửa.
- Lệnh test đã chạy, kết quả thật (số test qua, đỏ).
- Bảng thử làm đỏ của `/write-tests` bước 4 (code đã sửa sai, test đỏ, lỗi in ra). Không có bảng thì không báo xong.
- Cách thử bằng Postman hoặc giao diện, kết quả mong đợi.
- **Câu commit gợi ý** cho đúng thay đổi của lần chạy này, theo CONTRIBUTING.md mục 3 (AGENTS.md mục 8): dòng đầu
  `<loại>(<phạm vi>): <mô tả tiếng Việt có dấu>`, dòng `Refs: <mã card>, <FR/BR/OQ>`; chạy theo phần thì ghi "phần
  <N>" ở cuối mô tả; khối sửa trong card dùng loại của phần code được sửa (`feat`, `refactor`, `test`), không `fix`.
  Chỉ câu, đặt trong khung code để chép, không kèm lệnh git.
- Bước tiếp theo: Đình chạy `/self-review` bằng Claude Code; Huy đọc diff rồi gõ `/pre-pr-check` khi sẵn sàng.
  File chia phần, chưa tới phần cuối: Đình chạy `/self-review phần <N>`; Huy đọc diff của phần, đổi trạng thái phần
  thành `đã soát` rồi gõ `/implement <file luồng> phần <N+1>`.
