package com.ibkr.trading.ui.util;

import com.ibkr.trading.service.PriceMonitorService.MonitoredOrder;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for displaying price alerts when monitored orders reach their thresholds.
 * Provides visual and auditory alerts to the user.
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class PriceAlertDialog {
    
    /**
     * Shows a price alert dialog when an order reaches its threshold.
     * 
     * @param parent parent frame for the dialog
     * @param order the monitored order that triggered the alert
     * @param currentPrice the current market price
     * @param distance distance from current price to target
     */
    public static void showAlert(Frame parent, MonitoredOrder order, 
                                double currentPrice, double distance) {
        SwingUtilities.invokeLater(() -> {
            // Create alert dialog
            JDialog dialog = new JDialog(parent, "Price Alert!", true);
            dialog.setLayout(new BorderLayout(10, 10));
            
            // Create alert icon panel
            JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JLabel iconLabel = new JLabel("⚠");
            iconLabel.setFont(new Font("Arial", Font.BOLD, 48));
            iconLabel.setForeground(new Color(255, 140, 0)); // Orange
            iconPanel.add(iconLabel);
            
            // Create message panel
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            
            JLabel titleLabel = new JLabel("Price Alert Triggered!");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
            titleLabel.setForeground(new Color(255, 69, 0)); // Red-orange
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            messagePanel.add(titleLabel);
            messagePanel.add(Box.createVerticalStrut(15));
            
            // Order details
            addDetailLabel(messagePanel, "Order ID:", order.id);
            addDetailLabel(messagePanel, "Symbol:", order.contract.symbol());
            addDetailLabel(messagePanel, "Strike:", String.format("$%.2f", order.contract.strike()));
            addDetailLabel(messagePanel, "Action:", order.action);
            messagePanel.add(Box.createVerticalStrut(10));
            
            // Price information
            addDetailLabel(messagePanel, "Target Price:", String.format("$%.2f", order.targetPrice));
            addDetailLabel(messagePanel, "Current Price:", String.format("$%.2f", currentPrice));
            addDetailLabel(messagePanel, "Distance:", String.format("$%.2f (%.1f%%)", 
                distance, order.getDistancePercentage()));
            messagePanel.add(Box.createVerticalStrut(10));
            
            // Status message
            JLabel statusLabel = new JLabel("Order ready to be placed in TWS!");
            statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
            statusLabel.setForeground(new Color(0, 128, 0)); // Green
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            messagePanel.add(statusLabel);
            
            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            
            JButton okButton = new JButton("OK");
            okButton.setPreferredSize(new Dimension(100, 35));
            okButton.setFont(new Font("Arial", Font.BOLD, 14));
            okButton.setBackground(new Color(76, 175, 80)); // Green
            okButton.setForeground(Color.WHITE);
            okButton.setFocusPainted(false);
            okButton.addActionListener(e -> dialog.dispose());
            
            buttonPanel.add(okButton);
            
            // Assemble dialog
            dialog.add(iconPanel, BorderLayout.NORTH);
            dialog.add(messagePanel, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            
            // Configure dialog
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            
            // Play system beep
            Toolkit.getDefaultToolkit().beep();
            
            // Show dialog
            dialog.setVisible(true);
        });
    }
    
    private static void addDetailLabel(JPanel panel, String label, String value) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Arial", Font.BOLD, 13));
        
        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font("Arial", Font.PLAIN, 13));
        
        rowPanel.add(labelComponent);
        rowPanel.add(valueComponent);
        
        panel.add(rowPanel);
    }
    
    /**
     * Shows a simple information alert with custom message.
     */
    public static void showInfo(Frame parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Shows a simple error alert with custom message.
     */
    public static void showError(Frame parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, 
            JOptionPane.ERROR_MESSAGE);
    }
}
