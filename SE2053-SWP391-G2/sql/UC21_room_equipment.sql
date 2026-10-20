-- UC-21: danh sách thiết bị cụ thể của từng phòng (Điều hòa 1, Điều hòa 2...) để nhân viên báo hỏng đúng thiết bị,
-- và chọn nhiều ghế/thiết bị trong một lần gửi (mỗi thiết bị vẫn là một báo cáo riêng để UC-26 xử lý từng cái).
-- Chạy sau UC19-29_staff_ops.sql và các script dữ liệu mẫu (cinemaDB_data.sql, UC09_seed_seat_map.sql)
-- vì cần có sẵn phòng để nạp thiết bị mẫu; chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
GO

IF OBJECT_ID('RoomEquipment', 'U') IS NULL
    CREATE TABLE RoomEquipment (
        EquipmentID INT IDENTITY(1,1) PRIMARY KEY,
        RoomID INT NOT NULL CONSTRAINT FK_RoomEquipment_Room FOREIGN KEY REFERENCES Room(RoomID),
        Category VARCHAR(50) NOT NULL
            CONSTRAINT CK_RoomEquipment_Category CHECK (Category IN ('Projector', 'Sound System', 'Air Conditioning')),
        Name NVARCHAR(100) NOT NULL,
        CONSTRAINT UQ_RoomEquipment_Room_Name UNIQUE (RoomID, Name)
    );
GO

IF COL_LENGTH('Equipment_Report', 'EquipmentID') IS NULL
    ALTER TABLE Equipment_Report ADD EquipmentID INT NULL
        CONSTRAINT FK_EquipmentReport_Equipment FOREIGN KEY REFERENCES RoomEquipment(EquipmentID);
GO

-- Mỗi thiết bị cụ thể chỉ có một báo cáo chưa xử lý xong (chặn hai nhân viên báo cùng lúc)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_EquipmentReport_OpenUnit')
    CREATE UNIQUE INDEX UX_EquipmentReport_OpenUnit ON Equipment_Report (EquipmentID)
        WHERE EquipmentID IS NOT NULL AND Status IN ('Pending', 'In Progress');
GO

-- Index cũ chặn theo (phòng, loại) giờ chỉ áp dụng cho báo cáo không gắn thiết bị cụ thể
-- (phòng chưa khai báo thiết bị), nếu không thì không báo được Điều hòa 1 và Điều hòa 2 cùng lúc
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_EquipmentReport_OpenEquipment'
           AND object_id = OBJECT_ID('Equipment_Report') AND filter_definition NOT LIKE '%EquipmentID%')
    DROP INDEX UX_EquipmentReport_OpenEquipment ON Equipment_Report;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_EquipmentReport_OpenEquipment')
    CREATE UNIQUE INDEX UX_EquipmentReport_OpenEquipment ON Equipment_Report (RoomID, EquipmentCategory)
        WHERE SeatID IS NULL AND EquipmentID IS NULL AND Status IN ('Pending', 'In Progress')
            AND EquipmentCategory IN ('Projector', 'Sound System', 'Air Conditioning');
GO

-- Thiết bị mẫu: chỉ thêm thiết bị phòng chưa có, nên chạy lại được để bổ sung thiết bị mới cho DB cũ
INSERT INTO RoomEquipment (RoomID, Category, Name)
SELECT r.RoomID, v.Category, v.Name
FROM Room r
CROSS JOIN (VALUES
    ('Projector', N'Máy chiếu 1'),
    ('Projector', N'Máy chiếu 2'),
    ('Sound System', N'Loa 1'),
    ('Sound System', N'Loa 2'),
    ('Air Conditioning', N'Điều hòa 1'),
    ('Air Conditioning', N'Điều hòa 2')
) v (Category, Name)
WHERE NOT EXISTS (SELECT 1 FROM RoomEquipment e WHERE e.RoomID = r.RoomID AND e.Name = v.Name);
GO
