package vn.edu.fpt.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;

// UC-32 BR-02 (SYSTEM): ghi nhật ký mỗi khi request kết thúc bằng lỗi 5xx hoặc exception chưa được xử lý.
// Ghi ở luồng nền: lỗi thường do DB, chờ kết nối DB để ghi log không được làm chậm trang báo lỗi.
@WebFilter("/*")
public class ErrorLogFilter implements Filter {
    private ExecutorService writer;

    @Override
    public void init(FilterConfig filterConfig) {
        writer = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "activity-log-writer");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            record(req, "Lỗi chưa được xử lý", e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
            throw e;
        }
        if (resp.getStatus() >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            record(req, "Lỗi máy chủ (HTTP " + resp.getStatus() + ")", null);
        }
    }

    private void record(HttpServletRequest req, String action, String detail) {
        // Lấy dữ liệu từ request ngay tại đây, request không dùng được sau khi trả response
        User actor = ActivityLogger.currentUser(req);
        String ip = req.getRemoteAddr();
        String description = req.getMethod() + " " + req.getRequestURI() + (detail == null ? "" : " - " + detail);
        try {
            writer.execute(() -> ActivityLogger.error(actor, ip, LogEventType.SYSTEM, action, description));
        } catch (RejectedExecutionException e) {
            // Ứng dụng đang dừng: bỏ qua, lỗi gốc vẫn có trong server log
        }
    }

    // Thông báo của Jasper kèm cả đoạn mã JSP và stacktrace; chỉ giữ dòng đầu, chi tiết xem ở server log
    private String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int end = message.indexOf('\n');
        return (end < 0 ? message : message.substring(0, end)).trim();
    }

    @Override
    public void destroy() {
        writer.shutdown();
    }
}
