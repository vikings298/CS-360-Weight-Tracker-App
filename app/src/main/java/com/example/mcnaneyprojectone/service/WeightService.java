package com.example.mcnaneyprojectone.service;

import android.database.Cursor;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.WeightEntry;

import java.util.ArrayList;
import java.util.List;

public class WeightService {

    private DatabaseHelper dbHelper;

    public WeightService(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

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

    public boolean deleteWeight(int id) {
        return dbHelper.deleteWeight(id);
    }


}