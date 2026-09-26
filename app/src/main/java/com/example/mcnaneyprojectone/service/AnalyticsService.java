package com.example.mcnaneyprojectone.service;

import com.example.mcnaneyprojectone.model.WeightEntry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class AnalyticsService {

    /**
     * Calculates the moving average of the most recent weight entries.
     *
     * @param entries weight entries ordered from oldest to newest
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
}