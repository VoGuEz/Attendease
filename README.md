# 📚 AttendEase – Attendance Management System

A full-stack web application for managing student attendance with real-time session tracking, geolocation support, and a beautiful multi-theme UI.

---

## ✨ Features

- **Role-based access** — Student and Lecturer dashboards
- **JWT authentication** — Secure token-based auth
- **Real-time session management** — Lecturers host sessions, students join
- **Geolocation tracking** — Optional location capture on attendance
- **7 UI themes** — Dark, Light, Blue, Purple, Ocean, Forest, Sunset
- **Attendance statistics** — Progress tracking and rate calculation
- **Responsive design** — Works on mobile and desktop
- **Mock data fallback** — Frontend works standalone without backend

---

## 🏗️ Project Structure

```
AttendEase/
├── frontend/          # HTML/CSS/JS frontend
│   ├── index.html     # Sign Up (default landing)
│   ├── signin.html    # Sign In
│   ├── role-select.html
│   ├── student.html   # Student dashboard
│   ├── lecturer.html  # Lecturer dashboard
│   ├── css/styles.css
│   └── js/
│       ├── api.js
│       ├── auth.js
│       ├── student.js
│       └── lecturer.js
├── backend/           # Spring Boot backend
│   ├── pom.xml
│   └── src/main/java/com/attendease/
│       ├── controller/
│       ├── service/
│       ├── entity/
│       ├── repository/
│       ├── dto/
│       ├── security/
│       └── config/
└── database/
    └── schema.sql
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- A modern web browser

### 1. Database Setup

```bash
mysql -u root -p < database/schema.sql
```

Or manually:
```sql
CREATE DATABASE attendease_db CHARACTER SET utf8mb4;
```

### 2. Backend Setup

```bash
cd backend

# Edit database credentials if needed
nano src/main/resources/application.properties

# Build and run
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### 3. Frontend Setup

Open any HTML file directly in your browser, or serve with a static server:

```bash
cd frontend
npx serve .
# or
python3 -m http.server 3000
```

Then navigate to `http://localhost:3000`

> **Note:** The frontend includes mock data fallback — it works without the backend for UI exploration.

---

## 🔑 Default Credentials (after running schema.sql)

| Role     | Email                    | Password    |
|----------|--------------------------|-------------|
| Lecturer | lecturer@example.com     | password123 |
| Student  | student1@example.com     | password123 |
| Student  | student2@example.com     | password123 |
| Student  | student3@example.com     | password123 |

---

## 🌐 API Endpoints

### Authentication
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Create account |
| POST | `/api/auth/login` | Sign in |
| POST | `/api/auth/logout` | Sign out |
| GET  | `/api/auth/verify` | Verify token |

### Courses
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/api/courses` | List all courses |
| POST   | `/api/courses` | Create course (Lecturer) |
| GET    | `/api/courses/{id}` | Get course |
| PUT    | `/api/courses/{id}` | Update course |
| DELETE | `/api/courses/{id}` | Delete course |

### Sessions
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/api/sessions` | List all sessions |
| GET    | `/api/sessions/active` | Active & upcoming sessions |
| POST   | `/api/sessions` | Create session (Lecturer) |
| PUT    | `/api/sessions/{id}` | Update session |
| PUT    | `/api/sessions/{id}/status` | Update status |
| DELETE | `/api/sessions/{id}` | Delete session |

### Attendance
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/attendance/join` | Join session |
| POST | `/api/attendance/leave` | Leave session |
| GET  | `/api/attendance/session/{id}` | Session attendance list |
| GET  | `/api/attendance/student/{id}` | Student attendance history |
| GET  | `/api/attendance/statistics/{id}` | Student statistics |

### Users
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users/profile` | Get profile |
| PUT | `/api/users/profile` | Update profile |

---

## 🎨 Themes

Switch themes using the theme button in the header (🌙/☀). Available themes:

| Theme | Key |
|-------|-----|
| 🌑 Dark (default) | `dark` |
| ☀ Light | `light` |
| 🔵 Blue | `blue` |
| 🟣 Purple | `purple` |
| 🌊 Ocean | `ocean` |
| 🌿 Forest | `forest` |
| 🌅 Sunset | `sunset` |

Themes are persisted in `localStorage`.

---

## 🛡️ Security

- Passwords hashed with BCrypt
- JWT tokens (24h expiry by default)
- CORS configured for local development
- Spring Security for endpoint protection

---

## 🗄️ Database Schema

```
users          → id, email, password_hash, full_name, role, created_at
courses        → id, lecturer_id, course_name, course_code, description
sessions       → id, course_id, session_date, start_time, end_time, status
attendance     → id, session_id, student_id, join_time, leave_time, status, location
student_courses→ student_id, course_id, enrolled_at (composite PK)
```

---

## 🔧 Configuration

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/attendease_db
spring.datasource.username=root
spring.datasource.password=yourpassword
jwt.secret=your-secret-key
jwt.expiration=86400000  # 24 hours in ms
```

---

## 📱 Browser Support

Chrome 90+, Firefox 88+, Safari 14+, Edge 90+
