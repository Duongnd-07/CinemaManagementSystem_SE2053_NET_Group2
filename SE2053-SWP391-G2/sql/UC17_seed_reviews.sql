-- UC-17 Rate & Review Movie + View Movie Rating & Review: dữ liệu DEMO cho DB local để test.
-- Chạy SAU UC17_movie_review.sql; chạy lại nhiều lần không tạo trùng.
--
-- Script tạo:
--   * 8 khách hàng demo reviewer01..08@review.demo, mật khẩu Customer@123
--     (PasswordHash = 'hashed_pwd', LoginService tự nâng cấp sang PBKDF2 ở lần đăng nhập đầu).
--   * Mỗi phim Now Showing/Ended: dùng 1 suất chiếu đã kết thúc ở phòng có ghế; phim chưa có suất như vậy
--     (vd. phim Ended) được tạo 1 suất trong quá khứ (Status = Ended).
--   * Mỗi reviewer: 1 đơn Paid + 1 vé Watched cho suất đó (GB-07: có vé đã xem mới được đánh giá), rồi 1 review.
--   * reviewer01 có vé phim Ended nhưng CHƯA đánh giá -> test luồng viết review cho phim đã kết thúc.
--   * reviewer08: review ở phim Now Showing đầu tiên bị Manager ẩn -> test nhãn "Đã bị ẩn" + thông báo.
-- Dữ liệu demo nhận diện bằng: email @review.demo, TicketCode bắt đầu bằng 'UC17DEMO-'.
-- Lưu ý: các đơn demo là đơn Paid nên sẽ được tính vào doanh thu/báo cáo của DB local.

USE cinemaDB;
GO

-- Bảng Ticket có filtered index nên INSERT bắt buộc bật 2 tùy chọn này (SSMS bật sẵn, sqlcmd thì không)
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

-- ===== 1. Tài khoản reviewer demo =====
INSERT INTO [User] (FullName, Email, Phone, PasswordHash, Role, PointBalance, Status, DateOfBirth, Gender)
SELECT v.FullName, v.Email, v.Phone, 'hashed_pwd', 'Customer', 0, 'Active', v.DateOfBirth, v.Gender
FROM (VALUES
    (N'Nguyễn Minh Anh',   'reviewer01@review.demo', '0990017001', CAST('1999-02-14' AS DATE), 'Female'),
    (N'Trần Quốc Bảo',     'reviewer02@review.demo', '0990017002', CAST('1997-06-03' AS DATE), 'Male'),
    (N'Lê Thu Hà',         'reviewer03@review.demo', '0990017003', CAST('2001-09-21' AS DATE), 'Female'),
    (N'Phạm Đức Long',     'reviewer04@review.demo', '0990017004', CAST('1995-12-30' AS DATE), 'Male'),
    (N'Hoàng Gia Linh',    'reviewer05@review.demo', '0990017005', CAST('2003-04-08' AS DATE), 'Female'),
    (N'Võ Thanh Tùng',     'reviewer06@review.demo', '0990017006', CAST('1998-08-17' AS DATE), 'Male'),
    (N'Đặng Ngọc Mai',     'reviewer07@review.demo', '0990017007', CAST('2000-11-11' AS DATE), 'Female'),
    (N'Bùi Hải Nam',       'reviewer08@review.demo', '0990017008', CAST('1996-05-25' AS DATE), 'Male')
) v (FullName, Email, Phone, DateOfBirth, Gender)
WHERE NOT EXISTS (SELECT 1 FROM [User] u WHERE u.Email = v.Email OR u.Phone = v.Phone);

DECLARE @reviewers TABLE (ReviewerNo INT PRIMARY KEY, UserID INT NOT NULL);
INSERT INTO @reviewers (ReviewerNo, UserID)
SELECT CAST(SUBSTRING(Email, 9, 2) AS INT), UserID
FROM [User]
WHERE Email LIKE 'reviewer0[1-8]@review.demo';

