package com.ibkr.trading.ui.panel;

import apidemo.util.HtmlButton;
import apidemo.util.VerticalPanel;
import com.ibkr.trading.config.ConnectionConfig;
import com.ibkr.trading.service.ConnectionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panel for managing IB TWS/Gateway connection.
 */
public class ConnectionPanel extends JPanel {
    private final ConnectionService connectionService;
    private final JTextField hostField;
    private final JTextField portField;
    private final JTextField clientIdField;
    private final JTextField connectOptions;
    private final JLabel statusLabel;

    public ConnectionPanel(ConnectionService connectionService, ConnectionConfig config) {
        this.connectionService = connectionService;
        this.hostField = new JTextField(config.getHost(), 15);
        this.portField = new JTextField(String.valueOf(config.getPort()), 8);
        this.clientIdField = new JTextField(String.valueOf(config.getClientId()), 8);
        this.connectOptions = new JTextField(String.valueOf(config.getConnectOptions()), 20);
        this.statusLabel = new JLabel("Disconnected");
        
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        VerticalPanel inputPanel = new VerticalPanel();
        inputPanel.add("Host", hostField);
        inputPanel.add("Port", portField);
        inputPanel.add("Client ID", clientIdField);
        inputPanel.add("Connect options", connectOptions);
        
        JLabel infoLabel = new JLabel(
            "<html><b>Paper Trading Ports:</b> TWS: 7497, Gateway: 4002<br>" +
            "<b>Live Trading Ports:</b> TWS: 7496, Gateway: 4001</html>"
        );
        inputPanel.add("", infoLabel);

        VerticalPanel buttonPanel = new VerticalPanel();
        buttonPanel.add(createConnectButton());
        buttonPanel.add(createDisconnectButton());

        VerticalPanel statusPanel = new VerticalPanel();
        statusPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        statusPanel.add("Status: ", statusLabel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(inputPanel, BorderLayout.WEST);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.NORTH);
    }

    private HtmlButton createConnectButton() {
        return new HtmlButton("Connect") {
            @Override
            protected void actionPerformed() {
                String host = hostField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                int clientId = Integer.parseInt(clientIdField.getText().trim());
                String options = connectOptions.getText().trim();
                connectionService.connect(host, port, clientId, options);
                statusLabel.setText("Connecting...");
            }
        };
    }

    private HtmlButton createDisconnectButton() {
        return new HtmlButton("Disconnect") {
            @Override
            protected void actionPerformed() {
                connectionService.disconnect();
            }
        };
    }

    public void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }
}
