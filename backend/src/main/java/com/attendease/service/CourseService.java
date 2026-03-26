package com.attendease.service;

import com.attendease.dto.CourseDTO;
import com.attendease.entity.Course;
import com.attendease.entity.User;
import com.attendease.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserService userService;

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<CourseDTO> getCoursesByLecturer(Long lecturerId) {
        return courseRepository.findByLecturerId(lecturerId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public CourseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        return toDTO(course);
    }

    public CourseDTO createCourse(CourseDTO dto, String lecturerEmail) {
        User lecturer = userService.findByEmail(lecturerEmail)
            .orElseThrow(() -> new RuntimeException("Lecturer not found"));

        Course course = Course.builder()
            .lecturer(lecturer)
            .courseName(dto.getCourseName())
            .courseCode(dto.getCourseCode())
            .description(dto.getDescription())
            .build();

        return toDTO(courseRepository.save(course));
    }

    public CourseDTO updateCourse(Long id, CourseDTO dto) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setCourseName(dto.getCourseName());
        course.setCourseCode(dto.getCourseCode());
        course.setDescription(dto.getDescription());
        return toDTO(courseRepository.save(course));
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    private CourseDTO toDTO(Course course) {
        return CourseDTO.builder()
            .id(course.getId())
            .lecturerId(course.getLecturer() != null ? course.getLecturer().getId() : null)
            .lecturerName(course.getLecturer() != null ? course.getLecturer().getFullName() : null)
            .courseName(course.getCourseName())
            .courseCode(course.getCourseCode())
            .description(course.getDescription())
            .createdAt(course.getCreatedAt())
            .build();
    }
}
