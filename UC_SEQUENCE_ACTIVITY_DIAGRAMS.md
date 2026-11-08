# Sequence va Activity Diagram cho cac Use Case

Tai lieu nay duoc ve tu code hien tai va file UC. Mot so quy trinh UC-14 co the xu ly qua ca VNPay Return va IPN de dam bao tinh idempotent.

## UC-11 View Loyalty Points

<!-- mermaid-checked: every participant uses `participant Id as "Label"`, no \n in aliases/messages/notes, every alt/opt/loop closed by end, no `:` inside any alias -->
```mermaid
sequenceDiagram
    participant Customer as "Customer"
    participant Servlet as "LoyaltyPointsServlet"
    participant Service as "LoyaltyPointsService"
    participant DAO as "LoyaltyDAO"
    participant DB as "Database"

    Customer->>Servlet: Mo trang /customer/loyalty
    Servlet->>Service: Lay diem va lich su
    Service->>DAO: Kiem tra User.PointBalance
    DAO->>DB: Tim tai khoan Customer Active
    alt Khong tim thay tai khoan
        DB-->>DAO: Khong co du lieu
        DAO-->>Servlet: So du null
        Servlet-->>Customer: Dang xuat
    else Tai khoan hop le
        DB-->>DAO: PointBalance
        DAO-->>Service: So du
        Service->>DAO: Dem va truy van lich su
        DAO->>DB: LoyaltyPointHistory moi den cu
        DB-->>DAO: Trang lich su
        DAO-->>Service: Danh sach 10 giao dich
        Service-->>Servlet: So du va lich su phan trang
        Servlet-->>Customer: Hien thi diem va lich su
    end
```

```mermaid
flowchart TD
    A([Khach mo diem thuong]) --> B{Dang nhap va tai khoan Active?}
    B -->|Khong| C[Dang xuat]
    B -->|Co| D[Truy van PointBalance]
    D --> E[Lay LoyaltyPointHistory moi den cu]
    E --> F{Co giao dich?}
    F -->|Co| G[Hien thi 10 giao dich moi trang]
    F -->|Khong| H[Hien thi so du va thong bao lich su rong]
    G --> I([Ket thuc])
    H --> I
```

## UC-12 View Voucher Wallet

<!-- mermaid-checked: every participant uses `participant Id as "Label"`, no \n in aliases/messages/notes, every alt/opt/loop closed by end, no `:` inside any alias -->
```mermaid
sequenceDiagram
    participant Customer as "Customer"
    participant Servlet as "VoucherWalletServlet"
    participant Service as "VoucherWalletService"
    participant DAO as "VoucherDAO"
    participant DB as "Database"

    Customer->>Servlet: Mo /customer/vouchers
    alt Chua dang nhap
        Servlet-->>Customer: Chuyen den trang dang nhap
    else Da dang nhap
        Servlet->>Service: Lay voucher cua Customer
        Service->>DAO: findWalletForOwner
        DAO->>DB: Voucher theo OwnerUserID
        DB-->>DAO: Danh sach voucher
        DAO-->>Service: Voucher da tinh trang
        Service-->>Servlet: Danh sach voucher
        alt Vi trong
            Servlet-->>Customer: Hien thi thong bao vi trong
        else Co voucher
            Servlet-->>Customer: Nhom Active Used Expired Disabled
        end
    end
```

```mermaid
flowchart TD
    A([Khach mo Vi voucher]) --> B{Da dang nhap?}
    B -->|Khong| C[Chuyen den dang nhap]
    B -->|Co| D[Truy van voucher theo OwnerUserID]
    D --> E{Vi co voucher?}
    E -->|Khong| F[Hien thi thong bao vi trong]
    E -->|Co| G{Tinh trang voucher}
    G --> H[Active]
    G --> I[Used]
    G --> J[Expired]
    G --> K[Disabled]
    H --> L[Hien thi phan nhom]
    I --> L
    J --> L
    K --> L
    L --> M([Ket thuc])
    F --> M
```

## UC-14 Make Online Payment

