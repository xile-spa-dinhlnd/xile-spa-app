# Luồng xử lý theo tính năng

Mỗi tính năng có một file luồng: **thiết kế viết trước khi code**, code theo đúng file đó, và file phải **khớp code
khi mở PR**. Vì vậy file vừa là đầu vào của `/implement`, vừa là chỗ tra khi sửa lỗi (`/fix-bug`) và khi review
(`/self-review`, `/review-pr`).

Nguồn sự thật vẫn là SRS (`docs/srs/`), ERD (`docs/design/erd.md`) và code; file luồng nối chúng lại cho một tính
năng. Lúc code thấy thiết kế sai hoặc không làm được thì dừng, hỏi, sửa thiết kế trước rồi mới code khác đi.

## Cấu trúc

```
docs/design/flows/
├─ README.md                 # file này: quy ước và khung
└─ <module>/                 # cùng tên module backend: auth, catalog, staff, closing, visit...
   ├─ README.md              # phần chung của module: cấu hình, quy ước dùng cho mọi tính năng
   ├─ login.md               # một tính năng người dùng thấy
   └─ forgot-password.md
```

- Một file một **tính năng người dùng thấy**, tên tiếng Anh kebab-case (`service-group.md`, `close-day.md`). Card có
  nhiều tính năng thì mỗi tính năng một file; tính năng đã có file thì sửa file đó.
- Card nào cũng có file luồng, kể cả CRUD đơn giản (file ngắn: điểm vào, quy tắc, lỗi, bảng test). Card có giao diện
  thì luôn có mục "Màn hình" theo `docs/design/ux-guidelines.md`.
- Phần chung của module viết một lần ở `<module>/README.md`, file tính năng không chép lại.

## Vòng đời

| Trạng thái            | Khi nào                                             | Ai đổi                                           |
| --------------------- | --------------------------------------------------- | ------------------------------------------------ |
| `thiết kế (chờ duyệt)` | Vừa viết, còn mục "Câu hỏi mở"                     | Claude `/plan-feature` (Đình), `/plan-task` (Huy) |
| `thiết kế (đã duyệt)` | Người làm card duyệt, câu hỏi đã chốt              | người viết thiết kế                              |
| `khớp code`           | Code xong, file đã sửa theo code, có tên test thật | `/implement`, `/fix-bug`                         |

Tính năng làm dở giữa hai lần (ví dụ backend đã merge, giao diện chưa làm) thì ghi trạng thái theo từng tầng:
`khớp code (backend, PR #41) · thiết kế (chờ duyệt) (giao diện)`. File chia phần (mục dưới) thì giữ `thiết kế (đã
duyệt)` tới khi xong phần cuối; tiến độ từng phần ghi ở bảng "Các phần".

## Chia phần

Card lớn thì một lần `/implement` cả card làm agent kém đi ở phần cuối (test sơ sài, quên Postman, file luồng không
khớp) và diff khó soát. Nên file luồng chia card thành **phần**, code và soát từng phần một, vẫn một nhánh một PR.

- **Một phần là một chức năng người dùng thấy**, làm trọn từ entity tới test và Postman của chức năng đó: "Tạo combo",
  "Sửa combo", "Ngừng bán, bán lại". Không chia theo tầng (entity hết rồi mới tới service).
- **Thứ tự: phần nền và nặng trước, nhẹ sau.** Phần 1 thường là "Tạo" (và xem): đặt khuôn entity, DTO, mapper, hàm
  tính tiền mà mọi phần sau dùng lại, nên soát kỹ nhất. Tiếp theo là phần có logic khó (sửa nhiều dòng, ràng buộc
  trạng thái sang module khác), cuối là phần mỏng (danh sách, lọc). Phần sau chỉ thêm vào khuôn của phần trước.
- **Khi nào chia:** code chính ước lượng quá khoảng 250 dòng, hoặc card có từ ba chức năng trở lên. Mỗi phần khoảng
  100 đến 300 dòng. Card nhỏ hơn thì không cần mục "Các phần".
- **Mỗi phần ghi:** chức năng, endpoint hoặc màn hình, lớp và method thêm theo tầng, các dòng của bảng "Hành vi và
  test" thuộc phần, request Postman, ước lượng dòng. Không viết trước code của phần sau.
- **Phần cuối** thêm việc hoàn tất card: các dòng "Ảnh hưởng" chưa thuộc phần nào, file luồng `khớp code`, backlog "Xong".
- **Trạng thái phần** trong bảng "Các phần": `chưa làm` → `đã code` (`/implement ... phần <N>` xong, test xanh) →
  `đã soát` (Đình: `/self-review phần <N>` sạch; Huy: tự đọc diff của phần). Phần trước chưa `đã soát` (Huy: chưa
  `đã code`) thì không làm phần sau.

Mỗi lần đổi file thì thêm một dòng vào bảng lịch sử: ngày (`dd/MM/yyyy`), người kèm agent đã viết, card hoặc PR,
thay đổi gì.

## Quy ước

- Mermaid: `sequenceDiagram` cho luồng giữa các tầng, `flowchart TD` cho rẽ nhánh. Mỗi nhánh lỗi kết thúc bằng mã
  HTTP và `ErrorCode`. Ghi rõ chỗ mở transaction, ghi `audit_log`, kiểm tra ngày đã chốt. Nhãn tiếng Việt, ngắn.
