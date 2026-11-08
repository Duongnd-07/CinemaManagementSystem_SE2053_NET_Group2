# Workflow Use Case Specification Input (WF06-WF11)

> Mục đích: tài liệu đầu vào cho AI chuyên dụng khác vẽ sơ đồ Use Case.
> Phạm vi: tổng hợp theo mã WF06-WF11 từ ảnh yêu cầu và đối chiếu với implementation hiện tại.
> Ngôn ngữ sơ đồ: tiếng Việt.

## 1. System and actors

### System boundary

Tên hệ thống: `Cinema Management System`

Sơ đồ tổng thể nên đặt tất cả actor bên ngoài hệ thống và các use case bên trong hệ thống.

### Actors

| Actor | Vai trò trong workflow |
|---|---|
| Guest | Chỉ xem thông tin công khai; không tạo đánh giá hoặc giao dịch cần tài khoản. |
| Customer | Viết, sửa, xóa đánh giá; mua vé online; hủy vé; xem ví voucher và lịch sử giao dịch. |
| Staff | Mở ca, bán vé/bắp nước tại quầy, lập hóa đơn, báo sự cố, kiểm tra phòng chiếu. |
| Manager | Kiểm duyệt đánh giá; xử lý/tổng hợp sự cố phòng; xem dashboard doanh thu và hiệu suất. |
| Admin | Thiết lập quyền truy cập theo vai trò và cập nhật tham số cấu hình hệ thống. |
| Payment Gateway | Cổng thanh toán bên thứ ba cho giao dịch online (VNPay). |
| System / Scheduler | Chạy tác vụ nền: nhả ghế hết hạn, đồng bộ trạng thái phim/suất chiếu và ghi log. |

### Actor inheritance / overlap

- `Manager` kế thừa quyền truy cập khu vực quản lý như `Staff` khi hệ thống cho phép.
- `System / Scheduler` không phải người dùng đăng nhập.
- `Payment Gateway` chỉ tương tác với quy trình thanh toán online, không xuất hiện trong quy trình bán tại quầy.

## 2. Use Case packages

### UC IDs actually covered by WF06-WF11

| Workflow | Primary UC | Related UC | Main implementation |
|---|---|---|---|
| WF06 Movie Review | UC-17 | UC-12/UC-29 | `/customer/reviews`, `/movies/detail`, `/manager/reviews` |
| WF07 Counter Sales | UC-19 | UC-22, UC-28 | `/staff/counter`, `/staff/counter-history`, `/staff/shifts` |
| WF08 Room Incident | UC-21, UC-35 | UC-36 | `/staff/equipment-reports`, `/staff/room-inspections`, `/manager/equipment-reports`, `/manager/room-inspections` |
| WF09 Access Control | UC-02, UC-31 | UC-32 | `/login`, `/admin/permissions` |
| WF10 Configuration | UC-33 | UC-32 | `/admin/configurations` |
| WF11 Revenue Reporting | UC-13, UC-19, UC-12 | UC-29 | `/customer/cancel-ticket`, `/manager/reports`, `/staff/counter` |

## 3. WF06 - Movie Review

### Use case list

- `UC17-01 Xem đánh giá phim`
- `UC17-02 Gửi đánh giá phim`
- `UC17-03 Sửa đánh giá của mình`
- `UC17-04 Xóa đánh giá của mình`
- `UC17-05 Kiểm duyệt đánh giá phim`
- `UC17-06 Ẩn đánh giá vi phạm`
- `UC17-07 Bỏ ẩn đánh giá`

### Actors and connections

- Customer:
  - uses `UC17-01`.
  - uses `UC17-02`, `UC17-03`, `UC17-04` if the eligibility check passes.
- Manager:
  - uses `UC17-05`, `UC17-06`, `UC17-07`.
- System / Scheduler:
  - associated indirectly with review data consistency/activity logging; not the primary actor.

### Normal flow

1. Customer opens a movie detail or ended-movie review page.
2. System checks the movie status and loads visible reviews, rating summary, filter and sorting options.
3. System checks whether Customer has watched the movie.
4. Customer enters rating from 1 to 5 and a comment of 10-1000 characters.
5. System validates eligibility and input.
6. System creates the review or updates the existing visible review of Customer.
7. System recalculates the movie rating average and returns to the review page.

