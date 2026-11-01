# Requirements Traceability Matrix (RTM) Guide

The Requirements Traceability Matrix (RTM) proves bidirectional traceability between high-level requirements, software architecture components, database tables, and test cases.

---

## 1. Standard RTM Format for Cinema Management System

| UC ID | Use Case Name | Primary Actor | Controller / Servlet | Service Layer | DAO Layer | DB Tables Involved | Business Rules |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **UC-01** | Verify Email OTP | Guest / Customer | `vn.edu.fpt.controller.auth.VerifyOtpServlet` | `AuthService`, `EmailService` | `EmailVerificationDAO`, `UserDAO` | `Users`, `EmailVerifications` | — |
| **UC-05** | Movie Search & Filter | Guest / Customer | `vn.edu.fpt.controller.customer.MovieSearchServlet` | `MovieService` | `MovieDAO`, `GenreDAO` | `Movies`, `Genres`, `MovieGenres` | `GB-11` |
| **UC-09** | View & Select Seats | Customer | `vn.edu.fpt.controller.customer.SeatMapServlet` | `SeatMapService`, `SeatHoldService` | `SeatMapDAO`, `SeatDAO` | `Seats`, `SeatHolds`, `Rooms`, `Showtimes` | `GB-01`, `GB-02` |
| **UC-11** | Customer Loyalty Points | Customer | `vn.edu.fpt.controller.customer.LoyaltyServlet` | `LoyaltyService` | `LoyaltyDAO` | `Users`, `LoyaltyPointHistory` | `GB-04`, `GB-12` |
| **UC-13** | Online Ticket Booking | Customer | `vn.edu.fpt.controller.customer.BookingServlet` | `BookingService`, `TicketPricing` | `BookingDAO`, `SeatDAO`, `ConcessionDAO` | `Bookings`, `Tickets`, `BookingDetails` | `GB-01`, `GB-03`, `GB-05`, `GB-10` |
| **UC-14** | Make Online Payment | Customer | `vn.edu.fpt.controller.customer.PaymentReturnServlet` | `PaymentService`, `VNPayAdapter` | `PaymentDAO`, `BookingDAO` | `Payments`, `Bookings`, `Tickets`, `Transactions` | `GB-01`, `GB-10` |
| **UC-18** | Showtime Email Reminder | System / Scheduler | `vn.edu.fpt.listener.AppBackgroundListener` | `ShowtimeReminderService`, `EmailService` | `ShowtimeReminderDAO`, `BookingDAO` | `Bookings`, `Showtimes`, `Users` | `JOB-02` |
| **UC-19** | POS Counter Sale | Staff | `vn.edu.fpt.controller.staff.CounterSaleServlet` | `CounterSaleService`, `TicketPricing` | `CounterSaleDAO`, `SeatDAO`, `TicketDAO` | `Bookings`, `Tickets`, `Concessions` | `GB-01`, `GB-02`, `GB-10` |
| **UC-23** | Manage Movies Lifecycle | Manager | `vn.edu.fpt.controller.manager.MovieServlet` | `MovieService` | `MovieDAO` | `Movies` | `GB-03`, `GB-11`, `GB-12`, `JOB-03` |
| **UC-25** | Room & Sweetbox Editor | Manager | `vn.edu.fpt.controller.manager.RoomEditorServlet` | `RoomService`, `SeatLayoutService` | `RoomDAO`, `RoomLayoutDAO`, `SeatDAO` | `Rooms`, `Seats` | `Sweetbox Rule` |
| **UC-28** | Manage Vouchers | Manager | `vn.edu.fpt.controller.manager.VoucherServlet` | `VoucherService` | `VoucherDAO` | `Vouchers` | `GB-05`, `GB-06` |
| **UC-29** | Occupancy & Revenue Report | Manager | `vn.edu.fpt.controller.manager.ReportServlet` | `AnalyticsService` | `AnalyticsDAO` | `Bookings`, `Tickets`, `Rooms` | — |
| **UC-30** | User & RBAC Management | Admin | `vn.edu.fpt.controller.admin.UserManagementServlet` | `UserManagementService` | `UserDAO` | `Users`, `Roles` | `GB-12` |
| **UC-32** | View System Activity Logs | Admin | `vn.edu.fpt.controller.admin.SystemLogServlet` | `ActivityLogger` | `ActivityLogDAO` | `ActivityLogs` | `GB-12` |

---

## 2. Reverse-Auditing Checklist for RTM

When auditing or adding rows to the RTM:
1. Verify the Controller class actually exists under `vn.edu.fpt.controller.*`.
2. Verify the `@WebServlet` path corresponds to the actor's permission scope.
3. Verify the DAO methods call the stated database tables.
4. Verify all relevant business rules (`GB-xx`) are tagged.
