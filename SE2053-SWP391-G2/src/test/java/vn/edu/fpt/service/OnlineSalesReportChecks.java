package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import vn.edu.fpt.dao.BookingDAO;
import vn.edu.fpt.dao.CustomerBookingDAO;
import vn.edu.fpt.dao.OnlineSalesReportDAO;
import vn.edu.fpt.model.OnlineSalesFilter;
import vn.edu.fpt.model.OnlineSalesResult;
import vn.edu.fpt.model.OnlineSalesRow;
import vn.edu.fpt.model.OnlineSalesSummary;
import vn.edu.fpt.model.Permission;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.util.DBContext;

/** Run via scripts/run-uc37-tests.ps1 [-Database]. Database fixtures always roll back. */
public final class OnlineSalesReportChecks {
    private static int checks;
    private static final OnlineSalesReportService SERVICE = new OnlineSalesReportService(new OnlineSalesReportDAO(),
            Clock.fixed(Instant.parse("2026-11-05T18:00:00Z"), ZoneOffset.UTC));

    public static void main(String[] args) throws Exception {
        unitChecks();
        if (List.of(args).contains("--db")) {
            try {
                databaseChecks();
            } finally {
                DBContext.closePool();
            }
        }
        System.out.println("PASS: " + checks + " UC-37 checks" + (List.of(args).contains("--db") ? " (including SQL integration; fixtures rolled back)" : " (unit only)"));
    }

