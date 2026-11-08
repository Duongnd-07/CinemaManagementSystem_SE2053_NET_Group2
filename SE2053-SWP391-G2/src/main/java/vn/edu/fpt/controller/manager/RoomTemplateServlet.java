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
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.MovieService;
import vn.edu.fpt.service.RoomLayoutForm;
import vn.edu.fpt.service.RoomLayoutService;
import vn.edu.fpt.service.RoomTemplateService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-25 (A4, A5): tab Phòng mẫu của Phòng chiếu & Sơ đồ ghế - danh sách, thêm (?add=1), sửa (?edit=ID),
// xóa (POST action=delete) và lưu sơ đồ một phòng thành phòng mẫu (POST action=fromRoom).
// Dùng chung trình chỉnh sửa sơ đồ ghế room-editor.jsp ở chế độ phòng mẫu. AuthFilter giới hạn /manager/* cho Manager.
@WebServlet("/manager/room-templates")
public class RoomTemplateServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RoomTemplateServlet.class.getName());
    private static final String LIST_VIEW = "/WEB-INF/views/manager/room-templates.jsp";
    private static final String EDITOR_VIEW = "/WEB-INF/views/manager/room-editor.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String TARGET = "RoomTemplate";

    private final RoomTemplateService templateService = new RoomTemplateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String edit = request.getParameter("edit");
            if (edit != null) {
                try {
                    renderEditor(request, response, templateService.loadForm(parseInt(edit)), new LinkedHashMap<>());
                } catch (ValidationException e) {
                    redirect(request, response, "/manager/room-templates", FLASH_ERROR, e.getErrors().get("general"));
                }
            } else if ("1".equals(request.getParameter("add"))) {
                renderEditor(request, response, templateService.newForm(), new LinkedHashMap<>());
            } else {
                moveFlashToRequest(request);
                request.setAttribute("templates", templateService.listTemplates());
                request.setAttribute("csrfToken", CsrfToken.get(request.getSession()));
                request.setAttribute("activeMenu", "rooms");
                request.getRequestDispatcher(LIST_VIEW).forward(request, response);
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
        try {
            String action = trim(request.getParameter("action"));
            switch (action) {
                case "delete" -> delete(request, response);
                case "fromRoom" -> saveFromRoom(request, response);
                default -> save(request, response);
            }
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void save(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        RoomLayoutForm form = new RoomLayoutForm();
        form.setRoomId(parseInt(request.getParameter("roomId")));
        form.setName(trim(request.getParameter("name")));
        form.setFormat(trim(request.getParameter("format")));
        form.setRows(trim(request.getParameter("rows")));
        form.setCols(trim(request.getParameter("cols")));
        form.setLayout(trim(request.getParameter("layout")));
        boolean created = form.getRoomId() <= 0;
        int templateId;
        try {
            templateId = templateService.save(form);
        } catch (ValidationException e) {
            if (RoomTemplateService.TEMPLATE_NOT_FOUND.equals(e.getErrors().get("general"))) {
                redirect(request, response, "/manager/room-templates", FLASH_ERROR, RoomTemplateService.TEMPLATE_NOT_FOUND);
            } else {
                renderEditor(request, response, form, e.getErrors());
            }
            return;
        }
        ActivityLogger.info(request, LogEventType.CATALOG, created ? "Thêm phòng mẫu" : "Cập nhật phòng mẫu",
                TARGET, templateId, "Phòng mẫu " + form.getName() + " (" + form.getFormat() + ", " + form.getRows()
                        + " hàng x " + form.getCols() + " cột)");
        redirect(request, response, "/manager/room-templates", FLASH_SUCCESS, RoomTemplateService.TEMPLATE_SAVED);
    }

    // A4: lỗi (trùng tên, phòng không còn) quay lại sơ đồ ghế của phòng kèm thông báo
    private void saveFromRoom(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int roomId = parseInt(request.getParameter("roomId"));
        String name = trim(request.getParameter("templateName"));
        String back = "/manager/rooms?edit=" + roomId;
        try {
            int templateId = templateService.saveFromRoom(roomId, name);
            ActivityLogger.info(request, LogEventType.CATALOG, "Lưu phòng chiếu thành phòng mẫu", TARGET, templateId,
                    "Phòng mẫu " + name.trim() + " lưu từ phòng #RM-" + roomId);
            redirect(request, response, back, FLASH_SUCCESS, RoomTemplateService.TEMPLATE_SAVED);
        } catch (ValidationException e) {
            Map<String, String> errors = e.getErrors();
            if (Messages.ROOM_NOT_FOUND.equals(errors.get("general"))) {
                redirect(request, response, "/manager/rooms", FLASH_ERROR, Messages.ROOM_NOT_FOUND);
                return;
            }
            redirect(request, response, back, FLASH_ERROR, String.join(" ", errors.values()));
        }
    }

    private void delete(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int templateId = parseInt(request.getParameter("templateId"));
        try {
            String name = templateService.delete(templateId);
            ActivityLogger.info(request, LogEventType.CATALOG, "Xóa phòng mẫu", TARGET, templateId, "Phòng mẫu " + name);
            redirect(request, response, "/manager/room-templates", FLASH_SUCCESS, RoomTemplateService.TEMPLATE_DELETED);
        } catch (ValidationException e) {
            redirect(request, response, "/manager/room-templates", FLASH_ERROR, e.getErrors().get("general"));
        }
    }

    private void renderEditor(HttpServletRequest request, HttpServletResponse response, RoomLayoutForm form,
                              Map<String, String> errors) throws ServletException, IOException {
        request.setAttribute("templateMode", true);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("formats", MovieService.FORMATS);
        request.setAttribute("maxRows", RoomLayoutService.MAX_ROWS);
        request.setAttribute("maxCols", RoomLayoutService.MAX_COLS);
        request.setAttribute("maxName", RoomLayoutService.MAX_NAME);
        request.setAttribute("csrfToken", CsrfToken.get(request.getSession()));
        request.setAttribute("activeMenu", "rooms");
        request.getRequestDispatcher(EDITOR_VIEW).forward(request, response);
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String path, String key,
                          String message) throws IOException {
        request.getSession().setAttribute(key, message);
        response.sendRedirect(request.getContextPath() + path);
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
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu phòng mẫu", e);
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
