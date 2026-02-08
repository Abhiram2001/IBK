package com.ibkr.trading.ui.panel;

import apidemo.stategies.CalendarSpreadStrategyPanel;
import com.ibkr.trading.ui.util.HtmlButton;
import com.ibkr.trading.ui.util.UpperField;
import com.ibkr.trading.ui.util.VerticalPanel;
import com.ib.client.ComboLeg;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ibkr.trading.config.AppConfig;
import com.ibkr.trading.domain.OptionContract;
import com.ibkr.trading.domain.StrangleStrategy;
import com.ibkr.trading.service.*;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for executing Strangle option strategies.
 * A strangle involves selling both a call and put option at different strike prices,
 * both out-of-the-money. This strategy profits when the underlying stays within a range.
 */
public class StranglePanel extends JPanel {
    private final transient MarketDataService marketDataService;
    private final transient OrderService orderService;
    
    private final JLabel symbolLabel = new JLabel();
    private final JDateChooser expiryChooser;
    private final UpperField spotPriceField = new UpperField();
    private final UpperField callOffsetField = new UpperField();
    private final UpperField putOffsetField = new UpperField();
    private final UpperField quantityField = new UpperField();
    private final UpperField limitPriceField = new UpperField();
    private final JTextArea statusArea = new JTextArea(3, 40);
    
    private HtmlButton placeOrderButton;
    private transient ContractDetails callDetails;
    private transient ContractDetails putDetails;
    private int contractsValidated = 0;
    private static final int TOTAL_CONTRACTS = 2;

    public StranglePanel(ConnectionService connectionService) {
        this.marketDataService = new MarketDataService(connectionService.getController());
        this.orderService = new OrderService(connectionService.getController());
        updateSymbolLabel();
        this.expiryChooser = UIComponents.createDateChooser();
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

        JLabel titleLabel = new JLabel("Strangle Strategy");
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
        
        panel.add("Expiry Date", expiryChooser);
        panel.add("Spot Price", spotPriceField);
        panel.add("Call Strike Offset", callOffsetField);
        panel.add("Put Strike Offset", putOffsetField);
        panel.add("Quantity", quantityField);
        panel.add("Min Credit (total)", limitPriceField);
        return panel;
    }

    private VerticalPanel createButtonPanel() {
        VerticalPanel panel = new VerticalPanel();
        panel.add(createFetchPriceButton());
        panel.add(createValidateButton());
        
        placeOrderButton = new HtmlButton("Place Order") {
            @Override
            protected void actionPerformed() {
                updateStatus("Placing orders...");
                placeStrangleOrder();
            }
        };
        placeOrderButton.setVisible(false);
        panel.add(placeOrderButton);
        
        return panel;
    }

    private HtmlButton createFetchPriceButton() {
        return new HtmlButton("Fetch Stock Price & Populate Defaults") {
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
                        spotPriceField.setText(String.format("%.2f", customRound(price)));
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

  public double customRound(double value) {
    return CalendarSpreadStrategyPanel.customRound(value);
  }

    private HtmlButton createValidateButton() {
        return new HtmlButton("Validate Contracts") {
            @Override
            protected void actionPerformed() {
                updateSymbolLabel(); // Refresh symbol from AppConfig
                try {
                    StrangleStrategy strategy = buildStrategy();
                    strategy.validate(); // Validate strikes are different
                    updateStatus("Validating contracts...");
                    validateContracts(strategy);
                } catch (Exception e) {
                    updateStatus("Error: " + e.getMessage());
                }
            }
        };
    }

    private void populateDefaults() {
        LocalDate today = LocalDate.now();
        expiryChooser.setDate(java.sql.Date.valueOf(today.plusDays(8)));
        quantityField.setText("1");
        callOffsetField.setText("5");
        putOffsetField.setText("5");
        limitPriceField.setText("10");
    }

    private StrangleStrategy buildStrategy() {
        StrangleStrategy strategy = StrangleStrategy.builder()
            .symbol(getCurrentSymbol())
            .spotPrice(spotPriceField.getDouble())
            .quantity(quantityField.getInt())
            .expiry(toLocalDate(expiryChooser.getDate()))
            .callStrikeOffset(callOffsetField.getDouble())
            .putStrikeOffset(putOffsetField.getDouble())
            .build();
        
        // Show calculated strikes for verification
        double callStrike = Math.round(strategy.getSpotPrice() + callOffsetField.getDouble());
        double putStrike = Math.round(strategy.getSpotPrice() - putOffsetField.getDouble());
        updateStatus(String.format("Strategy built: Call Strike=$%.0f, Put Strike=$%.0f", 
            callStrike, putStrike));
        
        return strategy;
    }
    
    private String getCurrentSymbol() {
        return AppConfig.getInstance().getCurrentTradingSymbol();
    }
    
    public void updateSymbolLabel() {
        String symbol = getCurrentSymbol();
        symbolLabel.setText(symbol.isEmpty() ? "<Not Set>" : symbol);
    }

    private void validateContracts(StrangleStrategy strategy) {
        contractsValidated = 0;
        placeOrderButton.setVisible(false);
        callDetails = null;
        putDetails = null;
        
        OptionContract callOption = strategy.getCallContract();
        OptionContract putOption = strategy.getPutContract();
        
        Contract callContract = callOption.toIBContract();
        Contract putContract = putOption.toIBContract();
        
        // Log what we're requesting for debugging
        updateStatus(String.format("Requesting contracts: Call Strike=%.0f, Put Strike=%.0f", 
            callContract.strike(), putContract.strike()));
        
        // Validate call contract and store details
        orderService.getContractDetails(callContract)
            .thenAccept(details -> SwingUtilities.invokeLater(() -> {
                if (details.size() != 1) {
                    updateStatus("ERROR: Expected 1 call contract, found " + details.size());
                    return;
                }
                callDetails = details.get(0);
                updateStatus(String.format("Call validated: Strike=%.0f, ConID=%d", 
                    callDetails.contract().strike(), callDetails.contract().conid()));
                contractsValidated++;
                checkValidationComplete();
            }))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> 
                    updateStatus("Call contract error: " + ex.getMessage()));
                return null;
            });
        
