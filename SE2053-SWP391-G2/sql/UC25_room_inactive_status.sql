-- UC-25: thêm trạng thái phòng "Inactive" (Dừng hoạt động) bên cạnh Active và Maintenance.
-- Maintenance chỉ do UC-26 (xử lý sự cố thiết bị) bật/tắt; Active/Inactive do Manager chọn ở trang Phòng chiếu.
-- Chạy sau cinemaDB.sql; chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID('Room') AND name = 'CK_Room_Status')
BEGIN
    -- Ràng buộc cũ do cinemaDB.sql tạo không đặt tên nên tìm theo định nghĩa
    DECLARE @oldCheck SYSNAME, @sql NVARCHAR(400);
    SELECT TOP (1) @oldCheck = name FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('Room') AND definition LIKE '%Status%';
    IF @oldCheck IS NOT NULL
    BEGIN
        SET @sql = N'ALTER TABLE Room DROP CONSTRAINT ' + QUOTENAME(@oldCheck);
        EXEC sp_executesql @sql;
    END
    ALTER TABLE Room ADD CONSTRAINT CK_Room_Status CHECK (Status IN ('Active', 'Maintenance', 'Inactive'));
END
GO
