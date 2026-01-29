package com.ibkr.trading.ui;

import com.ibkr.trading.config.ConnectionConfig;
import com.ibkr.trading.service.ConnectionService;
import com.ibkr.trading.ui.panel.ConnectionPanel;
import com.ibkr.trading.ui.panel.CalendarSpreadPanel;
import com.ibkr.trading.ui.panel.PreMarketCloseOrderPanel;
import com.ibkr.trading.ui.panel.StranglePanel;
import com.ibkr.trading.ui.panel.MultiStockPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Main application window with tabbed interface for trading strategies.
 * Uses native system Look and Feel for OS-appropriate appearance.
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class MainFrame {
    static {
        try {
            // Use native OS look and feel for professional appearance
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | 
                 IllegalAccessException | UnsupportedLookAndFeelException e) {
            // Fall back to default cross-platform look and feel - will log after UI initialization
        }
    }

    private final JFrame frame;
    private final JTextArea messageArea;
    private final JTabbedPane tabbedPanel;
    private ConnectionPanel connectionPanel;
    private CalendarSpreadPanel calendarSpreadPanel;
    private StranglePanel stranglePanel;
    private MultiStockPanel multiStockPanel;
    private PreMarketCloseOrderPanel preMarketCloseOrderPanel;

    public MainFrame(ConnectionService connectionService, ConnectionConfig config) {
        this.frame = new JFrame("IBK Trading System - Production v2.0");
        this.messageArea = new JTextArea();
        this.tabbedPanel = new JTabbedPane();
        
        if (connectionService != null) {
            initializePanels(connectionService, config);
        }
        
        initializeUI();
    }
    
    public void setConnectionService(ConnectionService connectionService) {
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
        // Set application icon (try PNG first, fallback to ICO)
        boolean iconLoaded = false;
        
        // Try PNG format (most compatible with Java)
        try {
            java.net.URL iconURL = getClass().getResource("/TBI.png");
            if (iconURL != null) {
                ImageIcon icon = new ImageIcon(iconURL);
                if (icon.getImageLoadStatus() == MediaTracker.COMPLETE && icon.getIconWidth() > 0) {
                    frame.setIconImage(icon.getImage());
                    iconLoaded = true;
                }
            }
        } catch (Exception e) {
            // Try ICO format as fallback
        }
        
        // Fallback to ICO if PNG not found
        if (!iconLoaded) {
            try {
                java.net.URL iconURL = getClass().getResource("/TBI.ico");
                if (iconURL != null) {
                    ImageIcon icon = new ImageIcon(iconURL);
                    if (icon.getImageLoadStatus() == MediaTracker.COMPLETE && icon.getIconWidth() > 0) {
                        frame.setIconImage(icon.getImage());
                        iconLoaded = true;
                    }
                }
            } catch (Exception e) {
                // Silently fail - will use default Java icon
            }
        }
        
        if (!iconLoaded) {
            showMessage("Note: Using default application icon");
        }
        
        if (connectionPanel != null) {
            tabbedPanel.addTab("Connection", connectionPanel);
            tabbedPanel.addTab("Calendar Spread", calendarSpreadPanel);
            tabbedPanel.addTab("Strangle", stranglePanel);
            tabbedPanel.addTab("Multi-Stock", multiStockPanel);
            tabbedPanel.addTab("Pre-Market Close", preMarketCloseOrderPanel);
        }

        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setRows(8);
        
        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setPreferredSize(new Dimension(10000, 150));

        frame.setLayout(new BorderLayout());
        frame.add(tabbedPanel, BorderLayout.CENTER);
        frame.add(messageScroll, BorderLayout.SOUTH);
        frame.setSize(1200, 800);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
