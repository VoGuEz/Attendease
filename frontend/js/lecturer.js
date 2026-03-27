let currentUser = null;
let lecturerCourses = [];
let activeSection = 'overview';
let lastAttendanceData = null;

document.addEventListener('DOMContentLoaded', async () => {
  currentUser = requireRole('lecturer');
  if (!currentUser) return;

  document.getElementById('user-name').textContent = currentUser.fullName;
  renderThemeSwitcher('theme-switcher-container');

  document.getElementById('logout-btn').addEventListener('click', logout);

  document.querySelectorAll('[data-section]').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      showSection(link.dataset.section);
    });
  });

  document.getElementById('create-course-btn').addEventListener('click', () => openModal('modal-create-course'));
  document.getElementById('form-create-course').addEventListener('submit', handleCreateCourse);
  document.getElementById('form-create-session').addEventListener('submit', handleCreateSession);

  document.querySelectorAll('.modal-close').forEach(btn => {
    btn.addEventListener('click', () => closeModal(btn.closest('.modal-overlay').id));
  });

  await loadAll();
});

async function loadAll() {
  await Promise.all([loadCourses(), loadActiveSessions(), loadStudents()]);
  updateStats();
}

async function loadCourses() {
  try {
    const courses = await apiRequest('/courses');
    lecturerCourses = Array.isArray(courses) ? courses : [];
    renderCourses(lecturerCourses);
  } catch (err) {
    console.error('Failed to load courses:', err);
  }
}

async function loadActiveSessions() {
  try {
    const sessions = await apiRequest('/sessions/active');
    renderActiveSessions(sessions || []);
    document.getElementById('stat-active-sessions').textContent = (sessions || []).length;
  } catch (err) {
    console.error('Failed to load active sessions:', err);
  }
}

async function loadStudents() {
  try {
    const students = await apiRequest('/students');
    document.getElementById('stat-students').textContent = students.length ?? 0;
    renderStudentsList(students || []);
  } catch (err) {
    console.error('Failed to load students:', err);
  }
}

function updateStats() {
  document.getElementById('stat-courses').textContent = lecturerCourses.length;
  const totalSessions = lecturerCourses.reduce((sum, c) => sum + (c.sessionCount || 0), 0);
  document.getElementById('stat-sessions').textContent = totalSessions;
}

function renderCourses(courses) {
  const container = document.getElementById('courses-list');
  if (!courses.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">📘</div><h3>No Courses Yet</h3><p>Create your first course to get started.</p></div>`;
    return;
  }
  container.innerHTML = courses.map(c => `
    <div class="item-card">
      <div class="item-card-header">
        <div>
          <div class="item-card-title">${escHtml(c.courseName)}</div>
          <div class="item-card-subtitle">${escHtml(c.courseCode)}</div>
        </div>
      </div>
      <div class="item-card-body text-muted" style="font-size:13px; margin-bottom:8px">${escHtml(c.description || 'No description')}</div>
      <div class="item-card-footer">
        <button class="btn btn-sm btn-outline" onclick="viewSessions(${c.id}, '${escHtml(c.courseName)}')">📅 Sessions</button>
        <button class="btn btn-sm" onclick="openCreateSession(${c.id}, '${escHtml(c.courseName)}')">+ Add Session</button>
      </div>
    </div>
  `).join('');
}

function renderActiveSessions(sessions) {
  const container = document.getElementById('active-sessions-list');
  if (!sessions.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">📡</div><h3>No Active Sessions</h3><p>Mark a session as active to see it here.</p></div>`;
    return;
  }
  container.innerHTML = sessions.map(s => `
    <div class="item-card">
      <div class="item-card-header">
        <div>
          <div class="item-card-title">${escHtml(s.courseName || 'Session #' + s.id)}</div>
          <div class="item-card-subtitle">${escHtml(s.sessionDate || '')} · ${escHtml(s.startTime || '')} – ${escHtml(s.endTime || '')}</div>
        </div>
        <span class="badge badge-active">Live</span>
      </div>
      <div class="item-card-footer">
        <button class="btn btn-sm btn-outline" onclick="viewAttendance(${s.id})">View Attendance</button>
        <button class="btn btn-sm btn-warning" onclick="changeStatus(${s.id}, 'completed', this)">End Session</button>
      </div>
    </div>
  `).join('');
}

