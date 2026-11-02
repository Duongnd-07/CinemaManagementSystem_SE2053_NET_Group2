# UC09 – Manage Notifications (Manager)

> Plan đã được chủ UC duyệt. Người thực hiện: Codex. Reviewer: Claude.
> Không tự ý mở rộng phạm vi. Gặp điểm mơ hồ thì dừng lại hỏi, không đoán.

## 0. Bắt buộc trước khi code

1. **Đọc và tuân thủ skill dự án** `.agents/skills/jakarta-servlet-mvc/SKILL.md` (kèm `references/controller-guide.md`, `dao-guide.md`, `jsp-rules.md`). Đây là chuẩn kiến trúc: `jakarta.*`, `DBContext.getConnection()`, try-with-resources, PreparedStatement, PRG, CSRF, `<c:out>`.
2. **Dùng skill `ui-ux-pro-max`** (ở `~/.agents/skills/ui-ux-pro-max/SKILL.md`) cho **mọi phần giao diện**: trang `manager/notifications.jsp`, các hộp thư, tag danh sách thông báo và icon chuông có huy hiệu.
   - Script tìm kiếm thực tế nằm ở `~/.agents/skills/ui-ux-pro-max/scripts/search.py`. Đường dẫn `~/.gemini/...` ghi trong SKILL.md là sai, đừng dùng.
   - Gợi ý truy vấn: `"notification badge unread" --domain ux`, `"form validation error near field" --domain ux`, `"icon button accessible label" --domain icons`, `"confirm destructive action" --domain ux`, `"data table filter pagination" --domain ux`.
   - **Giữ phong cách sẵn có của từng khu vực**: manager và staff dùng token Tailwind (`bg-surface-container-*`, `text-on-surface*`, `primary-container`, `font-label-lg`…), customer và admin dùng nền tối với màu đỏ `#e50914`. Skill chỉ dùng để nâng chất lượng UX (accessibility, trạng thái lỗi, focus, kích thước nút ≥ 44px, aria-label cho nút chỉ có icon), **không đổi design system**.
   - Trước khi bàn giao, chạy pre-delivery checklist trong `references/pro-rules.md` của skill.
3. Code mẫu nên bám theo: [VoucherManagementServlet.java](../../SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/manager/VoucherManagementServlet.java) (form + PRG + ActivityLogger), [ReviewService.java](../../SE2053-SWP391-G2/src/main/java/vn/edu/fpt/service/ReviewService.java) quanh dòng 196 (transaction `setAutoCommit(false)` / commit / rollback + `addSuppressed`), [CustomerNotificationServlet.java](../../SE2053-SWP391-G2/src/main/java/vn/edu/fpt/controller/customer/CustomerNotificationServlet.java) (hộp thư + flash).

## 1. Mục tiêu

Manager gửi thông báo in-app tới:
- **Toàn bộ tài khoản** (Admin, Manager, Staff, Customer)
- **Toàn bộ nhân viên** (Staff + Manager)
- **Toàn bộ khách hàng**
- **1 khách hàng**, xác định bằng số điện thoại

Manager xem lịch sử gửi kèm thống kê đã đọc và có thể thu hồi thông báo. Mọi role có hộp thư riêng và icon chuông có huy hiệu số chưa đọc.

**Ngoài phạm vi (KHÔNG làm):** hẹn giờ gửi, gửi email/SMS, thông báo khuyến mãi hoặc kèm voucher, đính kèm ảnh/link, sửa thông báo đã gửi, gửi cho 1 nhân viên cụ thể.

## 2. Quy tắc nghiệp vụ

