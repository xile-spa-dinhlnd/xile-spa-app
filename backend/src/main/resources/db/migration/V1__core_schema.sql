-- =====================================================================
-- V1__core_schema.sql  |  Xile Spa Admin  |  PostgreSQL 16
--
-- Lược đồ lõi cho bản MVP (xem docs/design/erd.md).
--
-- Quy ước:
--   * Khóa chính: BIGINT IDENTITY. Tên bảng, cột: snake_case, tiếng Anh.
--   * Tiền: BIGINT, đơn vị đồng (BR-01). Ngày làm việc: DATE (BR-02).
--   * Giá trị phân loại cố định: VARCHAR + CHECK (mã tiếng Anh, giao diện dịch sang tiếng Việt).
--   * Danh mục người dùng tự quản lý: bảng riêng (service_group, expense_category, customer_source).
--   * Không lưu giá trị tính ra (tiền giảm, tiền khách thực trả, số lần đến...). Dùng view/hàm (BR-04, BR-11).
--   * Mã lỗi nghiệp vụ tự định nghĩa:
--       XL001 = ngày đã chốt sổ, không được thay đổi (BR-07)
--       XL002 = bảng chỉ thêm / không xóa cứng (BR-08, BR-09, BR-18)
--       XL004 = không chốt sổ ngày tương lai (BR-06)
--   * Yêu cầu: cơ sở dữ liệu dùng locale UTF-8 để lower() xử lý đúng chữ Việt.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Hàm tiện ích
-- ---------------------------------------------------------------------

-- "Hôm nay" theo giờ Việt Nam (BR-02).
CREATE FUNCTION today_vn() RETURNS date
    LANGUAGE sql STABLE
