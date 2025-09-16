package com.atiera.mobileapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

public class MaintenanceAdapter extends RecyclerView.Adapter<MaintenanceAdapter.MaintenanceViewHolder> {
    
    private Context context;
    private List<Maintenance> maintenanceList;
    
    public MaintenanceAdapter(Context context, List<Maintenance> maintenanceList) {
        this.context = context;
        this.maintenanceList = maintenanceList;
    }
    
    @Override
    public MaintenanceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.maintenance_item, parent, false);
        return new MaintenanceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(MaintenanceViewHolder holder, int position) {
        Maintenance maintenance = maintenanceList.get(position);
        
        // Set maintenance data
        holder.maintenanceIdText.setText(maintenance.getMaintenanceId());
        holder.statusText.setText(maintenance.getMaintenanceStatus());
        holder.vehicleText.setText(maintenance.getVehicleNumber());
        holder.serviceTypeText.setText(maintenance.getServiceType());
        holder.driverText.setText(maintenance.getAssignedDriver());
        holder.scheduledDateText.setText(maintenance.getScheduledDate());
        holder.costText.setText(maintenance.getCostService());
        
        // Show/hide update area based on status
        if ("In Progress".equals(maintenance.getMaintenanceStatus()) || 
            "Scheduled".equals(maintenance.getMaintenanceStatus())) {
            holder.updateArea.setVisibility(View.VISIBLE);
            setupUpdateArea(holder, maintenance);
        } else {
            holder.updateArea.setVisibility(View.GONE);
        }
        
        // Set status color
        setStatusColor(holder.statusText, maintenance.getMaintenanceStatus());
    }
    
    private void setupUpdateArea(MaintenanceViewHolder holder, Maintenance maintenance) {
        // Setup status dropdown adapter
        String[] statusOptions = {"In Progress", "Completed", "Cancelled"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(context, 
            android.R.layout.simple_dropdown_item_1line, statusOptions);
        holder.statusDropdown.setAdapter(statusAdapter);
        
        // Set current status as default
        holder.statusDropdown.setText(maintenance.getMaintenanceStatus(), false);
        
        // Setup service center input
        if (maintenance.getServiceCenter() != null && !maintenance.getServiceCenter().isEmpty()) {
            holder.serviceCenterInput.setText(maintenance.getServiceCenter());
        }
        
        // Setup service notes input
        if (maintenance.getServiceNotes() != null && !maintenance.getServiceNotes().isEmpty()) {
            holder.serviceNotesInput.setText(maintenance.getServiceNotes());
        }
        
        // Setup cost input
        if (maintenance.getCostService() != null && !maintenance.getCostService().isEmpty()) {
            holder.costInput.setText(maintenance.getCostService());
        }
        
        // Setup image picker
        holder.receiptImageView.setOnClickListener(v -> {
            // TODO: Implement image picker for maintenance receipts
            // Toast.makeText(context, "Receipt image picker clicked", Toast.LENGTH_SHORT).show();
        });
        
        // Setup update button
        holder.updateButton.setOnClickListener(v -> {
            String newStatus = holder.statusDropdown.getText().toString();
            String serviceCenter = holder.serviceCenterInput.getText().toString();
            String serviceNotes = holder.serviceNotesInput.getText().toString();
            String cost = holder.costInput.getText().toString();
            
            if (newStatus.isEmpty()) {
                // TODO: Show validation error
                return;
            }
            
            // TODO: Process maintenance update data
            // This would typically call an API to update the maintenance record
            // Toast.makeText(context, "Maintenance updated: " + newStatus, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void setStatusColor(TextView statusText, String status) {
        int color;
        switch (status.toLowerCase()) {
            case "completed":
                color = context.getResources().getColor(android.R.color.holo_green_dark);
                break;
            case "in progress":
                color = context.getResources().getColor(android.R.color.holo_blue_dark);
                break;
            case "scheduled":
                color = context.getResources().getColor(android.R.color.holo_orange_dark);
                break;
            case "pending":
                color = context.getResources().getColor(android.R.color.holo_orange_dark);
                break;
            case "cancelled":
                color = context.getResources().getColor(android.R.color.holo_red_dark);
                break;
            case "overdue":
                color = context.getResources().getColor(android.R.color.holo_red_dark);
                break;
            default:
                color = context.getResources().getColor(android.R.color.darker_gray);
                break;
        }
        statusText.setTextColor(color);
    }
    
    @Override
    public int getItemCount() {
        return maintenanceList.size();
    }
    
    public class MaintenanceViewHolder extends RecyclerView.ViewHolder {
        // Main maintenance info views
        TextView maintenanceIdText;
        TextView statusText;
        TextView vehicleText;
        TextView serviceTypeText;
        TextView driverText;
        TextView scheduledDateText;
        TextView costText;
        
        // Update area views
        LinearLayout updateArea;
        AutoCompleteTextView statusDropdown;
        TextInputEditText serviceCenterInput;
        TextInputEditText serviceNotesInput;
        TextInputEditText costInput;
        ImageView receiptImageView;
        TextView receiptImageText;
        Button updateButton;
        
        public MaintenanceViewHolder(View itemView) {
            super(itemView);
            
            // Initialize main views
            maintenanceIdText = itemView.findViewById(R.id.maintenanceIdText);
            statusText = itemView.findViewById(R.id.statusText);
            vehicleText = itemView.findViewById(R.id.vehicleText);
            serviceTypeText = itemView.findViewById(R.id.serviceTypeText);
            driverText = itemView.findViewById(R.id.driverText);
            scheduledDateText = itemView.findViewById(R.id.scheduledDateText);
            costText = itemView.findViewById(R.id.costText);
            
            // Initialize update area views
            updateArea = itemView.findViewById(R.id.updateArea);
            statusDropdown = itemView.findViewById(R.id.statusDropdown);
            serviceCenterInput = itemView.findViewById(R.id.serviceCenterInput);
            serviceNotesInput = itemView.findViewById(R.id.serviceNotesInput);
            costInput = itemView.findViewById(R.id.costInput);
            receiptImageView = itemView.findViewById(R.id.receiptImageView);
            receiptImageText = itemView.findViewById(R.id.receiptImageText);
            updateButton = itemView.findViewById(R.id.updateButton);
        }
    }
}
