package vn.edu.fpt.controller.customer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.BookingCheckout;

// UC-13: hằng số và tiện ích dùng chung cho các servlet của luồng đặt vé online
final class BookingFlow {
    static final String SESSION_CHECKOUT = "bookingCheckout";
    static final String SESSION_CHECKOUT_ERRORS = "bookingCheckoutErrors";
    static final String SESSION_VOUCHER_INPUT = "bookingVoucherInput";
    static final String FLASH_SUCCESS = "flashSuccess";
    static final String FLASH_ERROR = "flashError";
    static final String SEAT_PAGE = "/booking";
    static final String HOLD_PATH = "/customer/booking/hold";
    static final String CHECKOUT_PATH = "/customer/booking/checkout";
    static final String PAYMENT_PATH = "/customer/booking/payment";

    private BookingFlow() {
    }

    static User currentUser(HttpSession session) {
        return session == null ? null : (User) session.getAttribute(AuthFilter.SESSION_USER);
    }

    static BookingCheckout currentCheckout(HttpSession session) {
        return session == null ? null : (BookingCheckout) session.getAttribute(SESSION_CHECKOUT);
    }

    static String seatPageUrl(HttpServletRequest request, int showtimeId) {
        return request.getContextPath() + SEAT_PAGE + "?showtimeId=" + showtimeId;
    }

    static String paymentUrl(HttpServletRequest request, int bookingId) {
        return request.getContextPath() + PAYMENT_PATH + "?id=" + bookingId;
    }

    static void moveFlashToRequest(HttpSession session, HttpServletRequest request) {
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    static int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
