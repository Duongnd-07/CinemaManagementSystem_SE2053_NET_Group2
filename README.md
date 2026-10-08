# Cinema Management System (SE2053 – SWP391 – Group 2)

Hệ thống quản lý rạp chiếu phim: đặt vé online, bán vé tại quầy (POS), quản lý phim/suất chiếu/phòng/ghế, voucher, tích điểm, báo cáo. Chi tiết nghiệp vụ xem tài liệu **Group2_SRS** (các mã `UC-xx`, `BF-xx`, `GB-xx`, `JOB-xx`, `API-xx` trong README này tham chiếu tới SRS).

## 1. Công nghệ

| Hạng mục | Lựa chọn |
|---|---|
| Ngôn ngữ | Java 21 |
| Build | Maven (`SE2053-SWP391-G2/pom.xml`), encoding UTF-8 |
| Web | Java Servlet + JSP (MVC thuần), chạy trên Tomcat |
| CSDL | SQL Server (`cinemaDB`), JDBC `mssql-jdbc` |
| Truy cập dữ liệu | JDBC thuần qua `DBContext` (không dùng ORM) |

## 2. Cấu trúc thư mục

```
SE2053-SWP391-G2/
├─ pom.xml
└─ src/main/
   ├─ java/vn/edu/fpt/
   │  ├─ controller/{auth,customer,staff,manager,admin}   # Servlet theo actor
   │  ├─ service/        # Nghiệp vụ (business rule GB-xx nằm ở đây)
   │  ├─ dao/            # Truy vấn SQL, trả về model
   │  ├─ model/          # Entity (POJO) ánh xạ bảng
   │  ├─ filter/         # Authentication / Authorization (RBAC), encoding
   │  ├─ listener/       # Khởi động Background Job (JOB-01, JOB-02)
   │  └─ util/           # DBContext, helper dùng chung
   ├─ resources/         # ConnectDB.properties, config.properties
   └─ webapp/
      ├─ WEB-INF/views/{auth,common,customer,staff,manager,admin}   # JSP (ẩn khỏi truy cập trực tiếp)
      └─ assets/{css,js,images}
```

Luồng bắt buộc: **JSP → Controller → Service → DAO → DB**. Không được nhảy cách tầng.

| Tầng | Được làm | Không được làm |
|---|---|---|
| Controller | Đọc/validate cú pháp request, gọi Service, set attribute, forward/redirect | Viết SQL, chứa business rule, dùng `Connection` |
| Service | Business rule, tính tiền, điều phối nhiều DAO, quản lý transaction | Dùng `HttpServletRequest/Response`, in HTML |
| DAO | SQL, map `ResultSet` ↔ model | Business rule, gọi Service khác |
| Model | Field + getter/setter | Logic nghiệp vụ, truy cập DB |
| JSP | Hiển thị bằng JSTL/EL | Scriptlet `<% %>`, SQL, logic nghiệp vụ |

## 3. Quy tắc đặt tên

| Đối tượng | Quy tắc | Ví dụ |
|---|---|---|
| Package | chữ thường, `vn.edu.fpt.<tầng>[.<actor>]` | `vn.edu.fpt.controller.customer` |
| Class | `PascalCase`; hậu tố theo tầng | `BookingController`, `BookingService`, `BookingDAO`, `AuthFilter` |
| Model | Danh từ số ít, trùng tên bảng | `Booking`, `SeatHold`, `Movie` |
| Method / biến | `camelCase`, động từ cho method | `findByShowtimeId`, `holdSeats` |
| Hằng số | `UPPER_SNAKE_CASE` | `SEAT_HOLD_MINUTES` |
| Bảng / cột DB | Bảng `PascalCase`; cột `PascalCase` (`BookingID`, `ShowtimeID`) – thống nhất với script DB | `Booking.FinalAmount` |
| Field model ↔ cột | `camelCase` ↔ `PascalCase` | `finalAmount` ↔ `FinalAmount` |
| JSP | `kebab-case.jsp` trong thư mục theo actor | `views/customer/seat-map.jsp` |
| URL | chữ thường, `kebab-case`, tiền tố theo actor | `/customer/booking`, `/staff/pos`, `/admin/users` |
| Webhook API | `POST /api/v1/commerce/payment/webhook/{vnpay\|sepay}` (SRS API-02/03) | |
| CSS/JS | `kebab-case` | `assets/css/seat-map.css` |
| Git branch | `<tên>/<loại>-<mô-tả>` | `duong/feature-login` |

