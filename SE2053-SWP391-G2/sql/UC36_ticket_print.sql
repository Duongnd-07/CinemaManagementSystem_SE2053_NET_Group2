-- UC-36: Scan E-ticket & Print Paper Ticket - mỗi lần in vé giấy là một dòng.
-- PrintNo = 1 là lần in đầu, 2, 3... là in lại (nhãn "BẢN IN LẠI (lần PrintNo - 1)"). UNIQUE (TicketID, PrintNo)
-- chặn hai nhân viên cùng in lần đầu một vé (E5). Vé bán tại quầy (UC-19) coi như đã in lúc bán nên chỉ có dòng in lại.
-- In vé không đổi trạng thái vé, không có soát cửa (BR-36-08). Nhân viên phải đang mở ca nên ShiftID bắt buộc.
-- Chạy sau UC19-29_staff_ops.sql (cần bảng Shift); chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
GO

IF OBJECT_ID('TicketPrint', 'U') IS NULL
    CREATE TABLE TicketPrint (
        PrintID INT IDENTITY(1,1) PRIMARY KEY,
        TicketID INT NOT NULL CONSTRAINT FK_TicketPrint_Ticket FOREIGN KEY REFERENCES Ticket(TicketID),
        PrintNo INT NOT NULL CONSTRAINT CK_TicketPrint_PrintNo CHECK (PrintNo >= 1),
        -- Bắt buộc khi in lại (PrintNo > 1)
        Reason NVARCHAR(200) NULL,
        StaffID INT NOT NULL CONSTRAINT FK_TicketPrint_Staff FOREIGN KEY REFERENCES [User](UserID),
        ShiftID INT NOT NULL CONSTRAINT FK_TicketPrint_Shift FOREIGN KEY REFERENCES Shift(ShiftID),
        PrintedAt DATETIME NOT NULL CONSTRAINT DF_TicketPrint_PrintedAt DEFAULT GETDATE(),
        CONSTRAINT UQ_TicketPrint_Ticket_No UNIQUE (TicketID, PrintNo),
        CONSTRAINT CK_TicketPrint_Reason CHECK (PrintNo = 1 OR Reason IS NOT NULL)
    );
GO
