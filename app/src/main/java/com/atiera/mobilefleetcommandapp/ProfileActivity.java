package com.atiera.mobilefleetcommandapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;

public class ProfileActivity extends DashboardActivity {

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
        EditText id = findViewById(R.id.pf_driver_id);
        EditText first = findViewById(R.id.pf_first_name);
        EditText middle = findViewById(R.id.pf_middle_name);
        EditText last = findViewById(R.id.pf_last_name);
        EditText bdate = findViewById(R.id.pf_birth_date);
        EditText gender = findViewById(R.id.pf_gender);
        EditText contact = findViewById(R.id.pf_contact);
        EditText address = findViewById(R.id.pf_address);
        EditText licNum = findViewById(R.id.pf_license_number);
        EditText licIssued = findViewById(R.id.pf_license_issued);
        EditText licExpiry = findViewById(R.id.pf_license_expiry);
        EditText emName = findViewById(R.id.pf_emergency_name);
        EditText emNumber = findViewById(R.id.pf_emergency_number);
        EditText blood = findViewById(R.id.pf_blood_type);

        String username = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("username", null);
        if (username != null) {
            ApiClient.get().getDriverProfile(username).enqueue(new retrofit2.Callback<DriverProfileResponse>() {
                @Override
                public void onResponse(retrofit2.Call<DriverProfileResponse> call, retrofit2.Response<DriverProfileResponse> response) {
                    DriverProfileResponse body = response.body();
                    if (response.isSuccessful() && body != null && body.ok && body.driver != null) {
                        if (id != null) id.setText(body.driver.idNumber);
                        if (first != null) first.setText(body.driver.firstName);
                        if (middle != null) middle.setText(body.driver.middleName);
                        if (last != null) last.setText(body.driver.lastName);
                        if (bdate != null) bdate.setText(body.driver.birthDate);
                        if (gender != null) gender.setText(body.driver.gender);
                        if (contact != null) contact.setText(body.driver.contactNumber);
                        if (address != null) address.setText(body.driver.address);
                        if (licNum != null) licNum.setText(body.driver.licenseNumber);
                        if (licIssued != null) licIssued.setText(body.driver.licenseIssued);
                        if (licExpiry != null) licExpiry.setText(body.driver.licenseExpiry);
                        if (emName != null) emName.setText(body.driver.emergencyContactName);
                        if (emNumber != null) emNumber.setText(body.driver.emergencyContactNumber);
                        if (blood != null) blood.setText(body.driver.bloodType);
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
}
