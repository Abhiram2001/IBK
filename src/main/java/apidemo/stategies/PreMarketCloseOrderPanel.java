package apidemo.stategies;

import apidemo.util.HtmlButton;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;
import com.ib.client.*;
import com.ib.controller.ApiController;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JMonthChooser;
import com.toedter.calendar.JTextFieldDateEditor;

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

public class PreMarketCloseOrderPanel extends JPanel implements PriceMonitor.PriceAlertListener {
    
    private final TradingStrategies m_parent;
    private final PriceMonitor priceMonitor;
    private DefaultTableModel tableModel;
    private JTable configTable;
    private JLabel statusLabel;
    
    private static final String[] COLUMN_NAMES = {
        "Symbol", "Expiry", "Action", "Type", "Strike", "Target $", "Alert $", "Qty", "Order Type", "Status"
    };
    
    private List<CloseOrderConfig> configurations = new ArrayList<>();
    
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
    
    public PreMarketCloseOrderPanel(TradingStrategies parent) {
        this.m_parent = parent;
        this.priceMonitor = new PriceMonitor(parent.controller());
        this.priceMonitor.addAlertListener(this);
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel topPanel = createTopPanel();
        JPanel configPanel = createConfigPanel();
        JPanel buttonPanel = createButtonPanel();
        
        statusLabel = new JLabel("Ready - Add your close orders for monitoring");
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
        
        JPanel addPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        UpperField symbolField = new UpperField();
        symbolField.setText("SPY");
        symbolField.setPreferredSize(new Dimension(60, 25));
        
        JDateChooser expiryDateChooser = createDateChooser();
        
        JComboBox<String> actionCombo = new JComboBox<>(new String[]{"BUY", "SELL"});
        actionCombo.setPreferredSize(new Dimension(70, 25));
        
        JComboBox<String> optionTypeCombo = new JComboBox<>(new String[]{"CALL", "PUT"});
        optionTypeCombo.setPreferredSize(new Dimension(70, 25));
        
        UpperField strikeField = new UpperField();
        strikeField.setText("600");
        strikeField.setPreferredSize(new Dimension(60, 25));
        
        UpperField targetPriceField = new UpperField();
        targetPriceField.setText("1.50");
        targetPriceField.setPreferredSize(new Dimension(60, 25));
        
        UpperField alertThresholdField = new UpperField();
        alertThresholdField.setText("0.10");
        alertThresholdField.setPreferredSize(new Dimension(60, 25));
        
        UpperField quantityField = new UpperField();
        quantityField.setText("1");
        quantityField.setPreferredSize(new Dimension(50, 25));
        
        JComboBox<String> orderTypeCombo = new JComboBox<>(new String[]{"LMT", "MKT"});
        orderTypeCombo.setPreferredSize(new Dimension(60, 25));
        
        int col = 0;
        gbc.gridx = col++; gbc.gridy = 0;
        addPanel.add(new JLabel("Symbol:"), gbc);
        gbc.gridx = col++;
        addPanel.add(symbolField, gbc);
        
        gbc.gridx = col++;
        addPanel.add(new JLabel("Expiry:"), gbc);
        gbc.gridx = col++;
        addPanel.add(expiryDateChooser, gbc);
        
        gbc.gridx = col++;
        addPanel.add(new JLabel("Action:"), gbc);
        gbc.gridx = col++;
        addPanel.add(actionCombo, gbc);
        
        gbc.gridx = col++;
        addPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = col++;
        addPanel.add(optionTypeCombo, gbc);
        
        gbc.gridx = col++;
        addPanel.add(new JLabel("Strike:"), gbc);
        gbc.gridx = col++;
        addPanel.add(strikeField, gbc);
        
        col = 0;
        gbc.gridy = 1;
        gbc.gridx = col++;
        addPanel.add(new JLabel("Target $:"), gbc);
        gbc.gridx = col++;
        addPanel.add(targetPriceField, gbc);
        
        gbc.gridx = col++;
        addPanel.add(new JLabel("Alert $:"), gbc);
        gbc.gridx = col++;
        addPanel.add(alertThresholdField, gbc);
        
        gbc.gridx = col++;
        addPanel.add(new JLabel("Qty:"), gbc);
        gbc.gridx = col++;
        addPanel.add(quantityField, gbc);
        
        gbc.gridx = col++;
        addPanel.add(new JLabel("Order Type:"), gbc);
        gbc.gridx = col++;
        addPanel.add(orderTypeCombo, gbc);
        
        HtmlButton addButton = new HtmlButton("Add to List") {
            @Override
            protected void actionPerformed() {
                addConfiguration(symbolField, expiryDateChooser, actionCombo, optionTypeCombo,
                               strikeField, targetPriceField, alertThresholdField, 
                               quantityField, orderTypeCombo);
            }
        };
        addButton.setBackground(new Color(33, 150, 243));
        addButton.setForeground(Color.WHITE);
        
        gbc.gridx = col++;
        gbc.gridwidth = 2;
        addPanel.add(addButton, gbc);
        
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        configTable = new JTable(tableModel);
        configTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configTable.setRowHeight(25);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < configTable.getColumnCount(); i++) {
            configTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        configTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        configTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        configTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        configTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        configTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        configTable.getColumnModel().getColumn(5).setPreferredWidth(70);
        configTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        configTable.getColumnModel().getColumn(7).setPreferredWidth(40);
        configTable.getColumnModel().getColumn(8).setPreferredWidth(70);
        configTable.getColumnModel().getColumn(9).setPreferredWidth(120);
        
        JScrollPane scrollPane = new JScrollPane(configTable);
        scrollPane.setPreferredSize(new Dimension(800, 200));
        
        panel.add(addPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        
        HtmlButton startAllButton = new HtmlButton("Start Monitoring All") {
            @Override
            protected void actionPerformed() {
                startMonitoringAll();
            }
        };
        startAllButton.setBackground(new Color(76, 175, 80));
        startAllButton.setForeground(Color.WHITE);
        startAllButton.setFont(new Font("Arial", Font.BOLD, 14));
        startAllButton.setPreferredSize(new Dimension(200, 35));
        
        HtmlButton stopAllButton = new HtmlButton("Stop All Monitoring") {
            @Override
            protected void actionPerformed() {
                stopAllMonitoring();
            }
        };
        stopAllButton.setBackground(new Color(244, 67, 54));
        stopAllButton.setForeground(Color.WHITE);
        stopAllButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopAllButton.setPreferredSize(new Dimension(200, 35));
        
        HtmlButton removeButton = new HtmlButton("Remove Selected") {
            @Override
            protected void actionPerformed() {
                removeSelectedConfig();
            }
        };
        
        HtmlButton importButton = new HtmlButton("Import from Excel") {
            @Override
            protected void actionPerformed() {
                importFromExcel();
            }
        };
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
    
    private JDateChooser createDateChooser() {
        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setPreferredSize(new Dimension(120, 25));
        dateChooser.setDateFormatString("dd-MMM-yy");
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        dateChooser.setDate(cal.getTime());
        
        JTextFieldDateEditor editor = (JTextFieldDateEditor) dateChooser.getDateEditor();
        editor.setBackground(Color.WHITE);
        
        JCalendar calendar = dateChooser.getJCalendar();
        calendar.setPreferredSize(new Dimension(300, 300));
        calendar.setMinSelectableDate(new Date());
        calendar.setMaxSelectableDate(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000));
        
        JMonthChooser monthChooser = calendar.getMonthChooser();
        monthChooser.setPreferredSize(new Dimension(100, 25));
        monthChooser.getComboBox().setPreferredSize(new Dimension(100, 25));
        
        calendar.setWeekOfYearVisible(false);
        calendar.setDecorationBackgroundColor(Color.WHITE);
        calendar.setDecorationBordersVisible(true);
        
        return dateChooser;
    }
    
    private void addConfiguration(UpperField symbolField, JDateChooser expiryChooser,
                                  JComboBox<String> actionCombo, JComboBox<String> optionTypeCombo,
                                  UpperField strikeField, UpperField targetField, 
                                  UpperField thresholdField, UpperField qtyField,
                                  JComboBox<String> typeCombo) {
        try {
            String symbol = symbolField.getText().trim().toUpperCase();
            if (symbol.isEmpty()) {
                statusLabel.setText("Error: Symbol is required");
                statusLabel.setForeground(Color.RED);
                return;
            }
            
            Date expiryDate = expiryChooser.getDate();
            if (expiryDate == null) {
                statusLabel.setText("Error: Expiry date is required");
                statusLabel.setForeground(Color.RED);
                return;
            }
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(expiryDate);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DAY_OF_MONTH);
            String expiry = String.format("%d%02d%02d", year, month, day);
            
            String expiryDisplay = String.format("%02d-%s-%02d", day, 
                new String[]{"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"}[month-1],
                year % 100);
            
            String action = (String) actionCombo.getSelectedItem();
            String optionType = (String) optionTypeCombo.getSelectedItem();
            double strike = strikeField.getDouble();
            double targetPrice = targetField.getDouble();
            double alertThreshold = thresholdField.getDouble();
            int quantity = Integer.parseInt(qtyField.getText());
            String orderType = (String) typeCombo.getSelectedItem();
            
            if (strike <= 0 || targetPrice <= 0 || alertThreshold <= 0 || quantity <= 0) {
                statusLabel.setText("Error: All values must be positive");
                statusLabel.setForeground(Color.RED);
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
            
            statusLabel.setText("Configuration added successfully. Total: " + configurations.size());
            statusLabel.setForeground(new Color(0, 128, 0));
            
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
        }
    }
    
    private void startMonitoringAll() {
        if (configurations.isEmpty()) {
            statusLabel.setText("No configurations to monitor");
            statusLabel.setForeground(Color.ORANGE);
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
        
        statusLabel.setText(String.format("Started monitoring %d orders. Waiting for price alerts...", started));
        statusLabel.setForeground(new Color(0, 128, 0));
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
        
        m_parent.controller().reqContractDetails(contract, list -> {
            if (list.isEmpty() || list.size() > 1) {
                SwingUtilities.invokeLater(() -> {
                    tableModel.setValueAt("Contract Error", rowIndex, 9);
                    statusLabel.setText("Error: Contract validation failed for " + config.symbol);
                    statusLabel.setForeground(Color.RED);
                });
                return;
            }
            
            Contract validatedContract = list.get(0).contract();
            config.validatedContract = validatedContract;
            
            SwingUtilities.invokeLater(() -> {
                String monitorId = priceMonitor.startMonitoring(
                    validatedContract, config.targetPrice, config.alertThreshold, 
                    config.action);
                
                config.monitoringId = monitorId;
                config.isMonitoring = true;
                
                tableModel.setValueAt("Monitoring...", rowIndex, 9);
            });
        });
    }
    
    private void stopAllMonitoring() {
        priceMonitor.stopAllMonitoring();
        
        for (int i = 0; i < configurations.size(); i++) {
            configurations.get(i).isMonitoring = false;
            tableModel.setValueAt("Stopped", i, 9);
        }
        
        statusLabel.setText("All monitoring stopped");
        statusLabel.setForeground(Color.BLUE);
    }
    
    private void removeSelectedConfig() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow < 0) {
            statusLabel.setText("Please select a configuration to remove");
            statusLabel.setForeground(Color.ORANGE);
            return;
        }
        
        CloseOrderConfig config = configurations.get(selectedRow);
        if (config.isMonitoring && config.monitoringId != null) {
            priceMonitor.stopMonitoring(config.monitoringId);
        }
        
        configurations.remove(selectedRow);
        tableModel.removeRow(selectedRow);
        
        statusLabel.setText("Configuration removed");
        statusLabel.setForeground(Color.BLUE);
    }
    
