package vn.edu.fpt.service;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.dao.ShowtimeReminderDAO;
import vn.edu.fpt.model.ShowtimeReminder;
import vn.edu.fpt.util.EmailService;

public class ShowtimeReminderService {
    private static final Logger LOGGER = Logger.getLogger(ShowtimeReminderService.class.getName());
    private final ShowtimeReminderDAO reminderDAO = new ShowtimeReminderDAO();

    public int sendDueReminders() throws SQLException {
        reminderDAO.enqueueDueBookings();
        List<ShowtimeReminder> reminders = reminderDAO.sendDueBatch();
        int sent = 0;
        for (ShowtimeReminder reminder : reminders) {
            boolean delivered = EmailService.sendShowtimeReminderSync(reminder);
            if (delivered) {
                reminderDAO.markSent(reminder.getBookingId());
                sent++;
            } else {
                reminderDAO.markFailed(reminder.getBookingId());
                LOGGER.log(Level.WARNING, "JOB-02: gửi email nhắc suất chiếu thất bại cho đơn {0}, sẽ thử lại",
                        reminder.getBookingId());
            }
        }
        return sent;
    }
}
