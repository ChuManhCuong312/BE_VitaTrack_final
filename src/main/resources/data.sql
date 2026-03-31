-- ============================================================
-- VitaTrack Seed Data v4 – Dữ liệu mẫu phong phú
-- Mật khẩu tất cả tài khoản: VitaTrack@123
-- Password hash: $2a$12$sVwTL00LElB8ajtOh6O8g.OAQMFAuZyTXswoR0LcpAsPhA/7EaESq
--
-- Tài khoản:
--   admin@vitatrack.vn      (Admin)
--   expert1@vitatrack.vn    (Chuyên gia dinh dưỡng)
--   expert2@vitatrack.vn    (Bác sĩ thể thao)
--   user1@vitatrack.vn      (Nam – giảm cân – 21 ngày dữ liệu)
--   user2@vitatrack.vn      (Nữ – giảm cân – 14 ngày dữ liệu)
--   user3@vitatrack.vn      (Nam – tăng cơ – 10 ngày dữ liệu)
-- ============================================================
USE DB_VitaTrack;
GO

SET NOCOUNT ON;
GO

-- ══════════════════════════════════════════════════════════════
-- 1. USERS
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT 1 FROM users WHERE email='admin@vitatrack.vn')
  INSERT INTO users
    (email, password_hash, full_name, role, is_active, is_email_verified,
     last_login_at, created_at, updated_at)
  VALUES
    ('admin@vitatrack.vn',
     '$2a$12$sVwTL00LElB8ajtOh6O8g.OAQMFAuZyTXswoR0LcpAsPhA/7EaESq',
     N'Quản trị viên', 'ADMIN', 1, 1,
     DATEADD(hour,-2,GETDATE()), DATEADD(day,-90,GETDATE()), GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE email='expert1@vitatrack.vn')
  INSERT INTO users
    (email, password_hash, full_name, role, is_active, is_email_verified,
     expert_status, last_login_at, created_at, updated_at)
  VALUES
    ('expert1@vitatrack.vn',
     '$2a$12$sVwTL00LElB8ajtOh6O8g.OAQMFAuZyTXswoR0LcpAsPhA/7EaESq',
     N'ThS. Nguyễn Bích Phượng', 'EXPERT', 1, 1,
     'verified', DATEADD(hour,-5,GETDATE()), DATEADD(day,-60,GETDATE()), GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE email='expert2@vitatrack.vn')
  INSERT INTO users
    (email, password_hash, full_name, role, is_active, is_email_verified,
     expert_status, last_login_at, created_at, updated_at)
  VALUES
    ('expert2@vitatrack.vn',
     '$2a$12$sVwTL00LElB8ajtOh6O8g.OAQMFAuZyTXswoR0LcpAsPhA/7EaESq',
     N'BS. Trần Minh Khoa', 'EXPERT', 1, 1,
     'verified', DATEADD(day,-1,GETDATE()), DATEADD(day,-45,GETDATE()), GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE email='user1@vitatrack.vn')
  INSERT INTO users
    (email, password_hash, full_name, role, is_active, is_email_verified,
     last_login_at, created_at, updated_at)
  VALUES
    ('user1@vitatrack.vn',
     '$2a$12$sVwTL00LElB8ajtOh6O8g.OAQMFAuZyTXswoR0LcpAsPhA/7EaESq',
     N'Nguyễn Văn An', 'USER', 1, 1,
     DATEADD(hour,-1,GETDATE()), DATEADD(day,-30,GETDATE()), GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE email='user2@vitatrack.vn')
  INSERT INTO users
    (email, password_hash, full_name, role, is_active, is_email_verified,
     last_login_at, created_at, updated_at)
  VALUES
    ('user2@vitatrack.vn',
     '$2a$12$sVwTL00LElB8ajtOh6O8g.OAQMFAuZyTXswoR0LcpAsPhA/7EaESq',
     N'Trần Thị Bích', 'USER', 1, 1,
     DATEADD(hour,-3,GETDATE()), DATEADD(day,-20,GETDATE()), GETDATE());

IF NOT EXISTS (SELECT 1 FROM users WHERE email='user3@vitatrack.vn')
  INSERT INTO users
    (email, password_hash, full_name, role, is_active, is_email_verified,
     last_login_at, created_at, updated_at)
  VALUES
    ('user3@vitatrack.vn',
     '$2a$12$sVwTL00LElB8ajtOh6O8g.OAQMFAuZyTXswoR0LcpAsPhA/7EaESq',
     N'Lê Hoàng Nam', 'USER', 1, 1,
     DATEADD(hour,-6,GETDATE()), DATEADD(day,-12,GETDATE()), GETDATE());
GO

-- ══════════════════════════════════════════════════════════════
-- 2. HEALTH PROFILES
-- ══════════════════════════════════════════════════════════════
-- user1: Nam 29t, 172cm, 73.5kg → mục tiêu giảm còn 68kg (BMI 24.87)
IF NOT EXISTS (SELECT 1 FROM health_profiles WHERE user_id=(SELECT id FROM users WHERE email='user1@vitatrack.vn'))
  INSERT INTO health_profiles
    (user_id, height_cm, weight_kg, gender, age, activity_level, goal_type,
     target_weight_kg, bmi, bmr, tdee, daily_calorie_goal, daily_steps_goal, daily_water_goal,
     created_at, updated_at)
  VALUES (
    (SELECT id FROM users WHERE email='user1@vitatrack.vn'),
    172.0, 73.5, 'MALE', 29, 'MODERATE', 'LOSE_WEIGHT',
    68.0, 24.87, 1798.0, 2787.0, 2300, 10000, 2500,
    DATEADD(day,-28,GETDATE()), GETDATE()
  );

-- user2: Nữ 25t, 158cm, 55kg → mục tiêu giảm còn 50kg (BMI 22.04)
IF NOT EXISTS (SELECT 1 FROM health_profiles WHERE user_id=(SELECT id FROM users WHERE email='user2@vitatrack.vn'))
  INSERT INTO health_profiles
    (user_id, height_cm, weight_kg, gender, age, activity_level, goal_type,
     target_weight_kg, bmi, bmr, tdee, daily_calorie_goal, daily_steps_goal, daily_water_goal,
     created_at, updated_at)
  VALUES (
    (SELECT id FROM users WHERE email='user2@vitatrack.vn'),
    158.0, 55.0, 'FEMALE', 25, 'LIGHT', 'LOSE_WEIGHT',
    50.0, 22.04, 1336.0, 1872.0, 1500, 8000, 2000,
    DATEADD(day,-18,GETDATE()), GETDATE()
  );

-- user3: Nam 22t, 175cm, 62kg → mục tiêu tăng lên 67kg (BMI 20.24)
IF NOT EXISTS (SELECT 1 FROM health_profiles WHERE user_id=(SELECT id FROM users WHERE email='user3@vitatrack.vn'))
  INSERT INTO health_profiles
    (user_id, height_cm, weight_kg, gender, age, activity_level, goal_type,
     target_weight_kg, bmi, bmr, tdee, daily_calorie_goal, daily_steps_goal, daily_water_goal,
     created_at, updated_at)
  VALUES (
    (SELECT id FROM users WHERE email='user3@vitatrack.vn'),
    175.0, 62.0, 'MALE', 22, 'ACTIVE', 'GAIN_WEIGHT',
    67.0, 20.24, 1748.0, 2622.0, 3000, 12000, 3000,
    DATEADD(day,-10,GETDATE()), GETDATE()
  );
GO

-- ══════════════════════════════════════════════════════════════
-- 3. WEIGHT HISTORY (21 ngày – user1; 14 ngày – user2; 10 ngày – user3)
-- ══════════════════════════════════════════════════════════════
DECLARE @hp1 BIGINT = (SELECT id FROM health_profiles WHERE user_id=(SELECT id FROM users WHERE email='user1@vitatrack.vn'));
DECLARE @hp2 BIGINT = (SELECT id FROM health_profiles WHERE user_id=(SELECT id FROM users WHERE email='user2@vitatrack.vn'));
DECLARE @hp3 BIGINT = (SELECT id FROM health_profiles WHERE user_id=(SELECT id FROM users WHERE email='user3@vitatrack.vn'));

-- user1: 21 ngày giảm dần từ 76.0 → 73.5 kg
IF @hp1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM weight_history WHERE health_profile_id=@hp1)
  INSERT INTO weight_history (health_profile_id, weight, bmi, source, notes, recorded_at)
  VALUES
    (@hp1, 76.0, 25.71, 'profile_update', N'Bắt đầu chương trình giảm cân', DATEADD(day,-21,GETDATE())),
    (@hp1, 75.8, 25.64, 'manual',         NULL,                               DATEADD(day,-20,GETDATE())),
    (@hp1, 75.6, 25.57, 'manual',         N'Sau buổi tập gym',                DATEADD(day,-19,GETDATE())),
    (@hp1, 75.4, 25.50, 'manual',         NULL,                               DATEADD(day,-18,GETDATE())),
    (@hp1, 75.3, 25.47, 'manual',         NULL,                               DATEADD(day,-17,GETDATE())),
    (@hp1, 75.1, 25.40, 'manual',         N'Tuần 1 kết thúc',                 DATEADD(day,-14,GETDATE())),
    (@hp1, 74.9, 25.33, 'manual',         NULL,                               DATEADD(day,-13,GETDATE())),
    (@hp1, 74.7, 25.26, 'manual',         NULL,                               DATEADD(day,-12,GETDATE())),
    (@hp1, 74.6, 25.23, 'manual',         NULL,                               DATEADD(day,-11,GETDATE())),
    (@hp1, 74.5, 25.20, 'manual',         N'Cân sau khi ăn sáng',             DATEADD(day,-10,GETDATE())),
    (@hp1, 74.3, 25.13, 'manual',         NULL,                               DATEADD(day, -9,GETDATE())),
    (@hp1, 74.2, 25.10, 'manual',         N'Tuần 2 kết thúc',                 DATEADD(day, -7,GETDATE())),
    (@hp1, 74.0, 25.03, 'manual',         NULL,                               DATEADD(day, -6,GETDATE())),
    (@hp1, 73.9, 24.99, 'manual',         NULL,                               DATEADD(day, -5,GETDATE())),
    (@hp1, 73.8, 24.96, 'manual',         NULL,                               DATEADD(day, -4,GETDATE())),
    (@hp1, 73.7, 24.93, 'manual',         N'Sau marathon sáng',               DATEADD(day, -3,GETDATE())),
    (@hp1, 73.6, 24.90, 'manual',         NULL,                               DATEADD(day, -2,GETDATE())),
    (@hp1, 73.5, 24.87, 'manual',         N'Tiếp tục tiến trình tốt!',        DATEADD(day, -1,GETDATE())),
    (@hp1, 73.5, 24.87, 'profile_update', NULL,                               GETDATE());

-- user2: 14 ngày giảm dần từ 57.0 → 55.0 kg
IF @hp2 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM weight_history WHERE health_profile_id=@hp2)
  INSERT INTO weight_history (health_profile_id, weight, bmi, source, notes, recorded_at)
  VALUES
    (@hp2, 57.0, 22.84, 'profile_update', N'Bắt đầu theo dõi',   DATEADD(day,-14,GETDATE())),
    (@hp2, 56.7, 22.72, 'manual',         NULL,                    DATEADD(day,-13,GETDATE())),
    (@hp2, 56.6, 22.68, 'manual',         NULL,                    DATEADD(day,-12,GETDATE())),
    (@hp2, 56.4, 22.60, 'manual',         N'Sau buổi yoga',        DATEADD(day,-11,GETDATE())),
    (@hp2, 56.3, 22.56, 'manual',         NULL,                    DATEADD(day,-10,GETDATE())),
    (@hp2, 56.2, 22.52, 'manual',         N'Tuần 1 kết thúc',      DATEADD(day, -7,GETDATE())),
    (@hp2, 56.0, 22.44, 'manual',         NULL,                    DATEADD(day, -6,GETDATE())),
    (@hp2, 55.9, 22.40, 'manual',         NULL,                    DATEADD(day, -5,GETDATE())),
    (@hp2, 55.7, 22.32, 'manual',         N'Sau bơi lội',          DATEADD(day, -4,GETDATE())),
    (@hp2, 55.6, 22.28, 'manual',         NULL,                    DATEADD(day, -3,GETDATE())),
    (@hp2, 55.4, 22.20, 'manual',         NULL,                    DATEADD(day, -2,GETDATE())),
    (@hp2, 55.2, 22.12, 'manual',         N'Tốt lắm!',             DATEADD(day, -1,GETDATE())),
    (@hp2, 55.0, 22.04, 'profile_update', NULL,                    GETDATE());

-- user3: 10 ngày tăng dần từ 61.5 → 62.0 kg
IF @hp3 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM weight_history WHERE health_profile_id=@hp3)
  INSERT INTO weight_history (health_profile_id, weight, bmi, source, notes, recorded_at)
  VALUES
    (@hp3, 61.5, 20.08, 'profile_update', N'Bắt đầu tập gym',  DATEADD(day,-10,GETDATE())),
    (@hp3, 61.5, 20.08, 'manual',         NULL,                  DATEADD(day, -9,GETDATE())),
    (@hp3, 61.6, 20.12, 'manual',         N'Sau ngày Leg day',   DATEADD(day, -8,GETDATE())),
    (@hp3, 61.7, 20.15, 'manual',         NULL,                  DATEADD(day, -7,GETDATE())),
    (@hp3, 61.7, 20.15, 'manual',         NULL,                  DATEADD(day, -6,GETDATE())),
    (@hp3, 61.8, 20.18, 'manual',         N'Tăng đều',           DATEADD(day, -5,GETDATE())),
    (@hp3, 61.9, 20.22, 'manual',         NULL,                  DATEADD(day, -4,GETDATE())),
    (@hp3, 61.9, 20.22, 'manual',         NULL,                  DATEADD(day, -3,GETDATE())),
    (@hp3, 62.0, 20.24, 'manual',         NULL,                  DATEADD(day, -2,GETDATE())),
    (@hp3, 62.0, 20.24, 'profile_update', NULL,                  GETDATE());
GO

