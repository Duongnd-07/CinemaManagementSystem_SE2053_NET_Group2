package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.ReviewStatus;
import vn.edu.fpt.model.ReviewView;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.ReviewService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-17: Manager ẩn/bỏ ẩn đánh giá phim vi phạm
@WebServlet("/manager/reviews")
public class ReviewManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ReviewManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/reviews.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String idParam = trim(request.getParameter("id"));
            ReviewView review = null;
            if (!idParam.isEmpty()) {
                review = reviewService.findById(parseInt(idParam));
                if (review == null) {
                    redirectWithError(request, response, Messages.REVIEW_NOT_FOUND);
                    return;
                }
            }
            render(request, response, review, new LinkedHashMap<>());
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            String action = request.getParameter("action");
            if ("hide".equals(action)) {
                hide(request, response);
            } else if ("unhide".equals(action)) {
                unhide(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void hide(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        int reviewId = parseInt(request.getParameter("reviewId"));
        String reason = request.getParameter("reason");
        try {
            reviewService.hide(reviewId, currentUserId(request), reason);
            ActivityLogger.info(request, LogEventType.CATALOG, "Ẩn đánh giá phim vi phạm", "Review", reviewId,
                    reason);
            flash(request, FLASH_SUCCESS, Messages.REVIEW_HIDDEN);
            response.sendRedirect(listUrl(request));
        } catch (ValidationException e) {
            ReviewView review = reviewService.findById(reviewId);
            if (review == null) {
                // Khách vừa xóa review trong lúc Manager đang mở hộp thoại
                redirectWithError(request, response, Messages.REVIEW_NOT_FOUND);
                return;
            }
            // Nạp lại để hiện lỗi ngay trên hộp thoại ẩn đang mở
            render(request, response, review, e.getErrors());
        }
    }

    private void unhide(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int reviewId = parseInt(request.getParameter("reviewId"));
        try {
            reviewService.unhide(reviewId);
            ActivityLogger.info(request, LogEventType.CATALOG, "Bỏ ẩn đánh giá phim", "Review", reviewId, null);
            flash(request, FLASH_SUCCESS, Messages.REVIEW_UNHIDDEN);
        } catch (ValidationException e) {
            flash(request, FLASH_ERROR, e.getErrors().values().iterator().next());
        }
        response.sendRedirect(listUrl(request));
    }

    private void render(HttpServletRequest request, HttpServletResponse response, ReviewView review,
                        Map<String, String> errors) throws SQLException, ServletException, IOException {
        String status = trim(request.getParameter("status"));

        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("reviewPage", reviewService.listAll(status, parseInt(request.getParameter("page"))));
        request.setAttribute("statusCounts", reviewService.countAllByStatus());
        request.setAttribute("statusFilter", ReviewStatus.isValid(status) ? status : "");
        request.setAttribute("statuses", ReviewStatus.ALL);
        request.setAttribute("listQuery", listQuery(request));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "reviews");
        request.setAttribute("msgNoResult", Messages.MSG01);
        if (review != null) {
            request.setAttribute("review", review);
            request.setAttribute("errors", errors);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void redirectWithError(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        flash(request, FLASH_ERROR, message);
        response.sendRedirect(listUrl(request));
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private String listUrl(HttpServletRequest request) {
        return request.getContextPath() + "/manager/reviews" + listQuery(request);
    }

    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        return user.getUserId();
    }

    // Giữ bộ lọc/trang hiện tại khi mở và đóng hộp thoại ẩn review
    private String listQuery(HttpServletRequest request) {
        List<String> parts = new ArrayList<>();
        for (String name : List.of("status", "page")) {
            String value = trim(request.getParameter(name));
            if (!value.isEmpty()) {
                parts.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return parts.isEmpty() ? "" : "?" + String.join("&", parts);
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi quản lý đánh giá phim", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
