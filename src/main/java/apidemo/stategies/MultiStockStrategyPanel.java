package apidemo.stategies;

import apidemo.util.HtmlButton;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;
import com.ib.client.*;
import com.ib.controller.ApiController;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MultiStockStrategyPanel extends JPanel {
    private final TradingStrategies m_parent;
    private final List<StockEntry> m_stockEntries = new ArrayList<>();
    private final JLabel m_status = new JLabel();
    private HtmlButton m_placeOrderButton;
    private static final int MAX_STOCKS = 4;

    private static class StockEntry {
        final UpperField symbol = new UpperField();
        final UpperField quantity = new UpperField();
        final UpperField price = new UpperField();
        final JCheckBox isSell = new JCheckBox("Sell");
    }

    public MultiStockStrategyPanel(TradingStrategies parent) {
        m_parent = parent;
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Multi Stock Strategy");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Create stock entry fields
        for (int i = 0; i < MAX_STOCKS; i++) {
            StockEntry entry = new StockEntry();
            m_stockEntries.add(entry);

            JPanel stockPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            stockPanel.add(new JLabel("Stock " + (i + 1) + ":"));
            stockPanel.add(entry.symbol);
            stockPanel.add(new JLabel("Quantity:"));
            stockPanel.add(entry.quantity);
            stockPanel.add(new JLabel("Price:"));
            stockPanel.add(entry.price);
            stockPanel.add(entry.isSell);

            mainPanel.add(stockPanel);
        }

        // Add buttons
        VerticalPanel buttonPanel = new VerticalPanel();

        HtmlButton populateDefaults = new HtmlButton("Populate defaults") {
            @Override
            protected void actionPerformed() {
                populateDefaultValues();
            }
        };

        m_placeOrderButton = new HtmlButton("Place orders") {
            @Override
            protected void actionPerformed() {
                placeOrders();
            }
        };
        m_placeOrderButton.setVisible(false);

        buttonPanel.add(populateDefaults);
        buttonPanel.add(m_placeOrderButton);

        mainPanel.add(buttonPanel);
        mainPanel.add(m_status);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void populateDefaultValues() {
        // Example default values
        String[] defaultSymbols = { "AAPL", "MSFT", "GOOGL", "AMZN" };
        String[] defaultQuantities = { "100", "100", "100", "100" };

        for (int i = 0; i < m_stockEntries.size(); i++) {
            StockEntry entry = m_stockEntries.get(i);
            entry.symbol.setText(defaultSymbols[i]);
            entry.quantity.setText(defaultQuantities[i]);
            entry.isSell.setSelected(true);
        }

        m_status.setText("Default values populated");
        m_placeOrderButton.setVisible(true);
    }

    private void placeOrders() {
        m_status.setText("Placing orders...");

        for (StockEntry entry : m_stockEntries) {
            if (entry.symbol.getText().isEmpty())
                continue;

            Contract contract = new Contract();
            contract.symbol(entry.symbol.getText());
            contract.secType("STK");
            contract.exchange("SMART");
            contract.currency("USD");

            Order order = new Order();
            order.orderType("LMT");
            order.lmtPrice(Double.parseDouble(entry.price.getText()));
            order.action(entry.isSell.isSelected() ? "SELL" : "BUY");
            order.totalQuantity(Decimal.get(Integer.parseInt(entry.quantity.getText())));
            order.tif("GTC");

            m_parent.controller().placeOrModifyOrder(contract, order, new ApiController.IOrderHandler() {
                @Override
                public void orderState(OrderState orderState, Order order) {
                    m_status.setText("Order status: " + orderState.getStatus());
                }

                @Override
                public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining,
                        double avgFillPrice, int permId, int parentId, double lastFillPrice,
                        int clientId, String whyHeld, double mktCapPrice) {
                }

                @Override
                public void handle(int errorCode, String errorMsg) {
                    if (errorCode != 0) {
                        m_status.setText("Error: " + errorMsg);
                    }
                }
            });
        }
    }
}