-- ===== 2. Phim mục tiêu và suất chiếu đã kết thúc của từng phim =====
DECLARE @movies TABLE (
    Rn INT IDENTITY(1,1) PRIMARY KEY,
    MovieID INT NOT NULL,
    Title NVARCHAR(200) NOT NULL,
    Status VARCHAR(20) NOT NULL,
    Duration INT NOT NULL,
    ShowtimeID INT NULL,
    RoomID INT NULL,
    Price DECIMAL(18,2) NULL,
    EndTime DATETIME NULL
);
INSERT INTO @movies (MovieID, Title, Status, Duration)
SELECT MovieID, Title, Status, DurationMinutes
FROM Movie
WHERE Status IN ('Now Showing', 'Ended')
ORDER BY MovieID;

-- Ưu tiên suất demo đã tạo ở lần chạy trước, sau đó tới suất đã kết thúc gần nhất còn đủ ghế trống cho reviewer
DECLARE @reviewerCount INT = (SELECT COUNT(*) FROM @reviewers);
UPDATE m
SET ShowtimeID = x.ShowtimeID, RoomID = x.RoomID, Price = x.BasePrice, EndTime = x.EndTime
FROM @movies m
CROSS APPLY (
    SELECT TOP 1 s.ShowtimeID, s.RoomID, s.BasePrice, s.EndTime
    FROM Showtime s
    WHERE s.MovieID = m.MovieID
      AND s.EndTime <= GETDATE()
      AND (SELECT COUNT(*) FROM Seat se
           WHERE se.RoomID = s.RoomID AND se.Status = 'Active'
             AND NOT EXISTS (SELECT 1 FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID
                             WHERE b.ShowtimeID = s.ShowtimeID AND t.SeatID = se.SeatID
                               AND t.Status IN ('Upcoming', 'Watched'))) >= @reviewerCount
    ORDER BY CASE WHEN EXISTS (SELECT 1 FROM Booking b JOIN Ticket t ON t.BookingID = b.BookingID
                               WHERE b.ShowtimeID = s.ShowtimeID AND t.TicketCode LIKE 'UC17DEMO-%')
                  THEN 0 ELSE 1 END,
             s.EndTime DESC
) x;

-- Phòng còn hoạt động và có nhiều ghế nhất để tạo suất chiếu quá khứ
DECLARE @room INT = (
    SELECT TOP 1 r.RoomID
    FROM Room r
    WHERE r.Status = 'Active'
      AND EXISTS (SELECT 1 FROM Seat se WHERE se.RoomID = r.RoomID AND se.Status = 'Active')
    ORDER BY (SELECT COUNT(*) FROM Seat se WHERE se.RoomID = r.RoomID) DESC, r.RoomID
);

DECLARE @rn INT, @movieId INT, @duration INT, @start DATETIME, @end DATETIME;
DECLARE showtimeCursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT Rn, MovieID, Duration FROM @movies WHERE ShowtimeID IS NULL;
OPEN showtimeCursor;
FETCH NEXT FROM showtimeCursor INTO @rn, @movieId, @duration;
WHILE @@FETCH_STATUS = 0
BEGIN
    -- GB-08: kết thúc = bắt đầu + thời lượng + 15 phút dọn phòng; chọn ngày cách hiện tại hơn 1 tháng để không chồng suất
    SET @start = DATEADD(HOUR, 10, CAST(DATEADD(DAY, -35 - @rn, CAST(GETDATE() AS DATE)) AS DATETIME));
    SET @end = DATEADD(MINUTE, @duration + 15, @start);
    IF @room IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM Showtime WHERE RoomID = @room AND StartTime < @end AND EndTime > @start)
    BEGIN
        INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
        VALUES (@movieId, @room, @start, @end, 90000, 'Ended');
        UPDATE @movies
        SET ShowtimeID = SCOPE_IDENTITY(), RoomID = @room, Price = 90000, EndTime = @end
        WHERE Rn = @rn;
    END
    FETCH NEXT FROM showtimeCursor INTO @rn, @movieId, @duration;
