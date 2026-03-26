-- =====================================================
-- AttendEase Database Schema
-- MySQL 8.0+
-- =====================================================

CREATE DATABASE IF NOT EXISTS attendease_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE attendease_db;

-- ─── Users ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  email         VARCHAR(255)  NOT NULL,
  password_hash VARCHAR(255)  NOT NULL,
  full_name     VARCHAR(255)  NOT NULL,
  role          ENUM('STUDENT','LECTURER') NOT NULL DEFAULT 'STUDENT',
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Courses ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS courses (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  lecturer_id BIGINT       NOT NULL,
  course_name VARCHAR(255) NOT NULL,
  course_code VARCHAR(50)  NOT NULL,
  description TEXT,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_courses_lecturer (lecturer_id),
  CONSTRAINT fk_courses_lecturer FOREIGN KEY (lecturer_id)
    REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Sessions ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sessions (
  id           BIGINT   NOT NULL AUTO_INCREMENT,
  course_id    BIGINT   NOT NULL,
  session_date DATE     NOT NULL,
  start_time   TIME     NOT NULL,
  end_time     TIME     NOT NULL,
  status       ENUM('UPCOMING','ACTIVE','COMPLETED') NOT NULL DEFAULT 'UPCOMING',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  KEY idx_sessions_course  (course_id),
  KEY idx_sessions_status  (status),
  KEY idx_sessions_date    (session_date),
  CONSTRAINT fk_sessions_course FOREIGN KEY (course_id)
    REFERENCES courses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Attendance ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS attendance (
  id                 BIGINT NOT NULL AUTO_INCREMENT,
  session_id         BIGINT NOT NULL,
  student_id         BIGINT NOT NULL,
  join_time          DATETIME,
  leave_time         DATETIME,
  status             ENUM('PRESENT','ABSENT','LATE') NOT NULL DEFAULT 'PRESENT',
  location_latitude  DECIMAL(10,8),
  location_longitude DECIMAL(11,8),

  PRIMARY KEY (id),
  UNIQUE KEY uq_attendance_session_student (session_id, student_id),
  KEY idx_attendance_session (session_id),
  KEY idx_attendance_student (student_id),
  CONSTRAINT fk_attendance_session FOREIGN KEY (session_id)
    REFERENCES sessions (id) ON DELETE CASCADE,
  CONSTRAINT fk_attendance_student FOREIGN KEY (student_id)
    REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─── Student-Course enrolments ────────────────────────
CREATE TABLE IF NOT EXISTS student_courses (
  student_id  BIGINT   NOT NULL,
  course_id   BIGINT   NOT NULL,
  enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (student_id, course_id),
  KEY idx_sc_course (course_id),
  CONSTRAINT fk_sc_student FOREIGN KEY (student_id)
    REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_sc_course FOREIGN KEY (course_id)
    REFERENCES courses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Sample Data
-- Passwords are BCrypt-hashed: "password123"
-- =====================================================

INSERT IGNORE INTO users (email, password_hash, full_name, role) VALUES
  ('lecturer@example.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   'Dr. Sarah Johnson', 'LECTURER'),
  ('student1@example.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   'Alice Smith', 'STUDENT'),
  ('student2@example.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   'Bob Johnson', 'STUDENT'),
  ('student3@example.com',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   'Carol White', 'STUDENT');

INSERT IGNORE INTO courses (lecturer_id, course_name, course_code, description) VALUES
  (1, 'Computer Science 101', 'CS101',   'Introduction to Computer Science'),
  (1, 'Data Structures',      'CS201',   'Data Structures and Algorithms'),
  (1, 'Mathematics',          'MATH201', 'Calculus and Linear Algebra');

INSERT IGNORE INTO sessions (course_id, session_date, start_time, end_time, status) VALUES
  (1, CURDATE(),              '09:00:00', '10:30:00', 'ACTIVE'),
  (1, CURDATE(),              '11:00:00', '12:30:00', 'UPCOMING'),
  (2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:00:00', '15:30:00', 'COMPLETED'),
  (3, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:00:00', '11:30:00', 'COMPLETED');

INSERT IGNORE INTO student_courses (student_id, course_id) VALUES
  (2, 1), (2, 2),
  (3, 1), (3, 3),
  (4, 1), (4, 2), (4, 3);

INSERT IGNORE INTO attendance (session_id, student_id, join_time, status) VALUES
  (3, 2, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL '14:05:00' HOUR_SECOND, 'PRESENT'),
  (3, 3, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL '14:20:00' HOUR_SECOND, 'LATE'),
  (3, 4, NULL, 'ABSENT'),
  (4, 2, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL '10:03:00' HOUR_SECOND, 'PRESENT'),
  (4, 3, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL '10:08:00' HOUR_SECOND, 'PRESENT'),
  (4, 4, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL '10:01:00' HOUR_SECOND, 'PRESENT');
