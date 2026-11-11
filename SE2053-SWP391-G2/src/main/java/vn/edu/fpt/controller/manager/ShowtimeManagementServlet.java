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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.service.ActivityLogger;
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
        form.setRoomIds(parameterList(request, "batchRoomId"));
        form.setStartDate(trim(request.getParameter("startDate")));
        form.setEndDate(trim(request.getParameter("endDate")));
        form.setBasePrice(trim(request.getParameter("batchBasePrice")));
        form.setSkipCollisions("1".equals(request.getParameter("skipCollisions")));
        form.setTranslationType(trim(request.getParameter("batchTranslationType")));

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
            request.getSession().setAttribute(FLASH_SUCCESS, Messages.format(Messages.SHOWTIME_BATCH_SAVED,
                    count, movieTitle(form.getMovieId()), roomNames(form.getRoomIds())));
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
        boolean created = showtimeId == null || showtimeId <= 0;

        try {
            if (created) {
                // Tạo mới: cùng phim, cùng giờ chiếu ở tất cả các phòng đã chọn
                int count = showtimeService.createMany(form);
                ActivityLogger.info(request, LogEventType.CATALOG, "Thêm suất chiếu", "Showtime", null,
                        "Phim #" + form.getMovieId() + ", phòng #" + String.join(", #", form.getRoomIds())
                                + ", bắt đầu " + form.getStartTime() + ", giá "
                                + (form.getBasePrice().isEmpty() ? "tự tính theo phòng" : form.getBasePrice())
                                + ", bản chiếu " + form.getTranslationType());
                request.getSession().setAttribute(FLASH_SUCCESS, count == 1 ? Messages.SHOWTIME_SAVED
                        : Messages.format(Messages.SHOWTIME_MULTI_SAVED, count, movieTitle(form.getMovieId()),
                                roomNames(form.getRoomIds())));
            } else {
                showtimeService.save(showtimeId, form);
                ActivityLogger.info(request, LogEventType.CATALOG, "Cập nhật suất chiếu", "Showtime", showtimeId,
                        "Phim #" + form.getMovieId() + ", phòng #" + form.getRoomId() + ", bắt đầu "
                                + form.getStartTime() + ", giá " + form.getBasePrice()
                                + ", bản chiếu " + form.getTranslationType());
                request.getSession().setAttribute(FLASH_SUCCESS, Messages.SHOWTIME_UPDATED);
            }
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
            ActivityLogger.info(request, LogEventType.CATALOG, "Hủy suất chiếu", "Showtime", showtimeId, null);
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
            ActivityLogger.info(request, LogEventType.CATALOG, "Xóa suất chiếu", "Showtime", showtimeId, null);
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
        String translationType = trim(request.getParameter("translationType"));
        int page = parseInt(request.getParameter("page"));

        Page<Showtime> showtimePage = showtimeService.listManager(movieId, roomId, dateStr, status, translationType, page);
        Map<String, Integer> statusCounts = showtimeService.countByStatus(movieId, roomId, dateStr, translationType);
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
        request.setAttribute("selectedTranslationType", translationType);
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
        request.setAttribute("today", java.time.LocalDate.now().toString()); // yyyy-MM-dd cho HTML min attribute

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
        form.setTranslationType(s.getTranslationType());
        return form;
    }

    // Form gửi tới URL còn giữ bộ lọc danh sách (movieId, roomId, translationType) và getParameter trả giá trị
    // trên URL trước, nên các ô của form dùng tên riêng để không bị bộ lọc ghi đè (giống form hàng loạt)
    private ShowtimeForm readForm(HttpServletRequest request) {
        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(trim(request.getParameter("showtimeMovieId")));
        // Form sửa gửi 1 phòng (select), form tạo mới gửi nhiều phòng (checkbox) cùng tên tham số
        form.setRoomId(trim(request.getParameter("showtimeRoomId")));
        form.setRoomIds(parameterList(request, "showtimeRoomId"));
        form.setStartTime(trim(request.getParameter("startTime")));
        form.setBasePrice(trim(request.getParameter("basePrice")));
        form.setTranslationType(trim(request.getParameter("showtimeTranslationType")));
        return form;
    }

    private List<String> parameterList(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? new ArrayList<>() : Arrays.asList(values);
    }

    private String movieTitle(String movieId) throws SQLException {
        return showtimeService.findPublicMovies().stream()
                .filter(m -> String.valueOf(m.getMovieId()).equals(movieId))
                .map(Movie::getTitle)
                .findFirst().orElse("phim");
    }

    // Tên các phòng theo thứ tự đã chọn, ví dụ "Phòng chiếu 1, Phòng chiếu 3"
    private String roomNames(List<String> roomIds) throws SQLException {
        Map<String, String> names = new LinkedHashMap<>();
        for (Room room : showtimeService.findAllRooms()) {
            names.put(String.valueOf(room.getRoomId()), room.getRoomName());
        }
        List<String> selected = new ArrayList<>();
        for (String id : roomIds) {
            selected.add(names.getOrDefault(id, "phòng #" + id));
        }
        return String.join(", ", selected);
    }

    private String listQuery(HttpServletRequest request) {
        List<String> parts = new ArrayList<>();
        for (String name : List.of("date", "movieId", "roomId", "status", "translationType", "page")) {
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