END
CLOSE showtimeCursor;
DEALLOCATE showtimeCursor;

-- ===== 3. Nội dung nhận xét mẫu theo số sao =====
DECLARE @comments TABLE (Rating INT, Idx INT, Comment NVARCHAR(1000));
INSERT INTO @comments (Rating, Idx, Comment) VALUES
    (5, 0, N'Phim quá đỉnh! Hình ảnh, âm thanh đều xuất sắc, xem ở rạp đáng từng đồng.'),
    (5, 1, N'Cốt truyện cuốn từ đầu tới cuối, diễn viên nhập vai rất tốt. Chắc chắn sẽ xem lại.'),
    (5, 2, N'Một trong những phim hay nhất năm nay. Phòng chiếu sạch, ghế êm, trải nghiệm tuyệt vời.'),
    (4, 0, N'Phim hay, kỹ xảo đẹp. Đoạn giữa hơi chậm một chút nhưng cái kết rất ổn.'),
    (4, 1, N'Đáng xem, nhạc phim hay. Trừ 1 sao vì thời lượng hơi dài.'),
    (4, 2, N'Nội dung tốt, diễn xuất ổn. Đi xem cùng bạn bè rất vui.'),
    (3, 0, N'Phim ở mức trung bình, xem giải trí được nhưng không có gì quá đặc sắc.'),
    (3, 1, N'Hình ảnh đẹp nhưng kịch bản khá dễ đoán.'),
    (3, 2, N'Mở đầu hấp dẫn, càng về sau càng đuối.'),
    (2, 0, N'Hơi thất vọng so với kỳ vọng, nhiều tình tiết thiếu logic.'),
    (2, 1, N'Phim dài và lan man, nhiều đoạn có thể cắt bớt.'),
    (2, 2, N'Diễn viên chính chưa thuyết phục, cảm xúc chưa tới.'),
    (1, 0, N'Không hợp gu mình, xem được nửa phim đã thấy chán.'),
    (1, 1, N'Kịch bản rời rạc, khá phí tiền vé.'),
    (1, 2, N'Âm thanh lồng tiếng chưa khớp, khó theo dõi nội dung.');

-- ===== 4. Đơn Paid + vé Watched + review cho từng reviewer x phim =====
DECLARE @firstNowShowing INT = (SELECT MIN(Rn) FROM @movies WHERE Status = 'Now Showing' AND ShowtimeID IS NOT NULL);
DECLARE @firstEnded INT = (SELECT MIN(Rn) FROM @movies WHERE Status = 'Ended' AND ShowtimeID IS NOT NULL);
DECLARE @managerId INT = (SELECT TOP 1 UserID FROM [User] WHERE Role = 'Manager' ORDER BY UserID);

DECLARE @reviewerNo INT, @userId INT, @title NVARCHAR(200), @showtimeId INT, @roomId INT, @price DECIMAL(18,2),
        @showEnd DATETIME, @ticketCode VARCHAR(50), @seatId INT, @bookingId INT, @rating INT,
        @comment NVARCHAR(1000), @createdAt DATETIME, @hidden BIT, @reason NVARCHAR(255);

DECLARE planCursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT r.ReviewerNo, r.UserID, m.Rn, m.MovieID, m.Title, m.ShowtimeID, m.RoomID, m.Price, m.EndTime
    FROM @reviewers r CROSS JOIN @movies m
    WHERE m.ShowtimeID IS NOT NULL
    ORDER BY m.Rn, r.ReviewerNo;
