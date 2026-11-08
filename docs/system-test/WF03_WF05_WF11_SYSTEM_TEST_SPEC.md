# System Test Input Specification: WF03, WF05 and WF11

> Muc dich: tai lieu dau vao cho AI chuyen dung thiet ke system test case va test scenario.
> Pham vi: da doi chieu use case trong `MyUsecase.txt`, workflow tu bang workflow, implementation hien tai va CSDL.
> Khong duoc coi nhu da thay the test execution. Tat ca du lieu va ket qua duoi day de AI chuyen dung chuyen thanh test case co the lap lai.

## 1. Testing objective and scope

### In scope

| Workflow | UC | Main test objective |
|---|---|---|
| WF03 Online Booking | UC-14, UC-15, UC-16, UC-18 | Dat online, thanh toan VNPay, lich su dat ve, huy ve, thong bao va nhac lich |
| WF05 Promotion & Loyalty | UC-11, UC-12, UC-28 | Diem thuong, vi voucher va quan ly khuyen mai |
| WF11 Revenue Reporting | UC-14, UC-16, UC-18, UC-22, UC-29 plus UC-13 source transactions | Thanh toan/huy tao du lieu va doanh thu bao cao |

### Directly specified use cases

- UC-11 View Loyalty Points
- UC-12 View Voucher Wallet
- UC-14 Make Online Payment
- UC-15 View Booking History
- UC-16 Cancel Ticket Online
- UC-18 View Notifications
- UC-28 Manage Vouchers & Promotions

### Out of scope for this system-test package

- Full UC-13 booking-seat/checkout screens because UC-13 is only prerequisite evidence in this package.
- Testing VNPay merchant UI behavior outside the application integration boundary.
- Frontend visual pixel testing, browser compatibility and accessibility testing unless listed as non-functional acceptance criteria.
- Performance/load testing beyond concurrency/idempotency scenarios.
- UC-22, UC-29 detailed reports have only cross-workflow prerequisites and may need separate dedicated suites.

## 2. System and test actors

- Customer
- Manager
- Staff
- Admin
- VNPay Payment Gateway
- System/Scheduler
- Mail Service

## 3. Technical entry points

| Capability | Entry point |
|---|---|
| Loyalty balance/history | `/customer/loyalty` |
| Voucher wallet | `/customer/vouchers` |
| Payment page | `/customer/booking/payment?id=<bookingId>` |
| VNPay signed return | `/customer/booking/payment/return` |
| VNPay IPN/webhook | `/api/v1/commerce/payment/webhook/vnpay` |
| Booking history | `/customer/bookings` |
| Cancel ticket confirmation | `/customer/cancel-ticket?ticketId=<ticketId>` |
| Notifications | `/customer/notifications` |
| Manage promotions | `/manager/vouchers` |

## 4. Required test environment

### Application

- Java 21 / Jakarta Servlet application.
- Running Tomcat compatible with Servlet/JSP.
- SQL Server test database isolated from production.
- Email service configurable and observable in a local test mailbox or mock service.

### VNPay integration

- VNPay test/sandbox merchant configuration.
- HTTPS-capable test hosts for payment URL and return URL.
- Ability to generate:
  - signed successful Return/IPN callbacks;
  - signed failed/cancelled callbacks;
  - invalid signature callbacks;
  - mismatched amount, transaction reference and booking callbacks;
  - duplicate Return/IPN callbacks.

### Scheduler

- Ability to manually trigger or advance:
  - seat-hold cleanup;
  - shift/job timers;
  - reminder email JOB-02;
  - system configuration reads.

### Database verification

Every end-to-end case must be verified in both UI and database. Minimum tables/entities:

- `Users`
- `Bookings`
- `Tickets`
- `PaymentAttempt`
- `SeatHold` / seat status
- `Voucher`
- `LoyaltyPointHistory`
- `User.PointBalance`
- `CustomerNotification`
- `ShowtimeReminder`
- Activity log

## 5. Master test data

