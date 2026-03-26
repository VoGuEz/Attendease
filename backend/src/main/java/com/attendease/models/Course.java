package com.attendease.models;

import java.sql.Timestamp;

public class Course {
    private int id;
    private int lecturerId;
    private String courseName;
    private String courseCode;
    private String description;
    private Timestamp createdAt;
    private String lecturerName;

    public Course() {}

    public Course(int lecturerId, String courseName, String courseCode, String description) {
        this.lecturerId = lecturerId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getLecturerId() { return lecturerId; }
    public void setLecturerId(int lecturerId) { this.lecturerId = lecturerId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getLecturerName() { return lecturerName; }
    public void setLecturerName(String lecturerName) { this.lecturerName = lecturerName; }
}
