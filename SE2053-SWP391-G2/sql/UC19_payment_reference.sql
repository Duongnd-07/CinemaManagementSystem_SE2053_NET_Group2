-- UC-19: thông tin thu tiền của đơn bán tại quầy.
-- 1. PaymentReference: mã giao dịch của đơn trả bằng thẻ (mã chuẩn chi trên hóa đơn POS) hoặc chuyển khoản
--    (mã giao dịch ngân hàng), để nhân viên chỉ xác nhận khi đã thấy tiền về và Manager đối chiếu khi kết ca (UC-22).
-- 2. CashReceived: tiền mặt khách đưa, để lịch sử bán tại quầy hiện tiền thối (đơn cũ để NULL).
-- Chạy sau UC19-29_staff_ops.sql; chạy lại nhiều lần không lỗi.
SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF COL_LENGTH('Booking', 'PaymentReference') IS NULL
    ALTER TABLE Booking ADD PaymentReference VARCHAR(30) NULL;
IF COL_LENGTH('Booking', 'CashReceived') IS NULL
    ALTER TABLE Booking ADD CashReceived DECIMAL(18,2) NULL
        CONSTRAINT CK_Booking_CashReceived CHECK (CashReceived >= 0);
GO

-- Mã giao dịch ngân hàng là duy nhất: một lần chuyển khoản chỉ dùng cho một đơn.
-- Mã chuẩn chi POS chỉ có 6 số nên có thể trùng giữa các ngày, không ràng buộc duy nhất
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Booking_TransferReference')
    CREATE UNIQUE INDEX UX_Booking_TransferReference ON Booking (PaymentReference)
        WHERE PaymentMethod = 'Transfer' AND PaymentReference IS NOT NULL;
GO
