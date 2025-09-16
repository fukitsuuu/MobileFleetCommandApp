# Maintenance Dropdown System

This document explains how to use the maintenance dropdown system that fetches data from the `maintenance_tb` table, similar to the existing trip dropdown functionality.

## Overview

The maintenance dropdown system provides:
- **Data Fetching**: Retrieves maintenance records from the database via API
- **Status Updates**: Allows drivers to update maintenance status and details
- **Visual Interface**: Dropdown forms for status changes and information updates
- **Real-time Updates**: Syncs changes with the backend database

## Components

### 1. Data Models

#### `Maintenance.java`
```java
// Main data model for maintenance records
public class Maintenance {
    private String maintenanceId;
    private String vehicleNumber;
    private String assignedDriver;
    private String serviceType;
    private String maintenanceStatus;
    // ... other fields
}
```

#### `MaintenanceResponse.java`
```java
// API response wrapper for maintenance data
public class MaintenanceResponse {
    public boolean ok;
    public String msg;
    public List<Maintenance> maintenance;
    public int totalMaintenance;
}
```

### 2. Adapter

#### `MaintenanceAdapter.java`
- Extends `RecyclerView.Adapter`
- Handles maintenance item display and interactions
- Shows/hides update area based on maintenance status
- Manages dropdown selections and form inputs

### 3. API Integration

#### `ApiService.java` - New Endpoints
```java
// Fetch maintenance records for a driver
@POST("driver_maintenance.php")
Call<MaintenanceResponse> getDriverMaintenance(@Field("username") String username);

// Update maintenance status and details
@POST("driver_maintenance.php")
Call<GenericResponse> updateMaintenanceStatus(
    @Field("action") String action,
    @Field("maintenance_id") String maintenanceId,
    @Field("status") String status,
    @Field("service_center") String serviceCenter,
    @Field("service_notes") String serviceNotes,
    @Field("cost_service") String costService
);
```

#### `api/driver_maintenance.php`
- Backend API endpoint for maintenance operations
- Handles both data retrieval and status updates
- Validates maintenance status transitions
- Updates database with new information

### 4. UI Components

#### `maintenance_item.xml`
- Layout for individual maintenance records
- Includes main info display and collapsible update area
- Form fields for status, service center, notes, and cost
- Image picker for receipt uploads

## Database Schema

The system works with the `maintenance_tb` table:

```sql
CREATE TABLE `maintenance_tb` (
  `pk` int(11) NOT NULL,
  `MaintenanceID` varchar(50) NOT NULL,
  `vehicle_number` varchar(50) NOT NULL,
  `assigned_driver` varchar(50) DEFAULT NULL,
  `Service_Type` varchar(100) NOT NULL,
  `maintenance_status` varchar(50) NOT NULL,
  `Service_Notes` text DEFAULT NULL,
  `Service_Center` varchar(100) DEFAULT NULL,
  `servicecenter_address` varchar(255) NOT NULL,
  `Scheduled_Date` date DEFAULT NULL,
  `Completion_Date` date DEFAULT NULL,
  `Cost_Service` decimal(10,2) DEFAULT NULL,
  `Receipt_Uploads` varchar(255) DEFAULT NULL,
  `date_added` datetime NOT NULL DEFAULT current_timestamp()
);
```

## Usage

### 1. Basic Setup

```java
// In your Activity
private RecyclerView maintenanceRecyclerView;
private MaintenanceAdapter maintenanceAdapter;
private List<Maintenance> maintenanceList;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dashboard);
    
    // Initialize RecyclerView
    maintenanceRecyclerView = findViewById(R.id.maintenanceRecyclerView);
    maintenanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    
    // Initialize data and adapter
    maintenanceList = new ArrayList<>();
    maintenanceAdapter = new MaintenanceAdapter(this, maintenanceList);
    maintenanceRecyclerView.setAdapter(maintenanceAdapter);
    
    // Fetch data
    fetchMaintenanceData();
}
```

### 2. Fetching Data

