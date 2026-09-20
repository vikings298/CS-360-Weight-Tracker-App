package com.example.mcnaneyprojectone.service;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.Goal;

public class GoalService {

    private DatabaseHelper dbHelper;

    public GoalService(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public Goal getGoal(int userId) {

        double targetWeight =
                dbHelper.getGoalWeightValue(userId);

        if (targetWeight == -1) {
            return null;
        }

        return new Goal(
                -1,
                userId,
                targetWeight
        );
    }

    public boolean setGoal(Goal goal) {

        if (goal == null || goal.getTargetWeight() <= 0) {
            return false;
        }

        return dbHelper.setGoalWeight(
                goal.getUserId(),
                goal.getTargetWeight()
        );
    }
}