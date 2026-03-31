-- ============================================================
-- VitaTrack Database Schema – SQL Server 2019
-- Phiên bản đầy đủ: bao gồm tất cả cột mới từ BE (FR-01→FR-36)
-- ============================================================
USE DB_VitaTrack;
GO

-- ══════════════════════════════════════════════════════════════
-- 1. USERS
-- Bổ sung: is_email_verified, OTP fields, login rate-limiting,
--           last_login_at (so với schema cũ)
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='users' AND xtype='U')
CREATE TABLE users (
    id                              BIGINT IDENTITY(1,1) PRIMARY KEY,
    email                           NVARCHAR(255)  NOT NULL UNIQUE,
    password_hash                   NVARCHAR(255)  NULL,          -- NULL cho Google SSO
    full_name                       NVARCHAR(200)  NOT NULL,
    role                            NVARCHAR(20)   NOT NULL DEFAULT 'USER',
    is_active                       BIT            NOT NULL DEFAULT 1,
    -- FR-01 Email Verification
    is_email_verified               BIT            NOT NULL DEFAULT 0,
    email_verification_token        NVARCHAR(64)   NULL,
    email_verification_expiry       DATETIME2      NULL,
    verification_resend_count       INT            NOT NULL DEFAULT 0,
    verification_resend_window_start DATETIME2     NULL,
    -- FR-02 Login Rate Limiting
    login_attempts                  INT            NOT NULL DEFAULT 0,
    login_locked_until              DATETIME2      NULL,
    -- FR-04 OTP Password Reset
    otp_code                        NVARCHAR(6)    NULL,
    otp_expiry                      DATETIME2      NULL,
    otp_attempts                    INT            NOT NULL DEFAULT 0,
    otp_send_count                  INT            NOT NULL DEFAULT 0,
    otp_window_start                DATETIME2      NULL,
    -- Legacy reset token (tương thích cũ)
    reset_token                     NVARCHAR(200)  NULL,
    reset_token_expiry              DATETIME2      NULL,
    -- Metadata
    avatar_url                      NVARCHAR(500)  NULL,
    expert_status                   NVARCHAR(20)   NULL,
    expert_reject_reason            NVARCHAR(MAX)  NULL,
    last_login_at                   DATETIME2      NULL,
    created_at                      DATETIME2      NOT NULL DEFAULT GETDATE(),
    updated_at                      DATETIME2      NOT NULL DEFAULT GETDATE()
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_Users_Email')
    CREATE INDEX IX_Users_Email      ON users(email);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_Users_VerifToken')
    CREATE INDEX IX_Users_VerifToken ON users(email_verification_token);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_Users_OtpCode')
    CREATE INDEX IX_Users_OtpCode    ON users(otp_code);
GO

-- ══════════════════════════════════════════════════════════════
-- 2. HEALTH PROFILES
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='health_profiles' AND xtype='U')
CREATE TABLE health_profiles (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id            BIGINT         NOT NULL UNIQUE,
    height_cm          FLOAT,
    weight_kg          FLOAT,
    gender             NVARCHAR(10),
    age                INT,
    activity_level     NVARCHAR(30),
    goal_type          NVARCHAR(30),
    target_weight_kg   FLOAT,
    bmi                FLOAT,
    bmr                FLOAT,
    tdee               FLOAT,
    daily_calorie_goal INT,
    daily_steps_goal   INT,
    daily_water_goal   INT,
    allergies          NVARCHAR(1000),
    created_at         DATETIME2      NOT NULL DEFAULT GETDATE(),
    updated_at         DATETIME2      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_HealthProfiles_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_HealthProfiles_UserId')
    CREATE INDEX IX_HealthProfiles_UserId ON health_profiles(user_id);
GO

-- ══════════════════════════════════════════════════════════════
-- 3. WEIGHT HISTORY
-- Bổ sung: notes (FR-10), source (FR-10.AutoSave)
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='weight_history' AND xtype='U')
CREATE TABLE weight_history (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    health_profile_id BIGINT        NOT NULL,
    weight            FLOAT         NOT NULL,
    bmi               FLOAT,
    source            NVARCHAR(30)  NOT NULL DEFAULT 'profile_update',
    notes             NVARCHAR(100) NULL,
    recorded_at       DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_WeightHistory_Profile FOREIGN KEY (health_profile_id)
        REFERENCES health_profiles(id) ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_WeightHistory_ProfileId')
    CREATE INDEX IX_WeightHistory_ProfileId ON weight_history(health_profile_id);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_WeightHistory_RecordedAt')
    CREATE INDEX IX_WeightHistory_RecordedAt ON weight_history(recorded_at);
GO

-- ══════════════════════════════════════════════════════════════
-- 4. FOODS
-- Bổ sung: is_active (FR-33.SoftDelete), fiber_per_100g, serving_unit
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='foods' AND xtype='U')
CREATE TABLE foods (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    name              NVARCHAR(200) NOT NULL,
    calories_per_100g FLOAT         NOT NULL,
    protein_per_100g  FLOAT,
    carbs_per_100g    FLOAT,
    fat_per_100g      FLOAT,
    fiber_per_100g    FLOAT,
    category          NVARCHAR(100),
    serving_unit      NVARCHAR(100),
    allergens         NVARCHAR(500),
    created_by_admin  BIT           NOT NULL DEFAULT 0,
    is_active         BIT           NOT NULL DEFAULT 1,
    created_at        DATETIME2     NOT NULL DEFAULT GETDATE()
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_Foods_Name')
    CREATE INDEX IX_Foods_Name     ON foods(name);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_Foods_Category')
    CREATE INDEX IX_Foods_Category ON foods(category);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_Foods_IsActive')
    CREATE INDEX IX_Foods_IsActive ON foods(is_active);
GO

-- ══════════════════════════════════════════════════════════════
-- 5. MEAL LOGS
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='meal_logs' AND xtype='U')
CREATE TABLE meal_logs (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    food_id      BIGINT        NOT NULL,
    meal_type    NVARCHAR(20)  NOT NULL,
    log_date     DATE          NOT NULL,
    quantity     FLOAT         NOT NULL,
    unit         NVARCHAR(20),
    calories     FLOAT,
    protein      FLOAT,
    carbs        FLOAT,
    fat          FLOAT,
    added_via_ai BIT           NOT NULL DEFAULT 0,
    created_at   DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_MealLogs_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_MealLogs_Food FOREIGN KEY (food_id) REFERENCES foods(id)
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_MealLogs_UserDate')
    CREATE INDEX IX_MealLogs_UserDate ON meal_logs(user_id, log_date);
GO

-- ══════════════════════════════════════════════════════════════
-- 6. ACTIVITY LOGS
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='activity_logs' AND xtype='U')
CREATE TABLE activity_logs (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    activity_type   NVARCHAR(50)  NOT NULL,
    duration        INT           NOT NULL,
    calories_burned FLOAT,
    distance        FLOAT,
    steps_count     INT,
    sleep_hours     FLOAT,
    heart_rate_avg  INT,
    heart_rate_max  INT,
    log_date        DATE          NOT NULL,
    notes           NVARCHAR(500),
    source          NVARCHAR(30)  NOT NULL DEFAULT 'manual',
    created_at      DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_ActivityLogs_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_ActivityLogs_UserDate')
    CREATE INDEX IX_ActivityLogs_UserDate ON activity_logs(user_id, log_date);
GO

-- ══════════════════════════════════════════════════════════════
-- 7. ALLERGIES
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='allergies' AND xtype='U')
CREATE TABLE allergies (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    allergen_name NVARCHAR(100) NOT NULL,
    severity      NVARCHAR(20),
    noted_at      DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Allergies_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT UQ_Allergies_UserAllergen UNIQUE (user_id, allergen_name)
);
GO

