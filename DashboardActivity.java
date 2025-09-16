package com.atiera.mobileapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    
    private RecyclerView tripRecyclerView;
    private TripAdapter tripAdapter;
    private List<Trip> tripList;
    
    private RecyclerView maintenanceRecyclerView;
    private MaintenanceAdapter maintenanceAdapter;
    private List<Maintenance> maintenanceList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        // Initialize Trip RecyclerView
        tripRecyclerView = findViewById(R.id.tripRecyclerView);
        tripRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize Maintenance RecyclerView
        maintenanceRecyclerView = findViewById(R.id.maintenanceRecyclerView);
        maintenanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Create sample data
        createSampleData();
        
        // Setup adapters
        tripAdapter = new TripAdapter(this, tripList);
        tripRecyclerView.setAdapter(tripAdapter);
        
        maintenanceAdapter = new MaintenanceAdapter(this, maintenanceList);
        maintenanceRecyclerView.setAdapter(maintenanceAdapter);
    }
    
    private void createSampleData() {
        tripList = new ArrayList<>();
        
        // Add sample trips with different statuses
        tripList.add(new Trip(
            "TIDN-1000000",
            "Delivered",  // This one should show expenses area
            "Gerrycho Mendoza",
            "Ikea Moa",
            "VIDN-1000000",
            "35.4 km",
            "35 min",
            "Reimbursement"
        ));
        
        tripList.add(new Trip(
            "TIDN-1000001",
            "In Progress",  // This one should NOT show expenses area
            "John Doe",
            "Mall of Asia",
            "VIDN-1000001",
            "25.0 km",
            "30 min",
            "Company Budget"
        ));
        
        tripList.add(new Trip(
            "TIDN-1000002",
            "Delivered",  // This one should show expenses area
            "Jane Smith",
            "SM Megamall",
            "VIDN-1000002",
            "15.2 km",
            "20 min",
            "Personal"
        ));
        
        // Create sample maintenance data
        maintenanceList = new ArrayList<>();
        
        // Add sample maintenance records with different statuses
        maintenanceList.add(new Maintenance(
            "MIDN-1000000",
            "VIDN-1000000",
            "DIDN-1000000",
            "Oil Change",
            "In Progress",  // This one should show update area
            "Regular maintenance",
            "Auto Service Center",
            "123 Main St, Manila",
            "2025-01-15",
            "",
            "₱2,500.00",
            "",
            "2025-01-10 10:00:00"
        ));
        
        maintenanceList.add(new Maintenance(
            "MIDN-1000001",
            "VIDN-1000001",
            "DIDN-1000001",
            "Brake Inspection",
            "Scheduled",  // This one should show update area
            "Scheduled brake check",
            "",
            "",
            "2025-01-20",
            "",
            "₱0.00",
            "",
            "2025-01-12 14:30:00"
        ));
        
        maintenanceList.add(new Maintenance(
            "MIDN-1000002",
            "VIDN-1000002",
            "DIDN-1000002",
            "Air Filter Replacement",
            "Completed",  // This one should NOT show update area
            "Filter replaced successfully",
            "Quick Lube",
            "456 Service Ave, Quezon City",
            "2025-01-05",
            "2025-01-05",
            "₱800.00",
            "receipt_image.jpg",
            "2025-01-03 09:15:00"
        ));
    }
}
