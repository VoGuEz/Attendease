let currentUser = null;
let allCourses = {};
let activeSection = 'overview';
let pendingJoinButton = null;
let joinLocation = null;
let sidebarOpen = false;
let countdownInterval = null;

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr + 'T00:00:00');
  if (isNaN(d)) return dateStr;
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
}

function formatTime(timeStr) {
  if (!timeStr) return '';
  const [h, m] = timeStr.split(':');
  const date = new Date(2000, 0, 1, +h, +m);
  if (isNaN(date)) return timeStr;
  return date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
}

function renderSkeletonCards(container, count = 3) {
  container.innerHTML = Array.from({ length: count }, () => `
    <div class="skeleton">
      <div class="skeleton-line long"></div>
      <div class="skeleton-line short"></div>
      <div class="skeleton-line medium" style="margin-top:16px"></div>
      <div style="display:flex;gap:8px;margin-top:16px">
        <div class="skeleton-line short" style="margin:0"></div>
        <div class="skeleton-line short" style="margin:0"></div>
      </div>
    </div>
  `).join('');
}

function setAvatarBadge(user) {
  const el = document.getElementById('user-avatar');
  if (!el || !user?.fullName) return;
  const parts = user.fullName.trim().split(/\s+/);
  const initials = parts.length >= 2
    ? (parts[0][0] + parts[parts.length - 1][0])
    : parts[0].substring(0, 2);
  el.textContent = initials.toUpperCase();
}

function updateNotificationDot(activeCount) {
  const dot = document.getElementById('notif-dot-active');
  if (dot) dot.style.display = activeCount > 0 ? 'inline-block' : 'none';
}

function updateBreadcrumb(...crumbs) {
  const bc = document.getElementById('breadcrumb');
  if (!bc) return;
  if (crumbs.length === 0) { bc.innerHTML = ''; return; }
  bc.innerHTML = crumbs.map((c, i) => {
    if (i < crumbs.length - 1) {
      return `<a href="#" onclick="${c.onclick || ''};return false;">${escHtml(c.label)}</a><span class="separator">›</span>`;
    }
    return `<span class="current">${escHtml(c.label)}</span>`;
  }).join('');
}

function animateCount(el, target) {
  if (!el) return;
  const duration = 600;
  const start = performance.now();
  el.classList.add('counting');
  function tick(now) {
    const elapsed = now - start;
    const progress = Math.min(elapsed / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    el.textContent = Math.round(target * eased);
    if (progress < 1) requestAnimationFrame(tick);
    else { el.textContent = target; el.classList.remove('counting'); }
  }
  requestAnimationFrame(tick);
}

document.addEventListener('DOMContentLoaded', async () => {
  currentUser = requireRole('student');
  if (!currentUser) return;

  document.getElementById('user-name').textContent = currentUser.fullName;
  setAvatarBadge(currentUser);
  renderThemeSwitcher('theme-switcher-container');

  const sidebarUserName = document.getElementById('sidebar-user-name');
  const sidebarAvatar = document.getElementById('sidebar-avatar');
  if (sidebarUserName) sidebarUserName.textContent = currentUser.fullName;
  if (sidebarAvatar) {
    const initials = (currentUser.fullName || '').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
    sidebarAvatar.textContent = initials;
  }

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

  if (mobileOverlay) mobileOverlay.addEventListener('click', closeMobileMenu);

  document.getElementById('logout-btn').addEventListener('click', logout);
  document.getElementById('join-session-form')?.addEventListener('submit', submitJoinSession);
  document.getElementById('capture-location-btn')?.addEventListener('click', captureLocation);

  document.getElementById('search-enrolled')?.addEventListener('input', (e) => filterCards('enrolled-courses', e.target.value));
  document.getElementById('search-available')?.addEventListener('input', (e) => filterCards('available-courses', e.target.value));

  document.querySelectorAll('[data-section]').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      showSection(link.dataset.section);
      closeMobileMenu();
    });
  });

  await loadAll();
  syncSectionLayout();
  window.addEventListener('resize', syncSectionLayout);
});

function closeMobileMenu() {
  if (sidebarOpen) {
    sidebarOpen = false;
    document.getElementById('hamburger-btn').classList.remove('active');
    document.getElementById('sidebar').classList.remove('open');
    document.getElementById('mobile-overlay').classList.remove('open');
  }
}

async function loadAll() {
  showLoading('overview-stats', true);
  renderSkeletonCards(document.getElementById('enrolled-courses'));
  renderSkeletonCards(document.getElementById('available-courses'));
  renderSkeletonCards(document.getElementById('all-available-sessions'), 2);
  await Promise.all([loadStats(), loadCourses(), loadActiveSessions(), loadAllAvailableSessions()]);
  showLoading('overview-stats', false);
}

