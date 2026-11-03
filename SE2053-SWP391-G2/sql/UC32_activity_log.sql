-- UC-32: View System Logs
-- Bảng nhật ký hoạt động hệ thống (System Activity Log). Chỉ INSERT từ code, không có chức năng sửa/xóa (BR-05).
-- Lưu sẵn tên/email/vai trò của người thao tác tại thời điểm ghi để log không đổi khi tài khoản đổi tên.
-- Không có FK tới [User]: đăng nhập sai không có UserID và log phải giữ lại kể cả khi tài khoản bị xóa.
-- Chạy được nhiều lần không lỗi.

IF OBJECT_ID('ActivityLog', 'U') IS NULL
    CREATE TABLE ActivityLog (
        LogID BIGINT IDENTITY(1,1) PRIMARY KEY,
        CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),
        LogLevel VARCHAR(10) NOT NULL CHECK (LogLevel IN ('INFO', 'WARNING', 'ERROR')),
        EventType VARCHAR(20) NOT NULL
            CHECK (EventType IN ('AUTH', 'ACCOUNT', 'CONFIG', 'CATALOG', 'BOOKING', 'COUNTER', 'SYSTEM')),
        Action NVARCHAR(100) NOT NULL,
        UserID INT NULL,
        ActorName NVARCHAR(100) NULL,
        ActorEmail VARCHAR(100) NULL,
        ActorRole VARCHAR(20) NULL,
        TargetType VARCHAR(30) NULL,
        TargetID VARCHAR(50) NULL,
        Description NVARCHAR(1000) NULL,
        IpAddress VARCHAR(45) NULL
    );
GO

-- Trang xem log luôn lọc theo khoảng ngày và sắp xếp mới nhất trước
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ActivityLog_CreatedAt' AND object_id = OBJECT_ID('ActivityLog'))
    CREATE INDEX IX_ActivityLog_CreatedAt ON ActivityLog (CreatedAt DESC, LogID DESC);
GO
