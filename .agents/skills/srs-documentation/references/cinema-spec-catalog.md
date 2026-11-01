# Cinema Management System — Canonical Specification Catalog

This catalog is the single source of truth for all business codes, IDs, and rules implemented across `SE2053-SWP391-G2`. Every SRS chapter and Use Case must adhere to these IDs.

---

## 1. Actor Catalog (RBAC Roles)

| Role | Scope & Permissions | URL Prefix |
| :--- | :--- | :--- |
| **Guest** | Unauthenticated user. Browse movies, view showtimes, check seat availability, register/login. | `/`, `/auth/*`, `/movies/*` |
| **Customer** | Authenticated user. Hold seats, make online payment, apply vouchers, earn/redeem points, view tickets, submit reviews. | `/customer/*` |
| **Staff** | Cinema ticket counter & POS operator. Sell tickets at counter (no hold timeout), check-in QR codes, swap broken seats, log equipment reports. | `/staff/*` |
| **Manager** | Operational manager. Manage movies, create showtimes, design room/seat layouts, issue vouchers, view revenue & occupancy reports. | `/manager/*` |
| **Admin** | System administrator. Manage employee/customer accounts, RBAC permissions, configure global parameters, view system activity logs. | `/admin/*` |
| **System / Scheduler** | Background worker executing scheduled maintenance tasks. | `JOB-xx` |

---

## 2. Business Rules Catalog (`GB-01` through `GB-12`)

| Code | Rule Name | Description & Implementation Constraint |
| :--- | :--- | :--- |
| **GB-01** | Seat Hold Timeout & Exclusivity | Hold seats in status `PENDING` for 10 minutes (600s). Reverts to `AVAILABLE` automatically upon timeout (managed by `JOB-01`). Counter POS sales are not subject to hold timeout. Only 1 active hold session per seat/showtime pair at any moment. |
| **GB-02** | Seat State Machine | A seat must strictly possess one of 4 states: `AVAILABLE`, `PENDING`, `SOLD`, `MAINTENANCE`. Irreversible from `SOLD` or `MAINTENANCE` back to `PENDING`/`AVAILABLE` without authorized manager/staff action. |
| **GB-03** | Age Rating Enforcement | Every movie strictly has one rating (`P`, `C13`, `C16`, `C18`). Restricted movies require explicit user age acknowledgment modal prior to payment checkout. |
| **GB-04** | Flat Loyalty Point System | Points earned based on order final amount using `POINT_EARN_PERCENT` and redeemed via `POINT_EXCHANGE_RATE`. No tiered membership levels. `LoyaltyPointHistory` is strictly append-only. |
| **GB-05** | Voucher Constraints | Maximum **1 voucher per booking**. Order total must meet or exceed `MinOrderAmount`. Vouchers are non-stackable. |
| **GB-06** | Ticket Cancellation Policy | Online ticket cancellation permitted only $\ge$ 2 hours before showtime. Refunds issued as **80% value voucher**, never cash/bank transfer. Canceled seats revert immediately to `AVAILABLE`. |
| **GB-07** | Verified Movie Reviews | Only customers holding tickets in status `Watched` for that specific movie can review. Maximum 1 review per user per movie. Re-submission updates existing review. |
| **GB-08** | Showtime Conflict Buffer | Showtime end time = start time + movie duration + **15 minutes cleaning buffer**. No overlapping showtimes in the same room. Start time must be in the future. |
| **GB-09** | Damaged Seat Protocol | Seat reported damaged transitions to `MAINTENANCE` immediately. Customer reassignment priority: Same tier $\rightarrow$ Free upgrade to higher tier $\rightarrow$ 100% refund + complimentary voucher if room is fully booked. |
| **GB-10** | F&B Inventory Control | Concession stock deducted immediately upon payment confirmation. Unit price & stock $\ge 0$. Stock reaching 0 transitions item to `Out of Stock`. |
| **GB-11** | Public Catalog Filter | Public catalog and homepage exclusively list movies in `Now Showing` or `Coming Soon` status. |
| **GB-12** | Soft-Delete Policy | Never execute hard SQL `DELETE` on user accounts, movies with sales history, or booking orders. Only update `Status` flags. |

---

## 3. Special Domain Rules

* **Sweetbox (Ghế đôi - UC-25):**
  - Stored as **1 single `Seat` record** occupying **2 contiguous columns** on the seat map.
  - `ColNumber` represents the left column; right column is skipped in seat numbering.
  - Pricing: Equal to **2 standard seats** (`2 * basePrice`). VIP seats: base price + `SEAT_SURCHARGE_VIP` (default 30,000đ).
  - Capacity: Equal to 2 persons (`SeatType.capacityOf`).
* **Payment Webhook (UC-14):**
  - VNPay IPN signature validated using **HMAC-SHA512**.
  - Must be idempotent: Duplicate IPN requests must not create duplicate tickets or transactions.
  - Browser return URL (`payment.vnpay.return-url`) only displays status; payment confirmation occurs strictly via IPN webhook.

---

## 4. Background Jobs Catalog (`JOB-01` through `JOB-03`)

| Job ID | Schedule | Responsibility | Failure Handling |
| :--- | :--- | :--- | :--- |
| **JOB-01** | Every 1 minute | Releases seats held (`PENDING`) $> 10$ minutes back to `AVAILABLE`. Logs released seat count. | Retries on next scheduled execution. |
| **JOB-02** | Every 15 minutes | Sends showtime reminder emails with QR codes for tickets confirmed 1–2 hours before start time. | Logs warning; max 3 retries. |
| **JOB-03** | Daily at 00:00 + at server startup | Auto-transitions movies: `Coming Soon` $\rightarrow$ `Now Showing` (on `ReleaseDate`); `Now Showing` $\rightarrow$ `Ended` (past `EndDate` with no active showtimes). | Retries on next scheduled execution. |

---

## 5. System Messages Index (`MSGxx`)

* `MSG01`: No matching records found ("Không tìm thấy kết quả phù hợp").
* `MSG02`: Mandatory field missing ("The * field is required" / "Vui lòng nhập trường bắt buộc").
* `MSG08`: Input length exceeds limit ("Vượt quá độ dài tối đa cho phép").
* `MSG09`: Authentication failed ("Tài khoản hoặc mật khẩu không chính xác").
