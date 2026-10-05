-- =====================================================================
-- later-releases.sql  |  Thiết kế các bảng của đợt R2 và R3 (CHƯA là migration)
--
-- Chạy được trên nền V1 + V2. Khi đến sprint tương ứng, chuyển nội dung thành
-- V3__..., V4__... trong backend/src/main/resources/db/migration (Flyway chỉ đi tiến,
-- nên không tạo sớm những bảng chưa dùng).
--
--   R2: customer_contact (FR-CUS-06), sync_job (FR-SYN-01..06), app_setting (FR-SYS-04)
--   R3: staff_payment + view tiền tour (FR-PAY-01..04), marketing_spend (FR-ADS-01, 02)
-- =====================================================================

-- ---------------------------------------------------------------------
-- R2
-- ---------------------------------------------------------------------

-- Lần liên hệ chăm sóc khách (FR-CUS-06).
CREATE TABLE customer_contact (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id       BIGINT      NOT NULL REFERENCES customer (id),
    contacted_on      DATE        NOT NULL,
    channel           VARCHAR(15) NOT NULL CONSTRAINT ck_cc_channel CHECK (channel IN
                          ('CALL', 'ZALO', 'SMS', 'FACEBOOK', 'IN_PERSON', 'OTHER')),
    outcome           VARCHAR(20) NOT NULL CONSTRAINT ck_cc_outcome CHECK (outcome IN
                          ('NOT_CONTACTED', 'NO_ANSWER', 'ZALO_SENT', 'CALLED', 'REBOOKED', 'DECLINED', 'CALL_BACK')),
    next_follow_up_on DATE,
    note              TEXT,
    created_by        BIGINT      NOT NULL REFERENCES app_user (id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_cc_customer ON customer_contact (customer_id, contacted_on DESC);
CREATE INDEX ix_cc_follow_up ON customer_contact (next_follow_up_on) WHERE next_follow_up_on IS NOT NULL;

-- Tác vụ đồng bộ Google Sheets của một ngày đã chốt (FR-SYN, BR-15).
-- Khóa ngoại tới daily_closing bảo đảm chỉ đồng bộ ngày đã chốt.
-- Dịch vụ chốt sổ thêm một hàng PENDING trong CÙNG transaction với việc chốt.
CREATE TABLE sync_job (
    business_date    DATE        PRIMARY KEY REFERENCES daily_closing (business_date),
    status           VARCHAR(10) NOT NULL DEFAULT 'PENDING' CONSTRAINT ck_sync_status CHECK (status IN
                         ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    attempts         INT         NOT NULL DEFAULT 0 CONSTRAINT ck_sync_attempts CHECK (attempts >= 0),
    next_attempt_at  TIMESTAMPTZ NOT NULL DEFAULT now(),   -- thử lại có giãn cách tăng dần (FR-SYN-03)
    last_error       TEXT,
    batch_id         UUID        NOT NULL DEFAULT gen_random_uuid(),   -- gắn nhãn lô để chạy lại không trùng (FR-SYN-04)
    started_at       TIMESTAMPTZ,
    succeeded_at     TIMESTAMPTZ,
    reconcile_result JSONB,      -- {"rows_db":.., "rows_sheet":.., "sum_db":.., "sum_sheet":..} (FR-SYN-05)
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_sync_succeeded CHECK (status <> 'SUCCEEDED' OR succeeded_at IS NOT NULL)
);
-- Worker lấy việc đến hạn.
CREATE INDEX ix_sync_due ON sync_job (next_attempt_at) WHERE status IN ('PENDING', 'FAILED');
CREATE TRIGGER trg_sync_job_touch BEFORE UPDATE ON sync_job FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- Cấu hình hệ thống (ngưỡng phân nhóm khách, mốc cảnh báo...) (FR-SYS-04).
CREATE TABLE app_setting (
    key        VARCHAR(100) PRIMARY KEY,
    value      JSONB        NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by BIGINT       REFERENCES app_user (id)
);

-- ---------------------------------------------------------------------
-- R3
-- ---------------------------------------------------------------------

-- Khoản đã trả cho nhân viên: tách khỏi tiền tour phát sinh (BR-14).
CREATE TABLE staff_payment (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    staff_id   BIGINT      NOT NULL REFERENCES staff (id),
    paid_on    DATE        NOT NULL,
    amount     BIGINT      NOT NULL CONSTRAINT ck_sp_amount CHECK (amount > 0),
    kind       VARCHAR(10) NOT NULL CONSTRAINT ck_sp_kind CHECK (kind IN ('SAME_DAY', 'ADVANCE', 'MONTHLY')),
    note       TEXT,
    created_by BIGINT      NOT NULL REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_sp_staff ON staff_payment (staff_id, paid_on);

-- Tiền tour phát sinh từ các dòng bán của lượt đến hợp lệ (BR-13). Không nhập tay.
CREATE VIEW v_staff_tour_accrual AS
SELECT a.staff_id,
       a.business_date,
       COUNT(*)        AS tours,
       SUM(a.tour_fee) AS accrued
FROM visit_item_amounts a
         JOIN effective_visit ev ON ev.id = a.visit_id
WHERE a.staff_id IS NOT NULL
GROUP BY a.staff_id, a.business_date;

-- Số còn nợ của một nhân viên tại một ngày bất kỳ: phát sinh trừ đã trả (FR-PAY-03).
CREATE FUNCTION staff_balance_as_of(p_staff bigint, p_date date)
    RETURNS TABLE (accrued bigint, paid bigint, owed bigint)
    LANGUAGE sql STABLE
AS $$
    SELECT acc.v, pay.v, acc.v - pay.v
    FROM (SELECT COALESCE(SUM(t.accrued), 0)::bigint AS v
          FROM v_staff_tour_accrual t WHERE t.staff_id = p_staff AND t.business_date <= p_date) acc,
         (SELECT COALESCE(SUM(p.amount), 0)::bigint AS v
          FROM staff_payment p WHERE p.staff_id = p_staff AND p.paid_on <= p_date) pay
$$;

-- Chi phí quảng cáo (FR-ADS-01). CPA, ROAS tính khi truy vấn (FR-ADS-02).
CREATE TABLE marketing_spend (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    spend_date DATE        NOT NULL,
    channel    VARCHAR(15) NOT NULL CONSTRAINT ck_ms_channel CHECK (channel IN
                   ('FACEBOOK_ADS', 'GOOGLE_ADS', 'TIKTOK_ADS', 'ZALO_OA', 'ORGANIC', 'OTHER')),
    cost       BIGINT      NOT NULL DEFAULT 0 CONSTRAINT ck_ms_cost CHECK (cost >= 0),
    leads      INT         NOT NULL DEFAULT 0 CONSTRAINT ck_ms_leads CHECK (leads >= 0),
    bookings   INT         NOT NULL DEFAULT 0 CONSTRAINT ck_ms_bookings CHECK (bookings >= 0),
    visits     INT         NOT NULL DEFAULT 0 CONSTRAINT ck_ms_visits CHECK (visits >= 0),
    revenue    BIGINT      NOT NULL DEFAULT 0 CONSTRAINT ck_ms_revenue CHECK (revenue >= 0),
    note       TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_ms_day_channel UNIQUE (spend_date, channel)
);
