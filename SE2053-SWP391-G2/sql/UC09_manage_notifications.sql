-- UC-09: Manage Notifications; chạy sau UC11_12_15_16_18_customer_ops.sql.
-- ActivityLog phải đã được tạo bằng UC32_activity_log.sql. Chạy lại được nhiều lần.
IF OBJECT_ID('Announcement', 'U') IS NULL
BEGIN
    CREATE TABLE Announcement (
        AnnouncementID INT IDENTITY(1,1) CONSTRAINT PK_Announcement PRIMARY KEY,
        SenderID INT NOT NULL CONSTRAINT FK_Announcement_Sender REFERENCES [User](UserID),
        Title NVARCHAR(150) NOT NULL,
        Message NVARCHAR(1000) NOT NULL,
        NotificationType VARCHAR(30) NOT NULL,
        TargetType VARCHAR(20) NOT NULL,
        TargetUserID INT NULL CONSTRAINT FK_Announcement_TargetUser REFERENCES [User](UserID),
        RecipientCount INT NOT NULL CONSTRAINT DF_Announcement_RecipientCount DEFAULT 0,
        Status VARCHAR(20) NOT NULL CONSTRAINT DF_Announcement_Status DEFAULT 'Sent',
        CreatedAt DATETIME NOT NULL CONSTRAINT DF_Announcement_CreatedAt DEFAULT GETDATE(),
        RecalledAt DATETIME NULL,
        RecalledBy INT NULL CONSTRAINT FK_Announcement_RecalledBy REFERENCES [User](UserID),
        CONSTRAINT CK_Announcement_TargetType CHECK (TargetType IN ('All', 'Staff', 'Customer', 'SingleCustomer')),
        CONSTRAINT CK_Announcement_Status CHECK (Status IN ('Sent', 'Recalled')),
        CONSTRAINT CK_Announcement_TargetUser CHECK (
            (TargetType = 'SingleCustomer' AND TargetUserID IS NOT NULL)
            OR (TargetType <> 'SingleCustomer' AND TargetUserID IS NULL)),
        CONSTRAINT CK_Announcement_Type CHECK (NotificationType IN ('General', 'Movie', 'Internal', 'System'))
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Announcement_CreatedAt' AND object_id = OBJECT_ID('Announcement'))
    CREATE INDEX IX_Announcement_CreatedAt ON Announcement (CreatedAt DESC);
GO
IF COL_LENGTH('CustomerNotification', 'AnnouncementID') IS NULL
    ALTER TABLE CustomerNotification ADD AnnouncementID INT NULL
        CONSTRAINT FK_CustomerNotification_Announcement REFERENCES Announcement(AnnouncementID);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_CustomerNotification_Announcement' AND object_id = OBJECT_ID('CustomerNotification'))
    CREATE INDEX IX_CustomerNotification_Announcement ON CustomerNotification (AnnouncementID);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_CustomerNotification_User_Read' AND object_id = OBJECT_ID('CustomerNotification'))
    CREATE INDEX IX_CustomerNotification_User_Read ON CustomerNotification (UserID, ReadAt);
GO
DECLARE @eventCheck SYSNAME, @eventSql NVARCHAR(400);
SELECT TOP (1) @eventCheck = name
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID('ActivityLog')
    AND definition LIKE '%EventType%'
    AND name <> 'CK_ActivityLog_EventType';
IF @eventCheck IS NOT NULL
BEGIN
    SET @eventSql = N'ALTER TABLE ActivityLog DROP CONSTRAINT ' + QUOTENAME(@eventCheck);
    EXEC sp_executesql @eventSql;
END
GO
IF EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_ActivityLog_EventType' AND parent_object_id = OBJECT_ID('ActivityLog')
        AND definition NOT LIKE '%NOTIFICATION%'
)
    ALTER TABLE ActivityLog DROP CONSTRAINT CK_ActivityLog_EventType;
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_ActivityLog_EventType' AND parent_object_id = OBJECT_ID('ActivityLog')
)
    ALTER TABLE ActivityLog ADD CONSTRAINT CK_ActivityLog_EventType
        CHECK (EventType IN ('AUTH', 'ACCOUNT', 'CONFIG', 'CATALOG', 'BOOKING', 'COUNTER', 'SYSTEM', 'NOTIFICATION'));
GO
