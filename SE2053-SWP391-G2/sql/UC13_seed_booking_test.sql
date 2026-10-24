-- UC-13 Book Ticket Online: dữ liệu test để đặt vé online qua trình duyệt.
-- Chạy sau cinemaDB.sql + cinemaDB_data.sql + UC13_book_ticket_online.sql; chạy lại nhiều lần không lỗi.
-- Tạo:
--   * 3 suất chiếu trong tương lai (tối nay, trưa và tối ngày mai) ở "Phòng chiếu 1" (còn ghế trống,
--     không đụng ghế giữ/vé cũ) cho 2 phim đang "Now Showing".
--   * Nạp điểm thưởng cho khách test (duongpt@gmail.com) để có thể thử đổi điểm (BR-LOYALTY-01).
-- Không tạo booking/ticket mẫu: để người test tự chọn ghế và đi hết luồng (giữ ghế -> checkout -> thanh toán demo).
USE CinemaDB;
GO

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;

DECLARE @roomId INT = (SELECT RoomID FROM Room WHERE RoomName = N'Phòng chiếu 1' AND Status = 'Active');
IF @roomId IS NULL
BEGIN
    RAISERROR(N'Không tìm thấy phòng "Phòng chiếu 1" đang Active (chạy cinemaDB.sql + cinemaDB_data.sql trước).', 16, 1);
    RETURN;
END

DECLARE @movie1 INT, @movie2 INT, @dur1 INT, @dur2 INT;
SELECT TOP 1 @movie1 = MovieID, @dur1 = DurationMinutes FROM Movie WHERE Status = 'Now Showing' ORDER BY MovieID;
SELECT TOP 1 @movie2 = MovieID, @dur2 = DurationMinutes FROM Movie WHERE Status = 'Now Showing' AND MovieID <> @movie1 ORDER BY MovieID;
IF @movie2 IS NULL SET @movie2 = @movie1;
IF @dur2 IS NULL SET @dur2 = @dur1;
IF @movie1 IS NULL
BEGIN
    RAISERROR(N'Cần ít nhất một phim Status = ''Now Showing'' (chạy cinemaDB_data.sql trước).', 16, 1);
    RETURN;
END

DECLARE @price DECIMAL(18,2) = 90000;
DECLARE @today DATE = CAST(GETDATE() AS DATE);
DECLARE @tomorrow DATE = DATEADD(DAY, 1, @today);

-- Suất 1: tối nay (chỉ tạo nếu còn cách giờ hiện tại ít nhất 1 giờ, tránh suất "sắp chiếu" gây lỗi khi test)
DECLARE @start1 DATETIME = DATEADD(HOUR, 21, CAST(@today AS DATETIME));
DECLARE @start2 DATETIME = DATEADD(HOUR, 14, CAST(@tomorrow AS DATETIME));
DECLARE @start3 DATETIME = DATEADD(HOUR, 19, CAST(@tomorrow AS DATETIME) );
SET @start3 = DATEADD(MINUTE, 30, @start3);

IF @start1 > DATEADD(HOUR, 1, GETDATE())
    AND NOT EXISTS (SELECT 1 FROM Showtime WHERE RoomID = @roomId AND StartTime = @start1)
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@movie1, @roomId, @start1, DATEADD(MINUTE, @dur1 + 15, @start1), @price, 'Active');

IF NOT EXISTS (SELECT 1 FROM Showtime WHERE RoomID = @roomId AND StartTime = @start2)
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@movie2, @roomId, @start2, DATEADD(MINUTE, @dur2 + 15, @start2), @price, 'Active');

IF NOT EXISTS (SELECT 1 FROM Showtime WHERE RoomID = @roomId AND StartTime = @start3)
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@movie1, @roomId, @start3, DATEADD(MINUTE, @dur1 + 15, @start3), @price, 'Active');

-- Nạp điểm thưởng để test đổi điểm lúc checkout (BR-LOYALTY-01: POINT_EXCHANGE_RATE VND/điểm)
UPDATE [User] SET PointBalance = 50 WHERE Email = 'duongpt@gmail.com' AND PointBalance < 50;

SELECT s.ShowtimeID, m.Title AS Movie, r.RoomName AS Room, s.StartTime, s.BasePrice,
       '/booking?id=' + CAST(s.ShowtimeID AS VARCHAR(10)) AS Url
FROM Showtime s JOIN Movie m ON m.MovieID = s.MovieID JOIN Room r ON r.RoomID = s.RoomID
WHERE s.RoomID = @roomId AND s.StartTime > GETDATE()
ORDER BY s.StartTime;
GO
