package com.atiera.mobilefleetcommandapp;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

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

}


