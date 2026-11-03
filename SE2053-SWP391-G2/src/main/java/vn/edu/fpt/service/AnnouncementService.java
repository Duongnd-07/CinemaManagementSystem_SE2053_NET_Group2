package vn.edu.fpt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import vn.edu.fpt.dao.AnnouncementDAO;
import vn.edu.fpt.dao.UserDAO;
import vn.edu.fpt.model.Announcement;
import vn.edu.fpt.model.NotificationCategory;
import vn.edu.fpt.model.NotificationTarget;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

public class AnnouncementService {
    private static final int PAGE_SIZE = 10;
    private final AnnouncementDAO announcementDAO = new AnnouncementDAO();
    private final UserDAO userDAO = new UserDAO();

    public AnnouncementPreview preview(AnnouncementForm form, User sender) throws ValidationException, SQLException {
        requireManager(sender);
        User customer = validate(form);
        int count = announcementDAO.countRecipients(NotificationTarget.recipientRoles(form.getTarget()),
                customer == null ? null : customer.getUserId(), sender.getUserId());
        if (count == 0) throw error(Messages.ANNOUNCEMENT_NO_RECIPIENTS);
        return new AnnouncementPreview(count, customer == null ? null : customer.getFullName(),
                customer == null ? null : maskEmail(customer.getEmail()),
                NotificationTarget.labelOf(form.getTarget()), NotificationCategory.labelOf(form.getType()));
    }

    // The controller also needs the committed ID and count for ActivityLogger.
    public Announcement sendAnnouncement(AnnouncementForm form, User sender) throws ValidationException, SQLException {
        requireManager(sender);
        User customer = validate(form);
        Announcement announcement = new Announcement();
        announcement.setSenderId(sender.getUserId());
        announcement.setTitle(form.getTitle());
        announcement.setMessage(form.getMessage());
        announcement.setTargetType(form.getTarget());
        announcement.setNotificationType(form.getType());
        announcement.setTargetUserId(customer == null ? null : customer.getUserId());
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (announcementDAO.existsRecentDuplicate(conn, announcement)) {
                    throw error(Messages.ANNOUNCEMENT_DUPLICATE);
                }
                int id = announcementDAO.insert(conn, announcement);
                int count = announcementDAO.fanOut(conn, id, announcement,
                        NotificationTarget.recipientRoles(form.getTarget()), announcement.getTargetUserId(), sender.getUserId());
                if (count == 0) throw error(Messages.ANNOUNCEMENT_NO_RECIPIENTS);
                announcementDAO.updateRecipientCount(conn, id, count);
                conn.commit();
                announcement.setAnnouncementId(id);
                announcement.setRecipientCount(count);
                announcement.setStatus("Sent");
                return announcement;
            } catch (SQLException | ValidationException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    public Announcement recallAnnouncement(int announcementId, User manager) throws ValidationException, SQLException {
        requireManager(manager);
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!announcementDAO.markRecalled(conn, announcementId, manager.getUserId())) {
                    throw error(Messages.ANNOUNCEMENT_NOT_FOUND);
                }
                announcementDAO.deleteRecipients(conn, announcementId);
                Announcement announcement = announcementDAO.findById(conn, announcementId);
                conn.commit();
                return announcement;
            } catch (SQLException | ValidationException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    public Page<Announcement> search(String target, String type, String keyword, int page) throws SQLException {
        String normalizedTarget = NotificationTarget.isValid(target) ? target : "";
        String normalizedType = type != null && NotificationCategory.VALUES.contains(type) ? type : "";
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return Page.of(page, PAGE_SIZE, announcementDAO.count(normalizedTarget, normalizedType, normalizedKeyword),
                (offset, limit) -> announcementDAO.search(normalizedTarget, normalizedType, normalizedKeyword, offset, limit));
    }

    private User validate(AnnouncementForm form) throws ValidationException, SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!NotificationTarget.isValid(form.getTarget())) errors.put("target", Messages.ANNOUNCEMENT_TARGET_INVALID);
        if (!NotificationCategory.isAllowed(form.getType(), form.getTarget())) errors.put("type", Messages.ANNOUNCEMENT_TYPE_INVALID);
        if (form.getTitle().isEmpty() || form.getTitle().length() > 150) errors.put("title", Messages.ANNOUNCEMENT_TITLE_INVALID);
        if (form.getMessage().isEmpty() || form.getMessage().length() > 1000) errors.put("message", Messages.ANNOUNCEMENT_MESSAGE_INVALID);
        User customer = null;
        if (NotificationTarget.SINGLE_CUSTOMER.equals(form.getTarget())) {
            if (form.getPhone().isEmpty()) errors.put("phone", Messages.ANNOUNCEMENT_PHONE_REQUIRED);
            else if (!form.getPhone().matches("^0[0-9]{9}$")) errors.put("phone", Messages.ANNOUNCEMENT_PHONE_INVALID);
            else {
                customer = userDAO.findByPhone(form.getPhone());
                if (customer == null) errors.put("phone", Messages.ANNOUNCEMENT_PHONE_NOT_FOUND);
                else if (!Role.CUSTOMER.equals(customer.getRole())) errors.put("phone", Messages.ANNOUNCEMENT_PHONE_NOT_CUSTOMER);
                else if (!"Active".equals(customer.getStatus())) errors.put("phone", Messages.ANNOUNCEMENT_CUSTOMER_INACTIVE);
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return customer;
    }

    private void requireManager(User manager) throws ValidationException {
        if (manager == null || manager.getUserId() <= 0 || !Role.MANAGER.equals(manager.getRole())
                || !"Active".equals(manager.getStatus())) throw error(Messages.ANNOUNCEMENT_MANAGER_REQUIRED);
    }

    private ValidationException error(String message) {
        return new ValidationException(Map.of("general", message));
    }

    private String maskEmail(String email) {
        if (email == null) return "***";
        int at = email.indexOf('@');
        return at <= 0 ? "***" : email.substring(0, 1) + "***" + email.substring(at);
    }

    private void rollback(Connection conn, Exception cause) {
        try {
            conn.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }
}
