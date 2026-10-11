package vn.edu.fpt.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

// Đọc config.properties một lần khi nạp lớp
public final class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                PROPS.load(input);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Không đọc được config.properties, dùng giá trị mặc định", e);
        }
    }

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue).trim();
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
