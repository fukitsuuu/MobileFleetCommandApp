package com.atiera.mobilefleetcommandapp;

import java.util.List;

public class SavedLocationsResponse {
    public boolean ok;
    public String msg;
    public List<Location> locations;
    public int totalLocations;

    public static class Location {
        public String locationName;
        public String locationAddress;
        public int visitCount;
        public String lastVisit;
        public String distance;
    }
}
