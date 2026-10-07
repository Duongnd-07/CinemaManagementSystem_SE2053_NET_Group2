
-- 1. Chuyển kết nối ra khỏi CinemaDB (về master)
USE master;
GO

-- 2. Kiểm tra nếu CinemaDB đã tồn tại thì ép ngắt kết nối và xóa
IF EXISTS (SELECT name FROM sys.databases WHERE name = N'CinemaDB')
BEGIN
    -- Ép ngắt các kết nối đang tồn tại (Single-user mode)
    ALTER DATABASE CinemaDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    
    -- Xóa database
    DROP DATABASE CinemaDB;
END
GO

-- 3. Tạo lại database hoàn toàn mới
CREATE DATABASE CinemaDB;
GO

-- 4. Trỏ vào database vừa tạo để chuẩn bị chạy các lệnh tiếp theo
USE CinemaDB;
GO
-- 1. Bảng User (Tài khoản người dùng & nhân viên)
CREATE TABLE [User] (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    FullName NVARCHAR(100) NOT NULL,
    Email VARCHAR(100) UNIQUE NOT NULL,
    Phone VARCHAR(20) UNIQUE NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Role VARCHAR(20) CHECK (Role IN ('Admin', 'Manager', 'Staff', 'Customer')) NOT NULL,
    PointBalance INT DEFAULT 0,
    Status VARCHAR(20) CHECK (Status IN ('Active', 'Suspended', 'Deactivated')) DEFAULT 'Active'
);

-- 2. Bảng Genre (Thể loại phim)
CREATE TABLE Genre (
    GenreID INT IDENTITY(1,1) PRIMARY KEY,
    GenreName NVARCHAR(50) UNIQUE NOT NULL
);

-- 3. Bảng Movie (Phim)
CREATE TABLE Movie (
    MovieID INT IDENTITY(1,1) PRIMARY KEY,
    Title NVARCHAR(200) UNIQUE NOT NULL,
    Synopsis NVARCHAR(MAX),
    Director NVARCHAR(100),
    Cast NVARCHAR(500),
    DurationMinutes INT CHECK (DurationMinutes > 0),
    AgeRating VARCHAR(10) CHECK (AgeRating IN ('P', 'C13', 'C16', 'C18')),
    ReleaseDate DATE,
    PosterURL VARCHAR(500),
    TrailerURL VARCHAR(500),
    Status VARCHAR(20) CHECK (Status IN ('Now Showing', 'Coming Soon', 'Ended', 'Hidden'))
);

-- 4. Bảng Movie_Genre (Chi tiết Thể loại của Phim)
CREATE TABLE Movie_Genre (
    MovieID INT FOREIGN KEY REFERENCES Movie(MovieID),
    GenreID INT FOREIGN KEY REFERENCES Genre(GenreID),
    PRIMARY KEY (MovieID, GenreID)
);

-- 5. Bảng Room (Phòng chiếu)
CREATE TABLE Room (
    RoomID INT IDENTITY(1,1) PRIMARY KEY,
    RoomName NVARCHAR(50) UNIQUE NOT NULL,
    Format VARCHAR(20) CHECK (Format IN ('2D', '3D', 'IMAX')),
    TotalSeats INT NOT NULL,
    Status VARCHAR(20) CHECK (Status IN ('Active', 'Maintenance')) DEFAULT 'Active'
);

-- 6. Bảng Seat (Ghế)
CREATE TABLE Seat (
    SeatID INT IDENTITY(1,1) PRIMARY KEY,
    RoomID INT FOREIGN KEY REFERENCES Room(RoomID),
    RowName VARCHAR(5) NOT NULL, 
    ColNumber INT NOT NULL,      
    SeatType VARCHAR(20) CHECK (SeatType IN ('Standard', 'VIP', 'Sweetbox')),
    Status VARCHAR(20) CHECK (Status IN ('Active', 'Maintenance')) DEFAULT 'Active',
    UNIQUE (RoomID, RowName, ColNumber) -- Không thể có 2 ghế A1 trong cùng 1 phòng
);

-- 7. Bảng Showtime (Suất chiếu)
CREATE TABLE Showtime (
    ShowtimeID INT IDENTITY(1,1) PRIMARY KEY,
    MovieID INT FOREIGN KEY REFERENCES Movie(MovieID),
    RoomID INT FOREIGN KEY REFERENCES Room(RoomID),
    StartTime DATETIME NOT NULL,
    EndTime DATETIME NOT NULL, -- Đã gồm 15p dọn phòng
    BasePrice DECIMAL(18,2) NOT NULL,
    Status VARCHAR(20) CHECK (Status IN ('Active', 'Cancelled', 'Ended')) DEFAULT 'Active'
);

-- 8. Bảng Seat_Hold (Giữ ghế tạm thời - Dành cho lúc Online Checkout)
CREATE TABLE Seat_Hold (
    HoldID INT IDENTITY(1,1) PRIMARY KEY,
    SessionID VARCHAR(100) NOT NULL, -- Token hoặc ID phiên của khách
    UserID INT FOREIGN KEY REFERENCES [User](UserID) NULL,
    ShowtimeID INT FOREIGN KEY REFERENCES Showtime(ShowtimeID),
    SeatID INT FOREIGN KEY REFERENCES Seat(SeatID),
    HoldTime DATETIME DEFAULT GETDATE(),
    ExpiryTime DATETIME NOT NULL, -- Thường là HoldTime + 10 phút
    UNIQUE (ShowtimeID, SeatID) -- Tránh 2 người cùng giữ 1 ghế
);