-- ══════════════════════════════════════════════════════════════
-- 8. HEALTH ALERTS
-- Bổ sung: acknowledged, acknowledged_note, acknowledged_at (FR-32)
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='health_alerts' AND xtype='U')
CREATE TABLE health_alerts (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id            BIGINT         NOT NULL,
    alert_type         NVARCHAR(50)   NOT NULL,
    severity           NVARCHAR(10)   NOT NULL,
    message            NVARCHAR(1000) NOT NULL,
    triggered_value    FLOAT,
    threshold_value    FLOAT,
    is_read            BIT            NOT NULL DEFAULT 0,
    acknowledged       BIT            NOT NULL DEFAULT 0,
    acknowledged_note  NVARCHAR(200)  NULL,
    acknowledged_at    DATETIME2      NULL,
    created_at         DATETIME2      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_HealthAlerts_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_HealthAlerts_UserId')
    CREATE INDEX IX_HealthAlerts_UserId    ON health_alerts(user_id);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_HealthAlerts_AlertType')
    CREATE INDEX IX_HealthAlerts_AlertType ON health_alerts(alert_type);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_HealthAlerts_CreatedAt')
    CREATE INDEX IX_HealthAlerts_CreatedAt ON health_alerts(created_at);
GO

-- ══════════════════════════════════════════════════════════════
-- 9. DAILY NUTRITION LOGS
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='daily_nutrition_logs' AND xtype='U')
CREATE TABLE daily_nutrition_logs (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    log_date        DATE   NOT NULL,
    total_calories  FLOAT,
    total_protein_g FLOAT,
    total_fat_g     FLOAT,
    total_carbs_g   FLOAT,
    calorie_budget  FLOAT,
    calorie_balance FLOAT,
    CONSTRAINT FK_DailyLogs_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT UQ_DailyLog_UserDate UNIQUE (user_id, log_date)
);
GO

