package vn.edu.fpt.controller.staff;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.RoomInspectionService;
import vn.edu.fpt.service.ShiftService;

// Nghiệp vụ Cổng làm việc Nhân viên rạp (Staff Portal)
@WebServlet("/staff/dashboard")
public class StaffDashboardServlet extends HttpServlet {
    private static final String VIEW = "/WEB-INF/views/staff/dashboard.jsp";
    private static final Logger LOGGER = Logger.getLogger(StaffDashboardServlet.class.getName());

    private final RoomInspectionService inspectionService = new RoomInspectionService();
    private final ShiftService shiftService = new ShiftService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveFlashToRequest(session, request);

        User user = (User) session.getAttribute(AuthFilter.SESSION_USER);
        request.setAttribute("currentUser", user);
        setOpenShift(request, user);
        setInspectionWarning(request, user);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // UC-22: ca đang mở cho ô trạng thái; findOpenShift tự bỏ ca đã kết / quá giờ. Lỗi DB thì coi như chưa vào ca
    private void setOpenShift(HttpServletRequest request, User user) {
        try {
            request.setAttribute("openShift", user == null ? null : shiftService.findOpenShift(user.getUserId()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không đọc được ca đang mở (UC-22)", e);
        }
    }

    // UC-35 BR-35-03: cảnh báo phòng chưa kiểm tra cho các suất trong 30 phút tới. Chỉ là cảnh báo nên lỗi ở đây
    // (vd. DB chưa chạy sql/UC35_room_inspection.sql) không được làm hỏng bàn làm việc: bỏ qua cảnh báo, ghi log.
    private void setInspectionWarning(HttpServletRequest request, User user) {
        try {
            int count = inspectionService.countUrgent(inspectionService.listToday(user == null ? 0 : user.getUserId()));
            request.setAttribute("uncheckedRooms", count);
            request.setAttribute("uncheckedMessage", inspectionService.urgentMessage(count));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không đếm được phòng chưa kiểm tra (UC-35)", e);
        }
    }

    private void moveFlashToRequest(HttpSession session, HttpServletRequest request) {
        for (String key : List.of("flashSuccess", "flashError")) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }
}
