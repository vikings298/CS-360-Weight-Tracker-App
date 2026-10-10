package com.example.mcnaneyprojectone.service;

import android.database.Cursor;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.WeightEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps database rows into WeightEntry models for the Activities and analytics.
 * Read methods return newest-first entries and close their database cursors.
 */
public class WeightService {

    private DatabaseHelper dbHelper;

    public WeightService(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Delegates insertion of a non-null, positive-weight entry.
     * The helper also validates finite values, user IDs, ISO dates, and uniqueness.
     */
    public boolean addWeight(WeightEntry entry){

        if (entry == null || entry.getWeight() <= 0){
            return false;
        }

        return dbHelper.addWeight(
                entry.getUserId(),
                entry.getDate(),
                entry.getWeight()
        );
    }

    /**
     * Returns up to thirty newest measurements as models, or an empty list.
     */
    public List<WeightEntry> getLastThirtyWeights(int userId){

        List<WeightEntry> weights = new ArrayList<>();

        Cursor cursor = dbHelper.getLastThirtyWeights(userId);

        while (cursor.moveToNext()){
            int id = cursor.getInt(0);
            String date = cursor.getString(1);
            double weight = cursor.getDouble(2);

            WeightEntry entry = new WeightEntry(id, userId, date, weight);
            weights.add(entry);
        }

        cursor.close();
        return weights;
    }

    /**
     * Returns the latest measurement, or null when no weight exists.
     * The record ID is -1 because the underlying query does not select it.
     */
    public WeightEntry getMostRecentWeight(int userId) {

        Cursor cursor = dbHelper.getMostRecentWeight(userId);

        WeightEntry entry = null;

        if (cursor.moveToFirst()) {

            String date = cursor.getString(0);
            double weight = cursor.getDouble(1);

            entry = new WeightEntry(
                    -1,
                    userId,
                    date,
                    weight
            );
        }

        cursor.close();

        return entry;
    }

    /**
     * Returns all stored measurements for the user in newest-first order.
     */
    public List<WeightEntry> getAllWeights(int userId) {

        List<WeightEntry> weights = new ArrayList<>();

        Cursor cursor = dbHelper.getAllWeights(userId);

        while (cursor.moveToNext()) {

            int id = cursor.getInt(0);
            String date = cursor.getString(1);
            double weight = cursor.getDouble(2);

            WeightEntry entry =
                    new WeightEntry(id, userId, date, weight);

            weights.add(entry);
        }

        cursor.close();

        return weights;
    }

    /**
     * Updates the existing measurement identified by the model's user ID and date.
     */
    public boolean updateWeight(WeightEntry entry) {

        if (entry == null || entry.getWeight() <= 0) {
            return false;
        }

        return dbHelper.updateWeightByDate(
                entry.getUserId(),
                entry.getDate(),
                entry.getWeight()
        );
    }

    /**
     * Deletes the measurement identified by its database record ID.
     */
    public boolean deleteWeight(int id) {
        return dbHelper.deleteWeight(id);
    }


}