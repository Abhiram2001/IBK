package com.ibkr.trading.config;

/**
 * Configuration for IB TWS/Gateway connection parameters.
 * Encapsulates all connection-related settings with sensible defaults.
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
        return new Builder().build();
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
        private String host = "127.0.0.1";
        private int port = 7497; // Paper trading port
        private int clientId = 0;
        private String connectOptions = "+PACEAPI";

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
