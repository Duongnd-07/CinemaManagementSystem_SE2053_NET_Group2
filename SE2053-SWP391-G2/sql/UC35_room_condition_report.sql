-- UC-35: Report Room Condition Before Shift
-- Nhân viên kiểm tra phòng chiếu trước khi mở ca, mỗi hạng mục (enum RoomCheckItem) một dòng Đạt/Không đạt.
-- Không gắn ShiftID vì báo cáo được lập khi ca chưa tồn tại. Không sửa/xóa báo cáo sau khi gửi.
-- Chạy sau UC19-29_staff_ops.sql; chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID('RoomConditionReport', 'U') IS NULL
    CREATE TABLE RoomConditionReport (
        ReportID INT IDENTITY(1,1) PRIMARY KEY,
        RoomID INT NOT NULL CONSTRAINT FK_RoomCondition_Room FOREIGN KEY REFERENCES Room(RoomID),
        StaffID INT NOT NULL CONSTRAINT FK_RoomCondition_Staff FOREIGN KEY REFERENCES [User](UserID),
        Result VARCHAR(10) NOT NULL CONSTRAINT CK_RoomCondition_Result CHECK (Result IN ('Good', 'Issue')),
        Note NVARCHAR(500) NULL,
        CreatedAt DATETIME NOT NULL CONSTRAINT DF_RoomCondition_CreatedAt DEFAULT GETDATE()
    );
GO

IF OBJECT_ID('RoomConditionItem', 'U') IS NULL
    CREATE TABLE RoomConditionItem (
        ReportID INT NOT NULL CONSTRAINT FK_RoomConditionItem_Report FOREIGN KEY REFERENCES RoomConditionReport(ReportID),
        ItemKey VARCHAR(30) NOT NULL,
        IsOk BIT NOT NULL,
        Note NVARCHAR(200) NULL,
        CONSTRAINT PK_RoomConditionItem PRIMARY KEY (ReportID, ItemKey)
    );
GO

-- Trang quản lý lọc theo ngày, mới nhất trước
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_RoomCondition_CreatedAt')
    CREATE INDEX IX_RoomCondition_CreatedAt ON RoomConditionReport (CreatedAt DESC, ReportID DESC);
GO

-- Chặn một nhân viên báo trùng cùng phòng cho cùng một ca sắp tới
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_RoomCondition_Staff_Room')
    CREATE INDEX IX_RoomCondition_Staff_Room ON RoomConditionReport (StaffID, RoomID, CreatedAt);
GO