<!-- mermaid-checked: every participant uses `participant Id as "Label"`, no \n in aliases/messages/notes, every alt/opt/loop closed by end, no `:` inside any alias -->
```mermaid
sequenceDiagram
    participant Customer as "Customer"
    participant Servlet as "BookingPaymentServlet"
    participant Service as "BookingService"
    participant Payment as "PaymentService"
    participant VNPay as "VNPay Gateway"
    participant Callback as "VnPayWebhookServlet"
    participant DB as "Database"

    Customer->>Servlet: Xac nhan thanh toan don Pending
    alt Don bang 0 dong
        Servlet->>Service: Xac nhan don mien phi
        Service->>DB: Phat hanh ve va QR
        DB-->>Service: Da thanh toan
        Service-->>Servlet: Xac nhan thanh cong
        Servlet-->>Customer: Hien thi ket qua
    else Don co gia tri
        Servlet->>Payment: Khoi tao thanh toan
        Payment->>Service: Tao PaymentAttempt
        Service->>DB: Luu tham chieu va so tien
        DB-->>Service: PaymentAttempt
        Service-->>Payment: Payment URL
        Payment-->>Servlet: Payment URL
        Servlet-->>Customer: Chuyen den VNPay
        Customer->>VNPay: Thanh toan
        VNPay-->>Customer: Signed Return URL
        VNPay-->>Callback: IPN neu co ket noi cong khai
        Customer->>Callback: Goi Return URL
        Callback->>Payment: Kiem tra chu ky va tham so
        Payment->>Service: Xu ly VerifiedPayment
        Service->>DB: Tim attempt va don Pending
        alt Thanh toan hop le va idempotent
            Service->>DB: Transaction thanh toan
            DB-->>Service: Da Paid
            Service-->>Payment: Thanh cong
            Payment-->>Callback: Ket qua
            Callback-->>Customer: Chuyen den trang thanh toan
        else Du lieu khong hop le
            Service-->>Payment: Tu choi hoac ReviewRequired
            Payment-->>Callback: Ket qua loi
            Callback-->>Customer: Hien thi loi
        end
    end
```

```mermaid
flowchart TD
    A([Khach xac nhan thanh toan]) --> B{Don con Pending va co luc giu ghe?}
    B -->|Khong| C[Tu choi thanh toan]
    B -->|Co| D{Tong tien bang 0?}
    D -->|Co| E[Chot don ngay khong qua VNPay]
    E --> F[Ap dung voucher va diem da chon]
    F --> G[Phat hanh ve QR va gui email]
    D -->|Khong| H[Tao PaymentAttempt]
    H --> I[Chuyen den VNPay]
    I --> J[VNPay tra ve Signed Return va co the goi IPN]
    J --> K{Chu ky tham so va tham chieu hop le?}
    K -->|Khong| L[Tuyen bo callback khong hop le]
    K -->|Co| M{Du lieu thanh toan khop attempt va don?}
    M -->|Khong| N[Danh dau ReviewRequired]
    M -->|Co| O{Ket qua thanh toan thanh cong?}
    O -->|Khong| P[Ghi tran trang thai Failed]
    O -->|Co| Q[Chot don transaction va sinh ve QR]
    Q --> R[Thu voucher diem va cap nhat ghe]
    R --> S[Gui email xac nhan]
    S --> T([Ket thuc])
    G --> T
    C --> U([Ket thuc loi])
    L --> U
    N --> U
    P --> U
```

## UC-15 View Booking History

<!-- mermaid-checked: every participant uses `participant Id as "Label"`, no \n in aliases/messages/notes, every alt/opt/loop closed by end, no `:` inside any alias -->
```mermaid
sequenceDiagram
    participant Customer as "Customer"
    participant Servlet as "BookingHistoryServlet"
    participant Service as "BookingHistoryService"
    participant DAO as "CustomerBookingDAO"
    participant DB as "Database"

    Customer->>Servlet: Mo /customer/bookings
    alt Chua dang nhap
        Servlet-->>Customer: Chuyen den dang nhap
    else Da dang nhap
        Servlet->>Service: Lay lich su theo CustomerID
        Service->>DAO: Dem va truy van trang
        DAO->>DB: Ticket cung Booking Showtime Movie Seat
        DB-->>DAO: Lich su moi den cu
        DAO-->>Service: Toi da 10 ticket moi trang
        Service-->>Servlet: Page lich su
        Servlet-->>Customer: Hien thi thong tin va trang thai
        opt Ve Paid va du dieu kien
            Servlet-->>Customer: Hien thi QR va nut Cancel or Review
        end
    end
```

