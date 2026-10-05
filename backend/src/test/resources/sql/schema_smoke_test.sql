-- =====================================================================
-- schema_smoke_test.sql
--
-- Kiểm thử lược đồ V1 + V2 bằng psql, chạy trong một transaction rồi ROLLBACK (không để lại dữ liệu).
--   psql -v ON_ERROR_STOP=1 -d <db đã chạy V1, V2> -f schema_smoke_test.sql
-- Mọi kiểm tra sai sẽ làm lệnh dừng với mã lỗi khác 0.
--
-- Sau này nên chuyển các kiểm tra này thành kiểm thử Java dùng Testcontainers (NFR-MNT-02).
-- Nội dung kiểm chứng:
--   1. Danh mục khởi tạo (V2)
--   2. Công thức giảm giá (BR-04) và khớp số liệu Excel tháng 9 (BR-05, OQ-03)
--   3. Chốt sổ, khóa dữ liệu, bảng chỉ thêm (BR-06..BR-09, BR-18)
--   4. Bút toán điều chỉnh, gói liệu trình, thống kê khách (BR-08, BR-12, BR-20, OQ-16)
--   5. Các ràng buộc toàn vẹn dữ liệu
-- =====================================================================
\set ON_ERROR_STOP on
\pset pager off
BEGIN;

-- ---------- Hàm hỗ trợ (chỉ tồn tại trong phiên này) ----------
CREATE TEMP TABLE t_ids (name text PRIMARY KEY, id bigint NOT NULL);

CREATE FUNCTION pg_temp.id(n text) RETURNS bigint LANGUAGE sql AS $$ SELECT id FROM t_ids WHERE name = n $$;

