package com.example.mcnaneyprojectone.service;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.Goal;

/**
 * Adapts stored goal values into Goal models and delegates goal updates.
 */
public class GoalService {

    private DatabaseHelper dbHelper;

    public GoalService(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Returns the user's target as a Goal, or null if no stored goal exists.
     * The record ID is -1 because this query retrieves only the target value.
     */
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

    /**
     * Rejects a missing or nonpositive goal and delegates persistence validation.
     */
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