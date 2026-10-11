-- UC-19/20/21/22/26/27/29: bổ sung schema cho nghiệp vụ quầy, xử lý sự cố và dashboard.
-- Chạy sau script tạo CinemaDB của nhóm; chạy lại nhiều lần không lỗi.

-- ===== SystemConfig (UC-33 dùng chung; chỉ tạo nếu chưa có) =====
IF OBJECT_ID('SystemConfig', 'U') IS NULL
    CREATE TABLE SystemConfig (
        ConfigKey VARCHAR(50) PRIMARY KEY,
        ConfigValue NVARCHAR(200) NOT NULL,
        Description NVARCHAR(300) NULL,
        UpdatedAt DATETIME NOT NULL DEFAULT GETDATE()
    );
GO

INSERT INTO SystemConfig (ConfigKey, ConfigValue, Description)
SELECT v.ConfigKey, v.ConfigValue, v.Description
FROM (VALUES
    ('TOP_MOVIE_OCCUPANCY_PERCENT', N'70', N'UC-29: tỷ lệ lấp đầy (%) từ mức này trở lên là phim bán chạy'),
    ('LOW_MOVIE_OCCUPANCY_PERCENT', N'30', N'UC-29: tỷ lệ lấp đầy (%) dưới mức này là phim bán chậm'),
    ('BROKEN_SEAT_VOUCHER_VALUE', N'50000', N'UC-20: giá trị voucher đền bù (VND) khi không còn ghế để đổi'),
    ('BROKEN_SEAT_VOUCHER_DAYS', N'30', N'UC-20: số ngày hiệu lực của voucher đền bù'),
    ('SEAT_SURCHARGE_VIP', N'0', N'UC-19: phụ thu (VND) cho ghế VIP, cộng vào giá suất chiếu'),
    ('SEAT_SURCHARGE_SWEETBOX', N'0', N'UC-19: phụ thu (VND) cho ghế Sweetbox, cộng vào giá suất chiếu')
) v (ConfigKey, ConfigValue, Description)
WHERE NOT EXISTS (SELECT 1 FROM SystemConfig s WHERE s.ConfigKey = v.ConfigKey);
GO

-- ===== Shift (UC-22): ca làm việc tại quầy =====
IF OBJECT_ID('Shift', 'U') IS NULL
    CREATE TABLE Shift (
        ShiftID INT IDENTITY(1,1) PRIMARY KEY,
        StaffID INT NOT NULL FOREIGN KEY REFERENCES [User](UserID),
        CounterNo VARCHAR(10) NOT NULL,
        StartedAt DATETIME NOT NULL DEFAULT GETDATE(),
        EndedAt DATETIME NULL,
        -- Số liệu chốt khi kết ca; có ClosedAt nghĩa là báo cáo đã khóa
        TicketCount INT NULL,
        TicketRevenue DECIMAL(18,2) NULL,
        ConcessionRevenue DECIMAL(18,2) NULL,
        CashTotal DECIMAL(18,2) NULL,
        CardTotal DECIMAL(18,2) NULL,
        TransferTotal DECIMAL(18,2) NULL,
        RefundTotal DECIMAL(18,2) NULL,
        ClosedAt DATETIME NULL
    );
GO

-- Mỗi nhân viên chỉ có một ca đang mở
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Shift_OpenPerStaff')
    CREATE UNIQUE INDEX UX_Shift_OpenPerStaff ON Shift (StaffID) WHERE EndedAt IS NULL;
GO

-- ===== Booking (UC-19, UC-22, UC-29) =====
IF COL_LENGTH('Booking', 'Channel') IS NULL
    ALTER TABLE Booking ADD Channel VARCHAR(10) NOT NULL
        CONSTRAINT DF_Booking_Channel DEFAULT 'Online'
        CONSTRAINT CK_Booking_Channel CHECK (Channel IN ('Online', 'Counter'));
IF COL_LENGTH('Booking', 'PointsRedeemed') IS NULL
    ALTER TABLE Booking ADD PointsRedeemed INT NOT NULL CONSTRAINT DF_Booking_PointsRedeemed DEFAULT 0;
IF COL_LENGTH('Booking', 'PointsEarned') IS NULL
    ALTER TABLE Booking ADD PointsEarned INT NOT NULL CONSTRAINT DF_Booking_PointsEarned DEFAULT 0;
IF COL_LENGTH('Booking', 'ShiftID') IS NULL
    ALTER TABLE Booking ADD ShiftID INT NULL CONSTRAINT FK_Booking_Shift FOREIGN KEY REFERENCES Shift(ShiftID);
GO

-- Đơn cũ do nhân viên lập là đơn tại quầy
UPDATE Booking SET Channel = 'Counter' WHERE StaffID IS NOT NULL AND Channel = 'Online';
GO

