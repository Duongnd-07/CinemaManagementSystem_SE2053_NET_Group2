-- UC-25: ghế đôi (Sweetbox) là MỘT ghế chiếm 2 cột liền nhau (ColNumber = cột trái) và chứa 2 người.
-- Chuyển dữ liệu cũ (mỗi Sweetbox chiếm 1 cột, ví dụ H1, H2, H3... liền nhau) sang quy ước mới.
-- Chạy sau cinemaDB.sql và các migration khác (UC19-29_staff_ops.sql); chạy lại nhiều lần không lỗi.
-- Việc làm:
--   1. Ghế đôi ở cột n sẽ chiếm luôn cột n+1: ghế đang nằm ở cột n+1 bị xóa nếu chưa có lịch sử
--      (vé, giữ chỗ, báo cáo sự cố). Ghế đã có lịch sử thì giữ nguyên và được liệt kê để xử lý tay
--      trên trang Phòng chiếu (UC-25) - trang đó báo "có ghế chồng lên nhau" cho tới khi xử lý xong.
--   2. Room.TotalSeats = sức chứa theo người (ghế đôi tính 2) cho các phòng đã có ghế.
--   3. Bỏ tham số SEAT_SURCHARGE_SWEETBOX: giá ghế đôi = giá 2 ghế thường (TicketPricing).
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
GO

-- ===== 1. Gộp ghế đôi chiếm 2 cột =====
-- Duyệt từ trái sang phải: ghế đôi chưa bị ghế đôi khác bên trái che, và ô bên phải đang có ghế không lịch sử
DECLARE @removed INT = 0;
DECLARE @seatId INT;
WHILE 1 = 1
BEGIN
    SET @seatId = NULL;
    SELECT TOP 1 @seatId = covered.SeatID
    FROM Seat couple
    JOIN Seat covered ON covered.RoomID = couple.RoomID AND covered.RowName = couple.RowName
                     AND covered.ColNumber = couple.ColNumber + 1
    WHERE couple.SeatType = 'Sweetbox'
      AND NOT EXISTS (SELECT 1 FROM Seat leftCouple
                      WHERE leftCouple.RoomID = couple.RoomID AND leftCouple.RowName = couple.RowName
                        AND leftCouple.ColNumber = couple.ColNumber - 1 AND leftCouple.SeatType = 'Sweetbox')
      AND NOT EXISTS (SELECT 1 FROM Ticket t WHERE t.SeatID = covered.SeatID)
      AND NOT EXISTS (SELECT 1 FROM Seat_Hold h WHERE h.SeatID = covered.SeatID)
      AND NOT EXISTS (SELECT 1 FROM Equipment_Report e WHERE e.SeatID = covered.SeatID)
    ORDER BY couple.RoomID, couple.RowName, couple.ColNumber;

    IF @seatId IS NULL
        BREAK;
    DELETE FROM Seat WHERE SeatID = @seatId;
    SET @removed += 1;
END
PRINT N'UC-25: đã xóa ' + CAST(@removed AS NVARCHAR(10)) + N' ghế bị ghế đôi che.';

-- Chồng ghế còn lại (ô bên phải đã có lịch sử nên không tự xóa được)
IF EXISTS (SELECT 1 FROM Seat couple
           JOIN Seat covered ON covered.RoomID = couple.RoomID AND covered.RowName = couple.RowName
                            AND covered.ColNumber = couple.ColNumber + 1
           WHERE couple.SeatType = 'Sweetbox')
BEGIN
    PRINT N'UC-25: còn ghế chồng lên nhau vì ghế bị che đã có vé/giữ chỗ/báo cáo (danh sách bên dưới). '
        + N'Nếu đó là dữ liệu test (ví dụ vé từ UC09_seed_seat_map.sql bản cũ), cách nhanh nhất là tạo lại DB theo README mục 10; '
        + N'nếu là dữ liệu thật, cần xử lý tay các vé đó trước rồi chạy lại script này.';
    SELECT r.RoomName AS Room, couple.RowName + CAST(couple.ColNumber AS VARCHAR(10)) AS CoupleSeat,
           covered.RowName + CAST(covered.ColNumber AS VARCHAR(10)) AS CoveredSeat
    FROM Seat couple
    JOIN Seat covered ON covered.RoomID = couple.RoomID AND covered.RowName = couple.RowName
                     AND covered.ColNumber = couple.ColNumber + 1
    JOIN Room r ON r.RoomID = couple.RoomID
    WHERE couple.SeatType = 'Sweetbox'
    ORDER BY r.RoomName, couple.RowName, couple.ColNumber;
END
GO

-- ===== 2. Sức chứa theo người =====
UPDATE r SET r.TotalSeats = cap.Capacity
FROM Room r
JOIN (SELECT RoomID, SUM(CASE WHEN SeatType = 'Sweetbox' THEN 2 ELSE 1 END) AS Capacity
      FROM Seat GROUP BY RoomID) cap ON cap.RoomID = r.RoomID
WHERE r.TotalSeats <> cap.Capacity;
GO

-- ===== 3. Bỏ phụ thu ghế đôi =====
IF OBJECT_ID('SystemConfig', 'U') IS NOT NULL
    DELETE FROM SystemConfig WHERE ConfigKey = 'SEAT_SURCHARGE_SWEETBOX';
GO
