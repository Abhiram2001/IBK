package com.ibkr.trading.ui.panel;

import com.ibkr.trading.ui.util.HtmlButton;
import com.ibkr.trading.ui.util.VerticalPanel;
import com.ibkr.trading.config.AppConfig;
import com.ibkr.trading.config.ConnectionConfig;
import com.ibkr.trading.service.ConnectionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel for managing IB TWS/Gateway connection.
 */
public class ConnectionPanel extends JPanel {
    private final transient ConnectionService connectionService;
    private final JTextField hostField;
    private final JTextField portField;
    private final JTextField clientIdField;
    private final JTextField connectOptions;
    private final JLabel statusLabel;
    private final JList<String> accountsList;
    private final DefaultListModel<String> accountsListModel;
    private JButton connectButton;
    private JButton disconnectButton;
    
    // Modern color palette (Material Design inspired)
    private static final Color BACKGROUND_COLOR = new Color(240, 242, 245);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(37, 99, 235); // Blue-600
    private static final Color SUCCESS_COLOR = new Color(34, 197, 94); // Green-500
    private static final Color DANGER_COLOR = new Color(239, 68, 68); // Red-500
    private static final Color TEXT_PRIMARY = new Color(17, 24, 39); // Gray-900
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128); // Gray-500
    private static final Color BORDER_COLOR = new Color(229, 231, 235); // Gray-200
    private static final Color INPUT_FOCUS = new Color(147, 197, 253); // Blue-300

    public ConnectionPanel(ConnectionService connectionService, ConnectionConfig config) {
        this.connectionService = connectionService;
        
        this.hostField = createStyledTextField(config.getHost(), 15);
        this.portField = createStyledTextField(String.valueOf(config.getPort()), 8);
        this.clientIdField = createStyledTextField(String.valueOf(config.getClientId()), 8);
        this.connectOptions = createStyledTextField(String.valueOf(config.getConnectOptions()), 20);
        this.statusLabel = new JLabel("Disconnected");
        this.statusLabel.setForeground(TEXT_SECONDARY);
        
        // Initialize accounts list with modern styling
        this.accountsListModel = new DefaultListModel<>();
        this.accountsList = new JList<>(accountsListModel);
        this.accountsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.accountsList.setVisibleRowCount(5);
        this.accountsList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        this.accountsList.setSelectionBackground(new Color(219, 234, 254)); // Blue-100
        this.accountsList.setSelectionForeground(PRIMARY_COLOR);
        this.accountsList.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        this.accountsList.setFixedCellHeight(36);
        
        initializeUI();
    }
    
    private JTextField createStyledTextField(String text, int columns) {
        JTextField field = new JTextField(text, columns);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Add focus listener for better UX
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                field.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(INPUT_FOCUS, 2, true),
                    BorderFactory.createEmptyBorder(7, 11, 7, 11)
                ));
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                field.setBorder(new CompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        
        return field;
    }

    private void initializeUI() {
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(25, 25, 25, 25));
        setBackground(BACKGROUND_COLOR);

        // Left panel - Connection configuration (Modern Card)
        JPanel inputCard = createCard();
        inputCard.setLayout(new BorderLayout());
        inputCard.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(24, 24, 24, 24)
        ));
        
        JLabel configTitle = new JLabel("Connection Settings");
        configTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        configTitle.setForeground(TEXT_PRIMARY);
        inputCard.add(configTitle, BorderLayout.NORTH);
        
        VerticalPanel inputPanel = new VerticalPanel();
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(18, 0, 0, 0));
        
        inputPanel.add("Host", hostField);
        inputPanel.add("Port", portField);
        inputPanel.add("Client ID", clientIdField);
        inputPanel.add("Options", connectOptions);
        
        JLabel infoLabel = new JLabel(
            "<html><div style='margin-top: 12px; color: #6b7280; font-size: 12px; line-height: 1.5;'>" +
            "<b>Paper Trading:</b> TWS: 7497, Gateway: 4002<br>" +
            "<b>Live Trading:</b> TWS: 7496, Gateway: 4001</div></html>"
        );
        inputPanel.add("", infoLabel);
        
        inputCard.add(inputPanel, BorderLayout.CENTER);

        // Center panel - Buttons (Modern Card)
        JPanel buttonCard = createCard();
        buttonCard.setLayout(new BorderLayout());
        buttonCard.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(24, 24, 24, 24)
        ));
        
        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setOpaque(false);
        buttonContainer.setBorder(new EmptyBorder(50, 0, 0, 0));
        
        connectButton = createModernButton("Connect", PRIMARY_COLOR);
        disconnectButton = createModernButton("Disconnect", DANGER_COLOR);
        
        // Set initial button states
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        disconnectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        buttonContainer.add(connectButton);
        buttonContainer.add(Box.createVerticalStrut(16));
        buttonContainer.add(disconnectButton);
        buttonContainer.add(Box.createVerticalStrut(24));
        
        // Status badge
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusPanel.setOpaque(false);
        JPanel statusBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        statusBadge.setBackground(new Color(243, 244, 246));
        statusBadge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(6, 16, 6, 16)
        ));
        statusBadge.add(new JLabel("Status:"));
        statusBadge.add(statusLabel);
        statusPanel.add(statusBadge);
        buttonContainer.add(statusPanel);
        
        buttonCard.add(buttonContainer, BorderLayout.CENTER);

        // Right panel - Accounts list (Modern Card)
        JPanel accountsCard = createCard();
        accountsCard.setLayout(new BorderLayout(0, 12));
        accountsCard.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(24, 24, 24, 24)
        ));
        accountsCard.setPreferredSize(new Dimension(300, 220));
        
        JLabel accountsTitle = new JLabel("Connected Accounts");
        accountsTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        accountsTitle.setForeground(TEXT_PRIMARY);
        accountsCard.add(accountsTitle, BorderLayout.NORTH);
        
        JScrollPane accountsScroll = new JScrollPane(accountsList);
        accountsScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));
        accountsScroll.setPreferredSize(new Dimension(250, 190));
        accountsScroll.getViewport().setBackground(Color.WHITE);
        
        JLabel accountsInfoLabel = new JLabel(
            "<html><div style='text-align: center; color: #9ca3af; font-size: 12px;'>" +
            "Accounts will appear after connecting</div></html>"
        );
        accountsInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel accountsContentPanel = new JPanel(new BorderLayout(0, 10));
        accountsContentPanel.setOpaque(false);
        accountsContentPanel.add(accountsScroll, BorderLayout.CENTER);
        accountsContentPanel.add(accountsInfoLabel, BorderLayout.SOUTH);
        
        accountsCard.add(accountsContentPanel, BorderLayout.CENTER);

        // Assemble main layout
        JPanel centerPanel = new JPanel(new BorderLayout(20, 20));
        centerPanel.setOpaque(false);
        centerPanel.add(inputCard, BorderLayout.WEST);
        centerPanel.add(buttonCard, BorderLayout.CENTER);
        centerPanel.add(accountsCard, BorderLayout.EAST);

        add(centerPanel, BorderLayout.NORTH);
    }
    
    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(0, 0, 0, 0)
        ));
        return card;
    }
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(150, 42));
        button.setMaximumSize(new Dimension(150, 42));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.darker());
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                }
            }
        });
        
        // Add property change listener to handle disabled state styling
        button.addPropertyChangeListener("enabled", evt -> {
            if (button.isEnabled()) {
                button.setBackground(bgColor);
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
                button.setBackground(new Color(206, 212, 218)); // Muted gray for disabled
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        
        if (text.equals("Connect")) {
            button.addActionListener(e -> {
                String host = hostField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                int clientId = Integer.parseInt(clientIdField.getText().trim());
                String options = connectOptions.getText().trim();
                connectionService.connect(host, port, clientId, options);
                statusLabel.setText("Connecting...");
                statusLabel.setForeground(new Color(255, 193, 7));
            });
        } else {
            button.addActionListener(e -> connectionService.disconnect());
        }
        
        return button;
    }

    public void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            
            boolean isConnected = status.contains("Connected") && !status.contains("Disconnected");
            boolean isConnecting = status.contains("Connecting");
            boolean isDisconnected = status.contains("Disconnected");
            
            if (isConnected) {
                statusLabel.setForeground(SUCCESS_COLOR);
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
            } else if (isConnecting) {
                statusLabel.setForeground(new Color(255, 193, 7));
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
            } else {
                statusLabel.setForeground(TEXT_SECONDARY);
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
            }
        });
    }
    
    /**
     * Updates the displayed list of accounts.
     * Called when accounts are received from TWS.
     */
    public void updateAccountsList() {
        SwingUtilities.invokeLater(() -> {
            accountsListModel.clear();
            List<String> accounts = connectionService.getAccounts();
            
            if (accounts.isEmpty()) {
                accountsListModel.addElement("(No accounts connected)");
                accountsList.setEnabled(false);
                accountsList.setForeground(TEXT_SECONDARY);
            } else {
                accountsList.setEnabled(true);
                accountsList.setForeground(TEXT_PRIMARY);
                for (String account : accounts) {
                    accountsListModel.addElement(account);
                }
                // Auto-select first account
                if (!accounts.isEmpty()) {
                    accountsList.setSelectedIndex(0);
                }
            }
        });
    }
}