### Quy ước ánh xạ kiểu dữ liệu (theo model hiện có)
- Khóa chính: `int`; khóa ngoại có thể `NULL` → `Integer` (vd. `Booking.customerId`, `staffId`, `voucherId`).
- Tiền tệ: `BigDecimal` (tuyệt đối **không** dùng `double/float`).
- Thời gian: `java.time.LocalDateTime` / `LocalDate`; không dùng `java.util.Date`.
- Trạng thái (status): hằng số/enum thống nhất với SRS, không viết chuỗi "magic" rải rác:
  - Seat: `AVAILABLE | PENDING | SOLD | MAINTENANCE`
  - Booking: `Pending | Paid | Cancelled`; kênh: `Online | Counter`
  - Ticket: `Upcoming | Watched | Exchanged | Cancelled`
  - Movie: `Coming Soon | Now Showing | Ended | Hidden`; tuổi: `P | C13 | C16 | C18`
  - User: `Active | Suspended | Deactivated`; vai trò: `Admin | Manager | Staff | Customer`

## 4. Quy tắc code Java

1. **Format**: indent 4 space, không dùng tab; mỗi file một class public; dòng ≤ 120 ký tự; `{` cùng dòng; luôn dùng `{}` cho `if/for`.
2. **Import**: không `import x.*`; xóa import thừa.
3. **Model**: constructor mặc định + private field + getter/setter (giữ nguyên phong cách `Booking.java`).
4. **DAO**
   - Luôn dùng `PreparedStatement` với tham số `?`. **Cấm** nối chuỗi vào SQL (chống SQL Injection).
   - Dùng `try-with-resources` cho `Connection/PreparedStatement/ResultSet`.
   - Lấy kết nối bằng `DBContext.getConnection()`; riêng nghiệp vụ nhiều bước, Service mở transaction và truyền `Connection` xuống DAO (`setAutoCommit(false)` → `commit/rollback`).
   - Cột nullable đọc bằng `rs.getObject(..., Integer.class)` / kiểm tra `wasNull()`.
5. **Controller**
   - Một Servlet cho mỗi chức năng; dùng `@WebServlet`, tách `doGet` (hiển thị) / `doPost` (xử lý).
   - **POST thành công → redirect (PRG)** để tránh submit lại form.
   - Mọi input từ người dùng phải được validate ở server (không tin JS).
   - View đặt trong `WEB-INF/views` và truy cập qua `RequestDispatcher.forward`.
6. **Xử lý lỗi & log**
   - Không `e.printStackTrace()` / `System.out.println` trong code nghiệp vụ; dùng `java.util.logging` (hoặc logger thống nhất cả nhóm).
   - Không nuốt exception rỗng (`catch (Exception e) {}`). Lỗi hiển thị cho người dùng bằng thông điệp chuẩn (mục 7), không lộ stack trace/SQL.
7. **Comment**: chỉ comment *vì sao* (đặc biệt chỗ gắn với rule SRS, ví dụ `// GB-08: duration + 15 phút dọn phòng`). Không comment lại điều code đã tự nói.
8. **Không code chết**: không để code bị comment-out, `TODO` không có người phụ trách.

## 5. Quy tắc nghiệp vụ bắt buộc tuân thủ (trích từ SRS)

Những rule này phải được enforce ở **Service + ràng buộc DB**, không chỉ ở giao diện.