| Data | Suggested value | Purpose |
|---|---|---|
| Customer A | Active customer, 25 loyalty points, email A | Normal customer flows |
| Customer B | Active customer, 0 points, email B | Empty histories and isolation |
| Customer C | Active customer, negative or high-value voucher scenarios | Boundary voucher tests |
| Inactive Customer | Status != Active | Authentication/authorization |
| Manager | Active manager | Voucher management and analytics source |
| Staff | Active staff | Counter data feeding revenue reports |
| Movie A | All-movie fixed promotion, public/valid status | Scope All |
| Movie B | Selected movie for promotion | Scope Movie |
| Restricted Movie | Age rating C18 | Payment/checkout independent validation |
| Pending booking | Owned by Customer A, positive amount, live seat hold | Start VNPay |
| Free booking | Final amount = 0, live seats | Confirm without VNPay |
| Expired booking | Pending and hold past expiry | Late callback/review |
| Paid online upcoming ticket | More than configured cancellation hours remain | Successful cancellation |
| Paid online late ticket | Inside cancellation cutoff | Cancellation rejection |
| Paid counter ticket | Counter channel | Channel rejection for UC-16 |
| 11 loyalty transactions | Enough to create 2 pages at 10/page | Pagination |
| 11 booking tickets | Enough to create 2 pages at 10/page | Pagination |
| 16 notifications | Enough to create 2 pages at 15/page | Pagination |
| Active/Used/Expired/Disabled personal vouchers | One record in each status | Wallet categorization |

## 6. Workflow coverage matrix

| User story / workflow | UC(s) | Happy-path system scenarios | Main negative scenarios |
|---|---|---|---|
| WF03 Online Booking | UC-14, UC-15, UC-16, UC-18 | Pay, view history, cancel, see notification | Signature, ownership, cutoff, expired hold, duplicate callback |
| WF05 Promotion & Loyalty | UC-11, UC-12, UC-28 | View points/vouchers; create/edit promotion | Empty wallet/history; invalid code/range; usage limit; immutable status |
| WF11 Revenue Reporting | UC-14, UC-16, UC-18, UC-22, UC-29 | Paid/cancelled transactions and reminders feed reports | Failed/callback mismatch must not create revenue; cancellation not double counted |

## 7. UC-11 loyalty test cases

### ST-UC11-01 Happy path balance and history

Preconditions:
- Customer A is logged in and has a non-zero balance.
- At least 3 loyalty transactions exist.

Steps:
1. Open `/customer/loyalty`.
2. Observe balance and transaction table.

Expected:
- Active customer's current `User.PointBalance` is displayed.
- Transactions are ordered newest first.
- Earned, redeemed and adjustment transaction types are distinguishable.
- No membership tier is displayed.

Database check:
- UI balance equals `Users.PointBalance`.
- Transaction rows equal Customer A's `LoyaltyPointHistory`.

### ST-UC11-02 Zero-balance customer

Preconditions: Customer B has balance 0 and no transaction.

Expected:
- System displays 0 and the empty-history message.
- User is not signed out.

### ST-UC11-03 History pagination

Preconditions: Customer A has 11 or more transactions.

Expected:
- 10 records on page 1.
- Remaining records on page 2.
- Invalid/out-of-range page value is normalized or no records are displayed according to implemented pagination behavior.
- Transactions remain globally newest-first across pages.

### ST-UC11-04 Cross-account isolation

Preconditions: Customer B is logged in after Customer A data is created.

Expected:
- Customer B cannot see Customer A's balance history through UI or URL parameters.
- No customer ID input controls transactions; server session is authoritative.

### ST-UC11-05 Loyalty transaction after settlement/cancellation

Expected:
- Successful eligible booking creates Earned and/or Redeemed rows according to booking settlement.
- Eligible cancellation creates adjustment rows.
- Cancellation reversal does not make balance negative below the system transaction rules.

## 8. UC-12 voucher wallet test cases

### ST-UC12-01 Happy path wallet categories

