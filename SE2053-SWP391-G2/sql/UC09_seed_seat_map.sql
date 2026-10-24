-- UC-09 View Real-Time Seat Map: dữ liệu test sơ đồ ghế.
-- Chạy sau cinemaDB.sql + cinemaDB_data.sql (UC19-29_staff_ops.sql có hoặc không đều được); chạy lại nhiều lần không lỗi.
-- Tạo:
--   * Ma trận ghế A-H x 1-10 cho "Phòng chiếu 1" (A-C Standard, D-G VIP, C5 bảo trì; hàng H là 5 ghế đôi
--     Sweetbox H1, H3, H5, H7, H9 - mỗi ghế đôi chiếm 2 cột theo UC-25), chỉ thêm ghế còn thiếu
--   * Phòng "Phòng test UC-09 (bảo trì)" ở trạng thái Maintenance
--   * 3 suất chiếu ngày mai:
--       1) 19:00 Phòng chiếu 1  -> đủ 4 trạng thái: giữ tạm 10 phút (B4-B6), đã bán (F4-F6, ghế đôi H5), bảo trì (C5, H3)
--       2) ngay sau suất 1      -> đã bán hết ghế (E1)
--       3) 19:00 phòng bảo trì  -> bị chặn xem sơ đồ (E2)
-- Mỗi lần chạy lại, ghế giữ tạm của suất 1 được làm mới thêm 10 phút.
USE cinemaDB;
GO

SET NOCOUNT ON;
-- Ticket có filtered index (UC19-29) nên phải bật khi ghi, sqlcmd mặc định tắt
SET QUOTED_IDENTIFIER ON;

DECLARE @roomId INT = (SELECT RoomID FROM Room WHERE RoomName = N'Phòng chiếu 1');
DECLARE @movieId INT = (SELECT TOP 1 MovieID FROM Movie WHERE Status IN ('Now Showing', 'Coming Soon') ORDER BY MovieID);
DECLARE @customerId INT = (SELECT UserID FROM [User] WHERE Email = 'duongpt@gmail.com');
DECLARE @price DECIMAL(18,2) = 90000;

IF @roomId IS NULL OR @movieId IS NULL
BEGIN
    RAISERROR(N'Cần có phòng "Phòng chiếu 1" và ít nhất một phim Now Showing/Coming Soon (chạy cinemaDB_data.sql trước).', 16, 1);
    RETURN;
END

-- ===== Ghế Phòng chiếu 1 =====
INSERT INTO Seat (RoomID, RowName, ColNumber, SeatType, Status)
SELECT @roomId, r.RowName, c.n, r.SeatType,
       CASE WHEN r.RowName = 'C' AND c.n = 5 THEN 'Maintenance' ELSE 'Active' END
FROM (VALUES ('A', 'Standard'), ('B', 'Standard'), ('C', 'Standard'), ('D', 'VIP'),
             ('E', 'VIP'), ('F', 'VIP'), ('G', 'VIP'), ('H', 'Sweetbox')) r (RowName, SeatType)
CROSS JOIN (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10)) c (n)
-- Ghế đôi chiếm cột n và n+1 nên chỉ đặt ở cột lẻ
WHERE (r.SeatType <> 'Sweetbox' OR c.n % 2 = 1)
  AND NOT EXISTS (SELECT 1 FROM Seat s WHERE s.RoomID = @roomId AND s.RowName = r.RowName AND s.ColNumber = c.n);

-- Sức chứa phòng theo UC-25: ghế đôi tính 2 người
UPDATE Room SET TotalSeats = (SELECT SUM(CASE WHEN SeatType = 'Sweetbox' THEN 2 ELSE 1 END) FROM Seat WHERE RoomID = @roomId)
WHERE RoomID = @roomId;

-- ===== Phòng bảo trì (E2) =====
IF NOT EXISTS (SELECT 1 FROM Room WHERE RoomName = N'Phòng test UC-09 (bảo trì)')
    INSERT INTO Room (RoomName, Format, TotalSeats, Status) VALUES (N'Phòng test UC-09 (bảo trì)', '2D', 0, 'Maintenance');
DECLARE @maintenanceRoomId INT = (SELECT RoomID FROM Room WHERE RoomName = N'Phòng test UC-09 (bảo trì)');

-- ===== Suất chiếu ngày mai (GB-08: EndTime = StartTime + thời lượng + 15 phút) =====
DECLARE @duration INT = (SELECT DurationMinutes FROM Movie WHERE MovieID = @movieId);
DECLARE @tomorrow DATETIME = CAST(CAST(DATEADD(DAY, 1, GETDATE()) AS DATE) AS DATETIME);
DECLARE @start1 DATETIME = DATEADD(HOUR, 19, @tomorrow);
DECLARE @start2 DATETIME = DATEADD(MINUTE, @duration + 15, @start1);

IF NOT EXISTS (SELECT 1 FROM Showtime WHERE RoomID = @roomId AND StartTime = @start1)
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@movieId, @roomId, @start1, DATEADD(MINUTE, @duration + 15, @start1), @price, 'Active');
IF NOT EXISTS (SELECT 1 FROM Showtime WHERE RoomID = @roomId AND StartTime = @start2)
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@movieId, @roomId, @start2, DATEADD(MINUTE, @duration + 15, @start2), @price, 'Active');
IF NOT EXISTS (SELECT 1 FROM Showtime WHERE RoomID = @maintenanceRoomId AND StartTime = @start1)
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status)
    VALUES (@movieId, @maintenanceRoomId, @start1, DATEADD(MINUTE, @duration + 15, @start1), @price, 'Active');

