let currentUser = null;
let lecturerCourses = [];
let activeSection = 'overview';
let lastAttendanceData = null;
let sidebarOpen = false;
let countdownInterval = null;

document.addEventListener('DOMContentLoaded', async () => {
  currentUser = requireRole('lecturer');
  if (!currentUser) return;

  document.getElementById('user-name').textContent = currentUser.fullName;
  renderThemeSwitcher('theme-switcher-container');

  // Mobile menu functionality
  const hamburgerBtn = document.getElementById('hamburger-btn');
  const sidebar = document.getElementById('sidebar');
  const mobileOverlay = document.getElementById('mobile-overlay');
  
  if (hamburgerBtn) {
    hamburgerBtn.addEventListener('click', () => {
      sidebarOpen = !sidebarOpen;
      hamburgerBtn.classList.toggle('active');
      sidebar.classList.toggle('open');
      mobileOverlay.classList.toggle('open');
    });
  }
  
  if (mobileOverlay) {
    mobileOverlay.addEventListener('click', closeMobileMenu);
  }

  document.getElementById('logout-btn').addEventListener('click', logout);

  document.querySelectorAll('[data-section]').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      showSection(link.dataset.section);
      closeMobileMenu();
    });
  });

  document.getElementById('create-course-btn').addEventListener('click', () => openModal('modal-create-course'));
  document.getElementById('form-create-course').addEventListener('submit', handleCreateCourse);
  document.getElementById('form-create-session').addEventListener('submit', handleCreateSession);
  document.getElementById('form-edit-course').addEventListener('submit', handleEditCourse);
  document.getElementById('form-edit-session').addEventListener('submit', handleEditSession);

  // Search/filter listeners
  document.getElementById('search-courses')?.addEventListener('input', (e) => {
    filterCards('courses-list', e.target.value);
  });
  document.getElementById('search-sessions')?.addEventListener('input', (e) => {
    filterCards('sessions-detail', e.target.value);
  });
  document.getElementById('search-students')?.addEventListener('input', (e) => {
    filterTableRows('students-list', e.target.value);
  });

  document.querySelectorAll('.modal-close').forEach(btn => {
    btn.addEventListener('click', () => closeModal(btn.closest('.modal-overlay').id));
  });

  await loadAll();
  syncSectionLayout();
  window.addEventListener('resize', syncSectionLayout);
});

function closeMobileMenu() {
  if (sidebarOpen) {
    sidebarOpen = false;
    const hamburgerBtn = document.getElementById('hamburger-btn');
    const sidebar = document.getElementById('sidebar');
    const mobileOverlay = document.getElementById('mobile-overlay');
    
    hamburgerBtn.classList.remove('active');
    sidebar.classList.remove('open');
    mobileOverlay.classList.remove('open');
  }
}

