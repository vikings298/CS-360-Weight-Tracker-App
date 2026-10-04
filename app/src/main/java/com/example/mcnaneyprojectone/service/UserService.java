
package com.example.mcnaneyprojectone.service;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.User;

import android.database.Cursor;

public class UserService {

    private final DatabaseHelper dbHelper;

    public UserService(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public String getPhoneNumber(int userId) {
        return dbHelper.getPhoneNumber(userId);
    }

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

    public boolean loginUser(String username, String password) {

        if (username == null || password == null) {
            return false;
        }

        return dbHelper.checkUser(username, password);
    }

    public int getUserId(String username, String password) {

        if (username == null || password == null) {
            return -1;
        }

        return dbHelper.getUserId(username, password);
    }

    public boolean usernameExists(String username) {

        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        return dbHelper.checkUsername(username);
    }

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
