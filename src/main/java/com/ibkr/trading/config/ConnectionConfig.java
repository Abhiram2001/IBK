package com.ibkr.trading.config;

/**
 * Configuration for IB TWS/Gateway connection parameters.
 * Encapsulates all connection-related settings with sensible defaults.
 * Values are loaded from application.properties and can be overridden by environment variables.
 */
public class ConnectionConfig {
    private final String host;
    private final int port;
    private final int clientId;
    private final String connectOptions;

    private ConnectionConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.clientId = builder.clientId;
        this.connectOptions = builder.connectOptions;
    }

    public static ConnectionConfig getDefault() {
        AppConfig config = AppConfig.getInstance();
        return new Builder()
                .host(config.getConnectionHost())
                .port(config.getConnectionPort())
                .clientId(config.getConnectionClientId())
                .connectOptions(config.getConnectionOptions())
                .build();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getClientId() {
        return clientId;
    }

    public String getConnectOptions() {
        return connectOptions;
    }

    public static class Builder {
        private String host = AppConfig.getInstance().getConnectionHost();
        private int port = AppConfig.getInstance().getConnectionPort();
        private int clientId = AppConfig.getInstance().getConnectionClientId();
        private String connectOptions = AppConfig.getInstance().getConnectionOptions();

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder clientId(int clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder connectOptions(String connectOptions) {
            this.connectOptions = connectOptions;
            return this;
        }

        public ConnectionConfig build() {
            return new ConnectionConfig(this);
        }
    }
}
