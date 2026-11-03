-- UC-35: Inspect & Clean Screening Room - mỗi suất chiếu có tối đa một lần kiểm tra phòng (BR-35-01).
-- Không chặn bán vé; chỉ dùng để cảnh báo và cho Manager xem lịch sử.
-- Bỏ đánh dấu (hoàn tác trong 5 phút) là xóa dòng kiểm tra; báo cáo sự cố đã gửi (UC-21) vẫn giữ nguyên.
-- Chạy sau UC19-29_staff_ops.sql (cần bảng Shift); chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
GO

IF OBJECT_ID('RoomInspection', 'U') IS NULL
    CREATE TABLE RoomInspection (
        InspectionID INT IDENTITY(1,1) PRIMARY KEY,
        ShowtimeID INT NOT NULL CONSTRAINT FK_RoomInspection_Showtime FOREIGN KEY REFERENCES Showtime(ShowtimeID),
        StaffID INT NOT NULL CONSTRAINT FK_RoomInspection_Staff FOREIGN KEY REFERENCES [User](UserID),
        -- Ca đang mở của nhân viên lúc kiểm tra (không bắt buộc, BR-35-06)
        ShiftID INT NULL CONSTRAINT FK_RoomInspection_Shift FOREIGN KEY REFERENCES Shift(ShiftID),
        SeatsChecked BIT NOT NULL,
        RoomCleaned BIT NOT NULL,
        EquipmentOk BIT NOT NULL,
        -- Có ít nhất một báo cáo sự cố gửi trong lúc kiểm tra (BR-35-05)
        HasIssue BIT NOT NULL CONSTRAINT DF_RoomInspection_HasIssue DEFAULT 0,
        Note NVARCHAR(500) NULL,
        -- Lưu sau giờ bắt đầu suất chiếu (BR-35-02)
        IsLate BIT NOT NULL CONSTRAINT DF_RoomInspection_IsLate DEFAULT 0,
        InspectedAt DATETIME NOT NULL CONSTRAINT DF_RoomInspection_InspectedAt DEFAULT GETDATE(),
        UpdatedAt DATETIME NULL
    );
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_RoomInspection_Showtime')
    CREATE UNIQUE INDEX UX_RoomInspection_Showtime ON RoomInspection (ShowtimeID);
GO

-- Báo cáo sự cố (UC-21) gắn với lần kiểm tra
IF OBJECT_ID('RoomInspectionReport', 'U') IS NULL
    CREATE TABLE RoomInspectionReport (
        InspectionID INT NOT NULL CONSTRAINT FK_RoomInspectionReport_Inspection
            FOREIGN KEY REFERENCES RoomInspection(InspectionID) ON DELETE CASCADE,
        ReportID INT NOT NULL CONSTRAINT FK_RoomInspectionReport_Report FOREIGN KEY REFERENCES Equipment_Report(ReportID),
        CONSTRAINT PK_RoomInspectionReport PRIMARY KEY (InspectionID, ReportID)
    );
GO