Preconditions:
- Customer owns Active, Used, Expired and Disabled vouchers.
- Public promotion has OwnerUserID NULL.

Expected:
- Personal vouchers are displayed.
- Public promotion is not shown in wallet.
- Active, Used, Expired and Disabled records are in correct categories.
- Each voucher shows value/type, minimum order, expiry, and movie restriction if present.

### ST-UC12-02 Empty wallet

Preconditions: Customer has no owned voucher.

Expected:
- Empty-wallet message is displayed.
- Public promotion codes remain usable only through checkout.

### ST-UC12-03 Used voucher past expiry

Preconditions: Used voucher has expiry date before now.

Expected:
- It remains categorized as Used, not Expired.
- Used status takes precedence over validity date.

### ST-UC12-04 Expired active voucher

Preconditions: Active voucher has expiry before current time.

Expected:
- It is displayed in Expired category.

### ST-UC12-05 Cross-account isolation

Expected:
- Customer cannot view vouchers owned by another account through direct request or manipulated URL.

### ST-UC12-06 Cancellation welcome/refund voucher

Expected:
- Refund voucher value equals configured percentage of ticket price.
- Validity equals configured cancellation voucher days.
- Welcome voucher follows configured welcome voucher value/days/minimum order.

## 9. UC-14 VNPay payment test cases

### ST-UC14-01 Positive-value booking starts VNPay

Preconditions: Pending online booking owned by Customer A, positive amount, valid live seat holds.

Steps:
1. Open payment page.
2. Submit `pay`.

Expected:
- One PaymentAttempt is created with booking reference, payable amount and Pending status.
- Redirect uses exact VNPay URL and amount.
- Booking remains Pending.
- Seats remain held.

### ST-UC14-02 Signed successful VNPay Return

Preconditions: Valid payment session from ST-UC14-01.

Expected:
- Signature is verified.
- Booking is marked Paid exactly once.
- Payment attempt becomes Paid/Succeeded according to implementation.
- Tickets receive unique QR tokens.
- Voucher is consumed, points redeemed/earned and loyalty rows written.
- Seats are finalized.
- Confirmation email is scheduled/sent once.

### ST-UC14-03 Signed successful VNPay IPN

Expected:
- IPN settles a pending eligible booking transactionally.
- UI Return sent later does not duplicate settlement.
- Repeat IPN returns success/idempotent response and creates no duplicate data.

### ST-UC14-04 Return then IPN duplicate

Expected:
- Exactly one booking payment settlement.
- Exactly one set of tickets and QR tokens.
- Exactly one earned/redeemed loyalty effect and confirmation email.
- Exactly one relevant activity log entity or a deliberate deduplicated activity record.

### ST-UC14-05 Zero-value booking

Preconditions: Voucher/points reduce final amount to exactly 0.

Steps:
1. Open payment page.
2. Submit `confirmFree`.

Expected:
- No VNPay attempt or external redirect.
- Booking is settled, tickets and QR tokens issued, voucher/points processed.
- Exactly one confirmation email.

### ST-UC14-06 Failed/cancelled VNPay result

Expected:
- Payment attempt is failed/cancelled according to callback.
- Booking and holds remain valid if cutoff permits.
- Customer can retry.
- No ticket, QR, voucher consumption or earned points are created.

### ST-UC14-07 Invalid signature

Expected:
- Callback is rejected.
- No booking status or ticket change.
- Invalid callback is logged as rejected.

### ST-UC14-08 Unknown transaction reference

Expected:
- Callback is rejected or no settlement occurs.
- No booking/ticket/loyalty side effect.

### ST-UC14-09 Amount mismatch

Expected:
- Callback marked for review or rejected.
- Booking is not paid.
- No tickets or loyalty/voucher changes.

### ST-UC14-10 Booking mismatch/wrong owner

Expected:
- Payment attempt cannot settle wrong booking.
- System does not issue tickets.
- A review alert/activity entry is created if implemented.

### ST-UC14-11 Expired seat hold with successful callback

