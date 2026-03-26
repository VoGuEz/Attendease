package com.attendease.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_courses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@IdClass(StudentCourse.StudentCourseId.class)
public class StudentCourse {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Id
    @Column(name = "course_id")
    private Long courseId;

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private LocalDateTime enrolledAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentCourseId implements Serializable {
        private Long studentId;
        private Long courseId;
    }
}
