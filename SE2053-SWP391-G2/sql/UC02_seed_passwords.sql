-- UC-02: Script đặt mật khẩu mẫu cho 4 vai trò (Admin, Manager, Staff, Customer)
-- Mật khẩu mặc định tương ứng:
-- 1. admin@cinema.vn     -> Admin@123
-- 2. manager@cinema.vn   -> Manager@123
-- 3. staff@cinema.vn     -> Staff@123
-- 4. duongpt@gmail.com   -> Customer@123
-- Mật khẩu chỉ lưu dạng băm PBKDF2WithHmacSHA256 ({salt_hex}:{hash_hex}), không lưu mật khẩu gốc.

USE cinemaDB;
GO

-- Đảm bảo trạng thái tất cả các tài khoản mẫu đều là Active
UPDATE [User]
SET Status = 'Active'
WHERE Email IN ('admin@cinema.vn', 'manager@cinema.vn', 'staff@cinema.vn', 'duongpt@gmail.com');

-- Đặt lại mật khẩu mặc định (đã băm) cho từng tài khoản mẫu
UPDATE [User] SET PasswordHash = '0318f09b80c7cfbfa47c38f3dcb0bfd3:39f941b486a5a322eb5bbeb289f5e1e25be056f177fe4bf0a2f2729987f7a448' WHERE Email = 'admin@cinema.vn';
UPDATE [User] SET PasswordHash = '97c4b9ef9607ce3554c95452d90ce28a:f48ee615ec2bd32bc34d2b74a86d2842753d5af6efbdcbd53d7bc24833c9f0de' WHERE Email = 'manager@cinema.vn';
UPDATE [User] SET PasswordHash = 'b118a04912bcaa8016bcaab6b63c65e3:afcc6fee6a671c0453404de9e95d74650983aa20fd1642cb9c2fd4388b423a60' WHERE Email = 'staff@cinema.vn';
UPDATE [User] SET PasswordHash = '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f' WHERE Email = 'duongpt@gmail.com';
GO