Preconditions: Payment result is successful after hold expiry.

Expected:
- Booking does not settle.
- Expired seats are not silently assigned.
- Callback is recorded for review.
- Retry/reattribution is not automatic.

### ST-UC14-12 Callback after booking already paid

Expected:
- Idempotent handling.
- No duplicate Ticket, QR, Voucher, LoyaltyPointHistory or email.

## 10. UC-15 booking history test cases

### ST-UC15-01 Normal history

Preconditions: Customer has paid, pending, cancelled and watched/exchanged records.

Expected:
- Only Customer-owned tickets are shown.
- Ten ticket entries per page.
- Newest booking time first.
- Movie, showtime, seat, ticket code, statuses, channel and price are visible.
- Pending/cancelled ticket QR is hidden.

### ST-UC15-02 QR visibility matrix

| Booking paid | Ticket status | QR visible? |
|---|---|---|
| Yes | Upcoming | Yes |
| Yes | Watched | Yes |
| Yes | Exchanged | Yes |
| No or Pending | Upcoming | No |
| Yes | Cancelled | No |

### ST-UC15-03 Cancellation action visibility

Expected:
- Cancel action is offered only for paid Upcoming online tickets and an eligible cutoff.
- Counter, pending, cancelled, past showtime and ineligible online tickets do not offer cancellation.

### ST-UC15-04 Empty history

Expected:
- Exact empty message `Bạn chưa có lịch sử đặt vé nào.` is shown.

### ST-UC15-05 Pagination and ownership

Expected:
- 10 entries/page.
- No other customer's tickets are returned.
- Invalid page values do not expose unauthorized records.

## 11. UC-16 cancellation test cases

### ST-UC16-01 Successful cancellation

Preconditions:
- Paid online Upcoming ticket owned by Customer A.
- Remaining time exceeds `CANCELLATION_MIN_HOURS`.

Steps:
1. Open booking history.
2. Click Cancel Ticket.
3. Confirm cancellation.

Expected:
- Ticket is Cancelled and refund amount stored.
- Refund voucher is issued with configured percentage and validity.
- Customer notification is created.
- Loyalty adjustment is recorded if points were earned for that booking.
- All database changes commit together.

Implementation caveat to verify:
- Seat availability must not be asserted as updated because the current cancellation transaction does not implement that update.

### ST-UC16-02 Inside cancellation cutoff

Expected:
- Cancellation is blocked before any mutation.
- No voucher, notification, refund amount or loyalty adjustment.

### ST-UC16-03 Wrong owner

Expected:
- Cancellation denied even if Ticket ID is manually modified.

### ST-UC16-04 Counter ticket

Expected:
- Cancellation denied because channel is not Online.

### ST-UC16-05 Non-paid/pending ticket

Expected:
- Cancellation denied.

### ST-UC16-06 Cancelled/already used/exchanged ticket

Expected:
- Cancellation denied for status other than Upcoming.

### ST-UC16-07 Multi-ticket point reversal

Preconditions: One booking earned points for multiple tickets.

Expected:
- Adjustment allocated to cancellation ticket amount.
- Adjustment is capped by remaining points earned for booking.
- Other tickets remain cancellable independently according to policy.

### ST-UC16-08 Atomic rollback

Simulate failure while writing voucher/notification/loyalty/transaction.

Expected:
- No partial cancellation; ticket remains unchanged and no orphan voucher/notification/loyalty record exists.

## 12. UC-18 notification test cases

### ST-UC18-01 Happy path notifications

Expected:
- Authenticated Customer sees only their own notifications.
- Records are newest first.
- 15 records are shown per page.
- Title, message, creation time and read/unread status are displayed.

### ST-UC18-02 Mark all as read

Preconditions: Customer has at least 2 unread notifications.

Expected:
- All unread notifications for that Customer become read.
- Other Customer records are unchanged.
- Unread count becomes zero for that Customer.
- Confirmation message appears.

### ST-UC18-03 Empty notification list

Expected:
- Empty-notifications message is displayed.

