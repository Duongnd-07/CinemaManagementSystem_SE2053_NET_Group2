-- UC-02: Script đặt mật khẩu mẫu cho 4 vai trò (Admin, Manager, Staff, Customer)
-- Mật khẩu mặc định tương ứng:
-- 1. admin@cinema.vn     -> Admin@123
-- 2. manager@cinema.vn   -> Manager@123
-- 3. staff@cinema.vn     -> Staff@123
-- 4. duongpt@gmail.com   -> Customer@123
-- (Hệ thống tự động nhận diện và nâng cấp sang PBKDF2 hash khi đăng nhập lần đầu)

USE cinemaDB;
GO

-- Đảm bảo trạng thái tất cả các tài khoản mẫu đều là Active
UPDATE [User]
SET Status = 'Active'
WHERE Email IN ('admin@cinema.vn', 'manager@cinema.vn', 'staff@cinema.vn', 'duongpt@gmail.com');

-- Đặt PasswordHash thành hashed_pwd để LoginService tự động nâng cấp sang PBKDF2 chuẩn bảo mật
UPDATE [User]
SET PasswordHash = 'hashed_pwd'
WHERE Email IN ('admin@cinema.vn', 'manager@cinema.vn', 'staff@cinema.vn', 'duongpt@gmail.com');
GO
