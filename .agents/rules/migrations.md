---
trigger: glob
globs: backend/src/main/resources/db/migration/**
---

# Migration Flyway

- Migration đã có trên `develop` là bất biến: không sửa, không đổi tên, không xóa (AGENTS.md quy tắc 2). Hook chặn việc này.
- Đổi schema thì thêm file mới `V{n}__mo_ta.sql` bằng skill `/new-migration`.
- Tiền là `bigint`, thời điểm là `timestamptz`, ngày làm việc là `date`. Không xóa cứng dữ liệu nghiệp vụ.
- Bảng tài chính phải có trigger chặn sửa ngày đã chốt (`XL001`) như các bảng hiện có; xem `docs/design/erd.md` mục 5.5.
- Cập nhật `docs/design/erd.md` và `backend/src/test/resources/sql/schema_smoke_test.sql` trong cùng PR.
