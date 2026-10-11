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
import vn.edu.fpt.model.Concession;
import vn.edu.fpt.model.ConcessionStatus;
import vn.edu.fpt.service.ConcessionForm;
import vn.edu.fpt.service.ConcessionPage;
import vn.edu.fpt.service.ConcessionService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-27: Manage Concession Items
@WebServlet("/manager/concessions")
public class ConcessionManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ConcessionManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/concessions.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String ITEM_LABEL = "món";

    private final ConcessionService concessionService = new ConcessionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ConcessionForm form = null;
            String edit = request.getParameter("edit");
            if (edit != null) {
                Concession concession = concessionService.findById(parseInt(edit));
                if (concession == null) {
                    flash(request, FLASH_ERROR, Messages.CONCESSION_NOT_FOUND);
                } else {
                    form = concessionService.toForm(concession);
                }
            } else if ("1".equals(request.getParameter("add"))) {
                form = defaultForm();
            }
            render(request, response, form, new LinkedHashMap<>());
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
            if ("hide".equals(request.getParameter("action"))) {
                hide(request, response);
            } else {
                save(request, response);
            }
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void save(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        ConcessionForm form = readForm(request);
        boolean created;
        try {
            created = concessionService.save(form);
        } catch (ValidationException e) {
            render(request, response, form, e.getErrors());
            return;
        }
        flash(request, FLASH_SUCCESS, Messages.format(created ? Messages.MSG04 : Messages.MSG03, ITEM_LABEL));
        response.sendRedirect(request.getContextPath() + "/manager/concessions" + listQuery(request));
    }

    private void hide(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        try {
            concessionService.hide(parseInt(request.getParameter("concessionId")));
            flash(request, FLASH_SUCCESS, Messages.CONCESSION_HIDDEN);
        } catch (ValidationException e) {
            flash(request, FLASH_ERROR, e.getErrors().values().iterator().next());
        }
        response.sendRedirect(request.getContextPath() + "/manager/concessions" + listQuery(request));
    }

    private void render(HttpServletRequest request, HttpServletResponse response, ConcessionForm form,
                        Map<String, String> errors) throws SQLException, ServletException, IOException {
        String keyword = trim(request.getParameter("q"));
        String status = trim(request.getParameter("status"));
        ConcessionPage page = concessionService.list(keyword, status, parseInt(request.getParameter("page")));

        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("concessionPage", page);
        request.setAttribute("keyword", keyword);
        request.setAttribute("statusFilter", ConcessionStatus.isValid(status) ? status : "");
        request.setAttribute("statuses", ConcessionStatus.ALL);
        request.setAttribute("formStatuses", ConcessionService.FORM_STATUSES);
        request.setAttribute("listQuery", listQuery(request));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "concessions");
        request.setAttribute("msgNoResult", Messages.MSG01);
        if (form != null) {
            request.setAttribute("form", form);
            request.setAttribute("errors", errors);
            request.setAttribute("showForm", true);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private ConcessionForm readForm(HttpServletRequest request) {
        ConcessionForm form = new ConcessionForm();
        form.setConcessionId(parseInt(request.getParameter("concessionId")));
        form.setName(trim(request.getParameter("name")));
        form.setDescription(trim(request.getParameter("description")));
        form.setPrice(trim(request.getParameter("price")));
        form.setStockQuantity(trim(request.getParameter("stockQuantity")));
        form.setImageUrl(trim(request.getParameter("imageUrl")));
        form.setStatus(trim(request.getParameter("itemStatus")));
        return form;
    }

    private ConcessionForm defaultForm() {
        ConcessionForm form = new ConcessionForm();
        form.setStatus(ConcessionStatus.ACTIVE);
        return form;
    }

    // Giữ bộ lọc/trang hiện tại khi quay lại danh sách sau thao tác
    private String listQuery(HttpServletRequest request) {
        List<String> parts = new ArrayList<>();
        for (String name : List.of("q", "status", "page")) {
            String value = trim(request.getParameter(name));
            if (!value.isEmpty()) {
                parts.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return parts.isEmpty() ? "" : "?" + String.join("&", parts);
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu bắp nước", e);
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