DECLARE @showtime1 INT = (SELECT TOP 1 ShowtimeID FROM Showtime WHERE RoomID = @roomId AND StartTime = @start1);
DECLARE @showtime2 INT = (SELECT TOP 1 ShowtimeID FROM Showtime WHERE RoomID = @roomId AND StartTime = @start2);
DECLARE @showtime3 INT = (SELECT TOP 1 ShowtimeID FROM Showtime WHERE RoomID = @maintenanceRoomId AND StartTime = @start1);

-- ===== Suất 1: ghế đang giữ tạm (PENDING) =====
DELETE FROM Seat_Hold WHERE ShowtimeID = @showtime1 AND SessionID = 'uc09-demo';
INSERT INTO Seat_Hold (SessionID, UserID, ShowtimeID, SeatID, HoldTime, ExpiryTime)
SELECT 'uc09-demo', @customerId, @showtime1, s.SeatID, GETDATE(), DATEADD(MINUTE, 10, GETDATE())
FROM Seat s
WHERE s.RoomID = @roomId AND s.RowName = 'B' AND s.ColNumber IN (4, 5, 6)
  AND NOT EXISTS (SELECT 1 FROM Seat_Hold h WHERE h.ShowtimeID = @showtime1 AND h.SeatID = s.SeatID);

-- ===== Suất 1: ghế đã bán (SOLD) =====
-- Giá vé theo loại ghế (TicketPricing): ghế đôi = giá 2 ghế thường
DECLARE @soldSeats TABLE (SeatID INT, Price DECIMAL(18,2));
DECLARE @bookingId INT;
IF NOT EXISTS (SELECT 1 FROM Ticket WHERE TicketCode LIKE 'UC09-' + CAST(@showtime1 AS VARCHAR(10)) + '-%')
BEGIN
    INSERT INTO @soldSeats
    SELECT SeatID, CASE WHEN SeatType = 'Sweetbox' THEN 2 * @price ELSE @price END FROM Seat
    WHERE RoomID = @roomId AND Status = 'Active'
      AND ((RowName = 'F' AND ColNumber IN (4, 5, 6)) OR (RowName = 'H' AND ColNumber = 5));

    INSERT INTO Booking (CustomerID, StaffID, ShowtimeID, VoucherID, TotalPrice, DiscountAmount, FinalAmount,
                         PaymentMethod, BookingTime, Status)
    SELECT @customerId, NULL, @showtime1, NULL, SUM(Price), 0, SUM(Price), 'VNPay', GETDATE(), 'Paid'
    FROM @soldSeats;
    SET @bookingId = SCOPE_IDENTITY();

    INSERT INTO Ticket (BookingID, SeatID, Price, TicketCode, QRCode, Status)
    SELECT @bookingId, SeatID, Price, 'UC09-' + CAST(@showtime1 AS VARCHAR(10)) + '-' + CAST(SeatID AS VARCHAR(10)),
           NULL, 'Upcoming'
    FROM @soldSeats;
END

-- ===== Suất 2: bán hết toàn bộ ghế còn dùng được (E1) =====
IF NOT EXISTS (SELECT 1 FROM Ticket WHERE TicketCode LIKE 'UC09-' + CAST(@showtime2 AS VARCHAR(10)) + '-%')
BEGIN
    DELETE FROM @soldSeats;
    INSERT INTO @soldSeats
    SELECT SeatID, CASE WHEN SeatType = 'Sweetbox' THEN 2 * @price ELSE @price END
    FROM Seat WHERE RoomID = @roomId AND Status = 'Active';

    INSERT INTO Booking (CustomerID, StaffID, ShowtimeID, VoucherID, TotalPrice, DiscountAmount, FinalAmount,
                         PaymentMethod, BookingTime, Status)
    SELECT NULL, (SELECT UserID FROM [User] WHERE Email = 'staff@cinema.vn'), @showtime2, NULL,
           SUM(Price), 0, SUM(Price), 'Cash', GETDATE(), 'Paid'
    FROM @soldSeats;
    SET @bookingId = SCOPE_IDENTITY();

    INSERT INTO Ticket (BookingID, SeatID, Price, TicketCode, QRCode, Status)
    SELECT @bookingId, SeatID, Price, 'UC09-' + CAST(@showtime2 AS VARCHAR(10)) + '-' + CAST(SeatID AS VARCHAR(10)),
           NULL, 'Upcoming'
    FROM @soldSeats;
END

-- Đã chạy UC19-29_staff_ops.sql thì Ticket có cột ShowtimeID, đồng bộ theo Booking (dùng EXEC để không lỗi khi chưa có cột)
IF COL_LENGTH('Ticket', 'ShowtimeID') IS NOT NULL
    EXEC (N'UPDATE t SET t.ShowtimeID = b.ShowtimeID FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID
            WHERE t.ShowtimeID IS NULL AND t.TicketCode LIKE ''UC09-%''');

SELECT @showtime1 AS ShowtimeID, N'Đủ 4 trạng thái ghế' AS Scenario, '/seat-map?id=' + CAST(@showtime1 AS VARCHAR(10)) AS Url
UNION ALL
SELECT @showtime2, N'E1 - Hết ghế trống', '/seat-map?id=' + CAST(@showtime2 AS VARCHAR(10))
UNION ALL
SELECT @showtime3, N'E2 - Phòng bảo trì', '/seat-map?id=' + CAST(@showtime3 AS VARCHAR(10));
GO
