package com.zybooks.michael_foster_weight_tracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.jjoe64.graphview.GraphView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Controls the application's screens and user interactions.
 *
 * <p>The XML layout files act as the views, the database helper stores and retrieves
 * app data, and this controller connects user actions to the model and database.</p>
 */
public class Controller extends AppCompatActivity {
    //region Class variables
    // Handles app-level logic that is not directly tied to drawing the screen.
    private final Model model = new Model();

    // Handles all SQLite create, read, update, and delete operations.
    private WeightTrackingDatabase database;

    // Tracks whether the create-account form is visible for custom Back behavior.
    private boolean showingCreateAccount = false;

    private static final int SMS_PERMISSION_REQUEST_CODE = 100;

    private String pendingSmsUserName;
    //endregion

    /**
     * Initializes the activity and displays the first screen.
     *
     * @param savedInstanceState Android's saved state bundle for this activity, if one exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the database helper once so every screen setup method can use it.
        database = new WeightTrackingDatabase(this);

        setupLoginScreen();
    }

    /**
     * Sets up and displays the login screen and create-account form.
     *
     * <p>This method loads the login layout, connects login and registration widgets,
     * validates login attempts, starts account creation, and handles the Back button
     * when the create-account form is visible.</p>
     */
    private void setupLoginScreen() {

        // Load the login layout before calling findViewById for login widgets.
        setContentView(R.layout.activity_login);
        showingCreateAccount = false;

        //region widget declarations

        // Widgets for existing-user login.
        EditText userNameField = findViewById(R.id.userNameField);
        EditText userPasswordField = findViewById(R.id.userPasswordField);

        Button loginButton = findViewById(R.id.loginButton);
        Button createNewAccount = findViewById(R.id.newAccountButton);

        TableLayout loginView = findViewById(R.id.loginView);

        // Widgets for new-account registration.
        EditText newUserNameField = findViewById(R.id.newUserNameField);
        EditText newUserPasswordField = findViewById(R.id.newUserPasswordField);
        EditText newUserPasswordConfirmField = findViewById(R.id.confirmUserPasswordField);
        EditText goalWeightField = findViewById(R.id.goalWeightField);

        Button submitNewAccount = findViewById(R.id.createNewAccountButton);

        TableLayout newUserView = findViewById(R.id.newUserView);
        //endregion

        //region listeners

        // Validate login credentials, then move the user to the tracking screen.
        loginButton.setOnClickListener(v -> {
            String userName = userNameField.getText().toString().trim();
            String userPassword = userPasswordField.getText().toString().trim();

            boolean response = false;
            try {
                response = database.validateUser(userName,userPassword);
            }
            catch (Exception e){
                Log.d("Login", "Error validating user", e);
            }

            if (response) {
                setupWeightTrackingScreen(userName);
            }
            else {
                Toast.makeText(this,"Invalid username or password",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Show the registration form without leaving the login layout.
        createNewAccount.setOnClickListener(v ->{
            loginView.setVisibility(View.GONE);
            newUserView.setVisibility(View.VISIBLE);
            showingCreateAccount = true;
        });

        // Read and validate registration fields before creating the account.
        submitNewAccount.setOnClickListener(v ->{
            String newUserName = newUserNameField.getText().toString().trim();
            String newUserPassword = newUserPasswordField.getText().toString().trim();
            String newUserPasswordConfirmed =
                    newUserPasswordConfirmField.getText().toString().trim();
            String goalWeightText = goalWeightField.getText().toString().trim();
            double goalWeight;

            // Convert the goal weight text to a number before saving it.
            try {
                goalWeight = Double.parseDouble(goalWeightText);
            } catch (NumberFormatException e) {
                Log.d("CreateNewAccount", "Invalid Goal Weight", e);
                Toast.makeText(this, "Invalid Goal Weight", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean createAccountStatus = false;
            if (newUserPassword.equals(newUserPasswordConfirmed)) {
                // Ask the model to coordinate creating user and goal-weight records.
                createAccountStatus = model.createAccount(
                        database,
                        newUserName,
                        newUserPassword,
                        goalWeight
                );
            }

            if (createAccountStatus) {
                setupSmsPermissionsScreen(newUserName);
            }
            else {
                Toast.makeText(this, "Unable to create new account",
                        Toast.LENGTH_SHORT).show();}
        });

        // Let Android's Back button return from the create-account view to log in.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            /**
             * Handles Back button behavior while the login layout is active.
             *
             * <p>If the create-account form is showing, the user returns to the login form.
             * Otherwise, Android's normal Back behavior continues.</p>
             */
            @Override
            public void handleOnBackPressed() {
                if (showingCreateAccount) {
                    newUserView.setVisibility(View.GONE);
                    loginView.setVisibility(View.VISIBLE);
                    showingCreateAccount = false;
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        //endregion


    }

    /**
     * Sets up and displays the SMS preference screen for a newly created user.
     *
     * @param userName username whose SMS preference should be updated
     */
    private void setupSmsPermissionsScreen(String userName) {
        // Load the SMS layout before reading its radio group and submit button.
        setContentView(R.layout.activity_sms_notification);

        RadioGroup smsPermission = findViewById(R.id.smsPermission);

        Button smsChoice = findViewById(R.id.submitSMSChoice);

        Button disableSMS = findViewById(R.id.disableSMS);

        TextView smsWarning = findViewById(R.id.smsWarning);

        smsPermission.setOnCheckedChangeListener((group, checkedId) -> {
            smsWarning.setVisibility(checkedId == R.id.disableSMS ? View.VISIBLE : View.GONE);
        });

        // Save the selected SMS preference, then continue to the tracking screen.
        smsChoice.setOnClickListener(v -> {
            boolean result = false;
            int selectedId = smsPermission.getCheckedRadioButtonId();

            // Store the radio selection as 1 for enabled or 0 for disabled.
            int smsPref = selectedId == R.id.enableSMS? 1 : 0;

            if (selectedId == -1) {
                Toast.makeText(this, "Please choose an SMS preference", Toast.LENGTH_SHORT).show();
                return;
            }

            try {;

                if (smsPref == 1) {
                    if (!hasSmsPermission()) {
                        pendingSmsUserName = userName;
                        requestSmsPermission();
                        return;
                    }

                   result = database.updateSmsPreference(userName, 1);
                }
                else{
                    result = database.updateSmsPreference(userName,0);
                }
            }
            catch (Exception e){
                Log.d("SMS Preference", "Error entering SMS pref", e);
            }

            if (!result) {
                Toast.makeText(this,"SMS preference not captured",
                        Toast.LENGTH_SHORT).show();}
            else {
                setupWeightTrackingScreen(userName);
            }
        });
    }

    /**
     * Sets up and displays the weight tracking screen for a logged-in user.
     *
     * @param userName username whose weights and goal should be displayed
     */
    private void setupWeightTrackingScreen(String userName) {
        // Load the tracking layout before wiring its input field and buttons.
        setContentView(R.layout.activity_weight_tracking);

        EditText todayWeight = findViewById(R.id.todayWeightField);

        Button enterWeight = findViewById(R.id.enterWeightButton);

        TextView lbsToGo = findViewById(R.id.poundsToGoDiscription);

        LinearLayout weighInCardContainer = findViewById(R.id.weighInCardContainer);

        GraphView graph = findViewById(R.id.idGraphView);

        model.displayPreviousWeights(userName, database,weighInCardContainer,lbsToGo,todayWeight,graph,this);
        model.checkForTodayWeight(userName,database,lbsToGo,todayWeight,this);
        model.displayWeightGraph(userName,database,graph, this);


        enterWeight.setOnClickListener(v -> {
            boolean result = false;
            String weight = todayWeight.getText().toString().trim();
            try {
                boolean todayWeightLogged = database.getTodayWeight(userName) != -1.0;

                if (!todayWeightLogged) {
                    result = database.addDailyWeight(userName, Double.parseDouble(weight));
                } else {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    String formattedDate = formatter.format(new Date());
                    result = database.updateWeight(userName,formattedDate,Double.parseDouble(weight));
                }
            }
            catch (Exception e) {
                Log.d("Weight Tracker", "Error adding daily weight", e);
            }
            if (!result) {
                Toast.makeText(this,"Invalid weight",
                        Toast.LENGTH_SHORT).show();
            }
            else {
                checkGoalAndSendSms(userName, Double.parseDouble(weight));
                setupWeightTrackingScreen(userName);
            }
        });
    }

    /**
     * Checks whether the app currently has permission to send SMS messages.
     *
     * @return true when the SEND_SMS permission is granted, otherwise false
     */
    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests the Android runtime permission needed for sending SMS messages.
     */
    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.SEND_SMS },
                SMS_PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Handles the user's response to the SMS runtime permission request.
     *
     * @param requestCode identifies which permission request produced the result
     * @param permissions permissions included in the request
     * @param grantResults grant or denial results for each requested permission
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (pendingSmsUserName != null && requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                database.updateSmsPreference(pendingSmsUserName, 1);
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                database.updateSmsPreference(pendingSmsUserName, 0);
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
            setupWeightTrackingScreen(pendingSmsUserName);
            pendingSmsUserName = null;
        }
    }

    /**
     * Sends a congratulatory SMS message when the user reaches the saved goal weight.
     *
     * <p>The SMS is only attempted when the current weight reaches the goal, the
     * user enabled SMS notifications, and Android has granted SMS permission.</p>
     *
     * @param userName username associated with the current weight entry
     * @param currentWeight weight value entered by the user
     */
    private void checkGoalAndSendSms(String userName, double currentWeight) {
        double goalWeight = database.getGoalWeight(userName);

        boolean goalReached = currentWeight <= goalWeight;
        boolean smsEnabledByUser = database.getSmsPreference(userName) == 1;
        try {
            if (goalReached && smsEnabledByUser && hasSmsPermission()) {
                SmsManager smsManager = SmsManager.getDefault();

                smsManager.sendTextMessage(
                        database.getPhoneNumber(userName),
                        null,
                        "Congratulations! You reached your goal weight!",
                        null,
                        null
                );
                Toast.makeText(this, "Sending SMS to " + database.getPhoneNumber(userName), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.d("Text Message","Text Message Failed to Send",e);
            Toast.makeText(this,"Text message failed to send",Toast.LENGTH_SHORT).show();
        }
    }
}
