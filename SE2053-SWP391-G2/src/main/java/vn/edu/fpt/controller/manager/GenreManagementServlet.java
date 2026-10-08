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
import vn.edu.fpt.model.Genre;
import vn.edu.fpt.service.GenrePage;
import vn.edu.fpt.service.GenreService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-34: Manage Movie Genres
@WebServlet("/manager/genres")
public class GenreManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GenreManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/genres.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final GenreService genreService = new GenreService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Genre editing = null;
            boolean showForm = false;
            String edit = request.getParameter("edit");
            if (edit != null) {
                editing = genreService.findById(parseInt(edit));
                if (editing == null) {
                    flash(request, FLASH_ERROR, Messages.GENRE_NOT_FOUND);
                } else {
                    showForm = true;
                }
            } else if ("1".equals(request.getParameter("add"))) {
                showForm = true;
            }
            render(request, response, showForm, editing == null ? 0 : editing.getGenreId(),
                    editing == null ? "" : editing.getGenreName(), new LinkedHashMap<>());
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
            if ("delete".equals(request.getParameter("action"))) {
                delete(request, response);
            } else {
                save(request, response);
            }
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void save(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        int genreId = parseInt(request.getParameter("genreId"));
        String name = trim(request.getParameter("name"));
        try {
            genreService.save(genreId, name);
        } catch (ValidationException e) {
            render(request, response, true, genreId, name, e.getErrors());
            return;
        }
        flash(request, FLASH_SUCCESS, Messages.GENRE_SAVED);
        response.sendRedirect(request.getContextPath() + "/manager/genres" + listQuery(request));
    }

    private void delete(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        try {
            genreService.delete(parseInt(request.getParameter("genreId")));
            flash(request, FLASH_SUCCESS, Messages.GENRE_DELETED);
        } catch (ValidationException e) {
            flash(request, FLASH_ERROR, e.getErrors().values().iterator().next());
        }
        response.sendRedirect(request.getContextPath() + "/manager/genres" + listQuery(request));
    }

    private void render(HttpServletRequest request, HttpServletResponse response, boolean showForm, int genreId,
                        String name, Map<String, String> errors) throws SQLException, ServletException, IOException {
        String keyword = trim(request.getParameter("q"));
        GenrePage page = genreService.list(keyword, parseInt(request.getParameter("page")));

        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("genrePage", page);
        request.setAttribute("keyword", keyword);
        request.setAttribute("listQuery", listQuery(request));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "genres");
        request.setAttribute("msgNoResult", Messages.MSG01);
        if (showForm) {
            request.setAttribute("showForm", true);
            request.setAttribute("formGenreId", genreId);
            request.setAttribute("formName", name);
            request.setAttribute("errors", errors);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // Giữ từ khóa/trang hiện tại khi quay lại danh sách sau thao tác
    private String listQuery(HttpServletRequest request) {
        List<String> parts = new ArrayList<>();
        for (String name : List.of("q", "page")) {
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
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu thể loại", e);
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
