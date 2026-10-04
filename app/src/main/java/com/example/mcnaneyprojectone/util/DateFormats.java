package com.example.mcnaneyprojectone.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateFormats {

    private DateFormats() {
    }

    private static SimpleDateFormat format(String pattern) {
        SimpleDateFormat f =
                new SimpleDateFormat(pattern, Locale.US);

        f.setLenient(false);
        return f;
    }

    private static Date parseExactly(
            String value,
            String pattern) {

        if (value == null) {
            throw new IllegalArgumentException("Missing date");
        }

        try {
            SimpleDateFormat f = format(pattern);
            Date date = f.parse(value);

            if (date == null ||
                    !f.format(date).equals(value)) {

                throw new IllegalArgumentException(
                        "Invalid date: " + value
                );
            }

            return date;

        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Invalid date: " + value,
                    e
            );
        }
    }

    // Current date for database storage.
    public static String todayIso() {
        return format("yyyy-MM-dd").format(new Date());
    }

    // Converts 10/04/2026 to 2026-10-04.
    public static String toIso(String displayDate) {
        return format("yyyy-MM-dd").format(
                parseExactly(displayDate, "MM/dd/yyyy")
        );
    }

    // Converts 2026-10-04 to 10/04/2026.
    public static String toDisplay(String isoDate) {
        return format("MM/dd/yyyy").format(
                parseExactly(isoDate, "yyyy-MM-dd")
        );
    }

    public static boolean isIsoDate(String date) {
        try {
            parseExactly(date, "yyyy-MM-dd");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Handles dates during database migration.
    public static String normalizeStoredDate(String oldDate) {

        if (isIsoDate(oldDate)) {
            return oldDate;
        }

        return toIso(oldDate);
    }
}