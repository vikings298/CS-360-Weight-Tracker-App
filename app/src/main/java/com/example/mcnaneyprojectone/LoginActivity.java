package com.example.mcnaneyprojectone;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.User;
import com.example.mcnaneyprojectone.service.UserService;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView signUpText;

    private UserService userService;
    private DatabaseHelper dbHelper;

    private boolean operationInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signUpText = findViewById(R.id.signUpText);

        dbHelper = new DatabaseHelper(this);
        userService = new UserService(dbHelper);

        loginButton.setOnClickListener(v -> loginUser());
        signUpText.setOnClickListener(v -> createNewUser());
    }

    private void setLoading(boolean loading) {
        operationInProgress = loading;
        loginButton.setEnabled(!loading);
        signUpText.setEnabled(!loading);
    }

    private void loginUser() {

        if (operationInProgress) {
            return;
        }

        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please enter a username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new Thread(() -> {

            int userId = -1;
            boolean failed = false;

            try {
                // One lookup performs password verification
                // and retrieves the authenticated user's ID.
                userId = userService.getUserId(username, password);

            } catch (Exception e) {
                android.util.Log.e(
                        "LoginActivity",
                        "Login failed",
                        e
                );
                failed = true;
            }

            final int authenticatedUserId = userId;
            final boolean operationFailed = failed;

            runOnUiThread(() -> {

                setLoading(false);

                if (operationFailed) {
                    Toast.makeText(this,
                            "Unable to complete login",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (authenticatedUserId != -1) {

                    Toast.makeText(this,
                            "Login Successful",
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(
                            LoginActivity.this,
                            Home.class
                    );

                    intent.putExtra(
                            "USER_ID",
                            authenticatedUserId
                    );

                    startActivity(intent);
                    finish();

                } else {

                    Toast.makeText(this,
                            "Invalid username or password",
                            Toast.LENGTH_SHORT).show();
                }
            });

        }).start();
    }

    private void createNewUser() {

        if (operationInProgress) {
            return;
        }

        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please enter a username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new Thread(() -> {

            boolean usernameExists = false;
            boolean userCreated = false;
            boolean failed = false;

            try {

                usernameExists =
                        userService.usernameExists(username);

                if (!usernameExists) {

                    User user = new User(
                            -1,
                            username,
                            password
                    );

                    userCreated =
                            userService.createUser(user);
                }

            } catch (Exception e) {
                android.util.Log.e(
                        "LoginActivity",
                        "Registration failed",
                        e
                );
                failed = true;
            }

            final boolean exists = usernameExists;
            final boolean created = userCreated;
            final boolean operationFailed = failed;

            runOnUiThread(() -> {

                setLoading(false);

                if (operationFailed) {

                    Toast.makeText(this,
                            "Unable to create account",
                            Toast.LENGTH_SHORT).show();

                } else if (exists) {

                    Toast.makeText(this,
                            "Username already exists",
                            Toast.LENGTH_SHORT).show();

                } else if (created) {

                    Toast.makeText(this,
                            "Account created successfully",
                            Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(this,
                            "Account creation failed",
                            Toast.LENGTH_SHORT).show();
                }
            });

        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}