    @Override
    public void onPriceAlert(PriceMonitor.MonitoredOrder order, double currentPrice, double distance) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < configurations.size(); i++) {
                CloseOrderConfig config = configurations.get(i);
                if (config.monitoringId != null && config.monitoringId.equals(order.id)) {
                    tableModel.setValueAt("⚠ ALERT - Placing TWS Order", i, 9);
                    
                    placeOrderInTWS(config, order, currentPrice, i);
                    break;
                }
            }
            
            statusLabel.setText("🔔 ALERT: Order " + order.id + " threshold reached! Placing in TWS...");
            statusLabel.setForeground(new Color(255, 69, 0));
            
            showAlertDialog(order, currentPrice, distance);
        });
    }
    
    private void placeOrderInTWS(CloseOrderConfig config, PriceMonitor.MonitoredOrder order, 
                                 double currentPrice, int rowIndex) {
        if (config.validatedContract == null || config.validatedContract.conid() == 0) {
            SwingUtilities.invokeLater(() -> {
                tableModel.setValueAt("Error: No validated contract", rowIndex, 9);
                statusLabel.setText("Error: Contract not validated for " + config.symbol);
                statusLabel.setForeground(Color.RED);
            });
            return;
        }
        
        // Create contract using ONLY conid to avoid Error 478
        // (parameter conflict between conid and other fields)
        Contract orderContract = new Contract();
        orderContract.conid(config.validatedContract.conid());
        orderContract.exchange("SMART");
        
        Order twsOrder = new Order();
        twsOrder.action(config.action);
        twsOrder.totalQuantity(Decimal.get(config.quantity));
        twsOrder.orderType(config.orderType);
        
        if (config.orderType.equals("LMT")) {
            twsOrder.lmtPrice(config.targetPrice);
        }
        
        twsOrder.tif("GTC");
        twsOrder.outsideRth(false);
        
        m_parent.controller().placeOrModifyOrder(orderContract, twsOrder, 
            new ApiController.IOrderHandler() {
                @Override
                public void orderState(OrderState orderState, Order order) {
                    SwingUtilities.invokeLater(() -> {
                        String status = String.format("TWS Order: %s", orderState.getStatus());
                        tableModel.setValueAt(status, rowIndex, 9);
                        m_parent.show("Close order placed in TWS: " + orderState.getStatus());
                    });
                }
                
                @Override
                public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining,
                        double avgFillPrice, int permId, int parentId, double lastFillPrice,
                        int clientId, String whyHeld, double mktCapPrice) {
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setValueAt("TWS: " + status.name(), rowIndex, 9);
                    });
                }
                
                @Override
                public void handle(int errorCode, String errorMsg) {
                    SwingUtilities.invokeLater(() -> {
                        String fullError = String.format("Error %d: %s", errorCode, errorMsg);
                        tableModel.setValueAt(fullError, rowIndex, 9);
                        statusLabel.setText(fullError);
                        statusLabel.setForeground(Color.RED);
                        m_parent.show("Error placing TWS order: " + fullError);
                    });
                }
            });
    }
    
    private void showAlertDialog(PriceMonitor.MonitoredOrder order, double currentPrice, double distance) {
        PriceAlertDialog.showAlert(
            (Frame) SwingUtilities.getWindowAncestor(this),
            order, currentPrice, distance);
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
        statusLabel.setText("Importing from Excel: " + selectedFile.getName());
        statusLabel.setForeground(Color.BLUE);
        
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
                        statusLabel.setText("No valid orders found in Excel file");
                        statusLabel.setForeground(Color.RED);
                        return;
                    }
                    
                    int importedCount = 0;
                    for (ExcelOrderImporter.ImportedOrder order : importResult.orders) {
                        addImportedOrder(order);
                        importedCount++;
                    }
                    
                    statusLabel.setText(String.format("Successfully imported %d orders from Excel. Total: %d",
                        importedCount, configurations.size()));
                    statusLabel.setForeground(new Color(0, 128, 0));
                    
                    JOptionPane.showMessageDialog(PreMarketCloseOrderPanel.this,
                        String.format("Successfully imported %d orders!\n\nYou can now click 'Start Monitoring All' to begin monitoring.",
                            importedCount),
                        "Import Successful", JOptionPane.INFORMATION_MESSAGE);
                    
                } catch (Exception e) {
                    statusLabel.setText("Error importing Excel: " + e.getMessage());
                    statusLabel.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(PreMarketCloseOrderPanel.this,
                        "Error importing Excel file: " + e.getMessage(),
                        "Import Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    
    private void addImportedOrder(ExcelOrderImporter.ImportedOrder order) {
        String expiryDisplay = formatExpiryDisplay(order.expiry);
        
        CloseOrderConfig config = new CloseOrderConfig(order.symbol, order.expiry, order.action,
            order.optionType, order.strike, order.targetPrice, order.alertThreshold,
            order.quantity, order.orderType);
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
        
        int year = Integer.parseInt(expiry.substring(0, 4));
        int month = Integer.parseInt(expiry.substring(4, 6));
        int day = Integer.parseInt(expiry.substring(6, 8));
        
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        return String.format("%02d-%s-%02d", day, months[month-1], year % 100);
    }
    
    @Override
    public void onPriceUpdate(PriceMonitor.MonitoredOrder order, double currentPrice) {
    }
}
