package com.ibkr.trading.ui;

import com.ibkr.trading.config.AppConfig;
import com.ibkr.trading.config.ConnectionConfig;
import com.ibkr.trading.service.ConnectionService;
import com.ibkr.trading.ui.panel.ConnectionPanel;
import com.ibkr.trading.ui.panel.CalendarSpreadPanel;
import com.ibkr.trading.ui.panel.PreMarketCloseOrderPanel;
import com.ibkr.trading.ui.panel.StranglePanel;
import com.ibkr.trading.ui.panel.MultiStockPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Main application window with navigation bar for account selection and symbol entry.
 * Features a modern UI with left-side account selector and right-side symbol input.
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class MainFrame {
    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | 
                 IllegalAccessException | UnsupportedLookAndFeelException e) {
            // Fall back to default
        }
    }

    private final JFrame frame;
    private final JTextArea messageArea;
    private final JTabbedPane tabbedPanel;
    private final JComboBox<String> accountSelector;
    private final JTextField symbolField;
    private final JLabel accountLabel;
    private final JLabel symbolLabel;
    private ConnectionService connectionService;
    
    private ConnectionPanel connectionPanel;
    private CalendarSpreadPanel calendarSpreadPanel;
    private StranglePanel stranglePanel;
    private MultiStockPanel multiStockPanel;
    private PreMarketCloseOrderPanel preMarketCloseOrderPanel;

    public MainFrame(ConnectionService connectionService, ConnectionConfig config) {
        this.frame = new JFrame("IBKR Trading System");
        this.messageArea = new JTextArea();
        this.tabbedPanel = new JTabbedPane();
        this.connectionService = connectionService;
        
        // Initialize navbar components
        this.accountSelector = new JComboBox<>();
        this.symbolField = new JTextField(AppConfig.getInstance().getCurrentTradingSymbol(), 8);
        this.accountLabel = new JLabel("No account selected");
        this.symbolLabel = new JLabel("Symbol: " + AppConfig.getInstance().getCurrentTradingSymbol());
        
        // Add action listeners - Enter key triggers validation
        this.accountSelector.addActionListener(e -> onAccountSelected());
        this.symbolField.addActionListener(e -> onSymbolChangedWithValidation());
        // Note: Removed focusLost listener - validation only happens when user explicitly clicks button or presses Enter
        
        if (connectionService != null) {
            initializePanels(connectionService, config);
        }
        
        initializeUI();
    }
    
    public void setConnectionService(ConnectionService connectionService) {
        this.connectionService = connectionService;
        ConnectionConfig config = ConnectionConfig.getDefault();
        initializePanels(connectionService, config);
        updateTabbedPanels();
    }
    
    private void initializePanels(ConnectionService connectionService, ConnectionConfig config) {
        this.connectionPanel = new ConnectionPanel(connectionService, config);
        this.calendarSpreadPanel = new CalendarSpreadPanel(connectionService);
        this.stranglePanel = new StranglePanel(connectionService);
        this.multiStockPanel = new MultiStockPanel(connectionService.getOrderService());
        this.preMarketCloseOrderPanel = new PreMarketCloseOrderPanel(connectionService);
    }
    
    private void updateTabbedPanels() {
        tabbedPanel.removeAll();
        tabbedPanel.addTab("Connection", connectionPanel);
        tabbedPanel.addTab("Calendar Spread", calendarSpreadPanel);
        tabbedPanel.addTab("Strangle", stranglePanel);
        tabbedPanel.addTab("Multi-Stock", multiStockPanel);
        tabbedPanel.addTab("Pre-Market Close", preMarketCloseOrderPanel);
    }

    private void initializeUI() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        // Create navigation bar
        JPanel navbar = createNavigationBar();
        frame.add(navbar, BorderLayout.NORTH);
        
        // Add tabs
        if (connectionPanel != null) {
            tabbedPanel.addTab("Connection", connectionPanel);
            tabbedPanel.addTab("Calendar Spread", calendarSpreadPanel);
            tabbedPanel.addTab("Strangle", stranglePanel);
            tabbedPanel.addTab("Multi-Stock", multiStockPanel);
            tabbedPanel.addTab("Pre-Market Close", preMarketCloseOrderPanel);
        }
        
        frame.add(tabbedPanel, BorderLayout.CENTER);
        
        // Message area at bottom
        messageArea.setEditable(false);
        messageArea.setRows(4);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        messageArea.setBackground(new Color(240, 240, 240));
        
        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(BorderFactory.createTitledBorder("Messages"));
        messageScroll.setPreferredSize(new Dimension(frame.getWidth(), 120));
        
        frame.add(messageScroll, BorderLayout.SOUTH);
        
        loadApplicationIcon();
        frame.setSize(1200, 800);
        frame.setMinimumSize(new Dimension(1000, 600)); // Prevent resizing too small
        frame.setLocationRelativeTo(null);
    }
    
    /**
     * Creates a modern navigation bar with account selector and symbol input.
     */
    private JPanel createNavigationBar() {
        JPanel navbar = new JPanel(new BorderLayout());
        navbar.setBackground(new Color(30, 41, 59)); // Slate-800
        navbar.setBorder(new EmptyBorder(12, 20, 12, 20));
        navbar.setPreferredSize(new Dimension(frame.getWidth(), 65));
        
        // Left section - Account selector
        JPanel leftSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        leftSection.setOpaque(false);
        
        JLabel accountLabelText = new JLabel("<html>👤 &nbsp;Account:</html>");
        accountLabelText.setForeground(new Color(226, 232, 240)); // Slate-200
        accountLabelText.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        accountSelector.setPreferredSize(new Dimension(160, 36));
        accountSelector.setFont(new Font("SansSerif", Font.PLAIN, 13));
        accountSelector.setToolTipText("Select trading account");
        
        accountLabel.setForeground(new Color(34, 197, 94)); // Green-500
        accountLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        
        leftSection.add(accountLabelText);
        leftSection.add(accountSelector);
        leftSection.add(accountLabel);
        
        // Right section - Symbol input with icon
        JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        rightSection.setOpaque(false);
        
        JLabel symbolLabelText = new JLabel("<html>📊 &nbsp;Symbol:</html>");
        symbolLabelText.setForeground(new Color(226, 232, 240)); // Slate-200
        symbolLabelText.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        symbolField.setPreferredSize(new Dimension(110, 36));
        symbolField.setFont(new Font("SansSerif", Font.BOLD, 15));
        symbolField.setHorizontalAlignment(JTextField.CENTER);
        symbolField.setToolTipText("Enter stock symbol (e.g., SPY, AAPL)");
        symbolField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(71, 85, 105), 1, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        JButton setSymbolButton = new JButton("Verify & Set");
        setSymbolButton.setPreferredSize(new Dimension(130, 36));
        setSymbolButton.setBackground(new Color(37, 99, 235)); // Blue-600
        setSymbolButton.setForeground(Color.WHITE);
        setSymbolButton.setFocusPainted(false);
        setSymbolButton.setBorderPainted(false);
        setSymbolButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        setSymbolButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setSymbolButton.addActionListener(e -> onSymbolChangedWithValidation());
        
        // Add hover effect
        setSymbolButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                setSymbolButton.setBackground(new Color(29, 78, 216)); // Blue-700
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                setSymbolButton.setBackground(new Color(37, 99, 235)); // Blue-600
            }
        });
        
        symbolLabel.setForeground(new Color(96, 165, 250)); // Blue-400
        symbolLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        
        rightSection.add(symbolLabelText);
        rightSection.add(symbolField);
        rightSection.add(setSymbolButton);
        rightSection.add(Box.createHorizontalStrut(10));
        rightSection.add(symbolLabel);
        
        navbar.add(leftSection, BorderLayout.WEST);
        navbar.add(rightSection, BorderLayout.EAST);
        
        return navbar;
    }
    
    /**
     * Called when an account is selected from the dropdown.
     */
    private void onAccountSelected() {
        String selectedAccount = (String) accountSelector.getSelectedItem();
        if (selectedAccount != null && !selectedAccount.isEmpty() && !"No accounts".equals(selectedAccount)) {
            AppConfig.getInstance().setCurrentTradingAccount(selectedAccount);
            accountLabel.setText("<html>✓ &nbsp;" + selectedAccount + "</html>");
            showMessage("Selected account: " + selectedAccount);
        } else {
            AppConfig.getInstance().setCurrentTradingAccount(null);
        }
    }
    
    /**
     * Called when the symbol is changed.
     */
    private void onSymbolChanged() {
        String symbol = symbolField.getText().trim().toUpperCase();
        if (!symbol.isEmpty()) {
            AppConfig.getInstance().setCurrentTradingSymbol(symbol);
            symbolField.setText(symbol);
            symbolLabel.setText("Symbol: " + symbol);
            showMessage("Trading symbol changed to: " + symbol);
        }
    }
    
    /**
     * Validates the symbol with IB API before setting it.
     */
    private void onSymbolChangedWithValidation() {
        String symbol = symbolField.getText().trim().toUpperCase();
        
        if (symbol.isEmpty()) {
            showWarningDialog("Empty Symbol", "Please enter a valid stock symbol.");
            return;
        }
        
        if (!connectionService.isConnected()) {
            showWarningDialog("Not Connected", 
                "Please connect to TWS/Gateway first before validating symbols.");
            return;
        }
        
        // Show loading indicator
        symbolLabel.setText(">> Validating...");
        symbolLabel.setForeground(new Color(251, 191, 36)); // Amber
        
        // Validate in background thread
        new Thread(() -> {
            boolean isValid = connectionService.getSymbolValidationService().validateSymbol(symbol);
            
            SwingUtilities.invokeLater(() -> {
                if (isValid) {
                    AppConfig.getInstance().setCurrentTradingSymbol(symbol);
                    symbolField.setText(symbol);
                    symbolLabel.setText("<html>✓ &nbsp;" + symbol + "</html>");
                    symbolLabel.setForeground(new Color(34, 197, 94)); // Green
                    showMessage("[SUCCESS] Trading symbol validated and set to: " + symbol);
                    
                    // Notify all panels about symbol change
                    notifyPanelsSymbolChanged();
                } else {
                    symbolLabel.setText("[INVALID]");
                    symbolLabel.setForeground(new Color(239, 68, 68)); // Red
                    showWarningDialog("Invalid Symbol", 
                        "The symbol '" + symbol + "' is not available or invalid.\n\n" +
                        "Please check:\n" +
                        "• Symbol spelling is correct\n" +
                        "• Symbol exists on the exchange\n" +
                        "• You have market data permissions for this symbol");
                }
            });
        }).start();
    }
    
    /**
     * Notifies all strategy panels that the symbol has changed.
     */
    private void notifyPanelsSymbolChanged() {
        if (calendarSpreadPanel != null) {
            calendarSpreadPanel.updateSymbolLabel();
        }
        if (stranglePanel != null) {
            stranglePanel.updateSymbolLabel();
        }
        // MultiStockPanel and PreMarketCloseOrderPanel have their own symbol fields
    }
    
    /**
     * Shows a modern warning dialog.
     */
    private void showWarningDialog(String title, String message) {
        JOptionPane optionPane = new JOptionPane(
            message,
            JOptionPane.WARNING_MESSAGE,
            JOptionPane.DEFAULT_OPTION
        );
        
        JDialog dialog = optionPane.createDialog(frame, title);
        dialog.setVisible(true);
    }
    
    public void show() {
        frame.setVisible(true);
    }

    public void onConnected() {
        connectionPanel.updateStatus("Connected");
        showMessage("Successfully connected to IB TWS/Gateway");
    }

    public void onDisconnected() {
        connectionPanel.updateStatus("Disconnected");
        showMessage("Disconnected from IB TWS/Gateway");
    }

    public void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(message + "\n");
            scrollToBottom();
        });
    }

    public void showError(String error) {
        showMessage("ERROR: " + error);
    }

    private void scrollToBottom() {
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }
    
    /**
     * Updates the accounts list display in the Connection panel.
     * Also populates the account selector in the navbar.
     */
    public void updateAccountsList() {
        if (connectionPanel != null) {
            connectionPanel.updateAccountsList();
        }
        
        // Update navbar account selector
        SwingUtilities.invokeLater(() -> {
            accountSelector.removeAllItems();
            List<String> accounts = connectionService.getAccounts();
            
            if (accounts.isEmpty()) {
                accountSelector.addItem("No accounts");
                accountSelector.setEnabled(false);
                accountLabel.setText("No accounts");
                accountLabel.setForeground(Color.ORANGE);
                AppConfig.getInstance().setCurrentTradingAccount(null);
            } else {
                accountSelector.setEnabled(true);
                for (String account : accounts) {
                    accountSelector.addItem(account);
                }
                // Auto-select first account
                if (accounts.size() > 0) {
                    accountSelector.setSelectedIndex(0);
                    String firstAccount = accounts.get(0);
                    accountLabel.setText("<html>✓ &nbsp;" + firstAccount + "</html>");
                    accountLabel.setForeground(new Color(46, 204, 113)); // Green
                    AppConfig.getInstance().setCurrentTradingAccount(firstAccount);
                }
            }
        });
    }
    
    private void loadApplicationIcon() {
        // Try multiple icon formats for cross-platform compatibility
        String[] iconPaths = {"/TBI.png", "/TBI.ico", "/TBI.icns"};
        
        for (String iconPath : iconPaths) {
            try {
                java.net.URL iconURL = getClass().getResource(iconPath);
                if (iconURL != null) {
                    ImageIcon icon = new ImageIcon(iconURL);
                    // Check if icon loaded successfully and has valid dimensions
                    if (icon.getImageLoadStatus() == MediaTracker.COMPLETE && 
                        icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                        frame.setIconImage(icon.getImage());
                        return; // Successfully loaded
                    }
                }
            } catch (Exception e) {
                // Try next format
            }
        }
    }
}
