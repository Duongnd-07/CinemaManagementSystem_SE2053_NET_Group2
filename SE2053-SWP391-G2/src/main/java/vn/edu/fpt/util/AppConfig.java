package vn.edu.fpt.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final Properties PROPS = new Properties();
    private static final String CONFIG_PATH_PROPERTY = "app.config";
    private static final String CONFIG_PATH_ENV = "APP_CONFIG_FILE";

    static {
        loadClasspathDefaults();
        try {
            loadExternalConfig();
            loadSystemProperties();
            loadEnvironment();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
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

    // Nạp template rồi đến config.properties dùng chung của nhóm (đóng trong WAR); file ngoài/biến môi trường ghi đè sau
    private static void loadClasspathDefaults() {
        loadClasspathResource("config.properties.example");
        loadClasspathResource("config.properties");
    }

    private static void loadClasspathResource(String name) {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(name)) {
            if (input != null) {
                PROPS.load(input);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Không đọc được " + name + ", dùng giá trị mặc định.", e);
        }
    }

    private static void loadExternalConfig() throws IOException {
        String configuredPath = System.getProperty(CONFIG_PATH_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv(CONFIG_PATH_ENV);
        }
        if (configuredPath == null || configuredPath.isBlank()) {
            String catalinaBase = System.getProperty("catalina.base");
            Path tomcatConfig = catalinaBase == null || catalinaBase.isBlank() ? null
                    : Path.of(catalinaBase, "conf", "cineluxe", "config.properties");
            if (tomcatConfig != null && Files.isRegularFile(tomcatConfig)) {
                configuredPath = tomcatConfig.toString();
            } else {
                Path localConfig = Path.of("config.properties").toAbsolutePath();
                if (Files.isRegularFile(localConfig)) {
                    configuredPath = localConfig.toString();
                }
            }
        }
        if (configuredPath == null || configuredPath.isBlank()) {
            return;
        }

        Path configPath = Path.of(configuredPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(configPath)) {
            throw new IOException("Configured application settings file does not exist: " + configPath);
        }
        try (InputStream input = Files.newInputStream(configPath)) {
            PROPS.load(input);
        }
    }

    private static void loadSystemProperties() {
        for (String key : PROPS.stringPropertyNames()) {
            String value = System.getProperty(key);
            if (value != null) {
                PROPS.setProperty(key, value);
            }
        }
    }

    private static void loadEnvironment() {
        for (String key : PROPS.stringPropertyNames()) {
            String environmentKey = "APP_" + key.toUpperCase().replace('.', '_').replace('-', '_');
            String value = System.getenv(environmentKey);
            if (value != null) {
                PROPS.setProperty(key, value);
            }
        }
    }
}