| Mã | Quy tắc |
|---|---|
| BR-01 | `TargetType` ∈ `All`, `Staff`, `Customer`, `SingleCustomer`. Tập role nhận: `All` → Admin, Manager, Staff, Customer · `Staff` → Staff, Manager · `Customer` → Customer · `SingleCustomer` → đúng 1 UserID |
| BR-02 | Chỉ gửi cho tài khoản có `Status = 'Active'`. **Người gửi không nhận bản của chính mình** |
| BR-03 | `SingleCustomer`: SĐT được trim, phải khớp `^0[0-9]{9}$` (cùng regex với RegisterService), phải tồn tại, có `Role = 'Customer'` và `Status = 'Active'`. Mỗi trường hợp sai có thông báo lỗi riêng gắn vào trường `phone` |
| BR-04 | Tiêu đề được trim, dài 1–150 ký tự. Nội dung được trim, dài 1–1000 ký tự |
| BR-05 | Loại thông báo Manager được chọn (`NotificationCategory`): `General` "Thông báo chung" cho mọi đối tượng · `Movie` "Phim & lịch chiếu" chỉ cho `Customer`/`SingleCustomer` · `Internal` "Nội bộ" chỉ cho `Staff` · `System` "Hệ thống / bảo trì" cho mọi đối tượng. Không cho chọn các loại hệ thống tự sinh `Review`, `Refund`, `Voucher` |
| BR-06 | Gửi qua 2 bước. Bước **Xem trước** hiển thị bản xem trước thông báo và số người nhận; với `SingleCustomer` thì hiện họ tên + email đã che (`a***@gmail.com`). Bước **Xác nhận gửi**: server validate lại toàn bộ và đếm lại người nhận, không tin dữ liệu của bước xem trước |
| BR-07 | Số người nhận thực tế = 0 thì rollback, báo lỗi và không tạo bản ghi |
| BR-08 | Chống gửi trùng: nếu đã có Announcement `Sent` cùng SenderID, Title, Message, TargetType, TargetUserID trong 60 giây gần nhất thì từ chối. Kiểm tra trong transaction với `WITH (UPDLOCK, HOLDLOCK)` để hai request song song không cùng lọt qua. Phía client thì disable nút sau khi submit |
| BR-09 | Thu hồi được **bất cứ lúc nào**: `UPDATE … SET Status='Recalled', RecalledAt=GETDATE(), RecalledBy=? WHERE AnnouncementID=? AND Status='Sent'`. Nếu 0 dòng bị ảnh hưởng thì báo "Không tồn tại hoặc đã thu hồi". Nếu thành công thì `DELETE` các dòng `CustomerNotification` theo `AnnouncementID`, cùng transaction. Bản ghi Announcement vẫn giữ để tra cứu |
| BR-10 | Mọi Manager xem được toàn bộ lịch sử. Mỗi lần gửi hoặc thu hồi ghi `ActivityLogger.info(request, LogEventType.NOTIFICATION, "Gửi thông báo" / "Thu hồi thông báo", "Announcement", id, mô tả)`. Mô tả gồm đối tượng, loại và số người nhận. `NOTIFICATION` là event type **mới**, nhãn "Thông báo" (xem mục 3 và 4.1) |
| BR-11 | Danh sách người nhận được chốt lúc gửi. Tài khoản tạo sau đó không nhận thông báo cũ |

## 3. Database – tạo file mới `SE2053-SWP391-G2/sql/UC09_manage_notifications.sql`

- **Giữ nguyên** `UC09_seed_seat_map.sql`, không đổi tên, không sửa.
- Script phải chạy lại nhiều lần được (idempotent) theo kiểu các script khác: `IF OBJECT_ID(...) IS NULL`, `IF COL_LENGTH(...) IS NULL`, `IF NOT EXISTS (SELECT 1 FROM sys.indexes …)`. Constraint đặt tên tường minh. Ghi chú đầu file: "chạy sau UC11_12_15_16_18_customer_ops.sql".

```sql
CREATE TABLE Announcement (
    AnnouncementID INT IDENTITY(1,1) PRIMARY KEY,
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
CREATE INDEX IX_Announcement_CreatedAt ON Announcement (CreatedAt DESC);

ALTER TABLE CustomerNotification ADD AnnouncementID INT NULL
    CONSTRAINT FK_CustomerNotification_Announcement REFERENCES Announcement(AnnouncementID);
CREATE INDEX IX_CustomerNotification_Announcement ON CustomerNotification (AnnouncementID);
CREATE INDEX IX_CustomerNotification_User_Read ON CustomerNotification (UserID, ReadAt);
```

Giữ nguyên tên bảng `CustomerNotification` dù giờ bảng chứa thông báo của mọi role. **Không đổi tên.**

