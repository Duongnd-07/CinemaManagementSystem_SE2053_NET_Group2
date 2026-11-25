-- UC-36 (A7): soát vé tại cửa phòng chiếu - mỗi vé chỉ được vào cửa một lần (BR-36-08, BR-36-10).
-- UNIQUE (TicketID) chặn ở tầng DB: hai cửa quét cùng một vé (bản gốc và bản photo) cùng lúc thì chỉ một lượt ghi được.
-- PrintNo là lần in của vé giấy đã quét (1 = in lần đầu, vé điện tử hoặc vé bán tại quầy). Không đổi Ticket.Status:
-- vé vẫn chỉ thành Watched khi suất chiếu kết thúc như trước. Soát cửa không cần mở ca (BR-36-07) nên ShiftID
-- được để trống; nhân viên đang có ca thì ghi kèm ca.
-- Chạy sau UC36_ticket_print.sql; chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
GO

IF OBJECT_ID('TicketCheckIn', 'U') IS NULL
    CREATE TABLE TicketCheckIn (
        CheckInID INT IDENTITY(1,1) PRIMARY KEY,
        TicketID INT NOT NULL CONSTRAINT FK_TicketCheckIn_Ticket FOREIGN KEY REFERENCES Ticket(TicketID),
        PrintNo INT NOT NULL CONSTRAINT CK_TicketCheckIn_PrintNo CHECK (PrintNo >= 1),
        StaffID INT NOT NULL CONSTRAINT FK_TicketCheckIn_Staff FOREIGN KEY REFERENCES [User](UserID),
        ShiftID INT NULL CONSTRAINT FK_TicketCheckIn_Shift FOREIGN KEY REFERENCES Shift(ShiftID),
        CheckedInAt DATETIME NOT NULL CONSTRAINT DF_TicketCheckIn_CheckedInAt DEFAULT GETDATE(),
        CONSTRAINT UQ_TicketCheckIn_Ticket UNIQUE (TicketID)
    );
GO

-- DB đã tạo bảng bằng bản cũ (ShiftID NOT NULL): bỏ khóa ngoại, cho phép NULL rồi tạo lại khóa ngoại
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('TicketCheckIn') AND name = 'ShiftID' AND is_nullable = 0)
BEGIN
    IF OBJECT_ID('FK_TicketCheckIn_Shift', 'F') IS NOT NULL
        ALTER TABLE TicketCheckIn DROP CONSTRAINT FK_TicketCheckIn_Shift;
    ALTER TABLE TicketCheckIn ALTER COLUMN ShiftID INT NULL;
    ALTER TABLE TicketCheckIn ADD CONSTRAINT FK_TicketCheckIn_Shift FOREIGN KEY (ShiftID) REFERENCES Shift(ShiftID);
END
GO
