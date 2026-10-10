package com.example.mcnaneyprojectone.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Validates and converts calendar dates between display and storage formats.
 * Creates a formatter per call because SimpleDateFormat is not thread-safe.
 */
public final class DateFormats {

    private DateFormats() {
    }

    /**
     * Creates a strict US-locale formatter for the supplied pattern.
     */
    private static SimpleDateFormat format(String pattern) {
        SimpleDateFormat f =
                new SimpleDateFormat(pattern, Locale.US);

        f.setLenient(false);
        return f;
    }

    /**
     * Parses and round-trips the value to reject invalid dates and noncanonical formatting.
     * Throws IllegalArgumentException when the value does not exactly match the pattern.
     */
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
    /**
     * Returns today's local calendar date in yyyy-MM-dd storage format.
     */
    public static String todayIso() {
        return format("yyyy-MM-dd").format(new Date());
    }

    // Converts 10/04/2026 to 2026-10-04.
    /**
     * Converts a strict MM/dd/yyyy display date to yyyy-MM-dd.
     */
    public static String toIso(String displayDate) {
        return format("yyyy-MM-dd").format(
                parseExactly(displayDate, "MM/dd/yyyy")
        );
    }

    // Converts 2026-10-04 to 10/04/2026.
    /**
     * Converts a strict yyyy-MM-dd storage date to MM/dd/yyyy.
     */
    public static String toDisplay(String isoDate) {
        return format("MM/dd/yyyy").format(
                parseExactly(isoDate, "yyyy-MM-dd")
        );
    }

    /**
     * Returns whether the input exactly represents a valid yyyy-MM-dd date.
     */
    public static boolean isIsoDate(String date) {
        try {
            parseExactly(date, "yyyy-MM-dd");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Handles dates during database migration.
    /**
     * Preserves a valid ISO date or converts a legacy display date for migration.
     */
    public static String normalizeStoredDate(String oldDate) {

        if (isIsoDate(oldDate)) {
            return oldDate;
        }

        return toIso(oldDate);
    }
}