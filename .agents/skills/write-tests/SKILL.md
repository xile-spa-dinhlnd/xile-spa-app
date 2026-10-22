---
name: write-tests
description: Viết test cho code backend (JUnit, Mockito, Testcontainers) hoặc frontend (Vitest, Testing Library) của Xile Spa theo bảng điều kiện đạt - lập bảng ca test (chuẩn, dữ liệu sai, biên, lỗi nghiệp vụ, quyền, không tác dụng phụ), viết test, chứng minh test đỏ được, chạy và báo kết quả. Dùng khi cần thêm hoặc bổ sung test, khi người dùng gõ /write-tests, hoặc trong bước test của /implement.
---

# /write-tests: test đủ ca, đỏ được, chạy thật

Chuẩn phải đạt: `AGENTS.md` mục 10. Cách viết theo tầng: `backend/AGENTS.md` mục Test, `frontend/AGENTS.md` mục
Test và lệnh. Mẫu trong repo: `StaffServiceTest`, `StaffControllerIT`, `money.test.ts`, `StaffListPage.test.tsx`.

## 1. Xác định đối tượng test

Đọc code cần test (service, controller, schema, component) và yêu cầu FR/BR của nó. Liệt kê **mọi nhánh**: mỗi
`if`, mỗi `throw new BusinessException(ErrorCode.X)`, mỗi annotation validation, mỗi trạng thái hiển thị.

## 2. Lập bảng ca test trước khi viết

Trình bày bảng cho người dùng xem (không cần chờ duyệt nếu đang trong `/implement` đã duyệt):

| #   | Nhóm          | Tầng    | Đầu vào / tình huống      | Kết quả mong đợi                          |
| --- | ------------- | ------- | ------------------------- | ----------------------------------------- |
| 1   | Chuẩn         | Service | tên `"  Lan   A "`        | lưu "Lan A", ghi audit CREATE             |
| 2   | Dữ liệu sai   | IT      | tên rỗng                  | 400, `fieldErrors[0].field = fullName`    |
| 3   | Biên          | IT      | trùng tên khác hoa thường | 409 `STAFF_NAME_DUPLICATE`                |
| 4   | Lỗi nghiệp vụ | Service | ...                       | `ErrorCode...`, không `save`, không audit |
| 5   | Quyền         | IT      | chưa đăng nhập            | 401                                       |

Đủ sáu nhóm của `AGENTS.md` mục 10 (chuẩn, dữ liệu sai, biên, lỗi nghiệp vụ, quyền, không tác dụng phụ). Nhóm
nào không áp dụng thì ghi "không áp dụng" và lý do. Ca biên phải nghĩ theo nghiệp vụ của Xile Spa: tiền 0 và rất
lớn, ngày đã chốt, ngày tương lai, ngày đầu/cuối tháng, chuỗi toàn khoảng trắng, danh sách rỗng, trang cuối.

Chọn tầng rẻ nhất mà vẫn chứng minh được: logic thuần ở test đơn vị; thứ phụ thuộc DB, trigger, transaction,
bảo mật, JSON ở test tích hợp.

## 3. Viết test

- Một test một hành vi; tên nói hành vi (backend `camelCase` tiếng Anh, frontend `it('...')` tiếng Việt).
- Ba đoạn chuẩn bị / thực hiện / kiểm tra, cách nhau một dòng trống.
- Assert giá trị cụ thể; lỗi kiểm tra bằng mã (`ErrorCode`, `code` trong JSON).
- Thời gian qua `Clock.fixed(...)`; dữ liệu IT có tiền tố ngẫu nhiên; không `deleteAll()` bảng dùng chung.
- Frontend: tìm phần tử theo vai trò và chữ hiển thị; giả API bằng `vi.fn()` cho `fetch`.

## 4. Chứng minh test đỏ được

Với mỗi ca quan trọng (ít nhất ca lỗi nghiệp vụ và ca biên): tạm sửa sai code (đảo điều kiện, bỏ một dòng
kiểm tra), chạy riêng test đó, thấy đỏ, rồi **trả code về như cũ** và chạy lại thấy xanh. Ghi lại trong báo cáo
ca nào đã thử. Test không đỏ được thì viết lại assert.

## 5. Chạy và báo

```bash
cd backend && ./gradlew test --tests '<lớp test>'      # rồi ./gradlew spotlessApply test
cd frontend && npx vitest run <file test>              # rồi npm test
```

Báo cáo: bảng ca test (đánh dấu đã viết), số test qua/đỏ thật, các ca đã thử làm đỏ, ca nào chưa viết và vì sao.
Không ghi "đã qua" khi chưa chạy được (ví dụ Docker chưa bật); nói rõ lý do.

## Không bao giờ

- `@Disabled`, `it.skip`, xóa test đang đỏ, nới assert để test xanh (AGENTS.md quy tắc 5).
- Test chỉ kiểm tra mock được gọi mà không kiểm tra kết quả.
- Gọi backend thật hoặc dịch vụ ngoài từ test frontend.