function renderStudentsList(students) {
  const container = document.getElementById('students-list');
  if (!students.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">👥</div><h3>No Students Yet</h3></div>`;
    return;
  }
  container.innerHTML = `
    <div class="table-wrapper">
      <table>
        <thead><tr><th>#</th><th>Name</th><th>Email</th></tr></thead>
        <tbody>
          ${students.map((s, i) => `
            <tr>
              <td>${i + 1}</td>
              <td>${escHtml(s.fullName)}</td>
              <td>${escHtml(s.email)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `;
}

async function viewSessions(courseId, courseName) {
  showSection('sessions');
  const container = document.getElementById('sessions-detail');
  container.innerHTML = `<div class="loading-overlay"><span class="spinner"></span></div>`;

  try {
    const sessions = await apiRequest(`/courses/${courseId}/sessions`);
    if (!sessions.length) {
      container.innerHTML = `
        <div class="section-header">
          <h2>${escHtml(courseName)} – Sessions</h2>
          <button class="btn btn-sm" onclick="openCreateSession(${courseId}, '${escHtml(courseName)}')">+ Add Session</button>
        </div>
        <div class="empty-state"><div class="empty-icon">📅</div><h3>No Sessions Yet</h3></div>`;
      return;
    }
    container.innerHTML = `
      <div class="section-header">
        <h2>${escHtml(courseName)} – Sessions</h2>
        <button class="btn btn-sm" onclick="openCreateSession(${courseId}, '${escHtml(courseName)}')">+ Add Session</button>
      </div>
      <div class="items-grid">
        ${sessions.map(s => `
          <div class="item-card">
            <div class="item-card-header">
              <div>
                <div class="item-card-title">${escHtml(s.sessionDate || '')}</div>
                <div class="item-card-subtitle">${escHtml(s.startTime || '')} – ${escHtml(s.endTime || '')}</div>
              </div>
              <span class="badge badge-${s.status}">${s.status}</span>
            </div>
            <div class="item-card-footer">
              ${s.status === 'upcoming' ? `<button class="btn btn-sm btn-success" onclick="changeStatus(${s.id}, 'active', this)">Go Live</button>` : ''}
              ${s.status === 'active' ? `<button class="btn btn-sm btn-warning" onclick="changeStatus(${s.id}, 'completed', this)">End</button>` : ''}
              <button class="btn btn-sm btn-outline" onclick="viewAttendance(${s.id})">Attendance</button>
            </div>
          </div>
        `).join('')}
      </div>
    `;
  } catch (err) {
    container.innerHTML = `<p class="text-danger">Failed to load: ${err.message}</p>`;
  }
}

async function viewAttendance(sessionId) {
  showSection('sessions');
  const container = document.getElementById('sessions-detail');
  container.innerHTML = `<div class="loading-overlay"><span class="spinner"></span></div>`;

  try {
    const records = await apiRequest(`/attendance/session/${sessionId}`);
    if (!records.length) {
      container.innerHTML = `<h3 style="margin-bottom:16px">Session Attendance</h3><div class="empty-state"><div class="empty-icon">📋</div><h3>No attendance yet</h3><p>No students have joined this session.</p></div>`;
      return;
    }
    const rows = records.map(r => `
      <tr>
        <td>${escHtml(r.studentName || 'Unknown')}</td>
        <td>${escHtml(r.joinTime || '')}</td>
        <td><span class="badge badge-${r.status === 'present' ? 'active' : r.status === 'late' ? 'upcoming' : 'completed'}">${r.status}</span></td>
      </tr>
    `).join('');
    container.innerHTML = `
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
        <h3>Session Attendance (${records.length} student${records.length !== 1 ? 's' : ''})</h3>
        <button class="btn btn-sm btn-outline" onclick="downloadAttendanceCSV(${sessionId})">⬇ Download CSV</button>
      </div>
      <div class="table-wrapper">
        <table>
          <thead><tr><th>Student</th><th>Join Time</th><th>Status</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    `;
    // Store records for CSV export
    lastAttendanceData = { sessionId, records };
  } catch (err) {
    container.innerHTML = `<p class="text-danger">Failed: ${err.message}</p>`;
  }
}

function downloadAttendanceCSV(sessionId) {
  const data = lastAttendanceData;
  if (!data || data.sessionId !== sessionId || !data.records) {
    showToast('No attendance data to download.', 'error');
    return;
  }
  const escape = val => {
    const str = String(val ?? '');
    return str.includes(',') || str.includes('"') || str.includes('\n')
      ? `"${str.replace(/"/g, '""')}"` : str;
  };
  const lines = ['Student,Join Time,Status'];
  data.records.forEach(r => {
    lines.push([
      escape(r.studentName || 'Unknown'),
      escape(r.joinTime || ''),
      escape(r.status || '')
    ].join(','));
  });
  const csv = lines.join('\r\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `attendance-session-${sessionId}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

async function handleCreateCourse(e) {
  e.preventDefault();
  const btn = e.target.querySelector('button[type="submit"]');
  const courseName = document.getElementById('course-name').value.trim();
  const courseCode = document.getElementById('course-code').value.trim();
  const description = document.getElementById('course-desc').value.trim();

  if (!courseName || !courseCode) {
    showToast('Course name and code are required.', 'error');
    return;
  }

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';
  try {
    await apiRequest('/courses', 'POST', { courseName, courseCode, description });
    closeModal('modal-create-course');
    e.target.reset();
    showToast('Course created successfully!', 'success');
    await loadCourses();
    updateStats();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = 'Create Course';
  }
}

function openCreateSession(courseId, courseName) {
  document.getElementById('session-course-id').value = courseId;
  document.getElementById('session-course-name-label').textContent = courseName;
  openModal('modal-create-session');
}

async function handleCreateSession(e) {
  e.preventDefault();
  const btn = e.target.querySelector('button[type="submit"]');
  const courseId = parseInt(document.getElementById('session-course-id').value);
  const sessionDate = document.getElementById('session-date').value;
  const startTime = document.getElementById('session-start').value;
  const endTime = document.getElementById('session-end').value;

  if (!sessionDate || !startTime || !endTime) {
    showToast('All session fields are required.', 'error');
    return;
  }

  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';
  try {
    await apiRequest('/sessions', 'POST', { courseId, sessionDate, startTime, endTime });
    closeModal('modal-create-session');
    e.target.reset();
    showToast('Session created successfully!', 'success');
    await loadAll();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = 'Create Session';
  }
}

async function changeStatus(sessionId, status, btn) {
  btn.disabled = true;
  try {
    await apiRequest(`/sessions/${sessionId}/status`, 'PUT', { status });
    showToast(`Session status updated to ${status}`, 'success');
    await loadAll();
  } catch (err) {
    showToast(err.message, 'error');
    btn.disabled = false;
  }
}

function showSection(name) {
  activeSection = name;
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('[data-section]').forEach(l => {
    l.closest('li')?.classList.toggle('active', l.dataset.section === name);
  });
  const section = document.getElementById(`section-${name}`);
  if (section) section.classList.add('active');
}

function openModal(id) {
  document.getElementById(id)?.classList.add('open');
}

function closeModal(id) {
  document.getElementById(id)?.classList.remove('open');
}

function showToast(msg, type = 'success') {
  const existing = document.querySelector('.toast');
  if (existing) existing.remove();
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.style.cssText = `
    position: fixed; bottom: 24px; right: 24px; z-index: 1000;
    background: ${type === 'success' ? 'var(--success)' : type === 'error' ? 'var(--danger)' : 'var(--warning)'};
    color: white; padding: 12px 20px; border-radius: 8px;
    font-size: 14px; font-weight: 500; box-shadow: 0 4px 16px rgba(0,0,0,0.3);
  `;
  toast.textContent = msg;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}

function escHtml(str) {
  const div = document.createElement('div');
  div.appendChild(document.createTextNode(String(str)));
  return div.innerHTML;
}
