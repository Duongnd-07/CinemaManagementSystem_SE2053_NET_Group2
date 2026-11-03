-- UC-23 Manage Movies: vòng đời phim theo ngày + thông tin chi tiết. Chạy sau UC23_add_movie_formats.sql; chạy lại nhiều lần không lỗi.
-- 1. EndDate: ngày kết thúc chiếu (NULL = chưa xác định). JOB-03 chuyển phim sang Ended khi đã qua ngày này,
--    UC-24 chặn tạo suất chiếu sau ngày này.
-- 2. Country / Language / Distributor: quốc gia, ngôn ngữ gốc, nhà phát hành (hiển thị ở trang chi tiết phim).
-- 3. Bỏ UNIQUE(Title), thay bằng UNIQUE(Title, năm khởi chiếu) để thêm được phim làm lại trùng tên
--    (vd "Vua Sư Tử" 1994 và 2019).
IF COL_LENGTH('Movie', 'EndDate') IS NULL
    ALTER TABLE Movie ADD EndDate DATE NULL;
IF COL_LENGTH('Movie', 'Country') IS NULL
    ALTER TABLE Movie ADD Country NVARCHAR(100) NULL;
IF COL_LENGTH('Movie', 'Language') IS NULL
    ALTER TABLE Movie ADD [Language] NVARCHAR(100) NULL;
IF COL_LENGTH('Movie', 'Distributor') IS NULL
    ALTER TABLE Movie ADD Distributor NVARCHAR(200) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Movie_EndDate' AND parent_object_id = OBJECT_ID('Movie'))
    ALTER TABLE Movie ADD CONSTRAINT CK_Movie_EndDate CHECK (EndDate IS NULL OR EndDate >= ReleaseDate);
GO

-- Ràng buộc UNIQUE trên Title do cinemaDB.sql tạo có tên tự sinh (UQ__Movie__...), nên tìm theo cột
DECLARE @constraint SYSNAME;
SELECT @constraint = kc.name
FROM sys.key_constraints kc
JOIN sys.index_columns ic ON ic.object_id = kc.parent_object_id AND ic.index_id = kc.unique_index_id
JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE kc.parent_object_id = OBJECT_ID('Movie') AND kc.type = 'UQ' AND c.name = 'Title'
  AND (SELECT COUNT(*) FROM sys.index_columns x
       WHERE x.object_id = kc.parent_object_id AND x.index_id = kc.unique_index_id) = 1;
IF @constraint IS NOT NULL
BEGIN
    DECLARE @sql NVARCHAR(400) = N'ALTER TABLE Movie DROP CONSTRAINT ' + QUOTENAME(@constraint);
    EXEC (@sql);
END
GO

-- SQL Server không cho index trên biểu thức, nên dùng cột tính toán PERSISTED cho năm khởi chiếu
IF COL_LENGTH('Movie', 'ReleaseYear') IS NULL
    ALTER TABLE Movie ADD ReleaseYear AS YEAR(ReleaseDate) PERSISTED;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Movie_Title_ReleaseYear' AND object_id = OBJECT_ID('Movie'))
    CREATE UNIQUE INDEX UX_Movie_Title_ReleaseYear ON Movie (Title, ReleaseYear);
GO
