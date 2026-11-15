-- UC-19/UC-22: mỗi quầy một két tiền nên chỉ một ca đang mở tại một quầy.
-- Chạy sau UC19-29_staff_ops.sql; chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- Đang có hai ca cùng mở ở một quầy thì phải kết bớt trước, index mới tạo được
IF EXISTS (SELECT CounterNo FROM Shift WHERE EndedAt IS NULL GROUP BY CounterNo HAVING COUNT(*) > 1)
    PRINT N'Có quầy đang mở nhiều ca. Hãy kết bớt ca rồi chạy lại script này.';
ELSE IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Shift_OpenPerCounter')
    CREATE UNIQUE INDEX UX_Shift_OpenPerCounter ON Shift (CounterNo) WHERE EndedAt IS NULL;
GO
