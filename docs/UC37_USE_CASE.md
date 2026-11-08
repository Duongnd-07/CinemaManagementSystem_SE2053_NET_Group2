# UC-37 — View Online Ticket Sales History

Source: [Google Docs SRS](https://docs.google.com/document/d/17T0gNOSK2dXy32OZps6nHkysTtHXuXEUJdG0dGS2ap0/edit).

## UC ID and Name

UC-37: View Online Ticket Sales History

## Created By

Group 2

**Date Created:** 06/11/26

## Primary Actor

Manager

**Secondary Actors:** Staff explicitly granted VIEW_ONLINE_REPORTS by Admin (UC-31)

## Trigger

The actor selects Reports & Revenue → Online Ticket Sales History (“Báo cáo & doanh thu” → “Lịch sử bán vé online”) from the management sidebar.

## Description

Allows an authorized actor to view completed online ticket orders, filter them by payment date, and review four summary figures together with a daily or monthly collections chart. The actor can browse the paginated paid-order history, view an order’s ticket/concession details and export the matching orders to CSV.

## Preconditions

PRE-01: The actor is logged in with an Active Manager or Staff account.
PRE-02: The actor has VIEW_ONLINE_REPORTS permission. Manager has this permission by default; Staff must be granted it through UC-31.

## Postconditions

POST-01: For valid filters, System displays the matching paid online orders, four summary figures and the collections chart, or returns the requested order details/CSV.
POST-02: Summary figures and chart totals cover the entire selected period, independently of the displayed history page.
POST-03: Viewing, filtering, paging and exporting do not change bookings, tickets or payment records.
POST-04: Invalid requests or unavailable data produce the corresponding validation/access/system response; a data-loading failure is not presented as zero sales.

## Normal Flow

1. The actor opens Reports & Revenue and selects Online Ticket Sales History.
2. System checks authentication and permission, then opens the history with Today selected by default.
3. The actor keeps Today or selects another quick period, or enters From Date and To Date and selects View History.
4. System validates the date range and retrieves only online orders whose payment is completed and verified.
5. System displays four summary figures for the selected period: Successful Orders, Tickets Sold, Amount Before Discounts and Amount Collected.
6. System displays collections by day for Today, Last 7 Days and This Month, or by month for This Quarter and This Year. The actor may expand View Full Data Table to read the chart values.
7. System displays up to 10 paid orders per page, sorted by payment time from newest to oldest. Each row shows order/payment information, movie/showtime, ticket count, ticket value before discounts, concessions, discount and amount collected.
8. The actor reviews the history and may change the period, move between pages, open an order or export CSV through the alternative flows.

## Alternative Flows

AF1: Select a Quick Period
1. At Step 3, the actor selects Today, Last 7 Days, This Month, This Quarter or This Year.
2. System sets the period from today, six days before today, the first day of the current month, the first day of the current calendar quarter or the first day of the current year, respectively, through today.
3. System uses daily chart buckets for the first three options and monthly buckets for the last two, resets the history to page 1 and repeats Steps 4–7.
AF2: Select a Custom Date Range
1. The actor enters From Date and To Date and selects View History.
2. System validates the dates, retains the current daily/monthly chart interval and reloads the summaries, chart and first history page for that range.
AF3: Browse History Pages
1. At Step 7, the actor selects Next Page or Previous Page when available.
2. System keeps the selected dates and chart interval, displays the requested page with at most 10 orders, and preserves the full-period summary/chart totals.
AF4: View a Paid Order’s Details
1. The actor selects an order number in the history.
2. System displays the movie, room/showtime, creation/payment times, paid status, customer, ticket/concession values, discount, amount collected and payment method.
3. System lists the order’s ticket codes, seats, ticket statuses and prices, together with concession products, quantities and subtotals when present.
4. The actor selects Back to Sales History. System restores the selected period, chart interval and history page.
AF5: Export CSV
1. The actor selects Export CSV from the history.
2. System checks the same permission and date filters, then exports all matching paid online orders, including rows beyond the current page, when the total is at most 10,000.
3. System returns a UTF-8 CSV containing the reporting period, generation information and order details.
AF6: No Matching Orders
1. At Step 4, no completed online order matches the selected period.
2. System displays zero in all four summary figures, zero chart buckets and an empty-history message.

## Exceptions

EF1: Invalid Date Range or Request Parameters
1. System detects an invalid date, a year outside 1900–9998, a From Date after To Date, or a range longer than 366 inclusive days. A malformed/non-positive page value or unsupported chart interval is also rejected.
2. System displays the relevant validation errors and retains the submitted dates for correction; the invalid report request is not executed.
EF2: Authentication or Permission Failure
1. An unauthenticated actor is redirected to login. An actor without VIEW_ONLINE_REPORTS is denied access.
2. The same checks apply to direct history, order-detail and CSV requests.
EF3: Invalid or Unavailable Order
1. A malformed/non-positive order identifier is rejected.
2. A nonexistent order, a counter-sale order, or an online order without the required completed payment/date is not returned through this function.
EF4: Unsupported or Oversized Export
1. The requested export format is unsupported, or more than 10,000 paid orders match the period.
2. System reports the error. For an oversized CSV, the actor must narrow the date range; System does not silently truncate the export.
EF5: Data-Loading Failure
1. System cannot retrieve the required data.
2. System displays a system-error response instead of a successful history or misleading zero totals. The actor may retry the request.

## Priority

Medium

## Frequency of Use

Daily and on demand

## Business Rules

BR-37-01: Include only bookings with Channel = Online, Status = Paid and a recorded PaidAt. For a nonzero final amount, at least one Paid online-payment record must have matching requested/received amounts and a recorded payment time. A zero-value paid booking does not require a payment-gateway transaction.
BR-37-02: Filter by booking payment date, not order-creation date or showtime date. Both selected calendar dates are included, using Vietnam local-date boundaries. Orders without a reliable PaidAt are excluded from period-based results.
BR-37-03: Quick periods run through today. Last 7 Days includes today and the preceding six days; This Quarter starts on 1 January, 1 April, 1 July or 1 October as applicable; This Year starts on 1 January.
BR-37-04: Today, Last 7 Days and This Month use daily chart buckets. This Quarter and This Year use monthly buckets labelled MM/YYYY. Empty buckets show zero; each bucket includes only payments inside the selected period. Custom ranges retain the currently selected chart interval.
BR-37-05: Successful Orders counts each qualifying booking once. Tickets Sold counts original tickets; replacement/split tickets issued during seat exchanges do not create additional sales. Repeated payment attempts do not multiply orders or ticket values. A Sweetbox ticket is counted as one ticket record, not two seats.
BR-37-06: Amount Before Discounts is the sum of original ticket prices and concession subtotals. Amount Collected is the sum of the matching bookings’ stored FinalAmount. Use stored transaction values; do not reapply current prices or subtract voucher/point discounts again.
BR-37-07: The page has exactly four summary figures: Successful Orders, Tickets Sold, Amount Before Discounts and Amount Collected. All four and the chart use the full matching set, not only the displayed page. Collections are not reduced by cash refunds or cancellation-voucher values in this function.
BR-37-08: History contains paid online orders only, with at most 10 orders per page, ordered by PaidAt descending and BookingID descending for equal payment times. An out-of-range positive page is clamped to the last available page. Missing displayed numeric values are shown as 0 or 0đ.
BR-37-09: A qualifying zero-value booking still contributes to Successful Orders and Tickets Sold, contributes zero to Amount Collected, and is labelled as fully covered by vouchers/points.
BR-37-10: Date ranges contain at most 366 calendar days. Dates must be valid and use years 1900–9998; page numbers must be positive integers.
BR-37-11: CSV exports contain all matching paid orders, subject to a 10,000-row limit, and use the same access/date rules as the screen. CSV text is escaped and protected against spreadsheet formula interpretation. All UC-37 operations are read-only.

## Other Information

Related use cases: UC-13/UC-14 provide online booking/payment data; UC-31 controls access. This history has no movie, showtime-ID, booking-ID, content-tab or payment-attempt-status filters, and no adjustment/payment-inspection sections.
Traceability: /manager/online-reports → OnlineSalesReportServlet → OnlineSalesReportService → OnlineSalesReportDAO → Booking, Ticket, Booking_Concession, OnlinePayment, Showtime, Movie, Room and User. TicketExchange is read only to identify replacement tickets. View: /WEB-INF/views/manager/online-reports.jsp.

## Assumptions

Stored booking/payment times and historical ticket/concession amounts are accurate, and the reporting schema, including Booking.PaidAt, is available. Payment confirmation and changes to ticket states are handled by their existing use cases; UC-37 reads the resulting records.


