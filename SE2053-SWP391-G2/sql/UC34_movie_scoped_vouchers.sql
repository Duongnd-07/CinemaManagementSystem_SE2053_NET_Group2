-- UC-34: allow a promotion voucher to target one movie; NULL MovieID applies to every movie.
-- Run after UC28_vouchers.sql; safe to re-run.

IF COL_LENGTH('Voucher', 'MovieID') IS NULL
    ALTER TABLE Voucher ADD MovieID INT NULL;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_Voucher_Movie'
      AND parent_object_id = OBJECT_ID('Voucher')
)
    ALTER TABLE Voucher ADD CONSTRAINT FK_Voucher_Movie
        FOREIGN KEY (MovieID) REFERENCES Movie(MovieID);
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_Voucher_Movie_Status'
      AND object_id = OBJECT_ID('Voucher')
)
    CREATE INDEX IX_Voucher_Movie_Status ON Voucher (MovieID, Status, ExpiryDate);
GO
