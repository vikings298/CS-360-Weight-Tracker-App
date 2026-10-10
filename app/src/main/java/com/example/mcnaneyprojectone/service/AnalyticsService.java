package com.example.mcnaneyprojectone.service;

import com.example.mcnaneyprojectone.model.WeightEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Calculates progress statistics without database or UI dependencies.
 * Average, trend, change, ETA, and plateau calculations expect newest-first entries.
 * Entry windows count measurements rather than assuming consecutive calendar days.
 */
public class AnalyticsService {

    // Calculate average of the most recent entries.
    // Assumes entries are newest to oldest.
    /**
     * Returns the average of the newest windowSize entries, or -1 for insufficient input.
     * The deque keeps a running sum in O(n) time with O(windowSize) auxiliary space.
     */
    public double calculateMovingAverage(
            List<WeightEntry> entries,
            int windowSize) {

        if (entries == null ||
                windowSize <= 0 ||
                entries.size() < windowSize) {

            return -1;
        }

        Deque<Double> window = new ArrayDeque<>();
        double sum = 0;

        // Start with oldest entries.
        // The final window contains the newest values.
        for (int i = entries.size() - 1; i >= 0; i--) {

            double value = entries.get(i).getWeight();

            window.addLast(value);
            sum += value;

            if (window.size() > windowSize) {
                sum -= window.removeFirst();
            }
        }

        return sum / window.size();
    }

    // Classify the recent weight trend.
    /**
     * Compares the newest three-entry average with the preceding three-entry average.
     * Changes beyond half a pound determine direction; fewer than six entries give no trend.
     */
    public String classifyTrend(List<WeightEntry> entries) {

        if (entries == null || entries.size() < 6) {
            return "Not enough data";
        }

        double recentSum = 0;
        double previousSum = 0;

        for (int i = 0; i < 3; i++) {
            recentSum += entries.get(i).getWeight();
        }

        for (int i = 3; i < 6; i++) {
            previousSum += entries.get(i).getWeight();
        }

        double recentAverage = recentSum / 3;
        double previousAverage = previousSum / 3;

        double difference =
                recentAverage - previousAverage;

        if (difference < -0.5) {
            return "Trending Down";
        } else if (difference > 0.5) {
            return "Trending Up";
        } else {
            return "Stable";
        }
    }

    // Calculate the current daily logging streak.
    /**
     * Counts consecutive logged calendar days ending today or yesterday.
     * A HashSet supports date membership checks without depending on entry ordering.
     */
    public int calculateCurrentStreak(
            List<WeightEntry> entries) {

        if (entries == null || entries.isEmpty()) {
            return 0;
        }

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        Set<String> loggedDates = new HashSet<>();

        for (WeightEntry entry : entries) {
            loggedDates.add(entry.getDate());
        }

        Calendar currentDate = Calendar.getInstance();

        String today =
                dateFormat.format(currentDate.getTime());

        // If today is missing, start at yesterday.
        // An unfinished day doesn't break the streak.
        if (!loggedDates.contains(today)) {
            currentDate.add(
                    Calendar.DAY_OF_YEAR,
                    -1
            );
        }

        int streak = 0;

        while (loggedDates.contains(
                dateFormat.format(currentDate.getTime()))) {

            streak++;

            currentDate.add(
                    Calendar.DAY_OF_YEAR,
                    -1
            );
        }

        return streak;
    }

    // Compare the most recent three weights
    // with the previous three weights.
    /**
     * Returns the newest three-entry average minus the previous three-entry average.
     * Returns NaN when fewer than six entries are available.
     */
    public double calculateWeightChange(
            List<WeightEntry> entries) {

        if (entries == null || entries.size() < 6) {
            return Double.NaN;
        }

        double recentSum = 0;
        double previousSum = 0;

        for (int i = 0; i < 3; i++) {
            recentSum += entries.get(i).getWeight();
        }

        for (int i = 3; i < 6; i++) {
            previousSum += entries.get(i).getWeight();
        }

        double recentAverage = recentSum / 3;
        double previousAverage = previousSum / 3;

        return recentAverage - previousAverage;
    }

    // Estimate days to reach the goal.
    /**
     * Estimates whole days to the target using the oldest and newest supplied measurements.
     * Returns -1 for unusable dates, negligible change, or movement away from the goal.
     * The estimate extrapolates the observed rate rather than predicting future behavior.
     */
    public int calculateGoalETA(
            List<WeightEntry> entries,
            double goalWeight) {

        if (entries == null || entries.size() < 2) {
            return -1;
        }

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        dateFormat.setLenient(false);

        // Use UTC for calculating differences between
        // calendar dates, avoiding daylight-saving issues.
        dateFormat.setTimeZone(
                TimeZone.getTimeZone("UTC")
        );

        try {

            WeightEntry newest = entries.get(0);

            WeightEntry oldest =
                    entries.get(entries.size() - 1);

            Date newestDate =
                    dateFormat.parse(newest.getDate());

            Date oldestDate =
                    dateFormat.parse(oldest.getDate());

            if (newestDate == null || oldestDate == null) {
                return -1;
            }

            long differenceInMillis =
                    newestDate.getTime() -
                            oldestDate.getTime();

            long days =
                    differenceInMillis / 86400000L;

            if (days <= 0) {
                return -1;
            }

            double weightChange =
                    newest.getWeight() -
                            oldest.getWeight();

            double changePerDay =
                    weightChange / days;

            double remaining =
                    goalWeight - newest.getWeight();

            // No meaningful weight trend.
            if (Math.abs(changePerDay) < 0.01) {
                return -1;
            }

            // Weight is moving away from the goal.
            if ((remaining < 0 && changePerDay > 0) ||
                    (remaining > 0 && changePerDay < 0)) {

                return -1;
            }

            double estimatedDays =
                    remaining / changePerDay;

            if (!Double.isFinite(estimatedDays) ||
                    estimatedDays < 0 ||
                    estimatedDays > Integer.MAX_VALUE) {

                return -1;
            }

            return (int) Math.ceil(estimatedDays);

        } catch (ParseException e) {
            return -1;
        }
    }

    // Detect whether recent weights are stable.
    /**
     * Returns whether the newest seven measurements span at most one pound.
     * Returns false when fewer than seven measurements are supplied.
     */
    public boolean detectPlateau(
            List<WeightEntry> entries) {

        if (entries == null || entries.size() < 7) {
            return false;
        }

        double minimum = Double.MAX_VALUE;
        double maximum = -Double.MAX_VALUE;

        for (int i = 0; i < 7; i++) {

            double weight = entries.get(i).getWeight();

            minimum = Math.min(minimum, weight);
            maximum = Math.max(maximum, weight);
        }

        return (maximum - minimum) <= 1.0;
    }
}