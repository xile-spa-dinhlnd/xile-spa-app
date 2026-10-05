-- =====================================================================
-- V2__reference_data.sql  |  Danh mục khởi tạo
--
-- Chỉ chứa DANH MỤC (nhóm, loại chi phí, nguồn khách), lấy từ sheet DANH MỤC của file CRM.
-- KHÔNG chứa dịch vụ, giá, khách hàng hay tài khoản:
--   * Dịch vụ và giá: nhập từ Excel bằng FR-IMP-01 hoặc nhập tay.
--   * Tài khoản chủ tiệm: tạo bằng lệnh khởi tạo hoặc biến môi trường (FR-AUTH-07).
--   * Dữ liệu khách thật không được đưa vào repo (NFR-PRV-01).
-- =====================================================================

INSERT INTO service_group (name, sort_order) VALUES
    ('Gội đầu dưỡng sinh', 10),
    ('Chăm sóc da',        20),
    ('Stretching/Rehab',   30),
    ('Khác',               99);

INSERT INTO expense_category (name, kind) VALUES
    ('Chi mua hàng', 'PURCHASE'),
    ('Chi CTV',      'CTV'),
    ('Chi khác',     'OTHER');

INSERT INTO customer_source (name) VALUES
    ('Facebook Ads'),
    ('Fanpage'),
    ('TikTok'),
    ('Zalo'),
    ('Google'),
    ('Bạn bè giới thiệu'),
    ('Walk-in'),
    ('Website/Landing Page'),
    ('Remarketing'),
    ('CTV'),
    ('Khác');
