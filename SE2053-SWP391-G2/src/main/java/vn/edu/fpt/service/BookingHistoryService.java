package vn.edu.fpt.service;

import java.sql.SQLException;
import vn.edu.fpt.dao.CustomerBookingDAO;
import vn.edu.fpt.model.BookingHistoryEntry;
import vn.edu.fpt.util.Page;

public class BookingHistoryService {
    private static final int PAGE_SIZE = 10;
    private final CustomerBookingDAO bookingDAO = new CustomerBookingDAO();

    public Page<BookingHistoryEntry> findForCustomer(int customerId, int page) throws SQLException {
        return Page.of(page, PAGE_SIZE, bookingDAO.countHistory(customerId),
                (offset, limit) -> bookingDAO.findHistory(customerId, offset, limit));
    }
}
