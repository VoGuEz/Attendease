let currentUser = null;
let allCourses = {};
let activeSection = 'overview';
let pendingJoinButton = null;
let joinLocation = null;
let sidebarOpen = false;

document.addEventListener('DOMContentLoaded', async () => {
  currentUser = requireRole('student');
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
  document.getElementById('join-session-form')?.addEventListener('submit', submitJoinSession);
  document.getElementById('capture-location-btn')?.addEventListener('click', captureLocation);

  document.querySelectorAll('[data-section]').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      showSection(link.dataset.section);
      closeMobileMenu();
    });
  });

  await loadAll();

  // On mobile, open Active Sessions by default so students can join quickly.
  if (window.matchMedia('(max-width: 768px)').matches) {
    showSection('active');
  }
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
  showLoading('overview-stats', true);
  await Promise.all([loadStats(), loadCourses(), loadActiveSessions(), loadAllAvailableSessions()]);
  showLoading('overview-stats', false);
}

async function loadAllAvailableSessions() {
  try {
    const data = await apiRequest('/courses');
    const enrolledCourses = data.enrolled || [];
    
    // Fetch all sessions for each enrolled course
    const sessionsByDate = {};
    
    for (const course of enrolledCourses) {
      try {
        const sessions = await apiRequest(`/courses/${course.id}/sessions`);
        (sessions || []).forEach(s => {
          // Show upcoming and active sessions
          if (s.status === 'active' || s.status === 'upcoming') {
            if (!sessionsByDate[s.sessionDate]) {
              sessionsByDate[s.sessionDate] = [];
            }
            sessionsByDate[s.sessionDate].push({...s, courseName: course.courseName, courseId: course.id});
          }
        });
      } catch (err) {
        console.error(`Failed to load sessions for course ${course.id}:`, err);
      }
    }
    
    // Sort by date and flatten
    const allSessions = Object.entries(sessionsByDate)
      .sort(([dateA], [dateB]) => new Date(dateA) - new Date(dateB))
      .flatMap(([_, sessions]) => sessions);
    
    renderAllAvailableSessions(allSessions);
  } catch (err) {
    console.error('Failed to load all available sessions:', err);
    document.getElementById('all-available-sessions').innerHTML = `<div class="empty-state"><div class="empty-icon">📡</div><h3>Could not load sessions</h3></div>`;
  }
}

function renderAllAvailableSessions(sessions) {
  const container = document.getElementById('all-available-sessions');
  if (!sessions || sessions.length === 0) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">📡</div><h3>No Sessions Available</h3><p>No upcoming or active sessions in your enrolled courses.</p></div>`;
    return;
  }
  
  // Limit to 6 most recent/relevant sessions on overview
  const displaySessions = sessions.slice(0, 6);
  
  container.innerHTML = displaySessions.map(s => `
    <div class="item-card" id="session-card-${s.id}">
      <div class="item-card-header">
        <div>
          <div class="item-card-title">${escHtml(s.courseName || 'Session')}</div>
          <div class="item-card-subtitle">${escHtml(s.sessionDate || '')} · ${escHtml(s.startTime || '')} – ${escHtml(s.endTime || '')}</div>
        </div>
        <span class="badge ${s.status === 'active' ? 'badge-active' : 'badge-upcoming'}">
          ${s.status === 'active' ? 'Active' : 'Upcoming'}
        </span>
      </div>
      <div class="item-card-footer">
        <button class="btn btn-sm btn-success" data-session-id="${s.id}" onclick="openJoinModal(${s.id}, this)">Join</button>
      </div>
    </div>
  `).join('');
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
    const fullContainer = document.getElementById('active-sessions-full');
    if (fullContainer) {
      fullContainer.innerHTML = document.getElementById('active-sessions').innerHTML;
    }
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
        <button class="btn btn-sm btn-success" data-session-id="${s.id}" onclick="openJoinModal(${s.id}, this)">Join Session</button>
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

