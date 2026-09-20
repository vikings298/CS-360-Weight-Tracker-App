package com.example.mcnaneyprojectone.service;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import android.database.Cursor;

import com.example.mcnaneyprojectone.model.User;

public class UserService {

    private DatabaseHelper dbHelper;

    public UserService(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public String getPhoneNumber(int userId) {
        return dbHelper.getPhoneNumber(userId);
    }

    public User getAccountInfo(int userId) {

        Cursor cursor = dbHelper.getAccountInfo(userId);

        User user = null;

        if (cursor.moveToFirst()) {

            String firstName = cursor.getString(0);
            String lastName = cursor.getString(1);
            String email = cursor.getString(2);
            String phone = cursor.getString(3);

            user = new User(
                    userId,
                    firstName,
                    lastName,
                    email,
                    phone
            );
        }

        cursor.close();

        return user;
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

        return dbHelper.getUserId(username, password);
    }

    public boolean usernameExists(String username) {

        return dbHelper.checkUsername(username);
    }

    public boolean createUser(User user) {

        if (user == null) {
            return false;
        }

        return dbHelper.addUser(
                user.getUsername(),
                user.getPassword()
        );
    }


}