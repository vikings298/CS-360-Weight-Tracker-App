package com.example.mcnaneyprojectone;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;

import com.example.mcnaneyprojectone.database.DatabaseHelper;
import com.example.mcnaneyprojectone.model.Goal;
import com.example.mcnaneyprojectone.model.WeightEntry;
import com.example.mcnaneyprojectone.service.WeightService;
import com.example.mcnaneyprojectone.service.GoalService;
import com.example.mcnaneyprojectone.service.UserService;
import com.example.mcnaneyprojectone.service.NotificationService;
import com.example.mcnaneyprojectone.util.DateFormats;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Provides weight-history editing and goal entry for the selected user.
 * Dates are displayed as MM/dd/yyyy and converted to ISO format before storage.
 */
public class Progress extends AppCompatActivity {

    private EditText dateInput;
    private EditText weightInput;
    private EditText goalWeightInput;

    private WeightService weightService;
    private GoalService goalService;
    private UserService userService;
    private NotificationService notificationService;

    private int userId;

    private Button addWeightButton;
    private Button updateWeightButton;
    private Button setGoalButton;

    private TableLayout weightTable;

    private DatabaseHelper dbHelper;

    /**
     * Binds this screen's views, initializes dependencies, and attaches user actions.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progress_page);

        userId = getIntent().getIntExtra(
                "USER_ID",
                -1
        );

        NavigationBar.setupBottomNav(
                this,
                NavigationBar.PROGRESS,
                userId
        );

        dateInput =
                findViewById(R.id.dateInput);

        weightInput =
                findViewById(R.id.weightInput);

        addWeightButton =
                findViewById(R.id.addWeightButton);

        updateWeightButton =
                findViewById(R.id.updateWeightButton);

        weightTable =
                findViewById(R.id.weightTable);

        dateInput.setOnClickListener(
                v -> showDatePicker()
        );

        dbHelper = new DatabaseHelper(this);

        weightService = new WeightService(dbHelper);
        goalService = new GoalService(dbHelper);
        userService = new UserService(dbHelper);

        notificationService =
                new NotificationService(
                        goalService,
                        userService
                );

        goalWeightInput =
                findViewById(R.id.goalWeightInput);

        setGoalButton =
                findViewById(R.id.setGoalButton);

        setGoalButton.setOnClickListener(
                v -> setGoalWeight()
        );

        loadWeights();

        addWeightButton.setOnClickListener(
                v -> addWeight()
        );

        updateWeightButton.setOnClickListener(
                v -> updateWeight()
        );
    }

    // -------------------------------------------------
    // DATE AND WEIGHT VALIDATION
    // -------------------------------------------------

    /**
     * Returns the selected date in storage format, or null after marking invalid input.
     */
    private String selectedIsoDate() {

        try {

            String displayDate =
                    dateInput.getText().toString().trim();

            return DateFormats.toIso(displayDate);

        } catch (IllegalArgumentException e) {

            dateInput.setError(
                    "Select a valid date"
            );

            return null;
        }
    }

    /**
     * Returns a positive finite weight, or null after marking invalid input.
     */
    private Double enteredWeight() {

        try {

            double weight = Double.parseDouble(
                    weightInput.getText().toString().trim()
            );

            if (Double.isFinite(weight) && weight > 0) {
                return weight;
            }

        } catch (NumberFormatException ignored) {
        }

        weightInput.setError(
                "Enter a positive weight"
        );

        return null;
    }

    // -------------------------------------------------
    // ADD WEIGHT
    // -------------------------------------------------