### Alternative / exceptional flows

- Customer filters reviews by star or sorts by newest/highest/lowest.
- The customer submits again while a review already exists: update the existing review.
- Customer has not watched the movie: reject review submission.
- Rating is outside 1-5, comment is empty/too short/too long: keep the draft and show errors.
- Manager hides a review that violates policy and records a reason.
- A hidden review is locked so the Customer cannot edit or resend it.
- Customer deletes their own visible review.
- Two tabs submit simultaneously: detect the database unique constraint and update instead of creating a duplicate.

### Include / extend

- `UC17-02`, `UC17-03` include `Kiểm tra quyền đánh giá` and `Xác thực dữ liệu đánh giá`.
- `UC17-06`, `UC17-07` extend `Kiểm duyệt đánh giá`.
- `UC17-03`, `UC17-04` extend the review owned by the authenticated Customer.

### Evidence in code

- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/customer/MovieReviewServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/manager/ReviewManagementServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/ReviewService.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/dao/ReviewDAO.java`

## 4. WF07 - Counter Sales

### Use case list

- `UC19-01 Mở ca bán hàng`
- `UC19-02 Chọn suất chiếu`
- `UC19-03 Chọn ghế`
- `UC19-04 Chọn bắp nước`
- `UC19-05 Tra cứu thành viên`
- `UC19-06 Áp dụng voucher/điểm`
- `UC19-07 Báo giá đơn tại quầy`
- `UC19-08 Xác nhận thu tiền và tạo vé`
- `UC19-09 Xem hóa đơn`
- `UC22-01 Quản lý báo cáo ca`
- `UC22-02 Đóng ca và đếm tiền`

### Actors and connections

- Staff:
  - uses all UC19 counter-sales use cases.
  - uses UC22 shift/report use cases.
- Customer:
  - participant receiving the ticket/receipt; Customer may also be identified by Staff using email/phone.
- Manager:
  - uses UC22/UC19 counter-history reporting if granted permission.

### Normal flow

1. Staff opens a counter shift and enters counter number/opening cash.
2. System checks that the shift is still active.
3. Staff selects a sellable showtime.
4. System loads live seat availability and seat prices.
5. Staff selects seats and concessions.
6. Staff optionally identifies a Customer and enters voucher/loyalty points.
7. Staff chooses cash, transfer or POS; for cash the system calculates change.
8. System calculates final price.
9. On confirmation, system creates a paid booking and tickets in one transaction, reduces concession stock, grants loyalty points and saves the shift reference.
10. System displays the counter receipt.

### Alternative / exceptional flows

- Staff has not opened a shift: block sale.
- Shift is expired or closed: require a new shift.
- Selected seats are sold or held by another transaction: roll back the confirmation and ask Staff to recalculate.
- Transfer reference is duplicated: require recalculation.
- Cash received is less than the total: show amount missing.
- A Customer has no valid member account: reject voucher or loyalty-point usage.
- Restricted movie requires age confirmation.
- Voucher or points exceed/are invalid for the bill: keep the form values and show field errors.
- A transaction fails: do not create booking/tickets with partial data.

### Include / extend

- `UC19-08` includes `UC19-07 Báo giá` and `Ghi nhận thanh toán tại quầy`.
- `UC19-08` includes transaction operations for Booking, Ticket, Concession and Loyalty.
- `UC22-02` extends the counter shift/report workflow.
- `Áp dụng voucher/điểm` is optional in the counter sale flow.

### Evidence in code

- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/staff/CounterSaleServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/CounterSaleService.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/ShiftService.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/dao/CounterSaleDAO.java`

## 5. WF08 - Room Incidents

### Use case list

- `UC21-01 Báo cáo sự cố thiết bị/ghế`
- `UC21-02 Xem danh sách báo cáo sự cố của nhân viên`
- `UC35-01 Kiểm tra phòng chiếu`
- `UC35-02 Cập nhật kết quả kiểm tra`
- `UC35-03 Hoàn tác kết quả kiểm tra`
- `UC35-04 Liên kết báo cáo sự cố với lần kiểm tra`
- `UC35-05 Xem vé bị ảnh hưởng bởi ghế lỗi`
- `UC35-06 Xem lịch sử kiểm tra phòng`
- `UC36-01 Mở phòng sạch/không sạch theo kết quả kiểm tra`

