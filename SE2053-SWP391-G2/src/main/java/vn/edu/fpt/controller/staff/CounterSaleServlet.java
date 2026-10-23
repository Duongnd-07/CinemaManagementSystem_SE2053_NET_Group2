package vn.edu.fpt.controller.staff;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.CounterQuote;
import vn.edu.fpt.service.CounterReceipt;
import vn.edu.fpt.service.CounterSaleForm;
import vn.edu.fpt.service.CounterSaleService;
import vn.edu.fpt.service.ShiftService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-19: Sell Ticket & Concession at Counter
// GET: chưa mở ca -> form mở ca; ?showtimeId -> màn hình bán; ?receipt -> hóa đơn; còn lại -> danh sách suất chiếu
// POST action: openShift, quote (tính tiền), confirm (xác nhận đã thu tiền)
@WebServlet("/staff/counter")
public class CounterSaleServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CounterSaleServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/staff/counter-sale.jsp";
    private static final String RECEIPT_VIEW = "/WEB-INF/views/staff/counter-receipt.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String FLASH_CHANGE = "counterChangeDue";
    private static final String STAFF_ONLY = "Chỉ nhân viên quầy mới được bán vé.";
    private static final String SHIFT_OPENED = "Đã mở ca, bạn có thể bắt đầu bán vé.";
    private static final String SALE_DONE = "Bán vé thành công.";
    private static final String RECEIPT_NOT_FOUND = "Không tìm thấy đơn bán tại quầy.";
    private static final String QTY_PREFIX = "qty_";

    private final CounterSaleService saleService = new CounterSaleService();
    private final ShiftService shiftService = new ShiftService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isStaff(request, response)) {
            return;
        }
        try {
            int receiptId = parseInt(request.getParameter("receipt"));
            if (receiptId > 0) {
                showReceipt(request, response, receiptId);
                return;
            }
            Shift shift = shiftService.findOpenShift(currentUserId(request));
            int showtimeId = parseInt(request.getParameter("showtimeId"));
            if (shift != null && showtimeId > 0) {
                CounterSaleForm form = new CounterSaleForm();
                form.setShowtimeId(showtimeId);
                renderSale(request, response, shift, form, null, new LinkedHashMap<>());
                return;
            }
            renderList(request, response, shift);
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isStaff(request, response)) {
            return;
        }
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int staffId = currentUserId(request);
        String action = trim(request.getParameter("action"));
        try {
            switch (action) {
                case "openShift" -> {
                    runAndFlash(request, () -> {
                        shiftService.open(staffId, trim(request.getParameter("counterNo")),
                                request.getParameter("openingCash"));
                        ActivityLogger.info(request, LogEventType.COUNTER, "Mở ca bán hàng", "Shift", null,
                                "Quầy " + trim(request.getParameter("counterNo")) + ", tiền đầu ca "
                                        + trim(request.getParameter("openingCash")));
                    }, SHIFT_OPENED);
                    redirect(request, response, "");
                }
                case "quote", "confirm" -> sell(request, response, staffId, "confirm".equals(action));
                default -> redirect(request, response, "");
            }
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void sell(HttpServletRequest request, HttpServletResponse response, int staffId, boolean confirm)
            throws SQLException, ServletException, IOException {
        Shift shift = shiftService.findOpenShift(staffId);
        CounterSaleForm form = readForm(request);
        if (shift == null) {
            flash(request, FLASH_ERROR, "Bạn cần mở ca trước khi bán vé.");
            redirect(request, response, "");
            return;
        }
        CounterQuote quote;
        try {
            if (confirm) {
                CounterQuote paid = saleService.confirm(form, staffId);
                ActivityLogger.info(request, LogEventType.COUNTER, "Bán vé tại quầy", "Booking", paid.getBookingId(),
                        // getConcessions() gồm cả món số lượng 0 trong menu nên cộng theo số lượng đã bán
                        paid.getSeats().size() + " ghế, "
                                + paid.getConcessions().stream().mapToInt(line -> line.getQuantity()).sum()
                                + " món bắp nước, thu "
                                + paid.getFinalAmount() + " VND (" + form.getPaymentMethod() + ")");
                flash(request, FLASH_SUCCESS, SALE_DONE);
                request.getSession().setAttribute(FLASH_CHANGE, paid.getChangeDue());
                redirect(request, response, "?receipt=" + paid.getBookingId());
                return;
            }
            quote = saleService.quote(form);
        } catch (ValidationException e) {
            Map<String, String> errors = e.getErrors();
            // Suất chiếu không còn mở bán (đã bắt đầu, phòng bị khóa...) thì quay về danh sách
            if (errors.containsKey(CounterSaleService.ERROR_GENERAL)) {
                flash(request, FLASH_ERROR, errors.get(CounterSaleService.ERROR_GENERAL));
                redirect(request, response, "");
                return;
            }
            if (errors.containsKey(CounterSaleService.ERROR_SHIFT)) {
                flash(request, FLASH_ERROR, errors.get(CounterSaleService.ERROR_SHIFT));
                redirect(request, response, "");
                return;
            }
            renderSale(request, response, shift, form, quoteOrNull(form), errors);
            return;
        }
        renderSale(request, response, shift, form, quote, quote.getErrors());
    }

    // Sau khi giao dịch bị hủy vẫn hiển thị lại tiền để nhân viên sửa; suất chiếu hỏng thì không có
    private CounterQuote quoteOrNull(CounterSaleForm form) throws SQLException {
        try {
            return saleService.quote(form);
        } catch (ValidationException e) {
            return null;
        }
    }

    private void renderList(HttpServletRequest request, HttpServletResponse response, Shift shift)
            throws SQLException, ServletException, IOException {
        LocalDate date = parseDate(request.getParameter("date"));
        moveFlash(request);
        shiftAttributes(request, shift);
        request.setAttribute("counters", ShiftService.COUNTERS);
        request.setAttribute("date", date);
        request.setAttribute("today", LocalDate.now());
        if (shift != null) {
            request.setAttribute("showtimes", saleService.listShowtimes(date));
        }
        forward(request, response, VIEW);
    }

    private void renderSale(HttpServletRequest request, HttpServletResponse response, Shift shift,
                            CounterSaleForm form, CounterQuote quote, Map<String, String> errors)
            throws SQLException, ServletException, IOException {
        try {
            request.setAttribute("booking", saleService.loadSeats(form.getShowtimeId()));
        } catch (ValidationException e) {
            flash(request, FLASH_ERROR, e.getErrors().values().iterator().next());
            redirect(request, response, "");
            return;
        }
        moveFlash(request);
        shiftAttributes(request, shift);
        request.setAttribute("form", form);
        request.setAttribute("quote", quote);
        request.setAttribute("errors", errors);
        request.setAttribute("concessions", quote != null ? quote.getConcessions() : saleService.listConcessions());
        request.setAttribute("paymentMethods", CounterSaleService.PAYMENT_METHODS);
        request.setAttribute("selectedSeats", String.join(",", form.getSeatIds()));
        forward(request, response, VIEW);
    }

    // Ca đang mở, giờ ca tự kết và cờ sắp hết giờ (báo trước WARNING_MINUTES phút, UC-22)
    private void shiftAttributes(HttpServletRequest request, Shift shift) throws SQLException {
        request.setAttribute("shift", shift);
        if (shift != null) {
            LocalDateTime plannedEnd = shiftService.plannedEnd(shift, shiftService.shiftHours());
            request.setAttribute("shiftEndsAt", plannedEnd);
            request.setAttribute("shiftEndingSoon",
                    !LocalDateTime.now().plusMinutes(ShiftService.WARNING_MINUTES).isBefore(plannedEnd));
        }
    }

    private void showReceipt(HttpServletRequest request, HttpServletResponse response, int bookingId)
            throws SQLException, ServletException, IOException {
        CounterReceipt receipt = saleService.findReceipt(bookingId);
        if (receipt == null) {
            flash(request, FLASH_ERROR, RECEIPT_NOT_FOUND);
            redirect(request, response, "");
            return;
        }
        moveFlash(request);
        HttpSession session = request.getSession();
        request.setAttribute("changeDue", session.getAttribute(FLASH_CHANGE));
        session.removeAttribute(FLASH_CHANGE);
        request.setAttribute("receipt", receipt);
        forward(request, response, RECEIPT_VIEW);
    }

    private CounterSaleForm readForm(HttpServletRequest request) {
        CounterSaleForm form = new CounterSaleForm();
        form.setShowtimeId(parseInt(request.getParameter("showtimeId")));
        String[] seats = request.getParameterValues("seatIds");
        form.setSeatIds(seats == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(seats)));
        Map<Integer, String> quantities = new LinkedHashMap<>();
        for (String name : request.getParameterMap().keySet()) {
            if (name.startsWith(QTY_PREFIX)) {
                int concessionId = parseInt(name.substring(QTY_PREFIX.length()));
                if (concessionId > 0) {
                    quantities.put(concessionId, trim(request.getParameter(name)));
                }
            }
        }
        form.setQuantities(quantities);
        form.setCustomer(trim(request.getParameter("customer")));
        // Voucher gõ tay được ưu tiên hơn voucher chọn trong ví của khách
        String typed = trim(request.getParameter("voucherCodeTyped"));
        form.setVoucherCode(typed.isEmpty() ? trim(request.getParameter("voucherCode")) : typed);
        form.setPoints(trim(request.getParameter("points")));
        form.setPaymentMethod(trim(request.getParameter("paymentMethod")));
        form.setCashReceived(trim(request.getParameter("cashReceived")));
        form.setAgeConfirmed("1".equals(request.getParameter("ageConfirmed")));
        return form;
    }

    @FunctionalInterface
    private interface ShiftAction {
        void run() throws SQLException, ValidationException;
    }

    private void runAndFlash(HttpServletRequest request, ShiftAction action, String successMessage)
            throws SQLException {
        try {
            action.run();
            flash(request, FLASH_SUCCESS, successMessage);
        } catch (ValidationException e) {
            flash(request, FLASH_ERROR, e.getErrors().values().iterator().next());
        }
    }

    // AuthFilter cho cả Manager vào /staff/*, nhưng bán vé tại quầy là việc của nhân viên (SRS: Primary Actor Staff)
    private boolean isStaff(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        if (user != null && Role.STAFF.equals(user.getRole())) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, STAFF_ONLY);
        return false;
    }

    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        return user.getUserId();
    }

    private void moveFlash(HttpServletRequest request) {
        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "counter");
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String query) throws IOException {
        response.sendRedirect(request.getContextPath() + "/staff/counter" + query);
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        request.getRequestDispatcher(view).forward(request, response);
    }

    // Ngày không hợp lệ hoặc đã qua thì lấy hôm nay (không bán suất của ngày đã qua)
    private LocalDate parseDate(String value) {
        LocalDate today = LocalDate.now();
        try {
            LocalDate date = value == null || value.isBlank() ? today : LocalDate.parse(value.trim());
            return date.isBefore(today) ? today : date;
        } catch (DateTimeParseException e) {
            return today;
        }
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu bán vé tại quầy", e);
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
