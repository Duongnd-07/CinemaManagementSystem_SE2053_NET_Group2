package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.service.MovieService;
import vn.edu.fpt.service.RoomLayoutForm;
import vn.edu.fpt.service.RoomLayoutService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-25: Manage Rooms & Seat Layout - danh sách phòng, thêm (?add=1) và sửa (?edit=ID) phòng kèm sơ đồ ghế.
// AuthFilter đã giới hạn /manager/* cho Manager/Admin.
@WebServlet("/manager/rooms")
public class RoomManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RoomManagementServlet.class.getName());
    private static final String LIST_VIEW = "/WEB-INF/views/manager/rooms.jsp";
    private static final String EDITOR_VIEW = "/WEB-INF/views/manager/room-editor.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final RoomLayoutService roomService = new RoomLayoutService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String edit = request.getParameter("edit");
            if (edit != null) {
                try {
                    RoomLayoutForm form = roomService.loadForm(parseInt(edit));
                    renderEditor(request, response, form, new LinkedHashMap<>());
                } catch (ValidationException e) {
                    redirectToList(request, response, FLASH_ERROR, e.getErrors().get("general"));
                }
            } else if ("1".equals(request.getParameter("add"))) {
                renderEditor(request, response, roomService.newForm(), new LinkedHashMap<>());
            } else if (request.getParameter("info") != null) {
                try {
                    RoomLayoutForm form = roomService.loadInfoForm(parseInt(request.getParameter("info")));
                    renderList(request, response, form, new LinkedHashMap<>());
                } catch (ValidationException e) {
                    redirectToList(request, response, FLASH_ERROR, e.getErrors().get("general"));
                }
            } else {
                renderList(request, response, null, null);
            }
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
        RoomLayoutForm form = new RoomLayoutForm();
        form.setRoomId(parseInt(request.getParameter("roomId")));
        form.setName(trim(request.getParameter("name")));
        form.setFormat(trim(request.getParameter("format")));
        form.setStatus(trim(request.getParameter("status")));
        form.setRows(trim(request.getParameter("rows")));
        form.setCols(trim(request.getParameter("cols")));
        form.setLayout(trim(request.getParameter("layout")));
        try {
            if ("info".equals(request.getParameter("action"))) {
                saveInfo(request, response, form);
                return;
            }
            try {
                roomService.save(form);
            } catch (ValidationException e) {
                if (Messages.ROOM_NOT_FOUND.equals(e.getErrors().get("general"))) {
                    redirectToList(request, response, FLASH_ERROR, Messages.ROOM_NOT_FOUND);
                } else {
                    renderEditor(request, response, form, e.getErrors());
                }
                return;
            }
            redirectToList(request, response, FLASH_SUCCESS, Messages.ROOM_SAVED);
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    // Form Sửa (tên, định dạng, trạng thái) hiển thị dạng hộp thoại trên trang danh sách
    private void saveInfo(HttpServletRequest request, HttpServletResponse response, RoomLayoutForm form)
            throws SQLException, ServletException, IOException {
        try {
            roomService.saveInfo(form);
        } catch (ValidationException e) {
            if (Messages.ROOM_NOT_FOUND.equals(e.getErrors().get("general"))) {
                redirectToList(request, response, FLASH_ERROR, Messages.ROOM_NOT_FOUND);
            } else {
                renderList(request, response, form, e.getErrors());
            }
            return;
        }
        redirectToList(request, response, FLASH_SUCCESS, Messages.ROOM_INFO_SAVED);
    }

    // infoForm khác null thì mở hộp thoại Sửa của phòng đó
    private void renderList(HttpServletRequest request, HttpServletResponse response, RoomLayoutForm infoForm,
                            Map<String, String> errors) throws SQLException, ServletException, IOException {
        moveFlashToRequest(request);
        request.setAttribute("rooms", roomService.listRooms());
        request.setAttribute("activeMenu", "rooms");
        if (infoForm != null) {
            request.setAttribute("infoForm", infoForm);
            request.setAttribute("infoRoom", roomService.findRoom(infoForm.getRoomId()));
            request.setAttribute("infoUpcoming", roomService.countUpcomingShowtimes(infoForm.getRoomId()));
            request.setAttribute("errors", errors);
            request.setAttribute("formats", MovieService.FORMATS);
            request.setAttribute("maxName", RoomLayoutService.MAX_NAME);
            request.setAttribute("csrfToken", CsrfToken.get(request.getSession()));
        }
        request.getRequestDispatcher(LIST_VIEW).forward(request, response);
    }

    private void renderEditor(HttpServletRequest request, HttpServletResponse response, RoomLayoutForm form,
                              Map<String, String> errors) throws SQLException, ServletException, IOException {
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("room", roomService.findRoom(form.getRoomId()));
        request.setAttribute("upcomingShowtimes", roomService.countUpcomingShowtimes(form.getRoomId()));
        request.setAttribute("locks", roomService.findLocks(form.getRoomId()));
        request.setAttribute("formats", MovieService.FORMATS);
        request.setAttribute("maxRows", RoomLayoutService.MAX_ROWS);
        request.setAttribute("maxCols", RoomLayoutService.MAX_COLS);
        request.setAttribute("maxName", RoomLayoutService.MAX_NAME);
        request.setAttribute("csrfToken", CsrfToken.get(request.getSession()));
        request.setAttribute("activeMenu", "rooms");
        request.getRequestDispatcher(EDITOR_VIEW).forward(request, response);
    }

    private void redirectToList(HttpServletRequest request, HttpServletResponse response, String key, String message)
            throws IOException {
        request.getSession().setAttribute(key, message);
        response.sendRedirect(request.getContextPath() + "/manager/rooms");
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
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu phòng chiếu", e);
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
