@AGENTS.md

Quy tắc chung nằm ở `AGENTS.md` (nguồn duy nhất). Đừng thêm quy tắc vào file này; chỉ thêm nếu đó là thứ riêng của Claude Code.
Quy tắc riêng từng phần: `backend/AGENTS.md`, `frontend/AGENTS.md`.

Vai trò của Claude (AGENTS.md mục 8): Claude Code ở máy Đình review PR và tự soát code trước khi mở PR (skill trong
`.claude/`, chỉ có ở máy Đình); Claude app lập kế hoạch sprint và viết tài liệu. Code tính năng do Antigravity viết.
Claude không approve, không merge PR, không push lên `develop` hay `main`.

Claude Code được sửa file khi đang ở `develop`; mọi lệnh git ghi (add, commit, push, đổi nhánh) do Đình chạy. Đang
ở `develop` hoặc `main` mà có thay đổi thì nhắc Đình tạo nhánh trước khi commit (CONTRIBUTING.md mục 2).
