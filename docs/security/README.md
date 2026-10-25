# Bảo mật

Bốn file, mỗi file trả lời một câu hỏi. Yêu cầu gốc nằm ở `docs/srs/05-non-functional-requirements.md` (NFR-SEC,
NFR-PRV); quy tắc viết code ở `backend/AGENTS.md` mục Bảo mật và `AGENTS.md` mục 4. Ở đây chỉ trỏ đường, không chép lại.

| File                     | Trả lời câu hỏi                                                | Ai sửa, khi nào                                           |
| ------------------------ | -------------------------------------------------------------- | --------------------------------------------------------- |
| `threat-model.md`        | Cần bảo vệ gì, khỏi ai, bằng cách nào, còn hở gì               | Người làm card khi tính năng tạo tài sản hoặc mối đe dọa mới |
| `attack-surface.md`      | Kẻ tấn công chạm được vào hệ thống ở những đâu                 | Mọi PR thêm endpoint công khai, nhóm API, cookie, kết nối ra ngoài |
| `secrets.md`             | Bí mật nào tồn tại, nằm ở đâu, lộ thì làm gì                   | Mọi PR thêm hoặc đổi một bí mật                            |
| `security-checklist.md`  | Kiểm gì khi thiết kế, viết code, soát một tính năng            | Agent và người review dùng cho **mọi** card               |

Quy trình: thiết kế viết mục "Bảo mật" trong file luồng theo `security-checklist.md` phần A; soát (`/self-review`,
`/review-pr`) chạy phần C bằng đầu vào thật; trước khi mở PR làm phần D (cập nhật ba file còn lại nếu đổi).
