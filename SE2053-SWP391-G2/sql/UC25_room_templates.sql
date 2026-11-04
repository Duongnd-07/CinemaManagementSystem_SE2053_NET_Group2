-- UC-25 (A3-A5): Phòng mẫu - sơ đồ ghế dùng lại khi tạo phòng mới.
-- Phòng mẫu lưu riêng, không phải một dòng Room: không có trạng thái, suất chiếu, vé, giữ chỗ hay thiết bị,
-- nên không bao giờ lọt vào các màn hình đọc bảng Room (suất chiếu, bán vé, sơ đồ ghế, sự cố, thống kê).
-- Layout cùng định dạng với RoomLayoutForm: các hàng ngăn bởi "/", mỗi ký tự một cột
-- (S/V/C ghế dùng được, s/v/c ghế khóa, "=" nửa phải ghế đôi, "." lối đi). Tối đa 26 hàng x 30 cột.
-- Chạy sau cinemaDB.sql; chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
GO

IF OBJECT_ID('RoomTemplate', 'U') IS NULL
    CREATE TABLE RoomTemplate (
        TemplateID INT IDENTITY(1,1) PRIMARY KEY,
        TemplateName NVARCHAR(50) NOT NULL CONSTRAINT UQ_RoomTemplate_Name UNIQUE,
        -- Định dạng gợi ý khi tạo phòng từ mẫu (Manager vẫn đổi được)
        Format VARCHAR(10) NULL,
        GridRows INT NOT NULL CONSTRAINT CK_RoomTemplate_Rows CHECK (GridRows BETWEEN 1 AND 26),
        GridCols INT NOT NULL CONSTRAINT CK_RoomTemplate_Cols CHECK (GridCols BETWEEN 1 AND 30),
        Layout VARCHAR(1000) NOT NULL,
        UpdatedAt DATETIME NOT NULL CONSTRAINT DF_RoomTemplate_UpdatedAt DEFAULT GETDATE()
    );
GO

-- Vài phòng mẫu mặc định để danh sách không trống khi dùng lần đầu
IF NOT EXISTS (SELECT 1 FROM RoomTemplate)
BEGIN
    INSERT INTO RoomTemplate (TemplateName, Format, GridRows, GridCols, Layout) VALUES
    (N'Phòng 2D tiêu chuẩn (10 × 13)', '2D', 10, 13,
     'SSSSSS.SSSSSS/SSSSSS.SSSSSS/SSSSSS.SSSSSS/SSSSSS.SSSSSS/SSSSSS.SSSSSS/SSSSSS.SSSSSS/SSSSSS.SSSSSS/VVVVVV.VVVVVV/VVVVVV.VVVVVV/VVVVVV.VVVVVV'),
    (N'Phòng IMAX lớn (14 × 22)', 'IMAX', 14, 22,
     'SSSS.SSSSSSSSSSSS.SSSS/SSSS.SSSSSSSSSSSS.SSSS/SSSS.SSSSSSSSSSSS.SSSS/VVVV.VVVVVVVVVVVV.VVVV/VVVV.VVVVVVVVVVVV.VVVV/VVVV.VVVVVVVVVVVV.VVVV/VVVV.VVVVVVVVVVVV.VVVV/VVVV.VVVVVVVVVVVV.VVVV/VVVV.VVVVVVVVVVVV.VVVV/VVVV.VVVVVVVVVVVV.VVVV/VVVV.VVVVVVVVVVVV.VVVV/VVVV.VVVVVVVVVVVV.VVVV/SSSS.SSSSSSSSSSSS.SSSS/SSSS.SSSSSSSSSSSS.SSSS'),
    (N'Phòng nhỏ có ghế đôi (6 × 10)', '2D', 6, 10,
     'SSSSSSSSSS/SSSSSSSSSS/SSSSSSSSSS/SSSSSSSSSS/VVVVVVVVVV/C=C=C=C=C=');
END
GO
