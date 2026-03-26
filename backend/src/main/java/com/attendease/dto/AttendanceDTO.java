package com.attendease.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceDTO {
    private Long id;
    private Long sessionId;
    private Long studentId;
    private String studentName;
    private String email;
    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;
    private String status;
    private Double locationLatitude;
    private Double locationLongitude;
}
