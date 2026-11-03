-- UC-31: Manage Roles & Permissions
-- Chỉ lưu các quyền Admin đã thay đổi; quyền chưa có dòng dùng mặc định khai báo trong enum Permission.
-- Không seed dữ liệu: bảng rỗng cho kết quả đúng như phân quyền trước khi có UC-31.
-- Chạy được nhiều lần không lỗi.

IF OBJECT_ID('RolePermission', 'U') IS NULL
    CREATE TABLE RolePermission (
        RoleName VARCHAR(20) NOT NULL CHECK (RoleName IN ('Manager', 'Staff')),
        PermissionKey VARCHAR(50) NOT NULL,
        IsAllowed BIT NOT NULL,
        UpdatedAt DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT PK_RolePermission PRIMARY KEY (RoleName, PermissionKey)
    );
GO