-- 9. Bảng Voucher
CREATE TABLE Voucher (
    VoucherID INT IDENTITY(1,1) PRIMARY KEY,
    VoucherCode VARCHAR(50) UNIQUE NOT NULL,
    DiscountValue DECIMAL(18,2) NOT NULL,
    DiscountType VARCHAR(20) CHECK (DiscountType IN ('Percent', 'Fixed Amount')),
    ExpiryDate DATETIME NOT NULL,
    OwnerUserID INT FOREIGN KEY REFERENCES [User](UserID) NULL,
    Status VARCHAR(20) CHECK (Status IN ('Active', 'Used', 'Expired')) DEFAULT 'Active'
);

-- 10. Bảng Booking (Giao dịch Mua vé)
CREATE TABLE Booking (
    BookingID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT FOREIGN KEY REFERENCES [User](UserID) NULL, -- NULL nếu khách vãng lai mua tại quầy
    StaffID INT FOREIGN KEY REFERENCES [User](UserID) NULL,    -- NULL nếu khách tự đặt online
    ShowtimeID INT FOREIGN KEY REFERENCES Showtime(ShowtimeID),
    VoucherID INT FOREIGN KEY REFERENCES Voucher(VoucherID) NULL,
    TotalPrice DECIMAL(18,2) NOT NULL,
    DiscountAmount DECIMAL(18,2) DEFAULT 0,
    FinalAmount DECIMAL(18,2) NOT NULL,
    PaymentMethod VARCHAR(20) CHECK (PaymentMethod IN ('Cash', 'VNPay', 'SePay')),
    BookingTime DATETIME DEFAULT GETDATE(),
    Status VARCHAR(20) CHECK (Status IN ('Pending', 'Paid', 'Cancelled'))
);

-- 11. Bảng Ticket (Vé chi tiết cho từng ghế)
CREATE TABLE Ticket (
    TicketID INT IDENTITY(1,1) PRIMARY KEY,
    BookingID INT FOREIGN KEY REFERENCES Booking(BookingID),
    SeatID INT FOREIGN KEY REFERENCES Seat(SeatID),
    Price DECIMAL(18,2) NOT NULL,
    TicketCode VARCHAR(50) UNIQUE NOT NULL,
    QRCode VARCHAR(500),
    Status VARCHAR(20) CHECK (Status IN ('Upcoming', 'Watched', 'Cancelled')) DEFAULT 'Upcoming'
);

-- 12. Bảng Concession (Mặt hàng Bắp Nước)
CREATE TABLE Concession (
    ConcessionID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(100) UNIQUE NOT NULL,
    Price DECIMAL(18,2) NOT NULL,
    StockQuantity INT CHECK (StockQuantity >= 0),
    Status VARCHAR(20) CHECK (Status IN ('Active', 'Out of Stock', 'Hidden')) DEFAULT 'Active'
);

-- 13. Bảng Booking_Concession (Bắp nước bán trong đơn hàng)
CREATE TABLE Booking_Concession (
    BookingID INT FOREIGN KEY REFERENCES Booking(BookingID),
    ConcessionID INT FOREIGN KEY REFERENCES Concession(ConcessionID),
    Quantity INT CHECK (Quantity > 0),
    SubtotalPrice DECIMAL(18,2) NOT NULL,
    PRIMARY KEY (BookingID, ConcessionID)
);

-- 14. Bảng Review (Đánh giá)
CREATE TABLE Review (
    ReviewID INT IDENTITY(1,1) PRIMARY KEY,
    MovieID INT FOREIGN KEY REFERENCES Movie(MovieID),
    UserID INT FOREIGN KEY REFERENCES [User](UserID),
    Rating INT CHECK (Rating BETWEEN 1 AND 5),
    Comment NVARCHAR(1000),
    CreatedAt DATETIME DEFAULT GETDATE(),
    UNIQUE(MovieID, UserID) -- Đảm bảo 1 người chỉ review 1 lần / 1 phim
);

-- 15. Bảng Equipment_Report (Báo cáo sự cố thiết bị)
CREATE TABLE Equipment_Report (
    ReportID INT IDENTITY(1,1) PRIMARY KEY,
    StaffID INT FOREIGN KEY REFERENCES [User](UserID), -- Người báo cáo
    RoomID INT FOREIGN KEY REFERENCES Room(RoomID),    -- Xảy ra ở phòng nào
    EquipmentCategory VARCHAR(50) CHECK (EquipmentCategory IN ('Projector', 'Sound System', 'Air Conditioning', 'Seat', 'Other')),
    Description NVARCHAR(MAX) NOT NULL,
    UrgencyLevel VARCHAR(20) CHECK (UrgencyLevel IN ('Low', 'Medium', 'High', 'Critical')),
    Status VARCHAR(20) CHECK (Status IN ('Pending', 'In Progress', 'Resolved')) DEFAULT 'Pending',
    ReportDate DATETIME DEFAULT GETDATE(),
    ResolvedDate DATETIME NULL
);
GO