        // Validate put contract and store details
        orderService.getContractDetails(putContract)
            .thenAccept(details -> SwingUtilities.invokeLater(() -> {
                if (details.size() != 1) {
                    updateStatus("ERROR: Expected 1 put contract, found " + details.size());
                    return;
                }
                putDetails = details.get(0);
                updateStatus(String.format("Put validated: Strike=%.0f, ConID=%d", 
                    putDetails.contract().strike(), putDetails.contract().conid()));
                contractsValidated++;
                checkValidationComplete();
            }))
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> 
                    updateStatus("Put contract error: " + ex.getMessage()));
                return null;
            });
    }
    
    private void checkValidationComplete() {
        if (contractsValidated == TOTAL_CONTRACTS) {
            // Final validation: ensure strikes are actually different
            double callStrike = callDetails.contract().strike();
            double putStrike = putDetails.contract().strike();
            
            if (Math.abs(callStrike - putStrike) < 0.01) {
                updateStatus(String.format("ERROR: Riskless combination detected! " +
                    "Call strike (%.0f) equals Put strike (%.0f). " +
                    "Increase offsets or adjust spot price.", callStrike, putStrike));
                placeOrderButton.setVisible(false);
                return;
            }
            
            if (callStrike <= putStrike) {
                updateStatus(String.format("ERROR: Invalid strangle! " +
                    "Call strike (%.0f) must be higher than Put strike (%.0f).", 
                    callStrike, putStrike));
                placeOrderButton.setVisible(false);
                return;
            }
            
            placeOrderButton.setVisible(true);
            updateStatus(String.format("Contracts validated: Call=$%.0f, Put=$%.0f. Ready to place order.", 
                callStrike, putStrike));
        }
    }
    
    private void placeStrangleOrder() {
        // Verify contracts are validated
        if (callDetails == null || putDetails == null) {
            updateStatus("ERROR: Contracts not validated");
            return;
        }
        
        int quantity = quantityField.getInt();
        double limitPrice = limitPriceField.getDouble();

        // Create combo legs for BAG order
        List<ComboLeg> legs = new ArrayList<>();
        
        // SELL call leg (opening position)
        ComboLeg callLeg = new ComboLeg();
        callLeg.conid(callDetails.contract().conid());
        callLeg.ratio(1);
        callLeg.action("BUY");
        callLeg.exchange("SMART");
        legs.add(callLeg);
        
        // SELL put leg (opening position)
        ComboLeg putLeg = new ComboLeg();
        putLeg.conid(putDetails.contract().conid());
        putLeg.ratio(1);
        putLeg.action("BUY");
        putLeg.exchange("SMART");
        legs.add(putLeg);
        
        String symbol = getCurrentSymbol();
        updateStatus("Placing combo order with 2 legs...");
        
        String account = AppConfig.getInstance().getCurrentTradingAccount();
        // Place as a single BAG order
        orderService.placeComboOrder(symbol, legs, limitPrice, quantity, "SELL", account,
            status -> SwingUtilities.invokeLater(() -> {
                updateStatus(status);
                if (status.contains("Order Status: Submitted") || 
                    status.contains("Order Status: PreSubmitted")) {
                    placeOrderButton.setVisible(false);
                }
            }));
    }
    
    private void updateStatus(String message) {
        statusArea.setText(message);
    }

    private LocalDate toLocalDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
