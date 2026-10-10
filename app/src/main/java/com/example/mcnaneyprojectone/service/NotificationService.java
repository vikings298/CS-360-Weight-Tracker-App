package com.example.mcnaneyprojectone.service;

import androidx.appcompat.app.AppCompatActivity;
import com.example.mcnaneyprojectone.model.Goal;
import android.Manifest;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

/**
 * Checks the weight-loss goal threshold and attempts an SMS notification.
 * Sending depends on a saved phone number and the Android SMS permission.
 */
public class NotificationService {

    private GoalService goalService;
    private UserService userService;

    public NotificationService(
            GoalService goalService,
            UserService userService) {

        this.goalService = goalService;
        this.userService = userService;
    }

    /**
     * Returns true when currentWeight is at or below the stored target.
     * A true result indicates the goal threshold, not confirmed SMS delivery.
     */
    public boolean checkGoalAndSendSms(
            AppCompatActivity activity,
            int userId,
            double currentWeight) {

        Goal goal = goalService.getGoal(userId);

        if (goal == null) {
            return false;
        }

        if (currentWeight <= goal.getTargetWeight()) {

            String phoneNumber =
                    userService.getPhoneNumber(userId);

            sendGoalReachedSms(
                    activity,
                    phoneNumber
            );

            return true;
        }

        return false;
    }

    /**
     * Attempts to send the goal message when a phone number and permission are available.
     */
    private void sendGoalReachedSms(
            AppCompatActivity activity,
            String phoneNumber) {

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(
                    activity,
                    "No phone number saved for SMS",
                    Toast.LENGTH_SHORT
            ).show();
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

            Toast.makeText(
                    activity,
                    "SMS alert sent",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    activity,
                    "SMS permission denied. App still works without SMS.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}