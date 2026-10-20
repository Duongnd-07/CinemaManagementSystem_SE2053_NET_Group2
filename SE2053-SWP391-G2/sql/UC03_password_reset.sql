-- UC-03: Forgot Password - Bảng lưu token khôi phục mật khẩu qua email
-- Chạy trên database CinemaDB; an toàn khi chạy lại nhiều lần (idempotent).

USE CinemaDB;
GO

IF OBJECT_ID('PasswordResetToken', 'U') IS NULL
BEGIN
    CREATE TABLE PasswordResetToken (
        TokenID INT IDENTITY(1,1) PRIMARY KEY,
        UserID INT NOT NULL CONSTRAINT FK_PasswordResetToken_User REFERENCES [User](UserID),
        Email VARCHAR(100) NOT NULL,
        Token VARCHAR(100) NOT NULL,
        ExpiryTime DATETIME NOT NULL,
        IsUsed BIT NOT NULL DEFAULT 0,
        CreatedAt DATETIME NOT NULL DEFAULT GETDATE()
    );
END
GO

-- Index hỗ trợ tìm kiếm nhanh token hợp lệ
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_PasswordResetToken_Token')
BEGIN
    CREATE INDEX IX_PasswordResetToken_Token ON PasswordResetToken (Token, IsUsed, ExpiryTime);
END
GO
