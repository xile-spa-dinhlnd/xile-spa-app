# 7. Câu hỏi còn mở

Cột **Ai quyết định** cho biết cần hỏi ai. Câu hỏi đã có quyết định được chuyển sang `Closed` và ghi rõ quyết định, kèm quy tắc hoặc yêu cầu đã được cập nhật. Câu hỏi có độ chặn **Cao** phải xong trước sprint tương ứng.

## Đã quyết định

| ID | Câu hỏi | Quyết định | Ngày | Đã cập nhật |
|---|---|---|---|---|
| OQ-01 | Làm tròn tiền giảm giá | **Làm tròn đến đồng** (nửa lên) | 01/10/2026 | BR-04 |
| OQ-03 | Công thức "THỰC NHẬN" trong Excel trừ giảm giá hai lần | Giữ cách tính của chủ tiệm (giảm giá nằm trong Tổng chi) nhưng **bỏ cột "thu sau giảm"**, để Tổng doanh thu là số trước giảm. Xem chi tiết bên dưới | 01/10/2026 | BR-05, FR-DSH-07 |
| OQ-04 | Doanh thu của gói liệu trình tính lúc bán hay lúc dùng | **Tính đủ lúc bán gói.** Buổi dùng từ gói có giá tính doanh thu = 0 | 01/10/2026 | BR-20, FR-PKG, FR-VIS-12 |
| OQ-05 | Cách sửa ngày đã chốt | Nhóm tự chọn: **chỉ dùng bút toán điều chỉnh, không có mở lại sổ**; bút toán ghi vào ngày đang mở gần nhất, lưu kèm ngày gốc. Xem ADR-0003 | 01/10/2026 | BR-06, BR-08, FR-CLS-04, FR-CLS-07, ADR-0003 |
| OQ-13 | "Chi giảm giá" có tính vào chi không | **Có**, giữ theo cách chủ tiệm quen: giảm giá nằm trong Tổng chi. Loại chi nhập tay mặc định: *mua hàng*, *CTV*, *khác* (danh mục thêm được) | 01/10/2026 | BR-05, FR-EXP |

## Còn mở

| ID | Câu hỏi | Liên quan | Ai quyết định | Chặn sprint | Trạng thái |
|---|---|---|---|---|---|
| OQ-02 | Ranh giới ngày: nếu tiệm có khách sau nửa đêm thì tính vào ngày nào? Giờ mở và đóng cửa thông thường? | BR-02 | Chủ tiệm | Trung bình | Open |
| OQ-06 | Tiền tour cho combo và liệu trình da: chủ tiệm tự làm thì tour = 0. Khi KTV làm combo thì tính tour theo combo hay theo từng dịch vụ thành phần? | BR-13, FR-CMB-06 | Chủ tiệm | Thấp (trước R2/R3) | Open |
| OQ-07 | Một giao dịch có thể trả bằng hai hình thức (một phần CK, một phần TM) không? | FR-VIS-04 | Chủ tiệm | Trung bình | Open |
| OQ-08 | Khách vãng lai không để lại SĐT: ghi giao dịch không cần hồ sơ khách, hay tạo hồ sơ tạm? Có trường hợp hai khách dùng chung một SĐT không? | BR-10, FR-CUS-01 | Chủ tiệm | Trung bình | Open |
| OQ-09 | Cấu trúc Google Sheets: một spreadsheet mỗi tháng với tab nào? Tài khoản Google nào sở hữu? Có đưa tên, SĐT khách vào Sheets không hay chỉ số liệu tổng hợp? | FR-SYN-01, NFR-PRV-04 | Chủ tiệm và nhóm | Cao, trước R2 | Open |
| OQ-10 | Tên miền và dịch vụ gửi email cho tính năng quên mật khẩu (Resend, Brevo, Gmail SMTP...). | FR-AUTH-03, A-05 | Nhóm | Cao, trước Sprint làm xác thực | Open |
| OQ-11 | Ngưỡng phân nhóm khách (VIP từ 3.000.000 đ, thân thiết từ 3 lần) và các mốc cảnh báo có đúng ý chủ tiệm không? Tên các nhóm còn lại? | BR-17, FR-CUS-05 | Chủ tiệm | Thấp | Open |
| OQ-12 | Dữ liệu lịch sử cần nhập vào hệ thống mới: bao nhiêu tháng? Có file nào ngoài hai file đã gửi? | FR-IMP-01, FR-IMP-02 | Chủ tiệm | Trung bình | Open |
| OQ-14 | Dashboard có hiển thị số liệu ngày chưa chốt không, hay mặc định chỉ tính ngày đã chốt? | BR-16, FR-DSH-09 | Chủ tiệm | Trung bình | Open |
| OQ-15 | Lương: ngoài tour có các khoản khác (lương cứng, thưởng, phạt, phụ cấp) cần ghi không? | FR-PAY | Chủ tiệm | Thấp (trước R3) | Open |
| OQ-16 | Giao dịch nhập nhầm ở ngày đã chốt (nhầm khách, nhầm dịch vụ): bút toán chỉ sửa được **số tiền**. Có cần cách loại lượt đến đó khỏi thống kê của khách (số lần đến, tổng chi tiêu) không, hay chấp nhận để nguyên? Đề xuất: bút toán có cờ "hủy lượt đến" gắn với giao dịch gốc | BR-08, FR-CLS-04, FR-CUS-03 | Nhóm (đề xuất đã có trong ERD) | Trung bình | Proposed: cờ `adjustment.voids_visit` + view `effective_visit` (xem `docs/design/erd.md`); chờ chủ tiệm đồng ý |
| OQ-17 | Tiền tour trả cho nhân viên có tính vào **Tổng chi** của ngày không? (BR-05 hiện chỉ gồm chi giảm giá, mua hàng, CTV, chi khác) | BR-05, BR-14, FR-PAY | Chủ tiệm | Thấp (trước R3) | Open |
| OQ-18 | Tiệm có chịu phí cho kênh gửi tin tự động (Zalo ZNS cần Zalo OA, SMS brandname) không? Hiện nhóm đoán là không vì tiệm nhỏ; nếu không thì chỉ gửi thủ công (FR-AI-02) | FR-AI-02 | Chủ tiệm | Thấp (trước R3) | Open, chưa hỏi |
| OQ-19 | Chủ tiệm có đồng ý cho hệ thống gửi tên gọi và lịch sử dịch vụ của khách sang mô hình AI không? Nếu tự host mô hình thì dữ liệu không rời máy chủ của tiệm | FR-AI-01, NFR-PRV-05 | Chủ tiệm | Thấp (trước R3) | Đình đồng ý; chờ chủ tiệm xác nhận |
| OQ-20 | Chạy mô hình AI ở đâu: tự host bằng Ollama (Spring AI) trên máy riêng hay máy chủ mạnh hơn, hay gọi API bên ngoài? VPS hiện tại (4 GB RAM, NFR-PERF-03) không đủ chạy mô hình cùng hệ thống; VPS này phục vụ học tập và MVP cho người nhà dùng trước, khi sản phẩm được tin dùng sẽ có kinh phí thuê VPS mạnh hơn. Quyết định ghi thành ADR khi bắt đầu làm | FR-AI-05, C-01, NFR-PERF-03 | Nhóm | Thấp (trước R3) | Hướng dự kiến: dev chạy Ollama trên máy cá nhân; production tự host trên VPS mạnh hơn khi có kinh phí |