-- Quầy thu thêm thẻ và chuyển khoản; CHECK cũ có tên tự sinh nên phải tra tên trước khi thay
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Booking_PaymentMethod')
BEGIN
    DECLARE @bookingCk SYSNAME, @bookingSql NVARCHAR(400);
    SELECT @bookingCk = name FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('Booking') AND definition LIKE '%PaymentMethod%';
    IF @bookingCk IS NOT NULL
    BEGIN
        SET @bookingSql = N'ALTER TABLE Booking DROP CONSTRAINT ' + QUOTENAME(@bookingCk);
        EXEC sp_executesql @bookingSql;
    END
    ALTER TABLE Booking ADD CONSTRAINT CK_Booking_PaymentMethod
        CHECK (PaymentMethod IN ('Cash', 'Card', 'Transfer', 'VNPay', 'SePay'));
END
GO

-- ===== Ticket (UC-19, UC-20) =====
IF COL_LENGTH('Ticket', 'ShowtimeID') IS NULL
    ALTER TABLE Ticket ADD ShowtimeID INT NULL CONSTRAINT FK_Ticket_Showtime FOREIGN KEY REFERENCES Showtime(ShowtimeID);
IF COL_LENGTH('Ticket', 'RefundAmount') IS NULL
    ALTER TABLE Ticket ADD RefundAmount DECIMAL(18,2) NULL CONSTRAINT CK_Ticket_RefundAmount CHECK (RefundAmount >= 0);
IF COL_LENGTH('Ticket', 'RefundShiftID') IS NULL
    ALTER TABLE Ticket ADD RefundShiftID INT NULL CONSTRAINT FK_Ticket_RefundShift FOREIGN KEY REFERENCES Shift(ShiftID);
GO

UPDATE t SET t.ShowtimeID = b.ShowtimeID
FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID
WHERE t.ShowtimeID IS NULL;
GO

-- Thêm trạng thái Exchanged (vé đã đổi sang ghế khác)
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Ticket_Status')
BEGIN
    DECLARE @ticketCk SYSNAME, @ticketSql NVARCHAR(400);
    SELECT @ticketCk = name FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('Ticket') AND definition LIKE '%Status%';
    IF @ticketCk IS NOT NULL
    BEGIN
        SET @ticketSql = N'ALTER TABLE Ticket DROP CONSTRAINT ' + QUOTENAME(@ticketCk);
        EXEC sp_executesql @ticketSql;
    END
    ALTER TABLE Ticket ADD CONSTRAINT CK_Ticket_Status
        CHECK (Status IN ('Upcoming', 'Watched', 'Exchanged', 'Cancelled'));
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'UQ_Ticket_Booking_Seat')
    ALTER TABLE Ticket ADD CONSTRAINT UQ_Ticket_Booking_Seat UNIQUE (BookingID, SeatID);
GO

-- GB-01/GB-02: một ghế của một suất chiếu chỉ có một vé còn hiệu lực
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Ticket_Showtime_Seat')
    CREATE UNIQUE INDEX UX_Ticket_Showtime_Seat ON Ticket (ShowtimeID, SeatID)
        WHERE Status IN ('Upcoming', 'Watched') AND ShowtimeID IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Ticket_QRCode')
    CREATE UNIQUE INDEX UX_Ticket_QRCode ON Ticket (QRCode) WHERE QRCode IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Ticket_Booking')
    CREATE INDEX IX_Ticket_Booking ON Ticket (BookingID);
GO

-- ===== Concession (UC-27) =====
IF COL_LENGTH('Concession', 'Description') IS NULL
    ALTER TABLE Concession ADD Description NVARCHAR(500) NULL;
IF COL_LENGTH('Concession', 'ImageUrl') IS NULL
    ALTER TABLE Concession ADD ImageUrl VARCHAR(500) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Concession_Price')
    ALTER TABLE Concession ADD CONSTRAINT CK_Concession_Price CHECK (Price >= 0);
GO

-- ===== Equipment_Report (UC-21, UC-26) =====
IF COL_LENGTH('Equipment_Report', 'SeatID') IS NULL
    ALTER TABLE Equipment_Report ADD SeatID INT NULL CONSTRAINT FK_EquipmentReport_Seat FOREIGN KEY REFERENCES Seat(SeatID);
GO

-- Nhật ký xử lý sự cố: ai đổi trạng thái gì, lúc nào
IF OBJECT_ID('EquipmentReportLog', 'U') IS NULL
    CREATE TABLE EquipmentReportLog (
        LogID INT IDENTITY(1,1) PRIMARY KEY,
        ReportID INT NOT NULL FOREIGN KEY REFERENCES Equipment_Report(ReportID),
        ManagerID INT NOT NULL FOREIGN KEY REFERENCES [User](UserID),
        OldStatus VARCHAR(20) NULL,
        NewStatus VARCHAR(20) NOT NULL,
        Note NVARCHAR(500) NULL,
        CreatedAt DATETIME NOT NULL DEFAULT GETDATE()
    );
GO

-- ===== Index cho truy vấn của POS, báo cáo ca và dashboard =====
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Showtime_Room_Start')
    CREATE INDEX IX_Showtime_Room_Start ON Showtime (RoomID, StartTime);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Booking_Staff_Time')
    CREATE INDEX IX_Booking_Staff_Time ON Booking (StaffID, BookingTime);
GO
