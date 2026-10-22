-- Customer-facing support schema for UC-11, UC-12, UC-15, UC-16, and UC-18.
-- Run after UC19-29_staff_ops.sql and UC28_vouchers.sql; safe to re-run.

IF OBJECT_ID('CustomerNotification', 'U') IS NULL
BEGIN
    CREATE TABLE CustomerNotification (
        NotificationID INT IDENTITY(1,1) PRIMARY KEY,
        UserID INT NOT NULL CONSTRAINT FK_CustomerNotification_User
            REFERENCES [User](UserID),
        Title NVARCHAR(150) NOT NULL,
        Message NVARCHAR(1000) NOT NULL,
        NotificationType VARCHAR(30) NOT NULL,
        CreatedAt DATETIME NOT NULL CONSTRAINT DF_CustomerNotification_CreatedAt DEFAULT GETDATE(),
        ReadAt DATETIME NULL
    );
END
GO

IF OBJECT_ID('BookingReminderEmail', 'U') IS NULL
BEGIN
    CREATE TABLE BookingReminderEmail (
        BookingID INT NOT NULL PRIMARY KEY
            CONSTRAINT FK_BookingReminderEmail_Booking REFERENCES Booking(BookingID),
        AttemptCount TINYINT NOT NULL CONSTRAINT DF_BookingReminderEmail_AttemptCount DEFAULT 0,
        NextAttemptAt DATETIME NOT NULL CONSTRAINT DF_BookingReminderEmail_NextAttemptAt DEFAULT GETDATE(),
        LastAttemptAt DATETIME NULL,
        SentAt DATETIME NULL,
        LastError NVARCHAR(1000) NULL,
        CONSTRAINT CK_BookingReminderEmail_AttemptCount CHECK (AttemptCount BETWEEN 0 AND 3)
    );
END
GO

IF OBJECT_ID('LoyaltyPointHistory', 'U') IS NULL
BEGIN
    CREATE TABLE LoyaltyPointHistory (
        LoyaltyPointHistoryID INT IDENTITY(1,1) PRIMARY KEY,
        UserID INT NOT NULL CONSTRAINT FK_LoyaltyPointHistory_User
            REFERENCES [User](UserID),
        BookingID INT NULL CONSTRAINT FK_LoyaltyPointHistory_Booking
            REFERENCES Booking(BookingID),
        TransactionType VARCHAR(20) NOT NULL
            CONSTRAINT CK_LoyaltyPointHistory_Type CHECK (TransactionType IN ('Earned', 'Redeemed', 'Adjustment')),
        PointsChange INT NOT NULL CONSTRAINT CK_LoyaltyPointHistory_Points CHECK (PointsChange <> 0),
        Description NVARCHAR(200) NULL,
        CreatedAt DATETIME NOT NULL CONSTRAINT DF_LoyaltyPointHistory_CreatedAt DEFAULT GETDATE()
    );
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_LoyaltyPointHistory_Booking_Earned'
        AND object_id = OBJECT_ID('LoyaltyPointHistory')
)
    CREATE UNIQUE INDEX UX_LoyaltyPointHistory_Booking_Earned
        ON LoyaltyPointHistory (BookingID, TransactionType)
        WHERE BookingID IS NOT NULL AND TransactionType = 'Earned';
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_LoyaltyPointHistory_Booking_Redeemed'
        AND object_id = OBJECT_ID('LoyaltyPointHistory')
)
    CREATE UNIQUE INDEX UX_LoyaltyPointHistory_Booking_Redeemed
        ON LoyaltyPointHistory (BookingID, TransactionType)
        WHERE BookingID IS NOT NULL AND TransactionType = 'Redeemed';
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_LoyaltyPointHistory_User_CreatedAt'
        AND object_id = OBJECT_ID('LoyaltyPointHistory')
)
    CREATE INDEX IX_LoyaltyPointHistory_User_CreatedAt
        ON LoyaltyPointHistory (UserID, CreatedAt DESC, LoyaltyPointHistoryID DESC)
        INCLUDE (BookingID, TransactionType, PointsChange);
GO

CREATE OR ALTER TRIGGER TR_LoyaltyPointHistory_AppendOnly
ON LoyaltyPointHistory
INSTEAD OF UPDATE, DELETE
AS
BEGIN
    THROW 51001, 'LoyaltyPointHistory is append-only.', 1;
END;
GO

INSERT INTO LoyaltyPointHistory (UserID, BookingID, TransactionType, PointsChange, Description, CreatedAt)
SELECT b.CustomerID, b.BookingID, 'Earned', b.PointsEarned, N'Điểm từ đơn vé đã thanh toán', b.BookingTime
FROM Booking b
WHERE b.CustomerID IS NOT NULL AND b.Status = 'Paid' AND b.PointsEarned > 0
  AND NOT EXISTS (
      SELECT 1 FROM LoyaltyPointHistory h
      WHERE h.BookingID = b.BookingID AND h.TransactionType = 'Earned'
  );

INSERT INTO LoyaltyPointHistory (UserID, BookingID, TransactionType, PointsChange, Description, CreatedAt)
SELECT b.CustomerID, b.BookingID, 'Redeemed', -b.PointsRedeemed, N'Điểm sử dụng cho đơn vé đã thanh toán', b.BookingTime
FROM Booking b
WHERE b.CustomerID IS NOT NULL AND b.Status = 'Paid' AND b.PointsRedeemed > 0
  AND NOT EXISTS (
      SELECT 1 FROM LoyaltyPointHistory h
      WHERE h.BookingID = b.BookingID AND h.TransactionType = 'Redeemed'
  );
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_CustomerNotification_User_CreatedAt'
        AND object_id = OBJECT_ID('CustomerNotification')
)
    CREATE INDEX IX_CustomerNotification_User_CreatedAt
        ON CustomerNotification (UserID, CreatedAt DESC, NotificationID DESC)
        INCLUDE (ReadAt);
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_Booking_Customer_BookingTime'
        AND object_id = OBJECT_ID('Booking')
)
    CREATE INDEX IX_Booking_Customer_BookingTime
        ON Booking (CustomerID, BookingTime DESC, BookingID DESC);
GO

INSERT INTO SystemConfig (ConfigKey, ConfigValue, Description)
SELECT 'CANCELLATION_VOUCHER_DAYS', N'30',
       N'UC-16: số ngày hiệu lực của voucher hoàn 80% giá vé'
WHERE NOT EXISTS (
    SELECT 1 FROM SystemConfig WHERE ConfigKey = 'CANCELLATION_VOUCHER_DAYS'
);
GO
