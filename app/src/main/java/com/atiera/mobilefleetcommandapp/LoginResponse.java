package com.atiera.mobilefleetcommandapp;

public class LoginResponse {
    public boolean ok;
    public String token;
    public String msg;
    public Driver driver;

    public static class Driver {
        public int id;
        public String username;
        public String email;
    }
}


