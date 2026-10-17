package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
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
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.BookingCheckout;
import vn.edu.fpt.service.BookingService;
import vn.edu.fpt.service.BookingSeatView;
import vn.edu.fpt.service.SeatMapService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-13 bước 1-4: chọn ghế của suất chiếu. Công khai để Guest cũng chọn được ghế (AF1),
// chỉ khi bấm Tiếp tục mới yêu cầu đăng nhập. Trang chỉ đọc, ghế được giữ ở BookingHoldServlet.
@WebServlet(BookingFlow.SEAT_PAGE)
public class BookingSeatServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(BookingSeatServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/booking-seats.jsp";

    private final BookingService bookingService = new BookingService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        BookingFlow.moveFlashToRequest(session, request);
        int showtimeId = BookingFlow.parseInt(request.getParameter("showtimeId"));
        User user = BookingFlow.currentUser(session);
        try {
            BookingSeatView view = bookingService.loadSeatSelection(showtimeId);
            request.setAttribute("booking", view);
            request.setAttribute("seatMap", view.getSeatMap());
            // Ghế khách đang tự giữ (quay lại từ bước checkout) vẫn chọn được, không hiện là "Đang giữ"
            BookingCheckout checkout = BookingFlow.currentCheckout(session);
            List<Integer> ownSeatIds = checkout != null && checkout.getBookingId() == null
                    && checkout.getShowtimeId() == showtimeId ? checkout.getSeatIds() : List.of();
            String seatsParam = request.getParameter("seats");
            request.setAttribute("ownSeatIds", join(ownSeatIds));
            request.setAttribute("preselectedSeatIds",
                    seatsParam != null ? join(parseIds(seatsParam)) : join(ownSeatIds));
            request.setAttribute("statusUrl", request.getContextPath() + "/seat-map/status?id=" + showtimeId);
            request.setAttribute("refreshSeconds", SeatMapService.REFRESH_SECONDS);
        } catch (ValidationException e) {
            request.setAttribute("errorMessage", e.getErrors().values().iterator().next());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải sơ đồ chọn ghế", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
            return;
        }
        request.setAttribute("isGuest", user == null);
        request.setAttribute("canBook", user == null || Role.CUSTOMER.equals(user.getRole()));
        request.setAttribute("customerOnlyMessage", Messages.BOOKING_CUSTOMER_ONLY);
        request.setAttribute("seatTakenMessage", Messages.BOOKING_SEAT_TAKEN);
        request.setAttribute("fullyBookedMessage", Messages.SEAT_MAP_FULLY_BOOKED);
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private List<Integer> parseIds(String value) {
        List<Integer> ids = new ArrayList<>();
        for (String part : value.split(",")) {
            int id = BookingFlow.parseInt(part);
            if (id > 0 && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private String join(List<Integer> ids) {
        List<String> parts = new ArrayList<>();
        for (Integer id : ids) {
            parts.add(String.valueOf(id));
        }
        return String.join(",", parts);
    }
}
