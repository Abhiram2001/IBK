package com.ibkr.trading.ui.panel;

import com.ibkr.trading.ui.util.HtmlButton;
import com.ibkr.trading.ui.util.UpperField;
import com.ibkr.trading.ui.util.VerticalPanel;
import com.ibkr.trading.config.AppConfig;
import com.ibkr.trading.domain.CalendarSpread;
import com.ibkr.trading.domain.OptionContract;
import com.ibkr.trading.service.*;
import com.ib.client.ComboLeg;
import com.ib.client.ContractDetails;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Panel for executing Calendar Spread strategies using functional approach.
 */
public class CalendarSpreadPanel extends JPanel {
    private final transient MarketDataService marketDataService;
    private final transient OrderService orderService;
    
    private final JLabel symbolLabel = new JLabel();
    private final JDateChooser nearExpiryChooser;
    private final JDateChooser farExpiryChooser;
    private final UpperField spotPriceField = new UpperField();
    private final UpperField callSellOffsetField = new UpperField();
    private final UpperField putSellOffsetField = new UpperField();
    private final UpperField callBuyOffsetField = new UpperField();
    private final UpperField putBuyOffsetField = new UpperField();
    private final UpperField quantityField = new UpperField();
    private final UpperField limitPriceField = new UpperField();
    private final JCheckBox includeCallCheckbox = new JCheckBox("Include Call", true);
    private final JCheckBox includePutCheckbox = new JCheckBox("Include Put", true);
    private final JTextArea statusArea = new JTextArea(3, 40);
    
    private transient ContractDetails callSellDetails;
    private transient ContractDetails putSellDetails;
    private transient ContractDetails callBuyDetails;
    private transient ContractDetails putBuyDetails;

