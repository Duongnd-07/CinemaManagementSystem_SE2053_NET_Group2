-- UC-01: Register Account - Bảng lưu mã xác thực Email (OTP)
-- Chạy trên database CinemaDB; chạy lại nhiều lần không lỗi.

USE CinemaDB;
GO

IF OBJECT_ID('EmailVerification', 'U') IS NULL
BEGIN
    CREATE TABLE EmailVerification (
        VerificationID INT IDENTITY(1,1) PRIMARY KEY,
        Email VARCHAR(100) NOT NULL,
        OtpCode VARCHAR(10) NOT NULL,
        ExpiryTime DATETIME NOT NULL,
        IsUsed BIT NOT NULL DEFAULT 0,
        CreatedAt DATETIME NOT NULL DEFAULT GETDATE()
    );
END
GO

-- Index hỗ trợ tìm nhanh mã OTP hợp lệ mới nhất theo Email
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_EmailVerification_Email_Otp')
BEGIN
    CREATE INDEX IX_EmailVerification_Email_Otp ON EmailVerification (Email, OtpCode, IsUsed, ExpiryTime);
END
GO