```mermaid
flowchart TD
    A([Khach mo lich su dat ve]) --> B{Da dang nhap?}
    B -->|Khong| C[Chuyen den dang nhap]
    B -->|Co| D[Lay ticket cua CustomerID]
    D --> E[Sap xep theo BookingTime giam dan]
    E --> F[Phan trang 10 ticket]
    F --> G{Co lich su?}
    G -->|Khong| H[Hien thi thong bao chua co lich su]
    G -->|Co| I[Hien thi phim suat ghe ma ve gia va trang thai]
    I --> J{Booking Paid va ticket Upcoming Watched Exchanged?}
    J -->|Co| K[Hien thi QR]
    J -->|Khong| L[Khong hien thi QR]
    K --> M{Ve co the huy or review?}
    M -->|Co| N[Hien thi hanh dong tuong ung]
    M -->|Khong| O([Ket thuc])
    L --> O
    H --> O
```

## UC-16 Cancel Ticket Online

<!-- mermaid-checked: every participant uses `participant Id as "Label"`, no \n in aliases/messages/notes, every alt/opt/loop closed by end, no `:` inside any alias -->
```mermaid
sequenceDiagram
    participant Customer as "Customer"
    participant Servlet as "TicketCancellationServlet"
    participant Service as "TicketCancellationService"
    participant DAO as "CustomerBookingDAO"
    participant DB as "Database"

    Customer->>Servlet: Mo trang xac nhan huy ve
    Servlet->>Service: Kiem tra quyen va dieu kien
    Service->>DAO: Tim ung vien huy ve
    DAO->>DB: Ticket Booking Showtime
    DB-->>DAO: Du lieu ve
    alt Khong du dieu kien
        Service-->>Servlet: ValidationException
        Servlet-->>Customer: Hien thi loi va chuyen ve lich su
    else Du dieu kien
        Service-->>Servlet: Thong tin chinh sach
        Servlet-->>Customer: Trang xac nhan co CSRF
        Customer->>Servlet: POST xac nhan
        Servlet->>Service: Cancel transaction
        Service->>DB: Cap nhat Ticket Cancelled va RefundAmount
        Service->>DB: Dieu chinh diem neu co
        Service->>DB: Tao refund voucher va notification
        DB-->>Service: Commit
        Service-->>Servlet: Phan tram hoan
        Servlet-->>Customer: Thong bao thanh cong
    end
```

```mermaid
flowchart TD
    A([Khach chon huy ve]) --> B[Mo trang xac nhan]
    B --> C{Ve thuoc Customer va Booking Paid Online Upcoming?}
    C -->|Khong| D[Tu choi]
    C -->|Co| E{Con it nhat CANCELLATION_MIN_HOURS?}
    E -->|Khong| F[Tu choi theo moc gio]
    E -->|Co| G[Khach xac nhan voi CSRF]
    G --> H[Cap nhat Ticket Cancelled va RefundAmount]
    H --> I{Ve da duoc ghi diem?}
    I -->|Co| J[Thu hoi diem ty le theo gia ve]
    I -->|Khong| K
    J --> K[Phat hanh refund voucher]
    K --> L[Them notification]
    L --> M{Transaction thanh cong?}
    M -->|Co| N[Thong bao so phan tram hoan]
    M -->|Khong| O[Rollback va thong bao loi]
    N --> P([Ket thuc])
    O --> P
    D --> P
    F --> P
```

> Luu y theo code hien tai: UC-16 chua cap nhat Seat trong transaction huy ve. Tai lieu khong the ghi Seat duoc tra ve Available cho den khi code xu ly diem nay duoc bo sung.

## UC-18 View Notifications

<!-- mermaid-checked: every participant uses `participant Id as "Label"`, no \n in aliases/messages/notes, every alt/opt/loop closed by end, no `:` inside any alias -->
```mermaid
sequenceDiagram
    participant Customer as "Customer"
    participant Servlet as "CustomerNotificationServlet"
    participant Service as "CustomerNotificationService"
    participant DAO as "CustomerNotificationDAO"
    participant Listener as "ShowtimeReminderListener"
    participant DB as "Database"
    participant Mail as "EmailService"

    Customer->>Servlet: Mo /customer/notifications
    Servlet->>Service: Lay thong bao theo CustomerID
    Service->>DAO: Query trang thong bao
    DAO->>DB: CustomerNotification moi den cu
    DB-->>DAO: Trang thong bao
    DAO-->>Service: Toi da 15 notification
    Service-->>Servlet: Danh sach
    Servlet-->>Customer: Hien thi notification
    Customer->>Servlet: Mark all as read
    Servlet->>Service: Cap nhat tat ca notification chua doc
    Service->>DAO: Mark read theo CustomerID
    DAO->>DB: UPDATE
    DB-->>DAO: Thanh cong
    DAO-->>Service: Hoan tat
    Service-->>Servlet: Thong bao thanh cong
    Servlet-->>Customer: Redirect va lam moi

    Listener->>DB: Moi 15 phut tim booking eligible
    DB-->>Listener: Booking trong khoang 1 den 2 gio
    Listener->>Mail: Gui email nhac voi QR
    alt Gui thanh cong
        Mail-->>Listener: Success
        Listener->>DB: Mark Sent
    else Gui that bai
        Mail-->>Listener: Failure
        Listener->>DB: Tang attempt toi da 3 lan
    end
```