OPEN planCursor;
FETCH NEXT FROM planCursor INTO @reviewerNo, @userId, @rn, @movieId, @title, @showtimeId, @roomId, @price, @showEnd;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @ticketCode = CONCAT('UC17DEMO-', @movieId, '-', @reviewerNo);

    IF NOT EXISTS (SELECT 1 FROM Ticket WHERE TicketCode = @ticketCode)
    BEGIN
        -- Ghế còn trống của suất (không có vé còn hiệu lực, không bị giữ), ưu tiên ghế thường ở hàng cuối
        SET @seatId = (
            SELECT TOP 1 se.SeatID
            FROM Seat se
            WHERE se.RoomID = @roomId AND se.Status = 'Active'
              AND NOT EXISTS (SELECT 1 FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID
                              WHERE b.ShowtimeID = @showtimeId AND t.SeatID = se.SeatID
                                AND t.Status IN ('Upcoming', 'Watched'))
              AND NOT EXISTS (SELECT 1 FROM Seat_Hold h WHERE h.ShowtimeID = @showtimeId AND h.SeatID = se.SeatID)
            ORDER BY CASE WHEN se.SeatType = 'Standard' THEN 0 ELSE 1 END, se.RowName DESC, se.ColNumber
        );
        IF @seatId IS NOT NULL
        BEGIN
            INSERT INTO Booking (CustomerID, ShowtimeID, TotalPrice, DiscountAmount, FinalAmount, PaymentMethod,
                                 BookingTime, Status, Channel, PointsRedeemed, PointsEarned)
            VALUES (@userId, @showtimeId, @price, 0, @price, 'VNPay', DATEADD(HOUR, -26, @showEnd), 'Paid',
                    'Online', 0, 0);
            SET @bookingId = SCOPE_IDENTITY();
            INSERT INTO Ticket (BookingID, SeatID, Price, TicketCode, QRCode, Status, ShowtimeID)
            VALUES (@bookingId, @seatId, @price, @ticketCode,
                    REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''), 'Watched', @showtimeId);
        END
    END

    -- reviewer01 để trống review phim Ended để tự test luồng viết đánh giá phim đã kết thúc
    IF EXISTS (SELECT 1 FROM Ticket WHERE TicketCode = @ticketCode)
       AND NOT (@reviewerNo = 1 AND @rn = ISNULL(@firstEnded, -1))
       AND NOT EXISTS (SELECT 1 FROM Review WHERE MovieID = @movieId AND UserID = @userId)
    BEGIN
        -- Phân bố điểm nghiêng về 4-5 sao, đủ cả 1-3 sao để test lọc theo số sao
        SET @rating = CHOOSE(((@reviewerNo + @rn * 3) % 10) + 1, 5, 4, 5, 3, 4, 5, 2, 4, 1, 5);
        -- reviewer06 chỉ chấm sao, không viết nhận xét
        SET @comment = CASE WHEN @reviewerNo = 6 THEN NULL
                            ELSE (SELECT Comment FROM @comments WHERE Rating = @rating AND Idx = (@reviewerNo + @rn) % 3)
                       END;
        -- Thời điểm đánh giá: sau khi suất chiếu kết thúc, không vượt quá hiện tại
        SET @createdAt = DATEADD(MINUTE, 30 + @reviewerNo * 47, @showEnd);
        IF @createdAt > GETDATE()
            SET @createdAt = DATEADD(MINUTE, -@reviewerNo * 5, GETDATE());
        SET @hidden = CASE WHEN @reviewerNo = 8 AND @rn = @firstNowShowing AND @managerId IS NOT NULL
                           THEN 1 ELSE 0 END;
        SET @reason = N'Nội dung có ngôn từ không phù hợp (dữ liệu demo).';

        IF @hidden = 1
        BEGIN
            INSERT INTO Review (MovieID, UserID, Rating, Comment, CreatedAt, Status, HiddenReason, HiddenAt,
                                HiddenByUserID)
            VALUES (@movieId, @userId, 1, N'Phim dở tệ, ai xem là *** (bình luận vi phạm - dữ liệu demo)',
                    @createdAt, 'Hidden', @reason, DATEADD(HOUR, 1, @createdAt), @managerId);
            INSERT INTO CustomerNotification (UserID, Title, Message, NotificationType)
            VALUES (@userId, N'Đánh giá của bạn đã bị ẩn',
                    CONCAT(N'Đánh giá của bạn cho phim "', @title, N'" đã bị ẩn do vi phạm. Lý do: ', @reason),
                    'Review');
        END
        ELSE
        BEGIN
            -- reviewer03 đã sửa lại đánh giá -> hiển thị "đã chỉnh sửa"
            INSERT INTO Review (MovieID, UserID, Rating, Comment, CreatedAt, UpdatedAt, Status)
            VALUES (@movieId, @userId, @rating, @comment, @createdAt,
                    CASE WHEN @reviewerNo = 3 THEN DATEADD(MINUTE, 20, @createdAt) END, 'Visible');
        END
    END

    FETCH NEXT FROM planCursor INTO @reviewerNo, @userId, @rn, @movieId, @title, @showtimeId, @roomId, @price, @showEnd;
