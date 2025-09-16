package com.atiera.mobilefleetcommandapp;

public class DriverProfileResponse {
    public boolean ok;
    public String msg;
    public Driver driver;

    public static class Driver {
        public String fullName;
        public String idNumber;
        public String imageUrl;
        public String firstName;
        public String middleName;
        public String lastName;
        public String birthDate;
        public String gender;
        public String contactNumber;
        public String address;
        public String licenseNumber;
        public String licenseIssued;
        public String licenseExpiry;
        public String emergencyContactName;
        public String emergencyContactNumber;
        public String bloodType;
    }
}


