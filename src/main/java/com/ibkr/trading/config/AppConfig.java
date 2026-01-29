package com.ibkr.trading.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application-wide configuration loader.
 * Reads from application.properties and allows environment variable overrides.
 */
public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();
    private final Properties properties;
    private String currentTradingSymbol = "";

    private AppConfig() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("Failed to load application.properties: " + e.getMessage());
        }
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Get property with environment variable override support.
     * Checks environment variable first, then falls back to properties file.
     */
    public String getProperty(String key, String defaultValue) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        return properties.getProperty(key, defaultValue);
    }

    public String getProperty(String key) {
        return getProperty(key, null);
    }

    public int getIntProperty(String key, int defaultValue) {
        try {
            String value = getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Connection settings
    public String getConnectionHost() {
        return getProperty("connection.host", "127.0.0.1");
    }

    public int getConnectionPort() {
        return getIntProperty("connection.port", 7497);
    }

    public int getConnectionClientId() {
        return getIntProperty("connection.clientId", 0);
    }

    public String getConnectionOptions() {
        return getProperty("connection.connectOptions", "+PACEAPI");
    }

    // Trading symbol management - set by ConnectionPanel
    public void setCurrentTradingSymbol(String symbol) {
        this.currentTradingSymbol = symbol != null ? symbol.trim().toUpperCase() : "";
    }

    public String getCurrentTradingSymbol() {
        return currentTradingSymbol;
    }

    public boolean hasTradingSymbol() {
        return currentTradingSymbol != null && !currentTradingSymbol.isEmpty();
    }
}