**Mở rộng CHECK của `ActivityLog.EventType`** để có thêm `NOTIFICATION`. Constraint hiện tại được tạo inline **không có tên** trong `UC32_activity_log.sql`, nên phải tìm tên rồi xóa bằng dynamic SQL, theo đúng mẫu `UC28_vouchers.sql` đã làm với `DiscountType`:

```sql
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
-- Chạy lại script: nếu CK_ActivityLog_EventType đã có nhưng thiếu NOTIFICATION thì drop rồi tạo lại
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
```

- `EventType` là `VARCHAR(20)`, `'NOTIFICATION'` dài 12 ký tự nên vừa.
- **Không sửa** `UC32_activity_log.sql`.
- Kiểm tra sau khi chạy: `INSERT` thử 1 dòng log `NOTIFICATION` phải thành công, `INSERT` với giá trị lạ như `'FOO'` phải bị chặn, rồi xóa dòng thử.

## 4. Thiết kế code

### 4.1 Model (`vn.edu.fpt.model`)
- `Announcement`: POJO đủ các cột ở trên, thêm `senderName`, `targetUserName`, `targetUserPhone`, `readCount` (lấy từ JOIN/subquery). Thêm `isRecalled()`.
- `NotificationTarget`: final class hằng số theo mẫu `Role.java` (`ALL`, `STAFF`, `CUSTOMER`, `SINGLE_CUSTOMER`), `List<String> VALUES`, `isValid`, `labelOf` (nhãn tiếng Việt), `recipientRoles(target)` trả về `List<String>` role theo BR-01.
- `LogEventType` (sửa): thêm `public static final String NOTIFICATION = "NOTIFICATION";`, thêm vào `ALL` (đặt trước `SYSTEM`) và thêm nhãn `NOTIFICATION, "Thông báo"` vào `LABELS`. `Map.of` nhận tối đa 10 cặp, sau khi thêm là 8 cặp nên vẫn dùng được. Trang Nhật ký của Admin tự có thêm lựa chọn lọc vì đọc từ `ALL`. Kiểm tra lại JSP `system-logs.jsp` xem có hardcode danh sách nhóm hoặc màu theo nhóm không; nếu có thì bổ sung `NOTIFICATION`.
- `NotificationCategory`: hằng số `GENERAL`, `MOVIE`, `INTERNAL`, `SYSTEM` + `labelOf` (gồm cả nhãn cho `Review`/`Refund`/`Voucher` để hộp thư hiển thị), `selectableFor(target)` trả về danh sách loại hợp lệ theo BR-05, `isAllowed(type, target)`.

### 4.2 DAO
- `AnnouncementDAO` (mới):
  - `int insert(Connection, Announcement)` dùng `RETURN_GENERATED_KEYS`.
  - `int fanOut(Connection, int announcementId, Announcement, List<String> roles, Integer targetUserId, int senderId)` thực hiện **một câu** `INSERT INTO CustomerNotification (UserID, Title, Message, NotificationType, AnnouncementID) SELECT UserID, ?, ?, ?, ? FROM [User] WHERE Status = 'Active' AND UserID <> ? AND Role IN (?, …)` (cộng thêm `AND UserID = ?` khi là SingleCustomer). Trả về số dòng chèn được. Placeholder `IN` được sinh động theo số role, **tuyệt đối không nối chuỗi giá trị**.
  - `void updateRecipientCount(Connection, int id, int count)`.
  - `int countRecipients(List<String> roles, Integer targetUserId, int senderId)` dùng cho bước xem trước, cùng điều kiện WHERE với `fanOut`.
  - `boolean existsRecentDuplicate(Connection, …)` theo BR-08, có `WITH (UPDLOCK, HOLDLOCK)`.
  - `int count(String target, String type, String keyword)` và `List<Announcement> search(target, type, keyword, offset, limit)`: JOIN User để lấy tên người gửi và khách đích, subquery `readCount`, `ORDER BY CreatedAt DESC, AnnouncementID DESC`, phân trang `OFFSET/FETCH`. Keyword tìm trong Title (`LIKE ?` với giá trị `%kw%`).
  - `boolean markRecalled(Connection, int id, int managerId)` và `int deleteRecipients(Connection, int id)`.