async function loadAllAvailableSessions() {
  try {
    const data = await apiRequest('/courses');
    const enrolledCourses = data.enrolled || [];
    const sessionsByDate = {};
    for (const course of enrolledCourses) {
      try {
        const sessions = await apiRequest(`/courses/${course.id}/sessions`);
        (sessions || []).forEach(s => {
          if (s.status === 'active' || s.status === 'upcoming') {
            if (!sessionsByDate[s.sessionDate]) sessionsByDate[s.sessionDate] = [];
            sessionsByDate[s.sessionDate].push({...s, courseName: course.courseName, courseId: course.id});
          }
        });
      } catch (err) { console.error(`Failed to load sessions for course ${course.id}:`, err); }
    }
    const allSessions = Object.entries(sessionsByDate)
      .sort(([a], [b]) => new Date(a) - new Date(b))
      .flatMap(([_, s]) => s);
    renderAllAvailableSessions(allSessions);
  } catch (err) {
    document.getElementById('all-available-sessions').innerHTML = `<div class="empty-state"><div class="empty-icon">📡</div><h3>Could not load sessions</h3></div>`;
  }
}

function renderAllAvailableSessions(sessions) {
  const container = document.getElementById('all-available-sessions');
  if (!sessions || sessions.length === 0) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">📡</div><h3>No Sessions Available</h3><p>No upcoming or active sessions in your enrolled courses.</p></div>`;
    return;
  }
  container.innerHTML = sessions.slice(0, 6).map(s => `
    <div class="item-card" id="session-card-${s.id}">
      <div class="item-card-header">
        <div>
          <div class="item-card-title">${escHtml(s.courseName || 'Session')}</div>
          <div class="item-card-subtitle">${formatDate(s.sessionDate)} · ${formatTime(s.startTime)} – ${formatTime(s.endTime)}</div>
        </div>
        <span class="badge ${s.status === 'active' ? 'badge-active' : 'badge-upcoming'}">
          ${s.status === 'active' ? 'Active' : 'Upcoming'}
        </span>
      </div>
      ${s.status === 'active' ? `<div style="margin-bottom:12px;">${buildCountdownRing(s.id, s.sessionDate, s.startTime, s.endTime)}</div>` : ''}
      <div class="item-card-footer">
        <button class="btn btn-sm btn-success" data-session-id="${s.id}" onclick="openJoinModal(${s.id}, this)">✋ Join</button>
      </div>
    </div>
  `).join('');
  startCountdownTimers();
}

async function loadStats() {
  try {
    const stats = await apiRequest('/attendance/stats');
    animateCount(document.getElementById('stat-enrolled'), allCourses.enrolled?.length ?? 0);
    animateCount(document.getElementById('stat-attended'), stats.attendedSessions ?? 0);
    document.getElementById('stat-percentage').textContent = (stats.percentage ?? 0) + '%';
    animateCount(document.getElementById('total-sessions-stat'), stats.totalSessions ?? 0);
    const pct = stats.percentage ?? 0;
    document.getElementById('overall-progress-fill').style.width = pct + '%';
    document.getElementById('overall-progress-label').textContent = pct + '% attendance';
    const color = pct >= 75 ? 'var(--success)' : pct >= 50 ? 'var(--warning)' : 'var(--danger)';
    document.getElementById('overall-progress-fill').style.background = color;
  } catch (err) { console.error('Failed to load stats:', err); }
}

async function loadCourses() {
  try {
    const data = await apiRequest('/courses');
    allCourses = data;
    renderEnrolledCourses(data.enrolled || []);
    renderAvailableCourses(data.available || []);
    document.getElementById('stat-enrolled').textContent = (data.enrolled || []).length;
  } catch (err) { console.error('Failed to load courses:', err); }
}

async function loadActiveSessions() {
  try {
    const sessions = await apiRequest('/sessions/active');
    renderActiveSessions(sessions || []);
    const fullContainer = document.getElementById('active-sessions-full');
    if (fullContainer) fullContainer.innerHTML = document.getElementById('active-sessions').innerHTML;
  } catch (err) { console.error('Failed to load active sessions:', err); }
}

