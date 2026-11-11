-- ==============================================================================
-- Băm (mã hoá) mật khẩu cho các tài khoản còn lưu giá trị tạm 'hashed_pwd'.
-- Hệ thống chỉ chấp nhận mật khẩu đã băm PBKDF2WithHmacSHA256 dạng {salt_hex}:{hash_hex}.
-- Tài khoản còn 'hashed_pwd' sẽ KHÔNG đăng nhập được cho tới khi chạy script này.
--
-- Mật khẩu sau khi chạy (theo vai trò hiện tại của tài khoản):
--   Admin    -> Admin@123
--   Manager  -> Manager@123
--   Staff    -> Staff@123
--   Customer -> Customer@123
-- Chạy lại nhiều lần an toàn: chỉ cập nhật tài khoản còn 'hashed_pwd'.
-- Chạy bằng sqlcmd: sqlcmd -S localhost -U sa -f 65001 -i hash_seed_passwords.sql
-- ==============================================================================

USE cinemaDB;
GO

SET NOCOUNT ON;

UPDATE [User]
SET PasswordHash = CASE Role
        WHEN 'Admin'   THEN '0318f09b80c7cfbfa47c38f3dcb0bfd3:39f941b486a5a322eb5bbeb289f5e1e25be056f177fe4bf0a2f2729987f7a448'
        WHEN 'Manager' THEN '97c4b9ef9607ce3554c95452d90ce28a:f48ee615ec2bd32bc34d2b74a86d2842753d5af6efbdcbd53d7bc24833c9f0de'
        WHEN 'Staff'   THEN 'b118a04912bcaa8016bcaab6b63c65e3:afcc6fee6a671c0453404de9e95d74650983aa20fd1642cb9c2fd4388b423a60'
        ELSE                '2484478cd1fa6a944d87658b362ad6ef:e75de588056768c6fd1e1397dab9fb0968659f14f8f3727dbd676d87bb65481f'
    END
WHERE PasswordHash = 'hashed_pwd';

SELECT @@ROWCOUNT AS AccountsHashed;
GO

-- Kiểm tra: số tài khoản chưa ở dạng PBKDF2 (phải bằng 0)
SELECT COUNT(*) AS AccountsNotHashed
FROM [User]
WHERE PasswordHash IS NULL OR LEN(PasswordHash) <> 97 OR CHARINDEX(':', PasswordHash) <> 33;
GO
