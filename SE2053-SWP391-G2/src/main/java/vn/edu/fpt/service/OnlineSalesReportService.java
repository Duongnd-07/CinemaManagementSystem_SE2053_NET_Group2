package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import vn.edu.fpt.dao.OnlineSalesReportDAO;
import vn.edu.fpt.model.OnlineSalesDetail;
import vn.edu.fpt.model.OnlineSalesFilter;
import vn.edu.fpt.model.OnlineSalesResult;
import vn.edu.fpt.model.OnlineSalesRow;
import vn.edu.fpt.model.OnlineSalesSummary;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

public class OnlineSalesReportService {
    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final int PAGE_SIZE = 10;
    public static final int EXPORT_LIMIT = 10000;
    private final OnlineSalesReportDAO dao;
    private final Clock clock;

    public OnlineSalesReportService() {
        this(new OnlineSalesReportDAO(), Clock.system(VN_ZONE));
    }

    public OnlineSalesReportService(OnlineSalesReportDAO dao, Clock clock) {
        this.dao = dao;
        this.clock = clock.withZone(VN_ZONE);
    }

    public OnlineSalesFilter filter(Map<String, String> raw) throws ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();
        OnlineSalesFilter f = new OnlineSalesFilter();
        LocalDate today = LocalDate.now(clock);
        f.setFrom(date(raw.get("from"), today, "from", errors));
        f.setTo(date(raw.get("to"), today, "to", errors));
        String groupBy = raw.get("groupBy");
        if (groupBy != null && !groupBy.isBlank()) {
            if (!"day".equals(groupBy) && !"month".equals(groupBy)) {
                errors.put("general", Messages.ONLINE_REPORT_GROUP);
            } else {
                f.setGroupBy(groupBy);
            }
        }
        if (f.getFrom() != null && f.getTo() != null) {
            long days = ChronoUnit.DAYS.between(f.getFrom(), f.getTo());
            if (days < 0 || days >= 366) {
                errors.put("to", Messages.ONLINE_REPORT_RANGE);
            }
        }
        Integer page = id(raw.get("page"), "page", errors);
        f.setPage(page == null ? 1 : page);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return f;
    }

    public OnlineSalesResult report(OnlineSalesFilter f, boolean export) throws SQLException, ValidationException {
        return read(conn -> {
            OnlineSalesResult result = new OnlineSalesResult();
            result.setGeneratedAt(LocalDateTime.now(clock).withNano(0));
            OnlineSalesSummary summary = dao.summarize(conn, f);
            summary.setGrossAmount(summary.getTicketValue().add(summary.getConcessionValue()));
            result.setSummary(summary);
            int total = dao.count(conn, f);
            if (export && total > EXPORT_LIMIT) {
                throw new ValidationException(Map.of("general", Messages.ONLINE_REPORT_EXPORT_LIMIT));
            }
            int pages = Math.max(1, (int) ((total + (long) PAGE_SIZE - 1) / PAGE_SIZE));
            int page = Math.min(f.getPage(), pages);
            result.setTotalRows(total);
            result.setPage(page);
            result.setPageCount(pages);
            result.setRows(dao.rows(conn, f, export ? 0 : (page - 1) * PAGE_SIZE, export ? EXPORT_LIMIT : PAGE_SIZE));
            Map<LocalDate, BigDecimal> series = collectionSeries(f, dao.dailyNetCollections(conn, f));
            result.setCollections(series);
            result.setMaxCollection(series.values().stream().map(BigDecimal::abs)
                    .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            return result;
        });
    }

    static Map<LocalDate, BigDecimal> collectionSeries(OnlineSalesFilter f, Map<LocalDate, BigDecimal> daily) {
        boolean monthly = "month".equals(f.getGroupBy());
        Map<LocalDate, BigDecimal> series = new LinkedHashMap<>();
        LocalDate first = monthly ? f.getFrom().withDayOfMonth(1) : f.getFrom();
        for (LocalDate bucket = first; !bucket.isAfter(f.getTo());
                bucket = monthly ? bucket.plusMonths(1) : bucket.plusDays(1)) {
            series.put(bucket, BigDecimal.ZERO);
        }
        daily.forEach((day, amount) -> {
            if (!day.isBefore(f.getFrom()) && !day.isAfter(f.getTo())) {
                LocalDate bucket = monthly ? day.withDayOfMonth(1) : day;
                series.merge(bucket, amount, BigDecimal::add);
            }
        });
        return series;
    }

    public OnlineSalesDetail detail(int bookingId) throws SQLException, ValidationException {
        if (bookingId <= 0) {
            return null;
        }
        return read(conn -> dao.detail(conn, bookingId));
    }

    public String csv(OnlineSalesFilter f, OnlineSalesResult result) {
        StringBuilder out = new StringBuilder("\uFEFF");
        line(out, "Lịch sử bán vé online");
        line(out, "Từ ngày", f.getFrom(), "Đến ngày", f.getTo());
        line(out, "Cơ sở ngày", "Ngày thanh toán thành công");
        line(out, "Múi giờ", "Asia/Ho_Chi_Minh", "Đơn vị", "VND", "Tạo lúc", result.getGeneratedAt());
        line(out, "Mã đơn", "Tạo đơn", "Thanh toán", "Phim", "Phòng", "Suất", "Số vé", "Vé trước giảm",
                "Bắp nước", "Giảm giá", "Đã thu", "Hoàn tiền và bù ghế", "Thực thu", "Phương thức");
        for (OnlineSalesRow r : result.getRows()) {
            line(out, r.getBookingId(), r.getCreatedAt(), r.getPaidAt(), r.getTitle(), r.getRoomName(),
                    r.getShowtimeAt(), r.getTicketCount(), r.getTicketValue(), r.getConcessionValue(),
                    r.getDiscount(), r.getAmount(), r.getCashRefund(), r.getNetAmount(),
                    r.getAmount().signum() == 0 ? "Voucher/điểm bù toàn bộ" : r.getMethod());
        }
        return out.toString();
    }

    public static String csvCell(Object value) {
        String s = value == null ? "" : value instanceof BigDecimal amount ? amount.toPlainString() : value.toString();
        // Only trusted numeric objects bypass formula protection, so negative amounts remain numeric.
        if (!(value instanceof Number)) {
            String leading = s.stripLeading();
            if ((!leading.isEmpty() && "=+-@".indexOf(leading.charAt(0)) >= 0)
                    || s.startsWith("\t") || s.startsWith("\r") || s.startsWith("\n")) {
                s = "'" + s;
            }
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private void line(StringBuilder out, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(csvCell(values[i]));
        }
        out.append("\r\n");
    }

    private LocalDate date(String value, LocalDate fallback, String key, Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            LocalDate parsed = LocalDate.parse(value.trim());
            if (parsed.getYear() < 1900 || parsed.getYear() > 9998) {
                throw new DateTimeParseException("Unsupported SQL report year", value, 0);
            }
            return parsed;
        } catch (DateTimeParseException e) {
            errors.put(key, Messages.ONLINE_REPORT_DATE);
            return null;
        }
    }

    private Integer id(String value, String key, Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (!value.trim().matches("[1-9][0-9]{0,9}")) {
                throw new NumberFormatException();
            }
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            errors.put(key, Messages.ONLINE_REPORT_ID);
            return null;
        }
    }

    private <T> T read(ReadOperation<T> operation) throws SQLException, ValidationException {
        try (Connection conn = DBContext.getConnection()) {
            int isolation = conn.getTransactionIsolation();
            boolean autoCommit = conn.getAutoCommit();
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conn.setAutoCommit(false);
            try {
                T result = operation.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | ValidationException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
                conn.setTransactionIsolation(isolation);
            }
        }
    }

    @FunctionalInterface
    private interface ReadOperation<T> {
        T run(Connection conn) throws SQLException, ValidationException;
    }
}
