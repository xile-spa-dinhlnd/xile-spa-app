---
name: implement
description: Thực hiện một kế hoạch đã được duyệt cho card của Xile Spa - viết backend, frontend, test, Postman và cập nhật backlog theo đúng thứ tự và khuôn của dự án. Dùng sau khi người dùng duyệt kết quả của /plan-task hoặc /fix-bug, hoặc khi người dùng gõ /implement.
---

# /implement: làm theo kế hoạch đã duyệt

Chỉ chạy khi đã có kế hoạch được duyệt trong cuộc trò chuyện. Chưa có thì chạy `/plan-task <tên task>` trước.
Lệch khỏi kế hoạch (thêm bảng, đổi API, đụng module của người kia) thì dừng và hỏi.

## Thứ tự làm

Làm từng bước, chạy test của bước đó trước khi sang bước sau. Mỗi bước là một commit gợi ý.

1. **Backlog:** chuyển card sang "Đang làm" trong `docs/backlog/sprint-*.md`.
2. **DB** (nếu có): dùng skill `/new-migration`.
3. **Backend**, theo khuôn module ở `backend/AGENTS.md`:
   1. `entity/` (+ `enums/`): Lombok giới hạn, method nghiệp vụ thay cho setter, thời điểm truyền từ `Clock`.
   2. `repository/`.
   3. `service/`: `@Transactional`, kiểm tra nghiệp vụ, `AuditService.record(...)` trong cùng transaction,
      lỗi bằng `BusinessException` + mã trong `ErrorCode`. Viết test đơn vị ngay sau bằng `/write-tests`.
   4. `dto/request`, `dto/response` (record, thông báo validation tiếng Việt), `mapper/` (MapStruct, chỉ chép dữ liệu).
   5. `controller/`: URL theo quy ước, không có logic. Viết test `...ControllerIT` (`@IntegrationTest`).
      Xem `endpoint-checklist.md` trong thư mục skill này cho từng endpoint.
4. **Frontend**, theo `frontend/AGENTS.md`: `types.ts` → `api.ts` (query key, hook) → `schemas.ts` (Zod khớp
   backend) → component, page → `routes.tsx` → `index.ts`. Test Vitest cho logic và form. Kiểm tra cỡ điện
   thoại, iPad, máy tính. Style bằng CSS Modules cạnh component, không `style={{}}` (`frontend/AGENTS.md` mục CSS).
5. **Postman:** thêm hoặc sửa request trong `docs/postman/xile-spa.postman_collection.json` (body mẫu, kiểm tra mã
   trạng thái). Trường hợp lỗi chính cũng có request riêng.
6. **Tài liệu:** SRS, ERD, README nếu hành vi đổi; `AGENTS.md` nếu đổi quy ước (AGENTS.md quy tắc 11).
7. **Backlog:** chuyển card sang "Xong" khi đủ Definition of Done.
8. Chạy `/pre-pr-check`.

## Sau mỗi bước, báo ngắn

- File đã sửa.
- Lệnh test đã chạy, kết quả thật.
- Câu commit gợi ý: `<loại>(<phạm vi>): <mô tả>` + dòng `Refs: <mã card>, <FR/BR>`.

Người dùng tự commit. Không chạy `git add`, `git commit`, không đổi nhánh.
