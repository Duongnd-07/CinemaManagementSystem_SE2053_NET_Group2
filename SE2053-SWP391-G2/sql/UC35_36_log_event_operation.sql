-- UC-32: thêm nhóm sự kiện OPERATION ("Vận hành rạp") cho nhật ký kiểm tra phòng (UC-35) và in vé giấy (UC-36).
-- Giữ đủ các nhóm cũ, kể cả NOTIFICATION của UC09_manage_notifications.sql, nên chạy trước hay sau script đó đều được
-- (UC09 chỉ tạo lại ràng buộc khi thiếu NOTIFICATION). Chạy sau UC32_activity_log.sql; chạy lại nhiều lần không lỗi.
-- Chưa chạy script này thì UC-35/36 vẫn hoạt động, chỉ không ghi được dòng nhật ký nhóm OPERATION (ActivityLogger bỏ qua lỗi).
SET NOCOUNT ON;
GO

-- Ràng buộc không tên do UC32_activity_log.sql tạo (khi UC09 chưa chạy)
DECLARE @eventCheck SYSNAME, @eventSql NVARCHAR(400);
SELECT TOP (1) @eventCheck = name
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID('ActivityLog')
    AND definition LIKE '%EventType%'
    AND name <> 'CK_ActivityLog_EventType';
IF @eventCheck IS NOT NULL
BEGIN
    SET @eventSql = N'ALTER TABLE ActivityLog DROP CONSTRAINT ' + QUOTENAME(@eventCheck);
    EXEC sp_executesql @eventSql;
END
GO

IF EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_ActivityLog_EventType' AND parent_object_id = OBJECT_ID('ActivityLog')
        AND definition NOT LIKE '%OPERATION%'
)
    ALTER TABLE ActivityLog DROP CONSTRAINT CK_ActivityLog_EventType;
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_ActivityLog_EventType' AND parent_object_id = OBJECT_ID('ActivityLog')
)
    ALTER TABLE ActivityLog ADD CONSTRAINT CK_ActivityLog_EventType
        CHECK (EventType IN ('AUTH', 'ACCOUNT', 'CONFIG', 'CATALOG', 'BOOKING', 'COUNTER', 'SYSTEM', 'NOTIFICATION',
                             'OPERATION'));
GO
