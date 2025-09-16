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

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {
    
    private Context context;
    private List<Trip> trips;
    
    public TripAdapter(Context context, List<Trip> trips) {
        this.context = context;
        this.trips = trips;
    }
    
    @Override
    public TripViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.trip_item, parent, false);
        return new TripViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(TripViewHolder holder, int position) {
        Trip trip = trips.get(position);
        
        // Set trip data
        holder.tripIdText.setText(trip.getTripId());
        holder.statusText.setText(trip.getStatus());
        holder.requesterText.setText(trip.getRequester());
        holder.locationText.setText(trip.getLocation());
        holder.vehicleText.setText(trip.getVehicle());
        holder.distanceText.setText(trip.getDistance());
        holder.timeText.setText(trip.getTime());
        holder.budgetText.setText(trip.getBudget());
        
        // Show/hide expenses area based on status
        if ("Delivered".equals(trip.getStatus())) {
            holder.expensesArea.setVisibility(View.VISIBLE);
            setupExpensesArea(holder);
        } else {
            holder.expensesArea.setVisibility(View.GONE);
        }
    }
    
    private void setupExpensesArea(TripViewHolder holder) {
        // Setup dropdown adapter
        String[] expenseTypes = {"Fuel Cost", "Supply Cost", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, 
            android.R.layout.simple_dropdown_item_1line, expenseTypes);
        holder.expenseTypeDropdown.setAdapter(adapter);
        
        // Setup image picker
        holder.receiptImageView.setOnClickListener(v -> {
            // TODO: Implement image picker
            // Toast.makeText(context, "Image picker clicked", Toast.LENGTH_SHORT).show();
        });
        
        // Setup add expense button
        holder.addExpenseButton.setOnClickListener(v -> {
            String expenseType = holder.expenseTypeDropdown.getText().toString();
            String amount = holder.costAmountInput.getText().toString();
            
            if (expenseType.isEmpty() || amount.isEmpty()) {
                // TODO: Show validation error
                return;
            }
            
            // TODO: Process expense data
            // Toast.makeText(context, "Expense added: " + expenseType + " - " + amount, Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public int getItemCount() {
        return trips.size();
    }
    
    public class TripViewHolder extends RecyclerView.ViewHolder {
        // Existing views
        TextView tripIdText;
        TextView statusText;
        TextView requesterText;
        TextView locationText;
        TextView vehicleText;
        TextView distanceText;
        TextView timeText;
        TextView budgetText;
        
        // Expenses area views
        LinearLayout expensesArea;
        AutoCompleteTextView expenseTypeDropdown;
        TextInputEditText costAmountInput;
        ImageView receiptImageView;
        TextView receiptImageText;
        Button addExpenseButton;
        
        public TripViewHolder(View itemView) {
            super(itemView);
            
            // Initialize existing views
            tripIdText = itemView.findViewById(R.id.tripIdText);
            statusText = itemView.findViewById(R.id.statusText);
            requesterText = itemView.findViewById(R.id.requesterText);
            locationText = itemView.findViewById(R.id.locationText);
            vehicleText = itemView.findViewById(R.id.vehicleText);
            distanceText = itemView.findViewById(R.id.distanceText);
            timeText = itemView.findViewById(R.id.timeText);
            budgetText = itemView.findViewById(R.id.budgetText);
            
            // Initialize expenses area views
            expensesArea = itemView.findViewById(R.id.expensesArea);
            expenseTypeDropdown = itemView.findViewById(R.id.expenseTypeDropdown);
            costAmountInput = itemView.findViewById(R.id.costAmountInput);
            receiptImageView = itemView.findViewById(R.id.receiptImageView);
            receiptImageText = itemView.findViewById(R.id.receiptImageText);
            addExpenseButton = itemView.findViewById(R.id.addExpenseButton);
        }
    }
}
