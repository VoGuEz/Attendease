package com.attendease.dto;

public class StudentKpiDto {
    private int attended;
    private int totalSessions;
    private String attendanceRate;
    private String status;

    public StudentKpiDto(int attended, int totalSessions, String attendanceRate, String status) {
        this.attended = attended;
        this.totalSessions = totalSessions;
        this.attendanceRate = attendanceRate;
        this.status = status;
    }

    public int getAttended() {
        return attended;
    }

    public void setAttended(int attended) {
        this.attended = attended;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public String getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(String attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}