package com.example.mcnaneyprojectone;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.WeightEntry;
import com.example.mcnaneyprojectone.service.WeightService;
import com.example.mcnaneyprojectone.model.Goal;
import com.example.mcnaneyprojectone.service.GoalService;
import com.example.mcnaneyprojectone.service.UserService;
import com.example.mcnaneyprojectone.service.NotificationService;
import com.example.mcnaneyprojectone.service.AnalyticsService;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class Home extends AppCompatActivity {

    private GridLayout weightGrid;

    private WeightService weightService;
    private UserService userService;
    private GoalService goalService;
    private NotificationService notificationService;
    private AnalyticsService analyticsService;

    private EditText weightInput;

    private int userId;
    private Button logWeightButton;
    private LinearLayout logoutButton;

    private DatabaseHelper dbHelper;

    private TextView currentWeightText;
    private TextView currentWeightDateText;
    private TextView goalWeightText;
    private TextView toGoalText;
    private TextView sevenDayAverageText;

    private TextView loggingStreakText;
    private TextView weightTrendText;
    private TextView weightChangeText;
    private TextView goalEtaText;
    private TextView plateauText;

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
        sevenDayAverageText = findViewById(R.id.sevenDayAverageText);
        weightTrendText = findViewById(R.id.weightTrendText);
        loggingStreakText = findViewById(R.id.loggingStreakText);

        weightInput = findViewById(R.id.weightInput);
        logWeightButton = findViewById(R.id.logWeightButton);
        logoutButton = findViewById(R.id.logoutButton);
        weightChangeText = findViewById(R.id.weightChangeText);
        goalEtaText = findViewById(R.id.goalEtaText);
        plateauText = findViewById(R.id.plateauText);

        dbHelper = new DatabaseHelper(this);
        goalService = new GoalService(dbHelper);
        weightService = new WeightService(dbHelper);
        userService = new UserService(dbHelper);
        notificationService = new NotificationService(goalService, userService);
        analyticsService = new AnalyticsService();

        loadWeightGrid();
        loadStats();
        loadAnalytics();

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

        WeightEntry entry = new WeightEntry(
                -1,
                userId,
                today,
                weight
        );

        boolean success = weightService.addWeight(entry);

        if (success) {
            Toast.makeText(this, "Today's weight logged", Toast.LENGTH_SHORT).show();
            weightInput.setText("");
            loadWeightGrid();
            loadStats();
            loadAnalytics();
            checkGoalAndSendSms(weight);
        } else {
            Toast.makeText(this, "You already logged weight for today", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWeightGrid() {
        weightGrid.removeAllViews();

        List<WeightEntry> weights =
                weightService.getLastThirtyWeights(userId);

        for (WeightEntry entry: weights) {

            String date = entry.getDate();
            double weight = entry.getWeight();

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
    }

    private void loadAnalytics() {

        // Recent entries for moving average and trend
        List<WeightEntry> recentWeights =
                weightService.getLastThirtyWeights(userId);

        double sevenEntryAverage =
                analyticsService.calculateMovingAverage(recentWeights, 7);

        if (sevenEntryAverage != -1) {
            sevenDayAverageText.setText(
                    String.format(Locale.US, "%.1f lbs", sevenEntryAverage)
            );
        } else {
            sevenDayAverageText.setText("Not enough data");
        }

        String trend =
                analyticsService.classifyTrend(recentWeights);

        weightTrendText.setText(trend);


        // Complete history for longest logging streak
        List<WeightEntry> allWeights =
                weightService.getAllWeights(userId);

        int currentStreak =
                analyticsService.calculateCurrentStreak(allWeights);

        loggingStreakText.setText(
                currentStreak + (currentStreak == 1 ? " day" : " days")
        );

        // Recent weight change
        double weightChange =
                analyticsService.calculateWeightChange(recentWeights);

        if (!Double.isNaN(weightChange)) {
            weightChangeText.setText(
                    String.format(Locale.US, "%+.1f lbs", weightChange)
            );
        } else {
            weightChangeText.setText("Not enough data");
        }


        // Goal ETA
        Goal goal = goalService.getGoal(userId);

        if (goal != null) {

            int estimatedDays =
                    analyticsService.calculateGoalETA(
                            recentWeights,
                            goal.getTargetWeight()
                    );

            if (estimatedDays >= 0) {
                goalEtaText.setText(
                        estimatedDays + (estimatedDays == 1 ? " day" : " days")
                );
            } else {
                goalEtaText.setText("No estimate");
            }

        } else {
            goalEtaText.setText("Set a goal first");
        }


// Plateau detection
        if (recentWeights.size() >= 7) {

            boolean plateau =
                    analyticsService.detectPlateau(recentWeights);

            plateauText.setText(
                    plateau ? "Plateau detected" : "No plateau"
            );

        } else {
            plateauText.setText("Not enough data");
        }
    }

    private void loadStats() {

        boolean hasCurrentWeight = false;
        boolean hasGoalWeight = false;

        WeightEntry currentWeight =
                weightService.getMostRecentWeight(userId);

        if (currentWeight != null) {
            currentWeightText.setText(
                    currentWeight.getWeight() + " lbs"
            );
            currentWeightDateText.setText(
                    currentWeight.getDate()
            );

            hasCurrentWeight = true;
        } else {
            currentWeightText.setText("-- lbs");
            currentWeightDateText.setText("Log your first weight");
        }


        Goal goal = goalService.getGoal(userId);

        if (goal != null) {
            goalWeightText.setText(
                    goal.getTargetWeight() + " lbs"
            );

            hasGoalWeight = true;
        } else {
            goalWeightText.setText("-- lbs");
        }


        if (hasCurrentWeight && hasGoalWeight) {

            double difference =
                    currentWeight.getWeight() - goal.getTargetWeight();

            toGoalText.setText(difference + " lbs");

        } else {
            toGoalText.setText("-- lbs");
        }
    }
    private void checkGoalAndSendSms(double currentWeight) {

        boolean goalReached =
                notificationService.checkGoalAndSendSms(
                        this,
                        userId,
                        currentWeight
                );

        if (goalReached) {
            Toast.makeText(
                    this,
                    "Goal reached!",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}