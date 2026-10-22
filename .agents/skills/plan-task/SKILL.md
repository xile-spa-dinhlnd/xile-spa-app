---
name: plan-task
description: Lập kế hoạch chi tiết có sơ đồ luồng (Mermaid) cho một card hoặc task cụ thể của Xile Spa trước khi code, rồi dừng chờ duyệt. Dùng khi bắt đầu card mới, khi người dùng gõ /plan-task <tên task>, hoặc khi được nhờ lập kế hoạch cho một task.
---

# /plan-task <tên task>: kế hoạch trước khi code

Mục tiêu: người dùng đọc kế hoạch trong 5 phút là biết bạn sẽ làm gì, theo luồng nào, đụng file nào, test gì.
**Không sửa file code nào trong skill này.**

## Các bước

1. **Xác định task/card.** Lấy mã card hoặc tên task từ tham số đi kèm lệnh `/plan-task <tên task>` (ví dụ
   `/plan-task S2-06` hoặc `/plan-task "quản lý nhóm dịch vụ"`). Nếu người dùng không nhập tham số, lấy từ tên
   nhánh (`git branch --show-current`, ví dụ `feature/S2-06-service-group`) hoặc câu hỏi của người dùng.
   Đọc dòng của card trong `docs/backlog/sprint-*.md`. Nếu nhánh hiện tại là `develop` hoặc `main`: ghi ở đầu kế
   hoạch lệnh tạo nhánh cho card (`git switch -c feature/<mã-card>-<mô-tả>`, mẫu ở CONTRIBUTING.md mục 2) và nhắc
   người dùng chạy trước khi duyệt; `/implement` sẽ không bắt đầu khi chưa ở nhánh card.
2. **Đọc yêu cầu.** Mở đúng mục FR trong `docs/srs/03-functional-requirements.md`, các BR liên quan trong
   `docs/srs/04-business-rules.md`, OQ trong `docs/srs/07-open-questions.md`. Đụng tiền hay chốt sổ thì đọc thêm
   theo bảng ở `AGENTS.md` mục 2.
3. **Đọc code hiện có.** Tìm module gần giống nhất để làm theo (ví dụ `module/staff` cho danh mục đơn giản,
   `module/catalog` cho danh mục có giá). Đọc migration hiện có nếu cần bảng. Dùng `git log` để xem các card trước
   đã làm thế nào. Module đã có `docs/design/flows/<module>.md` thì đọc trước: sơ đồ trong kế hoạch vẽ phần thay
   đổi trên luồng đang có, không vẽ lại từ đầu.
4. **Viết kế hoạch** theo mẫu bên dưới, dưới dạng Implementation Plan của Antigravity.
5. **Chuyển card sang "Đang làm"** trong file backlog là việc của bước `/implement`; ở đây chỉ ghi vào kế hoạch.
6. **Dừng lại**, hỏi: "Bạn duyệt kế hoạch này chứ? Có gì cần đổi?" Chỉ khi người dùng duyệt mới chuyển sang
   `/implement`.

## Mẫu kế hoạch

```markdown
# Kế hoạch: <mã card> <tên card>

## 1. Mục tiêu

<1 đến 3 câu: người dùng cuối làm được gì sau card này.>
Yêu cầu: FR-..., BR-..., OQ-... (đã chốt / còn mở)

## 2. Ý tưởng

<Cách giải quyết chọn, và vì sao. Nếu có 2 cách, nêu cách bỏ và lý do.>

## 3. Luồng xử lý

<Sơ đồ Mermaid. Thường cần một sequenceDiagram cho API chính, và flowchart nếu có rẽ nhánh nghiệp vụ.>

## 4. Thay đổi

| Phần     | Thay đổi                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------ |
| DB       | Migration V{n}__... : bảng, cột, index, trigger (hoặc "Không")                                         |
| API      | `POST /api/...` body, mã trạng thái, mã lỗi mới trong ErrorCode                                        |
| Backend  | file sẽ thêm/sửa theo khuôn module trong backend/AGENTS.md                                             |
| Frontend | feature, page, component, schema Zod, query key                                                        |
| Tài liệu | Postman, backlog, ERD, SRS, AGENTS (nếu đổi quy ước)                                                   |
| Luồng    | Tạo / sửa mục nào trong `docs/design/flows/<module>.md`, hoặc "Không" (CRUD đơn giản, xem README ở đó) |

## 5. Quy tắc nghiệp vụ phải giữ

<Liệt kê cụ thể cho card này: tiền long, ngày đã chốt, audit_log cùng transaction, không xóa cứng, Clock...>

## 6. Test sẽ viết

Mỗi hành vi một dòng, theo sáu nhóm của `AGENTS.md` mục 10; `/implement` viết các test này bằng `/write-tests` và
đối chiếu lại ở bước cuối.

| Hành vi | Tầng (entity / service / IT / Vitest) | Các ca chính (chuẩn, dữ liệu sai, biên, lỗi nghiệp vụ, quyền, không tác dụng phụ) |
| ------- | ------------------------------------- | --------------------------------------------------------------------------------- |

Thử tay bằng Postman / giao diện: <bước và kết quả mong đợi>

## 7. Chia PR

Ước lượng số dòng thay đổi (code và test, không tính lock file, Postman). Dưới khoảng 400 dòng: "Một PR". Lớn hơn:
liệt kê từng PR theo CONTRIBUTING.md mục 2.

| PR  | Nhánh                           | Gồm                                         | Ước lượng |
| --- | ------------------------------- | ------------------------------------------- | --------- |
| 1/2 | `feature/<mã-card>-<mô-tả>-api` | migration, backend, test, Postman           | ~350 dòng |
| 2/2 | `feature/<mã-card>-<mô-tả>-ui`  | frontend, test, backlog "Xong", sơ đồ luồng | ~300 dòng |

## 8. Rủi ro và câu hỏi

<Điều chưa rõ trong SRS (ghi mã OQ), giả định tạm, phần dễ đụng file của người kia.>
```

## Quy tắc vẽ sơ đồ

- Dùng Mermaid để GitHub hiển thị được khi dán vào PR. Nhãn tiếng Việt, ngắn.
- `sequenceDiagram` với các bên: `Người dùng`, `Giao diện`, `Controller`, `Service`, `DB`. Ghi rõ chỗ mở
  transaction, chỗ ghi `audit_log`, chỗ kiểm tra ngày đã chốt.
- `flowchart TD` cho rẽ nhánh: mỗi nhánh lỗi kết thúc bằng mã lỗi trả về (ví dụ `409 DAY_CLOSED`).
- Ví dụ:

```mermaid
sequenceDiagram
    participant GD as Giao diện
    participant C as StaffController
    participant S as StaffService
    participant DB
    GD->>C: POST /api/staff {name, role}
    C->>S: create(request)
    Note over S: @Transactional
    S->>DB: kiểm tra tên trùng
    alt trùng tên
        S-->>GD: 409 STAFF_NAME_DUPLICATE
    else hợp lệ
        S->>DB: insert staff
        S->>DB: audit_log CREATE (cùng transaction)
        S-->>GD: 201 StaffResponse
    end
```
