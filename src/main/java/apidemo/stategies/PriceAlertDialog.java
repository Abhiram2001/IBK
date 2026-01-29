package apidemo.stategies;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PriceAlertDialog extends JDialog {
    
    public PriceAlertDialog(Frame parent, PriceMonitor.MonitoredOrder order, double currentPrice, double distance) {
        super(parent, "Price Alert - Order Placed in TWS", true);
        
        setLayout(new BorderLayout(10, 10));
        
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        messagePanel.setBackground(new Color(255, 248, 220));
        
        JLabel titleLabel = new JLabel("⚠ PRICE ALERT");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(255, 140, 0));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messagePanel.add(titleLabel);
        messagePanel.add(Box.createVerticalStrut(15));
        
        String direction = order.action.equals("BUY") ? "approaching your buy target" : "approaching your sell target";
        JLabel messageLabel = new JLabel(String.format(
            "<html><div style='text-align: center;'>Your order is <b>%s</b>!<br/>" +
            "<b style='color: green;'>Order has been placed in TWS</b></div></html>",
            direction
        ));
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messagePanel.add(messageLabel);
        messagePanel.add(Box.createVerticalStrut(10));
        
        JLabel detailsLabel = new JLabel(String.format(
            "<html><div style='text-align: center;'>" +
            "<b>Symbol:</b> %s %s<br/>" +
            "<b>Strike:</b> %.2f<br/>" +
            "<b>Current Price:</b> $%.2f<br/>" +
            "<b>Target Price:</b> $%.2f<br/>" +
            "<b>Distance:</b> $%.2f (%.2f%%)" +
            "</div></html>",
            order.contract.symbol(),
            order.contract.right().equals("C") ? "CALL" : "PUT",
            order.contract.strike(),
            currentPrice,
            order.targetPrice,
            distance,
            order.getDistancePercentage()
        ));
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        detailsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messagePanel.add(detailsLabel);
        
        add(messagePanel, BorderLayout.CENTER);
        
        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(76, 175, 80));
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, 12));
        okButton.addActionListener(e -> dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(400, 250));
        setLocationRelativeTo(parent);
        
        Toolkit.getDefaultToolkit().beep();
    }
    
    public static void showAlert(Frame parent, PriceMonitor.MonitoredOrder order, 
                                 double currentPrice, double distance) {
        SwingUtilities.invokeLater(() -> 
            new PriceAlertDialog(parent, order, currentPrice, distance).setVisible(true));
    }
}