-- ══════════════════════════════════════════════════════════════
-- 10. GOALS
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='goals' AND xtype='U')
CREATE TABLE goals (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    goal_type    NVARCHAR(30) NOT NULL,
    target_value FLOAT,
    start_date   DATE         NOT NULL,
    end_date     DATE,
    status       NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT FK_Goals_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

-- ══════════════════════════════════════════════════════════════
-- 11. REFRESH TOKENS
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='refresh_tokens' AND xtype='U')
CREATE TABLE refresh_tokens (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    token      NVARCHAR(512) NOT NULL UNIQUE,
    user_id    BIGINT        NOT NULL,
    expires_at DATETIME2     NOT NULL,
    created_at DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_RefreshTokens_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

-- ══════════════════════════════════════════════════════════════
-- 12. EXPERT ASSIGNMENTS
-- Bổ sung: status, ended_at (FR-35.AssignExpert / RevokeExpert)
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='expert_assignments' AND xtype='U')
CREATE TABLE expert_assignments (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    expert_id   BIGINT        NOT NULL,
    client_id   BIGINT        NOT NULL,
    status      NVARCHAR(20)  NOT NULL DEFAULT 'active',
    assigned_at DATETIME2     NOT NULL DEFAULT GETDATE(),
    ended_at    DATETIME2     NULL,
    CONSTRAINT FK_ExpertAssignments_Expert FOREIGN KEY (expert_id) REFERENCES users(id),
    CONSTRAINT FK_ExpertAssignments_Client FOREIGN KEY (client_id) REFERENCES users(id),
    CONSTRAINT UQ_ExpertAssignment UNIQUE (expert_id, client_id)
);
GO

-- ══════════════════════════════════════════════════════════════
-- 13. EXPERT CHATS
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='expert_chats' AND xtype='U')
CREATE TABLE expert_chats (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    expert_id  BIGINT        NOT NULL,
    client_id  BIGINT        NOT NULL,
    role       NVARCHAR(10)  NOT NULL,
    content    NVARCHAR(MAX) NOT NULL,
    is_read    BIT           NOT NULL DEFAULT 0,
    created_at DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_ExpertChats_Expert FOREIGN KEY (expert_id) REFERENCES users(id),
    CONSTRAINT FK_ExpertChats_Client FOREIGN KEY (client_id) REFERENCES users(id)
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_ExpertChats_Conversation')
    CREATE INDEX IX_ExpertChats_Conversation ON expert_chats(expert_id, client_id, created_at);
GO

-- ══════════════════════════════════════════════════════════════
-- 14. MEAL PLANS
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='meal_plans' AND xtype='U')
CREATE TABLE meal_plans (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    expert_id       BIGINT        NOT NULL,
    client_id       BIGINT        NOT NULL,
    title           NVARCHAR(200) NOT NULL,
    duration        INT,
    target_calories INT,
    notes           NVARCHAR(MAX),
    status          NVARCHAR(20)  NOT NULL DEFAULT 'draft',
    created_at      DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_MealPlans_Expert FOREIGN KEY (expert_id) REFERENCES users(id),
    CONSTRAINT FK_MealPlans_Client FOREIGN KEY (client_id) REFERENCES users(id)
);
GO