function renderEnrolledCourses(courses) {
  const container = document.getElementById('enrolled-courses');
  if (!courses.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">📚</div><h3>No Enrolled Courses</h3><p>Browse available courses below and enroll.</p></div>`;
    return;
  }
  container.innerHTML = courses.map(c => {
    const attended = c.attendedSessions ?? 0;
    const total = c.totalSessions ?? 0;
    const pct = total > 0 ? Math.round((attended / total) * 100) : 0;
    const barColor = pct >= 75 ? 'var(--success)' : pct >= 50 ? 'var(--warning)' : 'var(--danger)';
    return `
    <div class="item-card">
      <div class="item-card-header">
        <div>
          <div class="item-card-title">${escHtml(c.courseName)}</div>
          <div class="item-card-subtitle">${escHtml(c.courseCode)} · ${escHtml(c.lecturerName || '')}</div>
        </div>
      </div>
      <div class="item-card-body text-muted" style="font-size:13px">${escHtml(c.description || 'No description')}</div>
      ${total > 0 ? `
      <div class="course-progress">
        <div class="course-progress-label"><span>Attendance</span><span>${attended}/${total} (${pct}%)</span></div>
        <div class="course-progress-bar"><div class="course-progress-fill" style="width:${pct}%;background:${barColor}"></div></div>
      </div>` : ''}
      <div class="item-card-footer" style="margin-top:10px">
        <button class="btn btn-sm btn-outline" onclick="viewCourseSessions(${c.id}, '${escHtml(c.courseName)}')">📅 View Sessions</button>
      </div>
    </div>`;
  }).join('');
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
        <button class="btn btn-sm" onclick="enrollCourse(${c.id}, this)">➕ Enroll</button>
      </div>
    </div>`).join('');
}

function renderActiveSessions(sessions) {
  const container = document.getElementById('active-sessions');
  updateNotificationDot(sessions.length);
  if (!sessions.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">📡</div><h3>No Active Sessions</h3><p>There are no active sessions right now.</p></div>`;
    return;
  }
  container.style.display = '';
  container.innerHTML = sessions.map(s => `
    <div class="item-card" id="session-card-${s.id}">
      <div class="item-card-header">
        <div>
          <div class="item-card-title">${escHtml(s.courseName || 'Session')}</div>
          <div class="item-card-subtitle">${formatDate(s.sessionDate)} · ${formatTime(s.startTime)} – ${formatTime(s.endTime)}</div>
        </div>
        <span class="badge badge-active">Active</span>
      </div>
      <div style="margin-bottom:12px;">${buildCountdownRing(s.id, s.sessionDate, s.startTime, s.endTime)}</div>
      <div class="item-card-footer">
        <button class="btn btn-sm btn-success" data-session-id="${s.id}" onclick="openJoinModal(${s.id}, this)">✋ Join Session</button>
      </div>
    </div>`).join('');
  startCountdownTimers();
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

// ===== JOIN MODAL — Step 1: Code Entry =====

function openJoinModal(sessionId, btn) {
  pendingJoinButton = btn;
  joinLocation = null;

  const modal = document.getElementById('modal-join-session');
  const codeStep = document.getElementById('join-code-step');
  const detailsStep = document.getElementById('join-details-step');
  const form = document.getElementById('join-session-form');
  const sessionInput = document.getElementById('join-session-id');
  const msg = document.getElementById('join-session-msg');
  const codeMsg = document.getElementById('join-code-msg');
  const codeInput = document.getElementById('join-session-code');
  const locationStatus = document.getElementById('join-location-status');

  // Reset to step 1
  if (codeStep) codeStep.style.display = '';
  if (detailsStep) detailsStep.style.display = 'none';
  if (codeInput) codeInput.value = '';
  if (codeMsg) { codeMsg.style.display = 'none'; codeMsg.textContent = ''; }
  if (msg) { msg.style.display = 'none'; msg.textContent = ''; }
  if (locationStatus) locationStatus.textContent = 'Location not captured yet.';
  if (sessionInput) sessionInput.value = String(sessionId);
  form?.reset();

  const fullNameInput = document.getElementById('join-full-name');
  if (fullNameInput) fullNameInput.value = currentUser?.fullName || '';

  modal?.classList.add('open');
}

// Step 1: validate code then reveal the details form
async function validateAndProceed() {
  const sessionId = Number(document.getElementById('join-session-id')?.value);
  const code = document.getElementById('join-session-code')?.value.trim();
  const codeMsg = document.getElementById('join-code-msg');
  const validateBtn = document.getElementById('validate-code-btn');

  if (!code || code.length !== 6) {
    codeMsg.textContent = 'Please enter the 6-digit session code.';
    codeMsg.style.color = 'var(--danger)';
    codeMsg.style.display = 'block';
    return;
  }

  validateBtn.disabled = true;
  validateBtn.innerHTML = '<span class="spinner"></span>';

  try {
    await apiRequest(`/attendance/session/${sessionId}/validate?code=${code}`, 'GET');
    document.getElementById('join-session-code-confirmed').value = code;
    document.getElementById('join-code-step').style.display = 'none';
    document.getElementById('join-details-step').style.display = '';
  } catch (err) {
    codeMsg.textContent = err.message || 'Invalid session code. Please try again.';
    codeMsg.style.color = 'var(--danger)';
    codeMsg.style.display = 'block';
  } finally {
    validateBtn.disabled = false;
    validateBtn.innerHTML = 'Verify Code';
  }
}

