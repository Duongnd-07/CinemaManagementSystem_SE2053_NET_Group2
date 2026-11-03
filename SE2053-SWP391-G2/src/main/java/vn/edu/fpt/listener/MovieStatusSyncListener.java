package vn.edu.fpt.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.MovieService;

// JOB-03 (UC-23): mỗi ngày lúc 00:00 chuyển phim Sắp chiếu tới ngày khởi chiếu sang Đang chiếu,
// và phim Đang chiếu đã qua ngày kết thúc chiếu sang Đã kết thúc.
// Chạy thêm một lần ngay sau khi khởi động để bù các ngày server tắt.
// Idempotent: chỉ tác động lên phim đang lệch trạng thái so với ngày; lỗi thì thử lại ở lần chạy kế tiếp.
@WebListener
public class MovieStatusSyncListener implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(MovieStatusSyncListener.class.getName());
    private static final long STARTUP_DELAY_MINUTES = 1;
    private static final long INTERVAL_MINUTES = TimeUnit.DAYS.toMinutes(1);

    private final MovieService movieService = new MovieService();
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "job-03-movie-status-sync");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.schedule(this::runOnce, STARTUP_DELAY_MINUTES, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::runOnce, minutesUntilMidnight(), INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    // Làm tròn lên để lần chạy rơi vào ngay sau 00:00 (khi LocalDate.now() đã sang ngày mới)
    private long minutesUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight).toMinutes() + 1;
    }

    private void runOnce() {
        // Bắt mọi lỗi: exception thoát ra ngoài sẽ làm ScheduledExecutorService ngừng lịch chạy.
        // Mở chiếu trước rồi mới kết thúc, để phim vừa khởi chiếu nhưng đã qua ngày kết thúc cũng về đúng Ended
        try {
            log("Hệ thống tự chuyển phim sang Đang chiếu", movieService.promoteReleasedMovies());
            log("Hệ thống tự chuyển phim sang Đã kết thúc", movieService.endExpiredMovies());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "JOB-03: lỗi khi đồng bộ trạng thái phim, sẽ thử lại ở lần chạy sau", e);
        }
    }

    private void log(String action, List<String> titles) {
        if (titles.isEmpty()) {
            return;
        }
        String detail = String.join(", ", titles);
        LOGGER.log(Level.INFO, "JOB-03: {0}: {1}", new Object[]{action, detail});
        // UC-32: không có request nên người thao tác để trống (= hệ thống)
        ActivityLogger.info(null, LogEventType.CATALOG, action, "Movie", null, detail);
    }
}
