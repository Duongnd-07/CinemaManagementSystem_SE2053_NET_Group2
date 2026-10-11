-- UC-23 Manage Movies: thêm cột Formats (danh sách định dạng chiếu, ngăn cách bằng dấu phẩy: 2D,3D,IMAX)
IF COL_LENGTH('Movie', 'Formats') IS NULL
    ALTER TABLE Movie ADD Formats NVARCHAR(100) NULL;
GO