```mermaid
flowchart TD
    A([Khach mo chuong thong bao]) --> B[Truy van notification cua Customer]
    B --> C{Co notification?}
    C -->|Khong| D[Hien thi thong bao rong]
    C -->|Co| E[Hien thi toi da 15 moi trang]
    E --> F{Khach chon danh dau tat ca da doc?}
    F -->|Co| G[Cap nhat unread cua Customer]
    F -->|Khong| H([Ket thuc])
    G --> H
    I([JOB-02 moi 15 phut]) --> J{Booking Paid va Showtime con 1 den 2 gio?}
    J -->|Khong| K([Bo qua])
    J -->|Co| L[Gui email nhac kem ve va QR]
    L --> M{Gui thanh cong?}
    M -->|Co| N[Ghi SentAt]
    M -->|Khong| O{Da 3 lan thu?}
    O -->|Chua| P[Thu lai sau 15 phut]
    O -->|Da| Q[Ket thuc voi loi]
```

## UC-28 Manage Vouchers and Promotions

<!-- mermaid-checked: every participant uses `participant Id as "Label"`, no \n in aliases/messages/notes, every alt/opt/loop closed by end, no `:` inside any alias -->
```mermaid
sequenceDiagram
    participant Manager as "Manager"
    participant Servlet as "VoucherManagementServlet"
    participant Service as "VoucherService"
    participant DAO as "VoucherDAO"
    participant DB as "Database"

    Manager->>Servlet: Mo /manager/vouchers
    Servlet->>Service: Tim kiem loc va phan trang
    Service->>DAO: Query Voucher
    DAO->>DB: Voucher va UsageCount
    DB-->>DAO: Danh sach
    DAO-->>Service: VoucherPage
    Service-->>Servlet: Du lieu trang
    Servlet-->>Manager: Hien thi danh sach

    Manager->>Servlet: Tao hoac sua Promotion
    Servlet->>Service: VoucherForm
    Service->>Service: Validate code discount scope dates limits
    alt Du lieu khong hop le
        Service-->>Servlet: ValidationException
        Servlet-->>Manager: Hien thi loi
    else Hop le
        Service->>DAO: Insert or Update Promotion
        DAO->>DB: Luu Voucher
        DB-->>DAO: Thanh cong
        DAO-->>Service: Hoan tat
        Service-->>Servlet: Luu thanh cong
        Servlet-->>Manager: Redirect va thong bao
    end

    Manager->>Servlet: Xoa voucher
    Servlet->>Service: Delete Promotion
    Service->>DAO: Kiem tra voucher dang duoc tham chieu
    DAO->>DB: Voucher va Booking
    alt Voucher da duoc dung
        DB-->>DAO: Dang tham chieu
        DAO-->>Service: Khong the xoa
        Service-->>Manager: Thong bao loi
    else Voucher chua duoc dung
        DAO->>DB: Xoa Promotion
        DB-->>DAO: Thanh cong
        DAO-->>Service: Hoan tat
        Service-->>Manager: Thong bao da xoa
    end
```

```mermaid
flowchart TD
    A([Manager mo Voucher Management]) --> B[Loc theo tu khoa loai trang thai]
    B --> C[Tao hoac sua Promotion]
    C --> D[Nhap ma loai giam gia gia tri don toi thieu ngay pham vi gioi han]
    D --> E{Du lieu hop le?}
    E -->|Khong| F[Hien thi loi tung truong]
    E -->|Co| G{Ma da ton tai?}
    G -->|Co| H[Tu choi ma trung]
    G -->|Khong| I[Luu Promotion]
    I --> J{Tong hoac per-user limit >= usage hien tai?}
    J -->|Khong| K[Tu choi giam limit]
    J -->|Co| L[Hoan tat luu]
    L --> M{Manager chon xoa?}
    M -->|Khong| N([Ket thuc])
    M -->|Co| O{Voucher da duoc tham chieu?}
    O -->|Co| P[Khong cho xoa]
    O -->|Khong| Q[Xoa Promotion]
    Q --> N
    F --> N
    H --> N
    K --> N
    P --> N
```