-- Mong đợi một câu lệnh thất bại với đúng mã SQLSTATE.
CREATE FUNCTION pg_temp.expects(code text, stmt text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE stmt;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = code THEN
            RETURN;
        END IF;
        RAISE EXCEPTION 'Mong đợi lỗi % nhưng nhận % (%) ở câu: %', code, SQLSTATE, SQLERRM, stmt;
    END;
    RAISE EXCEPTION 'Mong đợi lỗi % nhưng câu lệnh chạy thành công: %', code, stmt;
END
$$;

-- Mô phỏng ứng dụng: tạo giao dịch một dịch vụ và chụp lại tên, giá, tour (BR-03).
CREATE FUNCTION pg_temp.add_service_visit(d date, cust bigint, pay text, svc text, pct numeric, stf text DEFAULT NULL)
    RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE v bigint;
BEGIN
    INSERT INTO visit (business_date, customer_id, payment_method, created_by)
    VALUES (d, cust, pay, pg_temp.id('owner')) RETURNING id INTO v;
    INSERT INTO visit_item (visit_id, item_type, service_id, staff_id, name_snapshot,
                            list_price_snapshot, discount_percent, tour_fee_snapshot)
    SELECT v, 'SERVICE', s.id, (SELECT id FROM staff WHERE full_name = stf), s.name, s.list_price, pct, s.tour_fee
    FROM service s WHERE s.name = svc;
    RETURN v;
END
$$;

-- =====================================================================
-- 1. Danh mục khởi tạo (V2)
-- =====================================================================
DO $$
BEGIN
    ASSERT (SELECT count(*) FROM service_group) = 4,     'V2: 4 nhóm dịch vụ';
    ASSERT (SELECT count(*) FROM expense_category) = 3,  'V2: 3 loại chi phí';
    ASSERT (SELECT count(*) FROM customer_source) = 11,  'V2: 11 nguồn khách';
    ASSERT (SELECT count(*) FROM service) = 0,           'V2 không được nạp dịch vụ hay giá';
    ASSERT (SELECT count(*) FROM customer) = 0,          'V2 không được nạp khách hàng';
    ASSERT (SELECT count(*) FROM app_user) = 0,          'V2 không được tạo tài khoản';
    RAISE NOTICE 'OK 1. Danh mục khởi tạo';
END $$;

-- ---------- Dữ liệu nền ----------
INSERT INTO app_user (email, password_hash, display_name) VALUES ('owner@example.com', 'x', 'Chủ tiệm');
INSERT INTO t_ids SELECT 'owner', id FROM app_user WHERE email = 'owner@example.com';

INSERT INTO staff (full_name, role, user_id) VALUES ('Chủ tiệm', 'OWNER', pg_temp.id('owner'));
INSERT INTO staff (full_name, role) VALUES ('KTV da', 'THERAPIST'), ('KTV giãn cơ', 'THERAPIST');

INSERT INTO service (group_id, name, list_price, tour_fee)
SELECT g.id, v.name, v.price, v.tour
FROM (VALUES ('Gội đầu dưỡng sinh', 'GỘI 30p', 69000, 10000),
             ('Gội đầu dưỡng sinh', 'GỘI 60p', 179000, 30000),
             ('Gội đầu dưỡng sinh', 'GỘI 80p', 289000, 40000),
             ('Chăm sóc da', 'DA CHUYÊN SÂU', 650000, 0)) AS v(grp, name, price, tour)
         JOIN service_group g ON g.name = v.grp;

-- =====================================================================
-- 2. Công thức giảm giá và khớp số liệu Excel tháng 9
-- =====================================================================
DO $$
BEGIN
    ASSERT calc_discount(179000, 44)  = 78760, '179.000 x 44% = 78.760';
    ASSERT calc_discount(289000, 10)  = 28900, '289.000 x 10% = 28.900';
    ASSERT calc_discount(650000, 10)  = 65000, '650.000 x 10% = 65.000';
    ASSERT calc_discount(1, 50)       = 1,     'làm tròn nửa lên: 0,5 -> 1';
    ASSERT calc_discount(333, 33.33)  = 111,   '333 x 33,33% = 110,9889 -> 111';
    ASSERT calc_discount(179000, 0)   = 0,     'không giảm';
    ASSERT calc_discount(179000, 100) = 179000,'giảm 100%';
    RAISE NOTICE 'OK 2a. calc_discount (BR-04)';
END $$;

-- Khách thử nghiệm (dùng cho phần 4)
INSERT INTO customer (full_name, phone) VALUES ('Khách thử 1', '+84900000001');
INSERT INTO t_ids SELECT 'c1', id FROM customer WHERE phone = '+84900000001';

-- Tái hiện các dòng 2..10 của sheet DOANH THU THÁNG (mỗi dòng Excel là một giao dịch một dịch vụ).
-- Dòng 10 thiếu cách thanh toán trong Excel, ở đây giả định tiền mặt. Dòng 8 gắn khách thử.
SELECT pg_temp.add_service_visit('2026-09-01', NULL, 'TRANSFER', 'GỘI 80p', 10);
SELECT pg_temp.add_service_visit('2026-09-01', NULL, 'TRANSFER', 'GỘI 80p', 10);
SELECT pg_temp.add_service_visit('2026-09-01', NULL, 'TRANSFER', 'DA CHUYÊN SÂU', 10);
SELECT pg_temp.add_service_visit('2026-09-01', NULL, 'CASH',     'GỘI 60p', 44);
SELECT pg_temp.add_service_visit('2026-09-01', NULL, 'CASH',     'GỘI 60p', 0);
SELECT pg_temp.add_service_visit('2026-09-01', NULL, 'CASH',     'GỘI 60p', 0);
INSERT INTO t_ids SELECT 'visit_row8', pg_temp.add_service_visit('2026-09-01', pg_temp.id('c1'), 'TRANSFER', 'GỘI 60p', 0);
SELECT pg_temp.add_service_visit('2026-09-01', NULL, 'CASH',     'GỘI 30p', 0);
SELECT pg_temp.add_service_visit('2026-09-01', NULL, 'CASH',     'GỘI 30p', 0);

INSERT INTO expense (business_date, category_id, amount, description, created_by)
SELECT '2026-09-01', id, 1402000, 'Chi mua hàng tháng 9 (tổng cột I)', pg_temp.id('owner')
FROM expense_category WHERE kind = 'PURCHASE';
INSERT INTO expense (business_date, category_id, amount, description, created_by)
SELECT '2026-09-01', id, 30000, 'Chi CTV (cột J)', pg_temp.id('owner')
FROM expense_category WHERE kind = 'CTV';

DO $$
DECLARE s v_daily_summary%ROWTYPE;
BEGIN
    SELECT * INTO s FROM v_daily_summary WHERE business_date = '2026-09-01';
    ASSERT s.gross_revenue   = 2082000, format('Tổng doanh thu = tổng cột D = 2.082.000, nhận %s', s.gross_revenue);
    ASSERT s.discount_total  = 201560,  format('Chi giảm giá = 201.560, nhận %s', s.discount_total);
    ASSERT s.purchase_expense = 1402000, 'Chi mua hàng';
    ASSERT s.ctv_expense     = 30000,   'Chi CTV';
    ASSERT s.total_expense   = 1633560, format('Tổng chi = H+I+J = 1.633.560, nhận %s', s.total_expense);
    ASSERT s.net_received    = 448440,  format('Thực nhận đúng = 448.440, nhận %s', s.net_received);
    ASSERT s.cash_collected + s.transfer_collected = 1880440, 'Tiền khách thực trả = tổng cột D trừ giảm giá';
    ASSERT s.cash_collected  = 596240 AND s.transfer_collected = 1284200, 'Tách tiền mặt và chuyển khoản';
    ASSERT s.visit_count = 9 AND s.item_count = 9, 'Số giao dịch và số dòng bán';
    ASSERT NOT s.is_closed, 'Chưa chốt';
    ASSERT s.net_received <> 315880, 'Khác với kết quả sai của Excel (315.880)';
    RAISE NOTICE 'OK 2b. Khớp số liệu tháng 9: thực nhận = % (Excel cũ ra 315880)', s.net_received;
END $$;

-- =====================================================================
-- 3. Chốt sổ, khóa dữ liệu, bảng chỉ thêm
-- =====================================================================
-- Một giao dịch ở ngày còn mở, dùng để thử "dời sang ngày đã chốt".
INSERT INTO t_ids SELECT 'open_visit', pg_temp.add_service_visit('2026-09-02', NULL, 'CASH', 'GỘI 60p', 0);

-- Chốt sổ 01/09: khóa ngày, rồi ghi ảnh chụp từ view (đúng như dịch vụ chốt sổ sẽ làm).
SELECT lock_business_day('2026-09-01');
INSERT INTO daily_closing (business_date, gross_revenue, revenue_adjustment, discount_total, purchase_expense,
                           ctv_expense, other_expense, expense_adjustment, cash_collected, transfer_collected,
                           visit_count, item_count, expense_count, adjustment_count, closed_by)
SELECT business_date, gross_revenue, revenue_adjustment, discount_total, purchase_expense, ctv_expense,
       other_expense, expense_adjustment, cash_collected, transfer_collected,
       visit_count, item_count, expense_count, adjustment_count, pg_temp.id('owner')
FROM v_daily_summary WHERE business_date = '2026-09-01';

DO $$
DECLARE c daily_closing%ROWTYPE;
BEGIN
    SELECT * INTO c FROM daily_closing WHERE business_date = '2026-09-01';
    ASSERT c.total_revenue = 2082000 AND c.total_expense = 1633560 AND c.net_received = 448440,
           'Cột sinh tự động của daily_closing: tổng doanh thu, tổng chi, thực nhận';
    ASSERT (SELECT is_closed FROM v_daily_summary WHERE business_date = '2026-09-01'), 'View báo ngày đã chốt';
    RAISE NOTICE 'OK 3a. Chốt sổ 01/09';
END $$;

-- Mọi thay đổi vào ngày đã chốt phải bị chặn (XL001).
SELECT pg_temp.expects('XL001', $q$ INSERT INTO visit (business_date, payment_method, created_by)
    VALUES ('2026-09-01', 'CASH', pg_temp.id('owner')) $q$);
SELECT pg_temp.expects('XL001', $q$ UPDATE visit SET note = 'sửa trộm' WHERE business_date = '2026-09-01' $q$);
SELECT pg_temp.expects('XL001', $q$ UPDATE visit SET status = 'VOID', voided_at = now() WHERE business_date = '2026-09-01' $q$);
SELECT pg_temp.expects('XL001', $q$ UPDATE visit_item SET discount_percent = 0
    WHERE visit_id IN (SELECT id FROM visit WHERE business_date = '2026-09-01') $q$);
SELECT pg_temp.expects('XL001', $q$ DELETE FROM visit_item
    WHERE visit_id IN (SELECT id FROM visit WHERE business_date = '2026-09-01') $q$);
SELECT pg_temp.expects('XL001', $q$ INSERT INTO visit_item (visit_id, item_type, service_id, name_snapshot, list_price_snapshot)
    SELECT pg_temp.id('visit_row8'), 'SERVICE', id, name, list_price FROM service LIMIT 1 $q$);
SELECT pg_temp.expects('XL001', $q$ INSERT INTO expense (business_date, category_id, amount, created_by)
    SELECT '2026-09-01', id, 1000, pg_temp.id('owner') FROM expense_category LIMIT 1 $q$);
SELECT pg_temp.expects('XL001', $q$ UPDATE expense SET amount = 1 WHERE business_date = '2026-09-01' $q$);
SELECT pg_temp.expects('XL001', format($q$ UPDATE visit SET business_date = '2026-09-01' WHERE id = %s $q$, pg_temp.id('open_visit')));
SELECT pg_temp.expects('XL001', $q$ INSERT INTO adjustment (recorded_on, original_date, kind, amount, reason, created_by)
    VALUES ('2026-09-01', '2026-09-01', 'REVENUE', -1, 'ghi vào ngày đã chốt', pg_temp.id('owner')) $q$);

-- Bảng chỉ thêm, không hard delete (XL002).
SELECT pg_temp.expects('XL002', $q$ UPDATE daily_closing SET gross_revenue = 0 $q$);
SELECT pg_temp.expects('XL002', $q$ DELETE FROM daily_closing $q$);
SELECT pg_temp.expects('XL002', $q$ DELETE FROM visit WHERE business_date = '2026-09-02' $q$);
SELECT pg_temp.expects('XL002', $q$ DELETE FROM expense $q$);
INSERT INTO audit_log (actor_id, action, entity_type, entity_id, after_data)
VALUES (pg_temp.id('owner'), 'CLOSE', 'daily_closing', '2026-09-01', '{"net_received": 448440}');
SELECT pg_temp.expects('XL002', $q$ UPDATE audit_log SET action = 'x' $q$);
SELECT pg_temp.expects('XL002', $q$ DELETE FROM audit_log $q$);

-- Không chốt sổ ngày tương lai (XL004).
SELECT pg_temp.expects('XL004', $q$ INSERT INTO daily_closing (business_date, gross_revenue, discount_total, cash_collected,
    transfer_collected, visit_count, item_count, expense_count, adjustment_count, closed_by)
    VALUES (today_vn() + 1, 0, 0, 0, 0, 0, 0, 0, 0, pg_temp.id('owner')) $q$);

-- Ngày đang mở vẫn sửa được bình thường.
UPDATE visit SET note = 'sửa được' WHERE id = pg_temp.id('open_visit');
DO $$ BEGIN RAISE NOTICE 'OK 3b. Khóa ngày đã chốt (XL001), bảng chỉ thêm và không hard delete (XL002), chặn chốt ngày tương lai (XL004)'; END $$;

-- =====================================================================
-- 4. Bút toán điều chỉnh, gói liệu trình, thống kê khách
-- =====================================================================
-- Ngày 02/09 (đang mở): các giao dịch của khách thử.
SELECT pg_temp.add_service_visit('2026-09-02', pg_temp.id('c1'), 'CASH', 'GỘI 30p', 0);

-- Bán gói 10 buổi + 3 buổi tặng, giá 2.890.000, giảm 10%. Tạo gói trước rồi mới tạo dòng bán tham chiếu tới gói.
INSERT INTO customer_package (customer_id, name, service_group_id, sessions_purchased, sessions_bonus, purchased_on, expires_on)
SELECT pg_temp.id('c1'), 'Gội Premium 10 buổi', id, 10, 3, '2026-09-02', '2026-12-02'
FROM service_group WHERE name = 'Gội đầu dưỡng sinh';
INSERT INTO t_ids SELECT 'pkg', max(id) FROM customer_package;

INSERT INTO visit (business_date, customer_id, payment_method, created_by)
VALUES ('2026-09-02', pg_temp.id('c1'), 'TRANSFER', pg_temp.id('owner'));
INSERT INTO visit_item (visit_id, item_type, package_id, name_snapshot, list_price_snapshot, discount_percent)
VALUES ((SELECT max(id) FROM visit), 'PACKAGE_SALE', pg_temp.id('pkg'), 'Gội Premium 10 buổi', 2890000, 10);

-- Hai buổi dùng từ gói: giá tính doanh thu = 0 (BR-20).
DO $$
DECLARE i int; v bigint;
BEGIN
    FOR i IN 1..2 LOOP
        INSERT INTO visit (business_date, customer_id, payment_method, created_by)
        VALUES ('2026-09-02', pg_temp.id('c1'), 'CASH', pg_temp.id('owner')) RETURNING id INTO v;
        INSERT INTO visit_item (visit_id, item_type, service_id, package_id, name_snapshot, list_price_snapshot, discount_percent)
        SELECT v, 'PACKAGE_USE', s.id, pg_temp.id('pkg'), s.name, 0, 0 FROM service s WHERE s.name = 'GỘI 60p';
    END LOOP;
END $$;

-- Một giao dịch bị hủy (VOID): không tính vào doanh thu.
DO $$
DECLARE v bigint;
BEGIN
    v := pg_temp.add_service_visit('2026-09-02', NULL, 'CASH', 'GỘI 60p', 0);
    UPDATE visit SET status = 'VOID', voided_at = now(), void_reason = 'nhập nhầm' WHERE id = v;
END $$;

-- Bút toán điều chỉnh ghi vào 02/09 cho sai sót ngày 01/09:
--   (a) loại lượt đến dòng 8 (nhập nhầm): doanh thu -179.000, đánh dấu voids_visit
--   (b) quên một khoản chi 5.000
INSERT INTO adjustment (recorded_on, original_date, kind, amount, reason, visit_id, voids_visit, created_by)
VALUES ('2026-09-02', '2026-09-01', 'REVENUE', -179000, 'Nhập nhầm lượt đến', pg_temp.id('visit_row8'), TRUE, pg_temp.id('owner'));
INSERT INTO adjustment (recorded_on, original_date, kind, amount, reason, created_by)
VALUES ('2026-09-02', '2026-09-01', 'EXPENSE', 5000, 'Quên ghi chi mua đồ', pg_temp.id('owner'));

DO $$
DECLARE s v_daily_summary%ROWTYPE; st v_customer_stats%ROWTYPE; used int; total int;
BEGIN
    SELECT * INTO s FROM v_daily_summary WHERE business_date = '2026-09-02';
    -- Doanh thu: GỘI 60p (giao dịch mở, 179.000) + GỘI 30p (69.000) + bán gói (2.890.000). Buổi dùng gói và giao dịch VOID = 0.
    ASSERT s.gross_revenue = 179000 + 69000 + 2890000, format('gross 02/09 nhận %s', s.gross_revenue);
    ASSERT s.revenue_adjustment = -179000 AND s.expense_adjustment = 5000, 'Bút toán tính vào ngày ghi nhận 02/09';
    ASSERT s.discount_total = 289000, 'Giảm giá gói 10% = 289.000';
    ASSERT s.total_revenue = 2959000, format('total_revenue nhận %s', s.total_revenue);
    ASSERT s.total_expense = 289000 + 5000, format('total_expense nhận %s', s.total_expense);
    ASSERT s.net_received = 2959000 - 294000, format('net_received nhận %s', s.net_received);
    ASSERT s.visit_count = 5 AND s.item_count = 5 AND s.adjustment_count = 2, 'Số lượng: giao dịch VOID không đếm';
    ASSERT NOT s.is_closed, '02/09 vẫn mở';
    -- Ngày 01/09 đã chốt không bị bút toán làm thay đổi.
    ASSERT (SELECT net_received FROM daily_closing WHERE business_date = '2026-09-01') = 448440, '01/09 không đổi sau bút toán';
    ASSERT (SELECT net_received FROM v_daily_summary WHERE business_date = '2026-09-01') = 448440, 'View 01/09 cũng không đổi';

    -- Gói liệu trình: đã dùng tính khi truy vấn (BR-12).
    SELECT count(*) INTO used FROM visit_item vi JOIN visit v ON v.id = vi.visit_id
    WHERE vi.package_id = pg_temp.id('pkg') AND vi.item_type = 'PACKAGE_USE' AND v.status = 'ACTIVE';
    SELECT sessions_purchased + sessions_bonus INTO total FROM customer_package WHERE id = pg_temp.id('pkg');
    ASSERT used = 2 AND total = 13 AND total - used = 11, 'Gói: tổng 13, đã dùng 2, còn 11';

    -- Thống kê khách: lượt đến nhập nhầm (đã chốt, bị loại bằng bút toán) không tính (OQ-16).
    SELECT * INTO st FROM v_customer_stats WHERE customer_id = pg_temp.id('c1');
    ASSERT st.visit_count = 4, format('Số lần đến = 4 (không tính lượt bị loại), nhận %s', st.visit_count);
    ASSERT st.total_spent = 69000 + 2601000, format('Tổng chi tiêu nhận %s', st.total_spent);
    ASSERT st.last_visit = '2026-09-02' AND st.first_visit = '2026-09-02', 'Ngày đầu, ngày cuối đến';
    ASSERT NOT EXISTS (SELECT 1 FROM effective_visit WHERE id = pg_temp.id('visit_row8')), 'effective_visit loại lượt bị bút toán hủy';
    RAISE NOTICE 'OK 4. Bút toán điều chỉnh, gói liệu trình, thống kê khách';
END $$;

-- =====================================================================
-- 5. Ràng buộc toàn vẹn dữ liệu
-- =====================================================================
-- Mã 23514 = check_violation, 23505 = unique_violation, 23503 = foreign_key_violation
SELECT pg_temp.expects('23514', $q$ INSERT INTO visit_item (visit_id, item_type, service_id, name_snapshot, list_price_snapshot, discount_percent)
    SELECT pg_temp.id('open_visit'), 'SERVICE', id, name, list_price, 101 FROM service LIMIT 1 $q$);   -- giảm > 100%
SELECT pg_temp.expects('23514', $q$ INSERT INTO visit_item (visit_id, item_type, service_id, name_snapshot, list_price_snapshot)
    SELECT pg_temp.id('open_visit'), 'SERVICE', id, name, -1 FROM service LIMIT 1 $q$);                 -- giá âm
SELECT pg_temp.expects('23514', format($q$ INSERT INTO visit_item (visit_id, item_type, service_id, package_id, name_snapshot, list_price_snapshot)
    SELECT %s, 'PACKAGE_USE', id, %s, name, 1000 FROM service LIMIT 1 $q$, pg_temp.id('open_visit'), pg_temp.id('pkg')));  -- dùng gói mà có giá
SELECT pg_temp.expects('23514', $q$ INSERT INTO visit_item (visit_id, item_type, name_snapshot, list_price_snapshot)
    VALUES (pg_temp.id('open_visit'), 'SERVICE', 'thiếu service_id', 1000) $q$);                         -- SERVICE phải có service_id
SELECT pg_temp.expects('23514', $q$ INSERT INTO visit_item (visit_id, item_type, service_id, combo_id, name_snapshot, list_price_snapshot)
    SELECT pg_temp.id('open_visit'), 'SERVICE', id, 1, name, 1000 FROM service LIMIT 1 $q$);            -- hai loại tham chiếu cùng lúc
SELECT pg_temp.expects('23505', format($q$ INSERT INTO visit_item (visit_id, item_type, package_id, name_snapshot, list_price_snapshot)
    VALUES (%s, 'PACKAGE_SALE', %s, 'bán gói lần hai', 1000) $q$, pg_temp.id('open_visit'), pg_temp.id('pkg')));  -- gói chỉ bán một lần
SELECT pg_temp.expects('23505', $q$ INSERT INTO service (group_id, name) SELECT id, '  gội   60P ' FROM service_group LIMIT 1 $q$); -- trùng tên đã chuẩn hóa
SELECT pg_temp.expects('23505', $q$ INSERT INTO customer (full_name, phone) VALUES ('Trùng SĐT', '+84900000001') $q$);
SELECT pg_temp.expects('23514', $q$ INSERT INTO customer (full_name, phone) VALUES ('SĐT sai chuẩn', '0901234567') $q$);   -- không phải E.164
SELECT pg_temp.expects('23514', $q$ INSERT INTO customer (full_name) VALUES ('Không có SĐT') $q$);                         -- SĐT bắt buộc
INSERT INTO customer (full_name, phone, anonymized_at) VALUES ('Khách đã ẩn danh', NULL, now());                         -- ẩn danh thì được trống SĐT
SELECT pg_temp.expects('23514', $q$ INSERT INTO expense (business_date, category_id, amount, created_by)
    SELECT '2026-09-03', id, 0, pg_temp.id('owner') FROM expense_category LIMIT 1 $q$);                  -- chi phí phải > 0
SELECT pg_temp.expects('23514', $q$ INSERT INTO adjustment (recorded_on, original_date, kind, amount, reason, created_by)
    VALUES ('2026-09-03', '2026-09-01', 'REVENUE', -1, '   ', pg_temp.id('owner')) $q$);                 -- lý do không được trống
SELECT pg_temp.expects('23514', $q$ INSERT INTO adjustment (recorded_on, original_date, kind, amount, reason, created_by)
    VALUES ('2026-09-03', '2026-09-01', 'REVENUE', 0, 'số tiền 0', pg_temp.id('owner')) $q$);            -- số tiền khác 0
SELECT pg_temp.expects('23514', $q$ INSERT INTO adjustment (recorded_on, original_date, kind, amount, reason, voids_visit, created_by)
    VALUES ('2026-09-03', '2026-09-01', 'REVENUE', -1, 'hủy lượt mà không gắn giao dịch', TRUE, pg_temp.id('owner')) $q$);
SELECT pg_temp.expects('23514', $q$ INSERT INTO adjustment (recorded_on, original_date, kind, amount, reason, created_by)
    VALUES ('2026-09-03', '2026-09-04', 'REVENUE', -1, 'ngày gốc sau ngày ghi nhận', pg_temp.id('owner')) $q$);

-- Combo: không lặp dịch vụ trong một combo.
INSERT INTO combo (name, price) VALUES ('Combo thử', 500000);
INSERT INTO combo_item (combo_id, service_id, quantity)
SELECT (SELECT max(id) FROM combo), id, 1 FROM service WHERE name = 'GỘI 60p';
SELECT pg_temp.expects('23505', $q$ INSERT INTO combo_item (combo_id, service_id)
    SELECT (SELECT max(id) FROM combo), id FROM service WHERE name = 'GỘI 60p' $q$);
SELECT pg_temp.expects('23514', $q$ INSERT INTO combo_item (combo_id, service_id, quantity)
    SELECT (SELECT max(id) FROM combo), id, 0 FROM service WHERE name = 'GỘI 80p' $q$);

-- Hạn gói không được trước ngày mua; email tài khoản không phân biệt hoa thường.
SELECT pg_temp.expects('23514', $q$ INSERT INTO customer_package (customer_id, name, sessions_purchased, purchased_on, expires_on)
    VALUES (pg_temp.id('c1'), 'Hạn sai', 5, '2026-09-10', '2026-09-01') $q$);
SELECT pg_temp.expects('23505', $q$ INSERT INTO app_user (email, password_hash, display_name) VALUES ('OWNER@Example.com', 'x', 'Trùng email') $q$);

DO $$ BEGIN RAISE NOTICE 'OK 5. Ràng buộc toàn vẹn dữ liệu'; END $$;

ROLLBACK;
\echo 'TẤT CẢ KIỂM TRA ĐÃ QUA. Dữ liệu thử đã được ROLLBACK.'
