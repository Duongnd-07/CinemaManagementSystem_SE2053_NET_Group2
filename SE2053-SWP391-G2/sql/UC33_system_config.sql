-- UC-33: Manage System Configurations
-- Bổ sung tham số hệ thống mà code đã đọc nhưng chưa migration nào tạo.
-- POINT_EARN_PERCENT: BookingService (UC-14) đọc, mặc định 1 khi chưa có -> giá trị seed giữ nguyên hành vi hiện tại.
-- Chạy sau UC13_book_ticket_online.sql / UC19-29_staff_ops.sql; chạy lại nhiều lần không lỗi.

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
    ('POINT_EARN_PERCENT', N'1', N'BR-LOYALTY-01: tỷ lệ (%) giá trị vé sau giảm giá được quy thành điểm thưởng'),
    -- UC-16: hạn hủy vé (giờ trước giờ chiếu) và % giá vé hoàn thành voucher
    ('CANCELLATION_MIN_HOURS', N'24', N'UC-16: chỉ được hủy vé online khi còn ít nhất chừng này giờ trước giờ chiếu'),
    ('CANCELLATION_VOUCHER_PERCENT', N'80', N'UC-16: % giá vé được hoàn vào ví dưới dạng voucher khi hủy vé online'),
    -- UC-01: voucher tặng khi đăng ký tài khoản; WELCOME_VOUCHER_VALUE = 0 là ngừng tặng
    ('WELCOME_VOUCHER_VALUE', N'50000', N'UC-01: giá trị (VND) voucher tặng khách khi đăng ký thành công'),
    ('WELCOME_VOUCHER_DAYS', N'30', N'UC-01: số ngày hiệu lực của voucher người mới'),
    ('WELCOME_VOUCHER_MIN_ORDER', N'0', N'UC-01: giá trị đơn tối thiểu (VND) để dùng voucher người mới')
) v (ConfigKey, ConfigValue, Description)
WHERE NOT EXISTS (SELECT 1 FROM SystemConfig s WHERE s.ConfigKey = v.ConfigKey);
GO

-- Tỷ lệ hoàn giờ đã cấu hình được nên mô tả cũ ghi cứng 80% không còn đúng
UPDATE SystemConfig SET Description = N'UC-16: số ngày hiệu lực của voucher hoàn vé khi hủy vé online'
WHERE ConfigKey = 'CANCELLATION_VOUCHER_DAYS'
  AND Description = N'UC-16: số ngày hiệu lực của voucher hoàn 80% giá vé';
GO
