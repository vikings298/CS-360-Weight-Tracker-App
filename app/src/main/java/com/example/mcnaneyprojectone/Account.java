
package com.example.mcnaneyprojectone;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.User;
import com.example.mcnaneyprojectone.service.UserService;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Displays and saves profile information and handles password changes.
 * Password verification and hashing run on a worker thread to keep the UI responsive.
 */
public class Account extends AppCompatActivity {

    private EditText firstNameInput;
    private EditText lastNameInput;
    private EditText emailInput;
    private EditText phoneInput;

    private Button saveAccountButton;
    private Button logoutButton;
    private Button changePasswordButton;

    private TextView notificationsText;

    private DatabaseHelper dbHelper;
    private UserService userService;

    private int userId;
    private boolean passwordChangeInProgress = false;

    /**
     * Binds this screen's views, initializes dependencies, and attaches user actions.
     */
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
        changePasswordButton =
                findViewById(R.id.changePasswordButton);

        notificationsText =
                findViewById(R.id.notificationsText);

        dbHelper = new DatabaseHelper(this);
        userService = new UserService(dbHelper);

        NavigationBar.setupBottomNav(
                this,
                NavigationBar.ACCOUNT,
                userId
        );

        loadAccountInfo();

        saveAccountButton.setOnClickListener(
                v -> saveAccountInfo()
        );

        changePasswordButton.setOnClickListener(
                v -> showChangePasswordDialog()
        );

        notificationsText.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Account.this,
                    SmsPermissionsActivity.class
            );

            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Account.this,
                    LoginActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });
    }

    /**
     * Collects profile fields and saves them through UserService.
     */
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

        boolean success = userService.saveAccountInfo(user);

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

    /**
     * Populates the profile form if a saved account record exists.
     */
    private void loadAccountInfo() {

        User user = userService.getAccountInfo(userId);

        if (user == null) {
            return;
        }

        firstNameInput.setText(user.getFirstName());
        lastNameInput.setText(user.getLastName());
        emailInput.setText(user.getEmail());
        phoneInput.setText(user.getPhone());
    }

    /**
     * Validates password input and performs the verified change on a worker thread.
     * Keeps the dialog open when validation or the update fails.
     */
    private void showChangePasswordDialog() {

        if (passwordChangeInProgress) {
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (
                20 * getResources()
                        .getDisplayMetrics().density
        );

        layout.setPadding(padding, 0, padding, 0);

        EditText currentPasswordInput = new EditText(this);
        currentPasswordInput.setHint("Current password");
        currentPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("New password");
        newPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        EditText confirmPasswordInput = new EditText(this);
        confirmPasswordInput.setHint("Confirm new password");
        confirmPasswordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        layout.addView(currentPasswordInput);
        layout.addView(newPasswordInput);
        layout.addView(confirmPasswordInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(layout)
                .setNegativeButton(
                        "Cancel",
                        (d, which) -> d.dismiss()
                )
                .setPositiveButton("Update", null)
                .create();

        // Replace the default button listener to prevent dismissal on invalid input.
        dialog.setOnShowListener(ignored -> {

            Button updateButton =
                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    );

            updateButton.setOnClickListener(v -> {

                if (passwordChangeInProgress) {
                    return;
                }

                String currentPassword =
                        currentPasswordInput
                                .getText().toString();

                String newPassword =
                        newPasswordInput
                                .getText().toString();

                String confirmPassword =
                        confirmPasswordInput
                                .getText().toString();

                if (currentPassword.isEmpty()) {
                    currentPasswordInput.setError(
                            "Enter your current password"
                    );
                    return;
                }

                if (newPassword.length() < 8) {
                    newPasswordInput.setError(
                            "Use at least 8 characters"
                    );
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    confirmPasswordInput.setError(
                            "Passwords do not match"
                    );
                    return;
                }

                if (currentPassword.equals(newPassword)) {
                    newPasswordInput.setError(
                            "Choose a different password"
                    );
                    return;
                }

                passwordChangeInProgress = true;
                updateButton.setEnabled(false);

                new Thread(() -> {

                    boolean success = false;

                    try {
                        success = userService.changePassword(
                                userId,
                                currentPassword,
                                newPassword
                        );
                    } catch (Exception e) {
                        Log.e(
                                "Account",
                                "Password change failed",
                                e
                        );
                    }

                    final boolean changed = success;

                    runOnUiThread(() -> {

                        passwordChangeInProgress = false;

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        updateButton.setEnabled(true);

                        if (changed) {

                            Toast.makeText(
                                    this,
                                    "Password updated successfully",
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();

                        } else {

                            Toast.makeText(
                                    this,
                                    "Password update failed. Check your current password.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

                }).start();
            });
        });

        dialog.show();
    }

    /**
     * Closes the database helper when this Activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}
