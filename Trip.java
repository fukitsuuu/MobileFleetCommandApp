package com.atiera.mobileapp;

public class Trip {
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
    
    // Setters
    public void setTripId(String tripId) { this.tripId = tripId; }
    public void setStatus(String status) { this.status = status; }
    public void setRequester(String requester) { this.requester = requester; }
    public void setLocation(String location) { this.location = location; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }
    public void setDistance(String distance) { this.distance = distance; }
    public void setTime(String time) { this.time = time; }
    public void setBudget(String budget) { this.budget = budget; }
}
