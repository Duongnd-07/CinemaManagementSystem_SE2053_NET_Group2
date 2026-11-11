-- ==============================================================================
-- Script seed dữ liệu 30 tài khoản Customer (Khách hàng) cho CinemaDB
-- Mật khẩu đăng nhập dùng chung: Customer@123 (lưu dạng băm PBKDF2, không lưu mật khẩu gốc)
-- Trạng thái: Active (Hoạt động), Suspended (Tạm khóa), Deactivated (Vô hiệu hóa)
-- Chạy lại nhiều lần an toàn (kiểm tra tồn tại theo Email trước khi INSERT)
-- ==============================================================================

USE CinemaDB;
GO

SET NOCOUNT ON;

-- Danh sách 30 khách hàng mẫu với thông tin đa dạng (họ tên tiếng Việt, ngày sinh, giới tính, điểm tích lũy)
MERGE INTO [User] AS target
USING (VALUES
    (N'Nguyễn Văn An',      'customer01@gmail.com', '0981000001', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 120, 'Active',      CAST('1995-03-12' AS DATE), 'Male'),
    (N'Trần Thị Bích',      'customer02@gmail.com', '0981000002', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 50,  'Active',      CAST('1998-07-25' AS DATE), 'Female'),
    (N'Lê Hoàng Cường',     'customer03@gmail.com', '0981000003', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 300, 'Active',      CAST('2000-11-05' AS DATE), 'Male'),
    (N'Phạm Minh Đức',      'customer04@gmail.com', '0981000004', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 0,   'Active',      CAST('1997-01-18' AS DATE), 'Male'),
    (N'Hoàng Thuỳ Dung',    'customer05@gmail.com', '0981000005', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 210, 'Active',      CAST('2002-09-30' AS DATE), 'Female'),
    (N'Vũ Quốc Đạt',        'customer06@gmail.com', '0981000006', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 80,  'Active',      CAST('1996-04-14' AS DATE), 'Male'),
    (N'Đỗ Ngọc Hân',        'customer07@gmail.com', '0981000007', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 150, 'Active',      CAST('2001-12-08' AS DATE), 'Female'),
    (N'Bùi Gia Huy',        'customer08@gmail.com', '0981000008', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 0,   'Active',      CAST('1999-06-22' AS DATE), 'Male'),
    (N'Đặng Khánh Huyền',   'customer09@gmail.com', '0981000009', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 95,  'Active',      CAST('2003-02-17' AS DATE), 'Female'),
    (N'Ngô Anh Khoa',       'customer10@gmail.com', '0981000010', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 450, 'Active',      CAST('1994-08-09' AS DATE), 'Male'),
    (N'Dương Thảo Linh',    'customer11@gmail.com', '0981000011', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 130, 'Active',      CAST('2000-05-19' AS DATE), 'Female'),
    (N'Lý Quang Minh',      'customer12@gmail.com', '0981000012', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 70,  'Active',      CAST('1998-10-03' AS DATE), 'Male'),
    (N'Mai Phương Nga',     'customer13@gmail.com', '0981000013', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 0,   'Active',      CAST('2002-04-28' AS DATE), 'Female'),
    (N'Phan Trọng Nghĩa',   'customer14@gmail.com', '0981000014', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 260, 'Active',      CAST('1997-12-15' AS DATE), 'Male'),
    (N'Trịnh Kim Ngân',     'customer15@gmail.com', '0981000015', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 180, 'Active',      CAST('2001-08-21' AS DATE), 'Female'),
    (N'Đinh Hữu Phúc',      'customer16@gmail.com', '0981000016', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 40,  'Active',      CAST('1995-09-11' AS DATE), 'Male'),
    (N'Đoàn Bảo Quyên',     'customer17@gmail.com', '0981000017', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 0,   'Active',      CAST('2004-01-05' AS DATE), 'Female'),
    (N'Hà Tuấn Sơn',        'customer18@gmail.com', '0981000018', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 320, 'Active',      CAST('1996-07-16' AS DATE), 'Male'),
    (N'Lâm Yến Trang',      'customer19@gmail.com', '0981000019', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 110, 'Active',      CAST('1999-11-29' AS DATE), 'Female'),
    (N'Cao Thái Thịnh',     'customer20@gmail.com', '0981000020', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 85,  'Active',      CAST('2000-03-07' AS DATE), 'Male'),
    (N'Lương Hồng Vân',     'customer21@gmail.com', '0981000021', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 200, 'Active',      CAST('1998-06-14' AS DATE), 'Female'),
    (N'Tạ Quốc Việt',       'customer22@gmail.com', '0981000022', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 500, 'Active',      CAST('1993-10-26' AS DATE), 'Male'),
    (N'Trương Thanh Xuân',  'customer23@gmail.com', '0981000023', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 60,  'Active',      CAST('2002-02-11' AS DATE), 'Female'),
    (N'Võ Thành Nam',       'customer24@gmail.com', '0981000024', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 0,   'Active',      CAST('1997-05-04' AS DATE), 'Male'),
    (N'Thái Mỹ Duyên',      'customer25@gmail.com', '0981000025', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 175, 'Active',      CAST('2001-09-19' AS DATE), 'Female'),
    (N'Châu Tấn Phát',      'customer26@gmail.com', '0981000026', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 90,  'Active',      CAST('1999-04-02' AS DATE), 'Male'),
    (N'Bạch Tú Uyên',       'customer27@gmail.com', '0981000027', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 40,  'Active',      CAST('2003-08-31' AS DATE), 'Female'),
    (N'Diệp Hải Đăng',      'customer28@gmail.com', '0981000028', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 230, 'Active',      CAST('1996-12-24' AS DATE), 'Male'),
    (N'Khổng Minh Trí',     'customer29@gmail.com', '0981000029', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 15,  'Suspended',   CAST('2000-07-09' AS DATE), 'Male'),
    (N'Tăng Ánh Tuyết',     'customer30@gmail.com', '0981000030', '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f', 'Customer', 0,   'Deactivated', CAST('2002-10-18' AS DATE), 'Female')
) AS source (FullName, Email, Phone, PasswordHash, Role, PointBalance, Status, DateOfBirth, Gender)
ON target.Email = source.Email
WHEN MATCHED THEN
    UPDATE SET 
        target.FullName = source.FullName,
        target.Phone = source.Phone,
        target.PasswordHash = source.PasswordHash,
        target.Role = source.Role,
        target.PointBalance = source.PointBalance,
        target.Status = source.Status,
        target.DateOfBirth = source.DateOfBirth,
        target.Gender = source.Gender
WHEN NOT MATCHED THEN
    INSERT (FullName, Email, Phone, PasswordHash, Role, PointBalance, Status, DateOfBirth, Gender)
    VALUES (source.FullName, source.Email, source.Phone, source.PasswordHash, source.Role, source.PointBalance, source.Status, source.DateOfBirth, source.Gender);

PRINT N'Đã hoàn tất nạp 30 tài khoản Customer vào hệ thống!';
GO
