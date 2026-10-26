package vn.edu.fpt.service;

import java.sql.SQLException;
import vn.edu.fpt.dao.CustomerNotificationDAO;
import vn.edu.fpt.model.CustomerNotification;
import vn.edu.fpt.util.Page;

public class CustomerNotificationService {
    private static final int PAGE_SIZE = 15;
    private final CustomerNotificationDAO notificationDAO = new CustomerNotificationDAO();

    public Page<CustomerNotification> findForUser(int userId, int page) throws SQLException {
        return Page.of(page, PAGE_SIZE, notificationDAO.countForUser(userId),
                (offset, limit) -> notificationDAO.findForUser(userId, offset, limit));
    }

    public int markAllRead(int userId) throws SQLException {
        return notificationDAO.markAllRead(userId);
    }
}
