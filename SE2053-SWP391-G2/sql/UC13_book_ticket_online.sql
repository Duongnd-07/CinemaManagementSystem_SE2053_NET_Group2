-- UC-13 Book Ticket Online: tham số cấu hình và index cho luồng giữ ghế / đơn chờ thanh toán.
-- Chạy sau UC19-29_staff_ops.sql (cần cột Booking.Channel, Booking.PointsRedeemed, Ticket.ShowtimeID); chạy lại nhiều lần không lỗi.
--
-- Quy ước giữ ghế dùng chung với UC-14 và JOB-01 (không đổi schema Seat_Hold):
--   * Bước 5 (bấm Tiếp tục): mỗi ghế một dòng Seat_Hold, SessionID = 'CHECKOUT-<uuid>' của phiên đặt vé,
--     ExpiryTime = GETDATE() + SEAT_HOLD_MINUTES (GB-01).
--   * Bước 9 (xác nhận đơn): tạo Booking Pending + Ticket (Upcoming, chưa có QR) + Booking_Concession
--     và trừ kho bắp nước ngay (giữ hàng cho đơn); các dòng Seat_Hold của phiên được đổi SessionID thành
--     'BOOKING-<BookingID>', GIỮ NGUYÊN ExpiryTime.
--   * Đơn Pending (Online) còn hiệu lực khi còn ít nhất một Seat_Hold 'BOOKING-<BookingID>' chưa hết hạn.
--     Thanh toán thành công (UC-14) -> Booking Paid, sinh QR cho Ticket, xóa các Seat_Hold đó, gửi email xác nhận.
--     Hết hạn hoặc khách hủy -> Booking/Ticket Cancelled, xóa Seat_Hold, trả lại kho bắp nước (JOB-01 quét mỗi phút).
--   * Từ ONLINE_SALE_CUTOFF_MINUTES phút trước giờ chiếu: không giữ ghế, không thanh toán online (chỉ bán tại quầy).

IF OBJECT_ID('SystemConfig', 'U') IS NULL
    CREATE TABLE SystemConfig (
        ConfigKey VARCHAR(50) PRIMARY KEY,
        ConfigValue NVARCHAR(200) NOT NULL,
        Description NVARCHAR(300) NULL,
        UpdatedAt DATETIME NOT NULL DEFAULT GETDATE()
    );
GO

INSERT INTO SystemConfig (ConfigKey, ConfigValue, Description)
SELECT v.ConfigKey, v.ConfigValue, v.Description
FROM (VALUES
    ('SEAT_HOLD_MINUTES', N'10', N'GB-01: số phút giữ ghế PENDING khi khách đặt vé online'),
    ('POINT_EXCHANGE_RATE', N'1000', N'BR-LOYALTY-01: giá trị (VND) của 1 điểm khi trừ vào tiền vé'),
    ('BOOKING_MAX_SEATS', N'8', N'UC-13: số ghế tối đa trong một đơn đặt vé online'),
    ('ONLINE_SALE_CUTOFF_MINUTES', N'15', N'UC-13: số phút trước giờ chiếu thì đóng bán online (giữ ghế, thanh toán)'),
    ('SEAT_SURCHARGE_VIP', N'30000', N'UC-19: phụ thu (VND) cho ghế VIP, cộng vào giá suất chiếu')
) v (ConfigKey, ConfigValue, Description)
WHERE NOT EXISTS (SELECT 1 FROM SystemConfig s WHERE s.ConfigKey = v.ConfigKey);
GO

-- Ghế VIP đắt hơn ghế thường 30.000đ: DB tạo trước đó còn giá trị mặc định cũ 0 thì nâng lên 30000
-- (giá trị khác 0 là đã được cấu hình riêng nên giữ nguyên)
UPDATE SystemConfig SET ConfigValue = N'30000', UpdatedAt = GETDATE()
WHERE ConfigKey = 'SEAT_SURCHARGE_VIP' AND ConfigValue = N'0';
GO

-- Tra cứu ghế đang giữ theo phiên/đơn và dọn ghế hết hạn (JOB-01)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_SeatHold_Session' AND object_id = OBJECT_ID('Seat_Hold'))
    CREATE INDEX IX_SeatHold_Session ON Seat_Hold (SessionID, ExpiryTime);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_SeatHold_Expiry' AND object_id = OBJECT_ID('Seat_Hold'))
    CREATE INDEX IX_SeatHold_Expiry ON Seat_Hold (ExpiryTime);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Booking_Status_Channel' AND object_id = OBJECT_ID('Booking'))
    CREATE INDEX IX_Booking_Status_Channel ON Booking (Status, Channel) INCLUDE (CustomerID, VoucherID, PointsRedeemed);
GO
