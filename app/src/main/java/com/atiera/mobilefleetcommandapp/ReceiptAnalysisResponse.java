package com.atiera.mobilefleetcommandapp;

public class ReceiptAnalysisResponse {
    public boolean ok;
    public String msg;
    public Double amount;      // May be null if no amount detected
    public Double fuelLiters;  // May be null if no liters detected
    public String description; // Fuel product name (e.g., XCS, Xtra Advance)
    public String serialNumber; // Receipt / invoice serial number
    public String invoiceDate;  // Invoice date/time as string
    public String rawText;     // Full OCR text (optional, for debugging)
}


