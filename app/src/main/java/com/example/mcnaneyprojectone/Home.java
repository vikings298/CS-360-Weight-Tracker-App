package com.example.mcnaneyprojectone;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.database.Cursor;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Home extends AppCompatActivity {

    private GridLayout weightGrid;

    private EditText weightInput;

    private int userId;
    private Button logWeightButton;
    private LinearLayout logoutButton;

    private DatabaseHelper dbHelper;

    private TextView currentWeightText;
    private TextView currentWeightDateText;
    private TextView goalWeightText;
    private TextView toGoalText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);
        userId = getIntent().getIntExtra("USER_ID", -1);

        weightGrid = findViewById(R.id.weightGrid);

        currentWeightText = findViewById(R.id.currentWeightText);
        currentWeightDateText = findViewById(R.id.currentWeightDateText);
        goalWeightText = findViewById(R.id.goalWeightText);
        toGoalText = findViewById(R.id.toGoalText);


        weightInput = findViewById(R.id.weightInput);
        logWeightButton = findViewById(R.id.logWeightButton);
        logoutButton = findViewById(R.id.logoutButton);

        dbHelper = new DatabaseHelper(this);

        loadWeightGrid();
        loadStats();

        NavigationBar.setupBottomNav(this, NavigationBar.HOME, userId);

        logWeightButton.setOnClickListener(v -> logTodaysWeight());

        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void logTodaysWeight() {
        String weightText = weightInput.getText().toString().trim();

        if (weightText.isEmpty()) {
            Toast.makeText(this, "Enter your weight", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight = Double.parseDouble(weightText);

        String today = new SimpleDateFormat(
                "MM/dd/yyyy",
                Locale.US
        ).format(new Date());

        boolean success = dbHelper.addWeight(userId, today, weight);

        if (success) {
            Toast.makeText(this, "Today's weight logged", Toast.LENGTH_SHORT).show();
            weightInput.setText("");
            loadWeightGrid();
            loadStats();
            checkGoalAndSendSms(weight);
        } else {
            Toast.makeText(this, "You already logged weight for today", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWeightGrid() {
        weightGrid.removeAllViews();

        Cursor cursor = dbHelper.getLastThirtyWeights(userId);

        while (cursor.moveToNext()) {
            String date = cursor.getString(1);
            double weight = cursor.getDouble(2);

            TextView weightCard = new TextView(this);
            weightCard.setText(date + "\n" + weight + " lbs");
            weightCard.setTextSize(14);
            weightCard.setTextColor(Color.parseColor("#101742"));
            weightCard.setGravity(Gravity.CENTER);
            weightCard.setPadding(8, 12, 8, 12);
            weightCard.setBackgroundColor(Color.WHITE);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(6, 6, 6, 6);

            weightCard.setLayoutParams(params);

            weightGrid.addView(weightCard);
        }

        cursor.close();
    }

    private void loadStats() {
        double currentWeight = 0;
        double goalWeight = 0;

        boolean hasCurrentWeight = false;
        boolean hasGoalWeight = false;

        Cursor currentCursor = dbHelper.getMostRecentWeight(userId);

        if (currentCursor.moveToFirst()) {
            String date = currentCursor.getString(0);
            currentWeight = currentCursor.getDouble(1);

            currentWeightText.setText(currentWeight + " lbs");
            currentWeightDateText.setText(date);

            hasCurrentWeight = true;
        } else {
            currentWeightText.setText("-- lbs");
            currentWeightDateText.setText("Log your first weight");
        }

        currentCursor.close();

        Cursor goalCursor = dbHelper.getGoalWeight(userId);

        if (goalCursor.moveToFirst()) {
            goalWeight = goalCursor.getDouble(0);
            goalWeightText.setText(goalWeight + " lbs");

            hasGoalWeight = true;
        } else {
            goalWeightText.setText("-- lbs");
        }

        goalCursor.close();

        if (hasCurrentWeight && hasGoalWeight) {
            double difference = currentWeight - goalWeight;
            toGoalText.setText(difference + " lbs");
        } else {
            toGoalText.setText("-- lbs");
        }
    }

    private void checkGoalAndSendSms(double currentWeight) {
        double goalWeight = dbHelper.getGoalWeightValue(userId);

        if (goalWeight != -1 && currentWeight <= goalWeight) {

            String phoneNumber = dbHelper.getPhoneNumber(userId);

            SmsPermissionsActivity.sendGoalReachedSmsIfAllowed(
                    this,
                    phoneNumber
            );

            Toast.makeText(
                    this,
                    "Goal reached!",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}