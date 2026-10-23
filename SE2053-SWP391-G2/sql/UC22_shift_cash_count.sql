-- UC-22: View Shift Report - đối chiếu két tiền cuối ca và tự động kết ca.
-- Chạy sau UC19-29_staff_ops.sql (bảng Shift, SystemConfig); chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- OpeningCash: tiền lẻ có sẵn trong két lúc mở ca; CountedCash: tiền nhân viên đếm được khi kết ca
-- (NULL = chưa đếm, ví dụ ca tự kết khi hết giờ); CashNote: giải thích khi thừa/thiếu;
-- ClosedBy: người kết ca (nhân viên hoặc quản lý), NULL = hệ thống tự kết khi hết giờ
IF COL_LENGTH('Shift', 'OpeningCash') IS NULL
    ALTER TABLE Shift ADD OpeningCash DECIMAL(18,2) NOT NULL
        CONSTRAINT DF_Shift_OpeningCash DEFAULT 0
        CONSTRAINT CK_Shift_OpeningCash CHECK (OpeningCash >= 0);
IF COL_LENGTH('Shift', 'CountedCash') IS NULL
    ALTER TABLE Shift ADD CountedCash DECIMAL(18,2) NULL
        CONSTRAINT CK_Shift_CountedCash CHECK (CountedCash >= 0);
IF COL_LENGTH('Shift', 'CashNote') IS NULL
    ALTER TABLE Shift ADD CashNote NVARCHAR(500) NULL;
IF COL_LENGTH('Shift', 'ClosedBy') IS NULL
    ALTER TABLE Shift ADD ClosedBy INT NULL
        CONSTRAINT FK_Shift_ClosedBy FOREIGN KEY REFERENCES [User](UserID);
GO

-- Thời lượng tối đa một ca; hết giờ hệ thống tự kết ca và chốt số liệu
INSERT INTO SystemConfig (ConfigKey, ConfigValue, Description)
SELECT 'SHIFT_HOURS', N'4', N'UC-22: số giờ tối đa của một ca bán tại quầy, hết giờ hệ thống tự kết ca'
WHERE NOT EXISTS (SELECT 1 FROM SystemConfig WHERE ConfigKey = 'SHIFT_HOURS');
GO
