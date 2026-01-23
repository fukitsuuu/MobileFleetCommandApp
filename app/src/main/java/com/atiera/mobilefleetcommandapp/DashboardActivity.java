package com.atiera.mobilefleetcommandapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    private ExpenseManager expenseManager;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_CAMERA_PERMISSION_ASKED = "camera_permission_asked";
    private static final int REQUEST_CAMERA_PERMISSION_FIRST_TIME = 1004;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1005;

    // Dashboard active trips UI
    private LinearLayout activeTripsContainer;
    private ProgressBar dashboardLoading;
    private TextView noActiveTripsText;
    
    // Heartbeat handler for active status
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private static final long HEARTBEAT_INTERVAL = 60000; // 1 minute

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Set status bar color BEFORE setting content view
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.topbar_bg));

        // Make status bar icons visible (light icons on dark background)
        getWindow().getDecorView().setSystemUiVisibility(
            getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        setContentView(R.layout.activity_dashboard);

        // Initialize drawer components
        initializeDrawerComponents();

        // Check for first-time camera permission request
        checkAndRequestCameraPermission();

        // Initialize dashboard active trips views
        activeTripsContainer = findViewById(R.id.activeTripsContainer);
        dashboardLoading = findViewById(R.id.dashboardLoading);
        noActiveTripsText = findViewById(R.id.noActiveTripsText);

        // Fetch active trips initially - this will also start location service if trips found
        fetchActiveTrips();
        
        // Initialize and start heartbeat
        initializeHeartbeat();
        startHeartbeat();

        // Set up modern back press handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    finish();
                }
            }
        });
    }

    protected void initializeDrawerComponents() {
        // Initialize drawer components first
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        // Set up navigation view listener for sidebar functionality
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            // Set Dashboard as checked since this is the Dashboard page
            navigationView.setCheckedItem(R.id.nav_dashboard);

            // Populate driver info in nav header
            View header = navigationView.getHeaderView(0);
            ImageView avatar = header.findViewById(R.id.avatarImage);
            TextView nameText = header.findViewById(R.id.driverName);
            TextView idText = header.findViewById(R.id.driverId);

            // Get saved username from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            String username = prefs.getString("username", null);
            if (username != null) {
                // Fallback defaults so header is not empty while loading
                nameText.setText(username);
                idText.setText("");
                
                ApiClient.get().getDriverProfile(username).enqueue(new retrofit2.Callback<DriverProfileResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<DriverProfileResponse> call, retrofit2.Response<DriverProfileResponse> response) {
                        DriverProfileResponse body = response.body();
                        if (response.isSuccessful() && body != null && body.ok && body.driver != null) {
                            if (body.driver.fullName != null && !body.driver.fullName.isEmpty()) {
                                nameText.setText(body.driver.fullName);
                            }
                            if (body.driver.idNumber != null && !body.driver.idNumber.isEmpty()) {
                                idText.setText("Driver ID: " + body.driver.idNumber);
                                // Save driverID to SharedPreferences for location tracking service and heartbeat
                                prefs.edit().putString("driverID", body.driver.idNumber).apply();
                                // Send immediate heartbeat now that driverID is available
                                sendHeartbeat();
                            }
                            if (body.driver.imageUrl != null && !body.driver.imageUrl.isEmpty()) {
                                try {
                                    com.bumptech.glide.Glide.with(DashboardActivity.this)
                                        .load(body.driver.imageUrl)
                                        .placeholder(R.drawable.circle_background)
                                        .error(R.drawable.circle_background)
                                        .circleCrop()
                                        .into(avatar);
                                } catch (Exception ignored) {}
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<DriverProfileResponse> call, Throwable t) {
                        // keep fallback username
                    }
                });
            }
        }

        // Set up click listeners for toolbar elements
        ImageView hamburgerMenu = findViewById(R.id.hamburgerMenu);
        ImageView notifications = findViewById(R.id.notifications);
        ImageView messageIcon = findViewById(R.id.messageIcon);

        hamburgerMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open/close the drawer
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        notifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, NotificationsActivity.class);
                startActivity(intent);
            }
        });

        messageIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, MessagesActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Always navigate to Dashboard when Dashboard button is clicked
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_profile) {
            // Launch Profile Activity
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_assignment) {
            // Launch Assignment Activity
            Intent intent = new Intent(this, AssignmentActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_trip_logging) {
            // Launch Trip Logging Activity
            Intent intent = new Intent(this, TripLoggingActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_report_issue) {
            // Launch Report Issue Activity
            Intent intent = new Intent(this, ReportIssueActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            // Logout user
            logout();
        }

        // Close the drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        // Clear login session
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Go back to login screen
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchActiveTrips() {
        if (activeTripsContainer == null) return;
        dashboardLoading.setVisibility(View.VISIBLE);
        noActiveTripsText.setVisibility(View.GONE);
        activeTripsContainer.removeAllViews();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String username = prefs.getString(KEY_USERNAME, "");
        if (username == null || username.isEmpty()) {
            dashboardLoading.setVisibility(View.GONE);
            noActiveTripsText.setVisibility(View.VISIBLE);
            return;
        }

        ApiClient.get().getDriverTrips(username).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                dashboardLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().ok) {
                    List<TripResponse.Trip> trips = response.body().trips;
                    List<TripResponse.Trip> active = new ArrayList<>();
                    if (trips != null) {
                        for (TripResponse.Trip t : trips) {
                            if (t.status == null) continue;
                            String s = t.status.toLowerCase();
                            if (s.equals("preparing") || s.equals("departed") || s.equals("in transit") || s.equals("picked up") || s.equals("delivered")) {
                                active.add(t);
                            }
                        }
                    }
                    renderActiveTrips(active);
                } else {
                    noActiveTripsText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                dashboardLoading.setVisibility(View.GONE);
                noActiveTripsText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void renderActiveTrips(List<TripResponse.Trip> activeTrips) {
        if (activeTrips == null || activeTrips.isEmpty()) {
            noActiveTripsText.setVisibility(View.VISIBLE);
            // No active trips - service will stop itself after checking
            Log.d("DashboardActivity", "No active trips found");
            return;
        }
        noActiveTripsText.setVisibility(View.GONE);
        Log.d("DashboardActivity", "Found " + activeTrips.size() + " active trip(s) - checking location permission");
        
        // Driver has active trips - request location permission and start tracking service
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            Log.d("DashboardActivity", "Location permission granted - starting service");
            startLocationTrackingService();
        } else {
            Log.d("DashboardActivity", "Location permission not granted - requesting permission");
            // Request location permission when active trips are found
            ActivityCompat.requestPermissions(this,
                new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (TripResponse.Trip trip : activeTrips) {
            View card = inflater.inflate(R.layout.trip_item, activeTripsContainer, false);

            TextView tripIdText = card.findViewById(R.id.tripIdText);
            TextView requesterText = card.findViewById(R.id.requesterText);
            TextView locationText = card.findViewById(R.id.locationText);
            TextView urgencyText = card.findViewById(R.id.urgencyText);
            TextView statusText = card.findViewById(R.id.statusText);
            TextView vehicleText = card.findViewById(R.id.vehicleText);
            TextView distanceText = card.findViewById(R.id.distanceText);
            TextView timeText = card.findViewById(R.id.timeText);
            TextView budgetText = card.findViewById(R.id.budgetText);

            LinearLayout dropdownContent = card.findViewById(R.id.dropdownContent);
            WebView mapWebView = card.findViewById(R.id.mapWebView);
            Button confirmButton = card.findViewById(R.id.confirmButton);

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

            // Description support
            TextView descriptionText = card.findViewById(R.id.descriptionText);
            LinearLayout descriptionContainer = card.findViewById(R.id.descriptionContainer);
            if (trip.description != null && !trip.description.trim().isEmpty()) {
                descriptionText.setText(trip.description.trim());
                descriptionContainer.setVisibility(View.VISIBLE);
            } else {
                descriptionContainer.setVisibility(View.GONE);
            }

            // Expenses area handling
            LinearLayout expensesArea = card.findViewById(R.id.expensesArea);
            if (expensesArea != null) {
                if ("Delivered".equals(trip.status)) {
                    expensesArea.setVisibility(View.VISIBLE);
                    setupExpensesArea(card);
                } else {
                    expensesArea.setVisibility(View.GONE);
                }
            }

            // Urgency color
            if (trip.urgency != null) {
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
            }

            // Status color
            if (trip.status != null) {
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
            }

            // Toggle dropdown and map
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dropdownContent.getVisibility() == View.VISIBLE) {
                        dropdownContent.setVisibility(View.GONE);
                    } else {
                        dropdownContent.setVisibility(View.VISIBLE);
                        loadMapForDashboard(mapWebView, trip);
                        LinearLayout locationsContainer = dropdownContent.findViewById(R.id.locationsContainer);
                        if (locationsContainer != null) {
                            loadSavedLocationsForDashboard(locationsContainer);
                        }
                    }
                }
            });

            // Configure confirm button label and behavior based on status (same mapping as Assignment)
            String currentStatus = trip.status != null ? trip.status : "";
            String lower = currentStatus.toLowerCase();
            String buttonLabel = "Confirm Trip";
            String nextStatus = null;
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
                buttonLabel = "Completed";
                nextStatus = null;
                confirmButton.setEnabled(false);
            }
            confirmButton.setText(buttonLabel);
            final String targetStatus = nextStatus;
            final String actionLabel = buttonLabel;
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (targetStatus == null) return;
                    if ("Completed".equalsIgnoreCase(targetStatus) && expenseManager != null) {
                        // Submit all expenses for this trip first (including current form)
                        expenseManager.submitAllExpensesForTrip(success -> {
                            if (success) {
                                confirmTripFromDashboard(trip, targetStatus, actionLabel);
                            } else {
                                Toast.makeText(DashboardActivity.this, "Submit expenses first (upload failed)", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        confirmTripFromDashboard(trip, targetStatus, actionLabel);
                    }
                }
            });

            // Prevent parent scroll intercept while panning map
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
                    return false;
                }
            });

            activeTripsContainer.addView(card);
        }
    }

    private void loadMapForDashboard(WebView mapWebView, TripResponse.Trip trip) {
        Object initializedTag = mapWebView.getTag();
        if (initializedTag instanceof Boolean && (Boolean) initializedTag) {
            return;
        }
        mapWebView.setTag(Boolean.TRUE);
        mapWebView.getSettings().setJavaScriptEnabled(true);
        mapWebView.getSettings().setDomStorageEnabled(true);
        mapWebView.getSettings().setLoadWithOverviewMode(true);
        mapWebView.getSettings().setUseWideViewPort(true);
        mapWebView.loadUrl("file:///android_asset/map.html");
        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                List<String> addresses = new ArrayList<>();
                if (trip.address != null && !trip.address.trim().isEmpty()) {
                    String[] parts = trip.address.split("\\|");
                    for (String p : parts) {
                        String a = p.trim();
                        if (!a.isEmpty() && !addresses.contains(a)) {
                            addresses.add(a);
                        }
                    }
                }
                if (addresses.isEmpty() && trip.location != null && !trip.location.trim().isEmpty()) {
                    addresses.add(trip.location.trim());
                }
                if (!addresses.isEmpty()) {
                    String addressesJson = new Gson().toJson(addresses);
                    String jsCode = "window.receiveMessage({type: 'updateMap', addresses: " + addressesJson + "});";
                    mapWebView.evaluateJavascript(jsCode, null);
                }
            }
        });
    }

    private void loadSavedLocationsForDashboard(LinearLayout locationsContainer) {
        ProgressBar loading = locationsContainer.findViewById(R.id.locationsLoadingIndicator);
        TextView empty = locationsContainer.findViewById(R.id.noLocationsText);
        if (loading != null) loading.setVisibility(View.VISIBLE);
        if (empty != null) empty.setVisibility(View.GONE);

        String username = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(KEY_USERNAME, "");
        if (username == null || username.isEmpty()) {
            if (loading != null) loading.setVisibility(View.GONE);
            if (empty != null) empty.setVisibility(View.VISIBLE);
            return;
        }

        ApiClient.get().getSavedLocations(username).enqueue(new Callback<SavedLocationsResponse>() {
            @Override
            public void onResponse(Call<SavedLocationsResponse> call, Response<SavedLocationsResponse> response) {
                if (loading != null) loading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().ok) {
                    displaySavedLocationsForDashboard(locationsContainer, response.body().locations);
                } else {
                    if (empty != null) empty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<SavedLocationsResponse> call, Throwable t) {
                if (loading != null) loading.setVisibility(View.GONE);
                if (empty != null) empty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displaySavedLocationsForDashboard(LinearLayout locationsContainer, List<SavedLocationsResponse.Location> locations) {
        if (locations == null || locations.isEmpty()) {
            TextView empty = locationsContainer.findViewById(R.id.noLocationsText);
            if (empty != null) empty.setVisibility(View.VISIBLE);
            return;
        }

        ProgressBar loading = locationsContainer.findViewById(R.id.locationsLoadingIndicator);
        TextView empty = locationsContainer.findViewById(R.id.noLocationsText);
        if (loading != null) loading.setVisibility(View.GONE);
        if (empty != null) empty.setVisibility(View.GONE);

        for (int i = locationsContainer.getChildCount() - 1; i >= 0; i--) {
            View child = locationsContainer.getChildAt(i);
            int childId = child.getId();
            if (childId != R.id.locationsLoadingIndicator && childId != R.id.noLocationsText) {
                locationsContainer.removeViewAt(i);
            }
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (SavedLocationsResponse.Location location : locations) {
            String fullAddress = location.locationAddress;
            if (fullAddress != null && fullAddress.contains("|")) {
                String[] addresses = fullAddress.split("\\|");
                for (int i = 0; i < addresses.length; i++) {
                    String address = addresses[i].trim();
                    if (!address.isEmpty()) {
                        View addressView = inflater.inflate(R.layout.location_step_item, null);
                        TextView addressText = addressView.findViewById(R.id.addressText);
                        ImageView stepIcon = addressView.findViewById(R.id.stepIcon);
                        ImageView dotsConnector = addressView.findViewById(R.id.dotsConnector);
                        addressText.setText(address);
                        if (i == addresses.length - 1) {
                            stepIcon.setImageResource(R.drawable.ic_location);
                            stepIcon.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
                            dotsConnector.setVisibility(View.GONE);
                        } else {
                            stepIcon.setImageResource(R.drawable.ic_circle_outline);
                            stepIcon.setColorFilter(getResources().getColor(android.R.color.darker_gray));
                        }
                        locationsContainer.addView(addressView);
                    }
                }
            } else {
                View locationView = inflater.inflate(R.layout.location_step_item, null);
                TextView addressText = locationView.findViewById(R.id.addressText);
                ImageView stepIcon = locationView.findViewById(R.id.stepIcon);
                ImageView dotsConnector = locationView.findViewById(R.id.dotsConnector);
                if (fullAddress != null && !fullAddress.isEmpty()) {
                    addressText.setText(fullAddress);
                } else {
                    addressText.setText("No address available");
                }
                stepIcon.setImageResource(R.drawable.ic_location);
                stepIcon.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
                dotsConnector.setVisibility(View.GONE);
                locationsContainer.addView(locationView);
            }
        }
    }

    private void confirmTripFromDashboard(TripResponse.Trip trip, String targetStatus, String buttonLabel) {
        StringBuilder message = new StringBuilder();
        message.append("Are you sure you want to ").append(buttonLabel.toLowerCase()).append("?\n\n");
        message.append("Trip ID: ").append(trip.tripID).append("\n");
        message.append("Location: ").append(trip.location).append("\n");
        message.append("Requester: ").append(trip.requester);

        new AlertDialog.Builder(this)
            .setTitle(buttonLabel)
            .setMessage(message.toString())
            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ApiClient.get().updateTripStatus("update_status", trip.tripID, targetStatus).enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().ok) {
                                Toast.makeText(DashboardActivity.this, "Trip updated to " + targetStatus, Toast.LENGTH_SHORT).show();
                                fetchActiveTrips();
                            } else {
                                String msg = (response.body() != null && response.body().msg != null) ? response.body().msg : "Failed to update trip";
                                Toast.makeText(DashboardActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            Toast.makeText(DashboardActivity.this, "Update failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setupExpensesArea(View card) {
        // Find expenses area views
        LinearLayout expensesArea = card.findViewById(R.id.expensesArea);
        if (expensesArea == null) return;

        // Get trip ID from the card (you may need to adjust this based on your data structure)
        TextView tripIdText = card.findViewById(R.id.tripIdText);
        String tripId = tripIdText != null ? tripIdText.getText().toString() : "unknown";
        
        // Create and setup expense manager
        expenseManager = new ExpenseManager(this);
        expenseManager.setupExpensesArea(card, tripId);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Handle image picker result
        if (requestCode == 1001) { // REQUEST_IMAGE_PICK
            if (expenseManager != null) {
                expenseManager.handleImagePickerResult(requestCode, resultCode, data);
            } else {
                android.util.Log.e("DashboardActivity", "expenseManager is null in onActivityResult!");
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // Handle camera permission result
        if (requestCode == 1003) { // REQUEST_CAMERA_PERMISSION
            if (expenseManager != null) {
                expenseManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
            } else {
                android.util.Log.e("DashboardActivity", "expenseManager is null in onRequestPermissionsResult!");
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION_FIRST_TIME) {
            // Handle first-time camera permission request
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted! You can now take photos for receipts.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied. You can still use gallery to select photos.", Toast.LENGTH_LONG).show();
            }
            // Mark that we've asked for permission
            sharedPreferences.edit().putBoolean(KEY_CAMERA_PERMISSION_ASKED, true).apply();
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Handle location permission request
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, start the service
                startLocationTrackingService();
            } else {
                // Permission denied - show message
                Toast.makeText(this, 
                    "Location permission is required for real-time vehicle tracking", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void checkAndRequestCameraPermission() {
        // Check if we've already asked for camera permission
        boolean hasAskedBefore = sharedPreferences.getBoolean(KEY_CAMERA_PERMISSION_ASKED, false);
        
        if (!hasAskedBefore) {
            // First time, ask for camera permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_FIRST_TIME);
            } else {
                // Permission already granted, mark as asked
                sharedPreferences.edit().putBoolean(KEY_CAMERA_PERMISSION_ASKED, true).apply();
            }
        }
    }

    /**
     * Check location permissions and start location tracking service if permissions are granted
     */
    private void checkLocationPermissionsAndStartService() {
        try {
            // Only start service if location permission is already granted
            // If not granted, wait for user to grant permission first
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, start the service
                startLocationTrackingService();
            } else {
                // Don't request permissions automatically on startup - let user decide
                // Permission will be requested when needed (e.g., when trip is assigned)
                Log.d("DashboardActivity", "Location permission not granted, service will start when permission is granted");
            }
        } catch (Exception e) {
            Log.e("DashboardActivity", "Error checking location permissions: " + e.getMessage());
            // Don't crash if there's an error, just log it
        }
    }

    /**
     * Start the location tracking service (it will check for active trips internally)
     */
    private void startLocationTrackingService() {
        try {
            // Double-check permission before starting
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("DashboardActivity", "Location permission not granted, cannot start tracking service");
                return;
            }

            Intent serviceIntent = new Intent(this, LocationTrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    ContextCompat.startForegroundService(this, serviceIntent);
                    Log.d("DashboardActivity", "Location tracking service started (foreground)");
                } catch (IllegalStateException e) {
                    // Fallback if foreground service fails
                    Log.w("DashboardActivity", "Foreground service start failed, trying regular service: " + e.getMessage());
                    startService(serviceIntent);
                }
            } else {
                startService(serviceIntent);
                Log.d("DashboardActivity", "Location tracking service started");
            }
        } catch (SecurityException e) {
            Log.e("DashboardActivity", "Security exception starting location service: " + e.getMessage());
        } catch (Exception e) {
            Log.e("DashboardActivity", "Error starting location tracking service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh active trips when activity resumes
        fetchActiveTrips();
        // Resume heartbeat when app becomes active
        startHeartbeat();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop heartbeat when app is paused/backgrounded
        stopHeartbeat();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up heartbeat when activity is destroyed
        stopHeartbeat();
    }
    
    /**
     * Initialize heartbeat handler and runnable
     */
    private void initializeHeartbeat() {
        heartbeatHandler = new Handler(Looper.getMainLooper());
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                sendHeartbeat();
                // Schedule next heartbeat
                if (heartbeatHandler != null) {
                    heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
                }
            }
        };
    }
    
    /**
     * Start sending periodic heartbeat signals
     */
    private void startHeartbeat() {
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            // Remove any existing callbacks to avoid duplicates
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            // Send immediate heartbeat
            sendHeartbeat();
            // Schedule periodic heartbeats
            heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL);
            Log.d("DashboardActivity", "Heartbeat started - will send every " + (HEARTBEAT_INTERVAL / 1000) + " seconds");
        } else {
            Log.e("DashboardActivity", "Cannot start heartbeat: handler not initialized");
        }
    }
    
    /**
     * Stop sending heartbeat signals
     */
    private void stopHeartbeat() {
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            Log.d("DashboardActivity", "Heartbeat stopped");
        }
    }
    
    /**
     * Send heartbeat to server to indicate driver is active
     */
    private void sendHeartbeat() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String username = prefs.getString(KEY_USERNAME, "");
        String driverID = prefs.getString("driverID", "");
        
        if (username == null || username.isEmpty()) {
            Log.d("DashboardActivity", "Cannot send heartbeat: username not available");
            return;
        }
        
        // Log what we're sending for debugging
        Log.d("DashboardActivity", "Sending heartbeat - username: " + username + ", driverID: " + (driverID.isEmpty() ? "not set" : driverID));
        
        ApiClient.get().sendHeartbeat(username, driverID).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().ok) {
                    Log.d("DashboardActivity", "Heartbeat sent successfully");
                } else {
                    String errorMsg = response.body() != null && response.body().msg != null ? response.body().msg : "Unknown error";
                    Log.e("DashboardActivity", "Heartbeat failed: " + errorMsg + " (HTTP " + response.code() + ")");
                    // Try to read error response body if available
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e("DashboardActivity", "Heartbeat error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e("DashboardActivity", "Could not read error body", e);
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Log.e("DashboardActivity", "Heartbeat network error: " + t.getMessage(), t);
            }
        });
    }

}


