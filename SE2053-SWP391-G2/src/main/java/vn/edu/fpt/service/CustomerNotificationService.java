package vn.edu.fpt.service;

import java.sql.SQLException;
import java.util.List;
import vn.edu.fpt.dao.CustomerNotificationDAO;
import vn.edu.fpt.model.CustomerNotification;
import vn.edu.fpt.util.Page;

public class CustomerNotificationService {
    private static final int PAGE_SIZE = 15;
    private final CustomerNotificationDAO notificationDAO = new CustomerNotificationDAO();

    public int countUnread(int userId) throws SQLException {
        return notificationDAO.countUnread(userId);
    }

    public Page<CustomerNotification> findForUser(int userId, int page) throws SQLException {
        return Page.of(page, PAGE_SIZE, notificationDAO.countForUser(userId),
                (offset, limit) -> notificationDAO.findForUser(userId, offset, limit));
    }

    // Mở hộp thư = đã xem các thông báo trên trang đang hiển thị. Danh sách trả về giữ trạng thái
    // trước khi đánh dấu để giao diện vẫn làm nổi bật thông báo mới trong lần xem này.
    public Page<CustomerNotification> openForUser(int userId, int page) throws SQLException {
        Page<CustomerNotification> notifications = findForUser(userId, page);
        List<Integer> unreadIds = notifications.getItems().stream()
                .filter(notification -> !notification.isRead())
                .map(CustomerNotification::getNotificationId)
                .toList();
        notificationDAO.markRead(userId, unreadIds);
        return notifications;
    }

    public int markAllRead(int userId) throws SQLException {
        return notificationDAO.markAllRead(userId);
    }
}
