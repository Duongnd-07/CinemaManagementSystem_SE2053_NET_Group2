package vn.edu.fpt.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import vn.edu.fpt.util.DBContext;

// Đóng HikariCP pool khi undeploy/redeploy để không rò rỉ connection và thread của pool
@WebListener
public class DBPoolShutdownListener implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        DBContext.closePool();
    }
}