    private static void unitChecks() throws Exception {
        OnlineSalesFilter defaults = SERVICE.filter(Map.of());
        check(defaults.getFrom().toString().equals("2026-11-06"), "Vietnam date differs from UTC date");
        check(defaults.getPage() == 1, "default page");
        check(OnlineSalesReportService.PAGE_SIZE == 10, "ten orders per page");
        check(defaults.getGroupBy().equals("day"), "daily chart by default");
        invalid(Map.of("groupBy", "year"), "general");
        OnlineSalesFilter monthly = SERVICE.filter(Map.of("from", "2024-01-15", "to", "2024-03-10", "groupBy", "month"));
        Map<LocalDate, BigDecimal> sales = Map.of(
                LocalDate.parse("2024-01-14"), new BigDecimal("999"),
                LocalDate.parse("2024-01-15"), new BigDecimal("100"),
                LocalDate.parse("2024-01-31"), new BigDecimal("200"),
                LocalDate.parse("2024-03-10"), new BigDecimal("400"),
                LocalDate.parse("2024-03-11"), new BigDecimal("999"));
        Map<LocalDate, BigDecimal> months = OnlineSalesReportService.collectionSeries(monthly, sales);
        check(new ArrayList<>(months.keySet()).equals(List.of(LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-02-01"), LocalDate.parse("2024-03-01"))), "chronological monthly buckets");
        money(months.get(LocalDate.parse("2024-01-01")), "300", "multiple sale days summed into one month");
        money(months.get(LocalDate.parse("2024-02-01")), "0", "empty month zero-filled");
        money(months.get(LocalDate.parse("2024-03-01")), "400", "partial month excludes sales outside filter");
        money(months.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add), "700", "monthly totals match filtered sales");
        OnlineSalesFilter leapDays = SERVICE.filter(Map.of("from", "2024-02-28", "to", "2024-03-01"));
        Map<LocalDate, BigDecimal> days = OnlineSalesReportService.collectionSeries(leapDays,
                Map.of(LocalDate.parse("2024-02-29"), BigDecimal.TEN));
        check(days.size() == 3 && days.get(LocalDate.parse("2024-02-29")).equals(BigDecimal.TEN), "daily chart retains leap day");
        check(OnlineSalesReportService.collectionSeries(monthly, Map.of()).size() == 3, "empty monthly chart keeps month buckets");
        OnlineSalesFilter newYear = SERVICE.filter(Map.of("from", "2024-12-31", "to", "2025-01-01", "groupBy", "month"));
        check(OnlineSalesReportService.collectionSeries(newYear, Map.of()).size() == 2, "monthly chart spans year boundary");
        check(SERVICE.filter(Map.of("from", "2024-01-01", "to", "2024-12-31")).getTo().getDayOfMonth() == 31,
                "366 inclusive leap-year days accepted");
        invalid(Map.of("from", "2024-01-01", "to", "2025-01-01"), "to");
        invalid(Map.of("from", "2026-11-07", "to", "2026-11-06"), "to");
        invalid(Map.of("from", "2026-02-30"), "from");
        invalid(Map.of("from", "0001-01-01"), "from");
        invalid(Map.of("to", "+999999999-12-31"), "to");
        for (String key : List.of("page")) {
            invalid(Map.of(key, "-1"), key);
            invalid(Map.of(key, "2147483648"), key);
            invalid(Map.of(key, "1 OR 1=1"), key);
        }
        OnlineSalesFilter legacy = SERVICE.filter(Map.of("tab", "payments", "status", "Other",
                "movieId", "1 OR 1=1", "showtimeId", "-1", "bookingId", "0"));
        check(legacy.getFrom().equals(defaults.getFrom()), "removed filters ignored without changing paid-order scope");
        check(OnlineSalesReportService.csvCell("=HYPERLINK(\"evil\")").startsWith("\"'="), "CSV formula escaped");
        check(OnlineSalesReportService.csvCell("  @SUM(A1)").startsWith("\"'"), "CSV leading space formula escaped");
        check(OnlineSalesReportService.csvCell("\ttext").startsWith("\"'"), "CSV control prefix escaped");
        check(OnlineSalesReportService.csvCell(new BigDecimal("-50000")).equals("\"-50000\""), "negative numeric preserved");
        check(OnlineSalesReportService.csvCell("a,\"b\"\nc").equals("\"a,\"\"b\"\"\nc\""), "CSV quote/newline escaped");
        check(Permission.fromPath("/manager/online-reports") == Permission.VIEW_ONLINE_REPORTS, "all endpoint actions mapped");
        check(Permission.VIEW_ONLINE_REPORTS.isDefaultFor(Role.MANAGER), "manager default");
        check(!Permission.VIEW_ONLINE_REPORTS.isDefaultFor(Role.STAFF)
                && Permission.VIEW_ONLINE_REPORTS.isAssignableTo(Role.STAFF), "staff requires grant");
    }

    private static void databaseChecks() throws Exception {
        OnlineSalesReportDAO dao = new OnlineSalesReportDAO();
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int room = scalar(c, "SELECT TOP 1 RoomID FROM Room ORDER BY RoomID");
                int customer = scalar(c, "SELECT TOP 1 UserID FROM [User] WHERE Role='Customer'");
                int staff = scalar(c, "SELECT TOP 1 UserID FROM [User] WHERE Role='Staff'");
                List<Integer> seats = new ArrayList<>();
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT TOP 8 SeatID FROM Seat WHERE RoomID=" + room + " ORDER BY SeatID")) {
                    while (rs.next()) { seats.add(rs.getInt(1)); }
                }
                check(seats.size() >= 6, "fixture room has seats");
                String token = "UC37-" + UUID.randomUUID();
                int movie = insert(c, "INSERT INTO Movie(Title,DurationMinutes,AgeRating,Status) VALUES (?,90,'P','Now Showing')", token);
                int show = insert(c, "INSERT INTO Showtime(MovieID,RoomID,StartTime,EndTime,BasePrice,Status) VALUES (?,?,'2035-01-01T10:00:00','2035-01-01T11:30:00',100000,'Active')", movie, room);
                int paid = booking(c, customer, show, 220000, 20000, 200000, "Paid", "Online", "2034-11-06T00:01:00");
                int ticket = ticket(c, paid, show, seats.get(0), 100000, "Upcoming", token + "a");
                ticket(c, paid, show, seats.get(1), 100000, "Upcoming", token + "b");
                int concession = scalar(c, "SELECT TOP 1 ConcessionID FROM Concession");
                update(c, "INSERT INTO Booking_Concession(BookingID,ConcessionID,Quantity,SubtotalPrice) VALUES (?,?,1,20000)", paid, concession);
                attempt(c, paid, "Failed", 200000, 0, "2034-11-05T17:00:00", null, token + "f1");
                attempt(c, paid, "Failed", 200000, 0, "2034-11-05T17:00:00", null, token + "f2");
                attempt(c, paid, "Paid", 200000, 200000, "2034-11-05T17:01:00", "2034-11-06T00:01:00", token + "p");
                int free = booking(c, customer, show, 100000, 100000, 0, "Pending", "Online", null);
                ticket(c, free, show, seats.get(2), 100000, "Upcoming", token + "c");
                check(new BookingDAO().markBookingPaid(c, free, null, 0, LocalDateTime.parse("2034-11-06T09:00:00")), "free booking paid timestamp write");
                check(!new BookingDAO().markBookingPaid(c, free, null, 0, LocalDateTime.parse("2034-11-07T09:00:00")), "repeated paid update rejected");
                int pending = booking(c, customer, show, 100000, 0, 100000, "Pending", "Online", null);
                attempt(c, pending, "ReviewRequired", 100000, 100000, "2034-11-06T02:00:00", null, token + "review");
                int previous = booking(c, customer, show, 100000, 0, 100000, "Paid", "Online", "2034-11-05T12:00:00");
                int old = ticket(c, previous, show, seats.get(3), 100000, "Cancelled", token + "old");
                int shift = insert(c, "INSERT INTO Shift(StaffID,CounterNo,StartedAt,EndedAt,ClosedAt,OpeningCash) VALUES (?,'UC37-test','2034-11-06T08:00:00','2034-11-06T09:00:00','2034-11-06T09:00:00',100000)", staff);
                update(c, "UPDATE Ticket SET RefundAmount=50000,RefundShiftID=? WHERE TicketID=?", shift, old);
                update(c, "INSERT INTO TicketExchange(OldTicketID,ExchangeType,Reason,CashRefund,StaffID,ShiftID,CreatedAt) VALUES (?,'Refund',N'UC37 fixture',50000,?,?,'2034-11-06T08:30:00')", old, staff, shift);
                int replaced = ticket(c, paid, show, seats.get(4), 100000, "Upcoming", token + "new");
                update(c, "UPDATE Ticket SET Status='Exchanged' WHERE TicketID=?", ticket);
                update(c, "INSERT INTO TicketExchange(OldTicketID,ExchangeType,NewTicketID,Reason,CashRefund,StaffID,CreatedAt) VALUES (?,'Same',?,N'UC37 replacement',0,?,'2034-11-06T09:00:00')", ticket, replaced, staff);
                int cancelled = booking(c, customer, show, 100000, 0, 100000, "Paid", "Online", "2034-11-05T10:00:00");
                int cancelledTicket = ticket(c, cancelled, show, seats.get(5), 100000, "Cancelled", token + "can");
                String voucherCode = "UC37" + UUID.randomUUID().toString().substring(0, 10);
                insert(c, "INSERT INTO Voucher(VoucherCode,DiscountValue,DiscountType,ExpiryDate,OwnerUserID,Status,VoucherType,MinOrderAmount) VALUES (?,80000,'Amount','2035-01-01',?,'Active','Refund80',0)", voucherCode, customer);
                update(c, "UPDATE Ticket SET RefundAmount=80000 WHERE TicketID=?", cancelledTicket);
                new CustomerBookingDAO().recordCancellation(c, cancelledTicket, voucherCode, new BigDecimal("80000"), customer, LocalDateTime.parse("2034-11-06T10:00:00"));
                int legacy = booking(c, customer, show, 100000, 0, 100000, "Paid", "Online", null);
                int counter = booking(c, customer, show, 100000, 0, 100000, "Paid", "Counter", "2034-11-06T09:00:00");
                OnlineSalesFilter f = SERVICE.filter(Map.of("from", "2034-11-06", "to", "2034-11-06"));
                OnlineSalesSummary summary = dao.summarize(c, f);
                check(summary.getOrderCount() == 2, "paid+free counted once; pending and counter excluded");
                check(summary.getTicketCount() == 3, "replacement tickets excluded; originals retained");
                money(summary.getTicketValue(), "300000", "original ticket value");
                money(summary.getConcessionValue(), "20000", "concession join not multiplied by payments");
                money(summary.getCollected(), "200000", "collection independent of payment attempts");
                check(dao.count(c, f) == 2 && dao.rows(c, f, 1, 1).size() == 1, "order pagination");
                money(dao.dailyCollections(c, f).values().iterator().next(), "200000", "daily sales use paid date, not future show date");
                check(dao.detail(c, counter) == null, "counter detail inaccessible");
                check(dao.detail(c, pending) == null, "pending detail inaccessible");
                check(dao.detail(c, legacy) == null, "unverified legacy paid date excluded");
                check(dao.detail(c, paid).getTickets().size() == 3, "detail retains purchased tickets");
                OnlineSalesRow freeRow = dao.rows(c, f, 0, 10).stream()
                        .filter(row -> row.getBookingId() == free).findFirst().orElseThrow();
                money(freeRow.getConcessionValue(), "0", "missing concessions mapped to zero");
                money(freeRow.getAmount(), "0", "fully discounted order retained");
                OnlineSalesResult export = new OnlineSalesResult();
                export.setSummary(summary);
                export.setGeneratedAt(LocalDateTime.parse("2034-11-06T12:00:00"));
                export.setRows(dao.rows(c, f, 0, 10000));
                String csv = SERVICE.csv(f, export);
                check(csv.startsWith("\uFEFF") && csv.contains("200000") && !csv.contains("80000"), "UTF-8 CSV exports only paid orders");
                check(legacy > 0, "fixture setup completed");
            } finally {
                c.rollback();
                c.setAutoCommit(true);
            }
        }
        OnlineSalesFilter empty = SERVICE.filter(Map.of("page", "2147483647",
                "from", "1900-01-01", "to", "1900-01-03"));
        OnlineSalesReportService reportService = new OnlineSalesReportService(new OnlineSalesReportDAO() {
            @Override
            public OnlineSalesSummary summarize(Connection c, OnlineSalesFilter f) {
                OnlineSalesSummary s = new OnlineSalesSummary();
                s.setTicketValue(new BigDecimal("300000"));
                s.setConcessionValue(new BigDecimal("20000"));
                s.setCollected(new BigDecimal("200000"));
                return s;
            }
        }, Clock.fixed(Instant.parse("2026-11-06T05:00:00Z"), ZoneOffset.UTC));
        OnlineSalesResult emptyResult = reportService.report(empty, false);
        money(emptyResult.getSummary().getGrossAmount(), "320000", "gross includes tickets and concessions");
        money(emptyResult.getSummary().getCollected(), "200000", "collected excludes refund adjustments");
        check(emptyResult.getCollections().size() == 3
                && emptyResult.getCollections().values().stream().allMatch(v -> v.signum() == 0), "zero-filled daily series");
        check(emptyResult.getPage() == 1 && emptyResult.getPageCount() == 1, "oversized page safely clamped");
        OnlineSalesReportService paged = new OnlineSalesReportService(new OnlineSalesReportDAO() {
            @Override
            public int count(Connection c, OnlineSalesFilter f) { return 21; }
            @Override
            public List<OnlineSalesRow> rows(Connection c, OnlineSalesFilter f, int offset, int limit) {
                check(offset == 20 && limit == 10, "last-page offset and ten-row limit");
                return List.of();
            }
        }, Clock.systemUTC());
        OnlineSalesResult lastPage = paged.report(empty, false);
        check(lastPage.getPage() == 3 && lastPage.getPageCount() == 3, "21 orders produce three pages");
        OnlineSalesReportService tooLarge = new OnlineSalesReportService(new OnlineSalesReportDAO() {
            @Override
            public OnlineSalesSummary summarize(Connection c, OnlineSalesFilter f) { return new OnlineSalesSummary(); }
            @Override
            public int count(Connection c, OnlineSalesFilter f) { return 10001; }
        }, Clock.systemUTC());
        try {
            tooLarge.report(empty, true);
            throw new AssertionError("Oversized export silently accepted");
        } catch (ValidationException e) {
            check(e.getErrors().containsKey("general"), "oversized CSV rejected without partial results");
        }
    }

    private static int booking(Connection c, int user, int show, int total, int discount, int amount, String status,
                               String channel, String paidAt) throws Exception {
        return insert(c, "INSERT INTO Booking(CustomerID,ShowtimeID,TotalPrice,DiscountAmount,FinalAmount,BookingTime,Status,Channel,PaidAt) VALUES (?,?,?,?,?,'2026-11-05T23:59:00',?,?,?)",
                user, show, total, discount, amount, status, channel, paidAt);
    }

    private static int ticket(Connection c, int booking, int show, int seat, int price, String status, String code) throws Exception {
        return insert(c, "INSERT INTO Ticket(BookingID,ShowtimeID,SeatID,Price,Status,TicketCode) VALUES (?,?,?,?,?,?)",
                booking, show, seat, price, status, code.replace("-", "").substring(0, 18) + code.charAt(code.length() - 1));
    }

    private static void attempt(Connection c, int booking, String status, int amount, int received, String created,
                                String paid, String reference) throws Exception {
        insert(c, "INSERT INTO OnlinePayment(BookingID,Provider,ProviderTxnRef,Amount,ReceivedAmount,Status,CreatedAt,PaidAt) VALUES (?,'VNPay',?,?,?,?,?,?)",
                booking, reference, amount, received, status, created, paid);
    }

    private static int scalar(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) { rs.next(); return rs.getInt(1); }
    }

    private static int insert(Connection c, String sql, Object... values) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.length; i++) { ps.setObject(i + 1, values[i]); }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static void update(Connection c, String sql, Object... values) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) { ps.setObject(i + 1, values[i]); }
            ps.executeUpdate();
        }
    }

    private static void invalid(Map<String, String> input, String key) throws Exception {
        try { SERVICE.filter(input); throw new AssertionError("Accepted invalid filter " + key); }
        catch (ValidationException e) { check(e.getErrors().containsKey(key), "invalid filter rejected: " + key); }
    }

    private static void money(BigDecimal actual, String expected, String name) {
        check(actual.compareTo(new BigDecimal(expected)) == 0, name + ": " + actual + " expected " + expected);
    }

    private static void check(boolean condition, String name) {
        if (!condition) { throw new AssertionError(name); }
        checks++;
    }
}
