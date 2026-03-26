package com.attendease.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseDTO {
    private Long id;
    private Long lecturerId;
    private String lecturerName;
    private String courseName;
    private String courseCode;
    private String description;
    private LocalDateTime createdAt;
}
