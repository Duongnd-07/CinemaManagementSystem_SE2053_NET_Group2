-- UC-24 / UC-08: Thêm tùy chọn hình thức bản chiếu (Phụ đề / Lồng tiếng) cho Suất chiếu (Showtime).
-- Chạy được nhiều lần không lỗi.

IF COL_LENGTH('Showtime', 'TranslationType') IS NULL
BEGIN
    ALTER TABLE Showtime ADD TranslationType NVARCHAR(20) NOT NULL
        CONSTRAINT DF_Showtime_TranslationType DEFAULT N'Phụ đề' WITH VALUES;
END
GO

-- Xóa constraint cũ nếu đã tồn tại để đảm bảo không bị lỗi mã hóa UTF-8
IF EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_Showtime_TranslationType' AND parent_object_id = OBJECT_ID('Showtime')
)
BEGIN
    ALTER TABLE Showtime DROP CONSTRAINT CK_Showtime_TranslationType;
END
GO

IF EXISTS (
    SELECT 1 FROM sys.default_constraints
    WHERE name = 'DF_Showtime_TranslationType' AND parent_object_id = OBJECT_ID('Showtime')
)
BEGIN
    ALTER TABLE Showtime DROP CONSTRAINT DF_Showtime_TranslationType;
END
GO

-- Chuẩn hóa dữ liệu về đúng tiếng Việt có dấu chuẩn Unicode
UPDATE Showtime SET TranslationType = N'Lồng tiếng' WHERE TranslationType LIKE N'%ng ti%';
UPDATE Showtime SET TranslationType = N'Phụ đề' WHERE TranslationType NOT LIKE N'Lồng tiếng';
GO

-- Tạo lại Default Constraint và Check Constraint chuẩn Unicode
ALTER TABLE Showtime ADD CONSTRAINT DF_Showtime_TranslationType DEFAULT N'Phụ đề' FOR TranslationType;
GO

ALTER TABLE Showtime ADD CONSTRAINT CK_Showtime_TranslationType
    CHECK (TranslationType IN (N'Phụ đề', N'Lồng tiếng'));
GO
