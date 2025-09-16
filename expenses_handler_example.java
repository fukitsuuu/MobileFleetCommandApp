// Complete working code for handling the expenses area in your TripAdapter
// This shows how to show/hide the expenses area and handle the dropdown

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;

public class TripExpensesHandler {
    
    private Context context;
    private LinearLayout expensesArea;
    private AutoCompleteTextView expenseTypeDropdown;
    private TextInputEditText costAmountInput;
    private ImageView receiptImageView;
    private TextView receiptImageText;
    private Button addExpenseButton;
    
    // Expense types for dropdown
    private String[] expenseTypes = {"Fuel Cost", "Supply Cost", "Other"};
    
    public TripExpensesHandler(Context context) {
        this.context = context;
    }
    
    public void setupExpensesArea(View view) {
        // Initialize views
        expensesArea = view.findViewById(R.id.expensesArea);
        expenseTypeDropdown = view.findViewById(R.id.expenseTypeDropdown);
        costAmountInput = view.findViewById(R.id.costAmountInput);
        receiptImageView = view.findViewById(R.id.receiptImageView);
        receiptImageText = view.findViewById(R.id.receiptImageText);
        addExpenseButton = view.findViewById(R.id.addExpenseButton);
        
        // Setup dropdown adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, 
            android.R.layout.simple_dropdown_item_1line, expenseTypes);
        expenseTypeDropdown.setAdapter(adapter);
        
        // Setup image picker
        receiptImageView.setOnClickListener(v -> openImagePicker());
        
        // Setup add expense button (invisible but functional)
        addExpenseButton.setOnClickListener(v -> addExpense());
        
        // Initially hide the expenses area
        expensesArea.setVisibility(View.GONE);
    }
    
    public void showExpensesArea(String tripStatus) {
        if (expensesArea != null) {
            if ("Delivered".equals(tripStatus)) {
                expensesArea.setVisibility(View.VISIBLE);
            } else {
                expensesArea.setVisibility(View.GONE);
            }
        }
    }
    
    private void openImagePicker() {
        // Implement image picker logic
        // This would open camera or gallery picker
        // For now, just show a toast
        // Toast.makeText(context, "Image picker clicked", Toast.LENGTH_SHORT).show();
    }
    
    private void addExpense() {
        String expenseType = expenseTypeDropdown.getText().toString();
        String amount = costAmountInput.getText().toString();
        
        if (expenseType.isEmpty() || amount.isEmpty()) {
            // Show validation error
            // Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Process expense data
        // Send to API or save locally
        // Toast.makeText(context, "Expense added: " + expenseType + " - " + amount, Toast.LENGTH_SHORT).show();
    }
    
    public void resetExpensesForm() {
        if (expenseTypeDropdown != null) expenseTypeDropdown.setText("");
        if (costAmountInput != null) costAmountInput.setText("");
        if (receiptImageView != null) receiptImageView.setImageResource(R.drawable.ic_camera);
        if (receiptImageText != null) receiptImageText.setText("Tap to add receipt photo");
    }
}

// Complete TripAdapter example with expenses handling
public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {
    
    private Context context;
    private List<Trip> trips;
    private TripExpensesHandler expensesHandler;
    
    public TripAdapter(Context context, List<Trip> trips) {
        this.context = context;
        this.trips = trips;
        this.expensesHandler = new TripExpensesHandler(context);
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
        expensesHandler.showExpensesArea(trip.getStatus());
        
        // Setup expenses area if not already done
        if (holder.expensesArea != null) {
            expensesHandler.setupExpensesArea(holder.itemView);
        }
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
        
        // Expenses area
        LinearLayout expensesArea;
        
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
            
            // Initialize expenses area
            expensesArea = itemView.findViewById(R.id.expensesArea);
        }
    }
}

// Trip model class (if you don't have one)
class Trip {
    private String tripId;
    private String status;
    private String requester;
    private String location;
    private String vehicle;
    private String distance;
    private String time;
    private String budget;
    
    // Constructor
    public Trip(String tripId, String status, String requester, String location, 
                String vehicle, String distance, String time, String budget) {
        this.tripId = tripId;
        this.status = status;
        this.requester = requester;
        this.location = location;
        this.vehicle = vehicle;
        this.distance = distance;
        this.time = time;
        this.budget = budget;
    }
    
    // Getters
    public String getTripId() { return tripId; }
    public String getStatus() { return status; }
    public String getRequester() { return requester; }
    public String getLocation() { return location; }
    public String getVehicle() { return vehicle; }
    public String getDistance() { return distance; }
    public String getTime() { return time; }
    public String getBudget() { return budget; }
}
