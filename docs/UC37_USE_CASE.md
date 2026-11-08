# UC-37 — View Online Ticket Sales Report

Source: [Google Docs SRS](https://docs.google.com/document/d/17T0gNOSK2dXy32OZps6nHkysTtHXuXEUJdG0dGS2ap0/edit).

## UC ID and Name

UC-37: View Online Ticket Sales Report

## Created By

Group 2

**Date Created:** 06/11/26

## Primary Actor

Manager

**Secondary Actors:** Staff explicitly granted VIEW_ONLINE_REPORTS by Admin (UC-31)

## Trigger

The actor selects Online Sales Report in the management menu.

## Description

Provides a read-only operational report of online bookings, ticket/concession values, discounts, successful collections, cash refunds, cancellation vouchers and payment attempts. Summary figures, paginated details, order history and CSV exports share the selected filters. This UC does not perform refunds, settle payments or reconcile bank settlements.

## Preconditions

PRE-01: The actor has an Active authenticated account and VIEW_ONLINE_REPORTS permission.
PRE-02: UC-37 reporting migration has been applied; booking/payment/cancellation records are available.
PRE-03: Payment and refund operations remain governed by UC-14, UC-16 and UC-20.

## Postconditions

POST-01: The requested online report or CSV is returned for valid filters; viewing/exporting does not change orders, tickets or payments.
POST-02: Totals cover all matching records, independently of the displayed page.
POST-03: Missing historical timestamps are explicitly disclosed and excluded from period-based totals rather than assigned an invented date.
POST-04: Invalid filters or access failures do not expose report data; database failures are not represented as zero sales.

## Normal Flow

1. The actor opens Online Sales Report.
2. System checks authentication/permission and defaults to today's calendar date in Asia/Ho_Chi_Minh.
3. The actor selects a date range and optionally filters by movie, showtime or booking ID.
4. System validates the filters and retrieves a consistent read of online sales, adjustments and data-quality counts.
5. System displays successful booking count, original ticket count, ticket/concession value before discounts, total discounts, collections, cash refunds and cancellation-voucher value.
6. System displays paginated successful orders and a daily collections series, including zero-sales days.
7. The actor opens an online order to inspect its original tickets, concessions, payment attempts and exchange/refund/cancellation history.
8. System displays the order history, clearly including events outside the selected reporting period when applicable.

## Alternative Flows

AF1: View Adjustments
1. The actor opens the Adjustments tab.
2. System uses adjustment occurrence dates and includes cash refunds or cancellation vouchers for online orders sold in earlier periods.
3. Cash amounts, voucher amounts, reasons and handling shift/staff where available are displayed separately.
AF2: Inspect Payment Attempts
1. The actor opens Payments and optionally chooses a payment status.
2. System filters attempts by their creation date and shows references, amounts, PaidAt, status and internal consistency warnings.
3. The attempt-status filter affects this tab only; it does not alter sales KPI definitions or mark a payment Paid.
AF3: Export CSV
1. The actor selects Export CSV on the current tab.
2. System rechecks access and filters and exports all matching rows within the disclosed export limit, including filter/time-basis metadata.
3. CSV escapes text and neutralizes spreadsheet formula injection; an oversized export requests a narrower range without silent truncation.
AF4: Zero-value Booking
1. A successfully confirmed zero-value online booking has no VNPay transaction.
2. System includes its booking/tickets/discounts and records zero collected, labelled Fully covered by voucher/points.
AF5: No Matching Data
1. System displays zero totals, an empty-state message and zero-filled dates where relevant.

## Exceptions

EF1: Invalid Filters
1. A date/identifier/status/tab/page is malformed, the start date exceeds the end date, or the range exceeds 366 inclusive days.
2. System displays validation errors with the submitted filters and does not execute the report query.
EF2: Access Denied or Unavailable Order
1. Missing authentication redirects to login; missing permission denies access, including direct detail/export URLs.
2. A missing or Counter order requested through online detail is not returned.
EF3: Payment Anomaly
1. A payment requires review, has mismatched amounts, or is inconsistent with the booking.
2. System displays a warning and excludes unconfirmed attempts from successful collections; the actor cannot force settlement here.
EF4: Historical Data Incomplete
1. A legacy booking has no reliable PaidAt or a legacy voucher cancellation has no occurrence/link record.
2. System discloses incomplete-record counts; it does not fabricate dates or guess a voucher-to-ticket link.
EF5: Database/Export Failure
1. System preserves filter context and reports the error; no partial CSV or misleading zero report is returned.

## Priority

Medium

## Frequency of Use

Daily and on demand

## Business Rules

BR-37-01: Online membership is Booking.Channel = Online, regardless of where a ticket is printed, exchanged or refunded.
BR-37-02: Sales periods use Booking.PaidAt; calendar-day boundaries are Asia/Ho_Chi_Minh, start inclusive/end exclusive. Payment attempts use their creation timestamps; showtime date is a separate dimension.
BR-37-03: Count distinct successfully completed bookings, including zero-value bookings. Repeated attempts/callbacks and replacement tickets do not create new sales.
BR-37-04: Original ticket value + concession value - Booking.DiscountAmount = Booking.FinalAmount. DiscountAmount already includes voucher/point reductions; do not subtract points again or apply current prices to historical transactions.
BR-37-05: Collections S = sum FinalAmount for successful online bookings paid in the period. Cash refunds R = sum actual TicketExchange.CashRefund events in the period for online bookings, including downgrade refunds. Cancellation vouchers V are displayed separately and are not cash outflows.
BR-37-06: Collections less cash refunds = S - R and may be negative. This is not profit, tax-exclusive accounting revenue or the amount remitted by VNPay. Compensation vouchers are separate from cancellation vouchers and are not subtracted as cash.
BR-37-07: A cash refund for an online order is associated with the original online channel and the shift that paid it; an all-cinema consolidation must not subtract it twice. Do not add Ticket.RefundAmount and TicketExchange.CashRefund for the same event.
BR-37-08: ReviewRequired/failed/expired/initiated attempts do not independently create successful sales. The report checks internal records; real provider reconciliation requires an independent statement/API and is outside scope.
BR-37-09: Historical PaidAt is backfilled only from one matching verified Paid transaction. New zero-value confirmations and voucher cancellations persist their occurrence times atomically. Historical missing data is disclosed.
BR-37-10: One original Sweetbox ticket and two expected seats are different measures; replacement/split tickets are excluded from new-sale counts. Current valid tickets are not an attendance/check-in count.
BR-37-11: Default page size is 15; date ranges are at most 366 inclusive days; CSV exports support at most 10,000 matching rows per request. All report operations are read-only and require the same permission.

## Other Information

Dependencies: UC-13/14 online booking/payment, UC-16 customer cancellation, UC-20 broken-seat exchange/refund, UC-22 shift cash reporting, UC-29 dashboard and UC-31 permissions. UC-29 currently uses booking-creation dates and booking-linked refunds; its figures are not silently redefined by UC-37. Implementation follows JSP → Servlet → Service → DAO → SQL Server. Public industry references informed the design; provider fee/settlement reporting, PDF/XLSX and multi-cinema support are future extensions.

## Assumptions

Stored prices, payment results and adjustment records are authoritative operational inputs. New records use explicit Vietnam-time reporting conventions; legacy UTC payment-creation timestamps are converted at query boundaries. The migration must precede deployment of the updated payment/cancellation write paths. Existing cancellation eligibility and compensation policy are preserved.


