package org.example;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Main {
    public static void main(String[] args) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2025, Calendar.FEBRUARY, 3);
        Date today = calendar.getTime();

        // Define the date format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        // Print today's date
        String todayFormatted = sdf.format(today);
        System.out.println("Today's Date: " + todayFormatted);

        // Add 7 days to today's date
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        String dateAfter7Days = sdf.format(calendar.getTime());
        System.out.println("Date after 7 days: " + dateAfter7Days);

        // Reset calendar to today's date and add 14 days
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_MONTH, 10);
        String dateAfter14Days = sdf.format(calendar.getTime());
        System.out.println("Date after 14 days: " + dateAfter14Days);
    }
}