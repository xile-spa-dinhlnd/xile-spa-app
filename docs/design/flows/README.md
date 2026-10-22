# Luồng xử lý

Sơ đồ luồng của các tính năng có rẽ nhánh nghiệp vụ hoặc nhiều bước, vẽ **theo code đang chạy** (không theo kế
hoạch). Dùng khi sửa lỗi (`/fix-bug` đọc trước khi truy nguyên nhân) và khi đổi luồng (`/plan-task` vẽ thay đổi
trên sơ đồ hiện có).

Nguồn sự thật vẫn là code, SRS (`docs/srs/`) và ERD (`docs/design/erd.md`); file ở đây chỉ nối chúng lại thành
luồng. Lệch với code thì sửa file này trong cùng PR với code (AGENTS.md mục 5 bước 6).

## Khi nào cần

- Có: tiền, chốt sổ, bút toán điều chỉnh, đăng nhập và phiên, giao dịch có kiểm tra trạng thái hay ngày đã chốt,
  luồng nhiều màn hình, việc chạy nền (đồng bộ Sheets).
- Không: CRUD danh mục chỉ kiểm tra trùng tên và ghi audit; FR và test đã đủ mô tả.

## Quy ước

- Một file một module, cùng tên module backend: `auth.md`, `closing.md`, `visit.md`. Mỗi luồng là một mục `##`.
- Mermaid: `sequenceDiagram` cho luồng giữa các tầng, `flowchart TD` cho rẽ nhánh. Mỗi nhánh lỗi kết thúc bằng mã
  HTTP và `ErrorCode` trả về.
- Không chép lại code: chỉ tên lớp và method làm điểm vào, các quy tắc BR/FR, thứ tự kiểm tra và tác dụng phụ (ghi
  gì, thu hồi gì, audit gì).
- Số cấu hình (thời hạn token, số lần sai) ghi kèm tên thuộc tính trong `application.yml`, vì giá trị có thể đổi.

## Khung một mục

```markdown
## <Tên luồng> (FR-..., BR-...)

Điểm vào: `METHOD /api/...` → `XxxController.method` → `XxxService.method`. Frontend: `features/x/...`.

<sơ đồ Mermaid>

Quy tắc giữ trong luồng:

- <BR-.., thứ tự kiểm tra, transaction, audit>

Lỗi trả về: `401 UNAUTHORIZED` (...), `409 ...` (...).
```
