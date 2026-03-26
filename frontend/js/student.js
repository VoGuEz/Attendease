let currentUser = null;
let allCourses = {};
let activeSection = 'overview';

document.addEventListener('DOMContentLoaded', async () => {
  currentUser = requireRole('student');
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

  await loadAll();
});

async function loadAll() {
  showLoading('overview-stats', true);
  await Promise.all([loadStats(), loadCourses(), loadActiveSessions()]);
  showLoading('overview-stats', false);
}

async function loadStats() {
  try {
    const stats = await apiRequest('/attendance/stats');
    document.getElementById('stat-enrolled').textContent = allCourses.enrolled?.length ?? 0;
    document.getElementById('stat-attended').textContent = stats.attendedSessions ?? 0;
    document.getElementById('stat-percentage').textContent = (stats.percentage ?? 0) + '%';
    document.getElementById('total-sessions-stat').textContent = stats.totalSessions ?? 0;

    const pct = stats.percentage ?? 0;
    document.getElementById('overall-progress-fill').style.width = pct + '%';
    document.getElementById('overall-progress-label').textContent = pct + '% attendance';

    const color = pct >= 75 ? 'var(--success)' : pct >= 50 ? 'var(--warning)' : 'var(--danger)';
    document.getElementById('overall-progress-fill').style.background = color;
  } catch (err) {
    console.error('Failed to load stats:', err);
  }
}

async function loadCourses() {
  try {
    const data = await apiRequest('/courses');
    allCourses = data;

    renderEnrolledCourses(data.enrolled || []);
    renderAvailableCourses(data.available || []);

    document.getElementById('stat-enrolled').textContent = (data.enrolled || []).length;
  } catch (err) {
    console.error('Failed to load courses:', err);
  }
}

async function loadActiveSessions() {
  try {
    const sessions = await apiRequest('/sessions/active');
    renderActiveSessions(sessions || []);
  } catch (err) {
    console.error('Failed to load active sessions:', err);
  }
}

function renderEnrolledCourses(courses) {
  const container = document.getElementById('enrolled-courses');
  if (!courses.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">📚</div><h3>No Enrolled Courses</h3><p>Browse available courses below and enroll.</p></div>`;
    return;
  }
  container.innerHTML = courses.map(c => `
    <div class="item-card">
      <div class="item-card-header">
        <div>
          <div class="item-card-title">${escHtml(c.courseName)}</div>
          <div class="item-card-subtitle">${escHtml(c.courseCode)} · ${escHtml(c.lecturerName || '')}</div>
        </div>
      </div>
      <div class="item-card-body text-muted" style="font-size:13px">${escHtml(c.description || 'No description')}</div>
      <div class="item-card-footer">
        <button class="btn btn-sm btn-outline" onclick="viewCourseSessions(${c.id}, '${escHtml(c.courseName)}')">View Sessions</button>
      </div>
    </div>
  `).join('');
}

function renderAvailableCourses(courses) {
  const container = document.getElementById('available-courses');
  if (!courses.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">🎓</div><h3>All courses enrolled</h3><p>You're enrolled in all available courses.</p></div>`;
    return;
  }
  container.innerHTML = courses.map(c => `
    <div class="item-card">
      <div class="item-card-header">
        <div>
          <div class="item-card-title">${escHtml(c.courseName)}</div>
          <div class="item-card-subtitle">${escHtml(c.courseCode)} · ${escHtml(c.lecturerName || '')}</div>
        </div>
      </div>
      <div class="item-card-body text-muted" style="font-size:13px">${escHtml(c.description || 'No description')}</div>
      <div class="item-card-footer">
        <button class="btn btn-sm" onclick="enrollCourse(${c.id}, this)">Enroll</button>
      </div>
    </div>
  `).join('');
}

function renderActiveSessions(sessions) {
  const container = document.getElementById('active-sessions');
  if (!sessions.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">📡</div><h3>No Active Sessions</h3><p>There are no active sessions right now.</p></div>`;
    return;
  }
  container.innerHTML = sessions.map(s => `
    <div class="item-card" id="session-card-${s.id}">
      <div class="item-card-header">
        <div>
          <div class="item-card-title">${escHtml(s.courseName || 'Session')}</div>
          <div class="item-card-subtitle">${escHtml(s.sessionDate || '')} · ${escHtml(s.startTime || '')} – ${escHtml(s.endTime || '')}</div>
        </div>
        <span class="badge badge-active">Active</span>
      </div>
      <div class="item-card-footer">
        <button class="btn btn-sm btn-success" onclick="joinSession(${s.id}, this)">Join Session</button>
      </div>
    </div>
  `).join('');
}

async function enrollCourse(courseId, btn) {
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';
  try {
    await apiRequest(`/courses/${courseId}/enroll`, 'POST');
    showToast('Enrolled successfully!', 'success');
    await loadCourses();
    await loadStats();
  } catch (err) {
    showToast(err.message, 'error');
    btn.disabled = false;
    btn.innerHTML = 'Enroll';
  }
}

async function joinSession(sessionId, btn) {
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';
  try {
    const result = await apiRequest('/attendance/join', 'POST', { sessionId });
    showToast('Joined session successfully!', 'success');

    const stats = result.stats;
    if (stats) {
      const pct = stats.percentage ?? 0;
      document.getElementById('stat-attended').textContent = stats.attendedSessions;
      document.getElementById('stat-percentage').textContent = pct + '%';
      document.getElementById('overall-progress-fill').style.width = pct + '%';
      document.getElementById('overall-progress-label').textContent = pct + '% attendance';
    }

    const card = document.getElementById(`session-card-${sessionId}`);
    if (card) {
      card.querySelector('.item-card-footer').innerHTML = '<span class="badge badge-active">✓ Joined</span>';
    }
  } catch (err) {
    showToast(err.message, 'error');
    btn.disabled = false;
    btn.innerHTML = 'Join Session';
  }
}

async function viewCourseSessions(courseId, courseName) {
  showSection('courses');
  const container = document.getElementById('course-sessions-detail');
  container.innerHTML = `<h3 style="margin-bottom:16px">${escHtml(courseName)} – Sessions</h3><div class="loading-overlay"><span class="spinner"></span></div>`;
  document.getElementById('sessions-panel').style.display = 'block';

  try {
    const sessions = await apiRequest(`/courses/${courseId}/sessions`);
    if (!sessions.length) {
      container.innerHTML = `<h3 style="margin-bottom:16px">${escHtml(courseName)} – Sessions</h3><div class="empty-state"><div class="empty-icon">📅</div><h3>No Sessions Yet</h3></div>`;
      return;
    }
    const rows = sessions.map(s => `
      <tr>
        <td>${escHtml(s.sessionDate || '')}</td>
        <td>${escHtml(s.startTime || '')} – ${escHtml(s.endTime || '')}</td>
        <td><span class="badge badge-${s.status}">${s.status}</span></td>
      </tr>
    `).join('');
    container.innerHTML = `
      <h3 style="margin-bottom:16px">${escHtml(courseName)} – Sessions</h3>
      <div class="table-wrapper">
        <table>
          <thead><tr><th>Date</th><th>Time</th><th>Status</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    `;
  } catch (err) {
    container.innerHTML = `<p class="text-danger">Failed to load sessions: ${err.message}</p>`;
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

function showLoading(id, loading) {
  const el = document.getElementById(id);
  if (!el) return;
  if (loading) el.style.opacity = '0.5';
  else el.style.opacity = '1';
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
    animation: slideIn 0.3s ease;
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
