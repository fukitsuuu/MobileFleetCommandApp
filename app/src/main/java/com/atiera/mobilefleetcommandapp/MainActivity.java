package com.atiera.mobilefleetcommandapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText usernameInput, passwordInput;
    private Button loginButton;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is already logged in
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            // User is already logged in, go directly to loading screen
            Intent intent = new Intent(MainActivity.this, LoadingActivity.class);
            startActivity(intent);
            finish(); // Close MainActivity so user can't go back
            return;
        }
        
        setContentView(R.layout.activity_main);

        usernameInput = findViewById(R.id.username);
        passwordInput = findViewById(R.id.password);
        loginButton = findViewById(R.id.signIn);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show loading state
                loginButton.setEnabled(false);
                loginButton.setText("Logging in...");

                // Call login API
                ApiClient.get().driverLogin(username, password).enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        loginButton.setEnabled(true);
                        loginButton.setText("Sign In");

                        if (response.isSuccessful() && response.body() != null) {
                            LoginResponse loginResponse = response.body();
                            if (loginResponse.ok) {
                                // Save login credentials and session
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(KEY_USERNAME, username);
                                editor.putString(KEY_PASSWORD, password);
                                editor.putBoolean(KEY_IS_LOGGED_IN, true);
                                editor.apply();

                                Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                
                                // Go to loading screen
                                Intent intent = new Intent(MainActivity.this, LoadingActivity.class);
                                startActivity(intent);
                                finish(); // Close MainActivity
                            } else {
                                // Show server error message if available
                                String errorMessage = loginResponse.msg != null ? loginResponse.msg : "Login failed";
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // Handle different HTTP status codes
                            String errorMessage = "Login failed";
                            if (response.code() == 401) {
                                errorMessage = "User not found or incorrect password";
                            } else if (response.code() == 500) {
                                errorMessage = "Server error. Please try again later";
                            } else if (response.code() == 404) {
                                errorMessage = "Service not found. Please check your connection";
                            }
                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        loginButton.setEnabled(true);
                        loginButton.setText("Sign In");
                        
                        String errorMessage = "No Internet Connection";
                        if (t.getMessage() != null && t.getMessage().contains("timeout")) {
                            errorMessage = "Connection timeout. Please try again";
                        }
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}