package com.example.mcnaneyprojectone;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WeightTracker.db";
    private static final int DATABASE_VERSION = 8;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Users Table
        db.execSQL(
                "CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE, " +
                "password TEXT)");

        // Weights Table
        db.execSQL(
                "CREATE TABLE weights (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "date TEXT, " +
                "weight REAL, " +
                "UNIQUE(user_id, date))");

        //Goal Weights Table
        db.execSQL(
                "CREATE TABLE goals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "goal_weight REAL)");

        //Account Information Table
        db.execSQL(
                "CREATE TABLE account_info (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "first_name TEXT, " +
                "last_name TEXT, " +
                "email TEXT, " +
                "phone TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS weights");
        db.execSQL("DROP TABLE IF EXISTS goals");
        db.execSQL("DROP TABLE IF EXISTS account_info");
        onCreate(db);
    }

    //Adds a user to the database.
    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("username", username);
        values.put("password", password);

        long result = db.insert("users", null, values);
        return result != -1;
    }

    //Checks to see if user exists in database and checks password against that user.
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE username=? AND password=?",
                new String[]{username, password}
        );

        boolean userExists = cursor.getCount() > 0;
        cursor.close();

        return userExists;
    }

    //Checks to see if a user exists provided username.
    public boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE username=?",
                new String[]{username}
        );

        boolean usernameExists = cursor.getCount() > 0;
        cursor.close();

        return usernameExists;
    }

    public int getUserId(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM users WHERE username=? AND password=?",
                new String[]{username, password}
        );

        int userId = -1;

        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0);
        }

        cursor.close();

        return userId;
    }

    //CRUD methods for daily weights

    //Create - Adds a weight entry to database
    public boolean addWeight(int userId, String date, double weight) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("date", date);
        values.put("weight", weight);

        long result = db.insert("weights", null, values);

        return result != -1;
    }

    //Read - Reads all weights in database
    public Cursor getAllWeights(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id, date, weight FROM weights WHERE user_id=? ORDER BY date DESC",
                new String[]{String.valueOf(userId)}
        );
    }

    //Update - Updates a selected weight
    public boolean updateWeight(int id, String date, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("date", date);
        values.put("weight", weight);

        int rowsUpdated = db.update(
                "weights",
                values,
                "id=?",
                new String[]{String.valueOf(id)}
        );

        return rowsUpdated > 0;
    }

    //Delete - Removes a selected weight from database
    public boolean deleteWeight(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        int rowsDeleted = db.delete(
                "weights",
                "id=?",
                new String[]{String.valueOf(id)}
        );

        return rowsDeleted > 0;
    }
    public boolean updateWeightByDate(int userId, String date, double weight) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("weight", weight);

        int rowsUpdated = db.update(
                "weights",
                values,
                "user_id=? AND date=?",
                new String[]{String.valueOf(userId), date}
        );

        return rowsUpdated > 0;
    }


    public Cursor getLastThirtyWeights(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id, date, weight FROM weights " +
                        "WHERE user_id=? " +
                        "ORDER BY substr(date, 7, 4) DESC, " +
                        "substr(date, 1, 2) DESC, " +
                        "substr(date, 4, 2) DESC LIMIT 30",
                new String[]{String.valueOf(userId)}
        );
    }

    public Cursor getMostRecentWeight(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT date, weight FROM weights " +
                        "WHERE user_id=? " +
                        "ORDER BY substr(date, 7, 4) DESC, " +
                        "substr(date, 1, 2) DESC, " +
                        "substr(date, 4, 2) DESC " +
                        "LIMIT 1",
                new String[]{String.valueOf(userId)}
        );
    }


    public boolean setGoalWeight(int userId, double goalWeight) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete("goals", "user_id=?", new String[]{String.valueOf(userId)});

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("goal_weight", goalWeight);

        long result = db.insert("goals", null, values);
        return result != -1;
    }

    // Saves account information
    public boolean saveAccountInfo(int userId, String firstName, String lastName, String email, String phone) {

        SQLiteDatabase db = this.getWritableDatabase();

        db.delete("account_info", "user_id=?", new String[]{String.valueOf(userId)});

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("first_name", firstName);
        values.put("last_name", lastName);
        values.put("email", email);
        values.put("phone", phone);

        long result = db.insert("account_info", null, values);

        return result != -1;
    }


    // Loads account information
    public Cursor getAccountInfo(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT first_name, last_name, email, phone FROM account_info WHERE user_id=? LIMIT 1",
                new String[]{String.valueOf(userId)}
        );
    }

    public double getGoalWeightValue(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT goal_weight FROM goals WHERE user_id=? LIMIT 1",
                new String[]{String.valueOf(userId)}
        );

        double goalWeight = -1;

        if (cursor.moveToFirst()) {
            goalWeight = cursor.getDouble(0);
        }

        cursor.close();

        return goalWeight;
    }

    public String getPhoneNumber(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT phone FROM account_info WHERE user_id=? LIMIT 1",
                new String[]{String.valueOf(userId)}
        );

        String phoneNumber = "";

        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(0);
        }

        cursor.close();

        return phoneNumber;
    }

    public Cursor getGoalWeight(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT goal_weight FROM goals WHERE user_id=? LIMIT 1",
                new String[]{String.valueOf(userId)}
        );
    }


}

