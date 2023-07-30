package com.example.covidtracker.common;

public class Constants {
    public static String IP = "192.168.254.179"; //REPLACE with your current IP
    private static final String host = String.format("http://%s:8443", IP);
    public static final String covidURL = host + "/summary";

    public static final String provinceURL = host + "/unique/provinces";
}
