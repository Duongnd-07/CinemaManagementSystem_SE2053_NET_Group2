-- UC-28: promotion vouchers and the UC-16 80% refund-voucher inventory.
-- Run after the core cinemaDB schema; safe to re-run.

IF OBJECT_ID('Voucher', 'U') IS NULL
BEGIN
    CREATE TABLE Voucher (
        VoucherID INT IDENTITY(1,1) PRIMARY KEY,
        VoucherCode VARCHAR(32) NOT NULL,
        DiscountValue DECIMAL(18,2) NOT NULL,
        DiscountType VARCHAR(20) NOT NULL,
        ExpiryDate DATETIME NOT NULL,
        OwnerUserID INT NULL CONSTRAINT FK_Voucher_OwnerUser REFERENCES [User](UserID),
        Status VARCHAR(20) NOT NULL CONSTRAINT DF_Voucher_Status DEFAULT 'Active',
        VoucherType VARCHAR(20) NOT NULL CONSTRAINT DF_Voucher_Type DEFAULT 'Promotion',
        MinOrderAmount DECIMAL(18,2) NOT NULL CONSTRAINT DF_Voucher_MinOrder DEFAULT 0,
        CreatedByUserID INT NULL CONSTRAINT FK_Voucher_CreatedBy REFERENCES [User](UserID),
        CONSTRAINT CK_Voucher_DiscountValue CHECK (DiscountValue > 0),
        CONSTRAINT CK_Voucher_MinOrder CHECK (MinOrderAmount >= 0),
        CONSTRAINT CK_Voucher_DiscountType CHECK (DiscountType IN ('Amount', 'Percent')),
        CONSTRAINT CK_Voucher_Type CHECK (VoucherType IN ('Promotion', 'Refund80'))
    );
END
GO

IF COL_LENGTH('Voucher', 'VoucherType') IS NULL
    ALTER TABLE Voucher ADD VoucherType VARCHAR(20) NOT NULL
        CONSTRAINT DF_Voucher_Type DEFAULT 'Promotion' WITH VALUES;
IF COL_LENGTH('Voucher', 'MinOrderAmount') IS NULL
    ALTER TABLE Voucher ADD MinOrderAmount DECIMAL(18,2) NOT NULL
        CONSTRAINT DF_Voucher_MinOrder DEFAULT 0 WITH VALUES;
IF COL_LENGTH('Voucher', 'CreatedByUserID') IS NULL
    ALTER TABLE Voucher ADD CreatedByUserID INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Voucher_CreatedBy')
    ALTER TABLE Voucher ADD CONSTRAINT FK_Voucher_CreatedBy
        FOREIGN KEY (CreatedByUserID) REFERENCES [User](UserID);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Voucher_Type')
    ALTER TABLE Voucher ADD CONSTRAINT CK_Voucher_Type CHECK (VoucherType IN ('Promotion', 'Refund80'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Voucher_MinOrder')
    ALTER TABLE Voucher ADD CONSTRAINT CK_Voucher_MinOrder CHECK (MinOrderAmount >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Voucher_Code')
    CREATE UNIQUE INDEX UX_Voucher_Code ON Voucher (VoucherCode);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Voucher_Owner_Status')
    CREATE INDEX IX_Voucher_Owner_Status ON Voucher (OwnerUserID, Status, ExpiryDate);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Voucher_Type_Status')
    CREATE INDEX IX_Voucher_Type_Status ON Voucher (VoucherType, Status, ExpiryDate);
GO
