package com.ibkr.trading.ui.panel;

import com.ibkr.trading.service.ConnectionService;
import com.ibkr.trading.service.OrderService;
import com.ibkr.trading.service.PriceMonitorService;
import com.ibkr.trading.service.PriceMonitorService.MonitoredOrder;
import com.ibkr.trading.ui.util.PriceAlertDialog;
import com.ibkr.trading.util.ExcelOrderImporter;
import com.ibkr.trading.util.ExcelOrderImporter.ImportedOrder;
import com.ib.client.*;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Panel for Pre-Market Close Order Configuration with Price Monitoring.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Configure close orders before market opens</li>
 *   <li>Monitor option prices in real-time</li>
 *   <li>Automatic order placement when price thresholds are reached</li>
 *   <li>Excel bulk import support</li>
 *   <li>Visual alerts when orders are ready</li>
 * </ul>
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class PreMarketCloseOrderPanel extends JPanel implements PriceMonitorService.PriceAlertListener {
    
    private static final String[] COLUMN_NAMES = {
        "Symbol", "Expiry", "Action", "Type", "Strike", "Target $", "Alert $", "Qty", "Order Type", "Status"
    };
    
    private final transient ConnectionService connectionService;
    private final transient OrderService orderService;
    private final transient PriceMonitorService priceMonitor;
    private final DefaultTableModel tableModel;
    private final JTable configTable;
    private final JLabel statusLabel;
    private final List<CloseOrderConfig> configurations = new ArrayList<>();
    
    /**
     * Configuration for a single close order.
     */
    private static class CloseOrderConfig {
        String symbol;
        String expiry;
        String action;
        String optionType;
        double strike;
        double targetPrice;
        double alertThreshold;
        int quantity;
        String orderType;
        String monitoringId;
        boolean isMonitoring;
        Contract validatedContract;
        
        CloseOrderConfig(String symbol, String expiry, String action, String optionType, 
                        double strike, double targetPrice, double alertThreshold, 
                        int quantity, String orderType) {
            this.symbol = symbol;
            this.expiry = expiry;
            this.action = action;
            this.optionType = optionType;
            this.strike = strike;
            this.targetPrice = targetPrice;
            this.alertThreshold = alertThreshold;
            this.quantity = quantity;
            this.orderType = orderType;
            this.isMonitoring = false;
        }
    }
    
    public PreMarketCloseOrderPanel(ConnectionService connectionService) {
        this.connectionService = connectionService;
        this.orderService = new OrderService(connectionService.getController());
        this.priceMonitor = new PriceMonitorService(connectionService.getController());
        this.priceMonitor.addAlertListener(this);
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        configTable = new JTable(tableModel);
        statusLabel = new JLabel("Ready - Add your close orders for monitoring");
        
        initializeUI();
    }
    
    private void initializeUI() {
        JPanel topPanel = createTopPanel();
        JPanel configPanel = createConfigPanel();
        JPanel buttonPanel = createButtonPanel();
        
        statusLabel.setForeground(Color.BLUE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(configPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Pre-Market Close Order Configuration");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel instructionLabel = new JLabel(
            "<html><div style='text-align: center; padding: 10px;'>" +
            "<b>Configure your close orders before market opens:</b><br/>" +
            "1. Fill in the fields and add orders to list<br/>" +
            "2. Click 'Start Monitoring All' before market opens<br/>" +
            "3. System will alert you and place order in TWS when threshold is reached" +
            "</div></html>"
        );
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(instructionLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Close Order Configurations"));
        
        // Input panel for adding orders
        JPanel addPanel = createAddOrderPanel();
        
        // Table for displaying configured orders
        configTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configTable.setRowHeight(25);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < configTable.getColumnCount(); i++) {
            configTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Set column widths
        configTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // Symbol
        configTable.getColumnModel().getColumn(1).setPreferredWidth(90);   // Expiry
        configTable.getColumnModel().getColumn(2).setPreferredWidth(50);   // Action
        configTable.getColumnModel().getColumn(3).setPreferredWidth(50);   // Type
        configTable.getColumnModel().getColumn(4).setPreferredWidth(60);   // Strike
        configTable.getColumnModel().getColumn(5).setPreferredWidth(70);   // Target $
        configTable.getColumnModel().getColumn(6).setPreferredWidth(70);   // Alert $
        configTable.getColumnModel().getColumn(7).setPreferredWidth(40);   // Qty
        configTable.getColumnModel().getColumn(8).setPreferredWidth(70);   // Order Type
        configTable.getColumnModel().getColumn(9).setPreferredWidth(150);  // Status
        
        JScrollPane scrollPane = new JScrollPane(configTable);
        scrollPane.setPreferredSize(new Dimension(800, 300));
        
        panel.add(addPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createAddOrderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Input fields
        JTextField symbolField = new JTextField("SPY", 8);
        JDateChooser expiryChooser = UIComponents.createDateChooser();
        JComboBox<String> actionCombo = new JComboBox<>(new String[]{"BUY", "SELL"});
        JComboBox<String> optionTypeCombo = new JComboBox<>(new String[]{"CALL", "PUT"});
        JTextField strikeField = new JTextField("600", 8);
        JTextField targetPriceField = new JTextField("1.50", 8);
        JTextField alertThresholdField = new JTextField("0.10", 8);
        JTextField quantityField = new JTextField("1", 6);
        JComboBox<String> orderTypeCombo = new JComboBox<>(new String[]{"LMT", "MKT"});
        
        // Set default expiry to 7 days from now
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        expiryChooser.setDate(cal.getTime());
        
        // Layout components
        int col = 0;
        gbc.gridx = col++; gbc.gridy = 0;
        panel.add(new JLabel("Symbol:"), gbc);
        gbc.gridx = col++;
        panel.add(symbolField, gbc);
        
        gbc.gridx = col++;
        panel.add(new JLabel("Expiry:"), gbc);
        gbc.gridx = col++;
        panel.add(expiryChooser, gbc);
        
        gbc.gridx = col++;
        panel.add(new JLabel("Action:"), gbc);
        gbc.gridx = col++;
        panel.add(actionCombo, gbc);
        
        gbc.gridx = col++;
        panel.add(new JLabel("Type:"), gbc);
        gbc.gridx = col++;
        panel.add(optionTypeCombo, gbc);
        
        col = 0;
        gbc.gridy = 1;
        gbc.gridx = col++;
        panel.add(new JLabel("Strike:"), gbc);
        gbc.gridx = col++;
        panel.add(strikeField, gbc);
        
        gbc.gridx = col++;
        panel.add(new JLabel("Target $:"), gbc);
        gbc.gridx = col++;
        panel.add(targetPriceField, gbc);
        
        gbc.gridx = col++;
        panel.add(new JLabel("Alert $:"), gbc);
        gbc.gridx = col++;
        panel.add(alertThresholdField, gbc);
        
        gbc.gridx = col++;
        panel.add(new JLabel("Qty:"), gbc);
        gbc.gridx = col++;
        panel.add(quantityField, gbc);
        
        gbc.gridx = col++;
        panel.add(new JLabel("Order Type:"), gbc);
        gbc.gridx = col++;
        panel.add(orderTypeCombo, gbc);
        
        // Add button
        JButton addButton = UIComponents.createButton("Add to List", e -> 
            addConfiguration(symbolField, expiryChooser, actionCombo, optionTypeCombo,
                           strikeField, targetPriceField, alertThresholdField, 
                           quantityField, orderTypeCombo));
        addButton.setBackground(new Color(33, 150, 243));
        addButton.setForeground(Color.WHITE);
        
        gbc.gridx = col++;
        gbc.gridwidth = 2;
        panel.add(addButton, gbc);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        
        JButton startAllButton = UIComponents.createButton("Start Monitoring All", e -> startMonitoringAll());
        startAllButton.setBackground(new Color(76, 175, 80));
        startAllButton.setForeground(Color.WHITE);
        startAllButton.setFont(new Font("Arial", Font.BOLD, 14));
        startAllButton.setPreferredSize(new Dimension(200, 35));
        
        JButton stopAllButton = UIComponents.createButton("Stop All Monitoring", e -> stopAllMonitoring());
        stopAllButton.setBackground(new Color(244, 67, 54));
        stopAllButton.setForeground(Color.WHITE);
        stopAllButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopAllButton.setPreferredSize(new Dimension(200, 35));
        
        JButton removeButton = UIComponents.createButton("Remove Selected", e -> removeSelectedConfig());
        
        JButton importButton = UIComponents.createButton("Import from Excel", e -> importFromExcel());
        importButton.setBackground(new Color(255, 152, 0));
        importButton.setForeground(Color.WHITE);
        importButton.setFont(new Font("Arial", Font.BOLD, 14));
        importButton.setPreferredSize(new Dimension(200, 35));
        
        panel.add(importButton);
        panel.add(startAllButton);
        panel.add(stopAllButton);
        panel.add(removeButton);
        
        return panel;
    }
    
    private void addConfiguration(JTextField symbolField, JDateChooser expiryChooser,
                                  JComboBox<String> actionCombo, JComboBox<String> optionTypeCombo,
                                  JTextField strikeField, JTextField targetField, 
                                  JTextField thresholdField, JTextField qtyField,
                                  JComboBox<String> typeCombo) {
        try {
            String symbol = symbolField.getText().trim().toUpperCase();
            if (symbol.isEmpty()) {
                updateStatus("Error: Symbol is required", Color.RED);
                return;
            }
            
            Date expiryDate = expiryChooser.getDate();
            if (expiryDate == null) {
                updateStatus("Error: Expiry date is required", Color.RED);
                return;
            }
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(expiryDate);
            String expiry = String.format("%d%02d%02d", 
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
            
            String expiryDisplay = new SimpleDateFormat("dd-MMM-yy").format(expiryDate);
            
            String action = (String) actionCombo.getSelectedItem();
            String optionType = (String) optionTypeCombo.getSelectedItem();
            double strike = Double.parseDouble(strikeField.getText());
            double targetPrice = Double.parseDouble(targetField.getText());
            double alertThreshold = Double.parseDouble(thresholdField.getText());
            int quantity = Integer.parseInt(qtyField.getText());
            String orderType = (String) typeCombo.getSelectedItem();
            
            if (strike <= 0 || targetPrice <= 0 || alertThreshold <= 0 || quantity <= 0) {
                updateStatus("Error: All values must be positive", Color.RED);
                return;
            }
            
            CloseOrderConfig config = new CloseOrderConfig(symbol, expiry, action, optionType,
                                                          strike, targetPrice, alertThreshold, 
                                                          quantity, orderType);
            configurations.add(config);
            
            tableModel.addRow(new Object[]{
                symbol,
                expiryDisplay,
                action,
                optionType,
                String.format("%.2f", strike),
                String.format("$%.2f", targetPrice),
                String.format("$%.2f", alertThreshold),
                quantity,
                orderType,
                "Ready"
            });
            
            updateStatus("Configuration added successfully. Total: " + configurations.size(), 
                        new Color(0, 128, 0));
            
        } catch (NumberFormatException e) {
            updateStatus("Error: Invalid numeric value - " + e.getMessage(), Color.RED);
        } catch (Exception e) {
            updateStatus("Error: " + e.getMessage(), Color.RED);
        }
    }
    
    private void startMonitoringAll() {
        if (configurations.isEmpty()) {
            updateStatus("No configurations to monitor", Color.ORANGE);
            return;
        }
        
        int started = 0;
        for (int i = 0; i < configurations.size(); i++) {
            CloseOrderConfig config = configurations.get(i);
            if (!config.isMonitoring) {
                startMonitoringConfig(config, i);
                started++;
            }
        }
        
        updateStatus(String.format("Started monitoring %d orders. Waiting for price alerts...", started),
                    new Color(0, 128, 0));
    }
    
    private void startMonitoringConfig(CloseOrderConfig config, int rowIndex) {
        Contract contract = new Contract();
        contract.symbol(config.symbol);
        contract.secType("OPT");
        contract.exchange("SMART");
        contract.currency("USD");
        contract.lastTradeDateOrContractMonth(config.expiry);
        contract.strike(config.strike);
        contract.right(config.optionType.equals("CALL") ? "C" : "P");
        contract.multiplier("100");
        
        // Validate contract first
        orderService.getContractDetails(contract)
            .thenAccept(detailsList -> SwingUtilities.invokeLater(() -> {
                if (detailsList.isEmpty() || detailsList.size() > 1) {
                    tableModel.setValueAt("Contract Error", rowIndex, 9);
                    updateStatus("Error: Contract validation failed for " + config.symbol, Color.RED);
                    return;
                }
                
                Contract validatedContract = detailsList.get(0).contract();
                config.validatedContract = validatedContract;
                
                // Start monitoring
                String monitorId = priceMonitor.startMonitoring(
                    validatedContract, 
                    config.targetPrice, 
                    config.alertThreshold, 
                    config.action
                );
                
                config.monitoringId = monitorId;
                config.isMonitoring = true;
                
                tableModel.setValueAt("Monitoring...", rowIndex, 9);
                updateStatus("Monitoring " + config.symbol + " - Waiting for price alert...", 
                           new Color(0, 100, 200));
            }))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    tableModel.setValueAt("Error: " + ex.getMessage(), rowIndex, 9);
                    updateStatus("Error validating contract: " + ex.getMessage(), Color.RED);
                });
                return null;
            });
    }
    
    private void stopAllMonitoring() {
        priceMonitor.stopAllMonitoring();
        
        for (int i = 0; i < configurations.size(); i++) {
            configurations.get(i).isMonitoring = false;
            tableModel.setValueAt("Stopped", i, 9);
        }
        
        updateStatus("All monitoring stopped", Color.BLUE);
    }
    
    private void removeSelectedConfig() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow < 0) {
            updateStatus("Please select a configuration to remove", Color.ORANGE);
            return;
        }
        
        CloseOrderConfig config = configurations.get(selectedRow);
        if (config.isMonitoring && config.monitoringId != null) {
            priceMonitor.stopMonitoring(config.monitoringId);
        }
        
        configurations.remove(selectedRow);
        tableModel.removeRow(selectedRow);
        
        updateStatus("Configuration removed", Color.BLUE);
    }
    
    @Override
    public void onPriceAlert(MonitoredOrder order, double currentPrice, double distance) {
        SwingUtilities.invokeLater(() -> {
            // Find matching configuration
            for (int i = 0; i < configurations.size(); i++) {
                CloseOrderConfig config = configurations.get(i);
                if (config.monitoringId != null && config.monitoringId.equals(order.id)) {
                    tableModel.setValueAt("⚠ ALERT - Placing TWS Order", i, 9);
                    
                    placeOrderInTWS(config, order, currentPrice, i);
                    break;
                }
            }
            
            updateStatus("🔔 ALERT: Order " + order.id + " threshold reached! Placing in TWS...",
                        new Color(255, 69, 0));
            
            // Show alert dialog
            showAlertDialog(order, currentPrice, distance);
        });
    }
    
    @Override
    public void onPriceUpdate(MonitoredOrder order, double currentPrice) {
        // Optional: Update UI with current prices (can be implemented for real-time tracking)
    }
    
    private void placeOrderInTWS(CloseOrderConfig config, MonitoredOrder order, 
                                 double currentPrice, int rowIndex) {
        if (config.validatedContract == null || config.validatedContract.conid() == 0) {
            SwingUtilities.invokeLater(() -> {
                tableModel.setValueAt("Error: No validated contract", rowIndex, 9);
                updateStatus("Error: Contract not validated for " + config.symbol, Color.RED);
            });
            return;
        }
        
        // Use validated contract with conid only
        Contract orderContract = new Contract();
        orderContract.conid(config.validatedContract.conid());
        orderContract.exchange("SMART");
        
        double orderPrice = config.orderType.equals("LMT") ? config.targetPrice : 0.0;
        
        orderService.placeSimpleOrder(
            orderContract,
            config.action,
            config.quantity,
            orderPrice,
            status -> SwingUtilities.invokeLater(() -> {
                tableModel.setValueAt("TWS: " + status, rowIndex, 9);
                if (status.contains("Submitted") || status.contains("PreSubmitted")) {
                    updateStatus("✓ Order placed successfully for " + config.symbol, 
                               new Color(0, 128, 0));
                } else if (status.contains("Error")) {
                    updateStatus("✗ Error placing order: " + status, Color.RED);
                }
            })
        );
    }
    
    private void showAlertDialog(MonitoredOrder order, double currentPrice, double distance) {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        PriceAlertDialog.showAlert(parent, order, currentPrice, distance);
    }
    
    private void importFromExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Excel File with Orders");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx, *.xls)", "xlsx", "xls"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File selectedFile = fileChooser.getSelectedFile();
        updateStatus("Importing from Excel: " + selectedFile.getName(), Color.BLUE);
        
        // Import in background
        new SwingWorker<ExcelOrderImporter.ImportResult, Void>() {
            @Override
            protected ExcelOrderImporter.ImportResult doInBackground() {
                return ExcelOrderImporter.importFromExcel(selectedFile);
            }
            
            @Override
            protected void done() {
                try {
                    ExcelOrderImporter.ImportResult importResult = get();
                    
                    if (!importResult.errors.isEmpty()) {
                        StringBuilder errorMsg = new StringBuilder("Import completed with errors:\n\n");
                        for (String error : importResult.errors) {
                            errorMsg.append("• ").append(error).append("\n");
                        }
                        JOptionPane.showMessageDialog(PreMarketCloseOrderPanel.this,
                            errorMsg.toString(), "Import Warnings", JOptionPane.WARNING_MESSAGE);
                    }
                    
                    if (importResult.orders.isEmpty()) {
                        updateStatus("No valid orders found in Excel file", Color.RED);
                        return;
                    }
                    
                    int importedCount = 0;
                    for (ImportedOrder order : importResult.orders) {
                        addImportedOrder(order);
                        importedCount++;
                    }
                    
                    updateStatus(String.format("Successfully imported %d orders from Excel. Total: %d",
                        importedCount, configurations.size()), new Color(0, 128, 0));
                    
                    JOptionPane.showMessageDialog(PreMarketCloseOrderPanel.this,
                        String.format("Successfully imported %d orders!\n\nYou can now click 'Start Monitoring All'.",
                            importedCount),
                        "Import Successful", JOptionPane.INFORMATION_MESSAGE);
                    
                } catch (Exception e) {
                    updateStatus("Error importing Excel: " + e.getMessage(), Color.RED);
                    JOptionPane.showMessageDialog(PreMarketCloseOrderPanel.this,
                        "Error importing Excel file: " + e.getMessage(),
                        "Import Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    
    private void addImportedOrder(ImportedOrder order) {
        String expiryDisplay = formatExpiryDisplay(order.expiry);
        
        CloseOrderConfig config = new CloseOrderConfig(
            order.symbol, order.expiry, order.action, order.optionType,
            order.strike, order.targetPrice, order.alertThreshold,
            order.quantity, order.orderType
        );
        configurations.add(config);
        
        tableModel.addRow(new Object[]{
            order.symbol,
            expiryDisplay,
            order.action,
            order.optionType,
            String.format("%.2f", order.strike),
            String.format("$%.2f", order.targetPrice),
            String.format("$%.2f", order.alertThreshold),
            order.quantity,
            order.orderType,
            "Ready"
        });
    }
    
    private String formatExpiryDisplay(String expiry) {
        if (expiry.length() != 8) return expiry;
        
        try {
            int year = Integer.parseInt(expiry.substring(0, 4));
            int month = Integer.parseInt(expiry.substring(4, 6));
            int day = Integer.parseInt(expiry.substring(6, 8));
            
            String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
            return String.format("%02d-%s-%02d", day, months[month-1], year % 100);
        } catch (Exception e) {
            return expiry;
        }
    }
    
    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }
}
