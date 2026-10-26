-- UC-04/UC-05 View & Search Movies: chỉ mục cho trang danh sách phim công khai.
-- Lọc theo trạng thái + sắp xếp theo ngày khởi chiếu, và lọc phim theo thể loại.
-- Tìm theo tên/đạo diễn/diễn viên dùng LIKE '%...%' nên không tận dụng được chỉ mục thường.
-- Chạy sau script tạo CinemaDB của nhóm; chạy lại nhiều lần không lỗi.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Movie_Status_ReleaseDate' AND object_id = OBJECT_ID('Movie'))
    CREATE INDEX IX_Movie_Status_ReleaseDate ON Movie (Status, ReleaseDate DESC);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_MovieGenre_GenreID' AND object_id = OBJECT_ID('Movie_Genre'))
    CREATE INDEX IX_MovieGenre_GenreID ON Movie_Genre (GenreID, MovieID);
GO
