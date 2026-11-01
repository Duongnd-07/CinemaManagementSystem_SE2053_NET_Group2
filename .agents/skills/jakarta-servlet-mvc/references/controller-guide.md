# Controller (Servlet 6.0) Reference Guide

This guide defines the standard structure and conventions for HTTP Controllers in `SE2053-SWP391-G2`.

---

## 1. Controller Template (CRUD / Admin Example)

```java
package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.service.MovieService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

@WebServlet("/manager/movies")
public class MovieManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(MovieManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/movies.jsp";
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final MovieService movieService = new MovieService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveFlashToRequest(session, request);

        String keyword = request.getParameter("keyword");
        int pageIndex = 1;
        try {
            String p = request.getParameter("page");
            if (p != null && !p.trim().isEmpty()) {
                pageIndex = Math.max(1, Integer.parseInt(p.trim()));
            }
        } catch (NumberFormatException e) {
            pageIndex = 1;
        }

        try {
            Page<Movie> moviePage = movieService.findMovies(keyword, pageIndex, DEFAULT_PAGE_SIZE);
            request.setAttribute("moviePage", moviePage);
            request.setAttribute("keyword", keyword);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load movie list", e);
            request.setAttribute("errorMessage", "Không thể tải danh sách phim. Vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // Validate CSRF token for state-modifying requests
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or expired CSRF token");
            return;
        }

        if ("create".equalsIgnoreCase(action)) {
            handleCreate(request, response);
        } else if ("delete".equalsIgnoreCase(action)) {
            handleDelete(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/manager/movies");
        }
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        try {
            String title = request.getParameter("title");
            int duration = Integer.parseInt(request.getParameter("duration"));
            
            movieService.createMovie(title, duration);
            
            // Post-Redirect-Get: Success flash message in session
            session.setAttribute("flashSuccess", "Thêm phim mới thành công!");
            response.sendRedirect(request.getContextPath() + "/manager/movies");
        } catch (ValidationException ve) {
            // Forward back to form on validation error to preserve input
            request.setAttribute("errorMessage", ve.getMessage());
            doGet(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating movie", e);
            session.setAttribute("flashError", "Có lỗi xảy ra khi tạo phim!");
            response.sendRedirect(request.getContextPath() + "/manager/movies");
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession();
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            movieService.deleteMovie(id);
            session.setAttribute("flashSuccess", "Xóa phim thành công!");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting movie", e);
            session.setAttribute("flashError", "Không thể xóa phim!");
        }
        response.sendRedirect(request.getContextPath() + "/manager/movies");
    }

    private void moveFlashToRequest(HttpSession session, HttpServletRequest request) {
        if (session == null) return;
        Object success = session.getAttribute("flashSuccess");
        if (success != null) {
            request.setAttribute("flashSuccess", success);
            session.removeAttribute("flashSuccess");
        }
        Object error = session.getAttribute("flashError");
        if (error != null) {
            request.setAttribute("flashError", error);
            session.removeAttribute("flashError");
        }
    }
}
```

---

## 2. Common Pitfalls to Avoid

* ❌ **Forwarding after a successful POST:** This causes the "Confirm Form Resubmission" dialog if the user clicks reload. Always `sendRedirect` after a successful POST.
* ❌ **Missing `request.getContextPath()` on redirect:** `response.sendRedirect("/manager/movies")` navigates to `http://localhost:8080/manager/movies` (dropping the context `/cinema`). Always write `response.sendRedirect(request.getContextPath() + "/manager/movies")`.
* ❌ **Calling DB directly in Servlet:** Don't put SQL queries or JDBC calls in the Servlet. Delegate to the `Service` layer.
