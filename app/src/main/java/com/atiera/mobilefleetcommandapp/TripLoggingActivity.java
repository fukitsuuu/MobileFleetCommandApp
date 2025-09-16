package com.atiera.mobilefleetcommandapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.AutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.ArrayAdapter;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.ArrayList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.SharedPreferences;

public class TripLoggingActivity extends DashboardActivity {

    private LinearLayout logsContainer;
    private ProgressBar loading;
    private TextView emptyText;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USERNAME = "username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_logging);
        
        // Re-initialize drawer components after setting content view
        initializeDrawerComponents();
        
        // Set Trip Logging as checked since this is the Trip Logging page
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_trip_logging);
        }

        logsContainer = findViewById(R.id.logsContainer);
        loading = findViewById(R.id.logsLoading);
        emptyText = findViewById(R.id.noLogsText);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        fetchCompletedTrips();
    }

    private void fetchCompletedTrips() {
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
            return;
        }
        loading.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
        logsContainer.removeAllViews();

        ApiClient.get().getDriverTrips(username).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                loading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().ok) {
                    List<TripResponse.Trip> completed = new ArrayList<>();
                    if (response.body().trips != null) {
                        for (TripResponse.Trip t : response.body().trips) {
                            if (t.status != null && t.status.equalsIgnoreCase("completed")) {
                                completed.add(t);
                            }
                        }
                    }
                    renderCompletedTrips(completed);
                } else {
                    emptyText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                loading.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void renderCompletedTrips(List<TripResponse.Trip> completed) {
        if (completed == null || completed.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            return;
        }
        emptyText.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (TripResponse.Trip trip : completed) {
            View card = inflater.inflate(R.layout.trip_item, logsContainer, false);
            ((TextView) card.findViewById(R.id.tripIdText)).setText(trip.tripID);
            ((TextView) card.findViewById(R.id.requesterText)).setText(trip.requester);
            ((TextView) card.findViewById(R.id.locationText)).setText(trip.location);
            ((TextView) card.findViewById(R.id.urgencyText)).setText(trip.urgency);
            ((TextView) card.findViewById(R.id.statusText)).setText(trip.status);
            ((TextView) card.findViewById(R.id.vehicleText)).setText(trip.vehicleNumber);
            ((TextView) card.findViewById(R.id.distanceText)).setText(trip.estimatedDistance);
            ((TextView) card.findViewById(R.id.timeText)).setText(trip.estimatedTime);
            TextView budgetText = card.findViewById(R.id.budgetText);
            if (trip.budget < 0.01) {
                budgetText.setText("Reimbursement");
                budgetText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            } else {
                budgetText.setText("₱" + String.format("%.2f", trip.budget));
                budgetText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }

            // Setup dropdown toggle (without map)
            View dropdown = card.findViewById(R.id.dropdownContent);
            if (dropdown != null) {
                dropdown.setVisibility(View.GONE);
                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleDropdownLogging(dropdown, trip.tripID != null ? trip.tripID.trim() : "");
                    }
                });
            }

            // Remove bottom button on Trip Logging screen
            Button confirmButton = card.findViewById(R.id.confirmButton);
            if (confirmButton != null) confirmButton.setVisibility(View.GONE);

            logsContainer.addView(card);
        }
    }

    private void toggleDropdownLogging(View dropdownContent, String tripId) {
        if (dropdownContent.getVisibility() == View.VISIBLE) {
            dropdownContent.setVisibility(View.GONE);
            return;
        }

        // Show dropdown
        dropdownContent.setVisibility(View.VISIBLE);

        // Hide the map section if present
        View mapWebView = dropdownContent.findViewById(R.id.mapWebView);
        if (mapWebView != null) {
            View mapContainer = (View) mapWebView.getParent();
            if (mapContainer != null) {
                mapContainer.setVisibility(View.GONE);
            } else {
                mapWebView.setVisibility(View.GONE);
            }
        }

        // Load saved locations like AssignmentActivity
        LinearLayout locationsContainer = dropdownContent.findViewById(R.id.locationsContainer);
        if (locationsContainer != null) {
            loadSavedLocationsLogging(locationsContainer);
        }

        // Show and initialize Expenses Area under Saved Locations, and load existing expenses
        LinearLayout expensesArea = dropdownContent.findViewById(R.id.expensesArea);
        if (expensesArea != null) {
            expensesArea.setVisibility(View.VISIBLE);
            
            // Add Expenses header above the expenses area
            addExpensesHeaderAboveArea(expensesArea);

            // Hide input controls on Trip Logging (display-only)
            AutoCompleteTextView expenseTypeDropdown = expensesArea.findViewById(R.id.expenseTypeDropdown);
            if (expenseTypeDropdown != null) {
                View typeBlock = null;
                View parent = (View) expenseTypeDropdown.getParent();
                if (parent != null) {
                    View parent2 = (View) parent.getParent();
                    if (parent2 != null) typeBlock = parent2;
                }
                if (typeBlock != null) { typeBlock.setVisibility(View.GONE); }
            }

            // Hide amount input row
            TextInputEditText costAmountInput = expensesArea.findViewById(R.id.costAmountInput);
            if (costAmountInput != null) {
                View row = null;
                View p1 = (View) costAmountInput.getParent();
                if (p1 != null) {
                    View p2 = (View) p1.getParent();
                    if (p2 != null) row = p2;
                }
                if (row != null) { row.setVisibility(View.GONE); }
            }

            // Hide Add Expenses button
            Button addExpenseButton = expensesArea.findViewById(R.id.addExpenseButton);
            if (addExpenseButton != null) { addExpenseButton.setVisibility(View.GONE); }

            // Total expenses text container
            LinearLayout totalContainer = expensesArea.findViewById(R.id.totalExpensesContainer);
            TextView totalText = expensesArea.findViewById(R.id.totalExpensesText);

            // List container for rendered expenses
            LinearLayout listHost = expensesArea.findViewById(R.id.expensesListContainer);
            TextView noExpensesText = expensesArea.findViewById(R.id.noExpensesText);

            // Hide all other children inside expensesArea except the list and total containers
            for (int i = 0; i < expensesArea.getChildCount(); i++) {
                View child = expensesArea.getChildAt(i);
                if (child != listHost && child != totalContainer) {
                    child.setVisibility(View.GONE);
                }
            }

            // Load expenses for this trip
            if (tripId != null && !tripId.isEmpty()) {
                ApiClient.get().getTripExpenses(tripId).enqueue(new Callback<TripExpensesResponse>() {
                    @Override
                    public void onResponse(Call<TripExpensesResponse> call, Response<TripExpensesResponse> response) {
                        android.util.Log.d("TripLogging", "expenses resp code=" + response.code());
                        try { android.util.Log.d("TripLogging", "expenses body=" + new com.google.gson.Gson().toJson(response.body())); } catch (Exception ignore) {}
                        if (!response.isSuccessful() || response.body() == null || !response.body().ok) {
                            if (noExpensesText != null) noExpensesText.setVisibility(View.VISIBLE);
                            if (noExpensesText != null) noExpensesText.setText("No expenses recorded for " + tripId);
                            if (totalContainer != null) totalContainer.setVisibility(View.GONE);
                            return;
                        }
                        TripExpensesResponse body = response.body();
                        if (totalContainer != null && totalText != null) {
                            totalContainer.setVisibility(View.VISIBLE);
                            totalText.setText("₱" + String.format("%.2f", body.totalExpenses));
                        }
                        if (listHost != null) {
                            // Clear any previous rendered items (keep as host)
                            listHost.removeAllViews();
                            if (body.expenses == null || body.expenses.isEmpty()) {
                                if (noExpensesText != null) {
                                    noExpensesText.setText("No expenses recorded for " + tripId);
                                    noExpensesText.setVisibility(View.VISIBLE);
                                }
                            } else {
                                if (noExpensesText != null) noExpensesText.setVisibility(View.GONE);
                                for (TripExpensesResponse.Expense e : body.expenses) {
                                    View row = LayoutInflater.from(TripLoggingActivity.this).inflate(R.layout.view_expense_row, listHost, false);
                                    TextView label = row.findViewById(R.id.expenseLabel);
                                    TextView value = row.findViewById(R.id.expenseValue);
                                    TextView typeHeader = row.findViewById(R.id.expenseType);
                                    TextView description = row.findViewById(R.id.expenseDescription);
                                    LinearLayout imagesWrap = row.findViewById(R.id.expenseImagesContainer);
                                    label.setText(e.recordDate != null ? e.recordDate : "Expense");
                                    value.setText("₱" + String.format("%.2f", e.fuelCost));
                                    if (typeHeader != null) {
                                        String t = "Fuel Expenses"; // Default
                                        if (e.type != null) {
                                            if (e.type.equalsIgnoreCase("Supply Cost")) {
                                                t = "Supply Expenses";
                                            } else if (e.type.equalsIgnoreCase("Other")) {
                                                t = "Other Expenses";
                                            } else if (e.type.equalsIgnoreCase("Fuel Cost")) {
                                                t = "Fuel Expenses";
                                            }
                                        }
                                        typeHeader.setText(t);
                                    }
                                    
                                    // Handle description for Supply Cost and Other expenses
                                    if (description != null) {
                                        if (e.type != null && (e.type.equalsIgnoreCase("Supply Cost") || e.type.equalsIgnoreCase("Other"))) {
                                            if (e.description != null && !e.description.trim().isEmpty()) {
                                                description.setText(e.description);
                                                description.setVisibility(View.VISIBLE);
                                            } else {
                                                description.setVisibility(View.GONE);
                                            }
                                        } else {
                                            description.setVisibility(View.GONE);
                                        }
                                    }
                                    // Render receipt thumbnails if any
                                    if (imagesWrap != null && e.receiptImages != null && !e.receiptImages.isEmpty()) {
                                        final float density = getResources().getDisplayMetrics().density;
                                        int sizePx = (int) (96f * density); // 96dp
                                        int marginPx = (int) (8f * density); // 8dp
                                        for (String url : e.receiptImages) {
                                            ImageView iv = new ImageView(TripLoggingActivity.this);
                                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                                            lp.rightMargin = marginPx;
                                            iv.setLayoutParams(lp);
                                            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                            try {
                                                com.bumptech.glide.Glide.with(TripLoggingActivity.this)
                                                    .load(url)
                                                    .placeholder(android.R.color.darker_gray)
                                                    .into(iv);
                                            } catch (Exception ignore) {}
                                            // Expand to full-screen preview on tap
                                            iv.setOnClickListener(vv -> showImagePreview(url));
                                            imagesWrap.addView(iv);
                                        }
                                    }
                                    listHost.addView(row);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<TripExpensesResponse> call, Throwable t) {
                        if (noExpensesText != null) {
                            noExpensesText.setText("No expenses recorded for " + tripId);
                            noExpensesText.setVisibility(View.VISIBLE);
                        }
                        if (totalContainer != null) totalContainer.setVisibility(View.GONE);
                    }
                });
            }

            // Hide upload button (we only display existing expenses here)
            ImageView uploadReceiptButton = expensesArea.findViewById(R.id.uploadReceiptButton);
            if (uploadReceiptButton != null) { uploadReceiptButton.setVisibility(View.GONE); }
        }
    }

    private void addExpensesHeaderAboveArea(LinearLayout expensesArea) {
        // Get the parent container of the expenses area
        View parent = (View) expensesArea.getParent();
        if (parent instanceof LinearLayout) {
            LinearLayout parentLayout = (LinearLayout) parent;
            
            // Create expenses header section (similar to Saved Locations)
            LinearLayout expensesHeaderSection = new LinearLayout(this);
            expensesHeaderSection.setOrientation(LinearLayout.VERTICAL);
            expensesHeaderSection.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            expensesHeaderSection.setPadding(0, 16, 0, 0);

            // Create section header (similar to Saved Locations)
            LinearLayout headerLayout = new LinearLayout(this);
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
            headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            headerLayout.setPadding(0, 0, 0, 12);

            // Add icon
            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.ic_receipt);
            icon.setColorFilter(0xFF6B7280);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density)
            );
            iconParams.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            icon.setLayoutParams(iconParams);
            headerLayout.addView(icon);

            // Add header text
            TextView headerText = new TextView(this);
            headerText.setText("Expenses");
            headerText.setTextColor(0xFF374151);
            headerText.setTextSize(16);
            headerText.setTypeface(null, android.graphics.Typeface.BOLD);
            headerLayout.addView(headerText);

            expensesHeaderSection.addView(headerLayout);

            // Find the index of the expenses area in its parent
            int expensesAreaIndex = parentLayout.indexOfChild(expensesArea);
            
            // Insert the header before the expenses area
            parentLayout.addView(expensesHeaderSection, expensesAreaIndex);
        }
    }

    private void loadSavedLocationsLogging(LinearLayout locationsContainer) {
        String username = sharedPreferences.getString(KEY_USERNAME, "");
        if (username == null || username.isEmpty()) {
            showNoLocationsLogging(locationsContainer);
            return;
        }

        ProgressBar loadingIndicator = locationsContainer.findViewById(R.id.locationsLoadingIndicator);
        TextView noLocationsText = locationsContainer.findViewById(R.id.noLocationsText);
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
        if (noLocationsText != null) noLocationsText.setVisibility(View.GONE);

        ApiClient.get().getSavedLocations(username).enqueue(new Callback<SavedLocationsResponse>() {
            @Override
            public void onResponse(Call<SavedLocationsResponse> call, Response<SavedLocationsResponse> response) {
                if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().ok) {
                    displaySavedLocationsLogging(locationsContainer, response.body().locations);
                } else {
                    showNoLocationsLogging(locationsContainer);
                }
            }

            @Override
            public void onFailure(Call<SavedLocationsResponse> call, Throwable t) {
                if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                showNoLocationsLogging(locationsContainer);
            }
        });
    }

    private void displaySavedLocationsLogging(LinearLayout locationsContainer, List<SavedLocationsResponse.Location> locations) {
        if (locations == null || locations.isEmpty()) {
            showNoLocationsLogging(locationsContainer);
            return;
        }

        ProgressBar loadingIndicator = locationsContainer.findViewById(R.id.locationsLoadingIndicator);
        TextView noLocationsText = locationsContainer.findViewById(R.id.noLocationsText);
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        if (noLocationsText != null) noLocationsText.setVisibility(View.GONE);

        // Remove dynamic items (keep placeholders)
        for (int i = locationsContainer.getChildCount() - 1; i >= 0; i--) {
            View child = locationsContainer.getChildAt(i);
            int childId = child.getId();
            if (childId != R.id.locationsLoadingIndicator && childId != R.id.noLocationsText) {
                locationsContainer.removeViewAt(i);
            }
        }

        for (SavedLocationsResponse.Location location : locations) {
            String fullAddress = location.locationAddress;
            if (fullAddress != null && fullAddress.contains("|")) {
                String[] addresses = fullAddress.split("\\|");
                for (int i = 0; i < addresses.length; i++) {
                    String address = addresses[i].trim();
                    if (!address.isEmpty()) {
                        View item = createAddressViewLogging(address, i + 1, addresses.length);
                        locationsContainer.addView(item);
                    }
                }
            } else {
                View item = createLocationViewLogging(location);
                locationsContainer.addView(item);
            }
        }
    }

    private View createLocationViewLogging(SavedLocationsResponse.Location location) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.location_step_item, null);
        EditText addressText = view.findViewById(R.id.addressText);
        ImageView stepIcon = view.findViewById(R.id.stepIcon);
        ImageView dotsConnector = view.findViewById(R.id.dotsConnector);

        String fullAddress = location.locationAddress;
        if (fullAddress != null && !fullAddress.isEmpty()) {
            addressText.setText(fullAddress);
        } else {
            addressText.setText("No address available");
        }

        stepIcon.setImageResource(R.drawable.ic_location);
        stepIcon.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
        dotsConnector.setVisibility(View.GONE);
        return view;
    }

    private View createAddressViewLogging(String address, int stepNumber, int totalSteps) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.location_step_item, null);
        EditText addressText = view.findViewById(R.id.addressText);
        ImageView stepIcon = view.findViewById(R.id.stepIcon);
        ImageView dotsConnector = view.findViewById(R.id.dotsConnector);

        addressText.setText(address);
        if (stepNumber == totalSteps) {
            stepIcon.setImageResource(R.drawable.ic_location);
            stepIcon.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
            dotsConnector.setVisibility(View.GONE);
        } else {
            stepIcon.setImageResource(R.drawable.ic_circle_outline);
            stepIcon.setColorFilter(getResources().getColor(android.R.color.darker_gray));
        }
        return view;
    }

    private void showNoLocationsLogging(LinearLayout locationsContainer) {
        ProgressBar loadingIndicator = locationsContainer.findViewById(R.id.locationsLoadingIndicator);
        TextView noLocationsText = locationsContainer.findViewById(R.id.noLocationsText);
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        if (noLocationsText != null) noLocationsText.setVisibility(View.VISIBLE);
    }

    private void showImagePreview(String url){
        try {
            // Root overlay fills the whole screen with semi-transparent black
            android.widget.FrameLayout root = new android.widget.FrameLayout(this);
            root.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            ));
            root.setBackgroundColor(0x99000000);
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

            try {
                com.bumptech.glide.Glide.with(this)
                    .load(url)
                    .into(preview);
            } catch (Exception ignore) {}

            android.widget.FrameLayout.LayoutParams centerParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            );
            centerParams.gravity = android.view.Gravity.CENTER;
            root.addView(preview, centerParams);

            // Fullscreen dialog
            android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(root);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);

            // Close when tapping outside the image
            root.setOnClickListener(v -> dialog.dismiss());
            // Consume taps on the image so it doesn't dismiss
            preview.setClickable(true);
            preview.setOnClickListener(v -> {});

            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                );
                dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
                );
            }
        } catch (Exception ignored) {}
    }
}
