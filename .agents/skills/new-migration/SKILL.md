---
name: new-migration
description: Thêm một migration Flyway mới (V{n}__mo_ta.sql) cho Xile Spa đúng số thứ tự và quy ước, cập nhật ERD và bộ test schema. Dùng khi cần thêm hoặc đổi bảng, cột, index, trigger, view, hoặc khi người dùng gõ /new-migration.
---

# /new-migration: đổi schema bằng migration mới

Đọc trước: `docs/design/erd.md` (mục 2 quy ước, mục 5 quyết định thiết kế), migration hiện có trong
`backend/src/main/resources/db/migration/`.

## Các bước

1. **Tìm số kế tiếp.** Lấy số lớn nhất trong thư mục migration **và** trên develop:
   `git ls-tree --name-only origin/develop backend/src/main/resources/db/migration/`. Số mới = lớn nhất + 1.
   Nhắc người dùng báo số này cho người kia (CONTRIBUTING.md mục 4, "Xung đột"), vì hai nhánh có thể cùng lấy một số.
2. **Đặt tên** `V{n}__<mo_ta_snake_case_tieng_anh>.sql`, ví dụ `V3__service_group_sort_order.sql`.
3. **Viết SQL** theo quy ước của V1:
   - Tiền `bigint` có `CHECK (... >= 0)` khi phù hợp; thời điểm `timestamptz`; ngày làm việc `date`.
   - Không `DROP` dữ liệu nghiệp vụ; ngừng dùng thì thêm cột trạng thái (BR-09, BR-19).
   - Bảng có `business_date` thuộc phần tài chính: gắn trigger chặn ngày đã chốt như các bảng trong V1 (`XL001`).
   - Bảng chỉ thêm (lịch sử, nhật ký): trigger chặn `UPDATE`/`DELETE` (`XL002`).
   - Index cho khóa ngoại và cột lọc thường dùng; ràng buộc duy nhất đặt tên `ux_<bảng>_<cột>`.
   - Ghi chú tiếng Việt ở đầu file: migration này để làm gì, card nào.
4. **Cập nhật entity** tương ứng (`ddl-auto: validate` sẽ báo lỗi nếu entity lệch schema).
5. **Cập nhật tài liệu và test schema:**
   - `docs/design/erd.md`: bảng, cột, quyết định mới.
   - `backend/src/test/resources/sql/schema_smoke_test.sql`: thêm kiểm tra cho ràng buộc hoặc trigger mới.
   - Sơ đồ trong `docs/design/diagrams/` là file sinh tự động: chỉ nhắc người dùng chạy lại
     `docs/design/tools/gen_erd_mermaid.py` (ERD mục 11), không sửa tay.
6. **Chạy test** (cần Docker): `cd backend && ./gradlew test --tests 'com.xilespa.db.SchemaSmokeIT'`, rồi toàn bộ
   `./gradlew spotlessApply test`.

## Không bao giờ

- Sửa, đổi tên, xóa migration đã có trên develop (hook sẽ chặn).
- Đặt `spring.jpa.hibernate.ddl-auto` thành `update` hay `create`.
- Merge migration mà chưa có code dùng nó (CONTRIBUTING.md mục 3).
