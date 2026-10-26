package vn.edu.fpt.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import vn.edu.fpt.dao.ActivityLogDAO;
import vn.edu.fpt.model.ActivityLog;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.LogLevel;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

// UC-32: View System Logs
public class ActivityLogService {
    public static final int PAGE_SIZE = 20;
    // BR-08: không nhập khoảng ngày thì xem 7 ngày gần nhất (tính cả hôm nay)
    public static final int DEFAULT_DAYS = 7;
    public static final int MAX_TEXT = 100;
    public static final String GENERAL_ERROR = "general";

    private final ActivityLogDAO logDAO = new ActivityLogDAO();

    // Chuẩn hóa bộ lọc; giá trị mức độ/loại sự kiện không hợp lệ bị bỏ qua thay vì báo lỗi
    public void normalize(ActivityLogSearchForm form) {
        form.setFromDate(trim(form.getFromDate()));
        form.setToDate(trim(form.getToDate()));
        form.setLevel(LogLevel.isValid(trim(form.getLevel())) ? trim(form.getLevel()) : "");
        form.setEventType(LogEventType.isValid(trim(form.getEventType())) ? trim(form.getEventType()) : "");
        form.setUser(cut(trim(form.getUser())));
        form.setKeyword(cut(trim(form.getKeyword())));
        if (form.getFromDate().isEmpty() && form.getToDate().isEmpty()) {
            LocalDate today = LocalDate.now();
            form.setFromDate(today.minusDays(DEFAULT_DAYS - 1L).toString());
            form.setToDate(today.toString());
        }
    }

    // E2: ngày sai định dạng hoặc ngày bắt đầu sau ngày kết thúc thì không truy vấn
    public void validate(ActivityLogSearchForm form) throws ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();
        LocalDate from = parseDate(form.getFromDate(), "fromDate", errors);
        LocalDate to = parseDate(form.getToDate(), "toDate", errors);
        if (from != null && to != null && from.isAfter(to)) {
            errors.put("fromDate", Messages.LOG_DATE_ORDER);
        }
        if (!errors.isEmpty()) {
            errors.put(GENERAL_ERROR, Messages.LOG_INVALID_FILTER);
            throw new ValidationException(errors);
        }
        form.setFrom(from);
        form.setTo(to);
    }

    public Page<ActivityLog> search(ActivityLogSearchForm form, int page) throws SQLException {
        int total = logDAO.count(form.getFrom(), form.getTo(), form.getLevel(), form.getEventType(),
                form.getUser(), form.getKeyword());
        return Page.of(page, PAGE_SIZE, total, (offset, limit) -> logDAO.search(form.getFrom(), form.getTo(),
                form.getLevel(), form.getEventType(), form.getUser(), form.getKeyword(), offset, limit));
    }

    // input type="date" luôn gửi yyyy-MM-dd
    private LocalDate parseDate(String value, String field, Map<String, String> errors) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            errors.put(field, Messages.LOG_INVALID_DATE);
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String cut(String value) {
        return value.length() <= MAX_TEXT ? value : value.substring(0, MAX_TEXT);
    }
}
