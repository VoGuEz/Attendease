# 📚 AttendEase

A comprehensive attendance management system built with HTML/CSS/JavaScript frontend and Java backend with MySQL database.

## Features

- **Authentication** — Sign up and sign in with Student or Lecturer roles
- **7 Color Themes** — Dark, Light, Blue, Purple, Ocean, Forest, Sunset
- **Student Dashboard** — Enroll in courses, join active sessions, track attendance progress bar
- **Lecturer Dashboard** — Create courses and sessions, view student attendance, manage session status
- **Progress Tracking** — Real-time attendance percentage with animated progress bar

## Tech Stack

| Layer    | Technology                         |
|----------|------------------------------------|
| Frontend | HTML5, CSS3 (custom properties), Vanilla JS |
| Backend  | Java 17, com.sun.net.httpserver    |
| Database | MySQL 8 via XAMPP                  |
| Auth     | JWT (jjwt 0.12.x) + BCrypt        |
| Build    | Apache Maven with shade plugin     |

## Project Structure

```
AttendEase/
├── database/
│   └── schema.sql          # MySQL schema — run this first
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/attendease/
│       ├── Main.java                   # HTTP server (port 8080)
│       ├── config/DatabaseConfig.java  # JDBC connection
│       ├── models/                     # User, Course, Session, Attendance
│       ├── repositories/               # JDBC data access
│       ├── services/                   # Business logic
│       ├── handlers/                   # HTTP request handlers
│       └── utils/                      # JWT, BCrypt, JSON helpers
└── frontend/
    ├── index.html       # Sign In
    ├── signup.html      # Sign Up
    ├── dashboard.html   # Welcome / role redirect
    ├── student.html     # Student dashboard
    ├── lecturer.html    # Lecturer dashboard
    ├── css/styles.css   # All styles + 7 themes
    └── js/
        ├── api.js       # fetch() wrapper + auth helpers
        ├── themes.js    # Theme switcher
        ├── auth.js      # Login/register handlers
        ├── student.js   # Student dashboard logic
        └── lecturer.js  # Lecturer dashboard logic
```

## Setup

### Prerequisites

- [XAMPP](https://www.apachefriends.org/) (MySQL 8.x)
- Java 17+
- Maven 3.8+

### 1. Database

1. Start XAMPP and open phpMyAdmin (or any MySQL client)
2. Run the schema:

```sql
-- In phpMyAdmin SQL tab or MySQL CLI:
source /path/to/AttendEase/database/schema.sql
```

This creates the `attendease` database with all required tables.

**Default connection settings** (edit `DatabaseConfig.java` if different):
```
Host:     localhost
Port:     3306
Database: attendease
User:     root
Password: (empty)
```

### 2. Backend

```bash
cd backend
mvn package
java -jar target/attendease-backend-1.0.0.jar
```

The server starts on **http://localhost:8080**.

### 3. Frontend

Open `frontend/index.html` directly in your browser, or serve with any static server:

```bash
# Python
cd frontend && python -m http.server 3000

# Node.js npx
cd frontend && npx serve .
```

Then navigate to `http://localhost:3000`.

## API Endpoints

| Method | Endpoint                        | Auth | Description                        |
|--------|---------------------------------|------|------------------------------------|
| POST   | /api/auth/register              | –    | Register new user                  |
| POST   | /api/auth/login                 | –    | Login and get JWT                  |
| GET    | /api/students                   | JWT  | List all students (lecturer)       |
| GET    | /api/courses                    | JWT  | Courses for current user           |
| POST   | /api/courses                    | JWT  | Create course (lecturer)           |
| POST   | /api/courses/{id}/enroll        | JWT  | Enroll in course (student)         |
| GET    | /api/courses/{id}/sessions      | JWT  | Sessions for a course              |
| POST   | /api/sessions                   | JWT  | Create session (lecturer)          |
| GET    | /api/sessions/active            | JWT  | List all active sessions           |
| PUT    | /api/sessions/{id}/status       | JWT  | Update session status (lecturer)   |
| POST   | /api/attendance/join            | JWT  | Join session (student)             |
| GET    | /api/attendance/stats           | JWT  | Student attendance stats           |
| GET    | /api/attendance/session/{id}    | JWT  | Attendance list for session        |

## Themes

Switch themes using the 🎨 button in the top-right corner or the Settings panel.

| Theme  | Accent Color | Background    |
|--------|-------------|---------------|
| Dark   | Indigo       | Deep slate    |
| Light  | Indigo       | Off-white     |
| Blue   | Blue         | Deep navy     |
| Purple | Purple       | Deep violet   |
| Ocean  | Cyan         | Deep teal     |
| Forest | Green        | Deep forest   |
| Sunset | Orange       | Deep burgundy |

## Wallpapers

Choose a floating background wallpaper in the **Settings → Wallpaper** panel. Elements gently animate across the background at low opacity.

| Wallpaper    | Description                  |
|--------------|------------------------------|
| None         | No wallpaper (default)       |
| Pencils ✏️   | Floating pencils             |
| Books 📚     | Floating books               |
| Stars ⭐     | Floating stars               |
| Leaves 🍃    | Floating leaves              |
| Music Notes 🎵 | Floating music notes       |
| Coffee ☕    | Floating coffee cups         |
| Clouds ☁️    | Floating clouds              |
| Bubbles 🫧   | Floating bubbles             |

## Usage

### As a Student

1. Sign up with the **Student** role
2. Sign in and go to **Student Dashboard**
3. Browse and **Enroll** in available courses
4. When a lecturer starts a session, it appears in **Active Sessions**
5. Click **Join Session** — your progress bar updates immediately
6. Track your attendance percentage in the **Overview** tab

### As a Lecturer

1. Sign up with the **Lecturer** role
2. Sign in and go to **Lecturer Dashboard**
3. Create a **Course** (name, code, description)
4. Create **Sessions** for each course (date, start/end time)
5. Set session status to **Active** so students can join
6. View **Attendance** for each session to see who joined

## Security

- Passwords are hashed with BCrypt (never stored in plaintext)
- All protected endpoints require a valid JWT (`Authorization: Bearer <token>`)
- CORS headers allow the frontend to connect to the local API
