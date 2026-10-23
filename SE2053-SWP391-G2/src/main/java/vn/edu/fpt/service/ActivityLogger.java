package vn.edu.fpt.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.dao.ActivityLogDAO;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.ActivityLog;
import vn.edu.fpt.model.LogLevel;
import vn.edu.fpt.model.User;

/**
 * UC-32: ghi một dòng vào nhật ký hệ thống (bảng ActivityLog). Các UC gọi sau khi thao tác đã thành công.
 * Người thao tác lấy từ session; không có session (webhook, đăng nhập sai) thì để trống.
 * Ghi log lỗi không bao giờ làm hỏng nghiệp vụ: mọi exception chỉ được ghi ra server log.
 * BR-06: không truyền mật khẩu, OTP, token đặt lại mật khẩu hay dữ liệu thanh toán vào description.
 */
public final class ActivityLogger {
    private static final Logger LOGGER = Logger.getLogger(ActivityLogger.class.getName());
    private static final ActivityLogDAO LOG_DAO = new ActivityLogDAO();

    private ActivityLogger() {
    }

    public static void info(HttpServletRequest request, String eventType, String action, String targetType,
            Object targetId, String description) {
        log(request, null, LogLevel.INFO, eventType, action, targetType, targetId, description);
    }

    public static void warning(HttpServletRequest request, String eventType, String action, String targetType,
            Object targetId, String description) {
        log(request, null, LogLevel.WARNING, eventType, action, targetType, targetId, description);
    }

    public static void error(HttpServletRequest request, String eventType, String action, String targetType,
            Object targetId, String description) {
        log(request, null, LogLevel.ERROR, eventType, action, targetType, targetId, description);
    }

    // Chỉ ghi nếu chưa có dòng cùng hành động trên cùng đối tượng: sự kiện bên ngoài gửi lặp (IPN VNPay) không sinh log trùng
    public static void infoOnce(HttpServletRequest request, String eventType, String action, String targetType,
            Object targetId, String description) {
        try {
            if (targetId != null && LOG_DAO.exists(eventType, cut(action, 100), cut(targetType, 30),
                    cut(String.valueOf(targetId), 50))) {
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không kiểm tra được nhật ký trùng: " + eventType + " - " + action, e);
        }
        log(request, null, LogLevel.INFO, eventType, action, targetType, targetId, description);
    }

    // Chưa có người đăng nhập (đăng ký, đặt lại mật khẩu): ghi lại email của tài khoản để Admin lọc theo người dùng
    public static void infoForEmail(HttpServletRequest request, String email, String eventType, String action,
            String targetType, Object targetId, String description) {
        log(request, email, LogLevel.INFO, eventType, action, targetType, targetId, description);
    }

    // Chưa có người đăng nhập (vd. đăng nhập sai): ghi lại email đã nhập để Admin lọc theo người dùng
    public static void warningForEmail(HttpServletRequest request, String email, String eventType, String action,
            String description) {
        log(request, email, LogLevel.WARNING, eventType, action, null, null, description);
    }

    // Dùng khi request đã kết thúc (ghi ở luồng nền): người thao tác và IP được lấy sẵn từ trước
    public static void error(User actor, String ipAddress, String eventType, String action, String description) {
        write(actor, null, ipAddress, LogLevel.ERROR, eventType, action, null, null, description);
    }

    public static User currentUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute(AuthFilter.SESSION_USER);
    }

    private static void log(HttpServletRequest request, String fallbackEmail, String level, String eventType,
            String action, String targetType, Object targetId, String description) {
        write(currentUser(request), fallbackEmail, request == null ? null : request.getRemoteAddr(), level,
                eventType, action, targetType, targetId, description);
    }

    private static void write(User actor, String fallbackEmail, String ipAddress, String level, String eventType,
            String action, String targetType, Object targetId, String description) {
        try {
            ActivityLog log = new ActivityLog();
            log.setLevel(level);
            log.setEventType(eventType);
            log.setAction(cut(action, 100));
            if (actor != null) {
                log.setUserId(actor.getUserId() > 0 ? actor.getUserId() : null);
                log.setActorName(cut(actor.getFullName(), 100));
                log.setActorEmail(cut(actor.getEmail(), 100));
                log.setActorRole(cut(actor.getRole(), 20));
            } else {
                log.setActorEmail(cut(fallbackEmail, 100));
            }
            log.setTargetType(cut(targetType, 30));
            log.setTargetId(targetId == null ? null : cut(String.valueOf(targetId), 50));
            log.setDescription(cut(description, 1000));
            log.setIpAddress(cut(ipAddress, 45));
            LOG_DAO.insert(log);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không ghi được nhật ký hệ thống: " + eventType + " - " + action, e);
        }
    }

    private static String cut(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
