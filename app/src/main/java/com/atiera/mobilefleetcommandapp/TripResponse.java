package com.atiera.mobilefleetcommandapp;

import java.util.List;

public class TripResponse {
    public boolean ok;
    public String msg;
    public List<Trip> trips;
    public int totalTrips;

    public static class Trip {
        public String tripID;
        public String reservationID;
        public String vehicleNumber;
        public String requester;
        public String description;
        public String urgency;
        public String location;
        public String address;
        public String estimatedDistance;
        public String estimatedTime;
        public double budget;
        public Double expenses;
        public String departedTime;
        public String deliveredTime;
        public String completedTime;
        public String status;
        public String dateAdded;
    }
}
