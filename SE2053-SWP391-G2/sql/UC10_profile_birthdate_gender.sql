-- UC-10: Bổ sung Ngày sinh và Giới tính cho hồ sơ khách hàng.
-- Cho phép NULL vì các tài khoản đã đăng ký trước đó chưa có dữ liệu này. Chạy lại nhiều lần vẫn an toàn.

IF COL_LENGTH('[User]', 'DateOfBirth') IS NULL
    ALTER TABLE [User] ADD DateOfBirth DATE NULL;
GO

IF COL_LENGTH('[User]', 'Gender') IS NULL
    ALTER TABLE [User] ADD Gender VARCHAR(10) NULL
        CONSTRAINT CK_User_Gender CHECK (Gender IN ('Male', 'Female', 'Other'));
GO