-- ══════════════════════════════════════════════════════════════
-- 4. FOODS (35 thực phẩm phổ biến Việt Nam)
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT 1 FROM foods WHERE name=N'Cơm trắng')
  INSERT INTO foods (name, category, calories_per_100g, protein_per_100g, fat_per_100g, carbs_per_100g, fiber_per_100g, serving_unit, allergens, created_by_admin, is_active)
  VALUES
    (N'Cơm trắng',            N'Ngũ cốc & Tinh bột',   130.0,  2.7,  0.3, 28.2, 0.4, N'1 chén = 200g',       NULL,    1,1),
    (N'Cơm gạo lứt',          N'Ngũ cốc & Tinh bột',   111.0,  2.6,  0.9, 23.0, 1.8, N'1 chén = 200g',       NULL,    1,1),
    (N'Yến mạch',             N'Ngũ cốc & Tinh bột',   389.0, 17.0,  7.0, 66.0, 10.6, N'1 gói = 40g',         N'Gluten',1,1),
    (N'Bánh mì ốp la',        N'Ngũ cốc & Tinh bột',   280.0,  9.0, 13.0, 32.0, 2.0, N'1 ổ = 150g',          N'Gluten',1,1),
    (N'Phở bò',               N'Ngũ cốc & Tinh bột',   120.0,  7.5,  2.5, 17.0, 0.5, N'1 tô = 500g',         NULL,    1,1),
    (N'Bún bò Huế',           N'Ngũ cốc & Tinh bột',   135.0,  9.0,  3.5, 18.0, 0.4, N'1 tô = 500g',         N'Hải sản',1,1),
    (N'Khoai lang nướng',     N'Ngũ cốc & Tinh bột',    86.0,  1.6,  0.1, 20.0, 3.0, N'1 củ = 130g',         NULL,    1,1),
    (N'Mì gói',               N'Ngũ cốc & Tinh bột',   454.0,  9.0, 18.0, 64.0, 1.5, N'1 gói = 75g',         N'Gluten',1,1),
    (N'Ức gà luộc',           N'Thịt & Gia cầm',       165.0, 31.0,  3.6,  0.0, 0.0, N'1 miếng = 120g',      NULL,    1,1),
    (N'Ức gà nướng',          N'Thịt & Gia cầm',       159.0, 32.0,  3.2,  0.0, 0.0, N'1 miếng = 120g',      NULL,    1,1),
    (N'Thịt bò xào',          N'Thịt & Gia cầm',       217.0, 26.0, 12.0,  0.0, 0.0, N'1 phần = 100g',       NULL,    1,1),
    (N'Ức heo luộc',          N'Thịt & Gia cầm',       143.0, 26.0,  4.0,  0.0, 0.0, N'1 miếng = 100g',      NULL,    1,1),
    (N'Trứng chiên',          N'Sữa & Trứng',          196.0, 13.6, 15.3,  0.6, 0.0, N'1 quả = 60g',         N'Trứng', 1,1),
    (N'Trứng luộc',           N'Sữa & Trứng',          155.0, 13.0, 11.0,  1.1, 0.0, N'1 quả = 60g',         N'Trứng', 1,1),
    (N'Sữa tươi không đường', N'Sữa & Trứng',           42.0,  3.4,  1.0,  5.0, 0.0, N'1 hộp = 200ml',       N'Lactose',1,1),
    (N'Yogurt không đường',   N'Sữa & Trứng',           59.0, 10.0,  0.4,  3.6, 0.0, N'1 hũ = 100g',         N'Lactose',1,1),
    (N'Cá hồi nướng',         N'Hải sản',              208.0, 20.0, 13.0,  0.0, 0.0, N'1 miếng = 150g',      N'Hải sản',1,1),
    (N'Tôm luộc',             N'Hải sản',               99.0, 24.0,  0.3,  0.0, 0.0, N'100g tôm sạch',       N'Hải sản',1,1),
    (N'Cá basa hấp',          N'Hải sản',               96.0, 18.0,  2.5,  0.0, 0.0, N'1 miếng = 120g',      N'Hải sản',1,1),
    (N'Đậu phụ sốt cà',       N'Protein',               80.0,  8.0,  4.0,  3.0, 0.5, N'1 bìa = 200g',        N'Đậu nành',1,1),
    (N'Đậu đen',              N'Protein',              132.0,  8.9,  0.5, 24.0, 8.7, N'1 chén = 170g',        NULL,    1,1),
    (N'Rau muống xào',        N'Rau củ',                21.0,  2.6,  0.3,  3.1, 1.8, N'1 dĩa = 100g',        NULL,    1,1),
    (N'Bắp cải xào',          N'Rau củ',                25.0,  1.3,  0.1,  5.8, 2.5, N'1 dĩa = 100g',        NULL,    1,1),
    (N'Cải xanh luộc',        N'Rau củ',                32.0,  3.0,  0.4,  5.6, 2.8, N'1 dĩa = 100g',        NULL,    1,1),
    (N'Salad rau trộn',       N'Rau củ',                20.0,  1.5,  0.2,  3.8, 2.0, N'1 tô = 150g',         NULL,    1,1),
    (N'Súp bí đỏ',            N'Rau củ',                49.0,  1.2,  1.8,  7.5, 0.5, N'1 bát = 200g',        NULL,    1,1),
    (N'Chuối',                N'Trái cây',               89.0,  1.1,  0.3, 23.0, 2.6, N'1 quả = 100g',        NULL,    1,1),
    (N'Táo',                  N'Trái cây',               52.0,  0.3,  0.2, 14.0, 2.4, N'1 quả = 150g',        NULL,    1,1),
    (N'Cam',                  N'Trái cây',               47.0,  0.9,  0.1, 12.0, 2.4, N'1 quả = 180g',        NULL,    1,1),
    (N'Dưa hấu',              N'Trái cây',               30.0,  0.6,  0.2,  7.6, 0.4, N'1 lát = 200g',        NULL,    1,1),
    (N'Sinh tố bơ',           N'Đồ uống',              160.0,  2.0, 14.0,  9.0, 6.7, N'1 ly = 250ml',        NULL,    1,1),
    (N'Nước cam ép',          N'Đồ uống',               45.0,  0.7,  0.2, 10.4, 0.2, N'1 ly = 200ml',        NULL,    1,1),
    (N'Hạt hạnh nhân',        N'Gia vị & Dầu ăn',      579.0, 21.0, 50.0, 22.0, 12.5, N'1 nắm = 30g',       N'Hạt cây',1,1),
    (N'Dầu ô liu',            N'Gia vị & Dầu ăn',      884.0,  0.0,100.0,  0.0, 0.0, N'1 muỗng = 14g',       NULL,    1,1),
    (N'Whey protein',         N'Protein',              400.0, 80.0,  5.0, 10.0, 0.0, N'1 muỗng = 30g',       N'Lactose',1,1);
GO

-- ══════════════════════════════════════════════════════════════
-- 5. ALLERGIES
-- ══════════════════════════════════════════════════════════════
-- user2 dị ứng Lactose (trung bình) + Gluten (nhẹ)
IF NOT EXISTS (SELECT 1 FROM allergies WHERE user_id=(SELECT id FROM users WHERE email='user2@vitatrack.vn') AND allergen_name='Lactose')
  INSERT INTO allergies (user_id, allergen_name, severity)
  VALUES ((SELECT id FROM users WHERE email='user2@vitatrack.vn'), 'Lactose', 'MEDIUM');

IF NOT EXISTS (SELECT 1 FROM allergies WHERE user_id=(SELECT id FROM users WHERE email='user2@vitatrack.vn') AND allergen_name='Gluten')
  INSERT INTO allergies (user_id, allergen_name, severity)
  VALUES ((SELECT id FROM users WHERE email='user2@vitatrack.vn'), 'Gluten', 'LOW');

-- user3 dị ứng hải sản (nghiêm trọng)
IF NOT EXISTS (SELECT 1 FROM allergies WHERE user_id=(SELECT id FROM users WHERE email='user3@vitatrack.vn') AND allergen_name=N'Hải sản')
  INSERT INTO allergies (user_id, allergen_name, severity)
  VALUES ((SELECT id FROM users WHERE email='user3@vitatrack.vn'), N'Hải sản', 'HIGH');
GO

-- ══════════════════════════════════════════════════════════════
-- 6. MEAL LOGS – user1 (21 ngày)
-- ══════════════════════════════════════════════════════════════
DECLARE @u1 BIGINT = (SELECT id FROM users WHERE email='user1@vitatrack.vn');
DECLARE @u2 BIGINT = (SELECT id FROM users WHERE email='user2@vitatrack.vn');
DECLARE @u3 BIGINT = (SELECT id FROM users WHERE email='user3@vitatrack.vn');

-- Food IDs
DECLARE @fRice     BIGINT = (SELECT id FROM foods WHERE name=N'Cơm trắng');
DECLARE @fBrownRice BIGINT= (SELECT id FROM foods WHERE name=N'Cơm gạo lứt');
DECLARE @fOat      BIGINT = (SELECT id FROM foods WHERE name=N'Yến mạch');
DECLARE @fBread    BIGINT = (SELECT id FROM foods WHERE name=N'Bánh mì ốp la');
DECLARE @fPho      BIGINT = (SELECT id FROM foods WHERE name=N'Phở bò');
DECLARE @fChicken  BIGINT = (SELECT id FROM foods WHERE name=N'Ức gà luộc');
DECLARE @fChickenG BIGINT = (SELECT id FROM foods WHERE name=N'Ức gà nướng');
DECLARE @fBeef     BIGINT = (SELECT id FROM foods WHERE name=N'Thịt bò xào');
DECLARE @fPork     BIGINT = (SELECT id FROM foods WHERE name=N'Ức heo luộc');
DECLARE @fEggFry   BIGINT = (SELECT id FROM foods WHERE name=N'Trứng chiên');
DECLARE @fEggBoil  BIGINT = (SELECT id FROM foods WHERE name=N'Trứng luộc');
DECLARE @fMilk     BIGINT = (SELECT id FROM foods WHERE name=N'Sữa tươi không đường');
DECLARE @fYogurt   BIGINT = (SELECT id FROM foods WHERE name=N'Yogurt không đường');
DECLARE @fSalmon   BIGINT = (SELECT id FROM foods WHERE name=N'Cá hồi nướng');
DECLARE @fShrimp   BIGINT = (SELECT id FROM foods WHERE name=N'Tôm luộc');
DECLARE @fTofu     BIGINT = (SELECT id FROM foods WHERE name=N'Đậu phụ sốt cà');
DECLARE @fVeg1     BIGINT = (SELECT id FROM foods WHERE name=N'Rau muống xào');
DECLARE @fVeg2     BIGINT = (SELECT id FROM foods WHERE name=N'Bắp cải xào');
DECLARE @fVeg3     BIGINT = (SELECT id FROM foods WHERE name=N'Cải xanh luộc');
DECLARE @fSalad    BIGINT = (SELECT id FROM foods WHERE name=N'Salad rau trộn');
DECLARE @fBanana   BIGINT = (SELECT id FROM foods WHERE name=N'Chuối');
DECLARE @fApple    BIGINT = (SELECT id FROM foods WHERE name=N'Táo');
DECLARE @fOrange   BIGINT = (SELECT id FROM foods WHERE name=N'Cam');
DECLARE @fSweetPot BIGINT = (SELECT id FROM foods WHERE name=N'Khoai lang nướng');
DECLARE @fWhey     BIGINT = (SELECT id FROM foods WHERE name=N'Whey protein');

