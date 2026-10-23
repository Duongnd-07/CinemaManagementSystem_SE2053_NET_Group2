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
import vn.edu.fpt.service.ShiftService;

// UC-22: mỗi phút kết các ca bán tại quầy đã quá SHIFT_HOURS giờ và chốt số liệu (nhân viên quên kết ca)
@WebListener
public class ShiftAutoCloseListener implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(ShiftAutoCloseListener.class.getName());
    private static final long INTERVAL_MINUTES = 1;
    private final ShiftService shiftService = new ShiftService();
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "uc22-shift-auto-close");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::runOnce, 1, INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void runOnce() {
        try {
            int closed = shiftService.closeExpired();
            if (closed > 0) {
                LOGGER.log(Level.INFO, "UC-22: đã tự kết {0} ca quá giờ", closed);
            }
        } catch (SQLException | RuntimeException e) {
            // Bắt cả RuntimeException để một lần lỗi không làm dừng hẳn lịch chạy
            LOGGER.log(Level.WARNING, "UC-22: lỗi tự kết ca, sẽ thử lại lần chạy sau", e);
        }
    }
}