### Actors and connections

- Staff:
  - reports equipment/seat issues.
  - performs room inspection and may update/undo the checklist.
- Manager:
  - views incident reports and inspection history.
  - monitors room status, affected shows and pending incidents.
- System / Scheduler:
  - can automatically lock a room or seats when incident thresholds are exceeded.
- Customer:
  - may be affected by seat locking and compensation, but does not initiate WF08.

### Normal flow

1. Staff selects room, seat/equipment category and urgency and submits an incident report.
2. System validates the report and stores it.
3. System evaluates whether the room should be auto-locked based on broken equipment/seat thresholds.
4. Before opening the selected showtime, Staff opens the inspection checklist.
5. Staff marks seat, cleaning and other equipment status and enters a note.
6. Staff may link pending incident reports and review the number of affected tickets.
7. System saves the room inspection and records Staff/System activity.
8. Manager can review the report history and room inspection history.

### Alternative / exceptional flows

- Staff starts inspection from an equipment report: prefill room/category.
- Equipment status is not normal: keep pending incidents visible for linking.
- Inspection data is invalid: keep the draft and show errors.
- Staff updates an existing inspection instead of creating a new one.
- Staff undoes the latest inspection to return the slot to unchecked.
- Room has too many broken seats/equipment: system suggests or performs room lock according to implemented rules.
- Manager sees affected tickets and resolves/report status through the management workflow.

### Include / extend

- `UC35-01` includes `Kiểm tra ghế` and optionally `Liên kết báo cáo sự cố`.
- `UC35-02` extends `UC35-01`.
- `UC35-03` extends `UC35-01`.
- `UC21-01` includes automatic room-lock evaluation.
- `UC35-05` is optional when the selected room has affected seats.

### Evidence in code

- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/staff/EquipmentReportServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/staff/RoomInspectionServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/manager/EquipmentReportManagementServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/manager/RoomInspectionHistoryServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/RoomInspectionService.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/EquipmentReportService.java`

## 6. WF09 - Access Control

### Use case list

- `UC02-01 Đăng nhập`
- `UC02-02 Xác thực phiên đăng nhập`
- `UC02-03 Phân hướng theo vai trò`
- `UC31-01 Xem cấu hình phân quyền`
- `UC31-02 Cấp hoặc thu hồi quyền theo vai trò`
- `UC32-01 Ghi nhật ký thay đổi quyền`
- Account inactive or role/password changed: revoke session.

### Actors and connections

- Admin uses UC31-01 and UC31-02.
- Customer/Staff/Manager/Admin use UC02-01.
- System / Scheduler and `AuthFilter` are technical actors supporting session and permission enforcement.
- No separate guest permission in protected areas; unauthenticated users are redirected to login.

### Normal flow

1. User submits email/identifier and password.
2. System validates credentials, active status and CSRF token.
3. System changes the session ID, stores the User in session and logs the successful login.
4. System redirects the user to a return URL or role-specific default page.
5. Admin opens role/permission management.
6. System loads configurable permissions grouped by role.
7. Admin grants or revokes assignable permissions.
8. System commits changes, clears the permission cache and writes activity logs.
9. AuthFilter rechecks every request against role prefixes and permission mappings.

### Alternative / exceptional flows

- Invalid credentials or inactive/locked account: show authentication error and log failed attempt.
- CSRF token is invalid: reject the request.
- Session points to a deleted account, inactive account, changed role or changed password hash: invalidate session.
- Invalid permission key is submitted: reject the whole change set.
- No permission changes: show no-change message.
- A permission is locked for a role: do not allow changes.
- Permission data is cached for 30 seconds; database changes take effect after cache expiration unless saved through the permission page.

### Role-to-area rules

- `/admin/*`: Admin only.
- `/manager/*`: Manager only.
- `/staff/*`: Staff, and Manager where permitted by the role implementation.
- `/customer/*`: authenticated Customer only.
- Manager/Staff functional permissions are checked additionally against `Permission.fromPath(...)`.

### Include / extend

- `UC02-01` includes `Kiểm tra CSRF`, `Xác thực tài khoản` and `Khởi tạo phiên`.
- `UC31-02` includes `Xác thực danh sách quyền` and `UC32-01`.
- `AuthFilter` extends every protected use case through session and RBAC checks.

### Evidence in code

- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/auth/LoginServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/filter/AuthFilter.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/admin/RolePermissionServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/RolePermissionService.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/model/Permission.java`

## 7. WF10 - Configuration

### Use case list

- `UC33-01 Xem tham số cấu hình`
- `UC33-02 Thay đổi cấu hình hệ thống`
- `UC33-03 Xác thực phạm vi cấu hình`
- `UC33-04 Áp dụng cấu hình cho nghiệp vụ`
- `UC32-01 Ghi nhật ký thay đổi cấu hình`

### Actors and connections

- Admin uses UC33-01 through UC33-03.
- System / Scheduler and application services consume the saved configuration values.
- Manager uses related configurations as read-only inputs for analytics thresholds, subject to implemented default/config behavior.

### Normal flow

1. Admin opens the system configuration page.
2. System groups editable parameters by booking, cancellation voucher, welcome voucher, broken-seat voucher and loyalty.
3. Admin changes one or more allowed parameter values.
4. System validates type, range, decimal precision and rules.
5. System saves changed values in one transaction and writes each change to activity logs.
6. Subsequent booking/cancellation/review/loyalty workflows read the applied values.
7. System returns to the configuration page via PRG and shows the number of changed parameters.

### Config groups represented in the diagram

- Booking: seat hold time, maximum seats, online sale cutoff.
- Cancellation voucher: minimum cancellation hours, refund percentage, voucher validity.
- Welcome voucher: amount, validity, minimum order.
- Broken-seat compensation voucher: amount and validity.
- Loyalty: point earning percentage and point exchange value.

### Alternative / exceptional flows

- Invalid CSRF: reject request.
- Value is outside the configured range: keep entered values, show field-specific errors and save nothing.
- Database error/transaction failure: keep configuration unchanged.
- No parameter value changed: show no-change message.
- Admin attempts to add/delete/rename a key: the fixed enum/config contract does not permit this through the UI.

### Include / extend

- `UC33-02` includes `UC33-03`.
- `UC33-02` includes `UC32-01` for each changed key.
- `UC33-04` extends UC business workflows rather than being a separate manual user action.
- Configuration operations are restricted to Admin by `AuthFilter`.

### Evidence in code

- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/admin/SystemConfigServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/SystemConfigParam.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/SystemConfigService.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/dao/SystemConfigDAO.java`

## 8. WF11 - Revenue Transaction and Reporting

### Use case list

- `UC13-01 Tạo giao dịch đặt vé online`
- `UC13-02 Thanh toán online`
- `UC13-03 Hoàn tất vé sau thanh toán`
- `UC19-01 Tạo giao dịch tại quầy`
- `UC12-01 Kiểm tra điều kiện hủy vé`
- `UC12-02 Xác nhận hủy vé`
- `UC12-03 Hoàn voucher cho vé hủy`
- `UC29-01 Xem dashboard doanh thu`
- `UC29-02 Lọc báo cáo theo ngày/tuần/tháng/năm`
- `UC29-03 So sánh doanh thu online và quầy`
- `UC29-04 Xem công suất phòng`
- `UC29-05 Xem hiệu suất phim`

### Actors and connections

- Customer creates online transactions, pays and cancels eligible tickets.
- Staff creates counter transactions.
- Manager uses the revenue analytics dashboard.
- Payment Gateway participates in online payment verification.
- System / Scheduler is associated with seat hold cleanup and status synchronization.

### Normal flow

1. Customer creates an online booking and holds seats.
2. Payment Gateway is invoked for online checkout.
3. After successful verification, system creates/updates paid booking and ticket records.
4. Staff can create a separate paid transaction at the counter through WF07.
5. Both channels produce sales records with booking channel.
6. Manager opens the analytics dashboard.
7. System calculates day/week/month/year period and queries online/counter revenue series, occupancy by room, sales summary and movie performance.
8. Dashboard shows totals and separates `online` and `at counter`.

### Cancellation flow

1. Customer opens a ticket cancellation request.
2. System verifies ownership and minimum hours before showtime.
3. System calculates the refund voucher percentage and validity.
4. Customer confirms cancellation.
5. System updates ticket/booking status and credits a cancellation voucher.
6. Customer is redirected to booking history with a success/error message.

### Alternative / exceptional flows

- Payment expires or fails: booking stays unpaid or is canceled according to payment flow.
- Payment callback/webhook is invalid: reject the transaction.
- Cancellation window is not yet reached: reject.
- Ticket is not owned by Customer or already canceled: reject.
- Cancellation configuration is invalid/unavailable: use the implemented validation/error path.
- Invalid period/date is supplied to analytics: normalize to month/current date.
- No transactions exist: show zero-value series and empty summaries.
- Manager/Admin permission for `/manager/*` is enforced by AuthFilter.

### Include / extend

- `UC13-02` includes `Xác thực giao dịch với Payment Gateway`.
- Online and counter sales feed the same analytics dashboard.
- `UC12-03 Hoàn voucher` extends `UC12-02 Xác nhận hủy vé`.
- `UC29-02 Lọc kỳ báo cáo` includes `UC29-03` through `UC29-05`.
- Seat hold cleanup is a scheduled system extension used by online booking.

### Evidence in code

- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/customer/BookingCheckoutServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/customer/BookingPaymentServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/customer/BookingPaymentReturnServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/payment/VnPayWebhookServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/customer/TicketCancellationServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/manager/AnalyticsServlet.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/AnalyticsService.java`
- `SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/TicketCancellationService.java`

## 9. Aggregated Actor-Use Case Matrix

| Actor | WF06 | WF07 | WF08 | WF09 | WF10 | WF11 |
|---|---|---|---|---|---|---|
| Customer | Submit/view/edit/delete review | Receive counter sale, member lookup optional | Seat/room impact only | Login | No direct access | Online purchase, cancellation, voucher, dashboard no access |
| Staff | No direct access | Open shift, sell, receipt | Report incident, inspect room, update/undo | Login | No direct access | Counter transaction |
| Manager | Hide/unhide review | View counter history where permitted | View incident/inspection history | Login | No direct access | Revenue dashboard, movie/room performance |
| Admin | No direct access | No direct access | No direct access | Login, manage permissions | Manage configurations | No direct access |
| Payment Gateway | No | No | No | No | No | Verify online payment |
| System / Scheduler | Review consistency support | Transaction support | Auto-lock room/seat | Session/RBAC support | Apply configuration | Seat cleanup/status synchronization/report data |

## 10. Recommended diagram decomposition

To keep diagrams readable, draw six diagrams instead of one overloaded diagram:

1. `WF06_Movie_Review`
2. `WF07_Counter_Sales`
3. `WF08_Room_Incident`
4. `WF09_Access_Control`
5. `WF10_System_Configuration`
6. `WF11_Revenue_Reporting`

Optional one-page context diagram:

- System boundary: `Cinema Management System`.
- Actors: Guest, Customer, Staff, Manager, Admin, Payment Gateway, System/Scheduler.
- Connect each actor only to the primary workflows, not to every internal sub-use-case.

## 11. Diagram drawing rules

1. Show Actor, System Boundary, Use Case ellipse, association, `<<include>>`, `<<extend>>`, and generalization only where specified.
2. Do not draw `Admin`, `Manager`, `Staff`, or `Customer` inside the system boundary.
3. `Payment Gateway` must be an external actor only in online-payment use cases.
4. `System / Scheduler` should be an external system actor, not a human icon.
5. Place CSRF, validation and activity logging as included technical use cases only when they help explain the main flow.
6. Avoid drawing database tables as actors; database storage belongs inside the system boundary.
7. VNPay is a concrete external `Payment Gateway`.
8. For WF11, clearly separate online sales and counter sales before they converge into dashboard reporting.
9. Preserve PRG behavior in the story but do not add extra use cases unless necessary for clarity.
10. Do not invent cancellation refund-to-card; the implemented customer cancellation flow credits a voucher.

