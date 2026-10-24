package vn.edu.fpt.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.service.BookingCheckout;
import vn.edu.fpt.service.BookingService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-13 bước 5: khách bấm Tiếp tục -> kiểm tra ghế còn trống và giữ ghế PENDING trong SEAT_HOLD_MINUTES phút
@WebServlet(BookingFlow.HOLD_PATH)
public class BookingHoldServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(BookingHoldServlet.class.getName());

    private final BookingService bookingService = new BookingService();

    // Chỉ xảy ra khi Guest gửi form rồi được đưa qua trang đăng nhập: quay về trang chọn ghế
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int showtimeId = BookingFlow.parseInt(request.getParameter("showtimeId"));
        response.sendRedirect(showtimeId > 0 ? BookingFlow.seatPageUrl(request, showtimeId)
                : request.getContextPath() + "/");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        HttpSession session = request.getSession();
        int showtimeId = BookingFlow.parseInt(request.getParameter("showtimeId"));
        List<Integer> seatIds = new ArrayList<>();
        String[] values = request.getParameterValues("seatId");
        if (values != null) {
            for (String value : values) {
                seatIds.add(BookingFlow.parseInt(value));
            }
        }
        boolean ageConfirmed = "1".equals(request.getParameter("ageConfirmed"));
        try {
            BookingCheckout checkout = bookingService.holdSeats(BookingFlow.currentUser(session), showtimeId,
                    seatIds, ageConfirmed, BookingFlow.currentCheckout(session));
            session.setAttribute(BookingFlow.SESSION_CHECKOUT, checkout);
            session.removeAttribute(BookingFlow.SESSION_CHECKOUT_ERRORS);
            session.removeAttribute(BookingFlow.SESSION_VOUCHER_INPUT);
            response.sendRedirect(request.getContextPath() + BookingFlow.CHECKOUT_PATH);
        } catch (ValidationException e) {
            String message = e.getErrors().values().iterator().next();
            session.setAttribute(BookingFlow.FLASH_ERROR, message);
            String url = BookingFlow.seatPageUrl(request, showtimeId);
            // Lỗi không do ghế bị chiếm thì giữ nguyên lựa chọn để khách khỏi chọn lại
            if (!Messages.BOOKING_SEAT_TAKEN.equals(message) && !seatIds.isEmpty()) {
                List<String> parts = new ArrayList<>();
                for (Integer id : seatIds) {
                    parts.add(String.valueOf(id));
                }
                url += "&seats=" + String.join(",", parts);
            }
            response.sendRedirect(url);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi giữ ghế khi đặt vé", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }
}
