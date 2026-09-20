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

public class Account extends AppCompatActivity {

    private EditText firstNameInput;

    private UserService userService;

    private int userId;
    private EditText lastNameInput;
    private EditText emailInput;
    private EditText phoneInput;

    private Button saveAccountButton;
    private Button logoutButton;

    private TextView notificationsText;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_page);

        userId = getIntent().getIntExtra("USER_ID", -1);

        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);

        saveAccountButton = findViewById(R.id.saveAccountButton);
        logoutButton = findViewById(R.id.logoutButton);

        notificationsText = findViewById(R.id.notificationsText);

        dbHelper = new DatabaseHelper(this);
        userService = new UserService(dbHelper);

        NavigationBar.setupBottomNav(this, NavigationBar.ACCOUNT, userId);

        loadAccountInfo();

        saveAccountButton.setOnClickListener(v -> saveAccountInfo());

        notificationsText.setOnClickListener(v -> {
            Intent intent = new Intent(Account.this, SmsPermissionsActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);

        });

        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(Account.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveAccountInfo() {

        String firstName =
                firstNameInput.getText().toString().trim();

        String lastName =
                lastNameInput.getText().toString().trim();

        String email =
                emailInput.getText().toString().trim();

        String phone =
                phoneInput.getText().toString().trim();

        User user = new User(
                userId,
                firstName,
                lastName,
                email,
                phone
        );

        boolean success =
                userService.saveAccountInfo(user);

        if (success) {

            Toast.makeText(
                    this,
                    "Account information saved",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    this,
                    "Could not save account information",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void loadAccountInfo() {

        User user = userService.getAccountInfo(userId);

        if (user != null) {

            firstNameInput.setText(
                    user.getFirstName()
            );

            lastNameInput.setText(
                    user.getLastName()
            );

            emailInput.setText(
                    user.getEmail()
            );

            phoneInput.setText(
                    user.getPhone()
            );
        }
    }
}