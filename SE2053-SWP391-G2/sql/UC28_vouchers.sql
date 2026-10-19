-- UC-28: promotion vouchers and the UC-16 80% refund-voucher inventory.
-- Run after the core cinemaDB schema; safe to re-run.

IF OBJECT_ID('Voucher', 'U') IS NULL
BEGIN
    CREATE TABLE Voucher (
        VoucherID INT IDENTITY(1,1) PRIMARY KEY,
        VoucherCode VARCHAR(32) NOT NULL,
        DiscountValue DECIMAL(18,2) NOT NULL,
        DiscountType VARCHAR(20) NOT NULL,
        CreatedAt DATETIME NOT NULL CONSTRAINT DF_Voucher_CreatedAt DEFAULT GETDATE(),
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
IF COL_LENGTH('Voucher', 'CreatedAt') IS NULL
    ALTER TABLE Voucher ADD CreatedAt DATETIME NOT NULL
        CONSTRAINT DF_Voucher_CreatedAt DEFAULT GETDATE() WITH VALUES;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'FK_Voucher_CreatedBy' AND parent_object_id = OBJECT_ID('Voucher')
)
    ALTER TABLE Voucher ADD CONSTRAINT FK_Voucher_CreatedBy
        FOREIGN KEY (CreatedByUserID) REFERENCES [User](UserID);

-- The core schema uses 'Fixed Amount'; UC-28 uses the shorter 'Amount'.
DECLARE @discountCheck SYSNAME, @discountSql NVARCHAR(400);
SELECT TOP (1) @discountCheck = name
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID('Voucher')
  AND definition LIKE '%DiscountType%'
  AND name <> 'CK_Voucher_DiscountType';
IF @discountCheck IS NOT NULL
BEGIN
    SET @discountSql = N'ALTER TABLE Voucher DROP CONSTRAINT ' + QUOTENAME(@discountCheck);
    EXEC sp_executesql @discountSql;
END

UPDATE Voucher SET DiscountType = 'Amount' WHERE DiscountType = 'Fixed Amount';

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_Voucher_DiscountType' AND parent_object_id = OBJECT_ID('Voucher')
)
    ALTER TABLE Voucher ADD CONSTRAINT CK_Voucher_DiscountType
        CHECK (DiscountType IN ('Amount', 'Percent'));
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_Voucher_DiscountValue' AND parent_object_id = OBJECT_ID('Voucher')
)
    ALTER TABLE Voucher ADD CONSTRAINT CK_Voucher_DiscountValue CHECK (DiscountValue > 0);
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_Voucher_Type' AND parent_object_id = OBJECT_ID('Voucher')
)
    ALTER TABLE Voucher ADD CONSTRAINT CK_Voucher_Type CHECK (VoucherType IN ('Promotion', 'Refund80'));
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_Voucher_MinOrder' AND parent_object_id = OBJECT_ID('Voucher')
)
    ALTER TABLE Voucher ADD CONSTRAINT CK_Voucher_MinOrder CHECK (MinOrderAmount >= 0);
IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes i
    JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
    JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
    WHERE i.object_id = OBJECT_ID('Voucher')
      AND i.is_unique = 1
      AND i.has_filter = 0
      AND ic.key_ordinal = 1
      AND c.name = 'VoucherCode'
      AND NOT EXISTS (
          SELECT 1 FROM sys.index_columns extra
          WHERE extra.object_id = i.object_id
            AND extra.index_id = i.index_id
            AND extra.key_ordinal > 1
      )
)
    CREATE UNIQUE INDEX UX_Voucher_Code ON Voucher (VoucherCode);
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes WHERE name = 'IX_Voucher_Owner_Status' AND object_id = OBJECT_ID('Voucher')
)
    CREATE INDEX IX_Voucher_Owner_Status ON Voucher (OwnerUserID, Status, ExpiryDate);
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes WHERE name = 'IX_Voucher_Type_Status' AND object_id = OBJECT_ID('Voucher')
)
    CREATE INDEX IX_Voucher_Type_Status ON Voucher (VoucherType, Status, ExpiryDate);
GO