- `UserDAO`: thêm `User findByPhone(String phone)` trả về UserID, FullName, Email, Phone, Role, Status.
- `CustomerNotificationDAO`: thêm `int countUnread(int userId)`. Các method cũ giữ nguyên.

### 4.3 Service
- `AnnouncementForm`: target, phone, type, title, message. Setter tự trim; null thì thành `""` (theo mẫu `ProfileForm`).
- `AnnouncementPreview`: DTO gồm `recipientCount`, `customerName`, `maskedEmail`, `targetLabel`, `typeLabel`.
- `AnnouncementService`:
  - `AnnouncementPreview preview(AnnouncementForm, User sender)`: validate (BR-01..05) rồi đếm. Lỗi thì ném `ValidationException(Map<field, message>)` với các field `target`, `phone`, `type`, `title`, `message`, `general`. Số người nhận = 0 thì báo lỗi `general`.
  - `int send(AnnouncementForm, User sender)`: validate lại, mở transaction, kiểm tra trùng (BR-08), insert, fanOut, nếu count = 0 thì rollback và ném lỗi (BR-07), cập nhật RecipientCount, commit. Trả về count.
  - `void recall(int announcementId, User manager)` theo BR-09, trong transaction.
  - `Page<Announcement> search(target, type, keyword, page)` với PAGE_SIZE = 10, dùng `Page.of`.
  - Hàm `maskEmail` private.
- `CustomerNotificationService`: thêm `countUnread(int userId)`.

### 4.4 Controller
- `controller/manager/NotificationManagementServlet` tại `@WebServlet("/manager/notifications")`:
  - **GET:** lấy flash, đọc bộ lọc `target`, `type`, `keyword`, `page` rồi gọi `search`. Set các attribute `history`, `targets`, `categories` (map target → các loại hợp lệ, để JS lọc dropdown), `csrfToken`, `activeMenu = "notifications"`. Forward tới `/WEB-INF/views/manager/notifications.jsp`.
  - **POST:** kiểm tra CSRF trước (sai thì 403), sau đó xử lý theo `action`:
    - `preview`: gọi `preview`. Thành công thì forward lại view với `preview` + `form` (không redirect). Lỗi thì forward kèm `errors` + `form`.
    - `send`: gọi `send`. Thành công thì set flash "Đã gửi thông báo đến N tài khoản.", ghi log rồi redirect về `/manager/notifications`. Lỗi validation thì forward lại form kèm lỗi.
    - `recall`: parse `id`, gọi `recall`, set flash, ghi log rồi redirect (giữ query của bộ lọc nếu có).
    - `action` lạ thì trả 400.
  - Bắt `SQLException`: ghi log SEVERE rồi `sendError(500, Messages.SYSTEM_ERROR)`, theo mẫu servlet sẵn có. Khi forward lại view vẫn phải nạp danh sách lịch sử.
- `controller/NotificationInboxServlet` tại `@WebServlet({"/staff/notifications", "/manager/inbox", "/admin/notifications"})`:
  - View theo servletPath: `staff/notifications.jsp`, `manager/inbox.jsp`, `admin/notifications.jsp`.
  - GET: phân trang qua `CustomerNotificationService.findForUser`. POST (kiểm tra CSRF): `markAllRead` rồi redirect về đúng servletPath. Logic giống `CustomerNotificationServlet`.
- `filter/UnreadNotificationFilter` tại `@WebFilter("/*")`:
  - Chỉ chạy khi method là GET, path không bắt đầu bằng `/assets/` và session có `user`.
  - Gọi `countUnread` rồi set `request.setAttribute("unreadNotificationCount", n)`.
  - `SQLException` thì log WARNING và **vẫn `chain.doFilter`**. Không bao giờ làm hỏng trang.
  - Không cache vào session.

