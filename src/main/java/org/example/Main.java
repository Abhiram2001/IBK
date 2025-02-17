package org.example;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

public class Main {
    public static void main(String[] args) {
        // Get today's date dynamically
        LocalDate todayDate = LocalDate.now();

        // Create Calendar instance and set it to today's date
        Calendar calendar = Calendar.getInstance();
        calendar.set(todayDate.getYear(), todayDate.getMonthValue() - 1, todayDate.getDayOfMonth());
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
        calendar.add(Calendar.DAY_OF_MONTH, 14);
        String dateAfter14Days = sdf.format(calendar.getTime());
        System.out.println("Date after 14 days: " + dateAfter14Days);
    }
}