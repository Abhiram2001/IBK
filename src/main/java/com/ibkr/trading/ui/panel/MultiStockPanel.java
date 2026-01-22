package com.ibkr.trading.ui.panel;

import com.ibkr.trading.domain.MultiStockStrategy;
import com.ibkr.trading.domain.MultiStockStrategy.StockOrder;
import com.ibkr.trading.service.OrderService;
import com.ibkr.trading.util.ValidationUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UI panel for multi-stock trading strategy.
 * Allows configuration and execution of multiple simultaneous stock orders.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Support for up to 4 simultaneous stock orders</li>
 *   <li>Configurable symbol, quantity, and limit price for each order</li>
 *   <li>Individual buy/sell action selection</li>
 *   <li>Default values for common tech stocks</li>
 *   <li>Batch order placement</li>
 *   <li>Input validation</li>
 * </ul>
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class MultiStockPanel extends JPanel {
    private static final int MAX_STOCKS = 4;
    private static final String[] DEFAULT_SYMBOLS = {"AAPL", "MSFT", "GOOGL", "AMZN"};
    private static final String DEFAULT_QUANTITY = "100";
    private static final String[] DEFAULT_PRICES = {"150.0", "350.0", "140.0", "175.0"};
    
    private final OrderService orderService;
    private final List<StockEntry> stockEntries = new ArrayList<>();
    private final JTextArea statusArea;
    private final JButton executeButton;
    
    /**
     * UI components for a single stock order entry.
     */
    private static class StockEntry {
        final JTextField symbolField = new JTextField(10);
        final JTextField quantityField = new JTextField(8);
        final JTextField priceField = new JTextField(10);
        final JCheckBox sellCheckBox = new JCheckBox("Sell");
        
        StockEntry() {
            symbolField.setToolTipText("Stock symbol (e.g., AAPL)");
            quantityField.setToolTipText("Number of shares");
            priceField.setToolTipText("Limit price per share");
            sellCheckBox.setToolTipText("Check for SELL, uncheck for BUY");
        }
        
        boolean hasData() {
            return !symbolField.getText().trim().isEmpty();
        }
        
        void clear() {
            symbolField.setText("");
            quantityField.setText("");
            priceField.setText("");
            sellCheckBox.setSelected(false);
        }
    }
    
    /**
     * Constructs a new MultiStockPanel.
     * 
     * @param orderService the order service for placing orders
     */
    public MultiStockPanel(OrderService orderService) {
        this.orderService = orderService;
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Initialize components
        executeButton = new JButton("Place Orders");
        executeButton.setVisible(false);
        
        statusArea = new JTextArea(3, 40);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        
        // Title
        JLabel titleLabel = new JLabel("Multi-Stock Strategy", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        
        // Stock entries panel
        JPanel entriesPanel = createStockEntriesPanel();
        contentPanel.add(entriesPanel, BorderLayout.CENTER);
        
        // Control panel
        JPanel controlPanel = createControlPanel();
        contentPanel.add(controlPanel, BorderLayout.SOUTH);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Fixed-height status area at bottom
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setPreferredSize(new Dimension(10000, 60));
        statusScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        add(statusScroll, BorderLayout.SOUTH);
    }
    
    /**
     * Creates the panel containing stock entry fields.
     */
    private JPanel createStockEntriesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Stock Orders",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Column headers
        gbc.gridy = 0;
        gbc.gridx = 0;
        panel.add(new JLabel("#", SwingConstants.CENTER), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel("Symbol", SwingConstants.CENTER), gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("Quantity", SwingConstants.CENTER), gbc);
        gbc.gridx = 3;
        panel.add(new JLabel("Limit Price", SwingConstants.CENTER), gbc);
        gbc.gridx = 4;
        panel.add(new JLabel("Action", SwingConstants.CENTER), gbc);
        
        // Create stock entry rows
        for (int i = 0; i < MAX_STOCKS; i++) {
            StockEntry entry = new StockEntry();
            stockEntries.add(entry);
            
            gbc.gridy = i + 1;
            
            gbc.gridx = 0;
            panel.add(new JLabel("Stock " + (i + 1) + ":", SwingConstants.RIGHT), gbc);
            
            gbc.gridx = 1;
            panel.add(entry.symbolField, gbc);
            
            gbc.gridx = 2;
            panel.add(entry.quantityField, gbc);
            
            gbc.gridx = 3;
            panel.add(entry.priceField, gbc);
            
            gbc.gridx = 4;
            JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            checkBoxPanel.add(entry.sellCheckBox);
            panel.add(checkBoxPanel, gbc);
        }
        
        return panel;
    }
    
    /**
     * Creates the control panel with action buttons.
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton defaultsButton = UIComponents.createButton("Populate Defaults", e -> populateDefaults());
        JButton clearButton = UIComponents.createButton("Clear All", e -> clearAll());
        executeButton.addActionListener(e -> executeStrategy());
        
        panel.add(defaultsButton);
        panel.add(clearButton);
        panel.add(executeButton);
        
        return panel;
    }
    
    /**
     * Populates default values for common tech stocks.
     */
    private void populateDefaults() {
        for (int i = 0; i < Math.min(stockEntries.size(), DEFAULT_SYMBOLS.length); i++) {
            StockEntry entry = stockEntries.get(i);
            entry.symbolField.setText(DEFAULT_SYMBOLS[i]);
            entry.quantityField.setText(DEFAULT_QUANTITY);
            entry.priceField.setText(DEFAULT_PRICES[i]);
            entry.sellCheckBox.setSelected(true);
        }
        
        executeButton.setVisible(true);
        updateStatus("Default values populated for " + DEFAULT_SYMBOLS.length + " stocks");
    }
    
    /**
     * Clears all input fields.
     */
    private void clearAll() {
        stockEntries.forEach(StockEntry::clear);
        updateStatus("All fields cleared");
        executeButton.setVisible(false);
    }
    
    /**
     * Validates and executes the multi-stock strategy.
     */
    private void executeStrategy() {
        try {
            MultiStockStrategy strategy = buildStrategy();
            
            if (strategy.getOrders().isEmpty()) {
                updateStatus("Error: No valid orders to place");
                return;
            }
            
            // Confirm with user
            int orderCount = strategy.getOrders().size();
            int result = JOptionPane.showConfirmDialog(
                this,
                String.format("Place %d orders?\n\n%s", orderCount, getOrderSummary(strategy)),
                "Confirm Orders",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (result != JOptionPane.YES_OPTION) {
                updateStatus("Order placement cancelled");
                return;
            }
            
            // Place orders
            updateStatus("Placing " + orderCount + " orders...");
            
            placeOrders(strategy);
            
        } catch (IllegalArgumentException e) {
            updateStatus("Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Builds a MultiStockStrategy from UI inputs.
     */
    private MultiStockStrategy buildStrategy() {
        MultiStockStrategy.Builder builder = MultiStockStrategy.builder();
        
        for (StockEntry entry : stockEntries) {
            if (!entry.hasData()) {
                continue;
            }
            
            // Validate and parse inputs
            String symbol = entry.symbolField.getText().trim().toUpperCase();
            ValidationUtils.validateSymbol(symbol);
            
            int quantity = ValidationUtils.parsePositiveInt(
                entry.quantityField.getText(),
                "Quantity for " + symbol
            );
            
            double price = ValidationUtils.parsePositiveDouble(
                entry.priceField.getText(),
                "Price for " + symbol
            );
            
            boolean isSell = entry.sellCheckBox.isSelected();
            
            builder.addOrder(symbol, quantity, price, isSell);
        }
        
        return builder.build();
    }
    
    /**
     * Creates a summary string of all orders.
     */
    private String getOrderSummary(MultiStockStrategy strategy) {
        StringBuilder summary = new StringBuilder();
        for (StockOrder order : strategy.getOrders()) {
            summary.append(String.format("%s %d %s @ $%.2f%n",
                order.getAction(),
                order.getQuantity(),
                order.getSymbol(),
                order.getLimitPrice()
            ));
        }
        return summary.toString();
    }
    
    /**
     * Places all orders in the strategy.
     */
    private void placeOrders(MultiStockStrategy strategy) {
        int successCount = 0;
        int failCount = 0;
        
        for (StockOrder order : strategy.getOrders()) {
            orderService.placeMultiStockOrder(
                order.getSymbol(),
                order.getQuantity(),
                order.getLimitPrice(),
                order.isSell()
            ).thenAccept(success -> {
                if (success) {
                    SwingUtilities.invokeLater(() -> 
                        updateStatus(String.format(
                            "Order placed: %s %d %s @ $%.2f",
                            order.getAction(),
                            order.getQuantity(),
                            order.getSymbol(),
                            order.getLimitPrice()
                        ))
                    );
                }
            }).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> 
                    updateStatus("Error placing order for " + order.getSymbol() + ": " + ex.getMessage())
                );
                return null;
            });
        }
        
        // Final status after all orders
        SwingUtilities.invokeLater(() -> 
            updateStatus(String.format(
                "Completed: %d orders placed",
                strategy.getOrders().size()
            ))
        );
    }
    
    private void updateStatus(String message) {
        statusArea.setText(message);
    }
}
