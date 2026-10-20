/*
 * UC-14: persist VNPay attempts so signed callbacks can be matched, amount-checked,
 * and processed idempotently with booking settlement.
 */
IF OBJECT_ID('dbo.OnlinePayment', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.OnlinePayment (
        OnlinePaymentID INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        BookingID INT NOT NULL,
        Provider VARCHAR(20) NOT NULL,
        ProviderTxnRef VARCHAR(100) NOT NULL,
        ProviderTransactionID VARCHAR(100) NULL,
        Amount DECIMAL(18,2) NOT NULL,
        ReceivedAmount DECIMAL(18,2) NULL,
        Status VARCHAR(20) NOT NULL,
        ResponseCode VARCHAR(10) NULL,
        CreatedAt DATETIME2(0) NOT NULL CONSTRAINT DF_OnlinePayment_CreatedAt DEFAULT SYSUTCDATETIME(),
        PaidAt DATETIME2(0) NULL,
        CONSTRAINT FK_OnlinePayment_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID),
        CONSTRAINT CK_OnlinePayment_Provider CHECK (Provider IN ('VNPay')),
        CONSTRAINT CK_OnlinePayment_Amount CHECK (Amount > 0),
        CONSTRAINT CK_OnlinePayment_Status
            CHECK (Status IN ('Initiated', 'Paid', 'Failed', 'Expired', 'ReviewRequired'))
    );

END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_OnlinePayment_ProviderTxnRef'
              AND object_id = OBJECT_ID('dbo.OnlinePayment'))
BEGIN
    CREATE UNIQUE INDEX UX_OnlinePayment_ProviderTxnRef
        ON dbo.OnlinePayment (Provider, ProviderTxnRef);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_OnlinePayment_ProviderTransactionID'
              AND object_id = OBJECT_ID('dbo.OnlinePayment'))
BEGIN
    CREATE UNIQUE INDEX UX_OnlinePayment_ProviderTransactionID
        ON dbo.OnlinePayment (Provider, ProviderTransactionID)
        WHERE ProviderTransactionID IS NOT NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_OnlinePayment_Booking_Status'
              AND object_id = OBJECT_ID('dbo.OnlinePayment'))
BEGIN
    CREATE INDEX IX_OnlinePayment_Booking_Status
        ON dbo.OnlinePayment (BookingID, Status);
END;
