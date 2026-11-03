package vn.edu.fpt.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.service.ShowtimeReminderService;

@WebListener
public class ShowtimeReminderListener implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(ShowtimeReminderListener.class.getName());
    private static final long INTERVAL_MINUTES = 15;
    private final ShowtimeReminderService reminderService = new ShowtimeReminderService();
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "job-02-showtime-reminder");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::runOnce, 0, INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void runOnce() {
        try {
            int sent = reminderService.sendDueReminders();
            if (sent > 0) {
                LOGGER.log(Level.INFO, "JOB-02: đã gửi {0} email nhắc suất chiếu", sent);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "JOB-02: lỗi xử lý email nhắc suất chiếu, sẽ thử lại lần chạy sau", e);
        }
    }
}