function openJoinModal(sessionId, btn) {
  pendingJoinButton = btn;
  joinLocation = null;

  const modal = document.getElementById('modal-join-session');
  const form = document.getElementById('join-session-form');
  const fullNameInput = document.getElementById('join-full-name');
  const sessionInput = document.getElementById('join-session-id');
  const msg = document.getElementById('join-session-msg');
  const locationStatus = document.getElementById('join-location-status');

  form?.reset();
  if (fullNameInput) {
    fullNameInput.value = currentUser?.fullName || '';
  }
  if (sessionInput) {
    sessionInput.value = String(sessionId);
  }
  if (msg) {
    msg.style.display = 'none';
    msg.textContent = '';
  }
  if (locationStatus) {
    locationStatus.textContent = 'Location not captured yet.';
  }
  modal?.classList.add('open');
}

function closeJoinModal() {
  document.getElementById('modal-join-session')?.classList.remove('open');
  joinLocation = null;
  pendingJoinButton = null;
}

async function captureLocation() {
  const statusEl = document.getElementById('join-location-status');
  if (!navigator.geolocation) {
    showJoinMessage('Geolocation is not supported on this device/browser.', 'error');
    return;
  }

  if (statusEl) statusEl.textContent = 'Capturing current location...';

  try {
    const position = await new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 0
      });
    });

    joinLocation = {
      latitude: Number(position.coords.latitude.toFixed(6)),
      longitude: Number(position.coords.longitude.toFixed(6))
    };

    if (statusEl) {
      statusEl.textContent = `Captured: ${joinLocation.latitude}, ${joinLocation.longitude}`;
    }
    showJoinMessage('Location captured successfully.', 'success');
  } catch (err) {
    if (statusEl) statusEl.textContent = 'Location not captured yet.';
    showJoinMessage('Allow location access to join this session.', 'error');
  }
}

async function submitJoinSession(e) {
  e.preventDefault();

  const sessionId = Number(document.getElementById('join-session-id')?.value);
  const fullName = document.getElementById('join-full-name')?.value.trim() || '';
  const indexNumber = document.getElementById('join-index-number')?.value.trim() || '';
  const level = document.getElementById('join-level')?.value.trim() || '';
  const submitBtn = document.getElementById('join-session-submit-btn');
  const buttonsToUpdate = Array.from(document.querySelectorAll(`[data-session-id="${sessionId}"]`));

  if (!fullName || !indexNumber || !level) {
    showJoinMessage('Full name, index number, and level are all required.', 'error');
    return;
  }

  if (!joinLocation) {
    showJoinMessage('Capture your GPS location before joining the session.', 'error');
    return;
  }

  buttonsToUpdate.forEach(button => {
    button.disabled = true;
    button.innerHTML = '<span class="spinner"></span>';
  });
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner"></span>';
  }

  try {
    const result = await apiRequest('/attendance/join', 'POST', {
      sessionId,
      fullName,
      indexNumber,
      level,
      latitude: joinLocation.latitude,
      longitude: joinLocation.longitude
    });
    showToast('Joined session successfully!', 'success');
    closeJoinModal();

    const stats = result.stats;
    if (stats) {
      const pct = stats.percentage ?? 0;
      document.getElementById('stat-attended').textContent = stats.attendedSessions;
      document.getElementById('stat-percentage').textContent = pct + '%';
      document.getElementById('overall-progress-fill').style.width = pct + '%';
      document.getElementById('overall-progress-label').textContent = pct + '% attendance';
    }

    document.querySelectorAll(`#session-card-${sessionId} .item-card-footer`).forEach(footer => {
      footer.innerHTML = '<span class="badge badge-active">✓ Joined</span>';
    });
  } catch (err) {
    showToast(err.message, 'error');
    showJoinMessage(err.message, 'error');
    buttonsToUpdate.forEach(button => {
      button.disabled = false;
      button.innerHTML = 'Join Session';
    });
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = 'Join Session';
    }
  }
}

function showJoinMessage(message, type) {
  const el = document.getElementById('join-session-msg');
  if (!el) return;
  el.textContent = message;
  el.style.display = 'block';
  el.style.color = type === 'error' ? 'var(--danger)' : 'var(--success)';
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