async function loadAll() {
  await Promise.all([loadCourses(), loadActiveSessions(), loadStudents()]);
  await loadLecturerSessionsPanels();
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
        <button class="btn btn-sm btn-outline" onclick="openEditCourse(${c.id})">✏️ Edit</button>
        <button class="btn btn-sm btn-danger" onclick="confirmDeleteCourse(${c.id}, '${escHtml(c.courseName)}')">🗑️ Delete</button>
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
      <div style="margin-bottom:12px;">
        ${buildCountdownRing(s.id, s.sessionDate, s.startTime, s.endTime)}
      </div>
      <div class="item-card-footer">
        <button class="btn btn-sm btn-outline" onclick="viewAttendance(${s.id})">View Attendance</button>
        <button class="btn btn-sm btn-warning" onclick="changeStatus(${s.id}, 'completed', this)">End Session</button>
      </div>
    </div>
  `).join('');
  startCountdownTimers();
}

async function loadLecturerSessionsPanels() {
  const sessionsContainer = document.getElementById('sessions-detail');
  const overviewContainer = document.getElementById('overview-sessions-list');

  if (!lecturerCourses.length) {
    if (sessionsContainer) {
      sessionsContainer.innerHTML = `<div class="empty-state"><div class="empty-icon">📅</div><h3>No sessions yet</h3><p>Create a course and add sessions to get started.</p></div>`;
    }
    if (overviewContainer) {
      overviewContainer.innerHTML = `<div class="empty-state"><div class="empty-icon">📅</div><h3>No sessions yet</h3></div>`;
    }
    return;
  }

  const courseSessions = await Promise.all(
    lecturerCourses.map(async (course) => {
      try {
        const sessions = await apiRequest(`/courses/${course.id}/sessions`);
        return (sessions || []).map(s => ({
          ...s,
          courseId: course.id,
          courseName: course.courseName,
          courseCode: course.courseCode
        }));
      } catch (err) {
        console.error(`Failed to load sessions for course ${course.id}:`, err);
        return [];
      }
    })
  );

  const allSessions = courseSessions
    .flat()
    .sort((a, b) => {
      const aDate = `${a.sessionDate || ''} ${a.startTime || ''}`;
      const bDate = `${b.sessionDate || ''} ${b.startTime || ''}`;
      return new Date(bDate) - new Date(aDate);
    });

  renderSessionsSectionList(allSessions);
  renderOverviewSessionsList(allSessions);
}

function renderOverviewSessionsList(sessions) {
  const container = document.getElementById('overview-sessions-list');
  if (!container) return;

  const visible = sessions.filter(s => s.status === 'upcoming' || s.status === 'active').slice(0, 6);
  if (!visible.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">📡</div><h3>No upcoming or active sessions</h3><p>Create a new session from My Courses.</p></div>`;
    return;
  }

  container.innerHTML = visible.map(s => `
    <div class="item-card">
      <div class="item-card-header">
        <div>
          <div class="item-card-title">${escHtml(s.courseName || 'Session')}</div>
          <div class="item-card-subtitle">${escHtml(s.courseCode || '')} · ${escHtml(s.sessionDate || '')} · ${escHtml(s.startTime || '')} – ${escHtml(s.endTime || '')}</div>
        </div>
        <span class="badge badge-${s.status}">${escHtml(s.status || 'upcoming')}</span>
      </div>
      ${s.status === 'active' ? `<div style="margin-bottom:12px;">${buildCountdownRing(s.id, s.sessionDate, s.startTime, s.endTime)}</div>` : ''}
      <div class="item-card-footer">
        ${s.status === 'upcoming' ? `<button class="btn btn-sm btn-success" onclick="changeStatus(${s.id}, 'active', this)">Go Live</button>` : ''}
        ${s.status === 'active' ? `<button class="btn btn-sm btn-warning" onclick="changeStatus(${s.id}, 'completed', this)">End</button>` : ''}
        <button class="btn btn-sm btn-outline" onclick="viewAttendance(${s.id})">Attendance</button>
      </div>
    </div>
  `).join('');
  startCountdownTimers();
}

