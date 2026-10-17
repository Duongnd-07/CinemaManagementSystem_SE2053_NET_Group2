package vn.edu.fpt.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.service.BookingService;

// JOB-01 (UC-13 E1): mỗi phút hủy đơn online Pending đã hết giờ giữ ghế và trả các ghế giữ quá hạn về AVAILABLE.
// Idempotent: chạy lại nhiều lần chỉ tác động lên dữ liệu đã hết hạn; lỗi thì thử lại ở lần chạy kế tiếp.
@WebListener
public class SeatHoldCleanupListener implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(SeatHoldCleanupListener.class.getName());
    private static final long INTERVAL_MINUTES = 1;

    private final BookingService bookingService = new BookingService();
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "job-01-seat-hold-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::runOnce, INTERVAL_MINUTES, INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void runOnce() {
        // Bắt mọi lỗi: exception thoát ra ngoài sẽ làm ScheduledExecutorService ngừng lịch chạy
        try {
            int cancelled = bookingService.cancelAllExpiredBookings();
            int released = bookingService.releaseAllExpiredHolds();
            if (cancelled > 0 || released > 0) {
                LOGGER.log(Level.INFO, "JOB-01: hủy {0} đơn chờ thanh toán quá hạn, nhả {1} ghế giữ quá hạn",
                        new Object[]{cancelled, released});
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "JOB-01: lỗi khi dọn ghế giữ quá hạn, sẽ thử lại ở lần chạy sau", e);
        }
    }
}
