
package com.example.mcnaneyprojectone.service;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.User;

import android.database.Cursor;

/**
 * Provides account and authentication operations to the Activities.
 * Maps profile cursors into User models and delegates credential persistence.
 */
public class UserService {

    private final DatabaseHelper dbHelper;

    public UserService(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Retrieves the stored phone number used for goal notifications.
     */
    public String getPhoneNumber(int userId) {
        return dbHelper.getPhoneNumber(userId);
    }

    /**
     * Returns a profile model, or null if no profile exists; closes the cursor.
     */
    public User getAccountInfo(int userId) {

        try (Cursor cursor = dbHelper.getAccountInfo(userId)) {

            if (cursor.moveToFirst()) {

                String firstName = cursor.getString(0);
                String lastName = cursor.getString(1);
                String email = cursor.getString(2);
                String phone = cursor.getString(3);

                return new User(
                        userId,
                        firstName,
                        lastName,
                        email,
                        phone
                );
            }
        }

        return null;
    }

    /**
     * Saves a non-null profile model through the database helper.
     */
    public boolean saveAccountInfo(User user) {

        if (user == null) {
            return false;
        }

        return dbHelper.saveAccountInfo(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone()
        );
    }

    /**
     * Checks credentials through the helper; credential upgrades may occur during login.
     */
    public boolean loginUser(String username, String password) {

        if (username == null || password == null) {
            return false;
        }

        return dbHelper.checkUser(username, password);
    }

    /**
     * Returns the authenticated user ID, or -1 for invalid credentials or null input.
     */
    public int getUserId(String username, String password) {

        if (username == null || password == null) {
            return -1;
        }

        return dbHelper.getUserId(username, password);
    }

    /**
     * Checks a nonblank username against the stored usernames.
     */
    public boolean usernameExists(String username) {

        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        return dbHelper.checkUsername(username);
    }

    /**
     * Delegates account creation; the helper hashes the model's supplied password.
     */
    public boolean createUser(User user) {

        if (user == null ||
                user.getUsername() == null ||
                user.getPassword() == null) {
            return false;
        }

        return dbHelper.addUser(
                user.getUsername(),
                user.getPassword()
        );
    }

    // Validates and processes password changes.
    /**
     * Checks basic password rules before requesting verification and persistence.
     */
    public boolean changePassword(
            int userId,
            String currentPassword,
            String newPassword) {

        if (userId < 0 ||
                currentPassword == null ||
                newPassword == null) {
            return false;
        }

        if (newPassword.length() < 8) {
            return false;
        }

        if (currentPassword.equals(newPassword)) {
            return false;
        }

        return dbHelper.changePassword(
                userId,
                currentPassword,
                newPassword
        );
    }
}
