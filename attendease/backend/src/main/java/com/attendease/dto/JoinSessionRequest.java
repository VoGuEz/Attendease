package com.attendease.dto;

public class JoinSessionRequest {
    private String sessionCode;
    private String fullname;
    private String indexNumber;
    private int level;
    private double latitude;
    private double longitude;

    public JoinSessionRequest() {}

    public JoinSessionRequest(String sessionCode, String fullname, String indexNumber, 
                            int level, double latitude, double longitude) {
        this.sessionCode = sessionCode;
        this.fullname = fullname;
        this.indexNumber = indexNumber;
        this.level = level;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getIndexNumber() {
        return indexNumber;
    }

    public void setIndexNumber(String indexNumber) {
        this.indexNumber = indexNumber;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}