END
CLOSE planCursor;
DEALLOCATE planCursor;

COMMIT TRANSACTION;
GO

-- ===== Kết quả =====
SELECT m.MovieID, m.Title, m.Status,
       SUM(CASE WHEN r.Status = 'Visible' THEN 1 ELSE 0 END) AS VisibleReviews,
       SUM(CASE WHEN r.Status = 'Hidden' THEN 1 ELSE 0 END) AS HiddenReviews,
       CAST(AVG(CASE WHEN r.Status = 'Visible' THEN r.Rating * 1.0 END) AS DECIMAL(3,1)) AS AvgRating
FROM Movie m
LEFT JOIN Review r ON r.MovieID = m.MovieID
WHERE m.Status IN ('Now Showing', 'Ended')
GROUP BY m.MovieID, m.Title, m.Status
ORDER BY m.MovieID;
GO

/*
-- ===== Gỡ dữ liệu demo (chỉ dùng trên DB local khi muốn làm sạch) =====
BEGIN TRANSACTION;
DECLARE @demoShowtimes TABLE (ShowtimeID INT PRIMARY KEY);
INSERT INTO @demoShowtimes
SELECT DISTINCT b.ShowtimeID FROM Booking b JOIN Ticket t ON t.BookingID = b.BookingID
WHERE t.TicketCode LIKE 'UC17DEMO-%';
DELETE FROM CustomerNotification WHERE UserID IN (SELECT UserID FROM [User] WHERE Email LIKE '%@review.demo');
DELETE FROM Review WHERE UserID IN (SELECT UserID FROM [User] WHERE Email LIKE '%@review.demo');
DELETE FROM Ticket WHERE TicketCode LIKE 'UC17DEMO-%';
DELETE FROM Booking WHERE CustomerID IN (SELECT UserID FROM [User] WHERE Email LIKE '%@review.demo')
    AND NOT EXISTS (SELECT 1 FROM Ticket t WHERE t.BookingID = Booking.BookingID);
-- Chỉ xóa suất quá khứ do script tạo (10:00, giá 90.000, Status Ended) và không còn đơn nào
DELETE s FROM Showtime s JOIN @demoShowtimes d ON d.ShowtimeID = s.ShowtimeID
WHERE s.Status = 'Ended' AND s.BasePrice = 90000 AND DATEPART(HOUR, s.StartTime) = 10
  AND DATEPART(MINUTE, s.StartTime) = 0 AND s.StartTime < DATEADD(DAY, -30, GETDATE())
  AND NOT EXISTS (SELECT 1 FROM Booking b WHERE b.ShowtimeID = s.ShowtimeID);
DELETE FROM [User] WHERE Email LIKE '%@review.demo'
    AND NOT EXISTS (SELECT 1 FROM Booking b WHERE b.CustomerID = [User].UserID);
COMMIT TRANSACTION;
*/
