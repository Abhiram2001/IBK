package com.ibkr.trading.ui;

import com.ibkr.trading.config.ConnectionConfig;
import com.ibkr.trading.service.ConnectionService;
import com.ibkr.trading.ui.panel.ConnectionPanel;
import com.ibkr.trading.ui.panel.CalendarSpreadPanel;
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
            // Fall back to default cross-platform look and feel
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }
    }

    private final JFrame frame;
    private final JTextArea messageArea;
    private final JTabbedPane tabbedPanel;
    private final ConnectionPanel connectionPanel;
    private final CalendarSpreadPanel calendarSpreadPanel;
    private final StranglePanel stranglePanel;
    private final MultiStockPanel multiStockPanel;

    public MainFrame(ConnectionService connectionService, ConnectionConfig config) {
        this.frame = new JFrame("IBK Trading System - Production v2.0");
        this.messageArea = new JTextArea();
        this.tabbedPanel = new JTabbedPane();
        
        this.connectionPanel = new ConnectionPanel(connectionService, config);
        this.calendarSpreadPanel = new CalendarSpreadPanel(connectionService);
        this.stranglePanel = new StranglePanel(connectionService);
        this.multiStockPanel = new MultiStockPanel(connectionService.getOrderService());
        
        initializeUI();
    }

    private void initializeUI() {
        // Set application icon
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/TBI.ico"));
            if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                frame.setIconImage(icon.getImage());
            } else {
                // Try PNG fallback if ICO doesn't load
                System.err.println("Could not load TBI.ico, application will use default icon");
            }
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }
        
        tabbedPanel.addTab("Connection", connectionPanel);
        tabbedPanel.addTab("Calendar Spread", calendarSpreadPanel);
        tabbedPanel.addTab("Strangle", stranglePanel);
        tabbedPanel.addTab("Multi-Stock", multiStockPanel);

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
}
