package com.atiera.mobilefleetcommandapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileActivity extends DashboardActivity {

    private boolean isEditMode = false;
    private EditText id, first, middle, last, contact, address;
    private EditText licNum, emName, emNumber;
    private TextInputEditText bdate, licIssued, licExpiry;
    private Spinner gender, blood;
    private Calendar calendar = Calendar.getInstance();
    private String currentDriverId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Re-initialize drawer components after setting content view
        initializeDrawerComponents();
        
        // Set Profile as checked since this is the Profile page
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_profile);
        }

        ImageView img = findViewById(R.id.pf_driver_image);
        id = findViewById(R.id.pf_driver_id);
        first = findViewById(R.id.pf_first_name);
        middle = findViewById(R.id.pf_middle_name);
        last = findViewById(R.id.pf_last_name);
        bdate = findViewById(R.id.pf_birth_date);
        gender = findViewById(R.id.pf_gender);
        contact = findViewById(R.id.pf_contact);
        address = findViewById(R.id.pf_address);
        licNum = findViewById(R.id.pf_license_number);
        licIssued = findViewById(R.id.pf_license_issued);
        licExpiry = findViewById(R.id.pf_license_expiry);
        emName = findViewById(R.id.pf_emergency_name);
        emNumber = findViewById(R.id.pf_emergency_number);
        blood = findViewById(R.id.pf_blood_type);

        // Setup Gender Spinner
        String[] genderOptions = {"", "Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_item_custom,
            genderOptions
        );
        genderAdapter.setDropDownViewResource(R.layout.spinner_dropdown_custom);
        gender.setAdapter(genderAdapter);

        // Setup Blood Type Spinner
        String[] bloodOptions = {"", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_item_custom,
            bloodOptions
        );
        bloodAdapter.setDropDownViewResource(R.layout.spinner_dropdown_custom);
        blood.setAdapter(bloodAdapter);

        // Prevent dropdown from opening unless Edit Mode is enabled
        lockSpinnerWhenNotEditing(gender);
        lockSpinnerWhenNotEditing(blood);

        // Setup Date Pickers
        setupDatePicker(bdate, "Birth Date");
        setupDatePicker(licIssued, "License Issued");
        setupDatePicker(licExpiry, "License Expiry");

        // Edit Profile Button
        Button editProfileButton = findViewById(R.id.editProfileButton);
        if (editProfileButton != null) {
            editProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleEditMode();
                }
            });
        }

        String username = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("username", null);
        if (username != null) {
            ApiClient.get().getDriverProfile(username).enqueue(new retrofit2.Callback<DriverProfileResponse>() {
                @Override
                public void onResponse(retrofit2.Call<DriverProfileResponse> call, retrofit2.Response<DriverProfileResponse> response) {
                    DriverProfileResponse body = response.body();
                    if (response.isSuccessful() && body != null && body.ok && body.driver != null) {
                        currentDriverId = body.driver.idNumber;
                        if (id != null) id.setText(body.driver.idNumber);
                        if (first != null) first.setText(body.driver.firstName);
                        if (middle != null) middle.setText(body.driver.middleName);
                        if (last != null) last.setText(body.driver.lastName);
                        if (bdate != null) bdate.setText(body.driver.birthDate);
                        if (contact != null) contact.setText(body.driver.contactNumber);
                        if (address != null) address.setText(body.driver.address);
                        if (licNum != null) licNum.setText(body.driver.licenseNumber);
                        if (licIssued != null) licIssued.setText(body.driver.licenseIssued);
                        if (licExpiry != null) licExpiry.setText(body.driver.licenseExpiry);
                        if (emName != null) emName.setText(body.driver.emergencyContactName);
                        if (emNumber != null) emNumber.setText(body.driver.emergencyContactNumber);
                        
                        // Set Gender Spinner
                        if (gender != null && body.driver.gender != null) {
                            String genderValue = body.driver.gender;
                            for (int i = 0; i < gender.getCount(); i++) {
                                if (gender.getItemAtPosition(i).toString().equals(genderValue)) {
                                    gender.setSelection(i);
                                    break;
                                }
                            }
                            // Ensure text color is visible even when disabled
                            setSpinnerTextColor(gender, "#0F172A");
                        }
                        
                        // Set Blood Type Spinner
                        if (blood != null && body.driver.bloodType != null && !body.driver.bloodType.isEmpty()) {
                            String bloodValue = body.driver.bloodType;
                            for (int i = 0; i < blood.getCount(); i++) {
                                if (blood.getItemAtPosition(i).toString().equals(bloodValue)) {
                                    blood.setSelection(i);
                                    break;
                                }
                            }
                            // Ensure text color is visible even when disabled
                            setSpinnerTextColor(blood, "#0F172A");
                        }
                        
                        if (img != null && body.driver.imageUrl != null && !body.driver.imageUrl.isEmpty()) {
                            try {
                                com.bumptech.glide.Glide.with(ProfileActivity.this)
                                        .load(body.driver.imageUrl)
                                        .placeholder(R.drawable.circle_background)
                                        .error(R.drawable.circle_background)
                                        .centerCrop()
                                        .into(img);
                            } catch (Exception ignored) {}
                        }
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<DriverProfileResponse> call, Throwable t) {
                    // ignore
                }
            });
        }
    }

    private void setupDatePicker(TextInputEditText editText, String fieldName) {
        if (editText == null) return;
        
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEditMode) return;
                
                // Parse current date if available
                String currentDateStr = editText.getText().toString();
                Calendar dateCalendar = Calendar.getInstance();
                int year = dateCalendar.get(Calendar.YEAR);
                int month = dateCalendar.get(Calendar.MONTH);
                int day = dateCalendar.get(Calendar.DAY_OF_MONTH);
                
                if (currentDateStr != null && !currentDateStr.isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        dateCalendar.setTime(sdf.parse(currentDateStr));
                        year = dateCalendar.get(Calendar.YEAR);
                        month = dateCalendar.get(Calendar.MONTH);
                        day = dateCalendar.get(Calendar.DAY_OF_MONTH);
                    } catch (Exception e) {
                        // Use default date
                    }
                }
                
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                    ProfileActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        editText.setText(sdf.format(selectedCalendar.getTime()));
                    },
                    year, month, day
                );
                datePickerDialog.show();
            }
        });
        
        editText.setFocusable(false);
        editText.setClickable(false);
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        Button editProfileButton = findViewById(R.id.editProfileButton);
        
        if (isEditMode) {
            // Enable all editable fields
            setEditTextEnabled(first, true);
            setEditTextEnabled(middle, true);
            setEditTextEnabled(last, true);
            setEditTextEnabled(contact, true);
            setEditTextEnabled(address, true);
            setEditTextEnabled(licNum, true);
            setEditTextEnabled(emName, true);
            setEditTextEnabled(emNumber, true);
            
            // Enable date fields
            if (bdate != null) {
                bdate.setEnabled(true);
                bdate.setFocusable(false);
                bdate.setClickable(true);
            }
            if (licIssued != null) {
                licIssued.setEnabled(true);
                licIssued.setFocusable(false);
                licIssued.setClickable(true);
            }
            if (licExpiry != null) {
                licExpiry.setEnabled(true);
                licExpiry.setFocusable(false);
                licExpiry.setClickable(true);
            }
            
            // Enable spinners
            if (gender != null) gender.setEnabled(true);
            if (blood != null) blood.setEnabled(true);
            
            editProfileButton.setText("Save Profile");
            Toast.makeText(this, "Edit mode enabled. Make your changes and click Save.", Toast.LENGTH_SHORT).show();
        } else {
            // Disable all fields
            setEditTextEnabled(first, false);
            setEditTextEnabled(middle, false);
            setEditTextEnabled(last, false);
            setEditTextEnabled(contact, false);
            setEditTextEnabled(address, false);
            setEditTextEnabled(licNum, false);
            setEditTextEnabled(emName, false);
            setEditTextEnabled(emNumber, false);
            
            // Disable date fields
            if (bdate != null) {
                bdate.setEnabled(false);
                bdate.setFocusable(false);
                bdate.setClickable(false);
            }
            if (licIssued != null) {
                licIssued.setEnabled(false);
                licIssued.setFocusable(false);
                licIssued.setClickable(false);
            }
            if (licExpiry != null) {
                licExpiry.setEnabled(false);
                licExpiry.setFocusable(false);
                licExpiry.setClickable(false);
            }
            
            // Disable spinners but keep text color visible
            if (gender != null) {
                gender.setEnabled(false);
                setSpinnerTextColor(gender, "#0F172A");
            }
            if (blood != null) {
                blood.setEnabled(false);
                setSpinnerTextColor(blood, "#0F172A");
            }
            
            editProfileButton.setText("Edit Profile");
            
            // Save profile
            saveProfile();
        }
    }

    private void saveProfile() {
        String username = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("username", null);
        if (username == null || currentDriverId == null || currentDriverId.isEmpty()) {
            Toast.makeText(this, "Unable to save: Missing user information.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Collect form data
        String firstName = first != null ? first.getText().toString().trim() : "";
        String middleName = middle != null ? middle.getText().toString().trim() : "";
        String lastName = last != null ? last.getText().toString().trim() : "";
        String birthDate = bdate != null ? bdate.getText().toString().trim() : "";
        String genderValue = gender != null && gender.getSelectedItemPosition() > 0 
            ? gender.getSelectedItem().toString() : "";
        String contactNumber = contact != null ? contact.getText().toString().trim() : "";
        String addressValue = address != null ? address.getText().toString().trim() : "";
        String licenseNumber = licNum != null ? licNum.getText().toString().trim() : "";
        String licenseIssued = licIssued != null ? licIssued.getText().toString().trim() : "";
        String licenseExpiry = licExpiry != null ? licExpiry.getText().toString().trim() : "";
        String emergencyName = emName != null ? emName.getText().toString().trim() : "";
        String emergencyNumber = emNumber != null ? emNumber.getText().toString().trim() : "";
        String bloodType = blood != null && blood.getSelectedItemPosition() > 0 
            ? blood.getSelectedItem().toString() : "";
        
        // Basic validation
        if (firstName.isEmpty() || lastName.isEmpty() || birthDate.isEmpty() || 
            genderValue.isEmpty() || contactNumber.isEmpty() || addressValue.isEmpty() ||
            licenseNumber.isEmpty() || licenseIssued.isEmpty() || licenseExpiry.isEmpty() ||
            emergencyName.isEmpty() || emergencyNumber.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            toggleEditMode(); // Re-enable edit mode
            return;
        }
        
        // Call API to update profile
        ApiClient.get().updateDriverProfile(
            currentDriverId,
            firstName,
            middleName,
            lastName,
            birthDate,
            genderValue,
            contactNumber,
            addressValue,
            licenseNumber,
            licenseIssued,
            licenseExpiry,
            emergencyName,
            emergencyNumber,
            bloodType
        ).enqueue(new retrofit2.Callback<GenericResponse>() {
            @Override
            public void onResponse(retrofit2.Call<GenericResponse> call, retrofit2.Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GenericResponse body = response.body();
                    if (body.ok) {
                        Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Error: " + (body.msg != null ? body.msg : "Failed to update profile"), Toast.LENGTH_LONG).show();
                        toggleEditMode(); // Re-enable edit mode on error
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show();
                    toggleEditMode(); // Re-enable edit mode on error
                }
            }

            @Override
            public void onFailure(retrofit2.Call<GenericResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                toggleEditMode(); // Re-enable edit mode on error
            }
        });
    }

    private void setEditTextEnabled(EditText editText, boolean enabled) {
        if (editText != null) {
            editText.setEnabled(enabled);
            editText.setFocusable(enabled);
            editText.setFocusableInTouchMode(enabled);
            editText.setCursorVisible(enabled);
        }
    }

    private void setSpinnerTextColor(Spinner spinner, String colorHex) {
        if (spinner != null) {
            try {
                android.view.View view = spinner.getSelectedView();
                if (view instanceof android.widget.TextView) {
                    ((android.widget.TextView) view).setTextColor(android.graphics.Color.parseColor(colorHex));
                }
            } catch (Exception e) {
                // Ignore if view is not available yet
            }
        }
    }

    /**
     * Spinners sometimes still open on tap/focus even when visually disabled.
     * This blocks touch interactions unless edit mode is enabled.
     */
    private void lockSpinnerWhenNotEditing(Spinner spinner) {
        if (spinner == null) return;
        spinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && !isEditMode) {
                return true; // consume touch, do not open dropdown
            }
            return false;
        });
    }
}
