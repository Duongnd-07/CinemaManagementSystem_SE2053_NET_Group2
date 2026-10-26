package vn.edu.fpt.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.LogLevel;
import vn.edu.fpt.service.ActivityLogSearchForm;
import vn.edu.fpt.service.ActivityLogService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.Messages;

// UC-32: View System Logs - chỉ đọc. AuthFilter đã giới hạn /admin/* cho Admin (E3).
@WebServlet("/admin/logs")
public class SystemLogServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(SystemLogServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/admin/system-logs.jsp";

    private final ActivityLogService logService = new ActivityLogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ActivityLogSearchForm form = new ActivityLogSearchForm();
        form.setFromDate(request.getParameter("from"));
        form.setToDate(request.getParameter("to"));
        form.setLevel(request.getParameter("level"));
        form.setEventType(request.getParameter("type"));
        form.setUser(request.getParameter("user"));
        form.setKeyword(request.getParameter("q"));
        logService.normalize(form);

        Map<String, String> errors = new LinkedHashMap<>();
        try {
            logService.validate(form);
            request.setAttribute("logPage", logService.search(form, parsePage(request.getParameter("page"))));
        } catch (ValidationException e) {
            // E2: báo lỗi cạnh ô ngày, không truy vấn
            errors = e.getErrors();
            request.setAttribute("flashError", errors.get(ActivityLogService.GENERAL_ERROR));
        } catch (SQLException e) {
            // E4: vẫn hiển thị trang và bộ lọc, chỉ báo không tải được
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi tải nhật ký hệ thống", e);
            request.setAttribute("flashError", Messages.LOG_LOAD_FAILED);
        }
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("levels", labels(LogLevel.ALL, LogLevel::labelOf));
        request.setAttribute("eventTypes", labels(LogEventType.ALL, LogEventType::labelOf));
        request.setAttribute("emptyMessage", Messages.LOG_NOT_FOUND);
        request.setAttribute("baseUrl", request.getContextPath() + "/admin/logs" + filterQuery(form));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // Mã -> nhãn hiển thị, giữ đúng thứ tự trong danh sách
    private Map<String, String> labels(List<String> codes, UnaryOperator<String> labelOf) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String code : codes) {
            result.put(code, labelOf.apply(code));
        }
        return result;
    }

    // A3: giữ nguyên bộ lọc khi chuyển trang
    private String filterQuery(ActivityLogSearchForm form) {
        StringBuilder query = new StringBuilder();
        append(query, "from", form.getFromDate());
        append(query, "to", form.getToDate());
        append(query, "level", form.getLevel());
        append(query, "type", form.getEventType());
        append(query, "user", form.getUser());
        append(query, "q", form.getKeyword());
        return query.toString();
    }

    private void append(StringBuilder query, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        query.append(query.length() == 0 ? '?' : '&')
                .append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private int parsePage(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