### 4.5 View
- `WEB-INF/views/common/notification-bell.jsp`: include, nhận `<jsp:param name="href">`. Hiển thị icon `notifications` có `aria-label="Thông báo, N chưa đọc"`. Nếu `unreadNotificationCount > 0` thì hiện badge đỏ (quá 9 thì hiện "9+"). Vùng bấm tối thiểu 44×44px.
- Gắn chuông vào:
  - `public-header.jsp`: thay link chuông hiện tại của Customer, giữ href `/customer/notifications`.
  - `staff-header.jsp`: `/staff/notifications`.
  - Header trong `manager-sidebar.jsp`: `/manager/inbox`.
  - Header inline của 3 trang admin (`users.jsp`, `system-config.jsp`, `system-logs.jsp`) và trang admin mới: `/admin/notifications`.
- `manager-sidebar.jsp`: thêm mục "Thông báo" (icon `campaign`, `activeMenu == 'notifications'`) đặt sau "Đánh giá phim".
- `WEB-INF/tags/notification-list.tag`: danh sách thông báo dùng chung cho 3 hộp thư mới. Attribute: `pageData`, `baseUrl`, `markReadUrl`, `csrfToken`, `emptyMessage`. Mỗi thông báo hiện tiêu đề, nhãn loại (`NotificationCategory.labelOf`), thời gian, nội dung `whitespace-pre-line`, và đánh dấu chưa đọc. Có nút "Đánh dấu tất cả đã đọc" và dùng lại `pagination.tag`. Chỉ dùng class trung tính chạy được trên mọi nền tối (`bg-white/5`, `text-white`, `text-gray-300`, `border-white/10`). **Không sửa** `customer/notifications.jsp`, ngoài việc trang đó tự có chuông qua header.
- `manager/notifications.jsp` (theo layout manager hiện có, include `manager-head.jsp` và `manager-sidebar.jsp`):
  1. **Form soạn:**
     - Radio chọn đối tượng; ô SĐT chỉ hiện khi chọn `SingleCustomer`.
     - Dropdown loại, lọc bằng JS theo đối tượng.
     - Tiêu đề và nội dung, có bộ đếm ký tự `x/150` và `x/1000`.
     - Lỗi hiển thị ngay dưới từng trường. Nút "Xem trước".
  2. **Khung xem trước** (khi có `preview`):
     - Hiển thị thông báo giống lúc người nhận xem, kèm câu "Sẽ gửi đến **N** tài khoản" hoặc tên + email đã che của khách.
     - Form ẩn chứa lại toàn bộ dữ liệu, nút "Xác nhận gửi" (disable sau khi bấm) và nút "Sửa lại".
  3. **Lịch sử:**
     - Bộ lọc theo đối tượng, loại và từ khóa.
     - Bảng gồm: thời gian, tiêu đề, loại, đối tượng (SingleCustomer thì kèm tên/SĐT), người gửi, `Đã đọc x/N`, trạng thái, hành động.
     - Hành động "Thu hồi" mở `confirm()` cảnh báo không hoàn tác được; dòng đã thu hồi thì không có nút.
     - Phân trang bằng `pagination.tag`.
- Mọi dữ liệu người dùng nhập đều xuất qua `<c:out>`.

### 4.6 Messages
Thêm hằng số vào `util/Messages.java` (tiếng Việt), không hardcode chuỗi trong servlet/service: lỗi đối tượng/loại không hợp lệ, SĐT trống/sai định dạng/không tồn tại/không phải khách hàng/tài khoản không hoạt động, độ dài tiêu đề/nội dung, không có người nhận, gửi trùng, thông báo không tồn tại hoặc đã thu hồi, flash gửi thành công (có `{0}` số người), flash thu hồi thành công.

## 5. Danh sách file

**Tạo mới:** `sql/UC09_manage_notifications.sql`, `model/Announcement.java`, `model/NotificationTarget.java`, `model/NotificationCategory.java`, `dao/AnnouncementDAO.java`, `service/AnnouncementForm.java`, `service/AnnouncementPreview.java`, `service/AnnouncementService.java`, `controller/manager/NotificationManagementServlet.java`, `controller/NotificationInboxServlet.java`, `filter/UnreadNotificationFilter.java`, `views/manager/notifications.jsp`, `views/manager/inbox.jsp`, `views/staff/notifications.jsp`, `views/admin/notifications.jsp`, `views/common/notification-bell.jsp`, `tags/notification-list.tag`.

