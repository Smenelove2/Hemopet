package br.com.hemopet.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConfig {
    private static final String PROPERTIES_FILE = "/db.properties";
    private static final Properties PROPERTIES = loadProperties();

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        String url = readConfig("HEMOPET_DB_URL", "db.url");
        String user = readConfig("HEMOPET_DB_USER", "db.user");
        String password = readConfig("HEMOPET_DB_PASSWORD", "db.password");
        return DriverManager.getConnection(url, user, password);
    }

    public static boolean canConnect() {
        try (Connection ignored = getConnection()) {
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    private static String readConfig(String envName, String propertyName) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return PROPERTIES.getProperty(propertyName, "");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel carregar db.properties", exception);
        }
        return properties;
    }
}