- Không chép code: chỉ tên lớp và method làm điểm vào, quy tắc BR/FR, thứ tự kiểm tra, tác dụng phụ (ghi gì, thu hồi
  gì, audit gì).
- Số cấu hình ghi kèm tên thuộc tính trong `application.yml`, vì giá trị có thể đổi.
- Mục nào không áp dụng thì ghi "Không" kèm lý do ngắn, không bỏ trống.

## Khung một file

````markdown
# <Tên tính năng bằng tiếng Việt>

Trạng thái: <thiết kế (chờ duyệt) | thiết kế (đã duyệt) | khớp code> · Card: <S2-06> · Yêu cầu: <FR-..., BR-..., OQ-...>

| Ngày       | Người              | Card / PR  | Thay đổi  |
| ---------- | ------------------ | ---------- | --------- |
| 23/10/2026 | Đình (Claude)      | S2-06      | Thiết kế  |
| 25/10/2026 | Đình (Antigravity) | S2-06, #42 | Khớp code |

## Mục tiêu

<1 đến 3 câu: người dùng làm được gì, vì sao cần.>

## Điểm vào

| Loại     | Điểm vào                                                                 | Ghi chú                            |
| -------- | ------------------------------------------------------------------------ | ---------------------------------- |
| API      | `POST /api/...` → `XxxController.method` → `XxxService.method`           | body, mã trạng thái thành công     |
| Màn hình | `features/<x>/pages/XxxPage` (route `/...`)                              | bảng ở máy tính, thẻ ở điện thoại |

## Luồng

<Sơ đồ Mermaid.>

## Màn hình

<Bỏ mục này nếu card chỉ có backend. Theo `docs/design/ux-guidelines.md`.>

| Phần                       | Điện thoại (dưới 768 px)            | iPad, máy tính                     |
| -------------------------- | ----------------------------------- | ---------------------------------- |
| Bố cục                     | <thẻ, drawer toàn màn hình...>      | <bảng, drawer 480 px...>           |
| Thứ tự thông tin trên thẻ  | <tên, số tiền, trạng thái...>       | <các cột của bảng>                 |

| Trạng thái | Hiển thị và câu chữ chính xác                              |
| ---------- | ---------------------------------------------------------- |
| Đang tải   |                                                            |
| Lỗi tải    | "Không tải được <...>" + "Thử lại"                         |
| Trống      | "Chưa có <...> nào" / "Không có kết quả khớp bộ lọc"      |
| Có dữ liệu |                                                            |

Hành động: <mỗi nút: nhãn, có xác nhận không (tiêu đề, mô tả hệ quả, nút đồng ý), câu báo thành công, lỗi hiện ở đâu>.

## Quy tắc nghiệp vụ

- <BR-.., thứ tự kiểm tra, transaction, audit, không xóa cứng, tiền long, Clock...>

## Lỗi trả về

| HTTP | `code` | Khi nào | Giao diện hiển thị |
| ---- | ------ | ------- | ------------------ |

## Ảnh hưởng

<Chỗ khác phải đổi theo: nơi gọi endpoint (frontend, test, Postman), method khác chạm cùng trạng thái, bản ghi tham
chiếu tới (cả hai chiều). Mỗi dòng: file hoặc lớp, đổi gì. Không có thì "Không".>

## Hành vi và test

Theo sáu nhóm của `AGENTS.md` mục 10. Lúc thiết kế ghi ca cần có; lúc khớp code ghi tên test thật.

| Hành vi | Tầng | Chuẩn | Dữ liệu sai | Biên | Lỗi nghiệp vụ | Quyền | Không tác dụng phụ |
| ------- | ---- | ----- | ----------- | ---- | ------------- | ----- | ------------------ |

Thử tay (Postman, giao diện): <bước và kết quả mong đợi>.

Dữ liệu mẫu dev: <bản ghi thêm vào `module/<tên>/devdata/<Tên>DevData` để thử tính năng, có ca biên nào; hoặc "Không"
kèm lý do> (`backend/AGENTS.md` mục "Dữ liệu mẫu cho máy dev").

## Các phần

<Chỉ có khi chia phần (mục "Chia phần" của README). Gõ `/implement <file này> phần <N>`, mỗi lần một phần.>

| Phần | Chức năng          | Khoảng   | Trạng thái |
| ---- | ------------------ | -------- | ---------- |
| 1    | <Tạo và xem ...>   | <n> dòng | chưa làm   |
| 2    | <Sửa ...>          | <n> dòng | chưa làm   |

### Phần 1. <Tên chức năng>

<Vì sao làm trước. Bảng tầng → lớp, method; dòng test; request Postman.>

## Câu hỏi mở

<Chỉ có khi chờ duyệt. Mỗi câu kèm **Đề xuất:** và lý do. Xóa mục này khi đã chốt; câu thành quyết định nghiệp vụ
thì ghi thêm OQ vào `docs/srs/07-open-questions.md`.>
````

File tách từ bản cũ (một file một module) giữ nguyên nội dung, chỉ thêm phần đầu; lần sửa tới thì bổ sung đủ các mục
theo khung.