AS $$ SELECT (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date $$;

-- Chuẩn hóa tên để so sánh trùng: bỏ khoảng trắng thừa, không phân biệt hoa thường.
CREATE FUNCTION norm_name(t text) RETURNS text
    LANGUAGE sql IMMUTABLE
AS $$ SELECT lower(regexp_replace(btrim(t), '\s+', ' ', 'g')) $$;

-- Quy tắc tính tiền giảm: làm tròn đến đồng, nửa lên (BR-04). Đây là NƠI DUY NHẤT định nghĩa công thức.
CREATE FUNCTION calc_discount(list_price bigint, pct numeric) RETURNS bigint
    LANGUAGE sql IMMUTABLE
AS $$ SELECT round(list_price * pct / 100)::bigint $$;

CREATE FUNCTION touch_updated_at() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END
$$;

-- ---------------------------------------------------------------------
-- 2. Xác thực (AUTH)
-- ---------------------------------------------------------------------

CREATE TABLE app_user (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email              VARCHAR(254) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    display_name       VARCHAR(100) NOT NULL,
    role               VARCHAR(20)  NOT NULL DEFAULT 'OWNER'
                           CONSTRAINT ck_app_user_role CHECK (role IN ('OWNER', 'STAFF')),
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_count INT          NOT NULL DEFAULT 0 CONSTRAINT ck_app_user_failed CHECK (failed_login_count >= 0),
    locked_until       TIMESTAMPTZ,
    last_login_at      TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_app_user_email ON app_user (lower(email));

CREATE TABLE refresh_token (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,           -- băm SHA-256 của token, không lưu token gốc
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_token_user ON refresh_token (user_id);

CREATE TABLE password_reset_token (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,                      -- chỉ dùng một lần (FR-AUTH-03)
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_password_reset_token_hash UNIQUE (token_hash)
);
CREATE INDEX ix_password_reset_token_user ON password_reset_token (user_id);

-- ---------------------------------------------------------------------
-- 3. Danh mục: nhóm dịch vụ, dịch vụ, combo, nhân viên
-- ---------------------------------------------------------------------

CREATE TABLE service_group (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_service_group_name ON service_group (norm_name(name));

CREATE TABLE service (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id         BIGINT       NOT NULL REFERENCES service_group (id),
    name             VARCHAR(150) NOT NULL,
    duration_minutes INT          CONSTRAINT ck_service_duration CHECK (duration_minutes > 0),
    list_price       BIGINT       NOT NULL DEFAULT 0 CONSTRAINT ck_service_price CHECK (list_price >= 0),
    tour_fee         BIGINT       NOT NULL DEFAULT 0 CONSTRAINT ck_service_tour CHECK (tour_fee >= 0),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- "GỘI 60P" và "GỘI 60p " là một tên (FR-SVC-02).
CREATE UNIQUE INDEX ux_service_name ON service (norm_name(name));
CREATE INDEX ix_service_group ON service (group_id);
CREATE INDEX ix_service_active ON service (active);

CREATE TABLE service_price_history (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    service_id BIGINT      NOT NULL REFERENCES service (id),
    old_price  BIGINT      NOT NULL CONSTRAINT ck_sph_old CHECK (old_price >= 0),
    new_price  BIGINT      NOT NULL CONSTRAINT ck_sph_new CHECK (new_price >= 0),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    changed_by BIGINT      NOT NULL REFERENCES app_user (id)
);
CREATE INDEX ix_sph_service ON service_price_history (service_id, changed_at DESC);

CREATE TABLE combo (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    price      BIGINT       NOT NULL CONSTRAINT ck_combo_price CHECK (price >= 0),
    tour_fee   BIGINT       NOT NULL DEFAULT 0 CONSTRAINT ck_combo_tour CHECK (tour_fee >= 0), -- tạm theo combo (OQ-06)
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_combo_name ON combo (norm_name(name));

CREATE TABLE combo_item (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_id   BIGINT NOT NULL REFERENCES combo (id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL REFERENCES service (id),
    quantity   INT    NOT NULL DEFAULT 1 CONSTRAINT ck_combo_item_qty CHECK (quantity >= 1),
    CONSTRAINT ux_combo_item UNIQUE (combo_id, service_id)   -- không lặp một dịch vụ trong combo (FR-CMB-02)
);
CREATE INDEX ix_combo_item_service ON combo_item (service_id);

CREATE TABLE staff (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name  VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL CONSTRAINT ck_staff_role CHECK (role IN ('OWNER', 'THERAPIST')),
    user_id    BIGINT       REFERENCES app_user (id),      -- người có tài khoản đăng nhập (chủ tiệm)
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_staff_user UNIQUE (user_id)
);

-- ---------------------------------------------------------------------
-- 4. Khách hàng và gói liệu trình
-- ---------------------------------------------------------------------

CREATE TABLE customer_source (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name   VARCHAR(100) NOT NULL,
    active BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_customer_source_name ON customer_source (norm_name(name));

CREATE TABLE customer (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name           VARCHAR(150) NOT NULL,
    phone               VARCHAR(20)  CONSTRAINT ck_customer_phone CHECK (phone ~ '^\+[0-9]{8,15}$'), -- chuẩn E.164 (BR-10)
    birth_date          DATE,
    gender              VARCHAR(10)  CONSTRAINT ck_customer_gender CHECK (gender IN ('F', 'M', 'OTHER')),
    zalo                VARCHAR(100),
    facebook            VARCHAR(255),
    source_id           BIGINT       REFERENCES customer_source (id),
    occupation          VARCHAR(100),
    interested_group_id BIGINT       REFERENCES service_group (id),
    health_notes        TEXT,                              -- dữ liệu nhạy cảm (NFR-PRV-02)
    care_status         VARCHAR(20)  NOT NULL DEFAULT 'NEW'
                            CONSTRAINT ck_customer_care CHECK (care_status IN
                                ('NEW', 'CARING', 'BOOKED', 'BOUGHT_PACKAGE', 'VIP', 'DORMANT', 'LOST')),
    anonymized_at       TIMESTAMPTZ,                       -- ẩn danh theo yêu cầu của khách (FR-CUS-10)
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- SĐT bắt buộc, trừ hồ sơ đã ẩn danh (OQ-08 còn mở)
    CONSTRAINT ck_customer_phone_required CHECK (anonymized_at IS NOT NULL OR phone IS NOT NULL)
);
CREATE UNIQUE INDEX ux_customer_phone ON customer (phone) WHERE phone IS NOT NULL;
CREATE INDEX ix_customer_name ON customer (lower(full_name) text_pattern_ops);
-- Truy vấn sinh nhật theo tháng và ngày (FR-CUS-07).
CREATE INDEX ix_customer_birthday ON customer ((extract(month FROM birth_date)), (extract(day FROM birth_date)))
    WHERE birth_date IS NOT NULL;

CREATE TABLE customer_package (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id        BIGINT       NOT NULL REFERENCES customer (id),
    name               VARCHAR(150) NOT NULL,
    service_group_id   BIGINT       REFERENCES service_group (id),   -- để báo cáo (BR-20)
    sessions_purchased INT          NOT NULL CONSTRAINT ck_pkg_purchased CHECK (sessions_purchased > 0),
    sessions_bonus     INT          NOT NULL DEFAULT 0 CONSTRAINT ck_pkg_bonus CHECK (sessions_bonus >= 0),
    purchased_on       DATE         NOT NULL,
    expires_on         DATE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_pkg_dates CHECK (expires_on IS NULL OR expires_on >= purchased_on)
);
CREATE INDEX ix_customer_package_customer ON customer_package (customer_id);
-- Giá gói nằm ở dòng bán PACKAGE_SALE. "Đã dùng" = số dòng PACKAGE_USE, tính khi truy vấn (BR-12).

-- ---------------------------------------------------------------------
-- 5. Giao dịch, chi phí, chốt sổ, bút toán điều chỉnh
-- ---------------------------------------------------------------------

CREATE TABLE visit (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_date  DATE        NOT NULL,                       -- ngày làm việc (BR-02)
    customer_id    BIGINT      REFERENCES customer (id),       -- trống = khách vãng lai
    payment_method VARCHAR(10) NOT NULL CONSTRAINT ck_visit_payment CHECK (payment_method IN ('CASH', 'TRANSFER')),
    source_id      BIGINT      REFERENCES customer_source (id),
    note           TEXT,
    status         VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CONSTRAINT ck_visit_status CHECK (status IN ('ACTIVE', 'VOID')),
    voided_at      TIMESTAMPTZ,
    void_reason    TEXT,
    created_by     BIGINT      NOT NULL REFERENCES app_user (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_visit_void CHECK ((status = 'VOID') = (voided_at IS NOT NULL))
);
CREATE INDEX ix_visit_date ON visit (business_date);
CREATE INDEX ix_visit_customer ON visit (customer_id, business_date DESC) WHERE customer_id IS NOT NULL;

CREATE TABLE visit_item (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    visit_id            BIGINT       NOT NULL REFERENCES visit (id),
    item_type           VARCHAR(15)  NOT NULL CONSTRAINT ck_vi_type CHECK (item_type IN
                            ('SERVICE', 'COMBO', 'PACKAGE_SALE', 'PACKAGE_USE')),
    service_id          BIGINT       REFERENCES service (id),
    combo_id            BIGINT       REFERENCES combo (id),
    package_id          BIGINT       REFERENCES customer_package (id),
    staff_id            BIGINT       REFERENCES staff (id),      -- người thực hiện
    -- Ảnh chụp tại thời điểm bán (BR-03). Không tính lại từ danh mục.
    name_snapshot       VARCHAR(150) NOT NULL,
    list_price_snapshot BIGINT       NOT NULL CONSTRAINT ck_vi_price CHECK (list_price_snapshot >= 0),
    discount_percent    NUMERIC(5,2) NOT NULL DEFAULT 0 CONSTRAINT ck_vi_pct CHECK (discount_percent BETWEEN 0 AND 100),
    tour_fee_snapshot   BIGINT       NOT NULL DEFAULT 0 CONSTRAINT ck_vi_tour CHECK (tour_fee_snapshot >= 0),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_vi_shape CHECK (
           (item_type = 'SERVICE'      AND service_id IS NOT NULL AND combo_id IS NULL AND package_id IS NULL)
        OR (item_type = 'COMBO'        AND combo_id   IS NOT NULL AND service_id IS NULL AND package_id IS NULL)
        OR (item_type = 'PACKAGE_SALE' AND package_id IS NOT NULL AND service_id IS NULL AND combo_id IS NULL)
        -- Buổi dùng từ gói: giá tính doanh thu = 0 (BR-20), vẫn ghi dịch vụ đã làm để thống kê lượt
        OR (item_type = 'PACKAGE_USE'  AND package_id IS NOT NULL AND service_id IS NOT NULL AND combo_id IS NULL
            AND list_price_snapshot = 0 AND discount_percent = 0)
    )
);
CREATE INDEX ix_vi_visit ON visit_item (visit_id);
CREATE INDEX ix_vi_service ON visit_item (service_id) WHERE service_id IS NOT NULL;
CREATE INDEX ix_vi_staff ON visit_item (staff_id) WHERE staff_id IS NOT NULL;
CREATE INDEX ix_vi_package ON visit_item (package_id) WHERE package_id IS NOT NULL;
-- Mỗi gói chỉ được bán đúng một lần.
CREATE UNIQUE INDEX ux_vi_package_sale ON visit_item (package_id) WHERE item_type = 'PACKAGE_SALE';

CREATE TABLE expense_category (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name   VARCHAR(100) NOT NULL,
    kind   VARCHAR(10)  NOT NULL CONSTRAINT ck_expcat_kind CHECK (kind IN ('PURCHASE', 'CTV', 'OTHER')),
    active BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_expense_category_name ON expense_category (norm_name(name));

CREATE TABLE expense (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_date DATE        NOT NULL,
    category_id   BIGINT      NOT NULL REFERENCES expense_category (id),
    amount        BIGINT      NOT NULL CONSTRAINT ck_expense_amount CHECK (amount > 0),
    description   TEXT,
    status        VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CONSTRAINT ck_expense_status CHECK (status IN ('ACTIVE', 'VOID')),
    voided_at     TIMESTAMPTZ,
    void_reason   TEXT,
    created_by    BIGINT      NOT NULL REFERENCES app_user (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_expense_void CHECK ((status = 'VOID') = (voided_at IS NOT NULL))
);
CREATE INDEX ix_expense_date ON expense (business_date);
CREATE INDEX ix_expense_category ON expense (category_id);

-- Một hàng = một ngày đã chốt. Không có hàng = ngày đang mở (BR-06).
-- Là ảnh chụp bất biến của tổng kết ngày (FR-CLS-02).
CREATE TABLE daily_closing (
    business_date      DATE        PRIMARY KEY,
    gross_revenue      BIGINT      NOT NULL,   -- Σ giá niêm yết các dòng bán không bị hủy
    revenue_adjustment BIGINT      NOT NULL DEFAULT 0,   -- bút toán loại REVENUE ghi nhận vào ngày này
    discount_total     BIGINT      NOT NULL,   -- "chi giảm giá" (BR-05)
    purchase_expense   BIGINT      NOT NULL DEFAULT 0,
    ctv_expense        BIGINT      NOT NULL DEFAULT 0,
    other_expense      BIGINT      NOT NULL DEFAULT 0,
    expense_adjustment BIGINT      NOT NULL DEFAULT 0,   -- bút toán loại EXPENSE ghi nhận vào ngày này
    cash_collected     BIGINT      NOT NULL,   -- tiền khách thực trả bằng tiền mặt
    transfer_collected BIGINT      NOT NULL,   -- tiền khách thực trả bằng chuyển khoản
    visit_count        INT         NOT NULL,
    item_count         INT         NOT NULL,
    expense_count      INT         NOT NULL,
    adjustment_count   INT         NOT NULL,
    total_revenue      BIGINT GENERATED ALWAYS AS (gross_revenue + revenue_adjustment) STORED,
    total_expense      BIGINT GENERATED ALWAYS AS
                           (discount_total + purchase_expense + ctv_expense + other_expense + expense_adjustment) STORED,
    net_received       BIGINT GENERATED ALWAYS AS
                           ((gross_revenue + revenue_adjustment)
                            - (discount_total + purchase_expense + ctv_expense + other_expense + expense_adjustment)) STORED,
    closed_by          BIGINT      NOT NULL REFERENCES app_user (id),
    closed_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Bút toán điều chỉnh: chỉ thêm, không sửa, không xóa (BR-08).
CREATE TABLE adjustment (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recorded_on   DATE        NOT NULL,     -- ngày ghi nhận: ngày đang mở gần nhất
    original_date DATE        NOT NULL,     -- ngày gốc của khoản được điều chỉnh
    kind          VARCHAR(10) NOT NULL CONSTRAINT ck_adj_kind CHECK (kind IN ('REVENUE', 'EXPENSE')),
    amount        BIGINT      NOT NULL CONSTRAINT ck_adj_amount CHECK (amount <> 0),   -- có dấu
    reason        TEXT        NOT NULL CONSTRAINT ck_adj_reason CHECK (btrim(reason) <> ''),
    visit_id      BIGINT      REFERENCES visit (id),
    expense_id    BIGINT      REFERENCES expense (id),
    voids_visit   BOOLEAN     NOT NULL DEFAULT FALSE,   -- loại lượt đến nhập nhầm khỏi thống kê khách (OQ-16)
    created_by    BIGINT      NOT NULL REFERENCES app_user (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_adj_dates CHECK (original_date <= recorded_on),
    CONSTRAINT ck_adj_void_ref CHECK (NOT voids_visit OR visit_id IS NOT NULL)
);
CREATE INDEX ix_adj_recorded ON adjustment (recorded_on);
CREATE INDEX ix_adj_original ON adjustment (original_date);
CREATE INDEX ix_adj_visit ON adjustment (visit_id) WHERE visit_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- 6. Nhật ký thay đổi (chỉ thêm)
-- ---------------------------------------------------------------------

CREATE TABLE audit_log (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id    BIGINT      REFERENCES app_user (id),      -- trống = hệ thống
    action      VARCHAR(40) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id   VARCHAR(64),
    before_data JSONB,
    after_data  JSONB
);
CREATE INDEX ix_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX ix_audit_time ON audit_log (occurred_at DESC);

-- ---------------------------------------------------------------------
-- 7. Bảo vệ dữ liệu bằng trigger (BR-07, BR-08, BR-09, BR-18)
-- ---------------------------------------------------------------------

-- Khóa theo ngày làm việc. Dùng chung cho mọi lệnh ghi và cho thao tác chốt sổ,
-- để việc chốt và việc thêm giao dịch không thể chen nhau (xem docs/design/erd.md, mục 5).
CREATE FUNCTION lock_business_day(d date) RETURNS void
    LANGUAGE sql
AS $$ SELECT pg_advisory_xact_lock(7001, (d - DATE '2000-01-01')) $$;

CREATE FUNCTION assert_day_open(d date) RETURNS void
    LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM lock_business_day(d);
    IF EXISTS (SELECT 1 FROM daily_closing WHERE business_date = d) THEN
        RAISE EXCEPTION 'Ngày % đã chốt sổ, không được thay đổi dữ liệu (BR-07)', d
            USING ERRCODE = 'XL001';
    END IF;
END
$$;

CREATE FUNCTION trg_dated_row_lock() RETURNS trigger   -- dùng cho visit, expense
    LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM assert_day_open(NEW.business_date);
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        PERFORM assert_day_open(LEAST(OLD.business_date, NEW.business_date));
        PERFORM assert_day_open(GREATEST(OLD.business_date, NEW.business_date));
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'Không xóa cứng bảng %, dùng trạng thái VOID (BR-09)', TG_TABLE_NAME
        USING ERRCODE = 'XL002';
END
$$;

CREATE TRIGGER trg_visit_day_lock   BEFORE INSERT OR UPDATE OR DELETE ON visit
    FOR EACH ROW EXECUTE FUNCTION trg_dated_row_lock();
CREATE TRIGGER trg_expense_day_lock BEFORE INSERT OR UPDATE OR DELETE ON expense
    FOR EACH ROW EXECUTE FUNCTION trg_dated_row_lock();

CREATE FUNCTION trg_visit_item_day_lock() RETURNS trigger
    LANGUAGE plpgsql
AS $$
DECLARE
    d date;
BEGIN
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        SELECT business_date INTO d FROM visit WHERE id = NEW.visit_id;
        PERFORM assert_day_open(d);
    END IF;
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        SELECT business_date INTO d FROM visit WHERE id = OLD.visit_id;
        PERFORM assert_day_open(d);
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END
$$;
CREATE TRIGGER trg_visit_item_day_lock BEFORE INSERT OR UPDATE OR DELETE ON visit_item
    FOR EACH ROW EXECUTE FUNCTION trg_visit_item_day_lock();

-- Bút toán chỉ được ghi vào ngày đang mở (BR-08).
CREATE FUNCTION trg_adjustment_day_open() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM assert_day_open(NEW.recorded_on);
    RETURN NEW;
END
$$;
CREATE TRIGGER trg_adjustment_day_open BEFORE INSERT ON adjustment
    FOR EACH ROW EXECUTE FUNCTION trg_adjustment_day_open();

-- Bảng chỉ thêm: adjustment, daily_closing, audit_log.
CREATE FUNCTION trg_append_only() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Bảng % chỉ cho phép thêm, không được % (BR-08, BR-18)', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'XL002';
END
$$;
CREATE TRIGGER trg_adjustment_append_only    BEFORE UPDATE OR DELETE ON adjustment
    FOR EACH ROW EXECUTE FUNCTION trg_append_only();
CREATE TRIGGER trg_daily_closing_append_only BEFORE UPDATE OR DELETE ON daily_closing
    FOR EACH ROW EXECUTE FUNCTION trg_append_only();
CREATE TRIGGER trg_audit_log_append_only     BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION trg_append_only();

-- Không chốt sổ ngày tương lai (BR-06). Khóa ngày để chờ các lệnh ghi đang chạy trên ngày đó.
CREATE FUNCTION trg_daily_closing_insert() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM lock_business_day(NEW.business_date);
    IF NEW.business_date > today_vn() THEN
        RAISE EXCEPTION 'Không chốt sổ ngày tương lai (%)', NEW.business_date USING ERRCODE = 'XL004';
    END IF;
    RETURN NEW;
END
$$;
CREATE TRIGGER trg_daily_closing_insert BEFORE INSERT ON daily_closing
    FOR EACH ROW EXECUTE FUNCTION trg_daily_closing_insert();

-- Tự cập nhật updated_at.
CREATE TRIGGER trg_app_user_touch         BEFORE UPDATE ON app_user         FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_service_touch          BEFORE UPDATE ON service          FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_combo_touch            BEFORE UPDATE ON combo            FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_staff_touch            BEFORE UPDATE ON staff            FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_customer_touch         BEFORE UPDATE ON customer         FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_customer_package_touch BEFORE UPDATE ON customer_package FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_visit_touch            BEFORE UPDATE ON visit            FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_expense_touch          BEFORE UPDATE ON expense          FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- ---------------------------------------------------------------------
-- 8. View: nơi duy nhất cài đặt công thức BR-04, BR-05, BR-11
-- ---------------------------------------------------------------------

-- Từng dòng bán kèm giá trị tính ra.
CREATE VIEW visit_item_amounts AS
SELECT vi.id                    AS visit_item_id,
       vi.visit_id,
       v.business_date,
       v.payment_method,
       v.status                 AS visit_status,
       v.customer_id,
       vi.item_type,
       vi.service_id,
       vi.combo_id,
       vi.package_id,
       vi.staff_id,
       vi.name_snapshot,
       vi.list_price_snapshot   AS list_price,
       vi.discount_percent,
       vi.tour_fee_snapshot     AS tour_fee,
       calc_discount(vi.list_price_snapshot, vi.discount_percent)                        AS discount_amount,
       vi.list_price_snapshot - calc_discount(vi.list_price_snapshot, vi.discount_percent) AS paid_amount
FROM visit_item vi
         JOIN visit v ON v.id = vi.visit_id;

-- Lượt đến được tính vào thống kê khách: không bị hủy và không bị loại bằng bút toán (OQ-16).
CREATE VIEW effective_visit AS
SELECT v.*
FROM visit v
WHERE v.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM adjustment a WHERE a.visit_id = v.id AND a.voids_visit);

-- Tổng kết theo ngày (BR-05). Dùng cho xem trước chốt sổ, dashboard và để ghi ảnh chụp khi chốt.
CREATE VIEW v_daily_summary AS
WITH days AS (
    SELECT business_date AS d FROM visit
    UNION
    SELECT business_date FROM expense
    UNION
    SELECT recorded_on FROM adjustment
),
sales AS (
    SELECT business_date AS d,
           SUM(list_price)                                              AS gross_revenue,
           SUM(discount_amount)                                         AS discount_total,
           COALESCE(SUM(paid_amount) FILTER (WHERE payment_method = 'CASH'), 0)     AS cash_collected,
           COALESCE(SUM(paid_amount) FILTER (WHERE payment_method = 'TRANSFER'), 0) AS transfer_collected,
           COUNT(DISTINCT visit_id)                                     AS visit_count,
           COUNT(*)                                                     AS item_count
    FROM visit_item_amounts
    WHERE visit_status = 'ACTIVE'
    GROUP BY business_date
),
exp AS (
    SELECT e.business_date AS d,
           COALESCE(SUM(e.amount) FILTER (WHERE c.kind = 'PURCHASE'), 0) AS purchase_expense,
           COALESCE(SUM(e.amount) FILTER (WHERE c.kind = 'CTV'), 0)      AS ctv_expense,
           COALESCE(SUM(e.amount) FILTER (WHERE c.kind = 'OTHER'), 0)    AS other_expense,
           COUNT(*)                                                      AS expense_count
    FROM expense e
             JOIN expense_category c ON c.id = e.category_id
    WHERE e.status = 'ACTIVE'
    GROUP BY e.business_date
),
adj AS (
    SELECT recorded_on AS d,
           COALESCE(SUM(amount) FILTER (WHERE kind = 'REVENUE'), 0) AS revenue_adjustment,
           COALESCE(SUM(amount) FILTER (WHERE kind = 'EXPENSE'), 0) AS expense_adjustment,
           COUNT(*)                                                 AS adjustment_count
    FROM adjustment
    GROUP BY recorded_on
),
base AS (
    SELECT days.d                                AS business_date,
           COALESCE(s.gross_revenue, 0)          AS gross_revenue,
           COALESCE(a.revenue_adjustment, 0)     AS revenue_adjustment,
           COALESCE(s.discount_total, 0)         AS discount_total,
           COALESCE(x.purchase_expense, 0)       AS purchase_expense,
           COALESCE(x.ctv_expense, 0)            AS ctv_expense,
           COALESCE(x.other_expense, 0)          AS other_expense,
           COALESCE(a.expense_adjustment, 0)     AS expense_adjustment,
           COALESCE(s.cash_collected, 0)         AS cash_collected,
           COALESCE(s.transfer_collected, 0)     AS transfer_collected,
           COALESCE(s.visit_count, 0)::int       AS visit_count,
           COALESCE(s.item_count, 0)::int        AS item_count,
           COALESCE(x.expense_count, 0)::int     AS expense_count,
           COALESCE(a.adjustment_count, 0)::int  AS adjustment_count
    FROM days
             LEFT JOIN sales s ON s.d = days.d
             LEFT JOIN exp x   ON x.d = days.d
             LEFT JOIN adj a   ON a.d = days.d
)
SELECT b.*,
       b.gross_revenue + b.revenue_adjustment AS total_revenue,
       b.discount_total + b.purchase_expense + b.ctv_expense + b.other_expense + b.expense_adjustment AS total_expense,
       (b.gross_revenue + b.revenue_adjustment)
           - (b.discount_total + b.purchase_expense + b.ctv_expense + b.other_expense + b.expense_adjustment) AS net_received,
       EXISTS (SELECT 1 FROM daily_closing c WHERE c.business_date = b.business_date) AS is_closed
FROM base b;

-- Số liệu khách (BR-11): tính khi truy vấn, không lưu.
CREATE VIEW v_customer_stats AS
SELECT c.id                                          AS customer_id,
       COUNT(ev.id)                                  AS visit_count,
       COALESCE(SUM(p.paid), 0)                      AS total_spent,
       MIN(ev.business_date)                         AS first_visit,
       MAX(ev.business_date)                         AS last_visit,
       today_vn() - MAX(ev.business_date)            AS days_since_last_visit
FROM customer c
         LEFT JOIN effective_visit ev ON ev.customer_id = c.id
         LEFT JOIN LATERAL (
             SELECT SUM(a.paid_amount) AS paid FROM visit_item_amounts a WHERE a.visit_id = ev.id
         ) p ON TRUE
GROUP BY c.id;

-- ---------------------------------------------------------------------
-- 9. Chú thích cho các bảng chính
-- ---------------------------------------------------------------------
COMMENT ON TABLE visit_item        IS 'Dòng bán. Lưu ảnh chụp tên, giá niêm yết, % giảm, tiền tour tại lúc bán (BR-03). Tiền giảm và tiền khách thực trả tính qua view visit_item_amounts (BR-04).';
COMMENT ON TABLE daily_closing     IS 'Ảnh chụp bất biến của một ngày đã chốt. Có hàng = đã chốt (BR-06, BR-07).';
COMMENT ON TABLE adjustment        IS 'Bút toán điều chỉnh, chỉ thêm (BR-08). Ghi vào ngày đang mở gần nhất, kèm ngày gốc.';
COMMENT ON TABLE audit_log         IS 'Nhật ký thay đổi, chỉ thêm (BR-18).';
COMMENT ON VIEW  v_daily_summary   IS 'Công thức BR-05: tổng doanh thu, chi giảm giá, tổng chi, thực nhận theo ngày.';
COMMENT ON FUNCTION calc_discount(bigint, numeric) IS 'BR-04: làm tròn đến đồng, nửa lên.';