    /**
     * Adds a validated record for the selected date and refreshes the history.
     */
    private void addWeight() {

        String isoDate = selectedIsoDate();
        Double weight = enteredWeight();

        if (isoDate == null || weight == null) {
            return;
        }

        WeightEntry entry = new WeightEntry(
                -1,
                userId,
                isoDate,
                weight
        );

        boolean success =
                weightService.addWeight(entry);

        if (success) {

            Toast.makeText(
                    this,
                    "Weight Added",
                    Toast.LENGTH_SHORT
            ).show();

            dateInput.setText("");
            weightInput.setText("");

            loadWeights();

            checkGoalAndSendSms(weight);

        } else {

            Toast.makeText(
                    this,
                    "A weight entry already exists for this date",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // -------------------------------------------------
    // UPDATE WEIGHT
    // -------------------------------------------------

    /**
     * Updates the selected user's existing record by date rather than inserting another.
     */
    private void updateWeight() {

        String isoDate = selectedIsoDate();
        Double weight = enteredWeight();

        if (isoDate == null || weight == null) {
            return;
        }

        WeightEntry entry = new WeightEntry(
                -1,
                userId,
                isoDate,
                weight
        );

        boolean success =
                weightService.updateWeight(entry);

        if (success) {

            Toast.makeText(
                    this,
                    "Weight Updated",
                    Toast.LENGTH_SHORT
            ).show();

            dateInput.setText("");
            weightInput.setText("");

            loadWeights();

            checkGoalAndSendSms(weight);

        } else {

            Toast.makeText(
                    this,
                    "No entry found for that date",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // -------------------------------------------------
    // DATE PICKER
    // -------------------------------------------------

    /**
     * Opens a calendar picker and writes the chosen date in display format.
     */
    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        int year =
                calendar.get(Calendar.YEAR);

        int month =
                calendar.get(Calendar.MONTH);

        int day =
                calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view,
                         selectedYear,
                         selectedMonth,
                         selectedDay) -> {

                            String formattedDate =
                                    String.format(
                                            Locale.US,
                                            "%02d/%02d/%04d",
                                            selectedMonth + 1,
                                            selectedDay,
                                            selectedYear
                                    );

                            dateInput.setText(
                                    formattedDate
                            );
                        },
                        year,
                        month,
                        day
                );

        datePickerDialog.show();
    }

    // -------------------------------------------------
    // WEIGHT HISTORY
    // -------------------------------------------------

    /**
     * Rebuilds the history rows while preserving the table's header row.
     */
    private void loadWeights() {

        int rowCount =
                weightTable.getChildCount();

        if (rowCount > 1) {

            weightTable.removeViews(
                    1,
                    rowCount - 1
            );
        }

        List<WeightEntry> weights =
                weightService.getAllWeights(userId);

        for (WeightEntry entry : weights) {

            int id = entry.getId();
            double weight = entry.getWeight();

            TableRow row = new TableRow(this);

            TextView dateText = new TextView(this);

            dateText.setText(
                    DateFormats.toDisplay(
                            entry.getDate()
                    )
            );

            dateText.setPadding(
                    12, 12, 12, 12
            );

            TextView weightTextView =
                    new TextView(this);

            weightTextView.setText(
                    weight + " lbs"
            );

            weightTextView.setPadding(
                    12, 12, 12, 12
            );

            Button deleteButton =
                    new Button(this);

            deleteButton.setText("Delete");

            deleteButton.setOnClickListener(v -> {

                weightService.deleteWeight(id);

                loadWeights();

                Toast.makeText(
                        Progress.this,
                        "Weight Deleted",
                        Toast.LENGTH_SHORT
                ).show();
            });

            row.addView(dateText);
            row.addView(weightTextView);
            row.addView(deleteButton);

            weightTable.addView(row);
        }
    }

    // -------------------------------------------------
    // GOAL WEIGHT
    // -------------------------------------------------

    /**
     * Validates a positive finite goal and saves it through GoalService.
     */
    private void setGoalWeight() {

        String goalText =
                goalWeightInput.getText().toString().trim();

        double target;

        try {

            target = Double.parseDouble(goalText);

        } catch (NumberFormatException e) {

            goalWeightInput.setError(
                    "Enter a valid goal weight"
            );

            return;
        }

        if (!Double.isFinite(target) || target <= 0) {

            goalWeightInput.setError(
                    "Enter a positive goal weight"
            );

            return;
        }

        Goal goal = new Goal(
                -1,
                userId,
                target
        );

        boolean success =
                goalService.setGoal(goal);

        if (success) {

            Toast.makeText(
                    this,
                    "Goal weight saved",
                    Toast.LENGTH_SHORT
            ).show();

            goalWeightInput.setText("");

        } else {

            Toast.makeText(
                    this,
                    "Goal weight could not be saved",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Delegates the goal check after a successful weight insert or update.
     */
    private void checkGoalAndSendSms(
            double currentWeight) {

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