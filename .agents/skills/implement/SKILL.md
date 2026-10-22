---
name: implement
description: Thực hiện một kế hoạch đã được duyệt cho card của Xile Spa - viết backend, frontend, mỗi tầng có test viết bằng /write-tests, Postman, bảng đối chiếu hành vi và test, cập nhật backlog theo đúng thứ tự và khuôn của dự án, rồi dừng cho người dùng commit. Dùng sau khi người dùng duyệt kết quả của /plan-task hoặc /fix-bug, hoặc khi người dùng gõ /implement.
---

# /implement: làm theo kế hoạch đã duyệt

Chỉ chạy khi đã có kế hoạch được duyệt trong cuộc trò chuyện. Chưa có thì chạy `/plan-task <tên task>` trước.
Lệch khỏi kế hoạch (thêm bảng, đổi API, đụng module của người kia) thì dừng và hỏi.

## Kiểm tra nhánh

Chạy `git branch --show-current`. Được sửa file khi đang ở `develop` (AGENTS.md mục 5 bước 1), nhưng nếu là
`develop` hoặc `main` thì ghi lên đầu báo cáo đầu tiên và nhắc lại ở bước cuối: người dùng phải tạo nhánh theo mã
card **trước khi commit**, tiền tố theo loại việc ở CONTRIBUTING.md mục 2:

```bash
git switch -c feature/<mã-card>-<mô-tả>   # fix/, docs/, chore/ tùy loại
```

Thay đổi chưa commit sẽ đi theo sang nhánh mới.

## Phạm vi của lần chạy này

Kế hoạch có mục "Chia PR" thì chỉ làm phần thuộc **PR hiện tại** (hỏi người dùng là PR thứ mấy nếu chưa rõ), không
làm trước phần của PR sau. Card chỉ chuyển sang "Xong" ở PR cuối cùng của card.

## Thứ tự làm

Làm từng bước. **Bước nào có code nghiệp vụ thì có bước test ngay sau, gọi `/write-tests`**: lập bảng ca theo sáu
nhóm của `AGENTS.md` mục 10, viết test, thử làm đỏ, chạy xanh. Test của bước chưa xanh thì không sang bước sau.
Mỗi bước là một commit gợi ý (code và test của nó đi chung một commit).

1. **Backlog:** chuyển card sang "Đang làm" trong `docs/backlog/sprint-*.md`.
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
5. **Postman:** thêm hoặc sửa request trong `docs/postman/xile-spa.postman_collection.json` (body mẫu, kiểm tra mã
   trạng thái). Trường hợp lỗi chính cũng có request riêng.
6. **Tài liệu:** SRS, ERD, README nếu hành vi đổi; `AGENTS.md` nếu đổi quy ước (AGENTS.md quy tắc 11). Kế hoạch
   có dòng "Luồng" thì cập nhật `docs/design/flows/<module>.md` theo **code vừa viết** (không chép sơ đồ trong kế
   hoạch nếu lúc làm đã đổi), theo khung ở `docs/design/flows/README.md`.
7. **Đối chiếu hành vi và test** (chốt chặn, xem mẫu bên dưới). Còn ô trống thì quay lại `/write-tests`, chưa
   được sang bước 8.
8. **Backlog:** chuyển card sang "Xong" khi đủ Definition of Done (chỉ ở PR cuối của card).
9. **Dừng.** Không tự chạy `/pre-pr-check`. Báo cho người dùng: bảng đối chiếu, các câu commit gợi ý, và bước tiếp
   theo là "đọc lại diff, commit, rồi gõ `/pre-pr-check`" (còn ở `develop` thì nhắc tạo nhánh trước khi commit).

## Bảng đối chiếu hành vi và test (bước 7)

Mỗi hành vi mới hoặc sửa là một dòng: mỗi nhánh `if`, mỗi `ErrorCode` có thể ném, mỗi ràng buộc validation, mỗi
trạng thái màn hình (đang tải, lỗi, trống, có dữ liệu). Ô ghi tên test thật đang có trong code, không ghi dự định.

| Hành vi                           | Chuẩn                      | Dữ liệu sai                | Biên | Lỗi nghiệp vụ | Quyền | Không tác dụng phụ |
| --------------------------------- | -------------------------- | -------------------------- | ---- | ------------- | ----- | ------------------ |
| Tạo combo (`ComboService.create`) | `createSavesComboAndAudit` | `createWithBlankNameIs400` | ...  | ...           | ...   | ...                |

Nhóm không áp dụng thì ghi "không áp dụng: <lý do>". Ô trống là chưa xong.

## Sau mỗi bước, báo ngắn

- File đã sửa.
- Lệnh test đã chạy, kết quả thật.
- Câu commit gợi ý: `<loại>(<phạm vi>): <mô tả>` + dòng `Refs: <mã card>, <FR/BR>`.

Người dùng tự commit. Không chạy `git add`, `git commit`, không đổi nhánh.
