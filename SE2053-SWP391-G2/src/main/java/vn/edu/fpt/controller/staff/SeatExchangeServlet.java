package vn.edu.fpt.controller.staff;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ExchangeOptions;
import vn.edu.fpt.service.ExchangeRecord;
import vn.edu.fpt.service.SeatExchangeService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-20: Exchange Broken Seat. GET: tìm vé + danh sách vé trên ghế hỏng; ?code -> phương án cho vé; ?done -> phiếu.
// POST action: exchange (đổi ghế theo phương án đã chọn), refund (hết ghế -> hoàn tiền)
@WebServlet("/staff/seat-exchange")
public class SeatExchangeServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(SeatExchangeServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/staff/seat-exchange.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String STAFF_ONLY = "Chỉ nhân viên mới được đổi ghế cho khách.";
    private static final String EXCHANGED = "Đổi ghế thành công.";
    private static final String REFUNDED = "Đã hoàn tiền vé cho khách.";
    private static final String RECORD_NOT_FOUND = "Không tìm thấy phiếu đổi ghế.";

    private final SeatExchangeService exchangeService = new SeatExchangeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isStaff(request, response)) {
            return;
        }
        try {
            int done = parseInt(request.getParameter("done"));
            if (done > 0) {
                ExchangeRecord record = exchangeService.record(done);
                if (record == null) {
                    flash(request, FLASH_ERROR, RECORD_NOT_FOUND);
                    response.sendRedirect(request.getContextPath() + "/staff/seat-exchange");
                    return;
                }
                request.setAttribute("record", record);
                render(request, response);
                return;
            }
            String code = trim(request.getParameter("code"));
            if (!code.isEmpty()) {
                renderOptions(request, response, code, new LinkedHashMap<>());
                return;
            }
            request.setAttribute("affected", exchangeService.listAffected());
            render(request, response);
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isStaff(request, response)) {
            return;
        }
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        int ticketId = parseInt(request.getParameter("ticketId"));
        String code = trim(request.getParameter("code"));
        String reason = request.getParameter("reason");
        boolean refund = "refund".equals(request.getParameter("action"));
        try {
            try {
                int exchangeId = refund
                        ? exchangeService.refund(ticketId, reason, user.getUserId())
                        : exchangeService.exchange(ticketId, request.getParameter("target"), reason, user.getUserId());
                flash(request, FLASH_SUCCESS, refund ? REFUNDED : EXCHANGED);
                response.sendRedirect(request.getContextPath() + "/staff/seat-exchange?done=" + exchangeId);
            } catch (ValidationException e) {
                renderOptions(request, response, code, e.getErrors());
            }
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    // Phương án tính lại theo ghế trống hiện tại; vé không còn đổi được thì quay về trang tìm vé kèm lý do
    private void renderOptions(HttpServletRequest request, HttpServletResponse response, String code,
                               Map<String, String> errors) throws SQLException, ServletException, IOException {
        ExchangeOptions options;
        try {
            options = exchangeService.options(code);
        } catch (ValidationException e) {
            flash(request, FLASH_ERROR, e.getErrors().values().iterator().next());
            response.sendRedirect(request.getContextPath() + "/staff/seat-exchange?lookup="
                    + URLEncoder.encode(code, StandardCharsets.UTF_8));
            return;
        }
        request.setAttribute("options", options);
        request.setAttribute("errors", errors);
        request.setAttribute("reason", trim(request.getParameter("reason")));
        request.setAttribute("target", trim(request.getParameter("target")));
        render(request, response);
    }

    private void render(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "seat-exchange");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // AuthFilter cho cả Manager vào /staff/*, nhưng đổi ghế tại phòng chiếu là việc của nhân viên (SRS: Staff)
    private boolean isStaff(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        if (user != null && Role.STAFF.equals(user.getRole())) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, STAFF_ONLY);
        return false;
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu đổi ghế hỏng", e);
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
