-- UC-17: Rate & Review Movie.
-- Chạy sau script tạo CinemaDB của nhóm; chạy lại nhiều lần không lỗi.
-- Bảng Review đã có từ cinemaDB.sql (ReviewID, MovieID, UserID, Rating, Comment, CreatedAt,
-- UNIQUE(MovieID, UserID)). Script này bổ sung chỉnh sửa/ẩn review vi phạm (GB-07, Manager moderation).

IF COL_LENGTH('Review', 'UpdatedAt') IS NULL
    ALTER TABLE Review ADD UpdatedAt DATETIME NULL;
IF COL_LENGTH('Review', 'Status') IS NULL
    ALTER TABLE Review ADD Status VARCHAR(20) NOT NULL CONSTRAINT DF_Review_Status DEFAULT 'Visible';
IF COL_LENGTH('Review', 'HiddenReason') IS NULL
    ALTER TABLE Review ADD HiddenReason NVARCHAR(255) NULL;
IF COL_LENGTH('Review', 'HiddenAt') IS NULL
    ALTER TABLE Review ADD HiddenAt DATETIME NULL;
IF COL_LENGTH('Review', 'HiddenByUserID') IS NULL
    ALTER TABLE Review ADD HiddenByUserID INT NULL
        CONSTRAINT FK_Review_HiddenBy FOREIGN KEY REFERENCES [User](UserID);
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Review_Status')
    ALTER TABLE Review ADD CONSTRAINT CK_Review_Status CHECK (Status IN ('Visible', 'Hidden'));
GO

-- Danh sách review công khai của một phim, mới nhất trước; lọc theo Status ngay trên index
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Review_Movie_Status')
    CREATE INDEX IX_Review_Movie_Status ON Review (MovieID, Status, CreatedAt DESC);
GO