## Chi tiết OQ-03: lỗi trừ hai lần trong Excel và cách sửa

Trong file `THÁNG 9 NĂM 2026 - SOURCE.xlsx`, sheet `DOANH THU THÁNG`:

- `TỔNG DOANH THU` (L) = tổng cột `THU SAU GIẢM` (G), tức đã là số **sau** giảm giá.
- `TỔNG CHI` (K) = `CHI GIẢM GIÁ` (H) + chi mua hàng (I) + chi CTV (J).
- `THỰC NHẬN` (M) = L − tổng chi.

Giảm giá bị trừ hai lần: một lần khi tính G (thu sau giảm), một lần nữa vì H nằm trong tổng chi.

**Cách sửa đã chốt (theo đề xuất của chủ tiệm):** bỏ cột "thu sau giảm", để `Tổng doanh thu` là tổng giá **trước** giảm, giữ "chi giảm giá" trong tổng chi:

1. Tổng doanh thu = Σ giá niêm yết
2. Giảm giá = giá × % giảm
3. Tổng chi = giảm giá + chi mua hàng + chi CTV (+ chi khác)
4. Thực nhận = Tổng doanh thu − Tổng chi

Cách này cho cùng kết quả với việc giữ "thu sau giảm" rồi không tính giảm giá vào chi, vì `D − (H + I + J) = (D − H) − I − J`. Hai cách chỉ khác nhau về cách trình bày. Cách của chủ tiệm được chọn vì khớp với thói quen và dễ hiểu hơn.

| Chỉ tiêu (dữ liệu tháng 9, 9 dòng có tên dịch vụ) | Số tiền (đ) |
|---|---|
| Tổng doanh thu theo giá niêm yết (Σ D) | 2.082.000 |
| Giảm giá (H) | 201.560 |
| Chi mua hàng (I) | 1.402.000 |
| Chi CTV (J) | 30.000 |
| Tổng chi (H + I + J) | 1.633.560 |
| **Thực nhận theo cách mới** | **448.440** |
| Thực nhận theo Excel hiện tại (M) | 315.880 |

**Một dòng dữ liệu cần chủ tiệm xác nhận:** dòng 46 của sheet có số **69.000 đ gõ tay đè lên công thức** ở cột "thu sau giảm", không có tên dịch vụ (đúng bằng giá GỘI 30p). Excel hiện tại đang cộng số này vào tổng doanh thu. Nếu đó là một lượt gội 30p có thật thì Tổng doanh thu là 2.151.000 đ và Thực nhận là 517.440 đ; nếu không thì là 448.440 đ như bảng trên. Đây cũng là lý do hệ thống mới tính mọi số từ các dòng bán thay vì cho gõ đè số.

Khi nhập dữ liệu lịch sử (FR-IMP), **không nhập các cột tổng của Excel** (Tổng doanh thu, Thực nhận). Hệ thống tính lại từ từng dòng bán theo BR-05.
