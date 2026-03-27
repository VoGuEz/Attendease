package com.attendease.models;

import java.sql.Timestamp;

public class Attendance {
    private int id;
    private int sessionId;
    private int studentId;
    private Timestamp joinTime;
    private Timestamp leaveTime;
    private String status;
    private String studentName;
    private String submittedFullName;
    private String submittedIndexNumber;
    private String submittedLevel;
    private Double latitude;
    private Double longitude;

    public Attendance() {}

    public Attendance(int sessionId, int studentId, String status) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public Timestamp getJoinTime() { return joinTime; }
    public void setJoinTime(Timestamp joinTime) { this.joinTime = joinTime; }

    public Timestamp getLeaveTime() { return leaveTime; }
    public void setLeaveTime(Timestamp leaveTime) { this.leaveTime = leaveTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getSubmittedFullName() { return submittedFullName; }
    public void setSubmittedFullName(String submittedFullName) { this.submittedFullName = submittedFullName; }

    public String getSubmittedIndexNumber() { return submittedIndexNumber; }
    public void setSubmittedIndexNumber(String submittedIndexNumber) { this.submittedIndexNumber = submittedIndexNumber; }

    public String getSubmittedLevel() { return submittedLevel; }
    public void setSubmittedLevel(String submittedLevel) { this.submittedLevel = submittedLevel; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
