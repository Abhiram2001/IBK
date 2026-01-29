package com.ibkr.trading.app;

import com.ibkr.trading.config.ConnectionConfig;
import com.ibkr.trading.service.ConnectionService;
import com.ibkr.trading.ui.MainFrame;
import com.ib.controller.ApiController.IConnectionHandler;

import javax.swing.SwingUtilities;
import java.util.List;

/**
 * Main application entry point for Interactive Brokers Trading System.
 * Coordinates the initialization and startup of all trading system components.
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class TradingApplication implements IConnectionHandler {
    private static TradingApplication instance;
    
    private final ConnectionService connectionService;
    private final MainFrame mainFrame;
    private final ConnectionConfig config;

    private TradingApplication() {
        this.config = ConnectionConfig.getDefault();
        this.mainFrame = new MainFrame(null, config);
        this.connectionService = new ConnectionService(this, config, mainFrame::showMessage);
        this.mainFrame.setConnectionService(connectionService);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            instance = new TradingApplication();
            instance.start();
        });
    }

    public static TradingApplication getInstance() {
        return instance;
    }

    private void start() {
        mainFrame.show();
        connectionService.autoConnect();
    }

    public ConnectionService getConnectionService() {
        return connectionService;
    }

    @Override
    public void connected() {
        mainFrame.onConnected();
        connectionService.onConnected();
    }

    @Override
    public void disconnected() {
        mainFrame.onDisconnected();
    }

    @Override
    public void accountList(List<String> accounts) {
        connectionService.setAccounts(accounts);
        mainFrame.showMessage("Received " + accounts.size() + " account(s): " + String.join(", ", accounts));
        mainFrame.updateAccountsList();
    }

    @Override
    public void error(Exception e) {
        mainFrame.showError(e.getMessage());
    }

    @Override
    public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
        StringBuilder msg = new StringBuilder();
        msg.append("ID: ").append(id).append(" Code: ").append(errorCode).append(" - ").append(errorMsg);
        if (advancedOrderRejectJson != null) {
            msg.append(" | ").append(advancedOrderRejectJson);
        }
        mainFrame.showMessage(msg.toString());
    }

    @Override
    public void show(String message) {
        mainFrame.showMessage(message);
    }
}
