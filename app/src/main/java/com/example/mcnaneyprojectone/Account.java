package com.example.mcnaneyprojectone;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Account extends AppCompatActivity {

    private EditText firstNameInput;

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

        boolean success = dbHelper.saveAccountInfo(userId, firstName, lastName, email, phone);

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

        Cursor cursor = dbHelper.getAccountInfo(userId);

        if (cursor.moveToFirst()) {

            firstNameInput.setText(
                    cursor.getString(0));

            lastNameInput.setText(
                    cursor.getString(1));

            emailInput.setText(
                    cursor.getString(2));

            phoneInput.setText(
                    cursor.getString(3));
        }

        cursor.close();
    }
}