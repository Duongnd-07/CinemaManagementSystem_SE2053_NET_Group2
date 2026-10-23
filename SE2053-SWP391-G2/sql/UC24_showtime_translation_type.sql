-- UC-24 / UC-08: Thêm tùy chọn hình thức bản chiếu (Phụ đề / Lồng tiếng) cho Suất chiếu (Showtime).
-- Chạy được nhiều lần không lỗi.

IF COL_LENGTH('Showtime', 'TranslationType') IS NULL
BEGIN
    ALTER TABLE Showtime ADD TranslationType NVARCHAR(20) NOT NULL
        CONSTRAINT DF_Showtime_TranslationType DEFAULT N'Phụ đề' WITH VALUES;
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_Showtime_TranslationType' AND parent_object_id = OBJECT_ID('Showtime')
)
BEGIN
    ALTER TABLE Showtime ADD CONSTRAINT CK_Showtime_TranslationType
        CHECK (TranslationType IN (N'Phụ đề', N'Lồng tiếng'));
END
GO

-- Cập nhật thử một số suất của Avengers: Endgame thành Lồng tiếng để kiểm thử trực quan
UPDATE Showtime SET TranslationType = N'Lồng tiếng' WHERE ShowtimeID IN (6, 9);
UPDATE Showtime SET TranslationType = N'Phụ đề' WHERE ShowtimeID NOT IN (6, 9);
GO
