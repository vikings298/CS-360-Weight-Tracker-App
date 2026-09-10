package com.example.mcnaneyprojectone;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import java.util.Calendar;

import androidx.appcompat.app.AppCompatActivity;

public class Progress extends AppCompatActivity {

    private EditText dateInput;

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

        boolean success = dbHelper.addWeight(userId, date, weight);

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

        boolean success = dbHelper.updateWeightByDate(userId, date, weight);

        if (success) {
            Toast.makeText(this, "Weight Updated", Toast.LENGTH_SHORT).show();
            dateInput.setText("");
            weightInput.setText("");
            loadWeights();
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

        Cursor cursor = dbHelper.getAllWeights(userId);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String date = cursor.getString(1);
            double weight = cursor.getDouble(2);

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
                dbHelper.deleteWeight(id);
                loadWeights();
                Toast.makeText(Progress.this, "Weight Deleted", Toast.LENGTH_SHORT).show();
            });

            row.addView(dateText);
            row.addView(weightTextView);
            row.addView(deleteButton);

            weightTable.addView(row);
        }

        cursor.close();
    }

    private void setGoalWeight() {
        String goalText = goalWeightInput.getText().toString().trim();

        if (goalText.isEmpty()) {
            Toast.makeText(this, "Enter a goal weight", Toast.LENGTH_SHORT).show();
            return;
        }

        double goalWeight = Double.parseDouble(goalText);

        boolean success = dbHelper.setGoalWeight(userId, goalWeight);

        if (success) {
            Toast.makeText(this, "Goal weight saved", Toast.LENGTH_SHORT).show();
            goalWeightInput.setText("");
        } else {
            Toast.makeText(this, "Goal weight could not be saved", Toast.LENGTH_SHORT).show();
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