```java
private void fetchMaintenanceData() {
    String username = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                     .getString("username", "");
    
    ApiClient.get().getDriverMaintenance(username).enqueue(new Callback<MaintenanceResponse>() {
        @Override
        public void onResponse(Call<MaintenanceResponse> call, Response<MaintenanceResponse> response) {
            if (response.isSuccessful() && response.body() != null && response.body().ok) {
                // Update your maintenance list
                maintenanceList.clear();
                for (MaintenanceResponse.Maintenance apiItem : response.body().maintenance) {
                    // Convert API response to local Maintenance objects
                    Maintenance maintenance = new Maintenance(/* ... */);
                    maintenanceList.add(maintenance);
                }
                maintenanceAdapter.notifyDataSetChanged();
            }
        }
        
        @Override
        public void onFailure(Call<MaintenanceResponse> call, Throwable t) {
            // Handle error
        }
    });
}
```

### 3. Updating Maintenance Records

```java
private void updateMaintenance(String maintenanceId, String status, 
                             String serviceCenter, String serviceNotes, String cost) {
    
    ApiClient.get().updateMaintenanceStatus("update_status", maintenanceId, 
                                           status, serviceCenter, serviceNotes, cost)
            .enqueue(new Callback<GenericResponse>() {
        @Override
        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
            if (response.isSuccessful() && response.body() != null && response.body().ok) {
                // Success - refresh data
                fetchMaintenanceData();
            }
        }
        
        @Override
        public void onFailure(Call<GenericResponse> call, Throwable t) {
            // Handle error
        }
    });
}
```

## Status Flow

The maintenance system supports the following status transitions:

1. **Pending** → **Scheduled** → **In Progress** → **Completed**
2. **Pending** → **Cancelled**
3. **Scheduled** → **Cancelled**
4. **In Progress** → **Cancelled**

### Status Colors
- **Completed**: Green
- **In Progress**: Blue
- **Scheduled**: Orange
- **Pending**: Orange
- **Cancelled**: Red
- **Overdue**: Red

## Features

### 1. Conditional Display
- Update area is only shown for "In Progress" and "Scheduled" statuses
- Completed and cancelled maintenance records show read-only information

### 2. Form Validation
- Status selection is required for updates
- Cost input accepts decimal values
- Service notes support multi-line text

### 3. Image Upload
- Receipt image picker for maintenance documentation
- Placeholder UI for image selection
- Support for camera and gallery selection

### 4. Real-time Updates
- Changes are immediately synced with the database
- UI refreshes after successful updates
- Error handling for failed operations

## Integration with Existing System

The maintenance dropdown integrates seamlessly with the existing trip system:

1. **Similar UI Patterns**: Uses the same card-based layout and dropdown patterns
2. **Consistent API Structure**: Follows the same API response format
3. **Shared Components**: Reuses existing UI components and styling
4. **Unified Dashboard**: Both trips and maintenance are displayed on the same dashboard

## Customization

### Adding New Status Options
1. Update the status array in `MaintenanceAdapter.java`:
```java
String[] statusOptions = {"In Progress", "Completed", "Cancelled", "Your New Status"};
```

2. Update the status color mapping in `setStatusColor()` method

3. Update the backend validation in `driver_maintenance.php`

### Adding New Fields
1. Update the `Maintenance.java` model class
2. Add UI components to `maintenance_item.xml`
3. Update the adapter to handle new fields
4. Modify the API endpoint to process new data

## Error Handling

The system includes comprehensive error handling:

- **Network Errors**: Graceful handling of connection issues
- **Validation Errors**: Input validation with user feedback
- **API Errors**: Server error handling with meaningful messages
- **Data Errors**: Handling of malformed or missing data

## Performance Considerations

- **Lazy Loading**: Data is fetched only when needed
- **Efficient Updates**: Only changed records are updated
- **Caching**: Consider implementing local caching for offline support
- **Pagination**: For large datasets, implement pagination

## Security

- **Authentication**: All API calls require valid user authentication
- **Input Validation**: Server-side validation of all inputs
- **SQL Injection Prevention**: Uses prepared statements
- **File Upload Security**: Validates file types and sizes

## Testing

### Unit Tests
- Test data model serialization/deserialization
- Test adapter functionality
- Test API response handling

### Integration Tests
- Test complete data flow from API to UI
- Test status update workflows
- Test error scenarios

### UI Tests
- Test dropdown interactions
- Test form validation
- Test image picker functionality

## Future Enhancements

1. **Offline Support**: Cache maintenance data for offline viewing
2. **Push Notifications**: Notify drivers of maintenance updates
3. **Photo Upload**: Implement actual image upload functionality
4. **Maintenance History**: Show detailed maintenance history
5. **Cost Tracking**: Enhanced cost analysis and reporting
6. **Scheduling**: Allow drivers to schedule maintenance appointments