    public CalendarSpreadPanel(ConnectionService connectionService) {
        this.marketDataService = new MarketDataService(connectionService.getController());
        this.orderService = new OrderService(connectionService.getController());
        updateSymbolLabel();
        this.nearExpiryChooser = UIComponents.createDateChooser();
        this.farExpiryChooser = UIComponents.createDateChooser();
        
        initializeUI();
        
        // Add component listener to refresh symbol when panel becomes visible
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                updateSymbolLabel();
            }
        });
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Calendar Spread Strategy");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        VerticalPanel inputPanel = createInputPanel();
        VerticalPanel buttonPanel = createButtonPanel();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(inputPanel);
        mainPanel.add(buttonPanel);

        add(mainPanel, BorderLayout.CENTER);
        
        // Create styled status panel with border and title
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Status",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP
        ));
        
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setBackground(new Color(255, 255, 224)); // Light yellow background
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusArea.setMargin(new Insets(5, 5, 5, 5));
        
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setPreferredSize(new Dimension(500, 60));
        statusScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        statusPanel.add(statusScroll, BorderLayout.CENTER);
        
        add(statusPanel, BorderLayout.SOUTH);
    }

    private VerticalPanel createInputPanel() {
        VerticalPanel panel = new VerticalPanel();
        
        // Display symbol as read-only label
        symbolLabel.setFont(symbolLabel.getFont().deriveFont(Font.BOLD, 14f));
        symbolLabel.setForeground(new Color(0, 100, 0));
        JPanel symbolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        symbolPanel.add(new JLabel("Symbol: "));
        symbolPanel.add(symbolLabel);
        JLabel symbolNote = new JLabel("(Set in Connection Panel)");
        symbolNote.setFont(symbolNote.getFont().deriveFont(Font.ITALIC, 10f));
        symbolNote.setForeground(Color.GRAY);
        symbolPanel.add(symbolNote);
        panel.add("", symbolPanel);
        
        panel.add("Near Expiry", nearExpiryChooser);
        panel.add("Far Expiry", farExpiryChooser);
        panel.add("Spot Price", spotPriceField);
        panel.add("Call Sell Offset", callSellOffsetField);
        panel.add("Call Buy Offset", callBuyOffsetField);
        panel.add("Put Sell Offset", putSellOffsetField);
        panel.add("Put Buy Offset", putBuyOffsetField);
        panel.add("Quantity", quantityField);
        panel.add("Limit Price", limitPriceField);
        
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkboxPanel.add(includeCallCheckbox);
        checkboxPanel.add(includePutCheckbox);
        panel.add("Options", checkboxPanel);
        
        return panel;
    }

    private VerticalPanel createButtonPanel() {
        VerticalPanel panel = new VerticalPanel();
        panel.add(createFetchPriceButton());
        panel.add(createValidateButton());
        panel.add(createPlaceOrderButton());
        return panel;
    }

    private HtmlButton createFetchPriceButton() {
        return new HtmlButton("Fetch Stock Price") {
            @Override
            protected void actionPerformed() {
                updateSymbolLabel(); // Refresh symbol from AppConfig
                String symbol = getCurrentSymbol();
                if (symbol.isEmpty()) {
                    updateStatus("Error: Please set Trading Symbol in Connection Panel first");
                    return;
                }
                updateStatus("Fetching " + symbol + " price...");
                marketDataService.getStockPrice(symbol)
                    .thenAccept(price -> SwingUtilities.invokeLater(() -> {
                        spotPriceField.setText(String.format("%.2f", price));
                        populateDefaults();
                        updateStatus("Price fetched for " + symbol + ": $" + String.format("%.2f", price));
                    }))
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> 
                            updateStatus("Error: " + ex.getMessage()));
                        return null;
                    });
            }
        };
    }

    private HtmlButton createValidateButton() {
        return new HtmlButton("Validate Contracts") {
            @Override
            protected void actionPerformed() {
                updateSymbolLabel(); // Refresh symbol from AppConfig
                try {
                    CalendarSpread strategy = buildStrategy();
                    updateStatus("Validating contracts...");
                    validateContracts(strategy);
                } catch (Exception e) {
                    updateStatus("Error: " + e.getMessage());
                }
            }
        };
    }

    private HtmlButton createPlaceOrderButton() {
        return new HtmlButton("Place Order") {
            @Override
            protected void actionPerformed() {
                if (!areContractsValidated()) {
                    updateStatus("Please validate contracts first");
                    return;
                }
                placeOrder();
            }
        };
    }

    private void populateDefaults() {
        LocalDate today = LocalDate.now();
        nearExpiryChooser.setDate(java.sql.Date.valueOf(today.plusDays(7)));
        farExpiryChooser.setDate(java.sql.Date.valueOf(today.plusDays(14)));
        quantityField.setText("1");
        callSellOffsetField.setText("5");
        putSellOffsetField.setText("5");
        callBuyOffsetField.setText("8");
        putBuyOffsetField.setText("8");
        limitPriceField.setText("0.50");
    }

    private CalendarSpread buildStrategy() {
        return CalendarSpread.builder()
            .symbol(getCurrentSymbol())
            .spotPrice(spotPriceField.getDouble())
            .quantity(quantityField.getInt())
            .nearExpiry(toLocalDate(nearExpiryChooser.getDate()))
            .farExpiry(toLocalDate(farExpiryChooser.getDate()))
            .callSellOffset(callSellOffsetField.getDouble())
            .callBuyOffset(callBuyOffsetField.getDouble())
            .putSellOffset(putSellOffsetField.getDouble())
            .putBuyOffset(putBuyOffsetField.getDouble())
            .includeCall(includeCallCheckbox.isSelected())
            .includePut(includePutCheckbox.isSelected())
            .build();
    }
    
    private String getCurrentSymbol() {
        return AppConfig.getInstance().getCurrentTradingSymbol();
    }
    
    private void updateSymbolLabel() {
        String symbol = getCurrentSymbol();
        symbolLabel.setText(symbol.isEmpty() ? "<Not Set>" : symbol);
    }

    private void validateContracts(CalendarSpread strategy) {
        List<CompletableFuture<ContractDetails>> futures = new ArrayList<>();
        
        // Reset contract details
        callSellDetails = null;
        putSellDetails = null;
        callBuyDetails = null;
        putBuyDetails = null;
        
        // Validate sell call contract
        if (strategy.getSellCallContract() != null) {
            futures.add(orderService.getContractDetails(
                strategy.getSellCallContract().toIBContract())
                .thenApply(list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Expected 1 call sell contract, found " + list.size());
                    }
                    callSellDetails = list.get(0);
                    return callSellDetails;
                }));
        }
        
        // Validate buy call contract
        if (strategy.getBuyCallContract() != null) {
            futures.add(orderService.getContractDetails(
                strategy.getBuyCallContract().toIBContract())
                .thenApply(list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Expected 1 call buy contract, found " + list.size());
                    }
                    callBuyDetails = list.get(0);
                    return callBuyDetails;
                }));
        }
        
        // Validate sell put contract
        if (strategy.getSellPutContract() != null) {
            futures.add(orderService.getContractDetails(
                strategy.getSellPutContract().toIBContract())
                .thenApply(list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Expected 1 put sell contract, found " + list.size());
                    }
                    putSellDetails = list.get(0);
                    return putSellDetails;
                }));
        }
        
        // Validate buy put contract
        if (strategy.getBuyPutContract() != null) {
            futures.add(orderService.getContractDetails(
                strategy.getBuyPutContract().toIBContract())
                .thenApply(list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Expected 1 put buy contract, found " + list.size());
                    }
                    putBuyDetails = list.get(0);
                    return putBuyDetails;
                }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> SwingUtilities.invokeLater(() -> 
                updateStatus("All contracts validated successfully. Ready to place order.")))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> 
                    updateStatus("Validation failed: " + ex.getMessage()));
                return null;
            });
    }

    private void placeOrder() {
        // Verify all required contracts are validated
        if (!areContractsValidated()) {
            updateStatus("Please validate contracts first");
            return;
        }
        
        updateStatus("Creating BAG order...");
        
        // Create combo legs
        List<ComboLeg> legs = new ArrayList<>();
        
        // Add SELL call leg (near-term)
        if (callSellDetails != null) {
            ComboLeg leg = new ComboLeg();
            leg.conid(callSellDetails.contract().conid());
            leg.ratio(1);
            leg.action("SELL");
            leg.exchange("SMART");
            legs.add(leg);
        }
        
        // Add SELL put leg (near-term)
        if (putSellDetails != null) {
            ComboLeg leg = new ComboLeg();
            leg.conid(putSellDetails.contract().conid());
            leg.ratio(1);
            leg.action("SELL");
            leg.exchange("SMART");
            legs.add(leg);
        }
        
        // Add BUY call leg (far-term)
        if (callBuyDetails != null) {
            ComboLeg leg = new ComboLeg();
            leg.conid(callBuyDetails.contract().conid());
            leg.ratio(1);
            leg.action("BUY");
            leg.exchange("SMART");
            legs.add(leg);
        }
        
        // Add BUY put leg (far-term)
        if (putBuyDetails != null) {
            ComboLeg leg = new ComboLeg();
            leg.conid(putBuyDetails.contract().conid());
            leg.ratio(1);
            leg.action("BUY");
            leg.exchange("SMART");
            legs.add(leg);
        }
        
        if (legs.isEmpty()) {
            updateStatus("Error: No legs to place");
            return;
        }
        
        // Place combo order
        double limitPrice = limitPriceField.getDouble();
        int quantity = quantityField.getInt();
        String symbol = getCurrentSymbol();
        
        updateStatus("Placing BAG order with " + legs.size() + " legs...");
        
        orderService.placeComboOrder(symbol, legs, limitPrice, quantity, "BUY",
            status -> SwingUtilities.invokeLater(() -> updateStatus(status)));
    }
    
    private boolean areContractsValidated() {
        boolean includeCall = includeCallCheckbox.isSelected();
        boolean includePut = includePutCheckbox.isSelected();
        
        if (includeCall && (callSellDetails == null || callBuyDetails == null)) {
            return false;
        }
        if (includePut && (putSellDetails == null || putBuyDetails == null)) {
            return false;
        }
        return includeCall || includePut;
    }
    
    private void updateStatus(String message) {
        statusArea.setText(message);
    }

    private LocalDate toLocalDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