function closeJoinModal() {
  document.getElementById('modal-join-session')?.classList.remove('open');
  joinLocation = null;
  pendingJoinButton = null;
}

async function captureLocation() {
  const statusEl = document.getElementById('join-location-status');
  if (!navigator.geolocation) { showJoinMessage('Geolocation is not supported on this device/browser.', 'error'); return; }
  if (statusEl) statusEl.textContent = 'Capturing current location...';
  try {
    const position = await new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 });
    });
    joinLocation = {
      latitude: Number(position.coords.latitude.toFixed(6)),
      longitude: Number(position.coords.longitude.toFixed(6))
    };
    if (statusEl) statusEl.textContent = `Captured: ${joinLocation.latitude}, ${joinLocation.longitude}`;
    showJoinMessage('Location captured successfully.', 'success');
  } catch (err) {
    if (statusEl) statusEl.textContent = 'Location not captured yet.';
    showJoinMessage('Allow location access to join this session.', 'error');
  }
}

async function submitJoinSession(e) {
  e.preventDefault();

  const sessionId = Number(document.getElementById('join-session-id')?.value);
  const code = document.getElementById('join-session-code-confirmed')?.value;
  const fullName = document.getElementById('join-full-name')?.value.trim() || '';
  const indexNumber = document.getElementById('join-index-number')?.value.trim() || '';
  const level = document.getElementById('join-level')?.value.trim() || '';
  const submitBtn = document.getElementById('join-session-submit-btn');
  const buttonsToUpdate = Array.from(document.querySelectorAll(`[data-session-id="${sessionId}"]`));

  if (!fullName || !indexNumber || !level) { showJoinMessage('Full name, index number, and level are all required.', 'error'); return; }
  if (!joinLocation) { showJoinMessage('Capture your GPS location before joining the session.', 'error'); return; }

  buttonsToUpdate.forEach(b => { b.disabled = true; b.innerHTML = '<span class="spinner"></span>'; });
  if (submitBtn) { submitBtn.disabled = true; submitBtn.innerHTML = '<span class="spinner"></span>'; }

  try {
    const result = await apiRequest('/attendance/join', 'POST', {
      sessionId, code, fullName, indexNumber, level,
      latitude: joinLocation.latitude, longitude: joinLocation.longitude
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
    buttonsToUpdate.forEach(b => { b.disabled = false; b.innerHTML = 'Join Session'; });
  } finally {
    if (submitBtn) { submitBtn.disabled = false; submitBtn.innerHTML = 'Join Session'; }
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
  updateBreadcrumb({ label: 'Courses', onclick: "showSection('courses')" }, { label: courseName });
  const container = document.getElementById('course-sessions-detail');
  container.innerHTML = `<h3 style="margin-bottom:16px">${escHtml(courseName)} – Sessions</h3>`;
  renderSkeletonCards(container, 2);
  document.getElementById('sessions-panel').style.display = 'block';
  try {
    const sessions = await apiRequest(`/courses/${courseId}/sessions`);
    if (!sessions.length) {
      container.innerHTML = `<h3 style="margin-bottom:16px">${escHtml(courseName)} – Sessions</h3><div class="empty-state"><div class="empty-icon">📅</div><h3>No Sessions Yet</h3></div>`;
      return;
    }
    const rows = sessions.map(s => `
      <tr>
        <td>${formatDate(s.sessionDate)}</td>
        <td>${formatTime(s.startTime)} – ${formatTime(s.endTime)}</td>
        <td><span class="badge badge-${s.status}">${s.status}</span></td>
      </tr>`).join('');
    container.innerHTML = `
      <h3 style="margin-bottom:16px">${escHtml(courseName)} – Sessions</h3>
      <div class="table-wrapper">
        <table>
          <thead><tr><th>Date</th><th>Time</th><th>Status</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>`;
  } catch (err) {
    container.innerHTML = `<p class="text-danger">Failed to load sessions: ${err.message}</p>`;
  }
}

function isMobile() { return window.matchMedia('(max-width: 768px)').matches; }

function syncSectionLayout() {
  document.body.classList.toggle('mobile-single-page', isMobile());
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  const current = document.getElementById(`section-${activeSection}`) || document.getElementById('section-overview');
  if (current) current.classList.add('active');
}

function showSection(name) {
  activeSection = name;
  document.querySelectorAll('[data-section]').forEach(l => l.closest('li')?.classList.toggle('active', l.dataset.section === name));
  const labels = { overview: 'Overview', courses: 'My Courses', active: 'Active Sessions', settings: 'Settings' };
  updateBreadcrumb({ label: labels[name] || name });
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  const section = document.getElementById(`section-${name}`);
  if (section) section.classList.add('active');
  if (isMobile()) window.scrollTo({ top: 0, behavior: 'smooth' });
}

function showLoading(id, loading) {
  const el = document.getElementById(id);
  if (el) el.style.opacity = loading ? '0.5' : '1';
}

function showToast(msg, type = 'success') {
  const existing = document.querySelector('.toast');
  if (existing) existing.remove();
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.style.cssText = `position:fixed;bottom:24px;right:24px;z-index:1000;
    background:${type === 'success' ? 'var(--success)' : type === 'error' ? 'var(--danger)' : 'var(--warning)'};
    color:white;padding:12px 20px;border-radius:8px;font-size:14px;font-weight:500;
    box-shadow:0 4px 16px rgba(0,0,0,0.3);display:flex;align-items:center;gap:12px;max-width:420px;`;
  const icon = document.createElement('span');
  icon.textContent = type === 'success' ? '✅' : type === 'error' ? '❌' : '⚠️';
  const text = document.createElement('span');
  text.textContent = msg; text.style.flex = '1';
  const closeBtn = document.createElement('button');
  closeBtn.textContent = '×';
  closeBtn.style.cssText = 'background:none;border:none;color:white;font-size:18px;cursor:pointer;padding:0 2px;line-height:1;';
  closeBtn.addEventListener('click', () => { toast.classList.add('toast-exit'); toast.addEventListener('animationend', () => toast.remove()); });
  toast.append(icon, text, closeBtn);
  document.body.appendChild(toast);
  setTimeout(() => { if (toast.parentNode) { toast.classList.add('toast-exit'); toast.addEventListener('animationend', () => toast.remove()); } }, 5000);
}

function escHtml(str) {
  const div = document.createElement('div');
  div.appendChild(document.createTextNode(String(str)));
  return div.innerHTML;
}

function filterCards(containerId, query) {
  const container = document.getElementById(containerId);
  if (!container) return;
  const q = query.toLowerCase().trim();
  container.querySelectorAll('.item-card').forEach(card => {
    card.style.display = q === '' || card.textContent.toLowerCase().includes(q) ? '' : 'none';
  });
}

const COUNTDOWN_CIRCUMFERENCE = 2 * Math.PI * 20;

function buildCountdownRing(sessionId, sessionDate, startTime, endTime) {
  return `
    <div class="countdown-ring" data-countdown data-session-id="${sessionId}"
         data-start="${sessionDate} ${startTime}" data-end="${sessionDate} ${endTime}">
      <svg viewBox="0 0 48 48">
        <circle class="ring-bg" cx="24" cy="24" r="20" />
        <circle class="ring-progress" cx="24" cy="24" r="20"
                stroke-dasharray="${COUNTDOWN_CIRCUMFERENCE}" stroke-dashoffset="0" />
      </svg>
      <div class="countdown-label">
        <span class="countdown-time">--:--</span>
        <small>remaining</small>
      </div>
    </div>`;
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
    const now = new Date(), end = new Date(endStr), start = new Date(startStr);
    const remaining = end - now;
    const ring = el.querySelector('.ring-progress');
    const label = el.querySelector('.countdown-time');
    if (remaining <= 0) {
      label.textContent = '0:00';
      ring.style.strokeDashoffset = COUNTDOWN_CIRCUMFERENCE;
      ring.classList.remove('warning'); ring.classList.add('danger');
      return;
    }
    const fraction = Math.max(0, Math.min(1, remaining / (end - start)));
    ring.style.strokeDashoffset = COUNTDOWN_CIRCUMFERENCE * (1 - fraction);
    ring.classList.remove('warning', 'danger');
    if (fraction <= 0.15) ring.classList.add('danger');
    else if (fraction <= 0.35) ring.classList.add('warning');
    const mins = Math.floor(remaining / 60000);
    const secs = Math.floor((remaining % 60000) / 1000);
    label.textContent = `${mins}:${secs.toString().padStart(2, '0')}`;
  });
}