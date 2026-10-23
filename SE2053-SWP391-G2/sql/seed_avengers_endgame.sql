-- =========================================================================
-- Thêm dữ liệu phim "Marvel Studios' Avengers: Endgame" vào cinemaDB
-- Trạng thái: Now Showing (Đang chiếu)
-- Trailer URL: https://www.youtube.com/watch?v=TcMBFSGVi1c
-- =========================================================================

USE cinemaDB;
GO

DECLARE @MovieID INT;

-- 1. Thêm hoặc cập nhật phim Avengers: Endgame
IF NOT EXISTS (SELECT 1 FROM Movie WHERE Title = N'Marvel Studios'' Avengers: Endgame')
BEGIN
    INSERT INTO Movie (
        Title, 
        Synopsis, 
        Director, 
        Cast, 
        DurationMinutes, 
        AgeRating, 
        ReleaseDate, 
        PosterURL, 
        TrailerURL, 
        Status, 
        Formats
    ) VALUES (
        N'Marvel Studios'' Avengers: Endgame',
        N'Sau sự kiện tàn khốc trong Avengers: Infinity War (2018), vũ trụ bị hủy hoại một nửa dưới tay bạo chúa Thanos. Với sự trợ giúp của các đồng minh còn sống sót, các thành viên biệt đội Avengers một lần nữa tập hợp để đảo ngược hành động của Thanos và khôi phục lại trật tự cho toàn vũ trụ.',
        N'Anthony Russo, Joe Russo',
        N'Robert Downey Jr., Chris Evans, Mark Ruffalo, Chris Hemsworth, Scarlett Johansson, Jeremy Renner, Paul Rudd, Brie Larson',
        181,
        'C13',
        '2019-04-26',
        'https://m.media-amazon.com/images/M/MV5BMTc5MDE2ODcwNV5BMl5BanBnXkFtZTgwMzI2NzQ2NzM@._V1_.jpg',
        'https://www.youtube.com/watch?v=TcMBFSGVi1c',
        'Now Showing',
        '2D, 3D, IMAX'
    );
    SET @MovieID = SCOPE_IDENTITY();
END
ELSE
BEGIN
    SELECT @MovieID = MovieID FROM Movie WHERE Title = N'Marvel Studios'' Avengers: Endgame';
    UPDATE Movie SET
        Synopsis = N'Sau sự kiện tàn khốc trong Avengers: Infinity War (2018), vũ trụ bị hủy hoại một nửa dưới tay bạo chúa Thanos. Với sự trợ giúp của các đồng minh còn sống sót, các thành viên biệt đội Avengers một lần nữa tập hợp để đảo ngược hành động của Thanos và khôi phục lại trật tự cho toàn vũ trụ.',
        Director = N'Anthony Russo, Joe Russo',
        Cast = N'Robert Downey Jr., Chris Evans, Mark Ruffalo, Chris Hemsworth, Scarlett Johansson, Jeremy Renner, Paul Rudd, Brie Larson',
        DurationMinutes = 181,
        AgeRating = 'C13',
        ReleaseDate = '2019-04-26',
        PosterURL = 'https://m.media-amazon.com/images/M/MV5BMTc5MDE2ODcwNV5BMl5BanBnXkFtZTgwMzI2NzQ2NzM@._V1_.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=TcMBFSGVi1c',
        Status = 'Now Showing',
        Formats = '2D, 3D, IMAX'
    WHERE MovieID = @MovieID;
END;

-- 2. Gán thể loại cho phim (1: Action, 2: Sci-Fi, 3: Drama)
IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MovieID AND GenreID = 1)
    INSERT INTO Movie_Genre (MovieID, GenreID) VALUES (@MovieID, 1);
IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MovieID AND GenreID = 2)
    INSERT INTO Movie_Genre (MovieID, GenreID) VALUES (@MovieID, 2);
IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MovieID AND GenreID = 3)
    INSERT INTO Movie_Genre (MovieID, GenreID) VALUES (@MovieID, 3);

-- 3. Tạo các suất chiếu sắp tới (Showtimes) để phim có thể hiển thị trong Lịch chiếu và Đặt vé online ngay
DECLARE @D1 VARCHAR(10) = CONVERT(VARCHAR(10), DATEADD(DAY, 1, GETDATE()), 120);
DECLARE @D2 VARCHAR(10) = CONVERT(VARCHAR(10), DATEADD(DAY, 2, GETDATE()), 120);
DECLARE @D3 VARCHAR(10) = CONVERT(VARCHAR(10), DATEADD(DAY, 3, GETDATE()), 120);

-- Suất 1: Ngày mai, Phòng 1 (2D), 09:30 - 12:46
IF NOT EXISTS (SELECT 1 FROM Showtime WHERE MovieID = @MovieID AND RoomID = 1 AND StartTime = CAST(@D1 + ' 09:30:00' AS DATETIME))
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@MovieID, 1, CAST(@D1 + ' 09:30:00' AS DATETIME), CAST(@D1 + ' 12:46:00' AS DATETIME), 85000, 'Active');

-- Suất 2: Ngày mai, Phòng 1 (2D), 14:00 - 17:16
IF NOT EXISTS (SELECT 1 FROM Showtime WHERE MovieID = @MovieID AND RoomID = 1 AND StartTime = CAST(@D1 + ' 14:00:00' AS DATETIME))
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@MovieID, 1, CAST(@D1 + ' 14:00:00' AS DATETIME), CAST(@D1 + ' 17:16:00' AS DATETIME), 95000, 'Active');

-- Suất 3: Ngày mai, Phòng 2 (3D), 19:30 - 22:46
IF NOT EXISTS (SELECT 1 FROM Showtime WHERE MovieID = @MovieID AND RoomID = 2 AND StartTime = CAST(@D1 + ' 19:30:00' AS DATETIME))
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@MovieID, 2, CAST(@D1 + ' 19:30:00' AS DATETIME), CAST(@D1 + ' 22:46:00' AS DATETIME), 130000, 'Active');

-- Suất 4: Ngày mốt, Phòng 2 (3D), 10:00 - 13:16
IF NOT EXISTS (SELECT 1 FROM Showtime WHERE MovieID = @MovieID AND RoomID = 2 AND StartTime = CAST(@D2 + ' 10:00:00' AS DATETIME))
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@MovieID, 2, CAST(@D2 + ' 10:00:00' AS DATETIME), CAST(@D2 + ' 13:16:00' AS DATETIME), 130000, 'Active');

-- Suất 5: Ngày mốt, Phòng 1 (2D), 18:30 - 21:46
IF NOT EXISTS (SELECT 1 FROM Showtime WHERE MovieID = @MovieID AND RoomID = 1 AND StartTime = CAST(@D2 + ' 18:30:00' AS DATETIME))
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@MovieID, 1, CAST(@D2 + ' 18:30:00' AS DATETIME), CAST(@D2 + ' 21:46:00' AS DATETIME), 95000, 'Active');

SELECT @MovieID AS MovieID, Title, Formats, Status, TrailerURL FROM Movie WHERE MovieID = @MovieID;
GO
