package com.atiera.mobilefleetcommandapp;

import java.util.List;

public class MaintenanceResponse {
    public boolean ok;
    public String msg;
    public List<Maintenance> maintenance;
    public int totalMaintenance;
    
    public static class Maintenance {
        public String maintenanceID;
        public String vehicleNumber;
        public String assignedDriver;
        public String serviceType;
        public String maintenanceStatus;
        public String serviceNotes;
        public String serviceCenter;
        public String serviceCenterAddress;
        public String scheduledDate;
        public String completionDate;
        public String costService;
        public String receiptUploads;
        public String dateAdded;
        
        // Vehicle details
        public String vehicleName;
        public String licensePlate;
        public String color;
        public String chassisNumber;
        public String engineNumber;
        
        // Getters
        public String getMaintenanceID() { return maintenanceID; }
        public String getVehicleNumber() { return vehicleNumber; }
        public String getAssignedDriver() { return assignedDriver; }
        public String getServiceType() { return serviceType; }
        public String getMaintenanceStatus() { return maintenanceStatus; }
        public String getServiceNotes() { return serviceNotes; }
        public String getServiceCenter() { return serviceCenter; }
        public String getServiceCenterAddress() { return serviceCenterAddress; }
        public String getScheduledDate() { return scheduledDate; }
        public String getCompletionDate() { return completionDate; }
        public String getCostService() { return costService; }
        public String getReceiptUploads() { return receiptUploads; }
        public String getDateAdded() { return dateAdded; }
    }
}
