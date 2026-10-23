package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.BookingCheckout;
import vn.edu.fpt.service.BookingService;
import vn.edu.fpt.service.CheckoutForm;
import vn.edu.fpt.service.CheckoutView;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-13 bước 6-9: chọn bắp nước, voucher, điểm trong thời gian giữ ghế rồi xác nhận đơn (tạo đơn Pending).
// Trang tự tính lại tiền: mỗi lần khách đổi lựa chọn, booking-checkout.js gửi ngầm action=update và nhận lại
// trang đã tính (không redirect để chỉ tốn một request). AF2: hủy đơn khi đang giữ ghế. E1: hết giờ -> về sơ đồ ghế.
@WebServlet(BookingFlow.CHECKOUT_PATH)
public class BookingCheckoutServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(BookingCheckoutServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/booking-checkout.jsp";
    private static final String QUANTITY_PREFIX = "qty_";
    private static final String ACTION_CONFIRM = "confirm";
    private static final String ACTION_CANCEL = "cancel";
    // Header do booking-checkout.js gửi kèm request tự tính lại tiền
    private static final String AJAX_HEADER = "X-Requested-With";

    private final BookingService bookingService = new BookingService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        BookingCheckout checkout = BookingFlow.currentCheckout(session);
        if (checkout == null) {
            response.sendRedirect(request.getContextPath() + "/movies");
            return;
        }
        if (checkout.getBookingId() != null) {
            response.sendRedirect(BookingFlow.paymentUrl(request, checkout.getBookingId()));
            return;
        }
        BookingFlow.moveFlashToRequest(session, request);
        try {
            CheckoutView view = bookingService.loadCheckout(BookingFlow.currentUser(session), checkout);
            @SuppressWarnings("unchecked")
            Map<String, String> flashErrors = (Map<String, String>) session.getAttribute(
                    BookingFlow.SESSION_CHECKOUT_ERRORS);
            session.removeAttribute(BookingFlow.SESSION_CHECKOUT_ERRORS);
            Object voucherInput = session.getAttribute(BookingFlow.SESSION_VOUCHER_INPUT);
            session.removeAttribute(BookingFlow.SESSION_VOUCHER_INPUT);
            render(request, response, session, checkout, view, flashErrors,
                    voucherInput != null ? voucherInput.toString() : null);
        } catch (ValidationException e) {
            endCheckout(request, response, session, checkout, e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải trang xác nhận đặt vé", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        HttpSession session = request.getSession();
        BookingCheckout checkout = BookingFlow.currentCheckout(session);
        if (checkout == null) {
            session.setAttribute(BookingFlow.FLASH_ERROR, Messages.BOOKING_HOLD_EXPIRED);
            response.sendRedirect(request.getContextPath() + "/movies");
            return;
        }
        String action = request.getParameter("action");
        CheckoutForm form = readForm(request);
        try {
            if (ACTION_CANCEL.equals(action)) {
                bookingService.releaseCheckout(checkout);
                session.removeAttribute(BookingFlow.SESSION_CHECKOUT);
                session.setAttribute(BookingFlow.FLASH_SUCCESS, Messages.BOOKING_CANCELLED);
                response.sendRedirect(BookingFlow.seatPageUrl(request, checkout.getShowtimeId()));
                return;
            }
            if (ACTION_CONFIRM.equals(action)) {
                int bookingId;
                // Bấm xác nhận hai lần liên tiếp: lần sau thấy đơn đã tạo thì chỉ chuyển sang trang thanh toán
                synchronized (checkout) {
                    // UC-32: chỉ ghi log lần tạo đơn, không ghi khi bấm xác nhận lại
                    boolean created = checkout.getBookingId() == null;
                    bookingId = checkout.getBookingId() != null ? checkout.getBookingId()
                            : bookingService.confirm(BookingFlow.currentUser(session), checkout, form);
                    if (created) {
                        ActivityLogger.info(request, LogEventType.BOOKING, "Tạo đơn đặt vé online", "Booking",
                                bookingId, "Suất chiếu #" + checkout.getShowtimeId() + ", "
                                        + checkout.getSeatIds().size() + " ghế");
                    }
                }
                response.sendRedirect(BookingFlow.paymentUrl(request, bookingId));
                return;
            }
            CheckoutView view = bookingService.applyOptions(BookingFlow.currentUser(session), checkout, form);
            if (request.getHeader(AJAX_HEADER) != null) {
                render(request, response, session, checkout, view, null, voucherInput(view.getErrors(), form));
                return;
            }
            // Trình duyệt tắt JavaScript bấm nút Cập nhật: PRG như form thường
            keepErrors(session, view.getErrors(), form);
            response.sendRedirect(request.getContextPath() + BookingFlow.CHECKOUT_PATH);
        } catch (ValidationException e) {
            if (e.getErrors().containsKey(BookingService.ERROR_ENDED)) {
                endCheckout(request, response, session, checkout, e);
                return;
            }
            keepErrors(session, e.getErrors(), form);
            response.sendRedirect(request.getContextPath() + BookingFlow.CHECKOUT_PATH);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi xử lý đơn đặt vé", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                        BookingCheckout checkout, CheckoutView view, Map<String, String> extraErrors,
                        String voucherInput) throws ServletException, IOException {
        Map<String, String> errors = new LinkedHashMap<>(view.getErrors());
        if (extraErrors != null) {
            errors.putAll(extraErrors);
        }
        request.setAttribute("checkout", view);
        request.setAttribute("errors", errors);
        request.setAttribute("voucherInput", voucherInput != null ? voucherInput : view.getQuote().getVoucherCode());
        request.setAttribute("maxConcessionQuantity", BookingService.MAX_CONCESSION_QUANTITY);
        request.setAttribute("seatPageUrl", BookingFlow.seatPageUrl(request, checkout.getShowtimeId()));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // E1 / suất chiếu hết mở bán: nhả ghế còn giữ (nếu có), kết thúc phiên và quay lại sơ đồ ghế
    private void endCheckout(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                             BookingCheckout checkout, ValidationException e) throws IOException {
        try {
            bookingService.releaseCheckout(checkout);
        } catch (SQLException ex) {
            // Ghế vẫn tự hết hạn và được JOB-01 dọn, không chặn khách quay lại chọn ghế
            LOGGER.log(Level.WARNING, "Không nhả được ghế của phiên đặt vé đã kết thúc", ex);
        }
        session.removeAttribute(BookingFlow.SESSION_CHECKOUT);
        session.setAttribute(BookingFlow.FLASH_ERROR, e.getErrors().values().iterator().next());
        response.sendRedirect(BookingFlow.seatPageUrl(request, checkout.getShowtimeId()));
    }

    private void keepErrors(HttpSession session, Map<String, String> errors, CheckoutForm form) {
        if (errors.isEmpty()) {
            session.setAttribute(BookingFlow.FLASH_SUCCESS, Messages.BOOKING_UPDATED);
            return;
        }
        session.setAttribute(BookingFlow.SESSION_CHECKOUT_ERRORS, new LinkedHashMap<>(errors));
        String input = voucherInput(errors, form);
        if (input != null) {
            session.setAttribute(BookingFlow.SESSION_VOUCHER_INPUT, input);
        }
    }

    // E3: giữ lại mã khách vừa nhập để khách thấy mã nào bị từ chối
    private String voucherInput(Map<String, String> errors, CheckoutForm form) {
        return errors.containsKey(BookingService.ERROR_VOUCHER) ? form.getVoucherCode() : null;
    }

    private CheckoutForm readForm(HttpServletRequest request) {
        CheckoutForm form = new CheckoutForm();
        Map<Integer, String> quantities = new LinkedHashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith(QUANTITY_PREFIX)) {
                int concessionId = BookingFlow.parseInt(name.substring(QUANTITY_PREFIX.length()));
                if (concessionId > 0) {
                    quantities.put(concessionId, request.getParameter(name));
                }
            }
        }
        form.setQuantities(quantities);
        form.setVoucherCode(request.getParameter("voucherCode"));
        form.setPoints(request.getParameter("points"));
        return form;
    }
}