function renderSessionsSectionList(sessions) {
  const container = document.getElementById('sessions-detail');
  if (!container) return;

  if (!sessions.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">📅</div><h3>No sessions yet</h3><p>Create sessions from your courses to manage attendance.</p></div>`;
    return;
  }

  container.innerHTML = `
    <div class="items-grid">
      ${sessions.map(s => `
        <div class="item-card">
          <div class="item-card-header">
            <div>
              <div class="item-card-title">${escHtml(s.courseName || 'Session')}</div>
              <div class="item-card-subtitle">${escHtml(s.courseCode || '')} · ${escHtml(s.sessionDate || '')} · ${escHtml(s.startTime || '')} – ${escHtml(s.endTime || '')}</div>
            </div>
            <span class="badge badge-${s.status}">${escHtml(s.status || 'upcoming')}</span>
          </div>
          ${s.status === 'active' ? `<div style="margin-bottom:12px;">${buildCountdownRing(s.id, s.sessionDate, s.startTime, s.endTime)}</div>` : ''}
          <div class="item-card-footer">
            ${s.status === 'upcoming' ? `<button class="btn btn-sm btn-success" onclick="changeStatus(${s.id}, 'active', this)">Go Live</button>` : ''}
            ${s.status === 'active' ? `<button class="btn btn-sm btn-warning" onclick="changeStatus(${s.id}, 'completed', this)">End</button>` : ''}
            <button class="btn btn-sm btn-outline" onclick="viewAttendance(${s.id})">Attendance</button>
            <button class="btn btn-sm" onclick='openCreateSession(${s.courseId}, ${JSON.stringify(s.courseName || '')})'>+ Add Session</button>
            ${s.status === 'upcoming' ? `<button class="btn btn-sm btn-outline" onclick="openEditSession(${s.id}, '${escHtml(s.sessionDate || '')}', '${escHtml(s.startTime || '')}', '${escHtml(s.endTime || '')}')">✏️ Edit</button>` : ''}
            ${s.status === 'upcoming' ? `<button class="btn btn-sm btn-danger" onclick="confirmDeleteSession(${s.id})">🗑️</button>` : ''}
          </div>
        </div>
      `).join('')}
    </div>
  `;
  startCountdownTimers();
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
              ${s.status === 'upcoming' ? `<button class="btn btn-sm btn-outline" onclick="openEditSession(${s.id}, '${escHtml(s.sessionDate || '')}', '${escHtml(s.startTime || '')}', '${escHtml(s.endTime || '')}')">✏️ Edit</button>` : ''}
              ${s.status === 'upcoming' ? `<button class="btn btn-sm btn-danger" onclick="confirmDeleteSession(${s.id})">🗑️</button>` : ''}
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
    const rawRecords = await apiRequest(`/attendance/session/${sessionId}`);
    const records = (rawRecords || []).map(normalizeAttendanceRecord);
    if (!records.length) {
      container.innerHTML = `<h3 style="margin-bottom:16px">Session Attendance</h3><div class="empty-state"><div class="empty-icon">📋</div><h3>No attendance yet</h3><p>No students have joined this session.</p></div>`;
      return;
    }
    const rows = records.map(r => `
      <tr>
        <td>${escHtml(r.studentName || 'Unknown')}</td>
        <td>${escHtml(r.submittedIndexNumber || '')}</td>
        <td>${escHtml(r.submittedLevel || '')}</td>
        <td>${formatLocation(r.latitude, r.longitude)}</td>
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
          <thead><tr><th>Student</th><th>Index Number</th><th>Level</th><th>GPS Location</th><th>Join Time</th><th>Status</th></tr></thead>
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
  const lines = ['Student,Index Number,Level,Latitude,Longitude,Join Time,Status'];
  data.records.map(normalizeAttendanceRecord).forEach(r => {
    lines.push([
      escape(r.studentName || 'Unknown'),
      escape(r.submittedIndexNumber || ''),
      escape(r.submittedLevel || ''),
      escape(r.latitude ?? ''),
      escape(r.longitude ?? ''),
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

function normalizeAttendanceRecord(record) {
  return {
    ...record,
    studentName: record.studentName ?? record.student_name ?? 'Unknown',
    submittedIndexNumber: record.submittedIndexNumber ?? record.submitted_index_number ?? '',
    submittedLevel: record.submittedLevel ?? record.submitted_level ?? '',
    latitude: record.latitude ?? record.lat ?? null,
    longitude: record.longitude ?? record.lng ?? null
  };
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

function isMobile() {
  return window.matchMedia('(max-width: 768px)').matches;
}

function syncSectionLayout() {
  const mobileSinglePage = isMobile();
  document.body.classList.toggle('mobile-single-page', mobileSinglePage);

  if (mobileSinglePage) {
    document.querySelectorAll('.section').forEach(s => s.classList.add('active'));
    return;
  }

  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  const currentSection = document.getElementById(`section-${activeSection}`) || document.getElementById('section-overview');
  if (currentSection) currentSection.classList.add('active');
}

function showSection(name) {
  activeSection = name;
  document.querySelectorAll('[data-section]').forEach(l => {
    l.closest('li')?.classList.toggle('active', l.dataset.section === name);
  });

  if (isMobile()) {
    // On mobile all sections are visible; just scroll to the target
    const section = document.getElementById(`section-${name}`);
    if (section) section.scrollIntoView({ behavior: 'smooth', block: 'start' });
    return;
  }

  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  const section = document.getElementById(`section-${name}`);
  if (section) section.classList.add('active');
}

function openModal(id) {
  document.getElementById(id)?.classList.add('open');
}

function closeModal(id) {
  document.getElementById(id)?.classList.remove('open');
}

function formatLocation(latitude, longitude) {
  if (latitude == null || longitude == null) {
    return 'Not captured';
  }
  return `${Number(latitude).toFixed(5)}, ${Number(longitude).toFixed(5)}`;
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
    display: flex; align-items: center; gap: 12px;
  `;

  const text = document.createElement('span');
  text.textContent = msg;
  text.style.flex = '1';
  toast.appendChild(text);

  const closeBtn = document.createElement('button');
  closeBtn.textContent = '\u00D7';
  closeBtn.style.cssText = 'background:none;border:none;color:white;font-size:18px;cursor:pointer;padding:0 2px;line-height:1;';
  closeBtn.addEventListener('click', () => toast.remove());
  toast.appendChild(closeBtn);

  document.body.appendChild(toast);
  setTimeout(() => { if (toast.parentNode) toast.remove(); }, 5000);
}

function escHtml(str) {
  const div = document.createElement('div');
  div.appendChild(document.createTextNode(String(str)));
  return div.innerHTML;
}

// ===== Edit/Delete Course =====

function openEditCourse(courseId) {
  const course = lecturerCourses.find(c => c.id === courseId);
  if (!course) { showToast('Course not found.', 'error'); return; }
  document.getElementById('edit-course-id').value = courseId;
  document.getElementById('edit-course-name').value = course.courseName || '';
  document.getElementById('edit-course-code').value = course.courseCode || '';
  document.getElementById('edit-course-desc').value = course.description || '';
  openModal('modal-edit-course');
}

async function handleEditCourse(e) {
  e.preventDefault();
  const btn = e.target.querySelector('button[type="submit"]');
  const courseId = parseInt(document.getElementById('edit-course-id').value);
  const courseName = document.getElementById('edit-course-name').value.trim();
  const courseCode = document.getElementById('edit-course-code').value.trim();
  const description = document.getElementById('edit-course-desc').value.trim();
  if (!courseName || !courseCode) { showToast('Course name and code are required.', 'error'); return; }
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';
  try {
    await apiRequest(`/courses/${courseId}`, 'PUT', { courseName, courseCode, description });
    closeModal('modal-edit-course');
    showToast('Course updated successfully!', 'success');
    await loadCourses();
    updateStats();
  } catch (err) { showToast(err.message, 'error'); }
  finally { btn.disabled = false; btn.innerHTML = 'Save Changes'; }
}

async function confirmDeleteCourse(courseId, courseName) {
  if (!confirm(`Are you sure you want to delete "${courseName}"? This will also delete all its sessions and attendance records.`)) return;
  try {
    await apiRequest(`/courses/${courseId}`, 'DELETE');
    showToast('Course deleted successfully!', 'success');
    await loadAll();
  } catch (err) { showToast(err.message, 'error'); }
}

// ===== Edit/Delete Session =====

function openEditSession(sessionId, sessionDate, startTime, endTime) {
  document.getElementById('edit-session-id').value = sessionId;
  document.getElementById('edit-session-date').value = sessionDate || '';
  document.getElementById('edit-session-start').value = (startTime || '').substring(0, 5);
  document.getElementById('edit-session-end').value = (endTime || '').substring(0, 5);
  openModal('modal-edit-session');
}

async function handleEditSession(e) {
  e.preventDefault();
  const btn = e.target.querySelector('button[type="submit"]');
  const sessionId = parseInt(document.getElementById('edit-session-id').value);
  const sessionDate = document.getElementById('edit-session-date').value;
  const startTime = document.getElementById('edit-session-start').value;
  const endTime = document.getElementById('edit-session-end').value;
  if (!sessionDate || !startTime || !endTime) { showToast('All session fields are required.', 'error'); return; }
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';
  try {
    await apiRequest(`/sessions/${sessionId}`, 'PUT', { sessionDate, startTime, endTime });
    closeModal('modal-edit-session');
    showToast('Session updated successfully!', 'success');
    await loadAll();
  } catch (err) { showToast(err.message, 'error'); }
  finally { btn.disabled = false; btn.innerHTML = 'Save Changes'; }
}

async function confirmDeleteSession(sessionId) {
  if (!confirm('Are you sure you want to delete this session? All attendance records for it will be lost.')) return;
  try {
    await apiRequest(`/sessions/${sessionId}`, 'DELETE');
    showToast('Session deleted successfully!', 'success');
    await loadAll();
  } catch (err) { showToast(err.message, 'error'); }
}

// ===== Countdown Timer =====

const COUNTDOWN_CIRCUMFERENCE = 2 * Math.PI * 20; // radius=20

function buildCountdownRing(sessionId, sessionDate, startTime, endTime) {
  return `
    <div class="countdown-ring" data-countdown data-session-id="${sessionId}"
         data-start="${sessionDate} ${startTime}" data-end="${sessionDate} ${endTime}">
      <svg viewBox="0 0 48 48">
        <circle class="ring-bg" cx="24" cy="24" r="20" />
        <circle class="ring-progress" cx="24" cy="24" r="20"
                stroke-dasharray="${COUNTDOWN_CIRCUMFERENCE}"
                stroke-dashoffset="0" />
      </svg>
      <div class="countdown-label">
        <span class="countdown-time">--:--</span>
        <small>remaining</small>
      </div>
    </div>
  `;
}

function startCountdownTimers() {
  if (countdownInterval) clearInterval(countdownInterval);
  updateAllCountdowns();
  countdownInterval = setInterval(updateAllCountdowns, 1000);
}

function updateAllCountdowns() {
  document.querySelectorAll('[data-countdown]').forEach(el => {
    const endStr = el.dataset.end;
    const startStr = el.dataset.start;
    if (!endStr || !startStr) return;

    const now = new Date();
    const end = new Date(endStr);
    const start = new Date(startStr);

    const totalDuration = end - start;
    const remaining = end - now;

    const ring = el.querySelector('.ring-progress');
    const label = el.querySelector('.countdown-time');

    if (remaining <= 0) {
      label.textContent = '0:00';
      ring.style.strokeDashoffset = COUNTDOWN_CIRCUMFERENCE;
      ring.classList.remove('warning');
      ring.classList.add('danger');
      return;
    }

    const fraction = Math.max(0, Math.min(1, remaining / totalDuration));
    ring.style.strokeDashoffset = COUNTDOWN_CIRCUMFERENCE * (1 - fraction);

    // Color based on remaining fraction
    ring.classList.remove('warning', 'danger');
    if (fraction <= 0.15) ring.classList.add('danger');
    else if (fraction <= 0.35) ring.classList.add('warning');

    const mins = Math.floor(remaining / 60000);
    const secs = Math.floor((remaining % 60000) / 1000);
    label.textContent = `${mins}:${secs.toString().padStart(2, '0')}`;
  });
}

// ===== Search/Filter =====

function filterCards(containerId, query) {
  const container = document.getElementById(containerId);
  if (!container) return;
  const q = query.toLowerCase().trim();
  const cards = container.querySelectorAll('.item-card');
  cards.forEach(card => {
    const text = card.textContent.toLowerCase();
    card.style.display = q === '' || text.includes(q) ? '' : 'none';
  });
}

function filterTableRows(containerId, query) {
  const container = document.getElementById(containerId);
  if (!container) return;
  const q = query.toLowerCase().trim();
  const rows = container.querySelectorAll('tbody tr');
  rows.forEach(row => {
    const text = row.textContent.toLowerCase();
    row.style.display = q === '' || text.includes(q) ? '' : 'none';
  });
}
