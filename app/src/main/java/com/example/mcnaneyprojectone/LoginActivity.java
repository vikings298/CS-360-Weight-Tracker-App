package com.example.mcnaneyprojectone;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signUpText = findViewById(R.id.signUpText);

        dbHelper = new DatabaseHelper(this);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        signUpText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewUser();
            }
        });
    }

    private void loginUser() {

        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please enter a username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean validLogin = dbHelper.checkUser(username, password);

        if (validLogin) {
            int userId = dbHelper.getUserId(username, password);

            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(LoginActivity.this, Home.class);
            intent.putExtra("USER_ID", userId);

            startActivity(intent);
            finish();

        } else {

            Toast.makeText(this,
                    "Invalid username or password",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void createNewUser() {

        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please enter a username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.checkUsername(username)) {

            Toast.makeText(this,
                    "Username already exists",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        boolean userCreated = dbHelper.addUser(username, password);

        if (userCreated) {

            Toast.makeText(this,
                    "Account created successfully",
                    Toast.LENGTH_SHORT).show();

        } else {

            Toast.makeText(this,
                    "Account creation failed",
                    Toast.LENGTH_SHORT).show();
        }
    }
}