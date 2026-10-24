package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.service.SeatMapCell;
import vn.edu.fpt.service.SeatMapService;
import vn.edu.fpt.service.SeatMapView;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.Messages;

// UC-09: View Real-Time Seat Map — công khai cho Guest và Customer.
// /seat-map hiển thị trang, /seat-map/status trả JSON để trình duyệt cập nhật trạng thái ghế định kỳ.
@WebServlet({SeatMapServlet.PAGE_PATH, SeatMapServlet.STATUS_PATH})
public class SeatMapServlet extends HttpServlet {
    static final String PAGE_PATH = "/seat-map";
    static final String STATUS_PATH = "/seat-map/status";
    private static final Logger LOGGER = Logger.getLogger(SeatMapServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/seat-map.jsp";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final SeatMapService seatMapService = new SeatMapService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean statusRequest = STATUS_PATH.equals(request.getServletPath());
        int showtimeId = parseInt(request.getParameter("id"));
        try {
            SeatMapView view = seatMapService.load(showtimeId);
            if (statusRequest) {
                writeJson(response, statusJson(view));
            } else {
                render(request, response, view, null);
            }
        } catch (ValidationException e) {
            String message = e.getErrors().get("general");
            if (statusRequest) {
                writeJson(response, "{\"error\":" + quote(message) + "}");
            } else {
                render(request, response, null, message);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu sơ đồ ghế", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, SeatMapView view,
                        String errorMessage) throws ServletException, IOException {
        if (view != null) {
            request.setAttribute("seatMap", view);
            request.setAttribute("showDate", view.getShowtime().getStartTime().format(DATE_FORMAT));
            request.setAttribute("showTime", view.getShowtime().getStartTime().format(HOUR_FORMAT));
            request.setAttribute("updatedAt", view.getLoadedAt().format(CLOCK_FORMAT));
            request.setAttribute("statusUrl", request.getContextPath() + STATUS_PATH
                    + "?id=" + view.getShowtime().getShowtimeId());
            request.setAttribute("refreshSeconds", SeatMapService.REFRESH_SECONDS);
        }
        request.setAttribute("errorMessage", errorMessage);
        request.setAttribute("fullyBookedMessage", Messages.SEAT_MAP_FULLY_BOOKED);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // {"updatedAt":"..","fullyBooked":..,"counts":{"AVAILABLE":n,..},"seats":{"<seatId>":"<status>",..}}
    private String statusJson(SeatMapView view) {
        List<String> counts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : view.getStatusCounts().entrySet()) {
            counts.add(quote(entry.getKey()) + ":" + entry.getValue());
        }
        List<String> seats = new ArrayList<>();
        for (SeatMapCell seat : view.getSeats()) {
            seats.add("\"" + seat.getSeatId() + "\":" + quote(seat.getStatus()));
        }
        return "{\"updatedAt\":" + quote(view.getLoadedAt().format(CLOCK_FORMAT))
                + ",\"fullyBooked\":" + view.isFullyBooked()
                + ",\"counts\":{" + String.join(",", counts) + "}"
                + ",\"seats\":{" + String.join(",", seats) + "}}";
    }

    private void writeJson(HttpServletResponse response, String json) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        // Trạng thái ghế thay đổi liên tục, không để trình duyệt/proxy trả bản cũ
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(json);
    }

    private String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '<' -> sb.append("\\u003c");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