### ST-UC18-04 Notification isolation

Expected:
- Direct requests cannot change or reveal another customer's notifications.

### ST-UC18-05 Reminder scheduler

Preconditions:
- Paid booking has Upcoming tickets.
- Showtime is more than 1 hour and at most 2 hours away.

Expected:
- JOB-02 creates/starts exactly one reminder per eligible booking.
- Email is sent to booking contact with upcoming ticket details.
- Scheduler runs at 15-minute cadence.
- Reminder has no duplicate account notification.
- Failed delivery is retried up to 3 total attempts at 15-minute intervals.

### ST-UC18-06 Reminder boundary cases

| Condition | Expected |
|---|---|
| Showtime exactly 1 hour away | Reminder eligibility according to configured inclusive/exclusive implementation |
| Showtime exactly 2 hours away | Eligible unless DAO boundary differs and must be recorded consistently |
| Showtime 30 minutes away | Not eligible if only 1-2 hour window |
| Booking not Paid | No reminder |
| No Upcoming tickets | No reminder |
| Duplicate scheduler run | No duplicate delivery for same booking/reminder cycle |

## 13. UC-28 voucher/promotion test cases

### ST-UC28-01 Create fixed amount promotion scoped to all movies

Expected:
- Promotion code uses 4-32 uppercase letters/digits/hyphen.
- Fixed amount is valid and positive.
- Minimum order, validity and usage limits are stored.
- Campaign is available only within its validity window.

### ST-UC28-02 Create percentage promotion scoped to one movie

Expected:
- Discount type Percent and selected MovieID are stored.
- Promotion is valid only at checkout for that movie.

### ST-UC28-03 Duplicate code

Expected:
- Field-level duplicate-code error.
- No duplicate campaign.

### ST-UC28-04 Invalid date range

Expected:
- Start after expiry and malformed dates are rejected.
- No create/update transaction.

### ST-UC28-05 Zero/negative usage limits

Expected:
- Total and per-user usage limits greater than zero.
- Field error identifies the invalid limit.

### ST-UC28-06 Lower total limit below active uses

Expected:
- Update is rejected if new limit is below paid or live-pending usage.

### ST-UC28-07 Lower per-user limit below active uses

Expected:
- Update is rejected if a customer has used the promotion more than the proposed limit.

### ST-UC28-08 Edit Used or Disabled promotion

Expected:
- System blocks mutation.
- Message identifies immutable Used/Disabled state.

### ST-UC28-09 Delete promotion referenced by booking

Expected:
- Delete is rejected.

### ST-UC28-10 Delete unreferenced promotion

Expected:
- Promotion is removed.

### ST-UC28-11 System-managed refund voucher

Expected:
- Manager can view but not edit or delete Refund voucher records.

### ST-UC28-12 Usage counts Pending holds and Paid bookings

Expected:
- Campaign usage count includes Paid bookings.
- It includes Pending bookings with a live, non-expired hold.
- It excludes expired/cancelled seats according to DAO implementation.

### ST-UC28-13 Public promotion and wallet separation

Expected:
- Public promotion has no owner and is entered by code at checkout.
- Public promotion is not listed in Customer voucher wallet.
- Cancellation/Welcome refund vouchers are personal and can appear in wallet.

### ST-UC28-14 Promotion and loyalty used together

Expected:
- Maximum one voucher is applied.
- Promotion discount and loyalty-point redemption may be combined.
- Minimum order, movie scope and usage policies are checked after current order state.

## 14. Cross-workflow E2E scenarios

### ST-E2E-01 Complete online paid booking

Preconditions: All relevant test configurations and VNPay sandbox are ready.

Flow:
1. Customer creates pending booking and holds seats.
2. Applies promotion and loyalty points.
3. Starts VNPay.
4. Completes signed successful payment.
5. System issues tickets and loyalty/voucher records.
6. Customer sees booking, loyalty balance, notification, wallet state where applicable.

Expected:
- Booking becomes Paid once.
- Revenue data becomes eligible exactly once.
- UI pages are consistent with database and each other.

