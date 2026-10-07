package vn.edu.fpt.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBContext {
    private static String url;
    private static String user;
    private static String password;

    static {
        try (InputStream input = DBContext.class.getClassLoader().getResourceAsStream("ConnectDB.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.out.println("Sorry, unable to find ConnectDB.properties");
            } else {
                prop.load(input);
                url = prop.getProperty("db.url");
                user = prop.getProperty("db.user");
                password = prop.getProperty("db.password");
                // Load SQL Server JDBC Driver
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
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
        }
    }
}
