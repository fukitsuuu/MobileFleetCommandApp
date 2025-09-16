package com.atiera.mobilefleetcommandapp;

import java.util.List;

public class TripExpensesResponse {
    public boolean ok;
    public double totalExpenses;
    public int count;
    public List<Expense> expenses;

    public static class Expense {
        public String fuelId;
        public String tripId;
        public String vehicleNumber;
        public String vehicleName;
        public String driverId;
        public String driverName;
        public double fuelLiters;
        public double fuelCost;
        public List<String> receiptImages;
        public String recordDate;
        public String type; // "Fuel" or "Other"
        public String description; // Description for Supply Cost and Other expenses
    }
}


