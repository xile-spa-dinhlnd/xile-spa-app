# Checklist bảo mật cho một tính năng

Dùng cho **mọi** card. Thiết kế (`/plan-feature`, `/plan-task`) làm phần A; viết code (`/implement`) theo phần B;
soát (`/self-review`, `/review-pr`) làm phần C; trước khi mở PR làm phần D. Mục nào không áp dụng thì ghi "Không" kèm lý
do trong file luồng, không bỏ trống.

## A. Khi thiết kế: mục "Bảo mật" trong file luồng

1. **Tài sản chạm tới** (`threat-model.md`): tiền, dữ liệu khách, tài khoản, nhật ký, bí mật.
2. **Bề mặt mới** (`attack-surface.md`): endpoint mới, có endpoint nào không cần đăng nhập không (mặc định là không),
   file tải lên, kết nối ra ngoài, cookie.
3. **Mối đe dọa liên quan**: mã `T-xx` và kiểm soát tính năng dùng; mối đe dọa mới thì thêm vào `threat-model.md`.
4. **Dữ liệu nhạy cảm**: trường nào không được ra log, `audit_log`, email, thông báo lỗi (NFR-PRV-02).
5. **Ca thử** của phần C cần cho tính năng này, ghi vào bảng "Hành vi và test" (cột "Dữ liệu sai", "Biên", "Quyền, bảo mật").

## B. Khi viết code

Quy tắc nằm ở `backend/AGENTS.md` mục Bảo mật, Lỗi từ cơ sở dữ liệu và `AGENTS.md` mục 4, 9. Chỗ hay sai:

- Truy vấn luôn có tham số, không nối chuỗi; LIKE dùng `likePattern`.
- Không nhận trường nhạy cảm từ request (id, trạng thái, giá đã chốt, người tạo); mapper `ignore = true`.
- `before`, `after` của `audit_log` là DTO, không chứa mật khẩu, token, dữ liệu nhạy cảm.
- Lỗi 500 không trả chi tiết; log không in bí mật, token, ghi chú sức khỏe.
- API đổi dữ liệu không dùng `GET`; endpoint mới mặc định cần đăng nhập.
- Không thêm thư viện khi chưa hỏi (`AGENTS.md` mục 3).

## C. Khi soát: thử bằng đầu vào thật

Không suy ra từ annotation. Với mỗi endpoint mới hoặc đổi, viết một test thăm dò trên **bản sao trong thư mục tạm**,
gửi từng loại đầu vào dưới đây, ghi mã trả về và dữ liệu đã lưu. Kết quả sai là "Cần sửa" kèm test bảo vệ.

| Nhóm | Đầu vào | Kết quả đúng |
| ---- | ------- | ------------ |
| Tiền, số lượng | `1.7`, `"100"`, `1e5`, âm, `0`, vượt giới hạn, vượt `Long` | 400 có `fieldErrors` đúng ô; `0` theo quy tắc nghiệp vụ |
| Chuỗi tên, ghi chú | chỉ NBSP, chỉ zero-width, khoảng trắng lạ ở giữa, chữ Việt dạng tổ hợp, ký tự đảo chiều U+202E, rất dài, `<script>` | Tên rỗng sau chuẩn hóa là 400; trông giống tên đã có là 409; HTML lưu nguyên và hiển thị như chữ |
| Danh sách | rỗng, phần tử `null`, phần tử lặp, quá số tối đa | 400 có `fieldErrors` |
| Id, tham số | không phải số, âm, quá lớn; `page`, `size` cực lớn hoặc âm; enum sai, sai hoa thường | 400 hoặc 404, trang rỗng; **không bao giờ 500** |
| Body | JSON hỏng, rỗng, sai Content-Type, `Accept` không phải JSON; trường lạ (`id`, `active`, giá đã chốt) | 400, 415, 406; trường lạ bị bỏ qua |
| Quyền | chưa đăng nhập; token hết hạn, sửa tay, của người đã đổi mật khẩu | 401; không đọc, không sửa được |
| Dữ liệu nhạy cảm | tạo, sửa bản ghi có trường nhạy cảm | Không xuất hiện trong log, `audit_log`, thông báo lỗi |
| Ngày, khóa sổ (từ Sprint 3, 4) | ngày đã chốt, ngày tương lai, hai request cùng lúc | 409 `DAY_CLOSED`; không có bản ghi nửa vời |

## D. Trước khi mở PR

- Thêm endpoint không cần đăng nhập, nhóm API, cookie, kết nối ra ngoài → cập nhật `attack-surface.md`.
- Thêm hoặc đổi bí mật → cập nhật `secrets.md` và `.env.example`.
- Tài sản, mối đe dọa mới hoặc đổi trạng thái → cập nhật `threat-model.md`.
- Mô tả PR có một dòng "Bảo mật: <mã T-xx liên quan, kết quả phần C>".
