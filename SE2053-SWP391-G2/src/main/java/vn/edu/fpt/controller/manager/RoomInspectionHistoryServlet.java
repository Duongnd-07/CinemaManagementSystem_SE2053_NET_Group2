package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.InspectionSlot;
import vn.edu.fpt.service.RoomInspectionService;
import vn.edu.fpt.service.RoomLayoutService;
import vn.edu.fpt.util.Messages;

// UC-35 A5: Manager xem lịch sử kiểm tra phòng theo ngày, phòng và trạng thái (chỉ xem).
// AuthFilter giới hạn /manager/* cho Manager.
@WebServlet("/manager/room-inspections")
public class RoomInspectionHistoryServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RoomInspectionHistoryServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/room-inspections.jsp";

    private final RoomInspectionService inspectionService = new RoomInspectionService();
    private final RoomLayoutService layoutService = new RoomLayoutService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate date = parseDate(request.getParameter("date"));
        int roomId = parseInt(request.getParameter("room"));
        String status = request.getParameter("status") == null ? "" : request.getParameter("status").trim();
        if (!RoomInspectionService.HISTORY_STATUSES.contains(status)) {
            status = "";
        }
        try {
            List<InspectionSlot> slots = inspectionService.history(date, roomId, status);
            long missed = slots.stream().filter(s -> InspectionSlot.MISSED.equals(s.getStatus())).count();
            long late = slots.stream().filter(InspectionSlot::isLate).count();
            request.setAttribute("slots", slots);
            request.setAttribute("missedCount", missed);
            request.setAttribute("lateCount", late);
            request.setAttribute("rooms", layoutService.listRooms());
            request.setAttribute("date", date);
            request.setAttribute("roomFilter", roomId);
            request.setAttribute("statusFilter", status);
            request.setAttribute("activeMenu", "room-inspections");
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu lịch sử kiểm tra phòng chiếu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