-- Kiểm tra và insert meal logs user1 cho 21 ngày (chỉ insert nếu chưa có)
IF NOT EXISTS (SELECT 1 FROM meal_logs WHERE user_id=@u1)
BEGIN
  -- ── Ngày 21 (3 tuần trước) ──
  INSERT INTO meal_logs (user_id,food_id,meal_type,log_date,quantity,unit,calories,protein,carbs,fat,created_at) VALUES
    (@u1,@fBread,  'breakfast',CAST(DATEADD(day,-21,GETDATE())AS DATE),150,'g',420,13.5,48.0,19.5,DATEADD(day,-21,GETDATE())),
    (@u1,@fEggFry, 'breakfast',CAST(DATEADD(day,-21,GETDATE())AS DATE),100,'g',196,13.6, 0.6,15.3,DATEADD(day,-21,GETDATE())),
    (@u1,@fRice,   'lunch',    CAST(DATEADD(day,-21,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-21,GETDATE())),
    (@u1,@fChicken,'lunch',    CAST(DATEADD(day,-21,GETDATE())AS DATE),150,'g',248,46.5, 0.0, 5.4,DATEADD(day,-21,GETDATE())),
    (@u1,@fVeg1,   'lunch',    CAST(DATEADD(day,-21,GETDATE())AS DATE),100,'g', 21, 2.6, 3.1, 0.3,DATEADD(day,-21,GETDATE())),
    (@u1,@fSalmon, 'dinner',   CAST(DATEADD(day,-21,GETDATE())AS DATE),150,'g',312,30.0, 0.0,19.5,DATEADD(day,-21,GETDATE())),
    (@u1,@fRice,   'dinner',   CAST(DATEADD(day,-21,GETDATE())AS DATE),150,'g',195, 4.1,42.3, 0.5,DATEADD(day,-21,GETDATE())),
    (@u1,@fBanana, 'snack',    CAST(DATEADD(day,-21,GETDATE())AS DATE),120,'g',107, 1.3,27.6, 0.4,DATEADD(day,-21,GETDATE()));
  -- ── Ngày 20 ──
  INSERT INTO meal_logs (user_id,food_id,meal_type,log_date,quantity,unit,calories,protein,carbs,fat,created_at) VALUES
    (@u1,@fOat,    'breakfast',CAST(DATEADD(day,-20,GETDATE())AS DATE),80, 'g',311,13.6,52.8, 5.6,DATEADD(day,-20,GETDATE())),
    (@u1,@fMilk,   'breakfast',CAST(DATEADD(day,-20,GETDATE())AS DATE),200,'ml', 84, 6.8,10.0, 2.0,DATEADD(day,-20,GETDATE())),
    (@u1,@fPho,    'lunch',    CAST(DATEADD(day,-20,GETDATE())AS DATE),400,'g',480,30.0,68.0,10.0,DATEADD(day,-20,GETDATE())),
    (@u1,@fRice,   'dinner',   CAST(DATEADD(day,-20,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-20,GETDATE())),
    (@u1,@fBeef,   'dinner',   CAST(DATEADD(day,-20,GETDATE())AS DATE),120,'g',260,31.2, 0.0,14.4,DATEADD(day,-20,GETDATE())),
    (@u1,@fVeg2,   'dinner',   CAST(DATEADD(day,-20,GETDATE())AS DATE),100,'g', 25, 1.3, 5.8, 0.1,DATEADD(day,-20,GETDATE())),
    (@u1,@fApple,  'snack',    CAST(DATEADD(day,-20,GETDATE())AS DATE),150,'g', 78, 0.5,21.0, 0.3,DATEADD(day,-20,GETDATE()));
  -- ── Ngày 19 ──
  INSERT INTO meal_logs (user_id,food_id,meal_type,log_date,quantity,unit,calories,protein,carbs,fat,created_at) VALUES
    (@u1,@fEggBoil,'breakfast',CAST(DATEADD(day,-19,GETDATE())AS DATE),120,'g',186,15.6,13.0,13.2,DATEADD(day,-19,GETDATE())),
    (@u1,@fBread,  'breakfast',CAST(DATEADD(day,-19,GETDATE())AS DATE),100,'g',280, 9.0,32.0,13.0,DATEADD(day,-19,GETDATE())),
    (@u1,@fBrownRice,'lunch',  CAST(DATEADD(day,-19,GETDATE())AS DATE),200,'g',222, 5.2,46.0, 1.8,DATEADD(day,-19,GETDATE())),
    (@u1,@fChickenG,'lunch',   CAST(DATEADD(day,-19,GETDATE())AS DATE),150,'g',239,48.0, 0.0, 4.8,DATEADD(day,-19,GETDATE())),
    (@u1,@fSalad,  'lunch',    CAST(DATEADD(day,-19,GETDATE())AS DATE),150,'g', 30, 2.3, 5.7, 0.3,DATEADD(day,-19,GETDATE())),
    (@u1,@fTofu,   'dinner',   CAST(DATEADD(day,-19,GETDATE())AS DATE),200,'g',160,16.0, 6.0, 8.0,DATEADD(day,-19,GETDATE())),
    (@u1,@fRice,   'dinner',   CAST(DATEADD(day,-19,GETDATE())AS DATE),150,'g',195, 4.1,42.3, 0.5,DATEADD(day,-19,GETDATE())),
    (@u1,@fVeg3,   'dinner',   CAST(DATEADD(day,-19,GETDATE())AS DATE),100,'g', 32, 3.0, 5.6, 0.4,DATEADD(day,-19,GETDATE())),
    (@u1,@fYogurt, 'snack',    CAST(DATEADD(day,-19,GETDATE())AS DATE),150,'g', 89,15.0, 5.4, 0.6,DATEADD(day,-19,GETDATE()));
  -- ── Ngày 18 ──
  INSERT INTO meal_logs (user_id,food_id,meal_type,log_date,quantity,unit,calories,protein,carbs,fat,created_at) VALUES
    (@u1,@fOat,    'breakfast',CAST(DATEADD(day,-18,GETDATE())AS DATE),80, 'g',311,13.6,52.8, 5.6,DATEADD(day,-18,GETDATE())),
    (@u1,@fBanana, 'breakfast',CAST(DATEADD(day,-18,GETDATE())AS DATE),100,'g', 89, 1.1,23.0, 0.3,DATEADD(day,-18,GETDATE())),
    (@u1,@fRice,   'lunch',    CAST(DATEADD(day,-18,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-18,GETDATE())),
    (@u1,@fPork,   'lunch',    CAST(DATEADD(day,-18,GETDATE())AS DATE),150,'g',215,39.0, 0.0, 6.0,DATEADD(day,-18,GETDATE())),
    (@u1,@fVeg1,   'lunch',    CAST(DATEADD(day,-18,GETDATE())AS DATE),100,'g', 21, 2.6, 3.1, 0.3,DATEADD(day,-18,GETDATE())),
    (@u1,@fBrownRice,'dinner', CAST(DATEADD(day,-18,GETDATE())AS DATE),150,'g',167, 3.9,34.5, 1.4,DATEADD(day,-18,GETDATE())),
    (@u1,@fShrimp, 'dinner',   CAST(DATEADD(day,-18,GETDATE())AS DATE),150,'g',149,36.0, 0.0, 0.5,DATEADD(day,-18,GETDATE())),
    (@u1,@fVeg2,   'dinner',   CAST(DATEADD(day,-18,GETDATE())AS DATE),100,'g', 25, 1.3, 5.8, 0.1,DATEADD(day,-18,GETDATE())),
    (@u1,@fApple,  'snack',    CAST(DATEADD(day,-18,GETDATE())AS DATE),150,'g', 78, 0.5,21.0, 0.3,DATEADD(day,-18,GETDATE()));
  -- ── Ngày 17 ──
  INSERT INTO meal_logs (user_id,food_id,meal_type,log_date,quantity,unit,calories,protein,carbs,fat,created_at) VALUES
    (@u1,@fBread,  'breakfast',CAST(DATEADD(day,-17,GETDATE())AS DATE),150,'g',420,13.5,48.0,19.5,DATEADD(day,-17,GETDATE())),
    (@u1,@fEggFry, 'breakfast',CAST(DATEADD(day,-17,GETDATE())AS DATE),100,'g',196,13.6, 0.6,15.3,DATEADD(day,-17,GETDATE())),
    (@u1,@fRice,   'lunch',    CAST(DATEADD(day,-17,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-17,GETDATE())),
    (@u1,@fChicken,'lunch',    CAST(DATEADD(day,-17,GETDATE())AS DATE),150,'g',248,46.5, 0.0, 5.4,DATEADD(day,-17,GETDATE())),
    (@u1,@fSalad,  'lunch',    CAST(DATEADD(day,-17,GETDATE())AS DATE),150,'g', 30, 2.3, 5.7, 0.3,DATEADD(day,-17,GETDATE())),
    (@u1,@fSalmon, 'dinner',   CAST(DATEADD(day,-17,GETDATE())AS DATE),150,'g',312,30.0, 0.0,19.5,DATEADD(day,-17,GETDATE())),
    (@u1,@fSweetPot,'dinner',  CAST(DATEADD(day,-17,GETDATE())AS DATE),150,'g',129, 2.4,30.0, 0.2,DATEADD(day,-17,GETDATE())),
    (@u1,@fVeg3,   'dinner',   CAST(DATEADD(day,-17,GETDATE())AS DATE),100,'g', 32, 3.0, 5.6, 0.4,DATEADD(day,-17,GETDATE())),
    (@u1,@fOrange, 'snack',    CAST(DATEADD(day,-17,GETDATE())AS DATE),180,'g', 85, 1.6,21.6, 0.2,DATEADD(day,-17,GETDATE()));
  -- ── Ngày 14 ──
  INSERT INTO meal_logs (user_id,food_id,meal_type,log_date,quantity,unit,calories,protein,carbs,fat,created_at) VALUES
    (@u1,@fOat,    'breakfast',CAST(DATEADD(day,-14,GETDATE())AS DATE),80, 'g',311,13.6,52.8, 5.6,DATEADD(day,-14,GETDATE())),
    (@u1,@fMilk,   'breakfast',CAST(DATEADD(day,-14,GETDATE())AS DATE),200,'ml', 84, 6.8,10.0, 2.0,DATEADD(day,-14,GETDATE())),
    (@u1,@fPho,    'lunch',    CAST(DATEADD(day,-14,GETDATE())AS DATE),400,'g',480,30.0,68.0,10.0,DATEADD(day,-14,GETDATE())),
    (@u1,@fRice,   'dinner',   CAST(DATEADD(day,-14,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-14,GETDATE())),
    (@u1,@fBeef,   'dinner',   CAST(DATEADD(day,-14,GETDATE())AS DATE),100,'g',217,26.0, 0.0,12.0,DATEADD(day,-14,GETDATE())),
    (@u1,@fVeg1,   'dinner',   CAST(DATEADD(day,-14,GETDATE())AS DATE),100,'g', 21, 2.6, 3.1, 0.3,DATEADD(day,-14,GETDATE())),
    (@u1,@fBanana, 'snack',    CAST(DATEADD(day,-14,GETDATE())AS DATE),100,'g', 89, 1.1,23.0, 0.3,DATEADD(day,-14,GETDATE()));
  -- ── Ngày 13-10 (dữ liệu đa dạng) ──
  INSERT INTO meal_logs (user_id,food_id,meal_type,log_date,quantity,unit,calories,protein,carbs,fat,created_at) VALUES
    (@u1,@fBread,  'breakfast',CAST(DATEADD(day,-13,GETDATE())AS DATE),150,'g',420,13.5,48.0,19.5,DATEADD(day,-13,GETDATE())),
    (@u1,@fEggBoil,'breakfast',CAST(DATEADD(day,-13,GETDATE())AS DATE),120,'g',186,15.6,13.0,13.2,DATEADD(day,-13,GETDATE())),
    (@u1,@fBrownRice,'lunch',  CAST(DATEADD(day,-13,GETDATE())AS DATE),200,'g',222, 5.2,46.0, 1.8,DATEADD(day,-13,GETDATE())),
    (@u1,@fChickenG,'lunch',   CAST(DATEADD(day,-13,GETDATE())AS DATE),150,'g',239,48.0, 0.0, 4.8,DATEADD(day,-13,GETDATE())),
    (@u1,@fVeg2,   'lunch',    CAST(DATEADD(day,-13,GETDATE())AS DATE),100,'g', 25, 1.3, 5.8, 0.1,DATEADD(day,-13,GETDATE())),
    (@u1,@fSalmon, 'dinner',   CAST(DATEADD(day,-13,GETDATE())AS DATE),120,'g',250,24.0, 0.0,15.6,DATEADD(day,-13,GETDATE())),
    (@u1,@fRice,   'dinner',   CAST(DATEADD(day,-13,GETDATE())AS DATE),150,'g',195, 4.1,42.3, 0.5,DATEADD(day,-13,GETDATE())),
    (@u1,@fSalad,  'dinner',   CAST(DATEADD(day,-13,GETDATE())AS DATE),150,'g', 30, 2.3, 5.7, 0.3,DATEADD(day,-13,GETDATE())),
    (@u1,@fApple,  'snack',    CAST(DATEADD(day,-13,GETDATE())AS DATE),150,'g', 78, 0.5,21.0, 0.3,DATEADD(day,-13,GETDATE())),
    -- Ngày -12
    (@u1,@fOat,    'breakfast',CAST(DATEADD(day,-12,GETDATE())AS DATE),80, 'g',311,13.6,52.8, 5.6,DATEADD(day,-12,GETDATE())),
    (@u1,@fBanana, 'breakfast',CAST(DATEADD(day,-12,GETDATE())AS DATE),100,'g', 89, 1.1,23.0, 0.3,DATEADD(day,-12,GETDATE())),
    (@u1,@fRice,   'lunch',    CAST(DATEADD(day,-12,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-12,GETDATE())),
    (@u1,@fTofu,   'lunch',    CAST(DATEADD(day,-12,GETDATE())AS DATE),200,'g',160,16.0, 6.0, 8.0,DATEADD(day,-12,GETDATE())),
    (@u1,@fVeg3,   'lunch',    CAST(DATEADD(day,-12,GETDATE())AS DATE),100,'g', 32, 3.0, 5.6, 0.4,DATEADD(day,-12,GETDATE())),
    (@u1,@fChicken,'dinner',   CAST(DATEADD(day,-12,GETDATE())AS DATE),150,'g',248,46.5, 0.0, 5.4,DATEADD(day,-12,GETDATE())),
    (@u1,@fSweetPot,'dinner',  CAST(DATEADD(day,-12,GETDATE())AS DATE),130,'g',112, 2.1,26.0, 0.1,DATEADD(day,-12,GETDATE())),
    (@u1,@fVeg1,   'dinner',   CAST(DATEADD(day,-12,GETDATE())AS DATE),100,'g', 21, 2.6, 3.1, 0.3,DATEADD(day,-12,GETDATE())),
    (@u1,@fYogurt, 'snack',    CAST(DATEADD(day,-12,GETDATE())AS DATE),150,'g', 89,15.0, 5.4, 0.6,DATEADD(day,-12,GETDATE())),
    -- Ngày -11
    (@u1,@fBread,  'breakfast',CAST(DATEADD(day,-11,GETDATE())AS DATE),150,'g',420,13.5,48.0,19.5,DATEADD(day,-11,GETDATE())),
    (@u1,@fEggFry, 'breakfast',CAST(DATEADD(day,-11,GETDATE())AS DATE),100,'g',196,13.6, 0.6,15.3,DATEADD(day,-11,GETDATE())),
    (@u1,@fPho,    'lunch',    CAST(DATEADD(day,-11,GETDATE())AS DATE),400,'g',480,30.0,68.0,10.0,DATEADD(day,-11,GETDATE())),
    (@u1,@fBeef,   'dinner',   CAST(DATEADD(day,-11,GETDATE())AS DATE),120,'g',260,31.2, 0.0,14.4,DATEADD(day,-11,GETDATE())),
    (@u1,@fRice,   'dinner',   CAST(DATEADD(day,-11,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-11,GETDATE())),
    (@u1,@fVeg2,   'dinner',   CAST(DATEADD(day,-11,GETDATE())AS DATE),100,'g', 25, 1.3, 5.8, 0.1,DATEADD(day,-11,GETDATE())),
    (@u1,@fOrange, 'snack',    CAST(DATEADD(day,-11,GETDATE())AS DATE),180,'g', 85, 1.6,21.6, 0.2,DATEADD(day,-11,GETDATE()));
  -- ── Ngày 7-4 ──
  INSERT INTO meal_logs (user_id,food_id,meal_type,log_date,quantity,unit,calories,protein,carbs,fat,created_at) VALUES
    (@u1,@fOat,    'breakfast',CAST(DATEADD(day,-7,GETDATE())AS DATE),80,'g',311,13.6,52.8, 5.6,DATEADD(day,-7,GETDATE())),
    (@u1,@fMilk,   'breakfast',CAST(DATEADD(day,-7,GETDATE())AS DATE),200,'ml',84, 6.8,10.0, 2.0,DATEADD(day,-7,GETDATE())),
    (@u1,@fBrownRice,'lunch',  CAST(DATEADD(day,-7,GETDATE())AS DATE),200,'g',222, 5.2,46.0, 1.8,DATEADD(day,-7,GETDATE())),
    (@u1,@fChicken,'lunch',    CAST(DATEADD(day,-7,GETDATE())AS DATE),150,'g',248,46.5, 0.0, 5.4,DATEADD(day,-7,GETDATE())),
    (@u1,@fVeg3,   'lunch',    CAST(DATEADD(day,-7,GETDATE())AS DATE),100,'g', 32, 3.0, 5.6, 0.4,DATEADD(day,-7,GETDATE())),
    (@u1,@fSalmon, 'dinner',   CAST(DATEADD(day,-7,GETDATE())AS DATE),150,'g',312,30.0, 0.0,19.5,DATEADD(day,-7,GETDATE())),
    (@u1,@fRice,   'dinner',   CAST(DATEADD(day,-7,GETDATE())AS DATE),150,'g',195, 4.1,42.3, 0.5,DATEADD(day,-7,GETDATE())),
    (@u1,@fBanana, 'snack',    CAST(DATEADD(day,-7,GETDATE())AS DATE),120,'g',107, 1.3,27.6, 0.4,DATEADD(day,-7,GETDATE())),
    -- Ngày -6
    (@u1,@fBread,  'breakfast',CAST(DATEADD(day,-6,GETDATE())AS DATE),150,'g',420,13.5,48.0,19.5,DATEADD(day,-6,GETDATE())),
    (@u1,@fEggBoil,'breakfast',CAST(DATEADD(day,-6,GETDATE())AS DATE),120,'g',186,15.6,13.0,13.2,DATEADD(day,-6,GETDATE())),
    (@u1,@fRice,   'lunch',    CAST(DATEADD(day,-6,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-6,GETDATE())),
    (@u1,@fPork,   'lunch',    CAST(DATEADD(day,-6,GETDATE())AS DATE),150,'g',215,39.0, 0.0, 6.0,DATEADD(day,-6,GETDATE())),
    (@u1,@fVeg1,   'lunch',    CAST(DATEADD(day,-6,GETDATE())AS DATE),100,'g', 21, 2.6, 3.1, 0.3,DATEADD(day,-6,GETDATE())),
    (@u1,@fTofu,   'dinner',   CAST(DATEADD(day,-6,GETDATE())AS DATE),200,'g',160,16.0, 6.0, 8.0,DATEADD(day,-6,GETDATE())),
    (@u1,@fSweetPot,'dinner',  CAST(DATEADD(day,-6,GETDATE())AS DATE),150,'g',129, 2.4,30.0, 0.2,DATEADD(day,-6,GETDATE())),
    (@u1,@fSalad,  'dinner',   CAST(DATEADD(day,-6,GETDATE())AS DATE),150,'g', 30, 2.3, 5.7, 0.3,DATEADD(day,-6,GETDATE())),
    (@u1,@fApple,  'snack',    CAST(DATEADD(day,-6,GETDATE())AS DATE),150,'g', 78, 0.5,21.0, 0.3,DATEADD(day,-6,GETDATE())),
    -- Ngày -5
    (@u1,@fOat,    'breakfast',CAST(DATEADD(day,-5,GETDATE())AS DATE),80,'g',311,13.6,52.8, 5.6,DATEADD(day,-5,GETDATE())),
    (@u1,@fBanana, 'breakfast',CAST(DATEADD(day,-5,GETDATE())AS DATE),100,'g', 89, 1.1,23.0, 0.3,DATEADD(day,-5,GETDATE())),
    (@u1,@fPho,    'lunch',    CAST(DATEADD(day,-5,GETDATE())AS DATE),400,'g',480,30.0,68.0,10.0,DATEADD(day,-5,GETDATE())),
    (@u1,@fChickenG,'dinner',  CAST(DATEADD(day,-5,GETDATE())AS DATE),150,'g',239,48.0, 0.0, 4.8,DATEADD(day,-5,GETDATE())),
    (@u1,@fBrownRice,'dinner', CAST(DATEADD(day,-5,GETDATE())AS DATE),200,'g',222, 5.2,46.0, 1.8,DATEADD(day,-5,GETDATE())),
    (@u1,@fVeg2,   'dinner',   CAST(DATEADD(day,-5,GETDATE())AS DATE),100,'g', 25, 1.3, 5.8, 0.1,DATEADD(day,-5,GETDATE())),
    (@u1,@fYogurt, 'snack',    CAST(DATEADD(day,-5,GETDATE())AS DATE),150,'g', 89,15.0, 5.4, 0.6,DATEADD(day,-5,GETDATE())),
    -- Ngày -4
    (@u1,@fBread,  'breakfast',CAST(DATEADD(day,-4,GETDATE())AS DATE),150,'g',420,13.5,48.0,19.5,DATEADD(day,-4,GETDATE())),
    (@u1,@fEggFry, 'breakfast',CAST(DATEADD(day,-4,GETDATE())AS DATE),100,'g',196,13.6, 0.6,15.3,DATEADD(day,-4,GETDATE())),
    (@u1,@fRice,   'lunch',    CAST(DATEADD(day,-4,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-4,GETDATE())),
    (@u1,@fBeef,   'lunch',    CAST(DATEADD(day,-4,GETDATE())AS DATE),120,'g',260,31.2, 0.0,14.4,DATEADD(day,-4,GETDATE())),
    (@u1,@fVeg3,   'lunch',    CAST(DATEADD(day,-4,GETDATE())AS DATE),100,'g', 32, 3.0, 5.6, 0.4,DATEADD(day,-4,GETDATE())),
    (@u1,@fShrimp, 'dinner',   CAST(DATEADD(day,-4,GETDATE())AS DATE),150,'g',149,36.0, 0.0, 0.5,DATEADD(day,-4,GETDATE())),
    (@u1,@fRice,   'dinner',   CAST(DATEADD(day,-4,GETDATE())AS DATE),150,'g',195, 4.1,42.3, 0.5,DATEADD(day,-4,GETDATE())),
    (@u1,@fVeg1,   'dinner',   CAST(DATEADD(day,-4,GETDATE())AS DATE),100,'g', 21, 2.6, 3.1, 0.3,DATEADD(day,-4,GETDATE())),
    (@u1,@fOrange, 'snack',    CAST(DATEADD(day,-4,GETDATE())AS DATE),180,'g', 85, 1.6,21.6, 0.2,DATEADD(day,-4,GETDATE())),
    -- Ngày -3
    (@u1,@fOat,    'breakfast',CAST(DATEADD(day,-3,GETDATE())AS DATE),80,'g',311,13.6,52.8, 5.6,DATEADD(day,-3,GETDATE())),
    (@u1,@fMilk,   'breakfast',CAST(DATEADD(day,-3,GETDATE())AS DATE),200,'ml',84, 6.8,10.0, 2.0,DATEADD(day,-3,GETDATE())),
    (@u1,@fBrownRice,'lunch',  CAST(DATEADD(day,-3,GETDATE())AS DATE),200,'g',222, 5.2,46.0, 1.8,DATEADD(day,-3,GETDATE())),
    (@u1,@fChicken,'lunch',    CAST(DATEADD(day,-3,GETDATE())AS DATE),150,'g',248,46.5, 0.0, 5.4,DATEADD(day,-3,GETDATE())),
    (@u1,@fVeg2,   'lunch',    CAST(DATEADD(day,-3,GETDATE())AS DATE),100,'g', 25, 1.3, 5.8, 0.1,DATEADD(day,-3,GETDATE())),
    (@u1,@fSalmon, 'dinner',   CAST(DATEADD(day,-3,GETDATE())AS DATE),150,'g',312,30.0, 0.0,19.5,DATEADD(day,-3,GETDATE())),
    (@u1,@fRice,   'dinner',   CAST(DATEADD(day,-3,GETDATE())AS DATE),150,'g',195, 4.1,42.3, 0.5,DATEADD(day,-3,GETDATE())),
    (@u1,@fBanana, 'snack',    CAST(DATEADD(day,-3,GETDATE())AS DATE),120,'g',107, 1.3,27.6, 0.4,DATEADD(day,-3,GETDATE())),
    -- Ngày -2
    (@u1,@fBread,  'breakfast',CAST(DATEADD(day,-2,GETDATE())AS DATE),150,'g',420,13.5,48.0,19.5,DATEADD(day,-2,GETDATE())),
    (@u1,@fEggBoil,'breakfast',CAST(DATEADD(day,-2,GETDATE())AS DATE),120,'g',186,15.6,13.0,13.2,DATEADD(day,-2,GETDATE())),
    (@u1,@fRice,   'lunch',    CAST(DATEADD(day,-2,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-2,GETDATE())),
    (@u1,@fPork,   'lunch',    CAST(DATEADD(day,-2,GETDATE())AS DATE),150,'g',215,39.0, 0.0, 6.0,DATEADD(day,-2,GETDATE())),
    (@u1,@fVeg1,   'lunch',    CAST(DATEADD(day,-2,GETDATE())AS DATE),100,'g', 21, 2.6, 3.1, 0.3,DATEADD(day,-2,GETDATE())),
    (@u1,@fTofu,   'dinner',   CAST(DATEADD(day,-2,GETDATE())AS DATE),200,'g',160,16.0, 6.0, 8.0,DATEADD(day,-2,GETDATE())),
    (@u1,@fSweetPot,'dinner',  CAST(DATEADD(day,-2,GETDATE())AS DATE),150,'g',129, 2.4,30.0, 0.2,DATEADD(day,-2,GETDATE())),
    (@u1,@fVeg3,   'dinner',   CAST(DATEADD(day,-2,GETDATE())AS DATE),100,'g', 32, 3.0, 5.6, 0.4,DATEADD(day,-2,GETDATE())),
    (@u1,@fApple,  'snack',    CAST(DATEADD(day,-2,GETDATE())AS DATE),150,'g', 78, 0.5,21.0, 0.3,DATEADD(day,-2,GETDATE())),
    -- Ngày -1
    (@u1,@fOat,    'breakfast',CAST(DATEADD(day,-1,GETDATE())AS DATE),80,'g',311,13.6,52.8, 5.6,DATEADD(day,-1,GETDATE())),
    (@u1,@fMilk,   'breakfast',CAST(DATEADD(day,-1,GETDATE())AS DATE),200,'ml',84, 6.8,10.0, 2.0,DATEADD(day,-1,GETDATE())),
    (@u1,@fRice,   'lunch',    CAST(DATEADD(day,-1,GETDATE())AS DATE),200,'g',260, 5.4,56.4, 0.6,DATEADD(day,-1,GETDATE())),
    (@u1,@fBeef,   'lunch',    CAST(DATEADD(day,-1,GETDATE())AS DATE),120,'g',260,31.2, 0.0,14.4,DATEADD(day,-1,GETDATE())),
    (@u1,@fVeg2,   'lunch',    CAST(DATEADD(day,-1,GETDATE())AS DATE),100,'g', 25, 1.3, 5.8, 0.1,DATEADD(day,-1,GETDATE())),
    (@u1,@fChickenG,'dinner',  CAST(DATEADD(day,-1,GETDATE())AS DATE),150,'g',239,48.0, 0.0, 4.8,DATEADD(day,-1,GETDATE())),
    (@u1,@fBrownRice,'dinner', CAST(DATEADD(day,-1,GETDATE())AS DATE),200,'g',222, 5.2,46.0, 1.8,DATEADD(day,-1,GETDATE())),
    (@u1,@fVeg3,   'dinner',   CAST(DATEADD(day,-1,GETDATE())AS DATE),100,'g', 32, 3.0, 5.6, 0.4,DATEADD(day,-1,GETDATE())),
    (@u1,@fYogurt, 'snack',    CAST(DATEADD(day,-1,GETDATE())AS DATE),150,'g', 89,15.0, 5.4, 0.6,DATEADD(day,-1,GETDATE())),
    -- Hôm nay
    (@u1,@fBread,  'breakfast',CAST(GETDATE()AS DATE),150,'g',420,13.5,48.0,19.5,GETDATE()),
    (@u1,@fEggFry, 'breakfast',CAST(GETDATE()AS DATE),100,'g',196,13.6, 0.6,15.3,GETDATE()),
    (@u1,@fRice,   'lunch',    CAST(GETDATE()AS DATE),200,'g',260, 5.4,56.4, 0.6,GETDATE()),
    (@u1,@fChicken,'lunch',    CAST(GETDATE()AS DATE),150,'g',248,46.5, 0.0, 5.4,GETDATE()),
    (@u1,@fVeg1,   'lunch',    CAST(GETDATE()AS DATE),100,'g', 21, 2.6, 3.1, 0.3,GETDATE()),
    (@u1,@fSalmon, 'dinner',   CAST(GETDATE()AS DATE),150,'g',312,30.0, 0.0,19.5,GETDATE()),
    (@u1,@fRice,   'dinner',   CAST(GETDATE()AS DATE),150,'g',195, 4.1,42.3, 0.5,GETDATE()),
    (@u1,@fBanana, 'snack',    CAST(GETDATE()AS DATE),120,'g',107, 1.3,27.6, 0.4,GETDATE());
END
GO

-- ══════════════════════════════════════════════════════════════
-- 7. MEAL LOGS – user2 (14 ngày)
-- ══════════════════════════════════════════════════════════════
DECLARE @u2b BIGINT = (SELECT id FROM users WHERE email='user2@vitatrack.vn');
IF NOT EXISTS (SELECT 1 FROM meal_logs WHERE user_id=@u2b)
BEGIN
  DECLARE @fOat2    BIGINT=(SELECT id FROM foods WHERE name=N'Yến mạch');
  DECLARE @fYog2    BIGINT=(SELECT id FROM foods WHERE name=N'Yogurt không đường');
  DECLARE @fPho2    BIGINT=(SELECT id FROM foods WHERE name=N'Phở bò');
  DECLARE @fChick2  BIGINT=(SELECT id FROM foods WHERE name=N'Ức gà luộc');
  DECLARE @fVeg12   BIGINT=(SELECT id FROM foods WHERE name=N'Rau muống xào');
  DECLARE @fVeg22   BIGINT=(SELECT id FROM foods WHERE name=N'Bắp cải xào');
  DECLARE @fVeg32   BIGINT=(SELECT id FROM foods WHERE name=N'Cải xanh luộc');
  DECLARE @fBan2    BIGINT=(SELECT id FROM foods WHERE name=N'Chuối');
  DECLARE @fApp2    BIGINT=(SELECT id FROM foods WHERE name=N'Táo');
  DECLARE @fEgg2    BIGINT=(SELECT id FROM foods WHERE name=N'Trứng luộc');
  DECLARE @fTofu2   BIGINT=(SELECT id FROM foods WHERE name=N'Đậu phụ sốt cà');
  DECLARE @fBR2     BIGINT=(SELECT id FROM foods WHERE name=N'Cơm gạo lứt');
  DECLARE @fSwP2    BIGINT=(SELECT id FROM foods WHERE name=N'Khoai lang nướng');
  DECLARE @fSal2    BIGINT=(SELECT id FROM foods WHERE name=N'Salad rau trộn');

  -- 14 ngày dữ liệu cho user2 (tránh Lactose và Gluten)
  INSERT INTO meal_logs (user_id,food_id,meal_type,log_date,quantity,unit,calories,protein,carbs,fat,created_at) VALUES
    -- Ngày -14 đến -8
    (@u2b,@fOat2,  'breakfast',CAST(DATEADD(day,-14,GETDATE())AS DATE),80,'g',311,13.6,52.8,5.6,DATEADD(day,-14,GETDATE())),
    (@u2b,@fBan2,  'breakfast',CAST(DATEADD(day,-14,GETDATE())AS DATE),100,'g',89,1.1,23.0,0.3,DATEADD(day,-14,GETDATE())),
    (@u2b,@fPho2,  'lunch',    CAST(DATEADD(day,-14,GETDATE())AS DATE),350,'g',420,26.3,59.5,8.8,DATEADD(day,-14,GETDATE())),
    (@u2b,@fVeg12, 'dinner',   CAST(DATEADD(day,-14,GETDATE())AS DATE),150,'g',32,3.9,4.7,0.5,DATEADD(day,-14,GETDATE())),
    (@u2b,@fChick2,'dinner',   CAST(DATEADD(day,-14,GETDATE())AS DATE),100,'g',165,31.0,0.0,3.6,DATEADD(day,-14,GETDATE())),
    (@u2b,@fBR2,   'dinner',   CAST(DATEADD(day,-14,GETDATE())AS DATE),150,'g',167,3.9,34.5,1.4,DATEADD(day,-14,GETDATE())),
    (@u2b,@fApp2,  'snack',    CAST(DATEADD(day,-14,GETDATE())AS DATE),150,'g',78,0.5,21.0,0.3,DATEADD(day,-14,GETDATE())),
    -- Ngày -13
    (@u2b,@fEgg2,  'breakfast',CAST(DATEADD(day,-13,GETDATE())AS DATE),120,'g',186,15.6,13.2,13.2,DATEADD(day,-13,GETDATE())),
    (@u2b,@fBR2,   'lunch',    CAST(DATEADD(day,-13,GETDATE())AS DATE),200,'g',222,5.2,46.0,1.8,DATEADD(day,-13,GETDATE())),
    (@u2b,@fChick2,'lunch',    CAST(DATEADD(day,-13,GETDATE())AS DATE),120,'g',198,37.2,0.0,4.3,DATEADD(day,-13,GETDATE())),
    (@u2b,@fVeg22, 'lunch',    CAST(DATEADD(day,-13,GETDATE())AS DATE),100,'g',25,1.3,5.8,0.1,DATEADD(day,-13,GETDATE())),
    (@u2b,@fTofu2, 'dinner',   CAST(DATEADD(day,-13,GETDATE())AS DATE),200,'g',160,16.0,6.0,8.0,DATEADD(day,-13,GETDATE())),
    (@u2b,@fSwP2,  'dinner',   CAST(DATEADD(day,-13,GETDATE())AS DATE),130,'g',112,2.1,26.0,0.1,DATEADD(day,-13,GETDATE())),
    (@u2b,@fVeg32, 'dinner',   CAST(DATEADD(day,-13,GETDATE())AS DATE),100,'g',32,3.0,5.6,0.4,DATEADD(day,-13,GETDATE())),
    (@u2b,@fBan2,  'snack',    CAST(DATEADD(day,-13,GETDATE())AS DATE),100,'g',89,1.1,23.0,0.3,DATEADD(day,-13,GETDATE())),
    -- Ngày -12 đến -8 (compact)
    (@u2b,@fOat2,  'breakfast',CAST(DATEADD(day,-12,GETDATE())AS DATE),80,'g',311,13.6,52.8,5.6,DATEADD(day,-12,GETDATE())),
    (@u2b,@fPho2,  'lunch',    CAST(DATEADD(day,-12,GETDATE())AS DATE),350,'g',420,26.3,59.5,8.8,DATEADD(day,-12,GETDATE())),
    (@u2b,@fVeg12, 'dinner',   CAST(DATEADD(day,-12,GETDATE())AS DATE),150,'g',32,3.9,4.7,0.5,DATEADD(day,-12,GETDATE())),
    (@u2b,@fChick2,'dinner',   CAST(DATEADD(day,-12,GETDATE())AS DATE),100,'g',165,31.0,0.0,3.6,DATEADD(day,-12,GETDATE())),
    (@u2b,@fApp2,  'snack',    CAST(DATEADD(day,-12,GETDATE())AS DATE),150,'g',78,0.5,21.0,0.3,DATEADD(day,-12,GETDATE())),
    -- Ngày -7
    (@u2b,@fEgg2,  'breakfast',CAST(DATEADD(day,-7,GETDATE())AS DATE),120,'g',186,15.6,13.2,13.2,DATEADD(day,-7,GETDATE())),
    (@u2b,@fBR2,   'lunch',    CAST(DATEADD(day,-7,GETDATE())AS DATE),200,'g',222,5.2,46.0,1.8,DATEADD(day,-7,GETDATE())),
    (@u2b,@fChick2,'lunch',    CAST(DATEADD(day,-7,GETDATE())AS DATE),120,'g',198,37.2,0.0,4.3,DATEADD(day,-7,GETDATE())),
    (@u2b,@fVeg22, 'lunch',    CAST(DATEADD(day,-7,GETDATE())AS DATE),100,'g',25,1.3,5.8,0.1,DATEADD(day,-7,GETDATE())),
    (@u2b,@fTofu2, 'dinner',   CAST(DATEADD(day,-7,GETDATE())AS DATE),200,'g',160,16.0,6.0,8.0,DATEADD(day,-7,GETDATE())),
    (@u2b,@fSal2,  'dinner',   CAST(DATEADD(day,-7,GETDATE())AS DATE),150,'g',30,2.3,5.7,0.3,DATEADD(day,-7,GETDATE())),
    (@u2b,@fBan2,  'snack',    CAST(DATEADD(day,-7,GETDATE())AS DATE),100,'g',89,1.1,23.0,0.3,DATEADD(day,-7,GETDATE())),
    -- Ngày -6 đến -1
    (@u2b,@fOat2,  'breakfast',CAST(DATEADD(day,-6,GETDATE())AS DATE),80,'g',311,13.6,52.8,5.6,DATEADD(day,-6,GETDATE())),
    (@u2b,@fPho2,  'lunch',    CAST(DATEADD(day,-6,GETDATE())AS DATE),350,'g',420,26.3,59.5,8.8,DATEADD(day,-6,GETDATE())),
    (@u2b,@fChick2,'dinner',   CAST(DATEADD(day,-6,GETDATE())AS DATE),100,'g',165,31.0,0.0,3.6,DATEADD(day,-6,GETDATE())),
    (@u2b,@fVeg32, 'dinner',   CAST(DATEADD(day,-6,GETDATE())AS DATE),100,'g',32,3.0,5.6,0.4,DATEADD(day,-6,GETDATE())),
    (@u2b,@fApp2,  'snack',    CAST(DATEADD(day,-6,GETDATE())AS DATE),150,'g',78,0.5,21.0,0.3,DATEADD(day,-6,GETDATE())),
    (@u2b,@fEgg2,  'breakfast',CAST(DATEADD(day,-5,GETDATE())AS DATE),120,'g',186,15.6,13.2,13.2,DATEADD(day,-5,GETDATE())),
    (@u2b,@fBR2,   'lunch',    CAST(DATEADD(day,-5,GETDATE())AS DATE),200,'g',222,5.2,46.0,1.8,DATEADD(day,-5,GETDATE())),
    (@u2b,@fTofu2, 'lunch',    CAST(DATEADD(day,-5,GETDATE())AS DATE),200,'g',160,16.0,6.0,8.0,DATEADD(day,-5,GETDATE())),
    (@u2b,@fVeg12, 'dinner',   CAST(DATEADD(day,-5,GETDATE())AS DATE),150,'g',32,3.9,4.7,0.5,DATEADD(day,-5,GETDATE())),
    (@u2b,@fChick2,'dinner',   CAST(DATEADD(day,-5,GETDATE())AS DATE),100,'g',165,31.0,0.0,3.6,DATEADD(day,-5,GETDATE())),
    (@u2b,@fBan2,  'snack',    CAST(DATEADD(day,-5,GETDATE())AS DATE),100,'g',89,1.1,23.0,0.3,DATEADD(day,-5,GETDATE())),
    (@u2b,@fOat2,  'breakfast',CAST(DATEADD(day,-4,GETDATE())AS DATE),80,'g',311,13.6,52.8,5.6,DATEADD(day,-4,GETDATE())),
    (@u2b,@fPho2,  'lunch',    CAST(DATEADD(day,-4,GETDATE())AS DATE),350,'g',420,26.3,59.5,8.8,DATEADD(day,-4,GETDATE())),
    (@u2b,@fVeg22, 'dinner',   CAST(DATEADD(day,-4,GETDATE())AS DATE),100,'g',25,1.3,5.8,0.1,DATEADD(day,-4,GETDATE())),
    (@u2b,@fChick2,'dinner',   CAST(DATEADD(day,-4,GETDATE())AS DATE),100,'g',165,31.0,0.0,3.6,DATEADD(day,-4,GETDATE())),
    (@u2b,@fApp2,  'snack',    CAST(DATEADD(day,-4,GETDATE())AS DATE),150,'g',78,0.5,21.0,0.3,DATEADD(day,-4,GETDATE())),
    -- Ngày -3
    (@u2b,@fEgg2,  'breakfast',CAST(DATEADD(day,-3,GETDATE())AS DATE),120,'g',186,15.6,13.2,13.2,DATEADD(day,-3,GETDATE())),
    (@u2b,@fBR2,   'lunch',    CAST(DATEADD(day,-3,GETDATE())AS DATE),200,'g',222,5.2,46.0,1.8,DATEADD(day,-3,GETDATE())),
    (@u2b,@fChick2,'lunch',    CAST(DATEADD(day,-3,GETDATE())AS DATE),120,'g',198,37.2,0.0,4.3,DATEADD(day,-3,GETDATE())),
    (@u2b,@fSal2,  'dinner',   CAST(DATEADD(day,-3,GETDATE())AS DATE),200,'g',40,3.0,7.6,0.4,DATEADD(day,-3,GETDATE())),
    (@u2b,@fTofu2, 'dinner',   CAST(DATEADD(day,-3,GETDATE())AS DATE),200,'g',160,16.0,6.0,8.0,DATEADD(day,-3,GETDATE())),
    (@u2b,@fBan2,  'snack',    CAST(DATEADD(day,-3,GETDATE())AS DATE),100,'g',89,1.1,23.0,0.3,DATEADD(day,-3,GETDATE())),
    -- Ngày -2
    (@u2b,@fOat2,  'breakfast',CAST(DATEADD(day,-2,GETDATE())AS DATE),80,'g',311,13.6,52.8,5.6,DATEADD(day,-2,GETDATE())),
    (@u2b,@fPho2,  'lunch',    CAST(DATEADD(day,-2,GETDATE())AS DATE),350,'g',420,26.3,59.5,8.8,DATEADD(day,-2,GETDATE())),
    (@u2b,@fVeg12, 'dinner',   CAST(DATEADD(day,-2,GETDATE())AS DATE),150,'g',32,3.9,4.7,0.5,DATEADD(day,-2,GETDATE())),
    (@u2b,@fChick2,'dinner',   CAST(DATEADD(day,-2,GETDATE())AS DATE),100,'g',165,31.0,0.0,3.6,DATEADD(day,-2,GETDATE())),
    (@u2b,@fApp2,  'snack',    CAST(DATEADD(day,-2,GETDATE())AS DATE),150,'g',78,0.5,21.0,0.3,DATEADD(day,-2,GETDATE())),
    -- Ngày -1
    (@u2b,@fEgg2,  'breakfast',CAST(DATEADD(day,-1,GETDATE())AS DATE),120,'g',186,15.6,13.2,13.2,DATEADD(day,-1,GETDATE())),
    (@u2b,@fBR2,   'lunch',    CAST(DATEADD(day,-1,GETDATE())AS DATE),200,'g',222,5.2,46.0,1.8,DATEADD(day,-1,GETDATE())),
    (@u2b,@fChick2,'lunch',    CAST(DATEADD(day,-1,GETDATE())AS DATE),120,'g',198,37.2,0.0,4.3,DATEADD(day,-1,GETDATE())),
    (@u2b,@fVeg32, 'dinner',   CAST(DATEADD(day,-1,GETDATE())AS DATE),100,'g',32,3.0,5.6,0.4,DATEADD(day,-1,GETDATE())),
    (@u2b,@fVeg22, 'dinner',   CAST(DATEADD(day,-1,GETDATE())AS DATE),100,'g',25,1.3,5.8,0.1,DATEADD(day,-1,GETDATE())),
    (@u2b,@fBan2,  'snack',    CAST(DATEADD(day,-1,GETDATE())AS DATE),100,'g',89,1.1,23.0,0.3,DATEADD(day,-1,GETDATE())),
    -- Hôm nay
    (@u2b,@fOat2,  'breakfast',CAST(GETDATE()AS DATE),80,'g',311,13.6,52.8,5.6,GETDATE()),
    (@u2b,@fPho2,  'lunch',    CAST(GETDATE()AS DATE),350,'g',420,26.3,59.5,8.8,GETDATE()),
    (@u2b,@fVeg12, 'dinner',   CAST(GETDATE()AS DATE),150,'g',32,3.9,4.7,0.5,GETDATE()),
    (@u2b,@fChick2,'dinner',   CAST(GETDATE()AS DATE),100,'g',165,31.0,0.0,3.6,GETDATE()),
    (@u2b,@fBan2,  'snack',    CAST(GETDATE()AS DATE),100,'g',89,1.1,23.0,0.3,GETDATE());
END
GO

-- ══════════════════════════════════════════════════════════════
-- 8. MEAL LOGS – user3 (10 ngày, nhiều protein – tăng cơ)
-- ══════════════════════════════════════════════════════════════
DECLARE @u3b BIGINT = (SELECT id FROM users WHERE email='user3@vitatrack.vn');
IF NOT EXISTS (SELECT 1 FROM meal_logs WHERE user_id=@u3b)
BEGIN
  DECLARE @fRice3   BIGINT=(SELECT id FROM foods WHERE name=N'Cơm trắng');
  DECLARE @fOat3    BIGINT=(SELECT id FROM foods WHERE name=N'Yến mạch');
  DECLARE @fChick3  BIGINT=(SELECT id FROM foods WHERE name=N'Ức gà nướng');
  DECLARE @fBeef3   BIGINT=(SELECT id FROM foods WHERE name=N'Thịt bò xào');
  DECLARE @fEgg3    BIGINT=(SELECT id FROM foods WHERE name=N'Trứng luộc');
  DECLARE @fWhey3   BIGINT=(SELECT id FROM foods WHERE name=N'Whey protein');
  DECLARE @fBan3    BIGINT=(SELECT id FROM foods WHERE name=N'Chuối');
  DECLARE @fVeg13   BIGINT=(SELECT id FROM foods WHERE name=N'Rau muống xào');
  DECLARE @fBR3     BIGINT=(SELECT id FROM foods WHERE name=N'Cơm gạo lứt');
  DECLARE @fPork3   BIGINT=(SELECT id FROM foods WHERE name=N'Ức heo luộc');
  DECLARE @fYog3    BIGINT=(SELECT id FROM foods WHERE name=N'Yogurt không đường');

  -- 10 ngày cho user3 (tăng cơ, calo cao)
  INSERT INTO meal_logs (user_id,food_id,meal_type,log_date,quantity,unit,calories,protein,carbs,fat,created_at) VALUES
    -- Ngày -10 đến -1, hôm nay
    (@u3b,@fOat3,  'breakfast',CAST(DATEADD(day,-10,GETDATE())AS DATE),100,'g',389,17.0,66.0,7.0,DATEADD(day,-10,GETDATE())),
    (@u3b,@fEgg3,  'breakfast',CAST(DATEADD(day,-10,GETDATE())AS DATE),180,'g',279,23.4,19.8,19.8,DATEADD(day,-10,GETDATE())),
    (@u3b,@fWhey3, 'breakfast',CAST(DATEADD(day,-10,GETDATE())AS DATE),30,'g',120,24.0,3.0,1.5,DATEADD(day,-10,GETDATE())),
    (@u3b,@fRice3, 'lunch',    CAST(DATEADD(day,-10,GETDATE())AS DATE),300,'g',390,8.1,84.6,0.9,DATEADD(day,-10,GETDATE())),
    (@u3b,@fChick3,'lunch',    CAST(DATEADD(day,-10,GETDATE())AS DATE),200,'g',318,64.0,0.0,6.4,DATEADD(day,-10,GETDATE())),
    (@u3b,@fVeg13, 'lunch',    CAST(DATEADD(day,-10,GETDATE())AS DATE),100,'g',21,2.6,3.1,0.3,DATEADD(day,-10,GETDATE())),
    (@u3b,@fBeef3, 'dinner',   CAST(DATEADD(day,-10,GETDATE())AS DATE),200,'g',434,52.0,0.0,24.0,DATEADD(day,-10,GETDATE())),
    (@u3b,@fBR3,   'dinner',   CAST(DATEADD(day,-10,GETDATE())AS DATE),250,'g',278,6.5,57.5,2.3,DATEADD(day,-10,GETDATE())),
    (@u3b,@fBan3,  'snack',    CAST(DATEADD(day,-10,GETDATE())AS DATE),200,'g',178,2.2,46.0,0.6,DATEADD(day,-10,GETDATE())),
    (@u3b,@fYog3,  'snack',    CAST(DATEADD(day,-10,GETDATE())AS DATE),200,'g',118,20.0,7.2,0.8,DATEADD(day,-10,GETDATE())),
    -- Ngày -7
    (@u3b,@fOat3,  'breakfast',CAST(DATEADD(day,-7,GETDATE())AS DATE),100,'g',389,17.0,66.0,7.0,DATEADD(day,-7,GETDATE())),
    (@u3b,@fEgg3,  'breakfast',CAST(DATEADD(day,-7,GETDATE())AS DATE),180,'g',279,23.4,19.8,19.8,DATEADD(day,-7,GETDATE())),
    (@u3b,@fWhey3, 'breakfast',CAST(DATEADD(day,-7,GETDATE())AS DATE),30,'g',120,24.0,3.0,1.5,DATEADD(day,-7,GETDATE())),
    (@u3b,@fRice3, 'lunch',    CAST(DATEADD(day,-7,GETDATE())AS DATE),300,'g',390,8.1,84.6,0.9,DATEADD(day,-7,GETDATE())),
    (@u3b,@fPork3, 'lunch',    CAST(DATEADD(day,-7,GETDATE())AS DATE),200,'g',286,52.0,0.0,8.0,DATEADD(day,-7,GETDATE())),
    (@u3b,@fVeg13, 'lunch',    CAST(DATEADD(day,-7,GETDATE())AS DATE),100,'g',21,2.6,3.1,0.3,DATEADD(day,-7,GETDATE())),
    (@u3b,@fChick3,'dinner',   CAST(DATEADD(day,-7,GETDATE())AS DATE),200,'g',318,64.0,0.0,6.4,DATEADD(day,-7,GETDATE())),
    (@u3b,@fBR3,   'dinner',   CAST(DATEADD(day,-7,GETDATE())AS DATE),250,'g',278,6.5,57.5,2.3,DATEADD(day,-7,GETDATE())),
    (@u3b,@fBan3,  'snack',    CAST(DATEADD(day,-7,GETDATE())AS DATE),200,'g',178,2.2,46.0,0.6,DATEADD(day,-7,GETDATE())),
    -- Ngày -5 và -3
    (@u3b,@fOat3,  'breakfast',CAST(DATEADD(day,-5,GETDATE())AS DATE),100,'g',389,17.0,66.0,7.0,DATEADD(day,-5,GETDATE())),
    (@u3b,@fEgg3,  'breakfast',CAST(DATEADD(day,-5,GETDATE())AS DATE),180,'g',279,23.4,19.8,19.8,DATEADD(day,-5,GETDATE())),
    (@u3b,@fWhey3, 'breakfast',CAST(DATEADD(day,-5,GETDATE())AS DATE),30,'g',120,24.0,3.0,1.5,DATEADD(day,-5,GETDATE())),
    (@u3b,@fRice3, 'lunch',    CAST(DATEADD(day,-5,GETDATE())AS DATE),300,'g',390,8.1,84.6,0.9,DATEADD(day,-5,GETDATE())),
    (@u3b,@fBeef3, 'lunch',    CAST(DATEADD(day,-5,GETDATE())AS DATE),200,'g',434,52.0,0.0,24.0,DATEADD(day,-5,GETDATE())),
    (@u3b,@fChick3,'dinner',   CAST(DATEADD(day,-5,GETDATE())AS DATE),200,'g',318,64.0,0.0,6.4,DATEADD(day,-5,GETDATE())),
    (@u3b,@fBR3,   'dinner',   CAST(DATEADD(day,-5,GETDATE())AS DATE),250,'g',278,6.5,57.5,2.3,DATEADD(day,-5,GETDATE())),
    (@u3b,@fYog3,  'snack',    CAST(DATEADD(day,-5,GETDATE())AS DATE),200,'g',118,20.0,7.2,0.8,DATEADD(day,-5,GETDATE())),
    -- Hôm nay
    (@u3b,@fOat3,  'breakfast',CAST(GETDATE()AS DATE),100,'g',389,17.0,66.0,7.0,GETDATE()),
    (@u3b,@fEgg3,  'breakfast',CAST(GETDATE()AS DATE),180,'g',279,23.4,19.8,19.8,GETDATE()),
    (@u3b,@fWhey3, 'breakfast',CAST(GETDATE()AS DATE),30,'g',120,24.0,3.0,1.5,GETDATE()),
    (@u3b,@fRice3, 'lunch',    CAST(GETDATE()AS DATE),300,'g',390,8.1,84.6,0.9,GETDATE()),
    (@u3b,@fChick3,'lunch',    CAST(GETDATE()AS DATE),200,'g',318,64.0,0.0,6.4,GETDATE()),
    (@u3b,@fVeg13, 'lunch',    CAST(GETDATE()AS DATE),100,'g',21,2.6,3.1,0.3,GETDATE()),
    (@u3b,@fBeef3, 'dinner',   CAST(GETDATE()AS DATE),200,'g',434,52.0,0.0,24.0,GETDATE()),
    (@u3b,@fBR3,   'dinner',   CAST(GETDATE()AS DATE),250,'g',278,6.5,57.5,2.3,GETDATE()),
    (@u3b,@fBan3,  'snack',    CAST(GETDATE()AS DATE),200,'g',178,2.2,46.0,0.6,GETDATE()),
    (@u3b,@fYog3,  'snack',    CAST(GETDATE()AS DATE),200,'g',118,20.0,7.2,0.8,GETDATE());
END
GO

-- ══════════════════════════════════════════════════════════════
-- 9. DAILY NUTRITION LOGS (21 ngày – user1)
-- ══════════════════════════════════════════════════════════════
DECLARE @u1c BIGINT=(SELECT id FROM users WHERE email='user1@vitatrack.vn');
DECLARE @u2c BIGINT=(SELECT id FROM users WHERE email='user2@vitatrack.vn');
DECLARE @u3c BIGINT=(SELECT id FROM users WHERE email='user3@vitatrack.vn');

IF NOT EXISTS (SELECT 1 FROM daily_nutrition_logs WHERE user_id=@u1c)
  INSERT INTO daily_nutrition_logs (user_id,log_date,total_calories,total_protein_g,total_fat_g,total_carbs_g,calorie_budget,calorie_balance) VALUES
    (@u1c,CAST(DATEADD(day,-21,GETDATE())AS DATE), 2145, 118, 65, 238, 2300, 155),
    (@u1c,CAST(DATEADD(day,-20,GETDATE())AS DATE), 2320, 108, 71, 272, 2300, -20),
    (@u1c,CAST(DATEADD(day,-19,GETDATE())AS DATE), 2018,  95, 57, 225, 2300, 282),
    (@u1c,CAST(DATEADD(day,-18,GETDATE())AS DATE), 2280, 115, 62, 260, 2300,  20),
    (@u1c,CAST(DATEADD(day,-17,GETDATE())AS DATE), 2195, 112, 68, 244, 2300, 105),
    (@u1c,CAST(DATEADD(day,-14,GETDATE())AS DATE), 2085, 102, 59, 240, 2300, 215),
    (@u1c,CAST(DATEADD(day,-13,GETDATE())AS DATE), 2248, 110, 65, 260, 2300,  52),
    (@u1c,CAST(DATEADD(day,-12,GETDATE())AS DATE), 1975,  98, 52, 218, 2300, 325),
    (@u1c,CAST(DATEADD(day,-11,GETDATE())AS DATE), 2405, 112, 72, 278, 2300,-105),
    (@u1c,CAST(DATEADD(day,-7,GETDATE())AS DATE),  2060,  99, 58, 232, 2300, 240),
    (@u1c,CAST(DATEADD(day,-6,GETDATE())AS DATE),  2185, 106, 63, 248, 2300, 115),
    (@u1c,CAST(DATEADD(day,-5,GETDATE())AS DATE),  2010,  93, 54, 228, 2300, 290),
    (@u1c,CAST(DATEADD(day,-4,GETDATE())AS DATE),  2320, 117, 72, 260, 2300, -20),
    (@u1c,CAST(DATEADD(day,-3,GETDATE())AS DATE),  2075, 104, 62, 238, 2300, 225),
    (@u1c,CAST(DATEADD(day,-2,GETDATE())AS DATE),  2145, 107, 65, 244, 2300, 155),
    (@u1c,CAST(DATEADD(day,-1,GETDATE())AS DATE),  2190, 112, 67, 250, 2300, 110),
    (@u1c,CAST(GETDATE()AS DATE),                  1759, 110, 61, 178, 2300, 541);

IF NOT EXISTS (SELECT 1 FROM daily_nutrition_logs WHERE user_id=@u2c)
  INSERT INTO daily_nutrition_logs (user_id,log_date,total_calories,total_protein_g,total_fat_g,total_carbs_g,calorie_budget,calorie_balance) VALUES
    (@u2c,CAST(DATEADD(day,-14,GETDATE())AS DATE), 1166, 64, 21, 154, 1500, 334),
    (@u2c,CAST(DATEADD(day,-13,GETDATE())AS DATE), 1415, 72, 38, 162, 1500,  85),
    (@u2c,CAST(DATEADD(day,-12,GETDATE())AS DATE), 1250, 65, 30, 148, 1500, 250),
    (@u2c,CAST(DATEADD(day,-7,GETDATE())AS DATE),  1480, 71, 40, 166, 1500,  20),
    (@u2c,CAST(DATEADD(day,-6,GETDATE())AS DATE),  1320, 63, 28, 148, 1500, 180),
    (@u2c,CAST(DATEADD(day,-5,GETDATE())AS DATE),  1390, 67, 35, 156, 1500, 110),
    (@u2c,CAST(DATEADD(day,-4,GETDATE())AS DATE),  1210, 59, 24, 140, 1500, 290),
    (@u2c,CAST(DATEADD(day,-3,GETDATE())AS DATE),  1445, 72, 38, 160, 1500,  55),
    (@u2c,CAST(DATEADD(day,-2,GETDATE())AS DATE),  1320, 64, 30, 148, 1500, 180),
    (@u2c,CAST(DATEADD(day,-1,GETDATE())AS DATE),  1380, 68, 35, 152, 1500, 120),
    (@u2c,CAST(GETDATE()AS DATE),                   928, 62, 18, 113, 1500, 572);

IF NOT EXISTS (SELECT 1 FROM daily_nutrition_logs WHERE user_id=@u3c)
  INSERT INTO daily_nutrition_logs (user_id,log_date,total_calories,total_protein_g,total_fat_g,total_carbs_g,calorie_budget,calorie_balance) VALUES
    (@u3c,CAST(DATEADD(day,-10,GETDATE())AS DATE), 2957, 161, 62, 298, 3000,  43),
    (@u3c,CAST(DATEADD(day,-7,GETDATE())AS DATE),  3085, 170, 68, 310, 3000, -85),
    (@u3c,CAST(DATEADD(day,-5,GETDATE())AS DATE),  3015, 165, 64, 302, 3000, -15),
    (@u3c,CAST(GETDATE()AS DATE),                  2866, 161, 63, 280, 3000, 134);
GO

-- ══════════════════════════════════════════════════════════════
-- 10. ACTIVITY LOGS (21 ngày – user1, 14 ngày – user2, 10 ngày – user3)
-- ══════════════════════════════════════════════════════════════
DECLARE @u1d BIGINT=(SELECT id FROM users WHERE email='user1@vitatrack.vn');
DECLARE @u2d BIGINT=(SELECT id FROM users WHERE email='user2@vitatrack.vn');
DECLARE @u3d BIGINT=(SELECT id FROM users WHERE email='user3@vitatrack.vn');

IF NOT EXISTS (SELECT 1 FROM activity_logs WHERE user_id=@u1d)
  INSERT INTO activity_logs (user_id,activity_type,duration,calories_burned,steps_count,sleep_hours,heart_rate_avg,log_date,source) VALUES
    (@u1d,'running', 35, 320, 4800, 7.5, 148, CAST(GETDATE()AS DATE),               'manual'),
    (@u1d,'walking', 20,  80, 2500, NULL,  88, CAST(GETDATE()AS DATE),               'manual'),
    (@u1d,'running', 30, 285, 4200, 8.0, 145, CAST(DATEADD(day,-1,GETDATE())AS DATE),'manual'),
    (@u1d,'cycling', 45, 360, 6000, NULL, 132, CAST(DATEADD(day,-2,GETDATE())AS DATE),'manual'),
    (@u1d,'walking', 25,  95, 3100, 7.0,  82, CAST(DATEADD(day,-2,GETDATE())AS DATE),'manual'),
    (@u1d,'gym',     60, 420, 3500, NULL, 125, CAST(DATEADD(day,-3,GETDATE())AS DATE),'manual'),
    (@u1d,'yoga',    30,  90, 1200, 8.5,  75, CAST(DATEADD(day,-4,GETDATE())AS DATE),'manual'),
    (@u1d,'running', 40, 380, 5500, NULL, 152, CAST(DATEADD(day,-5,GETDATE())AS DATE),'manual'),
    (@u1d,'walking', 30, 110, 3800, 7.5,  85, CAST(DATEADD(day,-6,GETDATE())AS DATE),'manual'),
    (@u1d,'gym',     60, 450, 3600, 8.0, 128, CAST(DATEADD(day,-7,GETDATE())AS DATE),'manual'),
    (@u1d,'running', 35, 325, 4900, NULL, 149, CAST(DATEADD(day,-8,GETDATE())AS DATE),'manual'),
    (@u1d,'swimming',30, 300, 2500, 7.0, 135, CAST(DATEADD(day,-9,GETDATE())AS DATE),'manual'),
    (@u1d,'yoga',    45, 130, 1500, 8.5,  72, CAST(DATEADD(day,-10,GETDATE())AS DATE),'manual'),
    (@u1d,'cycling', 50, 400, 6500, NULL, 138, CAST(DATEADD(day,-11,GETDATE())AS DATE),'manual'),
    (@u1d,'running', 35, 320, 4800, 7.5, 148, CAST(DATEADD(day,-12,GETDATE())AS DATE),'manual'),
    (@u1d,'gym',     60, 430, 3400, 8.0, 122, CAST(DATEADD(day,-13,GETDATE())AS DATE),'manual'),
    (@u1d,'walking', 40, 145, 5200, 7.0,  88, CAST(DATEADD(day,-14,GETDATE())AS DATE),'manual'),
    (@u1d,'running', 30, 275, 4000, NULL, 142, CAST(DATEADD(day,-17,GETDATE())AS DATE),'manual'),
    (@u1d,'cycling', 45, 360, 6100, 7.5, 130, CAST(DATEADD(day,-18,GETDATE())AS DATE),'manual'),
    (@u1d,'yoga',    30,  90, 1200, 8.0,  74, CAST(DATEADD(day,-19,GETDATE())AS DATE),'manual'),
    (@u1d,'running', 40, 385, 5600, NULL, 155, CAST(DATEADD(day,-20,GETDATE())AS DATE),'manual'),
    (@u1d,'gym',     55, 410, 3300, 8.0, 119, CAST(DATEADD(day,-21,GETDATE())AS DATE),'manual');

IF NOT EXISTS (SELECT 1 FROM activity_logs WHERE user_id=@u2d)
  INSERT INTO activity_logs (user_id,activity_type,duration,calories_burned,steps_count,sleep_hours,heart_rate_avg,log_date,source) VALUES
    (@u2d,'walking', 30, 110, 4200, 7.0,  82, CAST(GETDATE()AS DATE),               'manual'),
    (@u2d,'yoga',    45, 130, 2100, 7.5,  72, CAST(DATEADD(day,-1,GETDATE())AS DATE),'manual'),
    (@u2d,'walking', 25,  90, 3500, 6.5,  80, CAST(DATEADD(day,-2,GETDATE())AS DATE),'manual'),
    (@u2d,'swimming',30, 240, 2000, 8.0, 128, CAST(DATEADD(day,-3,GETDATE())AS DATE),'manual'),
    (@u2d,'walking', 20,  72, 2800, 7.0,  78, CAST(DATEADD(day,-4,GETDATE())AS DATE),'manual'),
    (@u2d,'yoga',    30,  85, 1500, 8.0,  70, CAST(DATEADD(day,-5,GETDATE())AS DATE),'manual'),
    (@u2d,'walking', 35, 125, 4800, 6.0,  84, CAST(DATEADD(day,-6,GETDATE())AS DATE),'manual'),
    (@u2d,'swimming',25, 200, 1800, 7.5, 122, CAST(DATEADD(day,-7,GETDATE())AS DATE),'manual'),
    (@u2d,'yoga',    45, 130, 2100, 7.0,  71, CAST(DATEADD(day,-8,GETDATE())AS DATE),'manual'),
    (@u2d,'walking', 30, 108, 4100, 8.0,  79, CAST(DATEADD(day,-9,GETDATE())AS DATE),'manual'),
    (@u2d,'swimming',30, 240, 2000, 6.5, 126, CAST(DATEADD(day,-10,GETDATE())AS DATE),'manual'),
    (@u2d,'yoga',    30,  85, 1500, 7.5,  70, CAST(DATEADD(day,-11,GETDATE())AS DATE),'manual'),
    (@u2d,'walking', 40, 140, 5500, 7.0,  83, CAST(DATEADD(day,-12,GETDATE())AS DATE),'manual'),
    (@u2d,'walking', 25,  90, 3600, 8.0,  76, CAST(DATEADD(day,-13,GETDATE())AS DATE),'manual'),
    (@u2d,'yoga',    45, 130, 2100, 7.0,  72, CAST(DATEADD(day,-14,GETDATE())AS DATE),'manual');

IF NOT EXISTS (SELECT 1 FROM activity_logs WHERE user_id=@u3d)
  INSERT INTO activity_logs (user_id,activity_type,duration,calories_burned,steps_count,sleep_hours,heart_rate_avg,log_date,source) VALUES
    (@u3d,'gym',     70, 520, 4000, 8.0, 140, CAST(GETDATE()AS DATE),               'manual'),
    (@u3d,'running', 20, 195, 3000, 7.5, 162, CAST(GETDATE()AS DATE),               'manual'),
    (@u3d,'gym',     65, 490, 3800, 7.5, 138, CAST(DATEADD(day,-2,GETDATE())AS DATE),'manual'),
    (@u3d,'gym',     75, 560, 4200, 8.5, 145, CAST(DATEADD(day,-4,GETDATE())AS DATE),'manual'),
    (@u3d,'running', 25, 240, 3500, 7.0, 158, CAST(DATEADD(day,-5,GETDATE())AS DATE),'manual'),
    (@u3d,'gym',     70, 525, 4100, 8.0, 142, CAST(DATEADD(day,-6,GETDATE())AS DATE),'manual'),
    (@u3d,'gym',     60, 450, 3500, 8.0, 135, CAST(DATEADD(day,-7,GETDATE())AS DATE),'manual'),
    (@u3d,'running', 20, 195, 3000, 7.5, 155, CAST(DATEADD(day,-8,GETDATE())AS DATE),'manual'),
    (@u3d,'gym',     65, 490, 3800, 8.5, 140, CAST(DATEADD(day,-9,GETDATE())AS DATE),'manual'),
    (@u3d,'gym',     70, 520, 4000, 7.5, 138, CAST(DATEADD(day,-10,GETDATE())AS DATE),'manual');
GO

-- ══════════════════════════════════════════════════════════════
-- 11. HEALTH ALERTS (đa dạng loại, có acknowledged)
-- ══════════════════════════════════════════════════════════════
DECLARE @u1e BIGINT=(SELECT id FROM users WHERE email='user1@vitatrack.vn');
DECLARE @u2e BIGINT=(SELECT id FROM users WHERE email='user2@vitatrack.vn');
DECLARE @u3e BIGINT=(SELECT id FROM users WHERE email='user3@vitatrack.vn');

IF NOT EXISTS (SELECT 1 FROM health_alerts WHERE user_id=@u1e)
  INSERT INTO health_alerts (user_id,alert_type,severity,message,is_read,triggered_value,threshold_value,acknowledged,acknowledged_note,acknowledged_at,created_at) VALUES
    (@u1e,'CALORIE_OVERAGE','MEDIUM',
     N'Hôm qua bạn đã nạp 2320 kcal, vượt 20 kcal so với mục tiêu 2300 kcal.',
     1, 2320, 2300, 1, N'Đã tư vấn người dùng điều chỉnh khẩu phần bữa tối. Hôm nay đã về mức bình thường.', DATEADD(day,-1,GETDATE()), DATEADD(day,-2,GETDATE())),
    (@u1e,'BMI_OVERWEIGHT','LOW',
     N'BMI 24.87 đang tiến gần mức thừa cân (≥25). Duy trì chế độ ăn uống và vận động.',
     1, 24.87, 25.0, 0, NULL, NULL, DATEADD(day,-10,GETDATE())),
    (@u1e,'CALORIE_OVERAGE','MEDIUM',
     N'4 ngày trước bạn nạp 2405 kcal – vượt ngân sách 5%.',
     0, 2405, 2300, 0, NULL, NULL, DATEADD(day,-4,GETDATE())),
    (@u1e,'SLEEP_DEFICIT','LOW',
     N'Giấc ngủ ngày -9 chỉ đạt 7 giờ – hơi ít. Cố gắng ngủ đủ 7.5 giờ mỗi đêm.',
     0, 7.0, 7.5, 0, NULL, NULL, DATEADD(day,-9,GETDATE())),
    (@u1e,'LOW_PROTEIN','MEDIUM',
     N'Protein ngày -5 chỉ đạt 93g, thấp hơn mức khuyến nghị 110g/ngày.',
     0, 93, 110, 0, NULL, NULL, DATEADD(day,-5,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM health_alerts WHERE user_id=@u2e)
  INSERT INTO health_alerts (user_id,alert_type,severity,message,is_read,triggered_value,threshold_value,acknowledged,created_at) VALUES
    (@u2e,'LOW_PROTEIN','MEDIUM',
     N'Protein hôm qua chỉ đạt 68g, thấp hơn khuyến nghị 75g. Thêm trứng hoặc đậu phụ.',
     0, 68, 75, 0, DATEADD(day,-1,GETDATE())),
    (@u2e,'CALORIE_DEFICIT','LOW',
     N'Bạn nạp 928 kcal hôm nay (đến thời điểm hiện tại). Hãy ăn thêm bữa tối.',
     0, 928, 1500, 0, GETDATE()),
    (@u2e,'LOW_CALORIE_STREAK','HIGH',
     N'Ăn ít calo liên tục – nguy cơ thiếu dinh dưỡng: Bạn đã ăn dưới 750 kcal/ngày (50% TDEE) trong 3 ngày liên tiếp.',
     0, 3, 3, 0, DATEADD(day,-5,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM health_alerts WHERE user_id=@u3e)
  INSERT INTO health_alerts (user_id,alert_type,severity,message,is_read,triggered_value,threshold_value,acknowledged,created_at) VALUES
    (@u3e,'BMI_CRITICAL_LOW','HIGH',
     N'BMI 20.24 đang ở mức thấp. Với mục tiêu tăng cân, hãy đảm bảo ăn đủ calo và protein.',
     0, 20.24, 22.0, 0, DATEADD(day,-3,GETDATE())),
    (@u3e,'TACHYCARDIA','HIGH',
     N'Nhịp tim trung bình trong buổi tập gần nhất: 162 bpm. Hãy nghỉ ngơi đủ giữa các set.',
     0, 162, 160, 0, DATEADD(day,-1,GETDATE()));
GO

-- ══════════════════════════════════════════════════════════════
-- 12. GOALS
-- ══════════════════════════════════════════════════════════════
DECLARE @u1f BIGINT=(SELECT id FROM users WHERE email='user1@vitatrack.vn');
DECLARE @u2f BIGINT=(SELECT id FROM users WHERE email='user2@vitatrack.vn');
DECLARE @u3f BIGINT=(SELECT id FROM users WHERE email='user3@vitatrack.vn');

IF NOT EXISTS (SELECT 1 FROM goals WHERE user_id=@u1f)
  INSERT INTO goals (user_id,goal_type,target_value,start_date,end_date,status) VALUES
    (@u1f,'LOSE_WEIGHT',68.0,CAST(DATEADD(day,-28,GETDATE())AS DATE),CAST(DATEADD(day,60,GETDATE())AS DATE),'ACTIVE'),
    (@u1f,'STEPS',10000,CAST(DATEADD(day,-28,GETDATE())AS DATE),NULL,'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM goals WHERE user_id=@u2f)
  INSERT INTO goals (user_id,goal_type,target_value,start_date,end_date,status) VALUES
    (@u2f,'LOSE_WEIGHT',50.0,CAST(DATEADD(day,-18,GETDATE())AS DATE),CAST(DATEADD(day,90,GETDATE())AS DATE),'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM goals WHERE user_id=@u3f)
  INSERT INTO goals (user_id,goal_type,target_value,start_date,end_date,status) VALUES
    (@u3f,'GAIN_WEIGHT',67.0,CAST(DATEADD(day,-10,GETDATE())AS DATE),CAST(DATEADD(day,60,GETDATE())AS DATE),'ACTIVE'),
    (@u3f,'STEPS',12000,CAST(DATEADD(day,-10,GETDATE())AS DATE),NULL,'ACTIVE');
GO

-- ══════════════════════════════════════════════════════════════
-- 13. EXPERT ASSIGNMENTS & CONSULTATIONS (với status)
-- ══════════════════════════════════════════════════════════════
DECLARE @exp1 BIGINT=(SELECT id FROM users WHERE email='expert1@vitatrack.vn');
DECLARE @exp2 BIGINT=(SELECT id FROM users WHERE email='expert2@vitatrack.vn');
DECLARE @u1g  BIGINT=(SELECT id FROM users WHERE email='user1@vitatrack.vn');
DECLARE @u2g  BIGINT=(SELECT id FROM users WHERE email='user2@vitatrack.vn');
DECLARE @u3g  BIGINT=(SELECT id FROM users WHERE email='user3@vitatrack.vn');

IF NOT EXISTS (SELECT 1 FROM expert_assignments WHERE expert_id=@exp1 AND client_id=@u1g)
  INSERT INTO expert_assignments (expert_id,client_id,status,assigned_at)
  VALUES (@exp1,@u1g,'active',DATEADD(day,-25,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM expert_assignments WHERE expert_id=@exp1 AND client_id=@u2g)
  INSERT INTO expert_assignments (expert_id,client_id,status,assigned_at)
  VALUES (@exp1,@u2g,'active',DATEADD(day,-15,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM expert_assignments WHERE expert_id=@exp2 AND client_id=@u3g)
  INSERT INTO expert_assignments (expert_id,client_id,status,assigned_at)
  VALUES (@exp2,@u3g,'active',DATEADD(day,-8,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM expert_consultations WHERE expert_id=@exp1 AND user_id=@u1g)
  INSERT INTO expert_consultations (expert_id,user_id,status,notes,started_at)
  VALUES (@exp1,@u1g,'ACTIVE',N'Tư vấn giảm cân và xây dựng thực đơn',DATEADD(day,-25,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM expert_consultations WHERE expert_id=@exp1 AND user_id=@u2g)
  INSERT INTO expert_consultations (expert_id,user_id,status,notes,started_at)
  VALUES (@exp1,@u2g,'ACTIVE',N'Tư vấn dinh dưỡng, xử lý dị ứng Lactose/Gluten',DATEADD(day,-15,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM expert_consultations WHERE expert_id=@exp2 AND user_id=@u3g)
  INSERT INTO expert_consultations (expert_id,user_id,status,notes,started_at)
  VALUES (@exp2,@u3g,'ACTIVE',N'Tư vấn tăng cơ, tối ưu dinh dưỡng thể thao',DATEADD(day,-8,GETDATE()));
GO

-- ══════════════════════════════════════════════════════════════
-- 14. EXPERT CHATS (phong phú hơn)
-- ══════════════════════════════════════════════════════════════
DECLARE @exp1h BIGINT=(SELECT id FROM users WHERE email='expert1@vitatrack.vn');
DECLARE @exp2h BIGINT=(SELECT id FROM users WHERE email='expert2@vitatrack.vn');
DECLARE @u1h   BIGINT=(SELECT id FROM users WHERE email='user1@vitatrack.vn');
DECLARE @u2h   BIGINT=(SELECT id FROM users WHERE email='user2@vitatrack.vn');
DECLARE @u3h   BIGINT=(SELECT id FROM users WHERE email='user3@vitatrack.vn');

IF NOT EXISTS (SELECT 1 FROM expert_chats WHERE expert_id=@exp1h AND client_id=@u1h)
  INSERT INTO expert_chats (expert_id,client_id,role,content,is_read,created_at) VALUES
    (@exp1h,@u1h,'expert',N'Chào anh An! Tôi đã xem hồ sơ sức khoẻ của anh. BMI 24.87 đang ổn, chúng ta cùng giảm về 68kg nhé.',1,DATEADD(day,-24,GETDATE())),
    (@exp1h,@u1h,'client',N'Cảm ơn chị! Em muốn giảm về 68kg trong 2 tháng, liệu khả thi không ạ?',1,DATEADD(day,-24,GETDATE())),
    (@exp1h,@u1h,'expert',N'Hoàn toàn khả thi! Cần thâm hụt 300–400 kcal/ngày. Tôi sẽ thiết kế thực đơn phù hợp nhé.',1,DATEADD(day,-23,GETDATE())),
    (@exp1h,@u1h,'client',N'Em chạy bộ 3 buổi/tuần và tập gym 2 buổi. Như vậy đủ không ạ?',1,DATEADD(day,-20,GETDATE())),
    (@exp1h,@u1h,'expert',N'Rất tốt! Lịch tập 5 buổi/tuần rất phù hợp. Tôi vừa gửi thực đơn tuần 1, anh xem nhé.',1,DATEADD(day,-19,GETDATE())),
    (@exp1h,@u1h,'client',N'Em xem rồi, thực đơn hợp lý lắm. Chỉ tội buổi sáng em thường không có thời gian nấu.',1,DATEADD(day,-16,GETDATE())),
    (@exp1h,@u1h,'expert',N'Anh có thể chuẩn bị yến mạch tối hôm trước, để tủ lạnh, sáng chỉ cần 3 phút là xong!',1,DATEADD(day,-16,GETDATE())),
    (@exp1h,@u1h,'client',N'Hay quá! Em sẽ thử ngay. Cảm ơn chị ạ.',1,DATEADD(day,-15,GETDATE())),
    (@exp1h,@u1h,'expert',N'Tuần 1 xong rồi anh nhé! Cân nặng giảm 0.9kg – rất tốt! Tiếp tục tuần 2 nhé.',1,DATEADD(day,-12,GETDATE())),
    (@exp1h,@u1h,'client',N'Dạ, em cảm thấy khoẻ hơn hẳn ạ. Chị ơi hôm qua em ăn vượt calo, em lo quá.',1,DATEADD(day,-4,GETDATE())),
    (@exp1h,@u1h,'expert',N'Không sao anh! Một ngày vượt không ảnh hưởng lớn. Cả tuần vẫn đúng hướng!',1,DATEADD(day,-4,GETDATE())),
    (@exp1h,@u1h,'client',N'Dạ em cảm ơn. Tuần này em đạt 73.5kg rồi, xuống 2.5kg sau 3 tuần!',0,DATEADD(hour,-2,GETDATE())),
    (@exp1h,@u1h,'expert',N'Tuyệt vời anh! Tiến độ rất tốt. Tuần tới mình giảm thêm 100 kcal nữa nhé.',0,DATEADD(hour,-1,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM expert_chats WHERE expert_id=@exp1h AND client_id=@u2h)
  INSERT INTO expert_chats (expert_id,client_id,role,content,is_read,created_at) VALUES
    (@exp1h,@u2h,'expert',N'Chào chị Bích! Tôi thấy chị dị ứng Lactose và Gluten nhẹ. Tôi sẽ thiết kế thực đơn tránh hai nhóm này.',1,DATEADD(day,-14,GETDATE())),
    (@exp1h,@u2h,'client',N'Cảm ơn bác sĩ, dạo này em hay mệt và thèm ăn vặt lắm ạ.',1,DATEADD(day,-13,GETDATE())),
    (@exp1h,@u2h,'expert',N'Thèm ăn vặt thường do thiếu protein. Chị thử ăn thêm trứng luộc buổi sáng xem sao.',1,DATEADD(day,-12,GETDATE())),
    (@exp1h,@u2h,'client',N'Em thử rồi, đỡ thèm hơn nhiều ạ. Protein tăng lên rõ.',1,DATEADD(day,-8,GETDATE())),
    (@exp1h,@u2h,'expert',N'Tuyệt vời! Chị tiếp tục nhé. Tôi sẽ xem lại thực đơn tuần tới cho chị.',1,DATEADD(day,-7,GETDATE())),
    (@exp1h,@u2h,'client',N'Dạ cảm ơn chị. Chị ơi em không ăn được sữa thì bổ sung canxi bằng gì ạ?',1,DATEADD(day,-3,GETDATE())),
    (@exp1h,@u2h,'expert',N'Chị có thể dùng: đậu phụ, cải xanh, hạnh nhân. Đây đều là nguồn canxi tốt không chứa Lactose.',1,DATEADD(day,-3,GETDATE())),
    (@exp1h,@u2h,'client',N'Dạ em sẽ thử ngay. Cảm ơn chị nhiều ạ!',0,DATEADD(hour,-5,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM expert_chats WHERE expert_id=@exp2h AND client_id=@u3h)
  INSERT INTO expert_chats (expert_id,client_id,role,content,is_read,created_at) VALUES
    (@exp2h,@u3h,'expert',N'Chào Nam! Tôi xem hồ sơ của bạn, BMI 20.24 – bạn cần tăng calo và protein để đạt mục tiêu 67kg.',1,DATEADD(day,-7,GETDATE())),
    (@exp2h,@u3h,'client',N'Dạ thầy ơi em tập gym 5 buổi/tuần nhưng cân không tăng ạ. Chế độ ăn em thiếu gì vậy ạ?',1,DATEADD(day,-6,GETDATE())),
    (@exp2h,@u3h,'expert',N'Bạn cần ăn dư 300-500 kcal so với TDEE. Và phải có đủ protein: ít nhất 2g/kg cân nặng = 124g protein/ngày.',1,DATEADD(day,-6,GETDATE())),
    (@exp2h,@u3h,'client',N'Dạ, em đang bổ sung thêm whey protein buổi sáng rồi ạ. Nhịp tim em hay cao lúc tập, có sao không ạ?',1,DATEADD(day,-4,GETDATE())),
    (@exp2h,@u3h,'expert',N'162 bpm là hơi cao. Bạn nghỉ ngơi đủ giữa các set nhé, ít nhất 90 giây. Không cần cố nặng quá.',1,DATEADD(day,-3,GETDATE())),
    (@exp2h,@u3h,'client',N'Dạ thầy ơi hôm nay em cân được 62kg rồi! Tăng 0.5kg so với tuần trước ạ.',0,DATEADD(hour,-4,GETDATE())),
    (@exp2h,@u3h,'expert',N'Tốt lắm! Tiến độ đúng hướng. Tuần này tập 5 buổi, đảm bảo ăn đủ 3000 kcal nhé.',0,DATEADD(hour,-3,GETDATE()));
GO

-- ══════════════════════════════════════════════════════════════
-- 15. MEAL PLANS (thực đơn từ chuyên gia)
-- ══════════════════════════════════════════════════════════════
DECLARE @exp1i BIGINT=(SELECT id FROM users WHERE email='expert1@vitatrack.vn');
DECLARE @exp2i BIGINT=(SELECT id FROM users WHERE email='expert2@vitatrack.vn');
DECLARE @u1i   BIGINT=(SELECT id FROM users WHERE email='user1@vitatrack.vn');
DECLARE @u2i   BIGINT=(SELECT id FROM users WHERE email='user2@vitatrack.vn');
DECLARE @u3i   BIGINT=(SELECT id FROM users WHERE email='user3@vitatrack.vn');

IF NOT EXISTS (SELECT 1 FROM meal_plans WHERE expert_id=@exp1i AND client_id=@u1i)
  INSERT INTO meal_plans (expert_id,client_id,title,duration,target_calories,notes,status,created_at) VALUES
    (@exp1i,@u1i,N'Thực đơn giảm cân tuần 1', 7, 2100,
     N'Ưu tiên protein cao, giảm tinh bột buổi tối. Uống đủ 2.5L nước mỗi ngày. Ăn chậm nhai kỹ.',
     'sent', DATEADD(day,-21,GETDATE())),
    (@exp1i,@u1i,N'Thực đơn giảm cân tuần 2', 7, 2000,
     N'Giảm thêm 100 kcal. Tăng rau xanh. Bổ sung cá hồi 2 lần/tuần.',
     'sent', DATEADD(day,-14,GETDATE())),
    (@exp1i,@u1i,N'Thực đơn giảm cân tuần 3', 7, 2000,
     N'Duy trì cường độ. Thêm salad buổi trưa. Hạn chế mì gói và đồ chiên.',
     'sent', DATEADD(day,-7,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM meal_plans WHERE expert_id=@exp1i AND client_id=@u2i)
  INSERT INTO meal_plans (expert_id,client_id,title,duration,target_calories,notes,status,created_at) VALUES
    (@exp1i,@u2i,N'Thực đơn không Lactose/Gluten tuần 1', 7, 1400,
     N'Thay sữa bò bằng sữa hạnh nhân. Dùng gạo lứt thay bột mì. Tránh bánh mì thường.',
     'sent', DATEADD(day,-12,GETDATE())),
    (@exp1i,@u2i,N'Thực đơn không Lactose/Gluten tuần 2', 7, 1350,
     N'Giảm thêm 50 kcal. Tăng protein từ trứng và đậu phụ.',
     'sent', DATEADD(day,-5,GETDATE()));

IF NOT EXISTS (SELECT 1 FROM meal_plans WHERE expert_id=@exp2i AND client_id=@u3i)
  INSERT INTO meal_plans (expert_id,client_id,title,duration,target_calories,notes,status,created_at) VALUES
    (@exp2i,@u3i,N'Thực đơn tăng cơ – Tuần 1', 7, 3200,
     N'Ăn 5-6 bữa/ngày. Bổ sung whey protein sau tập. Ưu tiên thịt nạc và cơm gạo lứt.',
     'sent', DATEADD(day,-6,GETDATE()));
GO

-- ══════════════════════════════════════════════════════════════
-- 16. CHAT MESSAGES (AI Assistant – FR-27, vài mẫu cho user1)
-- ══════════════════════════════════════════════════════════════
DECLARE @u1j BIGINT=(SELECT id FROM users WHERE email='user1@vitatrack.vn');
DECLARE @sesId1 NVARCHAR(36)='a1b2c3d4-e5f6-7890-abcd-ef1234567890';
DECLARE @sesId2 NVARCHAR(36)='b2c3d4e5-f6a7-8901-bcde-f12345678901';

IF NOT EXISTS (SELECT 1 FROM chat_messages WHERE user_id=@u1j)
BEGIN
  -- Phiên 1 (3 ngày trước)
  INSERT INTO chat_messages (session_id,user_id,sender,content,has_disclaimer,sent_at) VALUES
    (@sesId1,@u1j,'user',N'Hôm nay tôi nên ăn gì để đạt mục tiêu giảm cân?',0,DATEADD(day,-3,GETDATE())),
    (@sesId1,@u1j,'ai',  N'Dựa trên ngân sách calo còn lại của bạn, tôi gợi ý: Bữa trưa: 150g ức gà nướng + 200g cơm gạo lứt + rau xanh (≈520 kcal). Bữa tối: 150g cá hồi hấp + khoai lang + salad (≈480 kcal).',1,DATEADD(day,-3,GETDATE())),
    (@sesId1,@u1j,'user',N'Protein của tôi hôm nay đạt mức nào rồi?',0,DATEADD(day,-3,GETDATE())),
    (@sesId1,@u1j,'ai',  N'Dựa trên dữ liệu bữa ăn hôm nay, bạn đã nạp khoảng 110g protein, đạt 92% mục tiêu 120g/ngày. Thêm 1 hũ yogurt hoặc 2 quả trứng luộc là đủ!',1,DATEADD(day,-3,GETDATE())),
  -- Phiên 2 (hôm qua)
    (@sesId2,@u1j,'user',N'Tôi vừa tập chạy 35 phút, tôi có thể ăn thêm gì không?',0,DATEADD(day,-1,GETDATE())),
    (@sesId2,@u1j,'ai',  N'Tuyệt vời! Sau chạy bộ 35 phút, bạn đốt khoảng 320 kcal. Ngân sách calo của bạn tăng thêm đó! Bạn có thể ăn thêm: 1 quả chuối + 1 hũ yogurt (≈178 kcal) để hồi phục cơ bắp.',1,DATEADD(day,-1,GETDATE())),
    (@sesId2,@u1j,'user',N'Thực phẩm nào giàu protein mà ít calo?',0,DATEADD(day,-1,GETDATE())),
    (@sesId2,@u1j,'ai',  N'Những lựa chọn tốt nhất: 1) Ức gà luộc – 165 kcal/100g, 31g protein. 2) Tôm luộc – 99 kcal/100g, 24g protein. 3) Trứng luộc – 155 kcal/100g, 13g protein. 4) Đậu phụ – 80 kcal/100g, 8g protein.',1,DATEADD(day,-1,GETDATE()));
END
GO

PRINT '=== VitaTrack Seed Data v4 loaded successfully ===';
PRINT '  Users: 6 (1 admin, 2 expert, 3 user)';
PRINT '  user1: 21 days meal logs, 22 activity logs, 19 weight records';
PRINT '  user2: 14 days meal logs, 15 activity logs, 13 weight records';
PRINT '  user3: 10 days meal logs, 10 activity logs, 10 weight records';
PRINT '  Foods: 35 items | Alerts: 10 | Chats: 28 messages';
GO