| Mã | Quy tắc | Gợi ý triển khai |
|---|---|---|
| GB-01 | Giữ ghế `PENDING` 10 phút (600s); hết hạn tự về `AVAILABLE`; bán tại quầy không bị hold timeout | `SeatHold.expiresAt`; thời gian đọc từ cấu hình `SEAT_HOLD_MINUTES`, **không hard-code**; JOB-01 dọn dẹp mỗi 1 phút |
| GB-01/Mutual exclusion | Một ghế/suất chỉ bị giữ bởi 1 phiên thanh toán | Unique `(ShowtimeID, SeatID)` + transaction/lock; xử lý xung đột đồng thời |
| GB-02 | Ghế có đúng 1 trong 4 trạng thái; không quay ngược từ `SOLD`/`MAINTENANCE` | State machine trong Service |
| GB-03 | Mỗi phim đúng 1 độ tuổi (P/C13/C16/C18); phim giới hạn tuổi cần xác nhận trước checkout | Validate khi tạo phim + modal xác nhận |
| GB-04 | Tích điểm không phân hạng; tỉ lệ do Admin cấu hình | Đọc `POINT_EARN_PERCENT`, `POINT_EXCHANGE_RATE` từ bảng cấu hình |
| GB-05 | Tối đa **1 voucher/đơn**, tổng tiền ≥ `MinOrderAmount`, không cộng dồn | Validate ở Service tính tiền |
| GB-06 | Hủy vé online ≥ 2 giờ trước giờ chiếu → voucher **80%**, không hoàn tiền mặt; ghế về `AVAILABLE` | `CancellationService` sinh voucher |
| GB-07 | Chỉ người có vé `Watched` mới được review; 1 review/phim/người, gửi lại = cập nhật | Unique `(MovieID, UserID)` |
| GB-08 | Suất chiếu: kết thúc = bắt đầu + thời lượng + **15 phút**; không chồng lấn trong cùng phòng; bắt đầu phải ở tương lai | Kiểm tra xung đột khi lưu |
| GB-09 | Ghế hỏng → `MAINTENANCE` ngay; ưu tiên đổi: cùng hạng → nâng hạng miễn phí → hoàn 100% + voucher nếu phòng đầy | Module đổi ghế của POS |
| GB-10 | Trừ tồn kho ngay khi thanh toán thành công; giá & tồn kho ≥ 0; hết hàng → `Out of Stock` | Trừ trong cùng transaction thanh toán |
| GB-11 | Danh mục công khai chỉ hiện phim `Now Showing`/`Coming Soon` | Điều kiện lọc ở query public |
| GB-12 | **Không hard-delete** tài khoản, phim đã có bán, đơn hàng → đổi trạng thái; `LoyaltyPointHistory` chỉ **append-only** | Không viết `DELETE` cho các bảng này; điều chỉnh điểm bằng dòng bù trừ mới |

Ràng buộc duy nhất: email, SĐT (user), tiêu đề phim, tên thể loại, tên phòng, tên đồ ăn, mã voucher, mã vé, QR token, `(RoomID, RowName, ColNumber)` cho ghế, `(BookingID, SeatID)` cho vé.

## 6. Bảo mật & phân quyền (RBAC)

- Vai trò: **Guest, Customer, Staff, Manager, Admin**. Áp quyền bằng `filter/` theo tiền tố URL (`/customer/*`, `/staff/*`, `/manager/*`, `/admin/*`); URL công khai chỉ gồm duyệt phim, suất chiếu, sơ đồ ghế, đăng nhập/đăng ký.
- Mật khẩu **chỉ lưu hash** (BCrypt/PBKDF2 + salt); không bao giờ log hoặc trả mật khẩu/hash ra giao diện.
- Chống SQL Injection (PreparedStatement), XSS (`<c:out>` / `fn:escapeXml`, không in thẳng `${param.x}` vào HTML), CSRF (token cho form POST).
- Webhook thanh toán phải xác thực chữ ký (VNPay: **HMAC-SHA512**, SePay: HMAC) và xử lý idempotent (nhận lặp không tạo trùng giao dịch/vé).
- Session: `invalidate()` khi logout; không đặt dữ liệu nhạy cảm trong URL.
- **Không commit** thông tin nhạy cảm: mật khẩu DB, khóa cổng thanh toán, SMTP. File `ConnectDB.properties`/`config.properties` hiện đang chứa mật khẩu thật → đổi sang file mẫu (`*.properties.example`) và thêm file thật vào `.gitignore`.

## 7. Giao diện & thông điệp

