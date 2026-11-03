package vn.edu.fpt.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBContext {
    private static HikariDataSource dataSource;

    static {
        try (InputStream input = DBContext.class.getClassLoader().getResourceAsStream("ConnectDB.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.out.println("Sorry, unable to find ConnectDB.properties");
            } else {
                prop.load(input);
                HikariConfig config = new HikariConfig();
                config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                config.setJdbcUrl(prop.getProperty("db.url"));
                config.setUsername(prop.getProperty("db.user"));
                config.setPassword(prop.getProperty("db.password"));
                config.setPoolName("CinemaDBPool");
                config.setMaximumPoolSize(Integer.parseInt(prop.getProperty("db.pool.maxSize", "10")));
                config.setMinimumIdle(Integer.parseInt(prop.getProperty("db.pool.minIdle", "2")));
                config.setConnectionTimeout(Long.parseLong(prop.getProperty("db.pool.connectionTimeoutMs", "30000")));
                // Không fail lúc khởi động nếu DB chưa sẵn sàng; lỗi sẽ báo ở lần getConnection() như trước
                config.setInitializationFailTimeout(-1);
                dataSource = new HikariDataSource(config);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Connection pool is not initialized (check ConnectDB.properties)");
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // Test connection
    public static void main(String[] args) {
        try (Connection conn = DBContext.getConnection()) {
            if (conn != null) {
                System.out.println("Connected to the database successfully!");
            }
        } catch (SQLException e) {
            System.err.println("Failed to connect to the database!");
            e.printStackTrace();
        } finally {
            closePool();
        }
    }
}