### ST-E2E-02 Cancel paid ticket

Flow:
1. Customer opens booking history.
2. Cancels eligible ticket.
3. Refund voucher, notification and loyalty adjustment are created.
4. Customer opens loyalty, wallet and notifications.

Expected:
- Booking history shows cancelled ticket.
- QR disappears.
- Refund wallet entry has configured value/expiry.
- Loyalty balance reflects reversal consistently.
- Revenue dashboard excludes cancelled sale or applies the implemented reversal rule consistently.

### ST-E2E-03 Duplicate payment callback

Flow:
1. Send successful Return.
2. Send same callback through IPN.
3. Send both again.

Expected:
- Single settlement and no duplicate side effects.

### ST-E2E-04 Failed payment then successful retry

Flow:
1. Submit failed/cancelled callback.
2. Retry VNPay with a second payment attempt.
3. Submit signed successful callback.

Expected:
- Failed attempt remains recorded.
- Existing seats remain held.
- Only successful attempt settles the booking.
- No duplicate successful attempt.

### ST-E2E-05 Payment callback arrives after hold expiry

Flow:
1. Create pending booking.
2. Wait beyond seat hold expiry or advance scheduler.
3. Submit successful VNPay callback.

Expected:
- Booking is not Paid.
- Seats are not silently reassigned.
- Payment result remains for review.

## 15. Security and authorization system tests

| Test ID | Scenario | Expected |
|---|---|---|
| ST-SEC-01 | Customer calls another user's loyalty page via parameters/session manipulation | 403 or redirect; no foreign balance/history |
| ST-SEC-02 | Customer calls `/manager/vouchers` | 403 |
| ST-SEC-03 | Inactive user calls protected customer URL | Session invalidated or login redirect |
| ST-SEC-04 | POST notification mark-read without valid CSRF | 403 |
| ST-SEC-05 | POST cancellation/payment without valid CSRF | 403 |
| ST-SEC-06 | Tampered VNPay parameters | Signature validation failure; no mutation |
| ST-SEC-07 | Replay old callback amount and valid transaction reference mismatch | No settlement |
| ST-SEC-08 | Direct SQL/script content in voucher/reason/input fields | Stored and rendered safely or rejected according to validation; no script execution |

## 16. Configuration-driven boundary data

Test with default and modified values:

| Key | Default | Test impact |
|---|---|---|
| `SEAT_HOLD_MINUTES` | 10 | Hold expiry and retry after callback |
| `ONLINE_SALE_CUTOFF_MINUTES` | 15 | Payment cancellation cutoff |
| `CANCELLATION_MIN_HOURS` | 24 | UC-16 eligibility |
| `CANCELLATION_VOUCHER_PERCENT` | 80 | Refund voucher value |
| `CANCELLATION_VOUCHER_DAYS` | 30 | Refund expiry |
| `WELCOME_VOUCHER_VALUE` | 50000 | UC-12 wallet welcome voucher |
| `WELCOME_VOUCHER_DAYS` | 30 | Welcome expiry |
| `POINT_EARN_PERCENT` | 1 | Loyalty earned points |
| `POINT_EXCHANGE_RATE` | 1000 | VND value per redeemed point |

Every configuration test should record:

- source value;
- boundary entered;
- booking/voucher/loyalty result;
- database value after settlement.

## 17. System-test oracle rules

A system test passes only if:

1. UI result is consistent with HTTP status and redirect.
2. Database state is consistent with the UI.
3. Configured values are applied and defaults are not hard-coded inconsistently.
4. Positive actions perform exactly once.
5. Duplicate callbacks or repeated submission do not duplicate financial state.
6. Unauthorized or invalid requests do not change state.
7. Failed transactions leave no partial dependent records.
8. Email and account notification behavior matches their separate scopes.
9. No cancelled/pending/failed payment is counted as paid revenue unless implementation has a defined reversal rule.
10. The System Under Test is restored to isolation between test cases.
