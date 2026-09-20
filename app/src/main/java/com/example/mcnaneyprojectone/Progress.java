package com.example.mcnaneyprojectone;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import java.util.Calendar;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.Goal;
import com.example.mcnaneyprojectone.model.WeightEntry;

import com.example.mcnaneyprojectone.service.WeightService;
import com.example.mcnaneyprojectone.service.GoalService;
import com.example.mcnaneyprojectone.service.UserService;
import com.example.mcnaneyprojectone.service.NotificationService;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class Progress extends AppCompatActivity {

    private EditText dateInput;
    private WeightService weightService;
    private GoalService goalService;
    private UserService userService;
    private NotificationService notificationService;

    private int userId;
    private EditText weightInput;
    private Button addWeightButton;
    private Button updateWeightButton;
    private TableLayout weightTable;

    private EditText goalWeightInput;
    private Button setGoalButton;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress_page);
        userId = getIntent().getIntExtra("USER_ID", -1);
        NavigationBar.setupBottomNav(this, NavigationBar.PROGRESS, userId);

        dateInput = findViewById(R.id.dateInput);
        weightInput = findViewById(R.id.weightInput);
        addWeightButton = findViewById(R.id.addWeightButton);
        updateWeightButton = findViewById(R.id.updateWeightButton);
        weightTable = findViewById(R.id.weightTable);
        dateInput.setOnClickListener(v -> showDatePicker());

        dbHelper = new DatabaseHelper(this);
        weightService = new WeightService(dbHelper);
        goalService = new GoalService(dbHelper);
        userService = new UserService(dbHelper);
        notificationService = new NotificationService(goalService, userService);



        goalWeightInput = findViewById(R.id.goalWeightInput);
        setGoalButton = findViewById(R.id.setGoalButton);

        setGoalButton.setOnClickListener(v -> setGoalWeight());


        loadWeights();

        addWeightButton.setOnClickListener(v -> addWeight());
        updateWeightButton.setOnClickListener(v -> updateWeight());


    }

    private void addWeight() {
        String date = dateInput.getText().toString().trim();
        String weightText = weightInput.getText().toString().trim();

        if (date.isEmpty() || weightText.isEmpty()) {
            Toast.makeText(this, "Enter date and weight", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight = Double.parseDouble(weightText);

        WeightEntry entry = new WeightEntry(
                -1,
                userId,
                date,
                weight
        );

        boolean success = weightService.addWeight(entry);

        if (success) {
            Toast.makeText(this, "Weight Added", Toast.LENGTH_SHORT).show();
            dateInput.setText("");
            weightInput.setText("");
            loadWeights();
            checkGoalAndSendSms(weight);
        } else {
            Toast.makeText(this, "A weight entry already exists for this date", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateWeight() {
        String date = dateInput.getText().toString().trim();
        String weightText = weightInput.getText().toString().trim();

        if (date.isEmpty() || weightText.isEmpty()) {
            Toast.makeText(this, "Enter existing date and new weight", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight = Double.parseDouble(weightText);

        WeightEntry entry = new WeightEntry(
                -1,
                userId,
                date,
                weight
        );

        boolean success =
                weightService.updateWeight(entry);

        if (success) {
            Toast.makeText(this, "Weight Updated", Toast.LENGTH_SHORT).show();
            dateInput.setText("");
            weightInput.setText("");
            loadWeights();

            checkGoalAndSendSms(weight);
        } else {
            Toast.makeText(this, "No entry found for that date", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate =
                            String.format("%02d/%02d/%04d",
                                    selectedMonth + 1,
                                    selectedDay,
                                    selectedYear);

                    dateInput.setText(formattedDate);
                },
                year,
                month,
                day
        );

        datePickerDialog.show();
    }

    private void loadWeights() {
        int rowCount = weightTable.getChildCount();

        if (rowCount > 1) {
            weightTable.removeViews(1, rowCount - 1);
        }

        List<WeightEntry> weights =
                weightService.getAllWeights(userId);

        for (WeightEntry entry : weights) {

            int id = entry.getId();
            String date = entry.getDate();
            double weight = entry.getWeight();

            TableRow row = new TableRow(this);

            TextView dateText = new TextView(this);
            dateText.setText(date);
            dateText.setPadding(12, 12, 12, 12);

            TextView weightTextView = new TextView(this);
            weightTextView.setText(weight + " lbs");
            weightTextView.setPadding(12, 12, 12, 12);

            Button deleteButton = new Button(this);
            deleteButton.setText("Delete");

            deleteButton.setOnClickListener(v -> {
                weightService.deleteWeight(id);
                loadWeights();
                Toast.makeText(Progress.this, "Weight Deleted", Toast.LENGTH_SHORT).show();
            });

            row.addView(dateText);
            row.addView(weightTextView);
            row.addView(deleteButton);

            weightTable.addView(row);
        }
    }

    private void setGoalWeight() {
        String goalText = goalWeightInput.getText().toString().trim();

        if (goalText.isEmpty()) {
            Toast.makeText(this, "Enter a goal weight", Toast.LENGTH_SHORT).show();
            return;
        }

        double goalWeight = Double.parseDouble(goalText);

        Goal goal = new Goal(
                -1,
                userId,
                goalWeight
        );

        boolean success =
                goalService.setGoal(goal);

        if (success) {
            Toast.makeText(this, "Goal weight saved", Toast.LENGTH_SHORT).show();
            goalWeightInput.setText("");
        } else {
            Toast.makeText(this, "Goal weight could not be saved", Toast.LENGTH_SHORT).show();
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