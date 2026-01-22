package com.ibkr.trading.ui.panel;

import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JMonthChooser;
import com.toedter.calendar.JTextFieldDateEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

/**
 * Utility class for creating common UI components.
 * 
 * @author IBK Trading System
 * @version 2.0
 */
public class UIComponents {
    
    /**
     * Creates a styled button with an action listener.
     * 
     * @param text the button text
     * @param listener the action listener
     * @return configured JButton
     */
    public static JButton createButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        button.setPreferredSize(new Dimension(150, 30));
        return button;
    }
    
    /**
     * Creates a date chooser with standard configuration.
     * 
     * @return configured JDateChooser
     */
    public static JDateChooser createDateChooser() {
        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setPreferredSize(new Dimension(180, 25));
        dateChooser.setDateFormatString("yyyy-MM-dd");
        dateChooser.setCalendar(Calendar.getInstance());

        JTextFieldDateEditor editor = (JTextFieldDateEditor) dateChooser.getDateEditor();
        editor.setBackground(Color.WHITE);

        JCalendar calendar = dateChooser.getJCalendar();
        calendar.setPreferredSize(new Dimension(300, 300));
        calendar.setMinSelectableDate(new Date());
        calendar.setMaxSelectableDate(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000));

        JMonthChooser monthChooser = calendar.getMonthChooser();
        monthChooser.setPreferredSize(new Dimension(120, 25));
        monthChooser.getComboBox().setPreferredSize(new Dimension(120, 25));

        calendar.setWeekOfYearVisible(false);
        calendar.setDecorationBackgroundColor(Color.WHITE);
        calendar.setDecorationBordersVisible(true);

        return dateChooser;
    }
}
