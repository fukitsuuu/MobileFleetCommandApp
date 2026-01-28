package com.atiera.mobilefleetcommandapp;

import java.util.Date;

public class Expense {
    private String id;
    private String tripId;
    private String expenseType;
    private double amount;
    private double fuelConsumption; // Fuel quantity/consumption in liters
    private String receiptImagePath;
    private String description;
    private String serialNumber;
    private String invoiceDate;
    private Date createdAt;
    private String status; // PENDING, APPROVED, REJECTED
    
    // Constructor
    public Expense() {}
    
    public Expense(String tripId, String expenseType, double amount, String receiptImagePath, String description) {
        this.tripId = tripId;
        this.expenseType = expenseType;
        this.amount = amount;
        this.fuelConsumption = 0.0;
        this.receiptImagePath = receiptImagePath;
        this.description = description;
        this.serialNumber = "";
        this.invoiceDate = "";
        this.createdAt = new Date();
        this.status = "PENDING";
    }
    
    public Expense(String tripId, String expenseType, double amount, double fuelConsumption, String receiptImagePath, String description, String serialNumber, String invoiceDate) {
        this.tripId = tripId;
        this.expenseType = expenseType;
        this.amount = amount;
        this.fuelConsumption = fuelConsumption;
        this.receiptImagePath = receiptImagePath;
        this.description = description;
        this.serialNumber = serialNumber;
        this.invoiceDate = invoiceDate;
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
    
    public double getFuelConsumption() { return fuelConsumption; }
    public void setFuelConsumption(double fuelConsumption) { this.fuelConsumption = fuelConsumption; }
    
    public String getReceiptImagePath() { return receiptImagePath; }
    public void setReceiptImagePath(String receiptImagePath) { this.receiptImagePath = receiptImagePath; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    
    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    // Helper methods
    public String getFormattedAmount() {
        return "₱" + String.format("%.2f", amount);
    }
    
    public String getFormattedType() {
        if ("Fuel Cost".equals(expenseType)) {
            return "⛽ Fuel Expenses";
        }
        return expenseType;
    }
    
    public boolean isValid() {
        return tripId != null && !tripId.isEmpty() &&
               expenseType != null && !expenseType.isEmpty() &&
               amount > 0;
    }
}
