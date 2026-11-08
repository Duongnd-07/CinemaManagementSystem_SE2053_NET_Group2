package vn.edu.fpt.controller.staff;

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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.EquipmentReport;
import vn.edu.fpt.model.InspectionSlot;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.RoomInspectionService;
import vn.edu.fpt.service.RoomLayoutForm;
import vn.edu.fpt.service.RoomLayoutService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-35: Inspect & Clean Screening Room - danh sách suất chiếu hôm nay; ?showtimeId=ID mở bảng kiểm tra.
// POST action: save (xác nhận), update (sửa trong 5 phút), undo (hoàn tác trong 5 phút).
// AuthFilter cho Staff/Manager vào /staff/*; chỉ Staff được kiểm tra, Manager xem lịch sử ở /manager/room-inspections.
@WebServlet("/staff/room-inspections")
public class RoomInspectionServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RoomInspectionServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/staff/room-inspections.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String FORBIDDEN = "Bạn không có quyền truy cập vào khu vực này.";
    private static final String TARGET = "Showtime";

    private final RoomInspectionService inspectionService = new RoomInspectionService();
    private final RoomLayoutService layoutService = new RoomLayoutService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request);
        if (!Role.STAFF.equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/manager/room-inspections");
            return;
        }
        try {
            moveFlashToRequest(request);
            List<InspectionSlot> slots = inspectionService.listToday(user.getUserId());
            int urgent = inspectionService.countUrgent(slots);
            request.setAttribute("slots", slots);
            request.setAttribute("urgentCount", urgent);
            request.setAttribute("urgentMessage", inspectionService.urgentMessage(urgent));
            InspectionSlot selected = inspectionService.findSlot(parseInt(request.getParameter("showtimeId")),
                    user.getUserId());
            if (selected != null) {
                renderChecklist(request, selected, user.getUserId());
            }
            request.setAttribute("csrfToken", CsrfToken.get(request.getSession()));
            request.setAttribute("activeMenu", "room-inspections");
            request.setAttribute("maxNote", RoomInspectionService.MAX_NOTE);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request);
        if (!Role.STAFF.equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN);
            return;
        }
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int showtimeId = parseInt(request.getParameter("showtimeId"));
        boolean seats = request.getParameter("seatsChecked") != null;
        boolean cleaned = request.getParameter("roomCleaned") != null;
        boolean equipment = request.getParameter("equipmentOk") != null;
        String note = request.getParameter("note");
        String action = request.getParameter("action") == null ? "" : request.getParameter("action");
        try {
            String message;
            switch (action) {
                case "update" -> {
                    int linked = inspectionService.update(showtimeId, user.getUserId(), seats, cleaned, equipment, note);
                    log(request, "Sửa kết quả kiểm tra phòng chiếu", showtimeId, seats, cleaned, equipment, linked);
                    message = RoomInspectionService.UPDATED;
                }
                case "undo" -> {
                    inspectionService.undo(showtimeId, user.getUserId());
                    ActivityLogger.info(request, LogEventType.OPERATION, "Hoàn tác kiểm tra phòng chiếu", TARGET,
                            showtimeId, "Suất chiếu #" + showtimeId + " trở về chưa kiểm tra");
                    message = RoomInspectionService.UNDONE;
                }
                default -> {
                    int linked = inspectionService.save(showtimeId, user.getUserId(), seats, cleaned, equipment, note);
                    log(request, "Kiểm tra phòng chiếu", showtimeId, seats, cleaned, equipment, linked);
                    message = RoomInspectionService.SAVED;
                }
            }
            flash(request, FLASH_SUCCESS, message);
        } catch (ValidationException e) {
            flash(request, FLASH_ERROR, String.join(" ", e.getErrors().values()));
            // Giữ dấu tick và ghi chú đang nhập để hiện lại
            HttpSession session = request.getSession();
            session.setAttribute("inspectionDraft", Map.of("seatsChecked", seats, "roomCleaned", cleaned,
                    "equipmentOk", equipment, "note", note == null ? "" : note));
        } catch (SQLException e) {
            handleSystemError(response, e);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/staff/room-inspections?showtimeId=" + showtimeId);
    }

    private void renderChecklist(HttpServletRequest request, InspectionSlot slot, int staffId) throws SQLException {
        request.setAttribute("selected", slot);
        List<EquipmentReport> linked = inspectionService.linkedReports(slot);
        List<EquipmentReport> pending = inspectionService.pendingReports(slot, staffId);
        // Báo cáo đã gắn vẫn được trả về trong pending (chỉ loại báo cáo của lần kiểm tra khác) nên bỏ trùng
        List<EquipmentReport> fresh = new ArrayList<>();
        for (EquipmentReport report : pending) {
            if (linked.stream().noneMatch(r -> r.getReportId() == report.getReportId())) {
                fresh.add(report);
            }
        }
        List<EquipmentReport> all = new ArrayList<>(linked);
        all.addAll(fresh);
        int affected = inspectionService.affectedTickets(all);
        // Thiết bị của phòng đã được báo hỏng trước đó và chưa xử lý xong (không trùng các báo cáo ở trên)
        List<EquipmentReport> open = new ArrayList<>();
        for (EquipmentReport report : inspectionService.openEquipmentReports(slot)) {
            if (all.stream().noneMatch(r -> r.getReportId() == report.getReportId())) {
                open.add(report);
            }
        }
        request.setAttribute("linkedReports", linked);
        request.setAttribute("pendingReports", fresh);
        request.setAttribute("openReports", open);
        request.setAttribute("affectedTickets", affected);
        request.setAttribute("affectedMessage", inspectionService.affectedMessage(affected));
        try {
            RoomLayoutForm layout = layoutService.loadForm(slot.getRoomId());
            request.setAttribute("seatLayout", layout);
            StringBuilder locks = new StringBuilder();
            for (Map.Entry<String, String> lock : layoutService.findLocks(slot.getRoomId()).entrySet()) {
                if (RoomLayoutService.LOCK_REPORT.equals(lock.getValue())) {
                    locks.append(locks.length() == 0 ? "" : ",").append(lock.getKey());
                }
            }
            request.setAttribute("reportedSeats", locks.toString());
        } catch (ValidationException e) {
            // Sơ đồ kiểu cũ không dựng được lưới: bảng kiểm tra vẫn dùng được, chỉ không hiện sơ đồ
            request.setAttribute("seatLayout", null);
        }
        HttpSession session = request.getSession();
        Object draft = session.getAttribute("inspectionDraft");
        if (draft != null) {
            request.setAttribute("draft", draft);
            session.removeAttribute("inspectionDraft");
        }
    }

    private void log(HttpServletRequest request, String action, int showtimeId, boolean seats, boolean cleaned,
                     boolean equipment, int linkedReports) {
        ActivityLogger.info(request, LogEventType.OPERATION, action, TARGET, showtimeId,
                "Suất chiếu #" + showtimeId + ": ghế " + (seats ? "đã kiểm tra" : "chưa kiểm tra")
                        + ", phòng " + (cleaned ? "đã dọn" : "chưa dọn")
                        + ", thiết bị khác " + (equipment ? "bình thường" : "chưa xác nhận")
                        + (linkedReports > 0 ? ", gắn " + linkedReports + " báo cáo sự cố" : ""));
    }

    private User currentUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private void moveFlashToRequest(HttpServletRequest request) {
        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu kiểm tra phòng chiếu", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