- Ngôn ngữ giao diện: tiếng Việt; tiền tệ định dạng VND (`329.000đ`); giờ `HH:mm`.
- Thông điệp hệ thống dùng mã chuẩn của SRS, lưu tập trung (không rải chuỗi trong code), ví dụ: `MSG01` không có kết quả, `MSG02` trường bắt buộc (`The * field is required.`), `MSG08` vượt độ dài tối đa, `MSG09` sai tài khoản/mật khẩu.
- Lỗi validate hiển thị màu đỏ dưới ô nhập; thao tác thành công dùng toast.
- JSP: dùng JSTL/EL; tách phần lặp (header, footer, menu) vào `views/common`; CSS/JS tách file trong `assets/`, không viết inline dài.

## 8. Background Job (`listener/`)

| Job | Lịch | Nhiệm vụ | Khi lỗi |
|---|---|---|---|
| JOB-01 | 1 phút/lần | Hủy hold quá 10 phút, trả ghế về `AVAILABLE`, ghi log số ghế đã nhả | Thử lại ở lần chạy kế tiếp |
| JOB-02 | 15 phút/lần | Gửi email nhắc suất chiếu (kèm QR) cho vé xác nhận trước giờ chiếu 1–2 giờ; đánh dấu đã gửi để không gửi trùng | Ghi cảnh báo, thử lại tối đa 3 lần |

Job phải idempotent, dùng `ScheduledExecutorService` khởi tạo trong `ServletContextListener` và **shutdown** khi `contextDestroyed`.

## 9. Git workflow

- Nhánh chính: `main` (luôn build được). Mỗi thành viên làm trên nhánh riêng (vd. `duong`) rồi mở Pull Request vào `main`; ít nhất 1 người review.
- Commit message: `<type>: <mô tả ngắn>` với type ∈ `feat | fix | refactor | docs | style | test | chore`. Ví dụ: `feat: add seat hold service (GB-01)`.
- Mỗi commit một thay đổi logic; không commit file build (`target/`, `*.class`), file IDE, file chứa mật khẩu. Thêm `.gitignore` cho `target/`, `.idea/`, `*.iml`, `.vscode/`.
- Trước khi tạo PR: build thành công, tự kiểm tra luồng chính, đã kéo `main` mới nhất và giải quyết conflict.

## 10. Chạy dự án

1. Cài JDK 21, Maven, Tomcat (10.x nếu dùng `jakarta.servlet`), SQL Server.
2. Tạo database `cinemaDB` bằng script của nhóm; sao chép cấu hình mẫu thành `ConnectDB.properties` và điền `db.url`, `db.user`, `db.password`.
3. Build: `mvn clean package` (cần thêm `<packaging>war</packaging>` và dependency Servlet/JSTL `provided` vào `pom.xml` khi bắt đầu viết controller).
4. Deploy WAR lên Tomcat và mở `http://localhost:8080/<context>/`.
5. Kiểm tra kết nối DB nhanh bằng cách chạy `main` của `vn.edu.fpt.util.DBContext`.

## 11. Checklist trước khi tạo Pull Request

- [ ] Đúng tầng (Controller/Service/DAO/Model), không có SQL ngoài DAO, không có logic ngoài Service
- [ ] Mọi SQL dùng `PreparedStatement`; tài nguyên đóng bằng try-with-resources
- [ ] Tiền dùng `BigDecimal`, thời gian dùng `java.time`, trạng thái dùng hằng số/enum
- [ ] Đã áp dụng đúng rule `GB-xx` liên quan và có ràng buộc DB tương ứng
- [ ] Không hard-delete các bảng bị cấm (GB-12); không sửa/xóa dòng `LoyaltyPointHistory`
- [ ] Có kiểm tra quyền (filter/role) cho URL mới; input đã validate ở server; output đã escape
- [ ] Thông điệp dùng mã `MSGxx`, không hard-code chuỗi lỗi
- [ ] Không còn `System.out.println`, `printStackTrace`, import thừa, code comment-out
- [ ] Không commit mật khẩu/khóa bí mật, file `target/`
- [ ] Commit message đúng quy ước, PR mô tả rõ UC/GB liên quan
