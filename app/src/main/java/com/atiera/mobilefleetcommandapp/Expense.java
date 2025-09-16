package com.atiera.mobilefleetcommandapp;

import java.util.Date;

public class Expense {
    private String id;
    private String tripId;
    private String expenseType;
    private double amount;
    private String receiptImagePath;
    private String description;
    private Date createdAt;
    private String status; // PENDING, APPROVED, REJECTED
    
    // Constructor
    public Expense() {}
    
    public Expense(String tripId, String expenseType, double amount, String receiptImagePath, String description) {
        this.tripId = tripId;
        this.expenseType = expenseType;
        this.amount = amount;
        this.receiptImagePath = receiptImagePath;
        this.description = description;
        this.createdAt = new Date();
        this.status = "PENDING";
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    
    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getReceiptImagePath() { return receiptImagePath; }
    public void setReceiptImagePath(String receiptImagePath) { this.receiptImagePath = receiptImagePath; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    // Helper methods
    public String getFormattedAmount() {
        return "₱" + String.format("%.2f", amount);
    }
    
    public String getFormattedType() {
        switch (expenseType) {
            case "Fuel Cost": return "⛽ Fuel Expenses";
            case "Supply Cost": return "📦 Supply Expenses";
            case "Other": return "📋 Other Expenses";
            default: return expenseType;
        }
    }
    
    public boolean isValid() {
        return tripId != null && !tripId.isEmpty() &&
               expenseType != null && !expenseType.isEmpty() &&
               amount > 0;
    }
}
