USE CinemaDB;
GO

-- 1. Insert User
INSERT INTO [User] (FullName, Email, Phone, PasswordHash, Role, PointBalance, Status) VALUES
(N'Nguyễn Văn Admin', 'admin@cinema.vn', '0901234567', 'hashed_pwd', 'Admin', 0, 'Active'),
(N'Trần Thị Quản Lý', 'manager@cinema.vn', '0912345678', 'hashed_pwd', 'Manager', 0, 'Active'),
(N'Lê Văn Nhân Viên', 'staff@cinema.vn', '0923456789', 'hashed_pwd', 'Staff', 0, 'Active'),
(N'Phạm Tùng Dương', 'duongpt@gmail.com', '0934567890', 'hashed_pwd', 'Customer', 150, 'Active');

-- 2. Insert Genre
INSERT INTO Genre (GenreName) VALUES ('Action'), ('Sci-Fi'), ('Drama'), ('Comedy');

-- 3. Insert Movie
INSERT INTO Movie (Title, Synopsis, Director, Cast, DurationMinutes, AgeRating, ReleaseDate, PosterURL, TrailerURL, Status) VALUES
(N'Lật Mặt 7: Một Điều Ước', N'Phim tâm lý, tình cảm gia đình...', N'Lý Hải', N'Đinh Y Nhung', 138, 'P', '2026-04-26', 'poster_lm7.jpg', 'trailer_lm7.mp4', 'Now Showing'),
(N'Dune: Hành Tinh Cát', N'Phim viễn tưởng...', N'Denis Villeneuve', N'Timothée Chalamet', 166, 'C13', '2026-05-15', 'poster_dune.jpg', 'trailer_dune.mp4', 'Coming Soon');

-- 4. Insert Movie_Genre (Lật mặt 7: Drama, Comedy | Dune: Action, Sci-Fi)
INSERT INTO Movie_Genre (MovieID, GenreID) VALUES (1, 3), (1, 4), (2, 1), (2, 2);

-- 5. Insert Room
INSERT INTO Room (RoomName, Format, TotalSeats, Status) VALUES 
(N'Phòng chiếu 1', '2D', 100, 'Active'), 
(N'Phòng chiếu 2', '3D', 80, 'Active');

-- 6. Insert Seat (Tạo mẫu vài ghế cho Phòng 1)
INSERT INTO Seat (RoomID, RowName, ColNumber, SeatType, Status) VALUES
(1, 'A', 1, 'Standard', 'Active'),
(1, 'A', 2, 'Standard', 'Active'),
(1, 'D', 1, 'VIP', 'Active'),
(1, 'D', 2, 'VIP', 'Active'),
(1, 'H', 1, 'Sweetbox', 'Active'),
(1, 'H', 2, 'Sweetbox', 'Maintenance'); -- Một ghế đôi đang bảo trì

-- 7. Insert Showtime (Suất chiếu)
INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status) VALUES
(1, 1, '2026-10-04 18:00:00', '2026-10-04 20:33:00', 80000, 'Active'), -- Đã cộng 15p dọn dẹp
(2, 2, '2026-10-05 20:00:00', '2026-10-05 23:01:00', 120000, 'Active');

-- 8. Insert Seat_Hold (Giả lập có 1 khách đang ở màn hình thanh toán giữ ghế A1)
INSERT INTO Seat_Hold (SessionID, UserID, ShowtimeID, SeatID, HoldTime, ExpiryTime) VALUES
('session_xyz_123', 4, 1, 1, GETDATE(), DATEADD(MINUTE, 10, GETDATE()));

-- 9. Insert Voucher
INSERT INTO Voucher (VoucherCode, DiscountValue, DiscountType, ExpiryDate, OwnerUserID, Status) VALUES 
('GIAM20K', 20000, 'Fixed Amount', '2026-12-31', NULL, 'Active'),
('REFUND_ABC', 64000, 'Fixed Amount', '2026-11-30', 4, 'Active'); -- Voucher hoàn vé 80% của User 4

-- 10. Insert Concession
INSERT INTO Concession (Name, Price, StockQuantity, Status) VALUES
(N'Combo Bắp Nước Nhỏ', 65000, 100, 'Active'),
(N'Combo VIP', 95000, 50, 'Active');

-- 11. Insert Booking (Một đơn bán qua QUẦY bởi Nhân viên, khách vãng lai không có tài khoản)
INSERT INTO Booking (CustomerID, StaffID, ShowtimeID, VoucherID, TotalPrice, DiscountAmount, FinalAmount, PaymentMethod, BookingTime, Status) VALUES 
(NULL, 3, 1, NULL, 145000, 0, 145000, 'Cash', GETDATE(), 'Paid');

-- 12. Insert Ticket (Vé của Booking trên - Ghế A2)
INSERT INTO Ticket (BookingID, SeatID, Price, TicketCode, QRCode, Status) VALUES 
(1, 2, 80000, 'TK-A2-123', 'url_qr_code_A2', 'Upcoming');

-- 13. Insert Booking_Concession (Bắp nước của Booking trên)
INSERT INTO Booking_Concession (BookingID, ConcessionID, Quantity, SubtotalPrice) VALUES 
(1, 1, 1, 65000);

-- 14. Insert Equipment_Report (Nhân viên báo cáo hỏng điều hòa phòng 2)
INSERT INTO Equipment_Report (StaffID, RoomID, EquipmentCategory, Description, UrgencyLevel, Status, ReportDate) VALUES
(3, 2, 'Air Conditioning', N'Điều hòa tỏa ra hơi nóng, khách kêu rất nhiều.', 'High', 'Pending', GETDATE());

GO