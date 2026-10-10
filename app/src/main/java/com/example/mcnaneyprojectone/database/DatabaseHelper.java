package com.example.mcnaneyprojectone.database;

import com.example.mcnaneyprojectone.security.PasswordUtils;
import com.example.mcnaneyprojectone.util.DateFormats;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteException;
import android.content.ContentValues;
import android.database.Cursor;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the SQLite schema, version upgrades, and persistence operations.
 * Weight dates use yyyy-MM-dd so text sorting matches chronological order.
 * Authentication supports older credentials and upgrades them after verification.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WeightTracker.db";
    private static final int DATABASE_VERSION = 11;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Creates the tables for a new installation, including one weight per user and date.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        // Users
        db.execSQL(
                "CREATE TABLE users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT UNIQUE, " +
                        "password TEXT)"
        );

        // Weight entries
        db.execSQL(
                "CREATE TABLE weights (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER, " +
                        "date TEXT, " +
                        "weight REAL, " +
                        "UNIQUE(user_id, date))"
        );

        // Goal weights
        db.execSQL(
                "CREATE TABLE goals (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER, " +
                        "goal_weight REAL)"
        );

        // Account information
        db.execSQL(
                "CREATE TABLE account_info (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER, " +
                        "first_name TEXT, " +
                        "last_name TEXT, " +
                        "email TEXT, " +
                        "phone TEXT)"
        );
    }

    /**
     * Applies versioned schema changes without deleting existing user data.
     */
    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        if (oldVersion < 10) {

            // The UNIQUE(user_id, date) constraint already
            // provides an index starting with user_id.
            db.execSQL(
                    "DROP INDEX IF EXISTS idx_weights_user"
            );
        }

        if (oldVersion < 11) {
            migrateDatesToIso(db);
        }
    }

    // Convert existing dates to ISO format.
    // SQLiteOpenHelper executes upgrades in a transaction.
    // Any failure rolls back the migration.
    /**
     * Validates legacy dates before updating them within the framework upgrade transaction.
     * Invalid dates or failed updates abort the migration.
     */
    private void migrateDatesToIso(SQLiteDatabase db) {

        List<Long> ids = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        // Read and validate existing records first.
        try (Cursor cursor = db.rawQuery(
                "SELECT id, date FROM weights",
                null)) {

            while (cursor.moveToNext()) {

                long id = cursor.getLong(0);
                String oldDate = cursor.getString(1);

                try {
                    String newDate =
                            DateFormats.normalizeStoredDate(oldDate);

                    if (!newDate.equals(oldDate)) {
                        ids.add(id);
                        dates.add(newDate);
                    }

                } catch (IllegalArgumentException e) {

                    throw new SQLiteException(
                            "Cannot migrate weight id=" + id +
                                    ": invalid date " + oldDate
                    );
                }
            }
        }

        // Apply validated changes.
        for (int i = 0; i < ids.size(); i++) {

            ContentValues values = new ContentValues();
            values.put("date", dates.get(i));

            long id = ids.get(i);

            int updated = db.update(
                    "weights",
                    values,
                    "id=?",
                    new String[]{String.valueOf(id)}
            );

            if (updated != 1) {
                throw new SQLiteException(
                        "Could not migrate weight id=" + id
                );
            }
        }
    }

    // -------------------------------------------------
    // USER AUTHENTICATION
    // -------------------------------------------------

    /**
     * Hashes the supplied password and inserts a user; returns false if insertion fails.
     */
    public boolean addUser(
            String username,
            String password) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put(
                "password",
                PasswordUtils.hashPassword(password)
        );

        long result = db.insert(
                "users",
                null,
                values
        );

        return result != -1;
    }

    /**
     * Returns whether authentication succeeds, including any required credential upgrade.
     */
    public boolean checkUser(
            String username,
            String password) {

        return getUserId(username, password) != -1;
    }

    /**
     * Returns whether the exact username is already stored.
     */
    public boolean checkUsername(String username) {

        SQLiteDatabase db = this.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT id FROM users WHERE username=?",
                new String[]{username})) {

            return cursor.moveToFirst();
        }
    }


    /**
     * Verifies credentials and returns the user ID, or -1 on authentication failure.
     * Successfully verified older credentials are replaced with a new Argon2id hash.
     */
    public int getUserId(String username, String password) {

        if (username == null || password == null) {
            return -1;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT id, password FROM users WHERE username=?",
                new String[]{username})) {

            if (!cursor.moveToFirst()) {
                return -1;
            }

            int userId = cursor.getInt(0);
            String storedPassword = cursor.getString(1);

            if (storedPassword == null) {
                return -1;
            }

            boolean verified;

            if (storedPassword.startsWith("$argon2id$") ||
                    storedPassword.contains(":")) {

                verified = PasswordUtils.verifyPassword(
                        password,
                        storedPassword
                );

            } else {

                // Compatibility with old plaintext accounts.
                verified = MessageDigest.isEqual(
                        password.getBytes(StandardCharsets.UTF_8),
                        storedPassword.getBytes(StandardCharsets.UTF_8)
                );
            }

            if (!verified) {
                return -1;
            }

            // Convert older credentials after a successful
            // verification, preserving the user's password.
            if (PasswordUtils.needsUpgrade(storedPassword)) {

                String upgradedHash =
                        PasswordUtils.hashPassword(password);

                ContentValues values = new ContentValues();
                values.put("password", upgradedHash);

                // Match the old credential too, so a concurrent change is not overwritten.
                int updated = db.update(
                        "users",
                        values,
                        "id=? AND password=?",
                        new String[]{
                                String.valueOf(userId),
                                storedPassword
                        }
                );

                if (updated != 1) {
                    return -1;
                }
            }

            return userId;
        }
    }


    // -------------------------------------------------
    // PASSWORD CHANGES
    // -------------------------------------------------

    /**
     * Verifies the current password before saving a different password of at least eight characters.
     * Returns true only when the matching credential record is updated.
     */
    public boolean changePassword(
            int userId,
            String currentPassword,
            String newPassword) {

        if (userId < 0 ||
                currentPassword == null ||
                newPassword == null ||
                newPassword.length() < 8 ||
                currentPassword.equals(newPassword)) {

            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        String storedPassword;

        try (Cursor cursor = db.rawQuery(
                "SELECT password FROM users WHERE id=?",
                new String[]{String.valueOf(userId)})) {

            if (!cursor.moveToFirst()) {
                return false;
            }

            storedPassword = cursor.getString(0);
        }

        if (storedPassword == null) {
            return false;
        }

        boolean verified;

        if (storedPassword.startsWith("$argon2id$") ||
                storedPassword.contains(":")) {

            verified = PasswordUtils.verifyPassword(
                    currentPassword,
                    storedPassword
            );

        } else {

            verified = MessageDigest.isEqual(
                    currentPassword.getBytes(
                            StandardCharsets.UTF_8
                    ),
                    storedPassword.getBytes(
                            StandardCharsets.UTF_8
                    )
            );
        }

        if (!verified) {
            return false;
        }

        ContentValues values = new ContentValues();

        values.put(
                "password",
                PasswordUtils.hashPassword(newPassword)
        );

        int updated = db.update(
                "users",
                values,
                "id=? AND password=?",
                new String[]{
                        String.valueOf(userId),
                        storedPassword
                }
        );

        return updated == 1;
    }

    // -------------------------------------------------
    // WEIGHT ENTRIES
    // -------------------------------------------------

    /**
     * Inserts a positive finite weight with an ISO date; duplicate user/date pairs fail.
     */
    public boolean addWeight(
            int userId,
            String date,
            double weight) {

        if (userId < 0 ||
                !Double.isFinite(weight) ||
                weight <= 0 ||
                !DateFormats.isIsoDate(date)) {

            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("user_id", userId);
        values.put("date", date);
        values.put("weight", weight);

        long result = db.insert(
                "weights",
                null,
                values
        );

        return result != -1;
    }

    // All weights, newest first.
    /**
     * Returns this user's complete newest-first history. The caller must close the cursor.
     */
    public Cursor getAllWeights(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id, date, weight FROM weights " +
                        "WHERE user_id=? " +
                        "ORDER BY date DESC",
                new String[]{String.valueOf(userId)}
        );
    }

    // Delete a weight record.
    /**
     * Deletes by record ID and returns whether a row was removed.
     */
    public boolean deleteWeight(int id) {

        SQLiteDatabase db = this.getWritableDatabase();

        int rowsDeleted = db.delete(
                "weights",
                "id=?",
                new String[]{String.valueOf(id)}
        );

        return rowsDeleted > 0;
    }

    // Update the weight associated with a date.
    /**
     * Updates a validated weight for the specified user/date pair; returns whether a row changed.
     */
    public boolean updateWeightByDate(
            int userId,
            String date,
            double weight) {

        if (userId < 0 ||
                !Double.isFinite(weight) ||
                weight <= 0 ||
                !DateFormats.isIsoDate(date)) {

            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("weight", weight);

        int rowsUpdated = db.update(
                "weights",
                values,
                "user_id=? AND date=?",
                new String[]{
                        String.valueOf(userId),
                        date
                }
        );

        return rowsUpdated > 0;
    }

    // Most recent thirty entries.
    /**
     * Returns up to thirty newest entries. The caller must close the cursor.
     */
    public Cursor getLastThirtyWeights(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id, date, weight FROM weights " +
                        "WHERE user_id=? " +
                        "ORDER BY date DESC LIMIT 30",
                new String[]{String.valueOf(userId)}
        );
    }

    // Latest weight entry.
    /**
     * Returns a cursor containing at most one latest entry. The caller must close it.
     */
    public Cursor getMostRecentWeight(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT date, weight FROM weights " +
                        "WHERE user_id=? " +
                        "ORDER BY date DESC LIMIT 1",
                new String[]{String.valueOf(userId)}
        );
    }

    // -------------------------------------------------
    // GOAL WEIGHTS
    // -------------------------------------------------

    /**
     * Updates existing goal records for the user, inserting only when none were updated.
     */
    public boolean setGoalWeight(
            int userId,
            double goalWeight) {

        if (userId < 0 ||
                !Double.isFinite(goalWeight) ||
                goalWeight <= 0) {

            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("goal_weight", goalWeight);

        int rowsUpdated = db.update(
                "goals",
                values,
                "user_id=?",
                new String[]{String.valueOf(userId)}
        );

        if (rowsUpdated > 0) {
            return true;
        }

        values.put("user_id", userId);

        return db.insert(
                "goals",
                null,
                values
        ) != -1;
    }

    /**
     * Returns the stored target weight, or -1 when no goal record exists.
     */
    public double getGoalWeightValue(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        double goalWeight = -1;

        try (Cursor cursor = db.rawQuery(
                "SELECT goal_weight FROM goals " +
                        "WHERE user_id=? LIMIT 1",
                new String[]{String.valueOf(userId)})) {

            if (cursor.moveToFirst()) {
                goalWeight = cursor.getDouble(0);
            }
        }

        return goalWeight;
    }

    // -------------------------------------------------
    // ACCOUNT INFORMATION
    // -------------------------------------------------

    /**
     * Updates the user's existing profile, inserting only when none was updated.
     */
    public boolean saveAccountInfo(
            int userId,
            String firstName,
            String lastName,
            String email,
            String phone) {

        if (userId < 0) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("first_name", firstName);
        values.put("last_name", lastName);
        values.put("email", email);
        values.put("phone", phone);

        int rowsUpdated = db.update(
                "account_info",
                values,
                "user_id=?",
                new String[]{String.valueOf(userId)}
        );

        if (rowsUpdated > 0) {
            return true;
        }

        values.put("user_id", userId);

        return db.insert(
                "account_info",
                null,
                values
        ) != -1;
    }

    /**
     * Returns a cursor with at most one profile row. The caller must close it.
     */
    public Cursor getAccountInfo(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT first_name, last_name, email, phone " +
                        "FROM account_info " +
                        "WHERE user_id=? LIMIT 1",
                new String[]{String.valueOf(userId)}
        );
    }

    /**
     * Returns the stored phone value, or an empty string when no profile row exists.
     */
    public String getPhoneNumber(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        String phoneNumber = "";

        try (Cursor cursor = db.rawQuery(
                "SELECT phone FROM account_info " +
                        "WHERE user_id=? LIMIT 1",
                new String[]{String.valueOf(userId)})) {

            if (cursor.moveToFirst()) {
                phoneNumber = cursor.getString(0);
            }
        }

        return phoneNumber;
    }
}

