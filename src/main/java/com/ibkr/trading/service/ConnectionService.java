package com.ibkr.trading.service;

import com.ibkr.trading.config.ConnectionConfig;
import com.ib.controller.ApiConnection;
import com.ib.controller.ApiController;
import com.ib.controller.Formats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service responsible for managing connection to IB TWS/Gateway.
 * Handles connection lifecycle, account management, and API controller.
 */
public class ConnectionService {
    private final ApiController controller;
    private final ConnectionConfig config;
    private final List<String> accounts = new ArrayList<>();
    private final MarketDataService marketDataService;
    private final OrderService orderService;
    private final Consumer<String> messageHandler;
    private boolean connected = false;

    public ConnectionService(ApiController.IConnectionHandler handler, ConnectionConfig config, Consumer<String> messageHandler) {
        this.config = config;
        this.messageHandler = messageHandler;
        this.controller = new ApiController(handler, new SilentLogger(), new SilentLogger());
        this.marketDataService = new MarketDataService(controller);
        this.orderService = new OrderService(controller);
    }

    public void connect(String host, int port, int clientId, String options) {
        controller.connect(host, port, clientId, options);
    }

    public void autoConnect() {
        connect(config.getHost(), config.getPort(), config.getClientId(), config.getConnectOptions());
    }

    public void disconnect() {
        controller.disconnect();
        connected = false;
    }

    public void onConnected() {
        this.connected = true;
        requestServerTime();
        subscribeToBulletins();
    }

    public boolean isConnected() {
        return connected;
    }

    public ApiController getController() {
        return controller;
    }

    public MarketDataService getMarketDataService() {
        return marketDataService;
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public List<String> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public void setAccounts(List<String> newAccounts) {
        accounts.clear();
        accounts.addAll(newAccounts);
    }

    private void requestServerTime() {
        controller.reqCurrentTime(time -> {
            String timeMsg = "Server time: " + Formats.fmtDate(time * 1000);
            if (messageHandler != null) {
                messageHandler.accept(timeMsg);
            }
        });
    }

    private void subscribeToBulletins() {
        controller.reqBulletins(true, (msgId, newsType, message, exchange) -> {
            if (messageHandler != null) {
                String bulletinHeader = "Bulletin [" + newsType + " @ " + exchange + "]: " + 
                    "======================================================";
                messageHandler.accept(bulletinHeader);
                messageHandler.accept(message);
            }
        });
    }

    private static class SilentLogger implements ApiConnection.ILogger {
        @Override
        public void log(String str) {
            // Silent by default, can be enabled for debugging
        }
    }
}
