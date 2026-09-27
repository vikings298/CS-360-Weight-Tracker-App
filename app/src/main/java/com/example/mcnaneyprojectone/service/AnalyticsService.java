package com.example.mcnaneyprojectone.service;

import com.example.mcnaneyprojectone.model.WeightEntry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.Calendar;
import java.util.Locale;

public class AnalyticsService {

    /**
     * Calculates the moving average of the most recent weight entries.
     *
     * @param entries weight entries ordered from newest to oldest
     * @param windowSize number of entries included in the moving average
     * @return moving average, or -1 if there is not enough data
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

        for (WeightEntry entry : entries) {

            window.addLast(entry.getWeight());
            sum += entry.getWeight();

            if (window.size() > windowSize) {
                sum -= window.removeFirst();
            }
        }

        return sum / window.size();
    }

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

        double difference = recentAverage - previousAverage;

        if (difference < -0.5) {
            return "Trending Down";
        } else if (difference > 0.5) {
            return "Trending Up";
        } else {
            return "Stable";
        }
    }

    public int calculateCurrentStreak(List<WeightEntry> entries) {

        if (entries == null || entries.isEmpty()) {
            return 0;
        }

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("MM/dd/yyyy", Locale.US);

        Set<String> loggedDates = new HashSet<>();

        for (WeightEntry entry : entries) {
            loggedDates.add(entry.getDate());
        }

        Calendar currentDate = Calendar.getInstance();

        String today =
                dateFormat.format(currentDate.getTime());

        // If today has not been logged yet, begin checking from yesterday.
        // Today's streak is still active until the day is over.
        if (!loggedDates.contains(today)) {
            currentDate.add(Calendar.DAY_OF_YEAR, -1);
        }

        int streak = 0;

        while (loggedDates.contains(
                dateFormat.format(currentDate.getTime()))) {

            streak++;
            currentDate.add(Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }
}