**Sửa:** `model/LogEventType.java`, `dao/UserDAO.java`, `dao/CustomerNotificationDAO.java`, `service/CustomerNotificationService.java`, `util/Messages.java`, `views/common/public-header.jsp`, `views/common/staff-header.jsp`, `views/common/manager-sidebar.jsp`, `views/admin/users.jsp`, `views/admin/system-config.jsp`, `views/admin/system-logs.jsp`.

**Không được đụng:** `sql/UC09_seed_seat_map.sql`, `sql/UC32_activity_log.sql`, các service đang insert thông báo (`ReviewService`, `TicketCancellationService`, `WelcomeVoucherService`), `AuthFilter`.

## 6. Thứ tự thực hiện

1. SQL script, sau đó chạy thử 2 lần trên DB local để chắc chắn chạy lại được.
2. Model → DAO → Service.
3. Servlet quản lý → `manager/notifications.jsp` + mục trong sidebar.
4. Servlet hộp thư + tag + 3 view hộp thư.
5. Filter + include chuông + gắn vào các header.
6. `mvn -q compile` (hoặc build của dự án) không được có lỗi. Sau đó tự chạy checklist ở mục 7.

## 7. Checklist nghiệm thu (reviewer sẽ kiểm theo đây)

- [ ] Gửi `All`/`Staff`/`Customer`: số dòng `CustomerNotification` theo `AnnouncementID` bằng `RecipientCount` bằng số tài khoản Active đúng role, trừ người gửi. Tài khoản Suspended/Deactivated không nhận.
- [ ] `All` có cả Admin. `Staff` có Manager khác nhưng không có người gửi và không có Admin.
- [ ] `SingleCustomer`: SĐT sai định dạng, không tồn tại, thuộc Staff, hoặc khách Suspended đều bị từ chối với thông báo riêng. SĐT hợp lệ thì chỉ đúng 1 người nhận.
- [ ] Sửa tay POST với target/type lạ, hoặc `Internal` + `Customer`, hoặc `Movie` + `Staff`, đều bị từ chối.
- [ ] Tiêu đề 151 ký tự, nội dung 1001 ký tự, hoặc chỉ có khoảng trắng đều bị từ chối. Nội dung chứa `<script>alert(1)</script>` hiển thị dạng text ở mọi hộp thư.
- [ ] Bấm "Xác nhận gửi" 2 lần (hoặc gửi lại POST trong vòng 60 giây) chỉ tạo 1 Announcement.
- [ ] Bước xem trước hiện 5 người nhận, nhưng trước khi xác nhận có 1 tài khoản bị khóa: kết quả gửi được 4 và flash ghi đúng 4.
- [ ] Thu hồi: thông báo biến mất khỏi hộp thư người nhận, huy hiệu giảm, lịch sử hiện "Đã thu hồi". Thu hồi lần 2 bị từ chối.
- [ ] Huy hiệu: hiện đúng số ở cả 4 khu vực, quá 9 thì hiện "9+", bấm "Đánh dấu tất cả đã đọc" thì về 0. Trang công khai khi chưa đăng nhập vẫn chạy bình thường. Lỗi DB trong filter không làm hỏng trang.
- [ ] Phân quyền: Staff/Customer/Admin vào `/manager/notifications` bị chặn. Customer vào `/staff/notifications` bị chặn. POST không có CSRF trả 403.
- [ ] Thông báo hệ thống cũ (Review/Refund/Voucher) vẫn tạo và hiển thị bình thường.
- [ ] Mỗi lần gửi/thu hồi có đúng 1 dòng ActivityLog với `EventType = 'NOTIFICATION'`. Trang Nhật ký của Admin lọc được theo nhóm "Thông báo". Các nhóm log cũ vẫn ghi bình thường.
- [ ] Chạy `UC09_manage_notifications.sql` lần 2 không lỗi, và constraint `CK_ActivityLog_EventType` vẫn có đủ 8 giá trị.
- [ ] UI đạt pre-delivery checklist của `ui-ux-pro-max`: label hiển thị rõ, lỗi nằm cạnh trường, focus ring còn nguyên, nút chỉ có icon có aria-label, độ tương phản ≥ 4.5:1, không có thanh cuộn ngang.
