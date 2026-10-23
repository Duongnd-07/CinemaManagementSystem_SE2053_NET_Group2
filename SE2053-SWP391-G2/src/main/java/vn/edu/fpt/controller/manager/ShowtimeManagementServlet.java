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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.service.BatchShowtimeForm;
import vn.edu.fpt.service.ShowtimeForm;
import vn.edu.fpt.service.ShowtimeService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

@WebServlet("/manager/showtimes")
public class ShowtimeManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ShowtimeManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/showtimes.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final DateTimeFormatter DATETIME_LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final ShowtimeService showtimeService = new ShowtimeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ShowtimeForm form = null;
            boolean showForm = false;
            Showtime editingShowtime = null;

            String editId = trim(request.getParameter("edit"));
            if (!editId.isEmpty()) {
                Showtime existing = showtimeService.findById(parseInt(editId));
                if (existing == null) {
                    request.getSession().setAttribute(FLASH_ERROR, Messages.SHOWTIME_NOT_FOUND);
                } else {
                    editingShowtime = existing;
                    form = toForm(existing);
                    showForm = true;
                }
            } else if ("1".equals(request.getParameter("add"))) {
                form = new ShowtimeForm();
                showForm = true;
            }

            render(request, response, form, showForm, editingShowtime, new LinkedHashMap<>());
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
            if ("batch_save".equals(action)) {
                handleBatchSave(request, response);
                return;
            }
            if ("cancel".equals(action)) {
                handleCancel(request, response);
                return;
            }
            if ("delete".equals(action)) {
                handleDelete(request, response);
                return;
            }
            handleSave(request, response);
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void handleBatchSave(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        BatchShowtimeForm form = new BatchShowtimeForm();
        form.setMovieId(trim(request.getParameter("batchMovieId")));
        form.setRoomId(trim(request.getParameter("batchRoomId")));
        form.setStartDate(trim(request.getParameter("startDate")));
        form.setEndDate(trim(request.getParameter("endDate")));
        form.setBasePrice(trim(request.getParameter("batchBasePrice")));
        form.setSkipCollisions("1".equals(request.getParameter("skipCollisions")));

        String[] days = request.getParameterValues("daysOfWeek");
        if (days != null) {
            List<Integer> dList = new ArrayList<>();
            for (String d : days) {
                try {
                    dList.add(Integer.parseInt(d.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
            form.setDaysOfWeek(dList);
        }

        String[] slots = request.getParameterValues("timeSlots");
        if (slots != null) {
            List<String> sList = new ArrayList<>();
            for (String s : slots) {
                if (s != null && !s.trim().isEmpty()) {
                    sList.add(s.trim());
                }
            }
            form.setTimeSlots(sList);
        }

        try {
            int count = showtimeService.saveBatch(form);
            Movie movie = showtimeService.findPublicMovies().stream()
                    .filter(m -> String.valueOf(m.getMovieId()).equals(form.getMovieId()))
                    .findFirst().orElse(null);
            Room room = showtimeService.findAllRooms().stream()
                    .filter(r -> String.valueOf(r.getRoomId()).equals(form.getRoomId()))
                    .findFirst().orElse(null);
            String movieTitle = movie != null ? movie.getTitle() : "phim";
            String roomName = room != null ? room.getRoomName() : "phòng";

            request.getSession().setAttribute(FLASH_SUCCESS,
                    Messages.format(Messages.SHOWTIME_BATCH_SAVED, count, movieTitle, roomName));
        } catch (ValidationException e) {
            request.getSession().setAttribute(FLASH_ERROR, e.getErrors().values().iterator().next());
        }
        response.sendRedirect(request.getContextPath() + "/manager/showtimes" + listQuery(request));
    }

    private void handleSave(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        ShowtimeForm form = readForm(request);
        Integer showtimeId = parseNullableInt(request.getParameter("showtimeId"));
        if (showtimeId != null) {
            form.setShowtimeId(showtimeId);
        }

        try {
            showtimeService.save(showtimeId, form);
            request.getSession().setAttribute(FLASH_SUCCESS,
                    showtimeId == null || showtimeId <= 0 ? Messages.SHOWTIME_SAVED : Messages.SHOWTIME_UPDATED);
            response.sendRedirect(request.getContextPath() + "/manager/showtimes" + listQuery(request));
        } catch (ValidationException e) {
            Showtime editingShowtime = null;
            if (showtimeId != null && showtimeId > 0) {
                editingShowtime = showtimeService.findById(showtimeId);
            }
            render(request, response, form, true, editingShowtime, e.getErrors());
        }
    }

    private void handleCancel(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int showtimeId = parseInt(request.getParameter("showtimeId"));
        try {
            showtimeService.cancel(showtimeId);
            request.getSession().setAttribute(FLASH_SUCCESS, Messages.SHOWTIME_CANCELLED);
        } catch (ValidationException e) {
            request.getSession().setAttribute(FLASH_ERROR, e.getErrors().values().iterator().next());
        }
        response.sendRedirect(request.getContextPath() + "/manager/showtimes" + listQuery(request));
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int showtimeId = parseInt(request.getParameter("showtimeId"));
        try {
            showtimeService.delete(showtimeId);
            request.getSession().setAttribute(FLASH_SUCCESS, Messages.SHOWTIME_DELETED);
        } catch (ValidationException e) {
            request.getSession().setAttribute(FLASH_ERROR, e.getErrors().values().iterator().next());
        } catch (SQLException e) {
            if (e.getErrorCode() == 547) { // Chống lỗi Foreign Key 500 (Góp ý 3)
                request.getSession().setAttribute(FLASH_ERROR, Messages.SHOWTIME_DELETE_HAS_REFERENCES);
            } else {
                throw e;
            }
        }
        response.sendRedirect(request.getContextPath() + "/manager/showtimes" + listQuery(request));
    }

    private void render(HttpServletRequest request, HttpServletResponse response, ShowtimeForm form,
                        boolean showForm, Showtime editingShowtime, Map<String, String> errors)
            throws SQLException, ServletException, IOException {
        String dateStr = trim(request.getParameter("date"));
        Integer movieId = parseNullableInt(request.getParameter("movieId"));
        Integer roomId = parseNullableInt(request.getParameter("roomId"));
        String status = trim(request.getParameter("status"));
        if (!ShowtimeStatus.isValid(status)) {
            status = "";
        }
        int page = parseInt(request.getParameter("page"));

        Page<Showtime> showtimePage = showtimeService.listManager(movieId, roomId, dateStr, status, page);
        Map<String, Integer> statusCounts = showtimeService.countByStatus(movieId, roomId, dateStr);
        List<Movie> movies = showtimeService.findPublicMovies();
        List<Room> rooms = showtimeService.findAllRooms();

        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object val = session.getAttribute(key);
            if (val != null) {
                request.setAttribute(key, val);
                session.removeAttribute(key);
            }
        }

        request.setAttribute("showtimePage", showtimePage);
        request.setAttribute("statusCounts", statusCounts);
        request.setAttribute("movies", movies);
        request.setAttribute("rooms", rooms);
        request.setAttribute("selectedDate", dateStr);
        request.setAttribute("selectedMovieId", movieId);
        request.setAttribute("selectedRoomId", roomId);
        request.setAttribute("selectedStatus", status);
        request.setAttribute("listQuery", listQuery(request));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "showtimes");
        request.setAttribute("showForm", showForm);
        request.setAttribute("form", form == null ? new ShowtimeForm() : form);
        request.setAttribute("editingShowtime", editingShowtime);
        request.setAttribute("errors", errors);
        request.setAttribute("timeFormat", DateTimeFormatter.ofPattern("HH:mm"));
        request.setAttribute("dateFormat", DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        request.setAttribute("fullFormat", DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));

        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private ShowtimeForm toForm(Showtime s) {
        ShowtimeForm form = new ShowtimeForm();
        form.setShowtimeId(s.getShowtimeId());
        form.setMovieId(String.valueOf(s.getMovieId()));
        form.setRoomId(String.valueOf(s.getRoomId()));
        if (s.getStartTime() != null) {
            form.setStartTime(s.getStartTime().format(DATETIME_LOCAL_FORMAT));
        }
        if (s.getBasePrice() != null) {
            form.setBasePrice(s.getBasePrice().stripTrailingZeros().toPlainString());
        }
        form.setStatus(s.getStatus() != null ? s.getStatus() : ShowtimeStatus.ACTIVE);
        return form;
    }

    private ShowtimeForm readForm(HttpServletRequest request) {
        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(trim(request.getParameter("movieId")));
        form.setRoomId(trim(request.getParameter("roomId")));
        form.setStartTime(trim(request.getParameter("startTime")));
        form.setBasePrice(trim(request.getParameter("basePrice")));
        // Bỏ ô chọn status theo Góp ý 4
        return form;
    }

    private String listQuery(HttpServletRequest request) {
        List<String> parts = new ArrayList<>();
        for (String name : List.of("date", "movieId", "roomId", "status", "page")) {
            String val = trim(request.getParameter(name));
            if (!val.isEmpty()) {
                parts.add(name + "=" + URLEncoder.encode(val, StandardCharsets.UTF_8));
            }
        }
        return parts.isEmpty() ? "" : "?" + String.join("&", parts);
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu suất chiếu", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Integer parseNullableInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
