-- UC-20: Exchange Broken Seat - lịch sử đổi ghế / hoàn tiền khi ghế hỏng.
-- Chạy sau UC19-29_staff_ops.sql và UC22_shift_cash_count.sql; chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- Mỗi lần xử lý một vé trên ghế hỏng:
--   Same      : đổi sang ghế cùng hạng (luôn ưu tiên)
--   Upgrade   : ghế thường hết, nâng lên VIP miễn phí
--   Downgrade : ghế VIP hết, xuống ghế thường, trả tiền mặt chênh lệch
--   Split     : ghế đôi hết, chuyển sang 2 ghế cạnh nhau cùng loại
--   Refund    : khách chọn hủy vé (bất kỳ lúc nào), hoàn tiền mặt
-- NewTicketID2 chỉ có với Split. CashRefund/ShiftID: tiền mặt trả khách từ két (UC-22 trừ khỏi két)
IF OBJECT_ID('TicketExchange', 'U') IS NULL
    CREATE TABLE TicketExchange (
        ExchangeID INT IDENTITY(1,1) PRIMARY KEY,
        OldTicketID INT NOT NULL CONSTRAINT FK_TicketExchange_OldTicket FOREIGN KEY REFERENCES Ticket(TicketID),
        ExchangeType VARCHAR(20) NOT NULL
            CONSTRAINT CK_TicketExchange_Type CHECK (ExchangeType IN ('Same', 'Upgrade', 'Downgrade', 'Split', 'Refund')),
        NewTicketID INT NULL CONSTRAINT FK_TicketExchange_NewTicket FOREIGN KEY REFERENCES Ticket(TicketID),
        NewTicketID2 INT NULL CONSTRAINT FK_TicketExchange_NewTicket2 FOREIGN KEY REFERENCES Ticket(TicketID),
        Reason NVARCHAR(500) NOT NULL,
        CashRefund DECIMAL(18,2) NOT NULL CONSTRAINT DF_TicketExchange_CashRefund DEFAULT 0
            CONSTRAINT CK_TicketExchange_CashRefund CHECK (CashRefund >= 0),
        PointsReturned INT NOT NULL CONSTRAINT DF_TicketExchange_Points DEFAULT 0
            CONSTRAINT CK_TicketExchange_Points CHECK (PointsReturned >= 0),
        VoucherID INT NULL CONSTRAINT FK_TicketExchange_Voucher FOREIGN KEY REFERENCES Voucher(VoucherID),
        StaffID INT NOT NULL CONSTRAINT FK_TicketExchange_Staff FOREIGN KEY REFERENCES [User](UserID),
        ShiftID INT NULL CONSTRAINT FK_TicketExchange_Shift FOREIGN KEY REFERENCES Shift(ShiftID),
        CreatedAt DATETIME NOT NULL CONSTRAINT DF_TicketExchange_CreatedAt DEFAULT GETDATE()
    );
GO

-- Một vé chỉ được xử lý một lần (vé mới sinh ra sau khi đổi có thể được xử lý tiếp nếu lại hỏng)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_TicketExchange_OldTicket')
    CREATE UNIQUE INDEX UX_TicketExchange_OldTicket ON TicketExchange (OldTicketID);
GO
