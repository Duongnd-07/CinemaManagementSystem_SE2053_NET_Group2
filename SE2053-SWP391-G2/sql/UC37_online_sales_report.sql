-- UC-37. Apply after UC14_vnpay.sql and UC20_ticket_exchange.sql, before deploying the updated WAR.
-- PaidAt / cancellation timestamps below are Vietnam local time. OnlinePayment.CreatedAt remains UTC.
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF COL_LENGTH('dbo.Booking', 'PaidAt') IS NULL
    ALTER TABLE dbo.Booking ADD PaidAt DATETIME2(0) NULL;
GO
IF OBJECT_ID('dbo.TicketCancellation', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.TicketCancellation (
        CancellationID INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        TicketID INT NOT NULL UNIQUE REFERENCES dbo.Ticket(TicketID),
        VoucherID INT NOT NULL UNIQUE REFERENCES dbo.Voucher(VoucherID),
        VoucherAmount DECIMAL(18,2) NOT NULL CHECK (VoucherAmount >= 0),
        CancelledAt DATETIME2(0) NOT NULL,
        ActorID INT NOT NULL REFERENCES dbo.[User](UserID)
    );
END;
GO
-- A legacy timestamp is trustworthy only when exactly one Paid attempt matches the settled order.
;WITH Evidence AS (
    SELECT p.BookingID, MIN(p.PaidAt) AS PaidAt
    FROM dbo.OnlinePayment p JOIN dbo.Booking b ON b.BookingID = p.BookingID
    WHERE p.Status = 'Paid' AND b.Status = 'Paid' AND b.Channel = 'Online'
    GROUP BY p.BookingID, b.FinalAmount
    HAVING COUNT(*) = 1 AND COUNT(p.PaidAt) = 1
       AND MIN(p.Amount) = b.FinalAmount AND MIN(p.ReceivedAmount) = b.FinalAmount
)
UPDATE b SET PaidAt = e.PaidAt
FROM dbo.Booking b JOIN Evidence e ON e.BookingID = b.BookingID
WHERE b.PaidAt IS NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.Booking') AND name = 'IX_Booking_Online_PaidAt')
    CREATE INDEX IX_Booking_Online_PaidAt ON dbo.Booking(Channel, Status, PaidAt) INCLUDE (ShowtimeID, FinalAmount);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.OnlinePayment') AND name = 'IX_OnlinePayment_CreatedAt')
    CREATE INDEX IX_OnlinePayment_CreatedAt ON dbo.OnlinePayment(CreatedAt, Status) INCLUDE (BookingID);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.TicketExchange') AND name = 'IX_TicketExchange_CreatedAt')
    CREATE INDEX IX_TicketExchange_CreatedAt ON dbo.TicketExchange(CreatedAt) INCLUDE (OldTicketID, CashRefund);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.TicketCancellation') AND name = 'IX_TicketCancellation_CancelledAt')
    CREATE INDEX IX_TicketCancellation_CancelledAt ON dbo.TicketCancellation(CancelledAt) INCLUDE (TicketID, VoucherAmount);
GO
