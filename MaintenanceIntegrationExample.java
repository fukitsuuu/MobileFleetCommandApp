package com.atiera.mobileapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Example class showing how to integrate maintenance dropdown with real API calls
 * This demonstrates how to fetch maintenance data from the database and update it
 */
public class MaintenanceIntegrationExample {
    
    private Context context;
    private MaintenanceAdapter maintenanceAdapter;
    private List<Maintenance> maintenanceList;
    private SharedPreferences sharedPreferences;
    
    public MaintenanceIntegrationExample(Context context, MaintenanceAdapter adapter, List<Maintenance> list) {
        this.context = context;
        this.maintenanceAdapter = adapter;
        this.maintenanceList = list;
        this.sharedPreferences = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
    }
    
    /**
     * Fetch maintenance records for the logged-in driver
     */
    public void fetchMaintenanceData() {
        String username = sharedPreferences.getString("username", "");
        if (username.isEmpty()) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Call the API to get maintenance data
        ApiClient.get().getDriverMaintenance(username).enqueue(new Callback<MaintenanceResponse>() {
            @Override
            public void onResponse(Call<MaintenanceResponse> call, Response<MaintenanceResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().ok) {
                    List<MaintenanceResponse.Maintenance> apiMaintenance = response.body().maintenance;
                    
                    // Clear existing data
                    maintenanceList.clear();
                    
                    // Convert API response to local Maintenance objects
                    for (MaintenanceResponse.Maintenance apiItem : apiMaintenance) {
                        Maintenance maintenance = new Maintenance(
                            apiItem.getMaintenanceID(),
                            apiItem.getVehicleNumber(),
                            apiItem.getAssignedDriver(),
                            apiItem.getServiceType(),
                            apiItem.getMaintenanceStatus(),
                            apiItem.getServiceNotes(),
                            apiItem.getServiceCenter(),
                            apiItem.getServiceCenterAddress(),
                            apiItem.getScheduledDate(),
                            apiItem.getCompletionDate(),
                            apiItem.getCostService(),
                            apiItem.getReceiptUploads(),
                            apiItem.getDateAdded()
                        );
                        maintenanceList.add(maintenance);
                    }
                    
                    // Notify adapter of data change
                    maintenanceAdapter.notifyDataSetChanged();
                    
                    Toast.makeText(context, "Maintenance data loaded: " + maintenanceList.size() + " records", 
                                 Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to load maintenance data", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<MaintenanceResponse> call, Throwable t) {
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Update maintenance record status and details
     */
    public void updateMaintenanceRecord(String maintenanceId, String status, String serviceCenter, 
                                      String serviceNotes, String costService) {
        
        // Call the API to update maintenance record
        ApiClient.get().updateMaintenanceStatus("update_status", maintenanceId, status, 
                                               serviceCenter, serviceNotes, costService)
                .enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().ok) {
                    Toast.makeText(context, "Maintenance updated successfully", Toast.LENGTH_SHORT).show();
                    
                    // Refresh the maintenance data
                    fetchMaintenanceData();
                } else {
                    String errorMsg = response.body() != null ? response.body().msg : "Update failed";
                    Toast.makeText(context, "Update failed: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(context, "Update failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * Example of how to handle maintenance status updates from the adapter
     */
    public void handleMaintenanceUpdate(Maintenance maintenance, String newStatus, 
                                      String serviceCenter, String serviceNotes, String cost) {
        
        // Validate input
        if (newStatus.isEmpty()) {
            Toast.makeText(context, "Please select a status", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show confirmation dialog (optional)
        // You can add a confirmation dialog here if needed
        
        // Update the maintenance record
        updateMaintenanceRecord(maintenance.getMaintenanceId(), newStatus, serviceCenter, serviceNotes, cost);
    }
    
    /**
     * Filter maintenance records by status
     */
    public void filterMaintenanceByStatus(String status) {
        List<Maintenance> filteredList = new ArrayList<>();
        
        for (Maintenance maintenance : maintenanceList) {
            if (status.equals("All") || maintenance.getMaintenanceStatus().equals(status)) {
                filteredList.add(maintenance);
            }
        }
        
        // Update adapter with filtered data
        // Note: In a real implementation, you might want to create a separate filtered adapter
        // or use the original list with a filter mechanism
    }
    
    /**
     * Get maintenance statistics
     */
    public MaintenanceStats getMaintenanceStats() {
        int total = maintenanceList.size();
        int completed = 0;
        int inProgress = 0;
        int scheduled = 0;
        int pending = 0;
        
        for (Maintenance maintenance : maintenanceList) {
            String status = maintenance.getMaintenanceStatus();
            switch (status) {
                case "Completed":
                    completed++;
                    break;
                case "In Progress":
                    inProgress++;
                    break;
                case "Scheduled":
                    scheduled++;
                    break;
                case "Pending":
                    pending++;
                    break;
            }
        }
        
        return new MaintenanceStats(total, completed, inProgress, scheduled, pending);
    }
    
    /**
     * Data class for maintenance statistics
     */
    public static class MaintenanceStats {
        public int total;
        public int completed;
        public int inProgress;
        public int scheduled;
        public int pending;
        
        public MaintenanceStats(int total, int completed, int inProgress, int scheduled, int pending) {
            this.total = total;
            this.completed = completed;
            this.inProgress = inProgress;
            this.scheduled = scheduled;
            this.pending = pending;
        }
    }
}
