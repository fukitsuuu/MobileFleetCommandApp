package com.atiera.mobilefleetcommandapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class LocationTrackingService extends Service {
    private static final String TAG = "LocationTrackingService";
    private static final String CHANNEL_ID = "location_tracking_channel";
    private static final int NOTIFICATION_ID = 1;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private SharedPreferences prefs;
    private String username;
    private String driverID;
    private String vehicleNumber;
    private String currentTripID;
    private Handler tripCheckHandler;
    private Runnable tripCheckRunnable;
    private boolean isLocationTrackingActive = false;
    
    // Active trip statuses that require location tracking
    private static final String[] ACTIVE_TRIP_STATUSES = {
        "Departed", "In Transit", "In transit", "Picked Up", "Picked up", "Delivered"
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        try {
            prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            username = prefs.getString("username", "");
            driverID = prefs.getString("driverID", "");
            vehicleNumber = prefs.getString("vehicleNumber", "");
            
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            createNotificationChannel();
            
            // Start foreground service with notification
            try {
                startForeground(NOTIFICATION_ID, createNotification());
            } catch (Exception e) {
                Log.e(TAG, "Error starting foreground service: " + e.getMessage());
                // Try to continue without foreground notification (not ideal but prevents crash)
                e.printStackTrace();
            }
            
            createLocationRequest();
            createLocationCallback();
            
            // Check for active trips every 60 seconds
            tripCheckHandler = new Handler(Looper.getMainLooper());
            startTripStatusMonitoring();
        } catch (Exception e) {
            Log.e(TAG, "Error in service onCreate: " + e.getMessage());
            e.printStackTrace();
            // Don't crash, just log the error
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        checkActiveTripAndStartTracking();
        return START_STICKY; // Service restarts if killed
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Tracking vehicle location during active trips");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        // Use a simple icon that's always available
        int iconResId = android.R.drawable.ic_menu_mylocation;
        try {
            // Try to use app icon if available, fallback to system icon
            iconResId = getApplicationInfo().icon != 0 ? getApplicationInfo().icon : android.R.drawable.ic_menu_mylocation;
        } catch (Exception e) {
            Log.e(TAG, "Error getting icon: " + e.getMessage());
        }
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Tracking your vehicle location for active trips")
            .setSmallIcon(iconResId)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }
    
    private void createLocationRequest() {
        try {
            // For Google Play Services Location 21.0.1, use create() method
            locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000) // Update every 10 seconds
                .setFastestInterval(5000) // Fastest update: 5 seconds (if device moves faster)
                .setMaxWaitTime(60000) // Maximum wait time: 1 minute
                .setSmallestDisplacement(0); // Update every 10 seconds regardless of movement (0 = no displacement filter)
            Log.d(TAG, "Location request created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating location request: " + e.getMessage());
            e.printStackTrace();
            // Create minimal request as fallback
            try {
                locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(10000); // Update every 10 seconds
                locationRequest.setFastestInterval(5000); // Fastest update: 5 seconds
                Log.d(TAG, "Created minimal location request");
            } catch (Exception e2) {
                Log.e(TAG, "Critical: Cannot create LocationRequest: " + e2.getMessage());
                // Set to null, service will handle this gracefully
                locationRequest = null;
            }
        }
    }
    
    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                
                Location location = locationResult.getLastLocation();
                if (location != null && currentTripID != null && !currentTripID.isEmpty()) {
                    sendLocationToServer(location);
                } else if (location != null && (currentTripID == null || currentTripID.isEmpty())) {
                    // Trip ended, stop tracking
                    Log.d(TAG, "No active trip, stopping location updates");
                    stopLocationUpdates();
                }
            }
        };
    }
    
    /**
     * Monitor trip status and start/stop tracking accordingly
     */
    private void startTripStatusMonitoring() {
        tripCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkActiveTripAndStartTracking();
                // Check again after 60 seconds
                tripCheckHandler.postDelayed(this, 60000);
            }
        };
        tripCheckHandler.postDelayed(tripCheckRunnable, 5000); // First check after 5 seconds
    }
    
    /**
     * Fetch active trips from API and start/stop tracking based on trip status
     */
    private void checkActiveTripAndStartTracking() {
        if (username == null || username.isEmpty()) {
            Log.e(TAG, "Username not available");
            stopLocationUpdates();
            return;
        }
        
        // Ensure driverID is available - try to get from preferences first
        if (driverID == null || driverID.isEmpty()) {
            driverID = prefs.getString("driverID", "");
            if (driverID == null || driverID.isEmpty()) {
                Log.w(TAG, "DriverID not found in preferences, will fetch from profile");
                // Try to fetch driver profile to get driverID
                fetchDriverIDFromProfile();
                return; // Will retry after fetching profile
            }
        }
        
        Log.d(TAG, "Fetching trips for username: " + username);
        ApiClient.get().getDriverTrips(username).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().ok) {
                    List<TripResponse.Trip> trips = response.body().trips;
                    Log.d(TAG, "Received " + (trips != null ? trips.size() : 0) + " trips from API");
                    
                    if (trips != null && !trips.isEmpty()) {
                        // Log all trip statuses for debugging
                        for (TripResponse.Trip t : trips) {
                            Log.d(TAG, "Trip " + t.tripID + " - Status: " + (t.status != null ? t.status : "null"));
                        }
                    }
                    
                    TripResponse.Trip activeTrip = findActiveTrip(trips);
                    
                    if (activeTrip != null) {
                        // Driver has an active trip - start tracking
                        String newTripID = activeTrip.tripID;
                        String newVehicleNumber = activeTrip.vehicleNumber != null ? activeTrip.vehicleNumber : "";
                        
                        // Update trip ID and vehicle number if changed or null
                        boolean tripChanged = (currentTripID == null || !newTripID.equals(currentTripID));
                        boolean vehicleChanged = (vehicleNumber == null || !newVehicleNumber.equals(vehicleNumber));
                        
                        if (tripChanged || vehicleChanged) {
                            currentTripID = newTripID;
                            vehicleNumber = newVehicleNumber;
                            
                            // Save to preferences
                            prefs.edit()
                                .putString("currentTripID", currentTripID)
                                .putString("vehicleNumber", vehicleNumber)
                                .apply();
                            
                            Log.d(TAG, "Active trip found: " + currentTripID + ", Vehicle: " + vehicleNumber);
                            
                            // If trip changed, restart location tracking to ensure fresh start
                            if (tripChanged && isLocationTrackingActive) {
                                Log.d(TAG, "Trip changed, restarting location tracking");
                                stopLocationUpdates();
                                isLocationTrackingActive = false;
                            }
                        }
                        
                        // Start location tracking if not already active
                        if (!isLocationTrackingActive) {
                            Log.d(TAG, "Starting location tracking for trip: " + currentTripID);
                            startLocationUpdates();
                        } else {
                            Log.d(TAG, "Location tracking already active for trip: " + currentTripID);
                        }
                    } else {
                        // No active trip - stop tracking
                        Log.d(TAG, "No active trip found in " + (trips != null ? trips.size() : 0) + " trips");
                        if (isLocationTrackingActive) {
                            Log.d(TAG, "Stopping location tracking - no active trip");
                            stopLocationUpdates();
                        }
                        // Clear cached trip ID
                        prefs.edit().remove("currentTripID").apply();
                        currentTripID = null;
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().msg : "Unknown error";
                    Log.e(TAG, "Failed to fetch trips - Response code: " + response.code() + ", Message: " + errorMsg);
                    // On error, don't stop tracking to avoid interruption
                }
            }
            
            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching trips: " + t.getMessage());
                t.printStackTrace();
                // On error, don't stop tracking to avoid interruption
            }
        });
    }
    
    /**
     * Fetch driverID from profile if not available in preferences
     */
    private void fetchDriverIDFromProfile() {
        if (username == null || username.isEmpty()) {
            Log.e(TAG, "Cannot fetch profile: username not available");
            return;
        }
        
        ApiClient.get().getDriverProfile(username).enqueue(new Callback<DriverProfileResponse>() {
            @Override
            public void onResponse(Call<DriverProfileResponse> call, Response<DriverProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().ok && 
                    response.body().driver != null && response.body().driver.idNumber != null) {
                    driverID = response.body().driver.idNumber;
                    prefs.edit().putString("driverID", driverID).apply();
                    Log.d(TAG, "DriverID fetched from profile: " + driverID);
                    // Now retry checking for active trips
                    checkActiveTripAndStartTracking();
                } else {
                    Log.e(TAG, "Failed to fetch driver profile for driverID");
                }
            }
            
            @Override
            public void onFailure(Call<DriverProfileResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching driver profile: " + t.getMessage());
            }
        });
    }
    
    /**
     * Find the first active trip from the list
     */
    private TripResponse.Trip findActiveTrip(List<TripResponse.Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            Log.d(TAG, "No trips found or trips list is empty");
            return null;
        }
        
        Log.d(TAG, "Checking " + trips.size() + " trips for active status");
        for (TripResponse.Trip trip : trips) {
            if (trip.status != null) {
                String status = trip.status.trim();
                Log.d(TAG, "Checking trip " + trip.tripID + " with status: " + status);
                // Check if status is in active statuses list
                for (String activeStatus : ACTIVE_TRIP_STATUSES) {
                    if (status.equalsIgnoreCase(activeStatus)) {
                        Log.d(TAG, "Found active trip: " + trip.tripID + " with status: " + status);
                        return trip;
                    }
                }
            } else {
                Log.d(TAG, "Trip " + trip.tripID + " has null status");
            }
        }
        Log.d(TAG, "No active trips found matching statuses: " + java.util.Arrays.toString(ACTIVE_TRIP_STATUSES));
        return null;
    }
    
    private void startLocationUpdates() {
        try {
            if (locationRequest == null) {
                Log.e(TAG, "LocationRequest is null, cannot start updates");
                return;
            }
            
            if (fusedLocationClient == null || locationCallback == null) {
                Log.e(TAG, "Location client or callback is null");
                return;
            }
            
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Location permission not granted");
                return;
            }
            
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
            
            SettingsClient client = LocationServices.getSettingsClient(this);
            com.google.android.gms.tasks.Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
            
            task.addOnSuccessListener(locationSettingsResponse -> {
                try {
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    );
                    isLocationTrackingActive = true;
                    Log.d(TAG, "Location updates started for trip: " + currentTripID);
                } catch (SecurityException e) {
                    Log.e(TAG, "Security exception starting location updates: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Exception starting location updates: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            task.addOnFailureListener(e -> {
                Log.e(TAG, "Location settings not satisfied: " + e.getMessage());
            });
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception in startLocationUpdates: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception in startLocationUpdates: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void sendLocationToServer(Location location) {
        // Double-check we still have an active trip before sending
        if (currentTripID == null || currentTripID.isEmpty()) {
            Log.d(TAG, "No active trip, skipping location update");
            return;
        }
        
        // Ensure we have required fields
        if (driverID == null || driverID.isEmpty() || vehicleNumber == null || vehicleNumber.isEmpty()) {
            Log.e(TAG, "Missing driverID or vehicleNumber, cannot send location");
            return;
        }
        
        String accuracyStr = location.hasAccuracy() ? String.valueOf(location.getAccuracy()) : null;
        String bearingStr = (location.hasBearing() && location.getBearing() != 0.0f) ? String.valueOf(location.getBearing()) : null;
        String speedStr = (location.hasSpeed() && location.getSpeed() != 0.0f) ? String.valueOf(location.getSpeed()) : null;
        String altitudeStr = location.hasAltitude() ? String.valueOf(location.getAltitude()) : null;
        
        Log.d(TAG, "Sending location to server - TripID: " + currentTripID + 
                   ", DriverID: " + driverID + ", Vehicle: " + vehicleNumber + 
                   ", Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
        
        ApiClient.get().updateVehicleLocation(
            username != null ? username : "",
            driverID != null ? driverID : "",
            vehicleNumber != null ? vehicleNumber : "",
            String.valueOf(location.getLatitude()),
            String.valueOf(location.getLongitude()),
            accuracyStr,
            bearingStr,
            speedStr,
            altitudeStr,
            currentTripID != null ? currentTripID : ""
        ).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().ok) {
                        Log.d(TAG, "✓ Location sent successfully for trip: " + currentTripID);
                    } else {
                        Log.e(TAG, "✗ API returned error: " + response.body().msg);
                    }
                } else {
                    Log.e(TAG, "✗ HTTP error: " + response.code() + " - " + 
                        (response.body() != null ? response.body().msg : "Unknown error"));
                }
            }
            
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Log.e(TAG, "✗ Network error sending location: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }
    
    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null && isLocationTrackingActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationTrackingActive = false;
            Log.d(TAG, "Location updates stopped");
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        if (tripCheckHandler != null && tripCheckRunnable != null) {
            tripCheckHandler.removeCallbacks(tripCheckRunnable);
        }
        stopLocationUpdates();
    }
}
