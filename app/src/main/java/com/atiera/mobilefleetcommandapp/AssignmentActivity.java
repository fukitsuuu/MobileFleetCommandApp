package com.atiera.mobilefleetcommandapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.widget.ScrollView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.content.DialogInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import java.util.Calendar;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;

// Image picker imports
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssignmentActivity extends DashboardActivity {

    private LinearLayout tripsContainer;
    private LinearLayout maintenanceContainer;
    private TextView noTripsText;
    private TextView noMaintenanceText;
    private ProgressBar loadingIndicator;
    private ProgressBar maintenanceLoadingIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USERNAME = "username";
    
    // Image picker variables - Perfect copy from ExpenseManager

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment);
        
        // Re-initialize drawer components after setting content view
        initializeDrawerComponents();
        
        // Set Assignment as checked since this is the Assignment page
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_assignment);
        }

        // Initialize views
        tripsContainer = findViewById(R.id.tripsContainer);
        maintenanceContainer = findViewById(R.id.maintenanceContainer);
        noTripsText = findViewById(R.id.noTripsText);
        noMaintenanceText = findViewById(R.id.noMaintenanceText);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        maintenanceLoadingIndicator = findViewById(R.id.maintenanceLoadingIndicator);
        
        // Initially hide the "No assignments yet" message
        noTripsText.setVisibility(View.GONE);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // Initialize image picker
        initializeImagePicker();

        // Set up refresh button
        // Refresh icon removed from layout; use pull-to-refresh instead

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchDriverTrips();
                fetchDriverMaintenance();
            }
        });

        // Fetch driver trips and maintenance
        fetchDriverTrips();
        fetchDriverMaintenance();
    }

    private void fetchDriverTrips() {
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        if (username.isEmpty()) {
            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
                // Show loading
        loadingIndicator.setVisibility(View.VISIBLE);
        tripsContainer.setVisibility(View.GONE);
        noTripsText.setVisibility(View.GONE);

        ApiClient.get().getDriverTrips(username).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TripResponse tripResponse = response.body();
                    if (tripResponse.ok) {
                        Log.d("AssignmentActivity", "Successfully fetched " + tripResponse.totalTrips + " trips from trip_tb");
                        if (tripResponse.trips != null) {
                            for (TripResponse.Trip trip : tripResponse.trips) {
                                Log.d("AssignmentActivity", "Trip: " + trip.tripID + 
                                      ", Location: " + trip.location + 
                                      ", Description: " + (trip.description != null ? trip.description : "null"));
                            }
                        }
                        displayTrips(tripResponse.trips);
                    } else {
                        String errorMessage = tripResponse.msg != null ? tripResponse.msg : "Failed to fetch trips";
                        Toast.makeText(AssignmentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        showNoTrips();
                    }
                } else {
                    String errorMessage = "Failed to fetch trips";
                    if (response.code() == 404) {
                        errorMessage = "No trips found";
                    } else if (response.code() == 500) {
                        errorMessage = "Server error. Please try again later";
                    }
                    Toast.makeText(AssignmentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    showNoTrips();
                }
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                String errorMessage = "No Internet Connection";
                if (t.getMessage() != null && t.getMessage().contains("timeout")) {
                    errorMessage = "Connection timeout. Please try again";
                }
                Toast.makeText(AssignmentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                showNoTrips();
            }
        });
    }

    private void fetchDriverMaintenance() {
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        if (username.isEmpty()) {
            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading
        maintenanceLoadingIndicator.setVisibility(View.VISIBLE);
        maintenanceContainer.setVisibility(View.GONE);
        noMaintenanceText.setVisibility(View.GONE);

        ApiClient.get().getDriverMaintenance(username).enqueue(new Callback<MaintenanceResponse>() {
            @Override
            public void onResponse(Call<MaintenanceResponse> call, Response<MaintenanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MaintenanceResponse maintenanceResponse = response.body();
                    if (maintenanceResponse.ok) {
                        Log.d("AssignmentActivity", "Successfully fetched " + maintenanceResponse.totalMaintenance + " maintenance records");
                        displayMaintenance(maintenanceResponse.maintenance);
                    } else {
                        String errorMessage = maintenanceResponse.msg != null ? maintenanceResponse.msg : "Failed to fetch maintenance";
                        Toast.makeText(AssignmentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        showNoMaintenance();
                    }
                } else {
                    String errorMessage = "Failed to fetch maintenance";
                    if (response.code() == 404) {
                        errorMessage = "No maintenance records found";
                    } else if (response.code() == 500) {
                        errorMessage = "Server error. Please try again later";
                    }
                    Toast.makeText(AssignmentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    showNoMaintenance();
                }
            }

            @Override
            public void onFailure(Call<MaintenanceResponse> call, Throwable t) {
                String errorMessage = "No Internet Connection";
                if (t.getMessage() != null && t.getMessage().contains("timeout")) {
                    errorMessage = "Connection timeout. Please try again";
                }
                Toast.makeText(AssignmentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                showNoMaintenance();
            }
        });
    }

    private void displayTrips(List<TripResponse.Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            showNoTrips();
            return;
        }

        loadingIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        tripsContainer.setVisibility(View.VISIBLE);
        noTripsText.setVisibility(View.GONE);
        tripsContainer.removeAllViews();

        int added = 0;
        for (TripResponse.Trip trip : trips) {
            // Only show trips with Pending status on Assignment screen
            if (trip.status != null && trip.status.equalsIgnoreCase("pending")) {
                View tripView = createTripView(trip);
                tripsContainer.addView(tripView);
                added++;
            }
        }

        if (added == 0) {
            showNoTrips();
            // Check if we should show "No assignments yet" message
            checkAndShowNoAssignments();
        } else {
            // Hide "No assignments yet" message since we have trip records
            noTripsText.setVisibility(View.GONE);
        }
    }

    private void displayMaintenance(List<MaintenanceResponse.Maintenance> maintenanceList) {
        if (maintenanceList == null || maintenanceList.isEmpty()) {
            showNoMaintenance();
            // Check if we should show "No assignments yet" message
            checkAndShowNoAssignments();
            return;
        }

        maintenanceLoadingIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        maintenanceContainer.setVisibility(View.VISIBLE);
        noMaintenanceText.setVisibility(View.GONE);
        maintenanceContainer.removeAllViews();

        int added = 0;
        for (MaintenanceResponse.Maintenance maintenance : maintenanceList) {
            // Only show maintenance that is NOT completed and NOT cancelled on Assignment screen
            if (maintenance.maintenanceStatus != null && 
                !maintenance.maintenanceStatus.equalsIgnoreCase("completed") &&
                !maintenance.maintenanceStatus.equalsIgnoreCase("cancelled")) {
                View maintenanceView = createMaintenanceView(maintenance);
                maintenanceContainer.addView(maintenanceView);
                added++;
            }
        }

        if (added == 0) {
            showNoMaintenance();
            // Check if we should show "No assignments yet" message
            checkAndShowNoAssignments();
        } else {
            // Hide "No assignments yet" message since we have maintenance records
            noTripsText.setVisibility(View.GONE);
        }
    }

    private View createTripView(TripResponse.Trip trip) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View tripView = inflater.inflate(R.layout.trip_item, tripsContainer, false);

        // Set trip data
        TextView tripIdText = tripView.findViewById(R.id.tripIdText);
        TextView requesterText = tripView.findViewById(R.id.requesterText);
        TextView locationText = tripView.findViewById(R.id.locationText);
        TextView urgencyText = tripView.findViewById(R.id.urgencyText);
        TextView statusText = tripView.findViewById(R.id.statusText);
        TextView vehicleText = tripView.findViewById(R.id.vehicleText);
        TextView distanceText = tripView.findViewById(R.id.distanceText);
        TextView timeText = tripView.findViewById(R.id.timeText);
        TextView budgetText = tripView.findViewById(R.id.budgetText);

        // Get dropdown and map components
        LinearLayout dropdownContent = tripView.findViewById(R.id.dropdownContent);
        WebView mapWebView = tripView.findViewById(R.id.mapWebView);
        Button confirmButton = tripView.findViewById(R.id.confirmButton);

        // Prevent parent ScrollView from intercepting map gestures so panning works smoothly
        mapWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false; // let WebView handle the gesture
            }
        });

        tripIdText.setText(trip.tripID);
        requesterText.setText(trip.requester);
        locationText.setText(trip.location);
        urgencyText.setText(trip.urgency);
        statusText.setText(trip.status);
        vehicleText.setText(trip.vehicleNumber);
        distanceText.setText(trip.estimatedDistance);
        timeText.setText(trip.estimatedTime);
        if (trip.budget < 0.01) {
            budgetText.setText("Reimbursement");
            budgetText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            budgetText.setText("₱" + String.format("%.2f", trip.budget));
            budgetText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        

        
        // Display description from trip_tb if available
        TextView descriptionText = tripView.findViewById(R.id.descriptionText);
        LinearLayout descriptionContainer = tripView.findViewById(R.id.descriptionContainer);
        if (trip.description != null && !trip.description.trim().isEmpty()) {
            descriptionText.setText(trip.description.trim());
            descriptionContainer.setVisibility(View.VISIBLE);
            Log.d("AssignmentActivity", "Displaying description from trip_tb: " + trip.description.trim());
        } else {
            descriptionContainer.setVisibility(View.GONE);
            Log.d("AssignmentActivity", "No description available in trip_tb");
        }

        // Set urgency color
        switch (trip.urgency.toLowerCase()) {
            case "high":
                urgencyText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                break;
            case "normal":
                urgencyText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "low":
                urgencyText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
        }

        // Set status color
        switch (trip.status.toLowerCase()) {
            case "completed":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "in transit":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                break;
            case "pending":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            default:
                statusText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                break;
        }

        // Set up click listener for the entire trip card
        tripView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (trip.status != null && trip.status.equalsIgnoreCase("completed")) {
                    // Do not show dropdown for completed trips
                    return;
                }
                toggleDropdown(dropdownContent, mapWebView, trip);
            }
        });

        // Configure confirm button label and behavior based on status
        String currentStatus = trip.status != null ? trip.status : "";
        String lower = currentStatus.toLowerCase();
        String buttonLabel = "Confirm Trip";
        String nextStatus = null;
        boolean openTripLog = false;
        if (lower.equals("pending")) {
            buttonLabel = "Confirm Trip";
            nextStatus = "Preparing";
        } else if (lower.equals("preparing")) {
            buttonLabel = "Ready";
            nextStatus = "Departed";
        } else if (lower.equals("departed")) {
            buttonLabel = "Confirm Pick Up";
            nextStatus = "Picked Up";
        } else if (lower.equals("picked up")) {
            buttonLabel = "Confirm Delivery";
            nextStatus = "Delivered";
        } else if (lower.equals("delivered")) {
            buttonLabel = "Mark as done";
            nextStatus = "Completed";
        } else if (lower.equals("completed")) {
            // Completed trips: no dropdown and button opens Trip Logging screen
            buttonLabel = "Open Trip Log";
            nextStatus = null;
            openTripLog = true;
        }
        confirmButton.setText(buttonLabel);
        final String targetStatus = nextStatus;
        final String actionLabel = buttonLabel;
        final boolean finalOpenTripLog = openTripLog;
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (finalOpenTripLog) {
                    Intent intent = new Intent(AssignmentActivity.this, TripLoggingActivity.class);
                    intent.putExtra("trip_id", trip.tripID);
                    intent.putExtra("status", trip.status);
                    startActivity(intent);
                    return;
                }
                if (targetStatus == null) return;
                confirmTrip(trip, targetStatus, actionLabel);
            }
        });
        
        // Add pressed state for button
        confirmButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Darker green when pressed
                        confirmButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Light green when released
                        confirmButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
                        break;
                }
                return false; // Let the click listener handle the click
            }
        });

        return tripView;
    }

    private View createMaintenanceView(MaintenanceResponse.Maintenance maintenance) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View maintenanceView = inflater.inflate(R.layout.maintenance_item, maintenanceContainer, false);

        // Set maintenance data - Display fields
        TextView maintenanceIdText = maintenanceView.findViewById(R.id.maintenanceIdText);
        TextView dateAddedText = maintenanceView.findViewById(R.id.dateAddedText);
        TextView statusText = maintenanceView.findViewById(R.id.statusText);
        TextView vehicleIdText = maintenanceView.findViewById(R.id.vehicleIdText);
        TextView vehicleNameText = maintenanceView.findViewById(R.id.vehicleNameText);
        TextView licensePlateText = maintenanceView.findViewById(R.id.licensePlateText);
        TextView colorText = maintenanceView.findViewById(R.id.colorText);
        // Set display data
        maintenanceIdText.setText(maintenance.maintenanceID);
        dateAddedText.setText(maintenance.dateAdded != null ? maintenance.dateAdded : "N/A");
        statusText.setText(maintenance.maintenanceStatus);
        vehicleIdText.setText(maintenance.vehicleNumber);
        vehicleNameText.setText(maintenance.vehicleName != null ? maintenance.vehicleName : "N/A");
        licensePlateText.setText(maintenance.licensePlate != null ? maintenance.licensePlate : "N/A");
        colorText.setText(maintenance.color != null ? maintenance.color : "N/A");

        // Set status color
        switch (maintenance.maintenanceStatus.toLowerCase()) {
            case "completed":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "in progress":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                break;
            case "pending":
                statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            default:
                statusText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                break;
        }

        // Get dropdown components
        LinearLayout dropdownContent = maintenanceView.findViewById(R.id.dropdownContent);
        Button cancelButton = maintenanceView.findViewById(R.id.cancelButton);
        Button scheduledButton = maintenanceView.findViewById(R.id.scheduledButton);
        Button inProgressButton = maintenanceView.findViewById(R.id.inProgressButton);
        Button addToCompleteButton = maintenanceView.findViewById(R.id.addToCompleteButton);

        // Set up editable fields in dropdown
        setupMaintenanceDropdownFields(maintenanceView, maintenance);

        // Set up click listener for the entire maintenance card
        maintenanceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (maintenance.maintenanceStatus != null && maintenance.maintenanceStatus.equalsIgnoreCase("completed")) {
                    // Do not show dropdown for completed maintenance
                    return;
                }
                toggleMaintenanceDropdown(dropdownContent, maintenance);
            }
        });

        // Configure button click listeners
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCancellationDialog(maintenance);
            }
        });

        scheduledButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate before allowing action
                if (!isScheduledButtonValid(maintenanceView)) {
                    Toast.makeText(AssignmentActivity.this, "Please fill in Service Center, Address, and Scheduled Date", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Show confirmation dialog first, then save data after confirmation
                showStatusUpdateConfirmation(maintenanceView, maintenance, "Scheduled");
            }
        });

        inProgressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate before allowing action
                if (!isInProgressButtonValid(maintenanceView)) {
                    Toast.makeText(AssignmentActivity.this, "Please fill in Service Center, Address, Scheduled Date, and Completion Date", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Show confirmation dialog first, then save data after confirmation
                showStatusUpdateConfirmation(maintenanceView, maintenance, "In Progress");
            }
        });

        addToCompleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate before allowing action
                if (!isCompleteButtonValid(maintenanceView)) {
                    Toast.makeText(AssignmentActivity.this, "Please fill in all fields, Cost, and upload Receipt Image", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Show confirmation dialog first, then complete maintenance after confirmation
                showCompleteMaintenanceConfirmation(maintenanceView, maintenance);
            }
        });

        // Setup image upload functionality
        setupImageUpload(maintenanceView, maintenance.maintenanceID);

        // Setup button validation
        setupButtonValidation(maintenanceView, maintenance);

        return maintenanceView;
    }

    private void setupMaintenanceDropdownFields(View maintenanceView, MaintenanceResponse.Maintenance maintenance) {
        // Get editable fields
        EditText descriptionEditText = maintenanceView.findViewById(R.id.descriptionEditText);
        AutoCompleteTextView serviceTypeDropdown = maintenanceView.findViewById(R.id.serviceTypeDropdown);
        EditText serviceCenterEditText = maintenanceView.findViewById(R.id.serviceCenterEditText);
        EditText addressEditText = maintenanceView.findViewById(R.id.addressEditText);
        EditText scheduledDateEditText = maintenanceView.findViewById(R.id.scheduledDateEditText);
        EditText completionDateEditText = maintenanceView.findViewById(R.id.completionDateEditText);
        EditText costServiceEditText = maintenanceView.findViewById(R.id.costServiceEditText);

        // Set current values
        descriptionEditText.setText(maintenance.serviceNotes != null ? maintenance.serviceNotes : "");
        serviceCenterEditText.setText(maintenance.serviceCenter != null ? maintenance.serviceCenter : "");
        addressEditText.setText(maintenance.serviceCenterAddress != null ? maintenance.serviceCenterAddress : "");
        scheduledDateEditText.setText(maintenance.scheduledDate != null ? maintenance.scheduledDate : "");
        completionDateEditText.setText(maintenance.completionDate != null ? maintenance.completionDate : "");
        // Only set cost if it's not empty or "0.00", otherwise leave it empty for user input
        String costValue = maintenance.costService != null ? maintenance.costService : "";
        if (costValue.isEmpty() || "0.00".equals(costValue) || "0".equals(costValue)) {
            costServiceEditText.setText("");
        } else {
            costServiceEditText.setText(costValue);
        }

        // Setup Service Type dropdown
        setupServiceTypeDropdown(serviceTypeDropdown, maintenance.serviceType);

        // Setup date pickers
        setupDatePicker(scheduledDateEditText);
        setupDatePicker(completionDateEditText);
    }

    private void setupServiceTypeDropdown(AutoCompleteTextView dropdown, String currentValue) {
        String[] serviceTypes = {
            "Air Filter Replacement",
            "Battery Replacement", 
            "Brake Inspection",
            "Engine Check",
            "Fluid Top-Up",
            "General Inspection",
            "Oil Change",
            "Tire Replacement",
            "Tire Rotation",
            "Transmission Service",
            "Wheel Alignment",
            "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, serviceTypes);
        dropdown.setAdapter(adapter);
        dropdown.setText(currentValue != null ? currentValue : "", false);
    }

    private void setupServiceCenterDropdown(AutoCompleteTextView dropdown, String currentValue) {
        String[] serviceCenters = {
            "Auto Service Center",
            "Quick Lube Express",
            "Pro Auto Care",
            "Fleet Maintenance Hub",
            "Vehicle Service Plus",
            "Auto Repair Center",
            "Maintenance Station",
            "Service Garage Pro"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, serviceCenters);
        dropdown.setAdapter(adapter);
        dropdown.setText(currentValue != null ? currentValue : "", false);
    }

    private void setupDatePicker(EditText dateEditText) {
        dateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(dateEditText);
            }
        });
    }

    private void showDatePickerDialog(EditText dateEditText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    String selectedDate = String.format("%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                    dateEditText.setText(selectedDate);
                }
            }, year, month, day);
        datePickerDialog.show();
    }

    private void showNoTrips() {
        loadingIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        tripsContainer.setVisibility(View.GONE);
        // Don't show "No assignments yet" if maintenance records exist
        noTripsText.setVisibility(View.GONE);
    }

    private void showNoMaintenance() {
        maintenanceLoadingIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        maintenanceContainer.setVisibility(View.GONE);
        // Don't show "No maintenance assigned yet" if trips exist
        noMaintenanceText.setVisibility(View.GONE);
    }

    private void checkAndShowNoAssignments() {
        // Only show "No assignments yet" if both trips and maintenance are empty
        boolean hasTrips = tripsContainer.getVisibility() == View.VISIBLE && tripsContainer.getChildCount() > 0;
        boolean hasMaintenance = maintenanceContainer.getVisibility() == View.VISIBLE && maintenanceContainer.getChildCount() > 0;
        
        if (!hasTrips && !hasMaintenance) {
            noTripsText.setVisibility(View.VISIBLE);
        } else {
            noTripsText.setVisibility(View.GONE);
        }
    }

    private void toggleMaintenanceDropdown(LinearLayout dropdownContent, MaintenanceResponse.Maintenance maintenance) {
        if (dropdownContent.getVisibility() == View.VISIBLE) {
            // Hide dropdown
            dropdownContent.setVisibility(View.GONE);
        } else {
            // Show dropdown
            dropdownContent.setVisibility(View.VISIBLE);
            // Validate buttons when dropdown is opened
            validateButtons();
        }
    }

    private void updateMaintenanceStatus(MaintenanceResponse.Maintenance maintenance, String newStatus) {
        // Build confirmation message
        StringBuilder message = new StringBuilder();
        message.append("Update maintenance status to ").append(newStatus).append("?\n\n");
        message.append("Maintenance ID: ").append(maintenance.maintenanceID).append("\n");
        message.append("Vehicle: ").append(maintenance.vehicleNumber).append("\n");
        message.append("Service Type: ").append(maintenance.serviceType);
        
        // Show confirmation dialog
        new AlertDialog.Builder(this)
            .setTitle("Update Maintenance")
            .setMessage(message.toString())
            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Call API to update status
                    ApiClient.get().updateMaintenanceStatus(
                        "update_status", 
                        maintenance.maintenanceID, 
                        newStatus,
                        maintenance.serviceCenter != null ? maintenance.serviceCenter : "",
                        maintenance.serviceNotes != null ? maintenance.serviceNotes : "",
                        maintenance.costService != null ? maintenance.costService : ""
                    ).enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().ok) {
                                Toast.makeText(AssignmentActivity.this, "Maintenance status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                                // Hide dropdown and refresh the maintenance list
                                LinearLayout dropdownContent = maintenanceContainer.findViewById(R.id.dropdownContent);
                                if (dropdownContent != null) {
                                    dropdownContent.setVisibility(View.GONE);
                                }
                                fetchDriverMaintenance();
                            } else {
                                String msg = (response.body() != null && response.body().msg != null) ? response.body().msg : "Failed to update maintenance";
                                Toast.makeText(AssignmentActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            Toast.makeText(AssignmentActivity.this, "Update failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void toggleDropdown(LinearLayout dropdownContent, WebView mapWebView, TripResponse.Trip trip) {
        if (dropdownContent.getVisibility() == View.VISIBLE) {
            // Hide dropdown
            dropdownContent.setVisibility(View.GONE);
        } else {
            // Show dropdown and load map and locations
            dropdownContent.setVisibility(View.VISIBLE);
            loadMap(mapWebView, trip);
            
            // Load saved locations
            LinearLayout locationsContainer = dropdownContent.findViewById(R.id.locationsContainer);
            if (locationsContainer != null) {
                loadSavedLocations(locationsContainer, trip);
            }
        }
    }

    private void loadMap(WebView mapWebView, TripResponse.Trip trip) {
        // Prevent reloading if already initialized for this card
        Object initializedTag = mapWebView.getTag();
        if (initializedTag instanceof Boolean && (Boolean) initializedTag) {
            // Map already set up for this WebView; do nothing
            return;
        }
        // Mark as initialized to avoid subsequent reloads on toggle
        mapWebView.setTag(Boolean.TRUE);

        // Configure WebView
        mapWebView.getSettings().setJavaScriptEnabled(true);
        mapWebView.getSettings().setDomStorageEnabled(true);
        mapWebView.getSettings().setLoadWithOverviewMode(true);
        mapWebView.getSettings().setUseWideViewPort(true);

        // Load the map HTML file
        mapWebView.loadUrl("file:///android_asset/map.html");

        // Wait for page to load, then update map with trip addresses
        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Debug: Log the trip data
                Log.d("AssignmentActivity", "Trip data - Location: '" + trip.location + "', Address: '" + trip.address + "'");
                
                // Build list of addresses from trip data
                List<String> addresses = new ArrayList<>();
                
                // Priority: Use trip.address if available (contains full addresses from trip_tb)
                if (trip.address != null && !trip.address.trim().isEmpty()) {
                    // Check if address contains multiple addresses separated by |
                    String[] addressParts = trip.address.split("\\|");
                    Log.d("AssignmentActivity", "Found " + addressParts.length + " address parts");
                    
                    for (String part : addressParts) {
                        String trimmedPart = part.trim();
                        if (!trimmedPart.isEmpty() && !addresses.contains(trimmedPart)) {
                            addresses.add(trimmedPart);
                            Log.d("AssignmentActivity", "Added address: " + trimmedPart);
                        }
                    }
                }
                
                // Fallback: Only use trip.location if no addresses were found
                if (addresses.isEmpty() && trip.location != null && !trip.location.trim().isEmpty()) {
                    addresses.add(trip.location.trim());
                    Log.d("AssignmentActivity", "Using fallback location: " + trip.location.trim());
                }
                
                // If we have addresses, send them to the map
                if (!addresses.isEmpty()) {
                    // Send addresses to JavaScript for multiple pinning and routing
                    String addressesJson = new Gson().toJson(addresses);
                    String jsCode = "window.receiveMessage({type: 'updateMap', addresses: " + addressesJson + "});";
                    mapWebView.evaluateJavascript(jsCode, null);
                    
                    Log.d("AssignmentActivity", "Sent " + addresses.size() + " addresses to map: " + addresses);
                    
                    // Show toast to user about map loading
                    Toast.makeText(AssignmentActivity.this, 
                        "Loading " + addresses.size() + " locations on map...", 
                        Toast.LENGTH_SHORT).show();
                } else {
                    // No addresses found - just show empty map
                    Log.d("AssignmentActivity", "No addresses found, showing empty map");
                    
                    // Show toast to user
                    Toast.makeText(AssignmentActivity.this, 
                        "No trip locations to display", 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadSavedLocations(LinearLayout locationsContainer, TripResponse.Trip trip) {
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        if (username.isEmpty()) {
            return;
        }

        // Debug: Log the username being sent
        Log.d("AssignmentActivity", "Loading saved locations for username: " + username);

        // Show loading
        ProgressBar loadingIndicator = locationsContainer.findViewById(R.id.locationsLoadingIndicator);
        TextView noLocationsText = locationsContainer.findViewById(R.id.noLocationsText);
        
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
        if (noLocationsText != null) noLocationsText.setVisibility(View.GONE);

        ApiClient.get().getSavedLocations(username).enqueue(new Callback<SavedLocationsResponse>() {
            @Override
            public void onResponse(Call<SavedLocationsResponse> call, Response<SavedLocationsResponse> response) {
                Log.d("AssignmentActivity", "Saved locations API response: " + response.code());
                if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    SavedLocationsResponse locationsResponse = response.body();
                    Log.d("AssignmentActivity", "Response body: " + (locationsResponse.ok ? "OK" : "FAILED") + ", Locations: " + locationsResponse.totalLocations);
                    if (locationsResponse.ok) {
                        displaySavedLocations(locationsContainer, locationsResponse.locations);
                    } else {
                        Log.e("AssignmentActivity", "API returned error: " + locationsResponse.msg);
                        showNoLocations(locationsContainer);
                    }
                } else {
                    Log.e("AssignmentActivity", "API response not successful: " + response.code());
                    showNoLocations(locationsContainer);
                }
            }

            @Override
            public void onFailure(Call<SavedLocationsResponse> call, Throwable t) {
                Log.e("AssignmentActivity", "API call failed: " + t.getMessage());
                if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                showNoLocations(locationsContainer);
            }
        });
    }

    private void displaySavedLocations(LinearLayout locationsContainer, List<SavedLocationsResponse.Location> locations) {
        if (locations == null || locations.isEmpty()) {
            showNoLocations(locationsContainer);
            return;
        }

        // Ensure loading/empty indicators are hidden before rendering
        ProgressBar loadingIndicator = locationsContainer.findViewById(R.id.locationsLoadingIndicator);
        TextView noLocationsText = locationsContainer.findViewById(R.id.noLocationsText);
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        if (noLocationsText != null) noLocationsText.setVisibility(View.GONE);

        // Remove existing location items (keep loading indicator and no locations text)
        for (int i = locationsContainer.getChildCount() - 1; i >= 0; i--) {
            View child = locationsContainer.getChildAt(i);
            int childId = child.getId();
            if (childId != R.id.locationsLoadingIndicator && childId != R.id.noLocationsText) {
                locationsContainer.removeViewAt(i);
            }
        }

        // Add location items
        for (SavedLocationsResponse.Location location : locations) {
            Log.d("AssignmentActivity", "Creating view for location: " + location.locationName + " - " + location.locationAddress);
            
            // Handle multiple addresses in one location
            String fullAddress = location.locationAddress;
            if (fullAddress != null && fullAddress.contains("|")) {
                // Split addresses and create individual views
                String[] addresses = fullAddress.split("\\|");
                for (int i = 0; i < addresses.length; i++) {
                    String address = addresses[i].trim();
                    if (!address.isEmpty()) {
                        View addressView = createAddressView(address, i + 1, addresses.length);
                        locationsContainer.addView(addressView);
                    }
                }
            } else {
                // Single address
                View locationView = createLocationView(location);
                locationsContainer.addView(locationView);
            }
        }
    }

    private View createLocationView(SavedLocationsResponse.Location location) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View locationView = inflater.inflate(R.layout.location_step_item, null);

        // Set location data
        EditText addressText = locationView.findViewById(R.id.addressText);
        ImageView stepIcon = locationView.findViewById(R.id.stepIcon);
        ImageView dotsConnector = locationView.findViewById(R.id.dotsConnector);

        // Set the address text
        String fullAddress = location.locationAddress;
        if (fullAddress != null && !fullAddress.isEmpty()) {
            addressText.setText(fullAddress);
        } else {
            addressText.setText("No address available");
        }

        // Set icon to location pin for single address
        stepIcon.setImageResource(R.drawable.ic_location);
        stepIcon.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
        
        // Hide dots connector for single address
        dotsConnector.setVisibility(View.GONE);

        return locationView;
    }

    private View createAddressView(String address, int stepNumber, int totalSteps) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View addressView = inflater.inflate(R.layout.location_step_item, null);

        EditText addressText = addressView.findViewById(R.id.addressText);
        ImageView stepIcon = addressView.findViewById(R.id.stepIcon);
        ImageView dotsConnector = addressView.findViewById(R.id.dotsConnector);

        // Set address text
        addressText.setText(address);

        // Set icon (location pin for last step, circle for others)
        if (stepNumber == totalSteps) {
            stepIcon.setImageResource(R.drawable.ic_location);
            stepIcon.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            stepIcon.setImageResource(R.drawable.ic_circle_outline);
            stepIcon.setColorFilter(getResources().getColor(android.R.color.darker_gray));
        }

        // Hide dots connector for last item
        if (stepNumber == totalSteps) {
            dotsConnector.setVisibility(View.GONE);
        }

        return addressView;
    }

    private void showNoLocations(LinearLayout locationsContainer) {
        ProgressBar loadingIndicator = locationsContainer.findViewById(R.id.locationsLoadingIndicator);
        TextView noLocationsText = locationsContainer.findViewById(R.id.noLocationsText);
        
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        if (noLocationsText != null) noLocationsText.setVisibility(View.VISIBLE);
    }

    private void confirmTrip(TripResponse.Trip trip, String targetStatus, String buttonLabel) {
        // Build confirmation message
        StringBuilder message = new StringBuilder();
        message.append("Are you sure you want to ").append(buttonLabel.toLowerCase()).append("?\n\n");
        message.append("Trip ID: ").append(trip.tripID).append("\n");
        message.append("Location: ").append(trip.location).append("\n");
        message.append("Requester: ").append(trip.requester);
        
        // Show confirmation dialog
        new AlertDialog.Builder(this)
            .setTitle(buttonLabel)
            .setMessage(message.toString())
            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Call API to update status to target
                    ApiClient.get().updateTripStatus("update_status", trip.tripID, targetStatus).enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().ok) {
                                Toast.makeText(AssignmentActivity.this, "Trip updated to " + targetStatus, Toast.LENGTH_SHORT).show();
                                // Refresh the trips list to reflect new status
                                fetchDriverTrips();
                            } else {
                                String msg = (response.body() != null && response.body().msg != null) ? response.body().msg : "Failed to update trip";
                                Toast.makeText(AssignmentActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            Toast.makeText(AssignmentActivity.this, "Update failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    // Image picker methods - Perfect copy from ExpenseManager
    private void initializeImagePicker() {
        // No need to initialize launchers here - we'll use the same approach as ExpenseManager
    }
    
    // Image picker methods - Perfect copy from ExpenseManager
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;
    private static final int REQUEST_CAMERA_PERMISSION = 1003;
    private List<String> selectedImagePaths = new ArrayList<>();
    private Uri cameraImageUri = null;
    
    private void setupImageUpload(View maintenanceView, String maintenanceId) {
        ImageView uploadButton = maintenanceView.findViewById(R.id.uploadReceiptButton);
        LinearLayout selectedImagesContainer = maintenanceView.findViewById(R.id.selectedImagesContainer);
        
        if (uploadButton != null) {
            uploadButton.setOnClickListener(v -> {
                Log.d("AssignmentActivity", "Upload button clicked! Current images: " + selectedImagePaths.size());
                if (selectedImagePaths.size() >= 5) {
                    Toast.makeText(this, "Maximum 5 images allowed", Toast.LENGTH_SHORT).show();
                    return;
                }
                showImagePickerOptions();
            });
        }
        
        // Display selected images
        displaySelectedImages(selectedImagesContainer);
    }
    
    private void showImagePickerOptions() {
        // Check and request media permissions first
        if (!checkMediaPermissions()) {
            requestMediaPermissions();
            return;
        }
        
        // Create multiple intents to ensure all gallery apps appear
        List<Intent> intents = new ArrayList<>();
        
        // Intent 1: ACTION_GET_CONTENT (shows most gallery apps)
        Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getContentIntent.setType("image/*");
        getContentIntent.addCategory(Intent.CATEGORY_OPENABLE);
        intents.add(getContentIntent);
        
        // Intent 2: ACTION_PICK (alternative gallery picker)
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        intents.add(pickIntent);
        
        // Intent 3: Camera (if permission granted)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intents.add(cameraIntent);
        } else {
            // Request camera permission but still show gallery options
            ActivityCompat.requestPermissions(this, 
                new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
        
        // Create chooser with all intents
        Intent chooserIntent = Intent.createChooser(intents.get(0), "Select Receipt Image");
        if (intents.size() > 1) {
            Intent[] extraIntents = intents.subList(1, intents.size()).toArray(new Intent[0]);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
        }
        
        try {
            startActivityForResult(chooserIntent, REQUEST_IMAGE_PICK);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening image picker: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private boolean checkMediaPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - use new media permissions
            return ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_IMAGES") 
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 and below - use legacy storage permissions
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestMediaPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - request new media permissions
            ActivityCompat.requestPermissions(this, 
                new String[]{"android.permission.READ_MEDIA_IMAGES"}, REQUEST_CAMERA_PERMISSION);
        } else {
            // Android 12 and below - request legacy storage permissions
            ActivityCompat.requestPermissions(this, 
                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            handleImagePickerResult(data);
        }
    }
    
    private void handleImagePickerResult(Intent data) {
        Bitmap bitmap = null;
        String imagePath = null;
        
        // First try to get thumbnail from camera (most reliable)
        if (data != null && data.getExtras() != null) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            if (thumbnail != null) {
                bitmap = thumbnail;
                imagePath = saveImageToInternalStorage(thumbnail);
                Toast.makeText(this, "Image captured successfully", Toast.LENGTH_SHORT).show();
            }
        }
        
        // If no thumbnail, try to get URI from gallery
        if (bitmap == null && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    // Load the selected image
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                    imagePath = saveImageToInternalStorage(bitmap);
                    Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        
        // Add image to list and display
        Log.d("AssignmentActivity", "Processing image result - bitmap: " + (bitmap != null) + ", imagePath: " + imagePath);
        if (bitmap != null && imagePath != null) {
            Log.d("AssignmentActivity", "Image selected successfully. Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            selectedImagePaths.add(imagePath);
            addImageToContainer(bitmap, imagePath);
        } else {
            Log.d("AssignmentActivity", "Failed to get image - bitmap: " + (bitmap != null) + ", imagePath: " + imagePath);
        }
    }
    
    private String saveImageToInternalStorage(Bitmap bitmap) {
        try {
            String timeStamp = String.valueOf(System.currentTimeMillis());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir("Pictures");
            if (storageDir != null) {
                File image = File.createTempFile(imageFileName, ".jpg", storageDir);
                FileOutputStream fos = new FileOutputStream(image);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
                return image.getAbsolutePath();
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }
    
    private void addImageToContainer(Bitmap bitmap, String imagePath) {
        // Update all maintenance views with the new image
        for (int i = 0; i < maintenanceContainer.getChildCount(); i++) {
            View maintenanceView = maintenanceContainer.getChildAt(i);
            LinearLayout selectedImagesContainer = maintenanceView.findViewById(R.id.selectedImagesContainer);
            if (selectedImagesContainer != null) {
                addImageToSpecificContainer(bitmap, imagePath, selectedImagesContainer);
            }
        }
    }
    
    private void addImageToSpecificContainer(Bitmap bitmap, String imagePath, LinearLayout selectedImagesContainer) {
        if (selectedImagesContainer == null) {
            Log.e("AssignmentActivity", "selectedImagesContainer is null!");
            return;
        }
        
        Log.d("AssignmentActivity", "Adding image to container. Container child count before: " + selectedImagesContainer.getChildCount());
        
        // Convert dp to pixels
        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int) (80 * density); // 80dp to pixels
        int marginPx = (int) (8 * density); // 8dp to pixels
        
        // Create a FrameLayout container - EXACTLY 80x80dp in pixels
        android.widget.FrameLayout imageContainer = new android.widget.FrameLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(sizePx, sizePx);
        containerParams.setMargins(0, 0, marginPx, 0);
        imageContainer.setLayoutParams(containerParams);
        
        // FORCE resize bitmap to exactly 80x80dp in pixels
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true);
        
        // Create image view - EXACTLY 80x80dp in pixels, NO EXCEPTIONS
        ImageView imageView = new ImageView(this);
        android.widget.FrameLayout.LayoutParams imageParams = new android.widget.FrameLayout.LayoutParams(sizePx, sizePx);
        imageView.setLayoutParams(imageParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY); // Force exact fit
        imageView.setImageBitmap(resizedBitmap);
        imageView.setPadding(0, 0, 0, 0);
        imageView.setAdjustViewBounds(false);
        imageView.setMinimumWidth(sizePx);
        imageView.setMinimumHeight(sizePx);
        imageView.setMaxWidth(sizePx);
        imageView.setMaxHeight(sizePx);
        // Tap to preview full screen
        imageView.setOnClickListener(v -> showImagePreview(imagePath));
        
        // Create remove button
        ImageView removeButton = new ImageView(this);
        int removeButtonSizePx = (int) (28 * density); // 28dp to pixels
        android.widget.FrameLayout.LayoutParams removeParams = new android.widget.FrameLayout.LayoutParams(removeButtonSizePx, removeButtonSizePx);
        removeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        removeParams.setMargins(0, (int)(2 * density), (int)(2 * density), 0);
        removeButton.setLayoutParams(removeParams);
        removeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        removeButton.setBackgroundColor(0xCCFF0000);
        removeButton.setPadding(4, 4, 4, 4);
        removeButton.setColorFilter(0xFFFFFFFF);
        removeButton.setClickable(true);
        
        // Add views to container
        imageContainer.addView(imageView);
        imageContainer.addView(removeButton);
        
        // Set up remove functionality
        removeButton.setOnClickListener(v -> {
            selectedImagePaths.remove(imagePath);
            selectedImagesContainer.removeView(imageContainer);
            // Trigger button validation after removing image
            validateButtons();
        });
        
        selectedImagesContainer.addView(imageContainer);
        
        // Trigger button validation after adding image
        validateButtons();
        
        // Force refresh the container
        selectedImagesContainer.invalidate();
        selectedImagesContainer.requestLayout();
    }
    
    private void displaySelectedImages(LinearLayout container) {
        if (container == null) return;
        
        container.removeAllViews();
        
        // Re-add all selected images
        for (String imagePath : selectedImagePaths) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    addImageToSpecificContainer(bitmap, imagePath, container);
                }
            } catch (Exception e) {
                Log.e("AssignmentActivity", "Error loading image: " + imagePath, e);
            }
        }
    }
    
    private void showImagePreview(String imagePath) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null) {
                Toast.makeText(this, "Unable to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Root overlay fills the whole screen with semi-transparent black
            android.widget.FrameLayout root = new android.widget.FrameLayout(this);
            root.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            ));
            root.setBackgroundColor(0x33000000);
            root.setClickable(true);
            root.setFocusable(true);

            // Centered preview ImageView
            ImageView preview = new ImageView(this);
            preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
            preview.setAdjustViewBounds(true);

            int screenW = getResources().getDisplayMetrics().widthPixels;
            int screenH = getResources().getDisplayMetrics().heightPixels;
            int maxW = (int)(screenW * 0.95f);
            int maxH = (int)(screenH * 0.95f);
            preview.setMaxWidth(maxW);
            preview.setMaxHeight(maxH);

            preview.setImageBitmap(bitmap);

            android.widget.FrameLayout.LayoutParams centerParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            );
            centerParams.gravity = android.view.Gravity.CENTER;
            root.addView(preview, centerParams);

            // Fullscreen dialog
            android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(root);
            dialog.getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            dialog.show();

            // Tap anywhere to close
            root.setOnClickListener(v -> dialog.dismiss());

        } catch (Exception e) {
            Toast.makeText(this, "Error displaying image", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupButtonValidation(View maintenanceView, MaintenanceResponse.Maintenance maintenance) {
        // Get all required fields
        EditText serviceCenterEditText = maintenanceView.findViewById(R.id.serviceCenterEditText);
        EditText addressEditText = maintenanceView.findViewById(R.id.addressEditText);
        EditText scheduledDateEditText = maintenanceView.findViewById(R.id.scheduledDateEditText);
        EditText completionDateEditText = maintenanceView.findViewById(R.id.completionDateEditText);
        EditText costServiceEditText = maintenanceView.findViewById(R.id.costServiceEditText);
        LinearLayout selectedImagesContainer = maintenanceView.findViewById(R.id.selectedImagesContainer);
        
        // Get buttons
        Button scheduledButton = maintenanceView.findViewById(R.id.scheduledButton);
        Button inProgressButton = maintenanceView.findViewById(R.id.inProgressButton);
        Button addToCompleteButton = maintenanceView.findViewById(R.id.addToCompleteButton);
        
        // Create validation listener
        View.OnFocusChangeListener validationListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) { // Only validate when field loses focus
                    validateButtons();
                }
            }
        };
        
        // Add validation listeners to all fields
        serviceCenterEditText.setOnFocusChangeListener(validationListener);
        addressEditText.setOnFocusChangeListener(validationListener);
        scheduledDateEditText.setOnFocusChangeListener(validationListener);
        completionDateEditText.setOnFocusChangeListener(validationListener);
        costServiceEditText.setOnFocusChangeListener(validationListener);
        
        // Add TextWatcher for real-time validation
        android.text.TextWatcher textWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                validateButtons();
            }
        };
        
        serviceCenterEditText.addTextChangedListener(textWatcher);
        addressEditText.addTextChangedListener(textWatcher);
        scheduledDateEditText.addTextChangedListener(textWatcher);
        completionDateEditText.addTextChangedListener(textWatcher);
        costServiceEditText.addTextChangedListener(textWatcher);
        
        // Store references for validation method
        maintenanceView.setTag(R.id.serviceCenterEditText, serviceCenterEditText);
        maintenanceView.setTag(R.id.addressEditText, addressEditText);
        maintenanceView.setTag(R.id.scheduledDateEditText, scheduledDateEditText);
        maintenanceView.setTag(R.id.completionDateEditText, completionDateEditText);
        maintenanceView.setTag(R.id.costServiceEditText, costServiceEditText);
        maintenanceView.setTag(R.id.selectedImagesContainer, selectedImagesContainer);
        maintenanceView.setTag(R.id.scheduledButton, scheduledButton);
        maintenanceView.setTag(R.id.inProgressButton, inProgressButton);
        maintenanceView.setTag(R.id.addToCompleteButton, addToCompleteButton);
        
        // Initial validation
        validateButtons();
    }
    
    private void validateButtons() {
        // Find the maintenance view that's currently expanded
        for (int i = 0; i < maintenanceContainer.getChildCount(); i++) {
            View maintenanceView = maintenanceContainer.getChildAt(i);
            LinearLayout dropdownContent = maintenanceView.findViewById(R.id.dropdownContent);
            
            if (dropdownContent.getVisibility() == View.VISIBLE) {
                // Get all required fields directly from the view
                EditText serviceCenterEditText = maintenanceView.findViewById(R.id.serviceCenterEditText);
                EditText addressEditText = maintenanceView.findViewById(R.id.addressEditText);
                EditText scheduledDateEditText = maintenanceView.findViewById(R.id.scheduledDateEditText);
                EditText completionDateEditText = maintenanceView.findViewById(R.id.completionDateEditText);
                EditText costServiceEditText = maintenanceView.findViewById(R.id.costServiceEditText);
                LinearLayout selectedImagesContainer = maintenanceView.findViewById(R.id.selectedImagesContainer);
                
                // Get buttons
                Button scheduledButton = maintenanceView.findViewById(R.id.scheduledButton);
                Button inProgressButton = maintenanceView.findViewById(R.id.inProgressButton);
                Button addToCompleteButton = maintenanceView.findViewById(R.id.addToCompleteButton);
                
                if (serviceCenterEditText == null || addressEditText == null || scheduledDateEditText == null || 
                    completionDateEditText == null || costServiceEditText == null || selectedImagesContainer == null ||
                    scheduledButton == null || inProgressButton == null || addToCompleteButton == null) {
                    return;
                }
                
                // Check if images are uploaded
                boolean hasImages = selectedImagesContainer.getChildCount() > 0;
                
                // Get field values
                String serviceCenter = serviceCenterEditText.getText().toString().trim();
                String address = addressEditText.getText().toString().trim();
                String scheduledDate = scheduledDateEditText.getText().toString().trim();
                String completionDate = completionDateEditText.getText().toString().trim();
                String cost = costServiceEditText.getText().toString().trim();
                
                Log.d("AssignmentActivity", "Validation - ServiceCenter: '" + serviceCenter + "', Address: '" + address + "', Scheduled: '" + scheduledDate + "', Completion: '" + completionDate + "', Cost: '" + cost + "', HasImages: " + hasImages);
                
                // Validate Scheduled button: Service Center, Address, Scheduled Date
                boolean scheduledValid = !serviceCenter.isEmpty() && !address.isEmpty() && !scheduledDate.isEmpty();
                scheduledButton.setEnabled(scheduledValid);
                scheduledButton.setAlpha(scheduledValid ? 1.0f : 0.5f);
                
                // Validate In Progress button: Service Center, Address, Scheduled Date, Completion Date
                boolean inProgressValid = scheduledValid && !completionDate.isEmpty();
                inProgressButton.setEnabled(inProgressValid);
                inProgressButton.setAlpha(inProgressValid ? 1.0f : 0.5f);
                
                // Validate Add to Complete button: All fields + Cost + Receipt Image
                boolean completeValid = inProgressValid && !cost.isEmpty() && hasImages;
                addToCompleteButton.setEnabled(completeValid);
                addToCompleteButton.setAlpha(completeValid ? 1.0f : 0.5f);
                
                Log.d("AssignmentActivity", "Button states - Scheduled: " + scheduledValid + ", InProgress: " + inProgressValid + ", Complete: " + completeValid);
                
                break;
            }
        }
    }
    
    private boolean isScheduledButtonValid(View maintenanceView) {
        EditText serviceCenterEditText = maintenanceView.findViewById(R.id.serviceCenterEditText);
        EditText addressEditText = maintenanceView.findViewById(R.id.addressEditText);
        EditText scheduledDateEditText = maintenanceView.findViewById(R.id.scheduledDateEditText);
        
        if (serviceCenterEditText == null || addressEditText == null || scheduledDateEditText == null) {
            return false;
        }
        
        String serviceCenter = serviceCenterEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String scheduledDate = scheduledDateEditText.getText().toString().trim();
        
        return !serviceCenter.isEmpty() && !address.isEmpty() && !scheduledDate.isEmpty();
    }
    
    private boolean isInProgressButtonValid(View maintenanceView) {
        EditText serviceCenterEditText = maintenanceView.findViewById(R.id.serviceCenterEditText);
        EditText addressEditText = maintenanceView.findViewById(R.id.addressEditText);
        EditText scheduledDateEditText = maintenanceView.findViewById(R.id.scheduledDateEditText);
        EditText completionDateEditText = maintenanceView.findViewById(R.id.completionDateEditText);
        
        if (serviceCenterEditText == null || addressEditText == null || scheduledDateEditText == null || completionDateEditText == null) {
            return false;
        }
        
        String serviceCenter = serviceCenterEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String scheduledDate = scheduledDateEditText.getText().toString().trim();
        String completionDate = completionDateEditText.getText().toString().trim();
        
        return !serviceCenter.isEmpty() && !address.isEmpty() && !scheduledDate.isEmpty() && !completionDate.isEmpty();
    }
    
    private boolean isCompleteButtonValid(View maintenanceView) {
        EditText serviceCenterEditText = maintenanceView.findViewById(R.id.serviceCenterEditText);
        EditText addressEditText = maintenanceView.findViewById(R.id.addressEditText);
        EditText scheduledDateEditText = maintenanceView.findViewById(R.id.scheduledDateEditText);
        EditText completionDateEditText = maintenanceView.findViewById(R.id.completionDateEditText);
        EditText costServiceEditText = maintenanceView.findViewById(R.id.costServiceEditText);
        LinearLayout selectedImagesContainer = maintenanceView.findViewById(R.id.selectedImagesContainer);
        
        if (serviceCenterEditText == null || addressEditText == null || scheduledDateEditText == null || 
            completionDateEditText == null || costServiceEditText == null || selectedImagesContainer == null) {
            return false;
        }
        
        String serviceCenter = serviceCenterEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String scheduledDate = scheduledDateEditText.getText().toString().trim();
        String completionDate = completionDateEditText.getText().toString().trim();
        String cost = costServiceEditText.getText().toString().trim();
        boolean hasImages = selectedImagesContainer.getChildCount() > 0;
        
        return !serviceCenter.isEmpty() && !address.isEmpty() && !scheduledDate.isEmpty() && 
               !completionDate.isEmpty() && !cost.isEmpty() && hasImages;
    }
    
    private void showCancellationDialog(MaintenanceResponse.Maintenance maintenance) {
        // Create custom dialog with custom layout
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        
        // Inflate custom layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.cancel_maintenance_dialog, null);
        builder.setView(dialogView);
        
        // Get views from custom layout
        final EditText reasonInput = dialogView.findViewById(R.id.reasonInput);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);
        
        // Create dialog
        android.app.AlertDialog dialog = builder.create();
        
        // Set button click listeners
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String reason = reasonInput.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(AssignmentActivity.this, "Please enter a cancellation reason", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Update maintenance status with cancellation reason
                updateMaintenanceStatusWithReason(maintenance, "Cancelled", reason);
                dialog.dismiss();
            }
        });
        
        // Show dialog
        dialog.show();
        
        // Focus on the input field and set cursor to start
        reasonInput.requestFocus();
        
        // Post a runnable to ensure the view is fully laid out before setting selection
        reasonInput.post(new Runnable() {
            @Override
            public void run() {
                reasonInput.setSelection(0);
            }
        });
    }
    
    private void showCompleteMaintenanceConfirmation(View maintenanceView, MaintenanceResponse.Maintenance maintenance) {
        // Build confirmation message
        StringBuilder message = new StringBuilder();
        message.append("Complete this maintenance task?\n\n");
        message.append("This will:\n");
        message.append("• Update maintenance status to 'Completed'\n");
        message.append("• Set driver status to 'Available'\n");
        message.append("• Set vehicle status to 'Available'\n\n");
        message.append("Maintenance ID: ").append(maintenance.maintenanceID).append("\n");
        message.append("Vehicle: ").append(maintenance.vehicleNumber).append("\n");
        message.append("Service Type: ").append(maintenance.serviceType);

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Complete Maintenance")
                .setMessage(message.toString())
                .setPositiveButton("Complete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // After confirmation, complete the maintenance
                        completeMaintenance(maintenanceView, maintenance);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStatusUpdateConfirmation(View maintenanceView, MaintenanceResponse.Maintenance maintenance, String newStatus) {
        // Build confirmation message
        StringBuilder message = new StringBuilder();
        message.append("Update maintenance status to ").append(newStatus).append("?\n\n");
        message.append("Maintenance ID: ").append(maintenance.maintenanceID).append("\n");
        message.append("Vehicle: ").append(maintenance.vehicleNumber).append("\n");
        message.append("Service Type: ").append(maintenance.serviceType);

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Update Maintenance")
                .setMessage(message.toString())
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // After confirmation, save the form data and update status
                        saveMaintenanceDetails(maintenanceView, maintenance, newStatus);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeMaintenance(View maintenanceView, MaintenanceResponse.Maintenance maintenance) {
        // Get form data
        EditText serviceCenterEditText = maintenanceView.findViewById(R.id.serviceCenterEditText);
        EditText addressEditText = maintenanceView.findViewById(R.id.addressEditText);
        EditText scheduledDateEditText = maintenanceView.findViewById(R.id.scheduledDateEditText);
        EditText completionDateEditText = maintenanceView.findViewById(R.id.completionDateEditText);
        EditText costServiceEditText = maintenanceView.findViewById(R.id.costServiceEditText);
        EditText descriptionEditText = maintenanceView.findViewById(R.id.descriptionEditText);
        
        String serviceCenter = serviceCenterEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String scheduledDate = scheduledDateEditText.getText().toString().trim();
        String completionDate = completionDateEditText.getText().toString().trim();
        String costService = costServiceEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        
        // Get receipt images
        String receiptImages = getReceiptImagesString(maintenanceView);
        
        // Show loading
        maintenanceLoadingIndicator.setVisibility(View.VISIBLE);
        
        // Call API to complete maintenance (updates maintenance, driver, and vehicle status)
        ApiClient.get().updateMaintenanceDetails(
                "complete_maintenance",
                maintenance.maintenanceID,
                serviceCenter,
                address,
                scheduledDate,
                completionDate,
                costService,
                description,
                receiptImages
        ).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                maintenanceLoadingIndicator.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().ok) {
                        Toast.makeText(AssignmentActivity.this, "Maintenance completed successfully! Driver and vehicle are now available.", Toast.LENGTH_LONG).show();
                        // Refresh the maintenance list
                        fetchDriverMaintenance();
                    } else {
                        Toast.makeText(AssignmentActivity.this, "Failed to complete maintenance: " + response.body().msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AssignmentActivity.this, "Failed to complete maintenance", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                maintenanceLoadingIndicator.setVisibility(View.GONE);
                Toast.makeText(AssignmentActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveMaintenanceDetails(View maintenanceView, MaintenanceResponse.Maintenance maintenance, String newStatus) {
        // Get form data
        EditText serviceCenterEditText = maintenanceView.findViewById(R.id.serviceCenterEditText);
        EditText addressEditText = maintenanceView.findViewById(R.id.addressEditText);
        EditText scheduledDateEditText = maintenanceView.findViewById(R.id.scheduledDateEditText);
        EditText completionDateEditText = maintenanceView.findViewById(R.id.completionDateEditText);
        EditText costServiceEditText = maintenanceView.findViewById(R.id.costServiceEditText);
        EditText descriptionEditText = maintenanceView.findViewById(R.id.descriptionEditText);
        
        String serviceCenter = serviceCenterEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String scheduledDate = scheduledDateEditText.getText().toString().trim();
        String completionDate = completionDateEditText.getText().toString().trim();
        String costService = costServiceEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        
        // Get receipt images
        String receiptImages = getReceiptImagesString(maintenanceView);
        
        // Show loading
        maintenanceLoadingIndicator.setVisibility(View.VISIBLE);
        
        // Call API to save details
        ApiClient.get().updateMaintenanceDetails(
                "update_details",
                maintenance.maintenanceID,
                serviceCenter,
                address,
                scheduledDate,
                completionDate,
                costService,
                description,
                receiptImages
        ).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                maintenanceLoadingIndicator.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().ok) {
                        // After saving details, update the status (without confirmation dialog)
                        updateMaintenanceStatusDirect(maintenance, newStatus);
                    } else {
                        Toast.makeText(AssignmentActivity.this, "Failed to save maintenance details: " + response.body().msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AssignmentActivity.this, "Failed to save maintenance details", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                maintenanceLoadingIndicator.setVisibility(View.GONE);
                Toast.makeText(AssignmentActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private String getReceiptImagesString(View maintenanceView) {
        // Get the receipt images container
        LinearLayout receiptImagesContainer = maintenanceView.findViewById(R.id.receiptImagesContainer);
        
        if (receiptImagesContainer.getChildCount() == 0) {
            return "";
        }
        
        StringBuilder imagePaths = new StringBuilder();
        for (int i = 0; i < receiptImagesContainer.getChildCount(); i++) {
            View child = receiptImagesContainer.getChildAt(i);
            if (child instanceof ImageView) {
                ImageView imageView = (ImageView) child;
                String imagePath = (String) imageView.getTag();
                if (imagePath != null && !imagePath.isEmpty()) {
                    if (imagePaths.length() > 0) {
                        imagePaths.append(",");
                    }
                    imagePaths.append(imagePath);
                }
            }
        }
        
        return imagePaths.toString();
    }

    private void updateMaintenanceStatusDirect(MaintenanceResponse.Maintenance maintenance, String newStatus) {
        // Show loading indicator
        maintenanceLoadingIndicator.setVisibility(View.VISIBLE);

        // Call API to update status directly (no confirmation dialog)
        ApiClient.get().updateMaintenanceStatus(
                "update_status",
                maintenance.maintenanceID,
                newStatus,
                maintenance.serviceCenter != null ? maintenance.serviceCenter : "",
                maintenance.serviceNotes != null ? maintenance.serviceNotes : "",
                maintenance.costService != null ? maintenance.costService : ""
        ).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                maintenanceLoadingIndicator.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().ok) {
                        Toast.makeText(AssignmentActivity.this, "Maintenance status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                        // Refresh the maintenance list
                        fetchDriverMaintenance();
                    } else {
                        Toast.makeText(AssignmentActivity.this, "Failed to update status: " + response.body().msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AssignmentActivity.this, "Failed to update maintenance status", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                maintenanceLoadingIndicator.setVisibility(View.GONE);
                Toast.makeText(AssignmentActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMaintenanceStatusWithReason(MaintenanceResponse.Maintenance maintenance, String newStatus, String reason) {
        // Build confirmation message
        StringBuilder message = new StringBuilder();
        message.append("Update maintenance status to ").append(newStatus).append("?\n\n");
        message.append("Maintenance ID: ").append(maintenance.maintenanceID).append("\n");
        message.append("Vehicle: ").append(maintenance.vehicleNumber).append("\n");
        message.append("Service Type: ").append(maintenance.serviceType != null ? maintenance.serviceType : "N/A").append("\n");
        message.append("Cancellation Reason: ").append(reason);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Update Maintenance");
        builder.setMessage(message.toString());
        builder.setPositiveButton("Confirm", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                // Update the maintenance with the cancellation reason
                updateMaintenanceStatus(maintenance, newStatus, reason);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void updateMaintenanceStatus(MaintenanceResponse.Maintenance maintenance, String newStatus, String reason) {
        // Show loading indicator
        maintenanceLoadingIndicator.setVisibility(View.VISIBLE);

        // Prepare API call with cancellation reason
        String serviceCenter = maintenance.serviceCenter != null ? maintenance.serviceCenter : "";
        String serviceNotes = reason != null ? reason : (maintenance.serviceNotes != null ? maintenance.serviceNotes : "");
        String costService = maintenance.costService != null ? maintenance.costService : "";

        // Use the existing ApiClient pattern
        ApiClient.get().updateMaintenanceStatus(
                "update_status",
                maintenance.maintenanceID,
                newStatus,
                serviceCenter,
                serviceNotes,
                costService
        ).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                maintenanceLoadingIndicator.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().ok) {
                        Toast.makeText(AssignmentActivity.this, "Maintenance status updated successfully", Toast.LENGTH_SHORT).show();
                        // Refresh maintenance data
                        fetchDriverMaintenance();
                    } else {
                        Toast.makeText(AssignmentActivity.this, "Failed to update maintenance status: " + response.body().msg, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AssignmentActivity.this, "Failed to update maintenance status", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                maintenanceLoadingIndicator.setVisibility(View.GONE);
                Toast.makeText(AssignmentActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
