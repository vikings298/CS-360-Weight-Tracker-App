package com.example.mcnaneyprojectone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SmsPermissionsActivity extends AppCompatActivity {

    // SMS permission request code
    private static final int SMS_PERMISSION_CODE = 100;

    private int userId;

    private TextView permissionStatusText;
    private Button requestPermissionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the SMS permissions screen
        setContentView(R.layout.sms_permissions_page);
        userId = getIntent().getIntExtra("USER_ID", -1);
        NavigationBar.setupBottomNav(
                this,
                NavigationBar.ACCOUNT,
                userId
        );

        permissionStatusText = findViewById(R.id.permissionStatusText);
        requestPermissionButton = findViewById(R.id.requestPermissionButton);

        // Display the current permission status
        updatePermissionStatus();

        requestPermissionButton.setOnClickListener(v -> {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Ask the user for SMS permission
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_CODE);

            } else {

                permissionStatusText.setText("SMS Permission: Granted");
            }
        });
    }

    private void updatePermissionStatus() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {

            permissionStatusText.setText("SMS Permission: Granted");

        } else {

            permissionStatusText.setText("SMS Permission: Not Granted");
        }
    }

    // Handle the user's response to the permission request
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                permissionStatusText.setText("SMS Permission: Granted");

            } else {

                permissionStatusText.setText("SMS Permission: Denied");
            }
        }
    }

    public static void sendGoalReachedSmsIfAllowed(
            AppCompatActivity activity,
            String phoneNumber) {

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(activity,
                    "No phone number saved for SMS",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {

            SmsManager smsManager = SmsManager.getDefault();

            smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    "Congratulations! You reached your goal weight.",
                    null,
                    null
            );

            Toast.makeText(activity,
                    "SMS alert sent",
                    Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(activity,
                    "SMS permission denied. App still works without SMS.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}