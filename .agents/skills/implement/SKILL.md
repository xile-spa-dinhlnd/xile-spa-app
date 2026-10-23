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

Code đúng như thiết kế. Thấy thiết kế sai, thiếu hoặc không làm được (thêm bảng, đổi API, đụng module của người
kia) thì **dừng và hỏi**; được đồng ý thì sửa file luồng trước rồi mới code khác đi.

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
   Library) bằng `/write-tests`. Kiểm tra cỡ điện thoại, iPad, máy tính. Style bằng CSS Modules cạnh component,
   không `style={{}}` (`frontend/AGENTS.md` mục CSS).
5. **Ảnh hưởng:** làm hết các dòng ở mục "Ảnh hưởng" của file luồng (nơi gọi ở frontend, test cũ, Postman, method
   khác chạm cùng trạng thái). Trước khi coi là xong, grep lại URL và tên method đã đổi để chắc không còn chỗ sót
   (`AGENTS.md` mục 7).
6. **Postman:** thêm hoặc sửa request trong `docs/postman/xile-spa.postman_collection.json` (body mẫu, kiểm tra mã
   trạng thái). Trường hợp lỗi chính cũng có request riêng.
7. **Tài liệu:** SRS, ERD, README nếu hành vi đổi; `AGENTS.md` nếu đổi quy ước (AGENTS.md quy tắc 11).
8. **File luồng khớp code** (chốt chặn): sửa sơ đồ, quy tắc, lỗi theo **code vừa viết**; bảng "Hành vi và test"
   ghi tên test thật đang có trong code (không ghi dự định); nhóm không áp dụng ghi "không áp dụng: <lý do>". Đổi
   trạng thái thành `khớp code`, thêm dòng lịch sử `<dd/MM/yyyy> | <tên người dùng> (Antigravity) | <card> | Khớp
   code`. Còn ô trống thì quay lại `/write-tests`, chưa được sang bước 9.
9. **Backlog:** card sang "Xong" khi đủ Definition of Done.
10. **Dừng.** Báo cáo theo mục dưới. Không tự chạy `/pre-pr-check`, không hướng dẫn git.

Chạy theo khối sửa thì chỉ làm các bước của khối, rồi bước 8 nếu khối có đổi hành vi so với file luồng.

## Báo cáo

Một lần, không lặp lại:

- Nhánh đang làm (và đã tạo mới hay chưa).
- Mỗi bước của file luồng hoặc của khối sửa: đã làm hay chưa, khác kế hoạch chỗ nào và vì sao.
- File đã sửa.
- Lệnh test đã chạy, kết quả thật (số test qua, đỏ). Thử làm đỏ test nào, kết quả ra sao.
- Cách thử bằng Postman hoặc giao diện, kết quả mong đợi.
- Bước tiếp theo: Đình chạy `/self-review` bằng Claude Code; Huy đọc diff rồi gõ `/pre-pr-check` khi sẵn sàng.
