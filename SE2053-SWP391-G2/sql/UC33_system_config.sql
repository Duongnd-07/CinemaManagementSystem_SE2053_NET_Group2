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
    ('POINT_EARN_PERCENT', N'1', N'BR-LOYALTY-01: tỷ lệ (%) giá trị vé sau giảm giá được quy thành điểm thưởng')
) v (ConfigKey, ConfigValue, Description)
WHERE NOT EXISTS (SELECT 1 FROM SystemConfig s WHERE s.ConfigKey = v.ConfigKey);
GO
