-- ============================================================
-- SCRIPT TẠO CƠ SỞ DỮ LIỆU VÀ TÀI KHOẢN - SQL Server 2019
-- Chạy script này bằng tài khoản SA (System Administrator)
-- ============================================================

USE master;
GO

-- ── 1. Tạo database DB_VitaTrack ──────────────────────────────────────────
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'DB_VitaTrack')
BEGIN
    CREATE DATABASE DB_VitaTrack
    COLLATE Vietnamese_CI_AS;
    PRINT 'Database DB_VitaTrack da duoc tao thanh cong.';
END
ELSE
BEGIN
    PRINT 'Database DB_VitaTrack da ton tai.';
END
GO

-- ── 2. Tạo login user2 (SQL Server Authentication) ────────────────────────
IF NOT EXISTS (SELECT name FROM sys.server_principals WHERE name = 'user2')
BEGIN
    CREATE LOGIN user2 WITH PASSWORD = '123456789',
        DEFAULT_DATABASE = DB_VitaTrack,
        CHECK_EXPIRATION = OFF,
        CHECK_POLICY = OFF;
    PRINT 'Login user2 da duoc tao thanh cong.';
END
ELSE
BEGIN
    PRINT 'Login user2 da ton tai.';
END
GO

-- ── 3. Tạo user trong database DB_VitaTrack ───────────────────────────────
USE DB_VitaTrack;
GO

IF NOT EXISTS (SELECT name FROM sys.database_principals WHERE name = 'user2')
BEGIN
    CREATE USER user2 FOR LOGIN user2;
    PRINT 'User user2 trong DB_VitaTrack da duoc tao.';
END
GO

-- ── 4. Cấp quyền db_owner cho user2 ──────────────────────────────────────
ALTER ROLE db_owner ADD MEMBER user2;
PRINT 'Da cap quyen db_owner cho user2.';
GO

PRINT '============================================================';
PRINT 'Hoan tat! Spring Boot se tu dong tao cac bang khi khoi dong.';
PRINT 'Ket noi: jdbc:sqlserver://localhost:1433;databaseName=DB_VitaTrack';
PRINT 'Username: user2 | Password: 123456789';
PRINT '============================================================';
GO