-- ══════════════════════════════════════════════════════════════
-- 15. CHAT MESSAGES (AI Assistant – FR-27)
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='chat_messages' AND xtype='U')
CREATE TABLE chat_messages (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id     NVARCHAR(36)  NOT NULL,
    user_id        BIGINT        NOT NULL,
    sender         NVARCHAR(10)  NOT NULL,    -- 'user' | 'ai'
    content        NVARCHAR(MAX) NOT NULL,
    has_disclaimer BIT           NOT NULL DEFAULT 0,
    sent_at        DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_ChatMessages_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_ChatMsg_UserId')
    CREATE INDEX IX_ChatMsg_UserId    ON chat_messages(user_id);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_ChatMsg_SessionId')
    CREATE INDEX IX_ChatMsg_SessionId ON chat_messages(session_id);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_ChatMsg_SentAt')
    CREATE INDEX IX_ChatMsg_SentAt    ON chat_messages(sent_at);
GO

-- ══════════════════════════════════════════════════════════════
-- 16. WEARABLE DEVICES
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='wearable_devices' AND xtype='U')
CREATE TABLE wearable_devices (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id            BIGINT        NOT NULL,
    device_type        NVARCHAR(50)  NOT NULL,
    external_device_id NVARCHAR(200) NOT NULL,
    access_token       NVARCHAR(500),
    refresh_token      NVARCHAR(500),
    token_expiry       DATETIME2,
    last_sync_at       DATETIME2,
    is_active          BIT           NOT NULL DEFAULT 1,
    CONSTRAINT FK_WearableDevices_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

-- ══════════════════════════════════════════════════════════════
-- 17. AI ANALYSIS RESULTS
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='ai_analysis_results' AND xtype='U')
CREATE TABLE ai_analysis_results (
    id                       BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id                  BIGINT        NOT NULL,
    image_url                NVARCHAR(500) NOT NULL,
    detected_foods           NVARCHAR(MAX),
    total_estimated_calories FLOAT,
    raw_api_response         NVARCHAR(MAX),
    status                   NVARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at               DATETIME2     NOT NULL DEFAULT GETDATE(),
    completed_at             DATETIME2,
    CONSTRAINT FK_AIAnalysis_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

-- ══════════════════════════════════════════════════════════════
-- 18. EXPERT CONSULTATIONS
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='expert_consultations' AND xtype='U')
CREATE TABLE expert_consultations (
    id         BIGINT       IDENTITY(1,1) PRIMARY KEY,
    expert_id  BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    status     NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes      NVARCHAR(MAX),
    started_at DATETIME2,
    closed_at  DATETIME2,
    CONSTRAINT FK_ExpertConsultations_Expert FOREIGN KEY (expert_id) REFERENCES users(id),
    CONSTRAINT FK_ExpertConsultations_User   FOREIGN KEY (user_id)   REFERENCES users(id)
);
GO

-- ══════════════════════════════════════════════════════════════
-- 19. LEGACY: Meals & Meal Items (giữ tương thích schema)
-- ══════════════════════════════════════════════════════════════
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='meals' AND xtype='U')
CREATE TABLE meals (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id        BIGINT        NOT NULL,
    log_date       DATE          NOT NULL,
    meal_type      NVARCHAR(20)  NOT NULL,
    meal_name      NVARCHAR(200),
    total_calories FLOAT,
    notes          NVARCHAR(1000),
    created_at     DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Meals_User FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='meal_items' AND xtype='U')
CREATE TABLE meal_items (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    meal_id           BIGINT NOT NULL,
    food_id           BIGINT NOT NULL,
    quantity_g        FLOAT  NOT NULL,
    calories_consumed FLOAT,
    CONSTRAINT FK_MealItems_Meal FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE,
    CONSTRAINT FK_MealItems_Food FOREIGN KEY (food_id) REFERENCES foods(id)
);
GO

PRINT '=== VitaTrack Schema created successfully ===';
GO