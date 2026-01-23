package com.atiera.mobilefleetcommandapp;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @FormUrlEncoded
    @POST("driver_login.php")
    Call<LoginResponse> driverLogin(
            @Field("username") String username,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("driver_profile.php")
    Call<DriverProfileResponse> getDriverProfile(
            @Field("username") String username
    );

    @FormUrlEncoded
    @POST("driver_trips.php")
    Call<TripResponse> getDriverTrips(
            @Field("username") String username
    );

    // Fetch expenses for a specific trip
    @FormUrlEncoded
    @POST("trip_expenses.php")
    Call<TripExpensesResponse> getTripExpenses(
            @Field("trip_id") String tripId
    );

    @FormUrlEncoded
    @POST("saved_locations.php")
    Call<SavedLocationsResponse> getSavedLocations(
            @Field("username") String username
    );

    // Update trip status (backend should handle this action)
    @FormUrlEncoded
    @POST("driver_trips.php")
    Call<GenericResponse> updateTripStatus(
            @Field("action") String action,
            @Field("trip_id") String tripId,
            @Field("status") String status
    );

    // Fetch maintenance records for a specific driver
    @FormUrlEncoded
    @POST("driver_maintenance.php")
    Call<MaintenanceResponse> getDriverMaintenance(
            @Field("username") String username
    );

    // Update maintenance status
    @FormUrlEncoded
    @POST("driver_maintenance.php")
    Call<GenericResponse> updateMaintenanceStatus(
            @Field("action") String action,
            @Field("maintenance_id") String maintenanceId,
            @Field("status") String status,
            @Field("service_center") String serviceCenter,
            @Field("service_notes") String serviceNotes,
            @Field("cost_service") String costService
    );

    // Update maintenance details
    @FormUrlEncoded
    @POST("update_maintenance.php")
    Call<GenericResponse> updateMaintenanceDetails(
            @Field("action") String action,
            @Field("maintenance_id") String maintenanceId,
            @Field("service_center") String serviceCenter,
            @Field("address") String address,
            @Field("scheduled_date") String scheduledDate,
            @Field("completion_date") String completionDate,
            @Field("cost_service") String costService,
            @Field("service_notes") String serviceNotes,
            @Field("receipt_images") String receiptImages
    );

    // Update vehicle location (for real-time tracking)
    @FormUrlEncoded
    @POST("update_vehicle_location.php")
    Call<GenericResponse> updateVehicleLocation(
            @Field("username") String username,
            @Field("driverID") String driverID,
            @Field("vehicle_number") String vehicleNumber,
            @Field("latitude") String latitude,
            @Field("longitude") String longitude,
            @Field("accuracy") String accuracy,
            @Field("bearing") String bearing,
            @Field("speed") String speed,
            @Field("altitude") String altitude,
            @Field("trip_ID") String tripID
    );

    // Update driver profile
    @FormUrlEncoded
    @POST("update_driver_profile.php")
    Call<GenericResponse> updateDriverProfile(
            @Field("driver_id") String driverId,
            @Field("driver_fname") String firstName,
            @Field("driver_mname") String middleName,
            @Field("driver_lname") String lastName,
            @Field("driver_birthdate") String birthDate,
            @Field("driver_gender") String gender,
            @Field("driver_cnumber") String contactNumber,
            @Field("driver_address") String address,
            @Field("driver_licenseNumber") String licenseNumber,
            @Field("driver_licenseIssued") String licenseIssued,
            @Field("driver_licenseExpiry") String licenseExpiry,
            @Field("driver_emergencyContactName") String emergencyContactName,
            @Field("driver_emergencyContactNumber") String emergencyContactNumber,
            @Field("driver_bloodType") String bloodType
    );

    // Send heartbeat to indicate driver is active
    @FormUrlEncoded
    @POST("driver_heartbeat.php")
    Call<GenericResponse> sendHeartbeat(
            @Field("username") String username,
            @Field("driverID") String driverID
    );

    // --- Driver messaging (driver_messages.php) ---

    @GET("driver_messages.php")
    Call<DriverMessagesResponses.ConversationsResponse> getDriverConversations(
            @Query("action") String action,
            @Query("username") String username
    );

    @GET("driver_messages.php")
    Call<DriverMessagesResponses.HistoryResponse> getMessageHistory(
            @Query("action") String action,
            @Query("username") String username,
            @Query("peer") String peer
    );

    @GET("driver_messages.php")
    Call<DriverMessagesResponses.NewMessagesResponse> getNewMessages(
            @Query("action") String action,
            @Query("username") String username,
            @Query("peer") String peer,
            @Query("since") String since
    );

    @FormUrlEncoded
    @POST("driver_messages.php")
    Call<DriverMessagesResponses.SendMessageResponse> sendDriverMessage(
            @Field("action") String action,
            @Field("username") String username,
            @Field("to") String to,
            @Field("message") String message
    );

    @FormUrlEncoded
    @POST("driver_messages.php")
    Call<DriverMessagesResponses.MarkReadResponse> markMessagesAsRead(
            @Field("action") String action,
            @Field("username") String username,
            @Field("peer") String peer
    );

}


